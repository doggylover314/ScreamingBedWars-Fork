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

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelativeCoordinateTest {
    private static OptionalInt resolve(String token, Integer base) {
        return RelativeCoordinate.resolve(token, base);
    }

    @Test
    void absoluteNumbers() {
        assertEquals(OptionalInt.of(12), resolve("12", null));
        assertEquals(OptionalInt.of(-7), resolve("-7", null));
        assertEquals(OptionalInt.of(12), resolve("12.9", null));
        assertEquals(OptionalInt.of(-1), resolve("-0.5", null));
        assertEquals(OptionalInt.of(0), resolve("0", null));
    }

    @Test
    void relativeNumbers() {
        assertEquals(OptionalInt.of(10), resolve("~", 10));
        assertEquals(OptionalInt.of(15), resolve("~5", 10));
        assertEquals(OptionalInt.of(7), resolve("~-3", 10));
        assertEquals(OptionalInt.of(11), resolve("~1.5", 10));
    }

    @Test
    void absoluteNumbersIgnoreTheBase() {
        assertEquals(OptionalInt.of(3), resolve("3", 100));
    }

    @Test
    void invalidTokens() {
        assertEquals(OptionalInt.empty(), resolve("~", null));
        assertEquals(OptionalInt.empty(), resolve("abc", 5));
        assertEquals(OptionalInt.empty(), resolve("~x", 5));
        assertEquals(OptionalInt.empty(), resolve("", 5));
        assertEquals(OptionalInt.empty(), resolve("   ", 5));
        assertEquals(OptionalInt.empty(), resolve(null, 5));
        assertEquals(OptionalInt.empty(), resolve("NaN", 0));
        assertEquals(OptionalInt.empty(), resolve("Infinity", 0));
        assertEquals(OptionalInt.empty(), resolve("-", 0));
        assertEquals(OptionalInt.empty(), resolve("1d", 0));
    }

    @Test
    void resultsOutsideTheIntRange() {
        assertEquals(OptionalInt.empty(), resolve("99999999999", 0));
        assertEquals(OptionalInt.empty(), resolve("-99999999999", 0));
        assertEquals(OptionalInt.empty(), resolve("~2147483647", 1));
        assertEquals(OptionalInt.of(Integer.MAX_VALUE), resolve("2147483647", 0));
        assertEquals(OptionalInt.of(Integer.MIN_VALUE), resolve("-2147483648", 0));
    }
}
