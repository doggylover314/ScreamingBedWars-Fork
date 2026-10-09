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

import java.util.Locale;

/**
 * Clock formatting helpers (pure, no platform access).
 */
public final class TimeFormat {
    private TimeFormat() {
    }

    /**
     * @return "m:ss" below one hour ("5:32", "50:00"), "h:mm:ss" from one hour ("1:02:03"); negative -> "0:00"
     */
    public static @NotNull String formatClock(long seconds) {
        long s = Math.max(0, seconds);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return h > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec)
                : String.format(Locale.ROOT, "%d:%02d", m, sec);
    }

    /**
     * @return always "m:ss", minutes unbounded ("61:01"); negative -> "0:00"
     */
    public static @NotNull String formatMinutesSeconds(long seconds) {
        long s = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    /**
     * Countdown display: partial seconds round up; &lt;= 0 -> 0.
     */
    public static long ticksToSecondsCeil(long ticks) {
        return ticks <= 0 ? 0 : (ticks + 19) / 20;
    }
}
