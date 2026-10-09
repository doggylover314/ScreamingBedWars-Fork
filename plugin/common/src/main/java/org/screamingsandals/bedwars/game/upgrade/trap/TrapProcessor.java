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

package org.screamingsandals.bedwars.game.upgrade.trap;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.events.TrapTriggeredEventImpl;
import org.screamingsandals.bedwars.events.UpgradeLevelChangedEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.upgrade.builtin.TrapUpgradeDefinition;
import org.screamingsandals.bedwars.inventories.upgrade.UpgradeShopRefresher;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.bedwars.utils.Broadcasts;
import org.screamingsandals.bedwars.utils.TitleUtils;
import org.screamingsandals.lib.event.EventManager;
import org.screamingsandals.lib.item.meta.PotionEffectType;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.world.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Runtime trap logic, called once per second from {@code GameCycleImpl#processTraps} while the game is RUNNING.
 * Handles two mechanisms: permanent / single-use base area effects ({@link TrapUpgradeDefinition}, for example Heal
 * Pool) and the Hypixel-like trap queue (only the first queued trap fires, then the team cooldown starts).
 */
public final class TrapProcessor {
    private TrapProcessor() {
    }

    public static void process(@NotNull GameImpl game, @NotNull List<@NotNull TeamImpl> runningTeams) {
        long now = (long) game.getGameTime() - game.getCountdown(); // elapsed RUNNING seconds (monotonic)
        var queueDefinition = game.getGameVariant().getTrapQueue();
        var candidates = game.getConnectedPlayers().stream()
                .filter(player -> !player.isSpectator())
                .collect(Collectors.toList()); // once per tick for all teams
        for (var team : runningTeams) {
            var base = TeamBase.center(team);
            if (base == null) {
                continue;
            }
            processAreaTraps(game, team, base, candidates);
            if (queueDefinition.enabled()) {
                processQueue(game, team, base, candidates, queueDefinition, now);
            }
        }
    }

    private static boolean inRange(@NotNull Location base, @NotNull BedWarsPlayer player, double squaredRange) {
        var location = player.getLocation();
        return Objects.equals(base.getWorld(), location.getWorld()) && base.getDistanceSquared(location) <= squaredRange;
    }

    // ---- base area effects: Heal Pool and legacy single-use traps ----

    private static void processAreaTraps(@NotNull GameImpl game, @NotNull TeamImpl team, @NotNull Location base,
                                         @NotNull List<BedWarsPlayer> candidates) {
        for (var trap : List.copyOf(team.getTraps())) {
            var squared = trap.getDetectionRange() * trap.getDetectionRange();

            var stream = candidates.stream();
            if (trap.isEnemies() && !trap.isTeam()) {
                stream = stream.filter(player -> !team.isPlayerInTeam(player));
            } else if (!trap.isEnemies() && trap.isTeam()) {
                stream = stream.filter(team::isPlayerInTeam);
            }
            var affected = stream.filter(player -> inRange(base, player, squared)).collect(Collectors.toList());
            if (affected.isEmpty()) {
                continue;
            }

            boolean any = false;
            for (var player : affected) {
                var event = new TrapTriggeredEventImpl(game, player, team, trap.getEffects(), trap.isTeam(), trap.isEnemies(),
                        trap.isSingularUse(), trap.getDetectionRange());
                EventManager.fire(event);
                if (event.cancelled()) {
                    continue;
                }

                player.addPotionEffects(trap.getEffects());
                any = true;

                if (trap.getName() != null && trap.getMessage() != null) {
                    player.sendMessage(
                            Message.ofRichText(trap.getMessage())
                                    .prefix(game.getCustomPrefixComponent())
                                    .placeholder("team", Component.text(team.getName(), team.getColor().getTextColor()))
                                    .placeholder("trap", trap.getName())
                    );
                }
            }

            if (!any || !trap.isSingularUse()) {
                continue; // permanent area effects: no notification, as before
            }

            team.getTraps().remove(trap);
            resetLegacyTrapLevel(game, team, trap); // legacy single-use traps become re-buyable
            UpgradeShopRefresher.refreshTeam(team, null); // after the reset: open upgrade shops must show the re-buyable state

            if (trap.getName() != null) { // once per trigger, not once per affected player
                if (trap.getTeamTitle() != null) {
                    var title = Message
                            .ofRichText(trap.getTeamTitle())
                            .joinRichText(trap.getTeamSubtitle() != null ? trap.getTeamSubtitle() : "")
                            .placeholder("trap", trap.getName())
                            .times(TitleUtils.defaultTimes());
                    team.getPlayers().forEach(member -> member.showTitle(title));
                }
                var triggerSound = trap.getTriggerSound();
                if (triggerSound != null) {
                    team.getPlayers().forEach(member -> member.playSound(triggerSound));
                }
            }
        }
    }

    private static void resetLegacyTrapLevel(@NotNull GameImpl game, @NotNull TeamImpl team, @NotNull TrapUpgradeDefinition trap) {
        for (var entry : game.getGameVariant().getUpgrades().entrySet()) {
            if (entry.getValue() != trap) {
                continue;
            }
            var upgrade = team.getUpgrade(entry.getKey());
            if (upgrade == null || upgrade.getLevel() == upgrade.getInitialLevel()) {
                continue;
            }
            double oldLevel = upgrade.getLevel();
            upgrade.setLevel(upgrade.getInitialLevel());
            EventManager.fire(new UpgradeLevelChangedEventImpl(game, team, entry.getKey(), upgrade, oldLevel));
        }
    }

    // ---- trap queue ----

    private static void processQueue(@NotNull GameImpl game, @NotNull TeamImpl team, @NotNull Location base,
                                     @NotNull List<BedWarsPlayer> candidates, @NotNull TrapQueueDefinition definition, long now) {
        var queue = team.getTrapQueue();
        if (queue.isEmpty() || !queue.isReady(now)) {
            return;
        }
        var headId = queue.peek().orElse(null);
        if (headId == null) {
            return;
        }
        var trap = definition.trap(headId);
        if (trap == null) { // stale id (cannot normally happen)
            queue.dropHead();
            return;
        }

        double range = trap.getSpec().detectionRange();
        double squared = range * range;
        var intruders = candidates.stream()
                .filter(player -> !team.isPlayerInTeam(player) && game.getPlayerTeam(player) != null) // enemies that play (not lobby / spectators)
                .filter(player -> inRange(base, player, squared))
                .sorted(Comparator.comparingDouble(player -> base.getDistanceSquared(player.getLocation())))
                .collect(Collectors.toList());
        if (intruders.isEmpty()) {
            return;
        }

        var trigger = intruders.get(0);
        var event = new TrapTriggeredEventImpl(game, trigger, team, trap.getEnemyEffects(), !trap.getTeamEffects().isEmpty(),
                !trap.getEnemyEffects().isEmpty() || trap.getSpec().revealInvisible(), true, range);
        EventManager.fire(event);
        if (event.cancelled()) {
            return; // not consumed; retried next second
        }
        queue.consume(now, definition.settings().cooldownSeconds()); // only the FIRST queued trap fires

        var victims = trap.getSpec().affectAllIntruders() ? intruders : List.of(trigger);
        var teamComponent = Component.text(team.getName(), team.getColor().getTextColor());
        var invisibility = PotionEffectType.ofNullable("invisibility");
        for (var victim : victims) {
            if (!trap.getEnemyEffects().isEmpty()) {
                victim.addPotionEffects(trap.getEnemyEffects());
            }
            if (trap.getSpec().revealInvisible()) {
                if (invisibility != null) {
                    victim.removePotionEffect(invisibility.asEffect());
                }
                var victimTeam = game.getPlayerTeam(victim);
                Message.of(ForkLangKeys.IN_GAME_TRAPS_ALARM_TEAM)
                        .prefixOrDefault(game.getCustomPrefixComponent())
                        .placeholder("player", victim.getDisplayName())
                        .placeholder("intruder-team", victimTeam == null
                                ? Component.text("?")
                                : Component.text(victimTeam.getName(), victimTeam.getColor().getTextColor()))
                        .send(team.getPlayers());
            }
            if (trap.getSpec().messageIntruder()) {
                victim.sendMessage(Message.of(ForkLangKeys.IN_GAME_TRAPS_TRIGGERED_INTRUDER)
                        .prefixOrDefault(game.getCustomPrefixComponent())
                        .placeholder("team", teamComponent)
                        .placeholder("trap", trap.displayName()));
            }
        }

        if (!trap.getTeamEffects().isEmpty()) {
            double teamRange = trap.getSpec().teamEffectRange();
            for (var member : List.copyOf(team.getPlayers())) {
                if (!member.isSpectator() && (teamRange <= 0 || inRange(base, member, teamRange * teamRange))) {
                    member.addPotionEffects(trap.getTeamEffects());
                }
            }
        }

        // notify the owning team (all members including respawning ones)
        var receivers = new ArrayList<>(team.getPlayers());
        Broadcasts.title(receivers, Message.of(ForkLangKeys.IN_GAME_TRAPS_TRIGGERED_TITLE)
                .join(ForkLangKeys.IN_GAME_TRAPS_TRIGGERED_SUBTITLE)
                .placeholder("trap", trap.displayName()));
        Message.of(ForkLangKeys.IN_GAME_TRAPS_TRIGGERED_TEAM)
                .prefixOrDefault(game.getCustomPrefixComponent())
                .placeholder("trap", trap.displayName())
                .send(receivers);
        var sound = trap.getTriggerSound() != null
                ? trap.getTriggerSound()
                : Broadcasts.configuredSound("trap_triggered", "entity.ender_dragon.growl");
        Broadcasts.sound(receivers, sound);

        UpgradeShopRefresher.refreshTeam(team, null); // the queue display changed
    }
}
