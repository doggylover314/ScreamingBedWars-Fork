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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A party (pure data, main thread only). Read-only for everybody except the {@link PartyRegistry}, which is the only
 * caller of the package-private mutators.
 */
public class PartyImpl {
    private final @NotNull UUID id;
    private final long createdAt;
    private @NotNull UUID leader;
    /** join order -> last known name; the leader is included */
    private final LinkedHashMap<UUID, String> members = new LinkedHashMap<>();
    /** target -> invite (insertion order) */
    private final LinkedHashMap<UUID, PartyInvite> invites = new LinkedHashMap<>();
    /** member -> millis */
    private final Map<UUID, Long> offlineSince = new HashMap<>();

    PartyImpl(@NotNull UUID id, @NotNull UUID leader, @NotNull String leaderName, long createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.leader = leader;
        this.members.put(leader, leaderName);
    }

    /**
     * Random; stable for the lifetime of the party. Use it as a grouping key.
     */
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
        return List.copyOf(members.keySet());
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

    /**
     * Last known name of the member.
     */
    public @Nullable String getName(@NotNull UUID member) {
        return members.get(member);
    }

    /**
     * {@code equalsIgnoreCase} over the stored names.
     */
    public @NotNull Optional<UUID> findMemberByName(@NotNull String name) {
        var wanted = name.trim();
        for (var entry : members.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(wanted)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /**
     * Pending invites in insertion order (may contain expired ones that were not swept yet).
     */
    public @NotNull List<@NotNull PartyInvite> getInvites() {
        return List.copyOf(invites.values());
    }

    public @NotNull Optional<PartyInvite> getInvite(@NotNull UUID target) {
        return Optional.ofNullable(invites.get(target));
    }

    public boolean hasInvite(@NotNull UUID target) {
        return invites.containsKey(target);
    }

    public @NotNull OptionalLong getOfflineSince(@NotNull UUID member) {
        var since = offlineSince.get(member);
        return since == null ? OptionalLong.empty() : OptionalLong.of(since);
    }

    public @NotNull List<@NotNull UUID> getMembersExcept(@NotNull UUID player) {
        return members.keySet().stream().filter(u -> !u.equals(player)).collect(Collectors.toUnmodifiableList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // package-private mutators (PartyRegistry only)
    // ---------------------------------------------------------------------------------------------------------

    void setLeader(@NotNull UUID newLeader) {
        if (!members.containsKey(newLeader)) {
            throw new IllegalArgumentException("The new leader " + newLeader + " is not a member of " + this);
        }
        this.leader = newLeader;
    }

    void addMember(@NotNull UUID player, @NotNull String name) {
        members.put(player, name);
    }

    void removeMember(@NotNull UUID player) {
        if (leader.equals(player)) {
            throw new IllegalStateException("The current leader cannot be removed (setLeader first or drop the party): " + this);
        }
        members.remove(player);
        offlineSince.remove(player);
    }

    void updateName(@NotNull UUID player, @NotNull String name) {
        if (members.containsKey(player)) {
            members.put(player, name);
        }
    }

    void putInvite(@NotNull PartyInvite invite) {
        invites.put(invite.target(), invite);
    }

    @Nullable PartyInvite removeInvite(@NotNull UUID target) {
        return invites.remove(target);
    }

    void markOffline(@NotNull UUID player, long now) {
        if (members.containsKey(player)) {
            offlineSince.put(player, now);
        }
    }

    /**
     * @return true if the member was marked offline before
     */
    boolean markOnline(@NotNull UUID player) {
        return offlineSince.remove(player) != null;
    }

    @Override
    public String toString() {
        return "Party[" + id + ", leader=" + getLeaderName() + ", members=" + members.values() + "]";
    }
}
