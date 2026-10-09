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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * FOUNDATION-SKELETON: replaced by package P5 (PLAN.md).
 */
public class PartyImpl {
    private final @NotNull UUID id;
    private final long createdAt;
    private @NotNull UUID leader;
    private final LinkedHashMap<UUID, String> members = new LinkedHashMap<>();

    PartyImpl(@NotNull UUID id, @NotNull UUID leader, @NotNull String leaderName, long createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.leader = leader;
        this.members.put(leader, leaderName);
    }

    public @NotNull UUID getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public @NotNull UUID getLeader() {
        return leader;
    }

    public @NotNull String getLeaderName() {
        return members.getOrDefault(leader, "?");
    }

    /**
     * Unmodifiable, in join order, the leader is included.
     */
    public @NotNull List<@NotNull UUID> getMembers() {
        return Collections.unmodifiableList(new ArrayList<>(members.keySet()));
    }

    public boolean isLeader(@NotNull UUID player) {
        return leader.equals(player);
    }

    public boolean isMember(@NotNull UUID player) {
        return members.containsKey(player);
    }

    public int size() {
        return members.size();
    }
}
