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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class DragonAllocationTest {
    @Test
    void underTheCapNothingChanges() {
        assertArrayEquals(new int[]{1, 1, 1, 1}, DragonAllocation.allocate(new int[]{1, 1, 1, 1}, 16));
    }

    @Test
    void overTheCapDragonsAreHandedOutRoundRobin() {
        assertArrayEquals(new int[]{1, 1, 1, 1}, DragonAllocation.allocate(new int[]{2, 1, 2, 1}, 4));
        assertArrayEquals(new int[]{2, 1, 1, 1}, DragonAllocation.allocate(new int[]{2, 1, 2, 1}, 5));
    }

    @Test
    void aNonPositiveCapMeansUnlimited() {
        assertArrayEquals(new int[]{2, 1, 2, 1}, DragonAllocation.allocate(new int[]{2, 1, 2, 1}, 0));
        assertArrayEquals(new int[]{2, 1, 2, 1}, DragonAllocation.allocate(new int[]{2, 1, 2, 1}, -1));
    }

    @Test
    void negativeRequestsCountAsZero() {
        assertArrayEquals(new int[]{0, 3}, DragonAllocation.allocate(new int[]{-1, 3}, 10));
    }

    @Test
    void aCapOfOneGoesToTheFirstTeam() {
        assertArrayEquals(new int[]{1, 0}, DragonAllocation.allocate(new int[]{3, 3}, 1));
    }

    @Test
    void emptyInput() {
        assertArrayEquals(new int[0], DragonAllocation.allocate(new int[0], 5));
    }

    @Test
    void theResultNeverExceedsTheCapOrTheRequest() {
        var result = DragonAllocation.allocate(new int[]{5, 0, 2, 7}, 6);
        int sum = 0;
        for (int i = 0; i < result.length; i++) {
            sum += result[i];
        }
        org.junit.jupiter.api.Assertions.assertEquals(6, sum);
        org.junit.jupiter.api.Assertions.assertEquals(0, result[1]);
        org.junit.jupiter.api.Assertions.assertTrue(result[2] <= 2);
    }
}
