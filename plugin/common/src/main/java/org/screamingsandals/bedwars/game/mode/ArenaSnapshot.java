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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Immutable view of an arena used by the pure {@link ModeArenaSelector}.
 *
 * @param groupSizes sizes of the player groups (parties / singles) of a WAITING mode lobby; empty otherwise or when not requested
 */
public record ArenaSnapshot(@NotNull String id, @NotNull String name, @NotNull ArenaState state, int players,
                            @Nullable String activeModeId, int teamCount,
                            @NotNull Set<Integer> allowedTeamSizes, @NotNull Set<Integer> preferredTeamSizes,
                            @NotNull List<Integer> groupSizes) {
    public ArenaSnapshot {
        allowedTeamSizes = Set.copyOf(allowedTeamSizes);
        preferredTeamSizes = Set.copyOf(preferredTeamSizes);
        groupSizes = List.copyOf(groupSizes);
    }
}
