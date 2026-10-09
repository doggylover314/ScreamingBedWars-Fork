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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A set of players that must end up in the same team (a party, or a single player).
 *
 * @param id            stable identifier ("party:&lt;leader&gt;" or "solo:&lt;uuid&gt;")
 * @param members       the members in join order
 * @param preferredTeam name of the team the group already sits in (classic games), may be null
 * @param fragment      whether this group is a piece of a bigger group that had to be split
 */
public record PlayerGroup(@NotNull String id, @NotNull List<UUID> members, @Nullable String preferredTeam, boolean fragment) {
    public PlayerGroup {
        members = List.copyOf(members);
        if (members.isEmpty()) {
            throw new IllegalArgumentException("group without members");
        }
    }

    public int size() {
        return members.size();
    }

    public static @NotNull PlayerGroup single(@NotNull UUID player) {
        return new PlayerGroup("solo:" + player, List.of(player), null, false);
    }

    /**
     * Splits an oversized group into chunks of at most maxSize members (order kept), ids "&lt;id&gt;#1", "&lt;id&gt;#2", ...
     * marked as fragments.
     */
    public @NotNull List<PlayerGroup> splitInto(int maxSize) {
        var result = new ArrayList<PlayerGroup>();
        for (int from = 0, part = 1; from < members.size(); from += maxSize, part++) {
            result.add(new PlayerGroup(id + "#" + part, members.subList(from, Math.min(members.size(), from + maxSize)), preferredTeam, true));
        }
        return result;
    }
}
