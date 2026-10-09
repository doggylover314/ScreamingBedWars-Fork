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

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeParsingTest {
    private static void assertSeconds(long expected, Object raw) {
        assertEquals(OptionalLong.of(expected), TimeParsing.parseDurationSeconds(raw), "duration of " + raw);
    }

    private static void assertNoSeconds(Object raw) {
        assertTrue(TimeParsing.parseDurationSeconds(raw).isEmpty(), "duration of " + raw + " must be empty");
    }

    private static void assertTicks(long expected, Object raw) {
        assertEquals(OptionalLong.of(expected), TimeParsing.parseIntervalTicks(raw), "interval of " + raw);
    }

    private static void assertNoTicks(Object raw) {
        assertTrue(TimeParsing.parseIntervalTicks(raw).isEmpty(), "interval of " + raw + " must be empty");
    }

    @Test
    void durationAcceptsClockForms() {
        assertSeconds(360, "6:00");
        assertSeconds(30, "0:30");
        assertSeconds(3000, "50:00");
        assertSeconds(3723, "1:02:03");
        assertSeconds(65, " 1:5 ");
    }

    @Test
    void durationAcceptsNumbers() {
        assertSeconds(360, 360); // also what YAML 1.1 produces for an unquoted 6:00 (sexagesimal)
        assertSeconds(360, 360L);
        assertSeconds(0, 0);
        assertSeconds(360, "360");
        assertSeconds(3, 2.6);
        assertSeconds(3, 2.6f);
    }

    @Test
    void durationAcceptsUnits() {
        assertSeconds(90, "90s");
        assertSeconds(360, "6m");
        assertSeconds(360, "6 minutes");
        assertSeconds(360, "6 MIN");
        assertSeconds(3600, "1h");
        assertSeconds(7200, "2 hours");
        assertSeconds(2, "40 ticks");
        assertSeconds(2, "40t");
        assertSeconds(90, "1.5 minutes");
        assertSeconds(10, "10 seconds");
    }

    @Test
    void durationRejectsGarbage() {
        assertNoSeconds("6:60");
        assertNoSeconds("1:60:00");
        assertNoSeconds("1:00:60");
        assertNoSeconds("abc");
        assertNoSeconds("-5");
        assertNoSeconds(-5);
        assertNoSeconds(-0.5);
        assertNoSeconds("");
        assertNoSeconds("   ");
        assertNoSeconds(null);
        assertNoSeconds(Double.NaN);
        assertNoSeconds(true);
        assertNoSeconds("99999999999999999999999"); // overflow
        assertNoSeconds("6 parsecs");
    }

    @Test
    void intervalAcceptsNumbersAsSeconds() {
        assertTicks(600, 30);
        assertTicks(50, 2.5);
        assertTicks(600, 30L);
        assertTicks(600, "30");
        assertTicks(10, "0.5");
    }

    @Test
    void intervalAcceptsUnits() {
        assertTicks(460, "23 seconds");
        assertTicks(460, "23s");
        assertTicks(20, "1 second");
        assertTicks(10, "10 ticks");
        assertTicks(10, "10t");
        assertTicks(1200, "1 minute");
        assertTicks(1200, "1m");
        assertTicks(72000, "1 hour");
        assertTicks(30, "1.5 seconds");
    }

    @Test
    void intervalRejectsZeroNegativeAndGarbage() {
        assertNoTicks("0 seconds");
        assertNoTicks("0");
        assertNoTicks(0);
        assertNoTicks("fast");
        assertNoTicks("-1");
        assertNoTicks(-1);
        assertNoTicks("");
        assertNoTicks(null);
        assertNoTicks(Double.NaN);
        assertNoTicks(Double.POSITIVE_INFINITY);
        assertNoTicks("0.01"); // rounds to 0 ticks
        assertNoTicks(Long.MAX_VALUE);
    }

    @Test
    void tierKeyAcceptsNumbersAndRomanNumerals() {
        assertEquals(2, TimeParsing.parseTierKey(2));
        assertEquals(3, TimeParsing.parseTierKey("3"));
        assertEquals(2, TimeParsing.parseTierKey("II"));
        assertEquals(3, TimeParsing.parseTierKey("iii"));
        assertEquals(4, TimeParsing.parseTierKey(" IV "));
        assertEquals(1, TimeParsing.parseTierKey(1L));
    }

    @Test
    void tierKeyRejectsInvalid() {
        assertEquals(-1, TimeParsing.parseTierKey("0"));
        assertEquals(-1, TimeParsing.parseTierKey(0));
        assertEquals(-1, TimeParsing.parseTierKey(-3));
        assertEquals(-1, TimeParsing.parseTierKey("x1"));
        assertEquals(-1, TimeParsing.parseTierKey(""));
        assertEquals(-1, TimeParsing.parseTierKey("IIII"));
        assertEquals(-1, TimeParsing.parseTierKey("12345678901"));
        assertEquals(-1, TimeParsing.parseTierKey(null));
    }
}
