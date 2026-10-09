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

package org.screamingsandals.bedwars.game.upgrade.builtin;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;

/**
 * Parsed form of a {@code dragon-buff} upgrade. Pure value class.
 */
public record DragonBuffSpec(int maxLevel, int extraDragonsPerLevel) {

    public static @NotNull DragonBuffSpec parse(@NotNull ConfigurationNode node, @NotNull List<String> warnings) {
        int max = node.node("max-level").getInt(1);
        if (max < 1) {
            warnings.add("max-level must be >= 1");
            max = 1;
        }
        int per = node.node("extra-dragons-per-level").getInt(1);
        if (per < 0) {
            warnings.add("extra-dragons-per-level must be >= 0");
            per = 0;
        }
        return new DragonBuffSpec(max, per);
    }

    public int extraDragonsFor(int level) {
        return level <= 0 ? 0 : Math.min(level, maxLevel) * extraDragonsPerLevel;
    }
}
