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

package org.screamingsandals.bedwars.setup.clone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocationStringsTest {
    @Test
    void shiftHasNoBinaryNoise() {
        assertEquals("110.5;54.0;1.75;90.0;0.0", LocationStrings.shift("10.5;64.0;-3.25;90.0;0.0", 100, -10, 5));
        assertEquals("-5.0;0.0;5.0;0.0;0.0", LocationStrings.shift("0.0;0.0;0.0;0.0;0.0", -5, 0, 5));
    }

    @Test
    void shiftKeepsTheNumberOfParts() {
        assertEquals("2;3;4", LocationStrings.shift("1;2;3", 1, 1, 1));
    }

    @Test
    void yawAndPitchAreKeptVerbatim() {
        var input = "1.0;2.0;3.0;12.345678;-89.9";
        assertEquals(input, LocationStrings.shift(input, 0, 0, 0));
        assertEquals("11.0;2.0;3.0;12.345678;-89.9", LocationStrings.shift(input, 10, 0, 0));
    }

    @Test
    void scientificNotationIsAccepted() {
        var shifted = LocationStrings.shift("1.0E-4;0.0;0.0;0.0;0.0", 1, 0, 0);
        assertEquals("1.00010;0.0;0.0;0.0;0.0", shifted);
        assertEquals(1.0001, Double.parseDouble(shifted.split(";")[0]));
    }

    @Test
    void unparsableStringsGiveNull() {
        assertNull(LocationStrings.shift("x;1;2", 1, 1, 1));
        assertNull(LocationStrings.shift("1;2", 1, 1, 1));
        assertNull(LocationStrings.shift("", 1, 1, 1));
        assertNull(LocationStrings.shift("1;;3", 1, 1, 1));
    }

    @Test
    void blockCoordsFloor() {
        assertArrayEquals(new int[]{10, 64, -1}, LocationStrings.blockCoords("10.7;64.2;-0.5;0;0"));
        assertNull(LocationStrings.blockCoords("a;b;c"));
        assertNull(LocationStrings.blockCoords("1;2"));
    }
}
