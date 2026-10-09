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

import java.math.BigDecimal;

/**
 * Helpers for the location string format of {@code MiscUtils.writeLocationToString}: {@code x;y;z;yaw;pitch} (pure).
 */
public final class LocationStrings {
    private LocationStrings() {
    }

    /**
     * Adds the offset to x/y/z with {@link BigDecimal} (no binary noise: "10.5"+100 -> "110.5", "64.0"+5 -> "69.0");
     * all other parts are kept verbatim. Null if there are fewer than 3 parts or x/y/z are not decimal numbers.
     */
    public static @Nullable String shift(@NotNull String loc, int dx, int dy, int dz) {
        var parts = loc.split(";", -1);
        if (parts.length < 3) {
            return null;
        }
        int[] d = {dx, dy, dz};
        var out = new String[parts.length];
        System.arraycopy(parts, 0, out, 0, parts.length);
        try {
            for (int i = 0; i < 3; i++) {
                out[i] = new BigDecimal(parts[i].trim()).add(BigDecimal.valueOf(d[i])).toPlainString();
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return String.join(";", out);
    }

    /**
     * Floor of x/y/z, null if the string cannot be parsed.
     */
    public static int @Nullable [] blockCoords(@NotNull String loc) {
        var parts = loc.split(";", -1);
        if (parts.length < 3) {
            return null;
        }
        var result = new int[3];
        try {
            for (int i = 0; i < 3; i++) {
                result[i] = BlockBox.floor(Double.parseDouble(parts[i].trim()));
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }
}
