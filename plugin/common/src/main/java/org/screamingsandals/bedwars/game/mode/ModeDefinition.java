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

import java.util.List;

/**
 * A game mode (for example 4v4v4v4): how many teams play, how many players each team has and which arenas may host it.
 *
 * @param id          unique mode identifier
 * @param displayName MiniMessage display name
 * @param teamCount   number of playing teams
 * @param teamSize    players per team
 * @param minPlayers  minimal number of players to start (already clamped by the parser)
 * @param arenas      whitelist of arena names; empty = every arena
 */
public record ModeDefinition(@NotNull String id, @NotNull String displayName, int teamCount, int teamSize,
                             int minPlayers, @NotNull List<String> arenas) {
    public ModeDefinition {
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (teamCount < 1 || teamSize < 1) {
            throw new IllegalArgumentException("teamCount/teamSize must be >= 1");
        }
        arenas = List.copyOf(arenas);
    }

    public int maxPlayers() {
        return teamCount * teamSize;
    }

    /**
     * Empty whitelist = every arena. Case-insensitive.
     */
    public boolean allowsArena(@NotNull String arenaName) {
        return arenas.isEmpty() || arenas.stream().anyMatch(a -> a.equalsIgnoreCase(arenaName));
    }
}
