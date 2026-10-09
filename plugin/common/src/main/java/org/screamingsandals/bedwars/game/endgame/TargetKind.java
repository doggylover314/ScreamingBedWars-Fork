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

package org.screamingsandals.bedwars.game.endgame;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Kind of a destroyed team target, used to pick the wording of the bed destruction announcement.
 */
public enum TargetKind {
    BED,
    ANCHOR,
    CAKE,
    DOOR,
    OTHER,
    NON_BLOCK;

    /**
     * Kind used for the bed destruction announcement.
     * <ul>
     *     <li>empty: {@link #BED} (nothing was destroyed right now: keep the classic wording)</li>
     *     <li>all entries the same block kind: that kind</li>
     *     <li>anything mixed or containing {@link #NON_BLOCK}: {@link #OTHER}</li>
     * </ul>
     */
    public static @NotNull TargetKind aggregate(@NotNull Collection<@NotNull TargetKind> kinds) {
        if (kinds.isEmpty()) {
            return BED;
        }
        var first = kinds.iterator().next();
        if (first == NON_BLOCK) {
            return OTHER;
        }
        for (var kind : kinds) {
            if (kind != first) {
                return OTHER;
            }
        }
        return first;
    }
}
