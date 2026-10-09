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

package org.screamingsandals.bedwars.game.endgame.dragon;

import org.jetbrains.annotations.NotNull;

/**
 * Applies the arena-wide dragon cap to the dragons the teams asked for.
 */
public final class DragonAllocation {

    private DragonAllocation() {
    }

    /**
     * Negative requests count as 0; {@code maxTotal <= 0} means unlimited; over the cap the dragons are handed out
     * round robin (one each in team order, then a second one each ...).
     *
     * @param requested dragons wanted per team
     * @param maxTotal  cap for all teams together
     * @return dragons granted per team (same order and length as {@code requested})
     */
    public static int @NotNull [] allocate(int @NotNull [] requested, int maxTotal) {
        int n = requested.length;
        int[] req = new int[n];
        int sum = 0;
        for (int i = 0; i < n; i++) {
            req[i] = Math.max(0, requested[i]);
            sum += req[i];
        }
        if (maxTotal <= 0 || sum <= maxTotal) {
            return req;
        }
        int[] out = new int[n];
        int remaining = maxTotal;
        boolean progress = true;
        while (remaining > 0 && progress) {
            progress = false;
            for (int i = 0; i < n && remaining > 0; i++) {
                if (out[i] < req[i]) {
                    out[i]++;
                    remaining--;
                    progress = true;
                }
            }
        }
        return out;
    }
}
