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

/**
 * Whole-block translation applied to every coordinate of a cloned arena (pure).
 */
public record BlockOffset(int dx, int dy, int dz) {

    /**
     * The offset that moves the minimum corner of {@code source} to the target coordinates.
     */
    public static @NotNull BlockOffset toMinCorner(@NotNull BlockBox source, int targetMinX, int targetMinY, int targetMinZ) {
        return new BlockOffset(
                saturate((long) targetMinX - source.minX()),
                saturate((long) targetMinY - source.minY()),
                saturate((long) targetMinZ - source.minZ())
        );
    }

    public boolean isZero() {
        return dx == 0 && dy == 0 && dz == 0;
    }

    static int saturate(long v) {
        if (v > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (v < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) v;
    }
}
