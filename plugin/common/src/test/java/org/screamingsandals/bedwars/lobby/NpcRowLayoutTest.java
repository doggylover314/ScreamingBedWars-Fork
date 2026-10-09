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

package org.screamingsandals.bedwars.lobby;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcRowLayoutTest {
    private static final double EPS = 1e-9;

    private static void assertOffsets(List<NpcRowLayout.Offset> actual, double... xz) {
        assertEquals(xz.length / 2, actual.size());
        for (int i = 0; i < actual.size(); i++) {
            assertEquals(xz[2 * i], actual.get(i).x(), EPS, "x of #" + i);
            assertEquals(xz[2 * i + 1], actual.get(i).z(), EPS, "z of #" + i);
        }
    }

    @Test
    void facingSouthRowGoesWest() {
        assertOffsets(NpcRowLayout.rowOffsets(3, 2, 0f), 0, 0, -2, 0, -4, 0);
    }

    @Test
    void facingWestRowGoesNorth() {
        assertOffsets(NpcRowLayout.rowOffsets(3, 2, 90f), 0, 0, 0, -2, 0, -4);
    }

    @Test
    void facingNorthRowGoesEast() {
        assertOffsets(NpcRowLayout.rowOffsets(3, 2, 180f), 0, 0, 2, 0, 4, 0);
    }

    @Test
    void facingEastRowGoesSouth() {
        assertOffsets(NpcRowLayout.rowOffsets(2, 1.5, -90f), 0, 0, 0, 1.5);
    }

    @Test
    void zeroAndNegativeCountsAreEmpty() {
        assertTrue(NpcRowLayout.rowOffsets(0, 2, 0f).isEmpty());
        assertTrue(NpcRowLayout.rowOffsets(-3, 2, 45f).isEmpty());
    }

    @Test
    void firstOffsetIsTheOrigin() {
        var first = NpcRowLayout.rowOffsets(1, 5, 33f).get(0);
        assertEquals(0.0, first.x(), EPS);
        assertEquals(0.0, first.z(), EPS);
    }
}
