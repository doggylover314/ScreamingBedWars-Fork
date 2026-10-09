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

package org.screamingsandals.bedwars.game.mode;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure ordering of arena teams: configured priority first.
 */
public final class TeamOrder {
    private TeamOrder() {
    }

    /**
     * Priority names first (case-insensitive, unknown/duplicate ignored), then remaining arena teams in arena order.
     */
    public static @NotNull List<String> order(@NotNull List<String> arenaTeams, @NotNull List<String> priority) {
        var result = new ArrayList<String>();
        for (var wanted : priority) {
            if (wanted == null) {
                continue;
            }
            arenaTeams.stream()
                    .filter(t -> t.equalsIgnoreCase(wanted.trim()))
                    .findFirst()
                    .filter(t -> !result.contains(t))
                    .ifPresent(result::add);
        }
        for (var team : arenaTeams) {
            if (!result.contains(team)) {
                result.add(team);
            }
        }
        return result;
    }
}
