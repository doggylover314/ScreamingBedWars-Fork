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

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatTest {
    @Test
    void formatClock() {
        assertEquals("0:00", TimeFormat.formatClock(0));
        assertEquals("0:05", TimeFormat.formatClock(5));
        assertEquals("0:59", TimeFormat.formatClock(59));
        assertEquals("1:00", TimeFormat.formatClock(60));
        assertEquals("5:32", TimeFormat.formatClock(332));
        assertEquals("9:59", TimeFormat.formatClock(599));
        assertEquals("50:00", TimeFormat.formatClock(3000));
        assertEquals("59:59", TimeFormat.formatClock(3599));
        assertEquals("1:00:00", TimeFormat.formatClock(3600));
        assertEquals("1:02:03", TimeFormat.formatClock(3723));
        assertEquals("0:00", TimeFormat.formatClock(-5));
    }

    @Test
    void formatMinutesSeconds() {
        assertEquals("60:00", TimeFormat.formatMinutesSeconds(3600));
        assertEquals("61:01", TimeFormat.formatMinutesSeconds(3661));
        assertEquals("5:32", TimeFormat.formatMinutesSeconds(332));
        assertEquals("0:00", TimeFormat.formatMinutesSeconds(-1));
    }

    @Test
    void ticksToSecondsCeil() {
        assertEquals(0, TimeFormat.ticksToSecondsCeil(0));
        assertEquals(0, TimeFormat.ticksToSecondsCeil(-3));
        assertEquals(1, TimeFormat.ticksToSecondsCeil(1));
        assertEquals(1, TimeFormat.ticksToSecondsCeil(20));
        assertEquals(2, TimeFormat.ticksToSecondsCeil(21));
        assertEquals(2, TimeFormat.ticksToSecondsCeil(40));
    }
}
