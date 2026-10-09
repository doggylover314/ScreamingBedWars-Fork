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
 * Inclusive integer box of blocks (pure).
 * <p>
 * Why block boxes: {@code ArenaUtils.isInArea} compares the exact doubles of pos1/pos2; the box of the blocks that
 * contain pos1/pos2 is a superset of that region, so every block that can belong to the arena is copied.
 */
public record BlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    /**
     * Normalises min/max per axis.
     */
    public static @NotNull BlockBox of(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new BlockBox(
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)
        );
    }

    /**
     * Floors every coordinate, then {@link #of}.
     */
    public static @NotNull BlockBox ofDoubles(double x1, double y1, double z1, double x2, double y2, double z2) {
        return of(floor(x1), floor(y1), floor(z1), floor(x2), floor(y2), floor(z2));
    }

    static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    public long sizeX() {
        return (long) maxX - minX + 1;
    }

    public long sizeY() {
        return (long) maxY - minY + 1;
    }

    public long sizeZ() {
        return (long) maxZ - minZ + 1;
    }

    public long volume() {
        return sizeX() * sizeY() * sizeZ();
    }

    /**
     * The box moved by the offset (saturating at the int range instead of overflowing).
     */
    public @NotNull BlockBox shift(@NotNull BlockOffset o) {
        return new BlockBox(
                BlockOffset.saturate((long) minX + o.dx()), BlockOffset.saturate((long) minY + o.dy()), BlockOffset.saturate((long) minZ + o.dz()),
                BlockOffset.saturate((long) maxX + o.dx()), BlockOffset.saturate((long) maxY + o.dy()), BlockOffset.saturate((long) maxZ + o.dz())
        );
    }

    public @NotNull BlockBox withY(int minY, int maxY) {
        return of(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * Whether the block containing the point is inside the box (each coordinate is floored).
     */
    public boolean containsPoint(double x, double y, double z) {
        return contains(floor(x), floor(y), floor(z));
    }

    public boolean contains(@NotNull BlockBox other) {
        return other.minX >= minX && other.maxX <= maxX
                && other.minY >= minY && other.maxY <= maxY
                && other.minZ >= minZ && other.maxZ <= maxZ;
    }

    /**
     * Inclusive: touching boxes intersect.
     */
    public boolean intersects(@NotNull BlockBox o) {
        return minX <= o.maxX && maxX >= o.minX
                && minY <= o.maxY && maxY >= o.minY
                && minZ <= o.maxZ && maxZ >= o.minZ;
    }

    public @NotNull String minString() {
        return minX + "/" + minY + "/" + minZ;
    }

    public @NotNull String maxString() {
        return maxX + "/" + maxY + "/" + maxZ;
    }

    /**
     * {@code sizeX x sizeY x sizeZ}
     */
    public @NotNull String sizeString() {
        return sizeX() + " x " + sizeY() + " x " + sizeZ();
    }
}
