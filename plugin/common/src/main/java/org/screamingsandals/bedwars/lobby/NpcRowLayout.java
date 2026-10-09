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

package org.screamingsandals.bedwars.lobby;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry helper for {@code /bw npc spawnmodes}: places NPCs in a row next to the admin.
 */
public final class NpcRowLayout {
    private NpcRowLayout() {
    }

    /**
     * Horizontal offset of one NPC relative to the first one.
     */
    public record Offset(double x, double z) {
    }

    /**
     * Offsets of {@code count} NPCs placed to the RIGHT of a player looking at {@code yawDegrees}
     * (Minecraft yaw: 0 = +Z/south, 90 = -X/west). The first offset is always (0, 0).
     */
    public static @NotNull List<Offset> rowOffsets(int count, double spacing, float yawDegrees) {
        double rad = Math.toRadians(yawDegrees);
        double rightX = -Math.cos(rad);
        double rightZ = -Math.sin(rad);
        var list = new ArrayList<Offset>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            // "+ 0.0" turns a negative zero into a positive one
            list.add(new Offset(rightX * spacing * i + 0.0, rightZ * spacing * i + 0.0));
        }
        return list;
    }
}
