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

import java.util.Collection;

/**
 * Inclusive chunk range of an arena (pure). It is computed exactly like {@code GameImpl#configureChunkTickets}
 * (min/max of the pos1/pos2 block coordinates shifted right by 4) so that a clone job can tell which chunks another
 * arena claims plugin chunk tickets on.
 *
 * @param world   world name
 * @param minCX   lowest chunk X
 * @param maxCX   highest chunk X
 * @param minCZ   lowest chunk Z
 * @param maxCZ   highest chunk Z
 */
public record ChunkRange(@NotNull String world, int minCX, int maxCX, int minCZ, int maxCZ) {

    /**
     * @param x1 block X of the first corner
     * @param z1 block Z of the first corner
     * @param x2 block X of the second corner
     * @param z2 block Z of the second corner
     */
    public static @NotNull ChunkRange ofBlocks(@NotNull String world, int x1, int z1, int x2, int z2) {
        return new ChunkRange(world,
                Math.min(x1, x2) >> 4, Math.max(x1, x2) >> 4,
                Math.min(z1, z2) >> 4, Math.max(z1, z2) >> 4);
    }

    public boolean contains(@NotNull String world, int cx, int cz) {
        return this.world.equals(world) && cx >= minCX && cx <= maxCX && cz >= minCZ && cz <= maxCZ;
    }

    public static boolean anyContains(@NotNull Collection<ChunkRange> ranges, @NotNull String world, int cx, int cz) {
        for (var range : ranges) {
            if (range.contains(world, cx, cz)) {
                return true;
            }
        }
        return false;
    }
}
