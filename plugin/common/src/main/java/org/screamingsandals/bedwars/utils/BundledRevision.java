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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the {@code # fork-revision: N} marker of bundled files (pure, no platform access).
 */
public final class BundledRevision {
    public static final int MAX_SCAN_LINES = 64;
    private static final Pattern MARKER = Pattern.compile("^\\s*#\\s*fork-revision\\s*:\\s*(\\d{1,6})\\s*$");

    private BundledRevision() {
    }

    /**
     * @return the revision of the first marker within the first {@link #MAX_SCAN_LINES} lines, or -1
     */
    public static int parse(@NotNull List<@NotNull String> lines) {
        int limit = Math.min(lines.size(), MAX_SCAN_LINES);
        for (int i = 0; i < limit; i++) {
            Matcher m = MARKER.matcher(lines.get(i));
            if (m.matches()) {
                return Integer.parseInt(m.group(1));
            }
        }
        return -1;
    }

    public static boolean isOutdated(int onDisk, int bundled) {
        return bundled >= 0 && onDisk < bundled;
    }
}
