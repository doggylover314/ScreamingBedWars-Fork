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

package org.screamingsandals.bedwars.game.timeline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.utils.RomanNumerals;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.regex.Pattern;

/**
 * Parsing helpers for the variant {@code timeline:} section (pure).
 * <p>
 * Formatting a clock value is {@link org.screamingsandals.bedwars.utils.TimeFormat#formatClock(long)}.
 */
public final class TimeParsing {
    private static final Pattern MINUTES_SECONDS = Pattern.compile("^(\\d+):(\\d{1,2})$");
    private static final Pattern HOURS_MINUTES_SECONDS = Pattern.compile("^(\\d+):(\\d{1,2}):(\\d{1,2})$");
    private static final Pattern PLAIN_NUMBER = Pattern.compile("^\\d+$");
    private static final Pattern DECIMAL_NUMBER = Pattern.compile("^\\d+(?:\\.\\d+)?$");
    private static final Pattern WITH_UNIT = Pattern.compile(
            "^(\\d+(?:\\.\\d+)?)\\s*(t|ticks?|s|secs?|seconds?|m|mins?|minutes?|h|hours?)$",
            Pattern.CASE_INSENSITIVE
    );

    private TimeParsing() {
    }

    /**
     * Parses a point in time / duration into seconds.
     * <p>
     * Accepts integers (seconds), decimals (rounded), {@code "m:ss"}, {@code "h:mm:ss"}, plain digits and
     * {@code "<number> <unit>"} with the units ticks, seconds, minutes and hours (short forms {@code t s m h} allowed).
     *
     * @return the seconds, or empty when the value is missing, negative or malformed
     */
    public static @NotNull OptionalLong parseDurationSeconds(@Nullable Object raw) {
        if (raw == null) {
            return OptionalLong.empty();
        }
        if (raw instanceof Integer || raw instanceof Long || raw instanceof Short || raw instanceof Byte) {
            long value = ((Number) raw).longValue();
            return value >= 0 ? OptionalLong.of(value) : OptionalLong.empty();
        }
        if (raw instanceof Double || raw instanceof Float) {
            double value = ((Number) raw).doubleValue();
            return Double.isFinite(value) && value >= 0 ? OptionalLong.of(Math.round(value)) : OptionalLong.empty();
        }
        var s = raw.toString().trim();
        try {
            var hms = HOURS_MINUTES_SECONDS.matcher(s);
            if (hms.matches()) {
                long h = Long.parseLong(hms.group(1));
                long m = Long.parseLong(hms.group(2));
                long sec = Long.parseLong(hms.group(3));
                return m < 60 && sec < 60 ? OptionalLong.of(h * 3600 + m * 60 + sec) : OptionalLong.empty();
            }
            var ms = MINUTES_SECONDS.matcher(s);
            if (ms.matches()) {
                long m = Long.parseLong(ms.group(1));
                long sec = Long.parseLong(ms.group(2));
                return sec < 60 ? OptionalLong.of(m * 60 + sec) : OptionalLong.empty();
            }
            if (PLAIN_NUMBER.matcher(s).matches()) {
                return OptionalLong.of(Long.parseLong(s));
            }
            var unit = WITH_UNIT.matcher(s);
            if (unit.matches()) {
                double value = Double.parseDouble(unit.group(1));
                double seconds = switch (Character.toLowerCase(unit.group(2).charAt(0))) {
                    case 't' -> value / 20.0;
                    case 'm' -> value * 60.0;
                    case 'h' -> value * 3600.0;
                    default -> value;
                };
                return OptionalLong.of(Math.round(seconds));
            }
        } catch (NumberFormatException ignored) {
            // overflowing digits
        }
        return OptionalLong.empty();
    }

    /**
     * Parses a generator interval into ticks. Compatible with the {@code interval:} formats of custom spawner types
     * ({@code 30}, {@code "23 seconds"}) plus decimals and short units.
     * <p>
     * Plain numbers (and numeric strings) are seconds.
     *
     * @return the ticks (&gt;= 1), or empty when missing, zero, negative or malformed
     */
    public static @NotNull OptionalLong parseIntervalTicks(@Nullable Object raw) {
        if (raw == null) {
            return OptionalLong.empty();
        }
        long ticks;
        if (raw instanceof Integer || raw instanceof Long || raw instanceof Short || raw instanceof Byte) {
            long value = ((Number) raw).longValue();
            if (value > Long.MAX_VALUE / 20) {
                return OptionalLong.empty();
            }
            ticks = value * 20;
        } else if (raw instanceof Double || raw instanceof Float) {
            double value = ((Number) raw).doubleValue();
            if (!Double.isFinite(value) || value > Long.MAX_VALUE / 40.0) {
                return OptionalLong.empty();
            }
            ticks = Math.round(value * 20);
        } else {
            var s = raw.toString().trim();
            try {
                if (DECIMAL_NUMBER.matcher(s).matches()) {
                    ticks = Math.round(Double.parseDouble(s) * 20);
                } else {
                    var unit = WITH_UNIT.matcher(s);
                    if (!unit.matches()) {
                        return OptionalLong.empty();
                    }
                    double value = Double.parseDouble(unit.group(1));
                    double factor = switch (Character.toLowerCase(unit.group(2).charAt(0))) {
                        case 't' -> 1.0;
                        case 'm' -> 1200.0;
                        case 'h' -> 72000.0;
                        default -> 20.0;
                    };
                    ticks = Math.round(value * factor);
                }
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        }
        return ticks >= 1 ? OptionalLong.of(ticks) : OptionalLong.empty();
    }

    /**
     * Parses a tier number: {@code 2}, {@code "2"}, {@code "II"} and {@code "ii"} all mean 2.
     *
     * @return the tier (&gt;= 1) or -1 when invalid
     */
    public static int parseTierKey(@Nullable Object raw) {
        if (raw == null) {
            return -1;
        }
        if (raw instanceof Number number) {
            int value = number.intValue();
            return value >= 1 ? value : -1;
        }
        var s = raw.toString().trim();
        if (s.isEmpty()) {
            return -1;
        }
        if (PLAIN_NUMBER.matcher(s).matches()) {
            if (s.length() > 9) {
                return -1;
            }
            int value = Integer.parseInt(s);
            return value >= 1 ? value : -1;
        }
        OptionalInt roman = RomanNumerals.fromRoman(s.toUpperCase(Locale.ROOT));
        return roman.isPresent() ? roman.getAsInt() : -1;
    }
}
