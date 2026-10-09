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

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Pure geometry of Minecraft beds (no platform types), used to find the other half of a bed from its block states.
 */
public final class BedGeometry {
    /**
     * Minecraft beds: head = foot + facing (BedBlock#getNeighbourDirection).
     *
     * @return {dx, dz} from this half to the other half, null if the states are unknown
     */
    public static int @Nullable [] otherHalf(@Nullable String part, @Nullable String facing) {
        if (part == null || facing == null) {
            return null;
        }
        int dx;
        int dz;
        switch (facing.toLowerCase(Locale.ROOT)) {
            case "north" -> {
                dx = 0;
                dz = -1;
            }
            case "south" -> {
                dx = 0;
                dz = 1;
            }
            case "west" -> {
                dx = -1;
                dz = 0;
            }
            case "east" -> {
                dx = 1;
                dz = 0;
            }
            default -> {
                return null;
            }
        }
        return switch (part.toLowerCase(Locale.ROOT)) {
            case "foot" -> new int[]{dx, dz};
            case "head" -> new int[]{-dx, -dz};
            default -> null;
        };
    }

    private BedGeometry() {
    }
}
