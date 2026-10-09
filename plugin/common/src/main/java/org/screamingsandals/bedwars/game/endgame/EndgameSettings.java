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

package org.screamingsandals.bedwars.game.endgame;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.endgame.dragon.DragonBlockPolicy;
import org.screamingsandals.bedwars.lib.debug.Debug;

import java.util.ArrayList;
import java.util.List;

import static org.screamingsandals.bedwars.api.config.GameConfigurationContainer.*;

/**
 * Snapshot of the bed destruction / sudden death / game end by time settings of one arena
 * (arena constants, then variant config, then config.yml, then the defaults below).
 */
public record EndgameSettings(
        boolean bedDestructionAnnounce,
        boolean suddenDeathEnabled,
        boolean suddenDeathDestroyTargets,
        boolean suddenDeathBossbar,
        int dragonsPerTeam,
        int dragonMaxTotal,
        double dragonSpeed,
        double dragonTurnRate,
        boolean dragonInvulnerable,
        double dragonDamageMultiplier,
        boolean dragonDamageOwnTeam,
        @NotNull DragonBlockPolicy.Mode blockDestruction,
        @NotNull List<String> immuneBlocks,
        double boundsMargin,
        double cruiseHeight,
        boolean showName,
        boolean respawnLost,
        boolean forceMobGriefing,
        @NotNull TimeEndMode timeEndMode,
        @NotNull List<TimeEndResolver.Criterion> tieBreak,
        boolean drawCountsAsLoss
) {
    public static final List<String> DEFAULT_IMMUNE_BLOCKS = List.of("#beds", "#doors", "respawn_anchor", "cake",
            "ender_chest", "chest", "trapped_chest", "barrier", "bedrock");

    public static @NotNull EndgameSettings read(@NotNull GameImpl game) {
        var container = game.getConfigurationContainer();
        var name = game.getName();

        var blockMode = DragonBlockPolicy.Mode.fromConfig(container.getOrDefault(SUDDEN_DEATH_DRAGON_BLOCK_DESTRUCTION, "placed"))
                .orElseGet(() -> {
                    Debug.warn(name + ": invalid sudden-death.dragon.block-destruction, using 'placed'", true);
                    return DragonBlockPolicy.Mode.PLACED;
                });
        var timeMode = TimeEndMode.fromConfig(container.getOrDefault(GAME_END_BY_TIME_MODE, "draw"))
                .orElseGet(() -> {
                    Debug.warn(name + ": invalid game-end-by-time.mode, using 'draw'", true);
                    return TimeEndMode.DRAW;
                });

        var warnings = new ArrayList<String>();
        var criteria = TimeEndResolver.parseCriteria(container.getOrDefault(GAME_END_BY_TIME_TIE_BREAK, List.of("target", "players")), warnings);
        warnings.forEach(warning -> Debug.warn(name + ": game-end-by-time.tie-break: " + warning, true));

        return new EndgameSettings(
                container.getOrDefault(BED_DESTRUCTION_ANNOUNCE, true),
                container.getOrDefault(SUDDEN_DEATH_ENABLED, true),
                container.getOrDefault(SUDDEN_DEATH_DESTROY_TARGETS, true),
                container.getOrDefault(SUDDEN_DEATH_BOSSBAR_MESSAGE, true),
                Math.max(0, container.getOrDefault(SUDDEN_DEATH_DRAGON_PER_TEAM, 1)),
                container.getOrDefault(SUDDEN_DEATH_DRAGON_MAX_TOTAL, 16),
                clamp(container.getOrDefault(SUDDEN_DEATH_DRAGON_SPEED, 0.7), 0.05, 3.0),
                clamp(container.getOrDefault(SUDDEN_DEATH_DRAGON_TURN_RATE, 6.0), 0.5, 45.0),
                container.getOrDefault(SUDDEN_DEATH_DRAGON_INVULNERABLE, true),
                Math.max(0.0, container.getOrDefault(SUDDEN_DEATH_DRAGON_DAMAGE_MULTIPLIER, 1.0)),
                container.getOrDefault(SUDDEN_DEATH_DRAGON_DAMAGE_OWN_TEAM, false),
                blockMode,
                List.copyOf(container.getOrDefault(SUDDEN_DEATH_DRAGON_IMMUNE_BLOCKS, DEFAULT_IMMUNE_BLOCKS)),
                Math.max(0.0, container.getOrDefault(SUDDEN_DEATH_DRAGON_BOUNDS_MARGIN, 4.0)),
                Math.max(0.0, container.getOrDefault(SUDDEN_DEATH_DRAGON_CRUISE_HEIGHT, 15.0)),
                container.getOrDefault(SUDDEN_DEATH_DRAGON_SHOW_NAME, true),
                container.getOrDefault(SUDDEN_DEATH_DRAGON_RESPAWN_LOST, true),
                container.getOrDefault(SUDDEN_DEATH_DRAGON_FORCE_MOB_GRIEFING, false),
                timeMode,
                criteria,
                container.getOrDefault(GAME_END_BY_TIME_DRAW_COUNTS_AS_LOSS, false)
        );
    }

    private static double clamp(double value, double lo, double hi) {
        return Math.max(lo, Math.min(hi, value));
    }
}
