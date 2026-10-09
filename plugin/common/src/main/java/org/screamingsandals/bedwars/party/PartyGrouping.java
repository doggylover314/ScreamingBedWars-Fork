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

package org.screamingsandals.bedwars.party;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Helper for team assignment: keeps the members of one party together (PURE).
 */
public final class PartyGrouping {
    private PartyGrouping() {
    }

    /** Key of a player without a party. */
    private record Solo(UUID uuid) {
    }

    /**
     * Groups players that share a party id; order = first appearance; players without a party are singleton groups.
     */
    public static @NotNull List<List<UUID>> group(@NotNull List<UUID> players, @NotNull Function<UUID, Optional<UUID>> partyIdOf) {
        var groups = new LinkedHashMap<Object, List<UUID>>();
        for (var player : players) {
            Object key = partyIdOf.apply(player).<Object>map(id -> id).orElseGet(() -> new Solo(player));
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(player);
        }
        var result = new ArrayList<List<UUID>>(groups.size());
        groups.values().forEach(g -> result.add(List.copyOf(g)));
        return result;
    }
}
