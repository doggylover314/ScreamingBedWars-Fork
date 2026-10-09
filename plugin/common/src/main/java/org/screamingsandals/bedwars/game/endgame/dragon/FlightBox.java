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

package org.screamingsandals.bedwars.game.endgame.dragon;

import org.jetbrains.annotations.NotNull;

/**
 * Axis aligned box the dragons are allowed to fly in.
 */
public record FlightBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

    /**
     * Creates a box from two corners given in any order.
     */
    public static @NotNull FlightBox of(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new FlightBox(
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)
        );
    }

    /**
     * Moves every face inwards; an axis that would invert collapses to its middle.
     */
    public @NotNull FlightBox shrink(double horizontal, double vertical) {
        double[] x = axis(minX + horizontal, maxX - horizontal);
        double[] y = axis(minY + vertical, maxY - vertical);
        double[] z = axis(minZ + horizontal, maxZ - horizontal);
        return new FlightBox(x[0], y[0], z[0], x[1], y[1], z[1]);
    }

    private static double[] axis(double lo, double hi) {
        if (lo <= hi) {
            return new double[]{lo, hi};
        }
        double middle = (lo + hi) / 2;
        return new double[]{middle, middle};
    }

    /**
     * Intersects the Y range with [lo, hi]; an empty intersection collapses both ends to clamp((lo + hi) / 2, minY, maxY).
     */
    public @NotNull FlightBox withYRange(double lo, double hi) {
        double newMin = Math.max(minY, lo);
        double newMax = Math.min(maxY, hi);
        if (newMin > newMax) {
            double middle = Math.max(minY, Math.min(maxY, (lo + hi) / 2));
            newMin = middle;
            newMax = middle;
        }
        return new FlightBox(minX, newMin, minZ, maxX, newMax, maxZ);
    }

    public boolean contains(@NotNull Vec3 v) {
        return v.x() >= minX && v.x() <= maxX
                && v.y() >= minY && v.y() <= maxY
                && v.z() >= minZ && v.z() <= maxZ;
    }

    public @NotNull Vec3 clamp(@NotNull Vec3 v) {
        return new Vec3(clamp(v.x(), minX, maxX), clamp(v.y(), minY, maxY), clamp(v.z(), minZ, maxZ));
    }

    public @NotNull Vec3 centre() {
        return new Vec3((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    public double width() {
        return maxX - minX;
    }

    public double depth() {
        return maxZ - minZ;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
