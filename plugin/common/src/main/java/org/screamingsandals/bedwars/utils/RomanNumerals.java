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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.OptionalInt;

/**
 * Roman numeral helpers (pure, no platform access).
 */
public final class RomanNumerals {
    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private RomanNumerals() {
    }

    /**
     * @return 1..3999 as a canonical roman numeral; any other value as {@link Integer#toString(int)}
     */
    public static @NotNull String toRoman(int value) {
        if (value < 1 || value > 3999) {
            return Integer.toString(value);
        }
        var sb = new StringBuilder();
        int rest = value;
        for (int i = 0; i < VALUES.length; i++) {
            while (rest >= VALUES[i]) {
                sb.append(SYMBOLS[i]);
                rest -= VALUES[i];
            }
        }
        return sb.toString();
    }

    /**
     * Case-insensitive, trimmed; canonical numerals only ("IIII", "IC", "VV" are rejected).
     */
    public static @NotNull OptionalInt fromRoman(@Nullable String roman) {
        if (roman == null) {
            return OptionalInt.empty();
        }
        String s = roman.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) {
            return OptionalInt.empty();
        }
        int total = 0;
        int pos = 0;
        for (int i = 0; i < VALUES.length && pos < s.length(); i++) {
            while (s.startsWith(SYMBOLS[i], pos)) {
                total += VALUES[i];
                pos += SYMBOLS[i].length();
            }
        }
        if (pos != s.length() || total < 1 || total > 3999 || !toRoman(total).equals(s)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(total);
    }

    /**
     * "2" -> 2, "II"/"ii" -> 2, "0" -> 0; negative numbers, overflow and garbage -> empty.
     */
    public static @NotNull OptionalInt parseIntOrRoman(@Nullable String text) {
        if (text == null) {
            return OptionalInt.empty();
        }
        String s = text.trim();
        if (!s.isEmpty() && s.chars().allMatch(c -> c >= '0' && c <= '9')) {
            if (s.length() > 9) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(Integer.parseInt(s));
        }
        return fromRoman(s);
    }
}
