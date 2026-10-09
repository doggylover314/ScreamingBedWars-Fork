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

import java.util.ArrayList;
import java.util.List;

/**
 * Spreads the spawn points of the dragons on a ring.
 */
public final class DragonSpawnPlanner {

    private DragonSpawnPlanner() {
    }

    /**
     * Returns {@code count} points on a horizontal ring around {@code centre} (angle 0 = +X, counter-clockwise in the
     * XZ plane), each clamped into {@code box}.
     */
    public static @NotNull List<Vec3> plan(@NotNull Vec3 centre, int count, double radius, @NotNull FlightBox box) {
        if (count <= 0) {
            return List.of();
        }
        var list = new ArrayList<Vec3>(count);
        for (int i = 0; i < count; i++) {
            if (count == 1 || radius <= 0) {
                list.add(box.clamp(centre));
                continue;
            }
            double angle = 2 * Math.PI * i / count;
            list.add(box.clamp(new Vec3(centre.x() + Math.cos(angle) * radius, centre.y(), centre.z() + Math.sin(angle) * radius)));
        }
        return List.copyOf(list);
    }
}
