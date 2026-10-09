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
 * Immutable 3D point/vector used by the pure dragon flight maths.
 */
public record Vec3(double x, double y, double z) {

    public @NotNull Vec3 add(double dx, double dy, double dz) {
        return new Vec3(x + dx, y + dy, z + dz);
    }

    public double distanceSquared(@NotNull Vec3 other) {
        double a = x - other.x;
        double b = y - other.y;
        double c = z - other.z;
        return a * a + b * b + c * c;
    }

    public double horizontalDistance(@NotNull Vec3 other) {
        double a = x - other.x;
        double c = z - other.z;
        return Math.sqrt(a * a + c * c);
    }
}
