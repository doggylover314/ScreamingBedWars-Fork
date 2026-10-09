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

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RomanNumeralsTest {
    @Test
    void toRomanConvertsCanonicalNumerals() {
        assertEquals("I", RomanNumerals.toRoman(1));
        assertEquals("II", RomanNumerals.toRoman(2));
        assertEquals("III", RomanNumerals.toRoman(3));
        assertEquals("IV", RomanNumerals.toRoman(4));
        assertEquals("V", RomanNumerals.toRoman(5));
        assertEquals("IX", RomanNumerals.toRoman(9));
        assertEquals("XIV", RomanNumerals.toRoman(14));
        assertEquals("XL", RomanNumerals.toRoman(40));
        assertEquals("XC", RomanNumerals.toRoman(90));
        assertEquals("CD", RomanNumerals.toRoman(400));
        assertEquals("MCMXCIV", RomanNumerals.toRoman(1994));
        assertEquals("MMMCMXCIX", RomanNumerals.toRoman(3999));
    }

    @Test
    void toRomanFallsBackToPlainNumberOutsideRange() {
        assertEquals("0", RomanNumerals.toRoman(0));
        assertEquals("-2", RomanNumerals.toRoman(-2));
        assertEquals("4000", RomanNumerals.toRoman(4000));
    }

    @Test
    void fromRomanParsesCanonicalNumerals() {
        assertEquals(OptionalInt.of(2), RomanNumerals.fromRoman("II"));
        assertEquals(OptionalInt.of(4), RomanNumerals.fromRoman("iv"));
        assertEquals(OptionalInt.of(14), RomanNumerals.fromRoman(" XIV "));
        assertEquals(OptionalInt.of(1994), RomanNumerals.fromRoman("MCMXCIV"));
    }

    @Test
    void fromRomanRejectsNonCanonicalOrGarbage() {
        assertTrue(RomanNumerals.fromRoman("IIII").isEmpty());
        assertTrue(RomanNumerals.fromRoman("VV").isEmpty());
        assertTrue(RomanNumerals.fromRoman("IC").isEmpty());
        assertTrue(RomanNumerals.fromRoman("").isEmpty());
        assertTrue(RomanNumerals.fromRoman("  ").isEmpty());
        assertTrue(RomanNumerals.fromRoman(null).isEmpty());
        assertTrue(RomanNumerals.fromRoman("A").isEmpty());
        assertTrue(RomanNumerals.fromRoman("X5").isEmpty());
    }

    @Test
    void parseIntOrRomanAcceptsBothForms() {
        assertEquals(OptionalInt.of(2), RomanNumerals.parseIntOrRoman("2"));
        assertEquals(OptionalInt.of(2), RomanNumerals.parseIntOrRoman("II"));
        assertEquals(OptionalInt.of(2), RomanNumerals.parseIntOrRoman("ii"));
        assertEquals(OptionalInt.of(0), RomanNumerals.parseIntOrRoman("0"));
    }

    @Test
    void parseIntOrRomanRejectsInvalidInput() {
        assertTrue(RomanNumerals.parseIntOrRoman("-1").isEmpty());
        assertTrue(RomanNumerals.parseIntOrRoman("abc").isEmpty());
        assertTrue(RomanNumerals.parseIntOrRoman("99999999999").isEmpty());
        assertTrue(RomanNumerals.parseIntOrRoman(null).isEmpty());
    }

    @Test
    void roundTripForAllSupportedValues() {
        for (int n = 1; n <= 3999; n++) {
            assertEquals(OptionalInt.of(n), RomanNumerals.fromRoman(RomanNumerals.toRoman(n)), "round trip of " + n);
        }
    }
}
