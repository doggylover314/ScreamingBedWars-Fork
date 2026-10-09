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
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.bedwars.events.GameTickEventImpl;
import org.screamingsandals.bedwars.events.PlayerRespawnedEventImpl;
import org.screamingsandals.bedwars.events.UpgradeLevelChangedEventImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.lib.event.OnEvent;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.List;

/**
 * Applies {@link EffectUpgradeDefinition} effects (Maniac Miner) to the alive members of the team: on level change,
 * one tick after a respawn and periodically while the game is RUNNING.
 * <p>
 * Removal needs no code: leaving the game restores the pre-game effects, death clears effects and spectators are skipped.
 */
@Service
public class TeamEffectUpgradeHandler {
    @OnEvent
    public void onLevelChanged(@NotNull UpgradeLevelChangedEventImpl event) {
        if (!(event.getUpgradable() instanceof TeamImpl)) {
            return;
        }
        if (!(event.getGame().getGameVariant().getUpgrade(event.getName()) instanceof EffectUpgradeDefinition)) {
            return;
        }
        if (event.getGame().getStatus() != GameStatus.RUNNING) {
            return;
        }
        var team = (TeamImpl) event.getUpgradable();
        List.copyOf(team.getPlayers()).forEach(player -> applyAll(player, team));
    }

    @OnEvent
    public void onRespawn(@NotNull PlayerRespawnedEventImpl event) {
        var team = event.getTeam();
        if (team == null) {
            return;
        }
        var player = event.getPlayer();
        // one tick later: the vanilla respawn clears all effects
        Tasker.runDelayed(player, () -> applyAll(player, team), 1L, TaskerTime.TICKS);
    }

    @OnEvent
    public void onTick(@NotNull GameTickEventImpl event) {
        if (event.getStatus() != GameStatus.RUNNING) {
            return;
        }
        int period = MainConfig.getInstance().node("upgrades", "effect-refresh-seconds").getInt(3);
        if (period <= 0) {
            return;
        }
        var game = event.getGame();
        long elapsed = (long) game.getGameTime() - event.getCountdown();
        if (elapsed % period != 0) {
            return;
        }
        for (var team : game.getTeamsInGame()) {
            List.copyOf(team.getPlayers()).forEach(player -> applyAll(player, team));
        }
    }

    public static void applyAll(@NotNull BedWarsPlayer player, @NotNull TeamImpl team) {
        if (!player.isInGame() || player.isSpectator()) {
            return;
        }
        var game = player.getGame();
        if (game.getStatus() != GameStatus.RUNNING || game.getPlayerTeam(player) != team) {
            return;
        }
        int refreshTicks = Math.max(1, MainConfig.getInstance().node("upgrades", "effect-refresh-seconds").getInt(3)) * 20;
        for (var entry : game.getGameVariant().getUpgrades().entrySet()) {
            if (!(entry.getValue() instanceof EffectUpgradeDefinition)) {
                continue;
            }
            var definition = (EffectUpgradeDefinition) entry.getValue();
            var upgrade = team.getUpgrade(entry.getKey());
            int level = upgrade == null ? 0 : (int) Math.floor(upgrade.getLevel() - upgrade.getInitialLevel() + 1e-9);
            var desired = definition.effectForLevel(level);
            if (desired == null) {
                continue; // level 0: leave other sources of the effect alone
            }
            var current = player.getActivePotionEffects().stream()
                    .filter(effect -> effect.type().equals(definition.getType()))
                    .findFirst()
                    .orElse(null);
            if (!EffectUpgradeSpec.needsReapply(
                    current == null ? null : current.amplifier(),
                    current == null ? null : current.duration(),
                    desired.amplifier(),
                    definition.getSpec().permanent(),
                    refreshTicks
            )) {
                continue;
            }
            if (current != null && current.amplifier() != desired.amplifier()) {
                player.removePotionEffect(definition.getType().asEffect());
            }
            player.addPotionEffect(desired);
        }
    }
}
