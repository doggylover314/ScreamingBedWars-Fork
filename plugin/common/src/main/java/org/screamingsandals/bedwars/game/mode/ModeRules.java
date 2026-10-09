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

package org.screamingsandals.bedwars.game.mode;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Pure rules of the game modes.
 */
public final class ModeRules {
    private ModeRules() {
    }

    /**
     * "Two teams' worth" (approximate Hypixel behaviour).
     */
    public static int defaultMinPlayers(int teamSize) {
        return 2 * teamSize;
    }

    /**
     * Clamp into [2, teamCount*teamSize]; a game can never start with fewer than 2 players.
     */
    public static int clampMinPlayers(int configured, int teamCount, int teamSize) {
        int max = Math.max(2, teamCount * teamSize);
        return Math.max(2, Math.min(configured, max));
    }

    /**
     * (2,4) -> "4v4", (3,1) -> "1v1v1", (4,2) -> "2v2v2v2".
     */
    public static @NotNull String defaultId(int teamCount, int teamSize) {
        return String.join("v", Collections.nCopies(teamCount, Integer.toString(teamSize)));
    }

    /**
     * Ceiling division, a >= 0, b > 0.
     */
    public static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    public static boolean partyFits(@NotNull ModeDefinition mode, int partySize) {
        return partySize <= mode.teamSize();
    }
}
