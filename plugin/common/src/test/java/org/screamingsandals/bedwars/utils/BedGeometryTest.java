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

package org.screamingsandals.bedwars.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BedGeometryTest {
    @Test
    void footPointsTowardsFacing() {
        assertArrayEquals(new int[]{0, -1}, BedGeometry.otherHalf("foot", "north"));
        assertArrayEquals(new int[]{0, 1}, BedGeometry.otherHalf("foot", "south"));
        assertArrayEquals(new int[]{-1, 0}, BedGeometry.otherHalf("foot", "west"));
        assertArrayEquals(new int[]{1, 0}, BedGeometry.otherHalf("foot", "east"));
    }

    @Test
    void headPointsAgainstFacing() {
        assertArrayEquals(new int[]{0, 1}, BedGeometry.otherHalf("head", "north"));
        assertArrayEquals(new int[]{0, -1}, BedGeometry.otherHalf("head", "south"));
        assertArrayEquals(new int[]{-1, 0}, BedGeometry.otherHalf("head", "east"));
        assertArrayEquals(new int[]{1, 0}, BedGeometry.otherHalf("head", "west"));
    }

    @Test
    void isCaseInsensitive() {
        assertArrayEquals(new int[]{0, -1}, BedGeometry.otherHalf("FOOT", "NORTH"));
    }

    @Test
    void unknownStatesGiveNull() {
        assertNull(BedGeometry.otherHalf("foot", null));
        assertNull(BedGeometry.otherHalf(null, "north"));
        assertNull(BedGeometry.otherHalf("side", "north"));
        assertNull(BedGeometry.otherHalf("foot", "up"));
    }
}
