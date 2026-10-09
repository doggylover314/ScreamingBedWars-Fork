/*
 * Copyright (C) 2026 ScreamingSandals
 *
 * This file is part of Screaming BedWars.
 *
 * Screaming BedWars is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Screaming BedWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Screaming BedWars. If not, see <https://www.gnu.org/licenses/>.
 */

package org.screamingsandals.bedwars.game.upgrade.builtin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.events.GameEndEventImpl;
import org.screamingsandals.bedwars.events.GameStartedEventImpl;
import org.screamingsandals.bedwars.events.GameTickEventImpl;
import org.screamingsandals.bedwars.events.PreRebuildingEventImpl;
import org.screamingsandals.bedwars.events.UpgradeLevelChangedEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.ItemSpawnerImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.upgrade.trap.TeamBase;
import org.screamingsandals.bedwars.lib.debug.Debug;
import org.screamingsandals.lib.entity.Entities;
import org.screamingsandals.lib.event.OnEvent;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.methods.OnPreDisable;
import org.screamingsandals.lib.world.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Applies {@link ForgeUpgradeDefinition} tiers: raises the amount per spawn of the team's iron/gold generators and
 * drops emeralds at the team generator on emerald tiers.
 * <p>
 * Generators are the ones linked to the team; when the team has none of a type linked, the closest UNLINKED generator
 * of that type near the team base is used (first team to buy claims it).
 */
@Service
public class ForgeUpgradeHandler {
    private final Map<UUID, GameForgeState> games = new ConcurrentHashMap<>();

    static final class GameForgeState {
        /** team name to state */
        final Map<String, TeamForgeState> teams = new HashMap<>();
        /** auto-discovered spawner to team name */
        final Map<ItemSpawnerImpl, String> claimedBy = new IdentityHashMap<>();
    }

    static final class TeamForgeState {
        final Map<ItemSpawnerImpl, Double> appliedDelta = new IdentityHashMap<>();
        @Nullable ForgeUpgradeDefinition definition;
        ForgeSpec.ForgeTier tier = ForgeSpec.ForgeTier.NONE;
        @Nullable Location emeraldLocation;
        /** seconds since the emerald tier became active */
        long emeraldSeconds;
    }

    @OnEvent
    public void onLevelChanged(@NotNull UpgradeLevelChangedEventImpl event) {
        if (!(event.getUpgradable() instanceof TeamImpl)) {
            return;
        }
        var definition = event.getGame().getGameVariant().getUpgrade(event.getName());
        if (!(definition instanceof ForgeUpgradeDefinition)) {
            return;
        }
        var upgrade = event.getUpgrade();
        apply(event.getGame(), (TeamImpl) event.getUpgradable(), (ForgeUpgradeDefinition) definition,
                (int) Math.floor(upgrade.getLevel() - upgrade.getInitialLevel() + 1e-9));
    }

    void apply(@NotNull GameImpl game, @NotNull TeamImpl team, @NotNull ForgeUpgradeDefinition definition, int level) {
        var gameState = games.computeIfAbsent(game.getUuid(), k -> new GameForgeState());
        var teamState = gameState.teams.computeIfAbsent(team.getName(), k -> new TeamForgeState());
        var tier = definition.getSpec().tierFor(level);
        var spawners = selectSpawners(game, team, definition.getSpec(), gameState);
        for (var spawner : spawners) {
            double previous = teamState.appliedDelta.getOrDefault(spawner, 0.0);
            double delta = ForgeMath.delta(spawner.getBaseAmountPerSpawn(), tier.resourceBonus());
            teamState.appliedDelta.put(spawner, delta);
            spawner.getLocation().tasker().run(() ->
                    spawner.setAmountPerSpawn(ForgeMath.rebase(spawner.getAmountPerSpawn(), previous, delta))
            );
        }
        boolean hadEmeralds = teamState.tier.spawnsEmeralds();
        teamState.definition = definition;
        teamState.tier = tier;
        // the first entry of spawner-types wins (iron)
        teamState.emeraldLocation = spawners.isEmpty() ? null : spawners.get(0).getLocation();
        if (!tier.spawnsEmeralds() || !hadEmeralds) {
            teamState.emeraldSeconds = 0;
        }
        if (tier.spawnsEmeralds() && teamState.emeraldLocation == null) {
            Debug.warn(game.getName() + ": forge of team " + team.getName() + " has no generator to spawn emeralds at", true);
        }
    }

    /**
     * Linked spawners of each configured type; else (auto-discover) the closest UNLINKED, unclaimed one within range.
     */
    static @NotNull List<ItemSpawnerImpl> selectSpawners(@NotNull GameImpl game, @NotNull TeamImpl team, @NotNull ForgeSpec spec, @NotNull GameForgeState gameState) {
        var base = TeamBase.center(team);
        var result = new ArrayList<ItemSpawnerImpl>();
        for (var typeKey : spec.spawnerTypes()) {
            var linked = game.getSpawners().stream()
                    .filter(s -> s.getTeam() == team && typeKey.equalsIgnoreCase(s.getItemSpawnerType().configKey()))
                    .collect(Collectors.toList());
            if (!linked.isEmpty()) {
                result.addAll(linked);
                continue;
            }
            if (!spec.autoDiscover() || base == null) {
                continue;
            }
            double max = spec.autoDiscoverMaxDistance();
            game.getSpawners().stream()
                    .filter(s -> s.getTeam() == null && typeKey.equalsIgnoreCase(s.getItemSpawnerType().configKey()))
                    .filter(s -> {
                        var owner = gameState.claimedBy.get(s);
                        return owner == null || owner.equals(team.getName());
                    })
                    .filter(s -> Objects.equals(s.getLocation().getWorld(), base.getWorld()))
                    .filter(s -> max <= 0 || s.getLocation().getDistanceSquared(base) <= max * max)
                    .min(Comparator.comparingDouble(s -> s.getLocation().getDistanceSquared(base)))
                    .ifPresent(s -> {
                        gameState.claimedBy.put(s, team.getName());
                        result.add(s);
                    });
        }
        return result;
    }

    @OnEvent
    public void onTick(@NotNull GameTickEventImpl event) {
        if (event.getStatus() != GameStatus.RUNNING) {
            return;
        }
        var game = event.getGame();
        var gameState = games.get(game.getUuid());
        if (gameState == null) {
            return;
        }
        for (var team : game.getTeamsAlive()) {
            var teamState = gameState.teams.get(team.getName());
            if (teamState == null || teamState.definition == null || !teamState.tier.spawnsEmeralds() || teamState.emeraldLocation == null) {
                continue;
            }
            teamState.emeraldSeconds++;
            if (ForgeMath.shouldSpawnEmerald(teamState.emeraldSeconds, teamState.tier.emeraldIntervalSeconds())) {
                spawnEmeralds(game, teamState.emeraldLocation, teamState.definition.getSpec().emeraldSpawnerType(), teamState.tier.emeraldAmount());
            }
        }
    }

    static void spawnEmeralds(@NotNull GameImpl game, @NotNull Location at, @NotNull String typeKey, int amount) {
        var type = game.getGameVariant().getItemSpawnerType(typeKey);
        if (type == null) {
            Debug.warn(game.getName() + ": forge emerald spawner type '" + typeKey + "' does not exist", true);
            return;
        }
        var location = at.add(0, 0.05, 0);
        location.tasker().run(() -> {
            // named currency item, like ItemSpawnerImpl
            var item = Entities.dropItem(type.getItem(amount), location);
            if (item == null) {
                return;
            }
            item.setPickupDelay(0, TimeUnit.SECONDS);
            if (type.getSpread() != 1.0) {
                item.setVelocity(item.getVelocity().multiply(type.getSpread()));
            }
        });
    }

    @OnEvent
    public void onStarted(@NotNull GameStartedEventImpl event) {
        games.remove(event.getGame().getUuid());
    }

    @OnEvent
    public void onEnd(@NotNull GameEndEventImpl event) {
        games.remove(event.getGame().getUuid());
    }

    @OnEvent
    public void onRebuild(@NotNull PreRebuildingEventImpl event) {
        games.remove(event.getGame().getUuid());
    }

    @OnPreDisable
    public void onPreDisable() {
        games.clear();
    }
}
