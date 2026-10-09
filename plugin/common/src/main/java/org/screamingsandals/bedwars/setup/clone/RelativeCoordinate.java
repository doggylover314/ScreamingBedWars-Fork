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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

/**
 * Parser of a command coordinate token: an integer, a decimal (floored), {@code ~} (the sender's block coordinate)
 * or {@code ~<n>} (block coordinate plus n, floored) (pure).
 */
public final class RelativeCoordinate {
    private RelativeCoordinate() {
    }

    /**
     * "12" -> 12, "-7" -> -7, "12.9" -> 12, "-0.5" -> -1, "~" -> base, "~5" -> base+5, "~-3" -> base-3,
     * "~1.5" -> floor(base+1.5).
     * Empty for: null/blank, unparsable, NaN/infinite, result outside the int range, '~' with base == null.
     */
    public static @NotNull OptionalInt resolve(@Nullable String token, @Nullable Integer base) {
        if (token == null) {
            return OptionalInt.empty();
        }
        var t = token.trim();
        if (t.isEmpty()) {
            return OptionalInt.empty();
        }

        double value;
        if (t.charAt(0) == '~') {
            if (base == null) {
                return OptionalInt.empty();
            }
            var rest = t.substring(1).trim();
            if (rest.isEmpty()) {
                return OptionalInt.of(base);
            }
            var delta = parse(rest);
            if (Double.isNaN(delta)) {
                return OptionalInt.empty();
            }
            value = base + delta;
        } else {
            value = parse(t);
        }

        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return OptionalInt.empty();
        }
        double floored = Math.floor(value);
        if (floored < Integer.MIN_VALUE || floored > Integer.MAX_VALUE) {
            return OptionalInt.empty();
        }
        return OptionalInt.of((int) floored);
    }

    /**
     * NaN when unparsable or not finite. Rejects hex floats, "NaN", "Infinity" and trailing type suffixes.
     */
    private static double parse(@NotNull String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E';
            if (!ok) {
                return Double.NaN;
            }
        }
        try {
            var d = Double.parseDouble(s);
            return Double.isInfinite(d) ? Double.NaN : d;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
