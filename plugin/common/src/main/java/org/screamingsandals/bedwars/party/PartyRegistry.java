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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.screamingsandals.bedwars.party.PartyError.*;
import static org.screamingsandals.bedwars.party.PartyEventType.*;

/**
 * The party state machine (PURE: the clock and the "is online" predicate are parameters, results are plain objects).
 * Not thread-safe: the owning service calls it on the server main thread only.
 */
public final class PartyRegistry {
    private final Map<UUID, PartyImpl> partiesById = new LinkedHashMap<>();
    private final Map<UUID, PartyImpl> partyByMember = new HashMap<>();
    private final Supplier<UUID> idSupplier;

    public PartyRegistry() {
        this(UUID::randomUUID);
    }

    PartyRegistry(@NotNull Supplier<UUID> idSupplier) {
        this.idSupplier = idSupplier;
    }

    // ---------------------------------------------------------------------------------------------------------
    // queries
    // ---------------------------------------------------------------------------------------------------------

    public @NotNull Optional<PartyImpl> getParty(@NotNull UUID player) {
        return Optional.ofNullable(partyByMember.get(player));
    }

    public @NotNull Optional<PartyImpl> getPartyById(@NotNull UUID id) {
        return Optional.ofNullable(partiesById.get(id));
    }

    public @NotNull Collection<PartyImpl> getParties() {
        return List.copyOf(partiesById.values());
    }

    /**
     * Pending, non-expired invites of the player; newest first by creation time, ties by insertion order reversed.
     */
    public @NotNull List<PartyInvite> getInvitesFor(@NotNull UUID target, long now) {
        var list = new ArrayList<PartyInvite>();
        for (var party : partiesById.values()) {
            party.getInvite(target).filter(i -> !i.isExpired(now)).ifPresent(list::add);
        }
        Collections.reverse(list);
        list.sort(Comparator.comparingLong(PartyInvite::createdAt).reversed()); // stable: ties keep the reversed order
        return list;
    }

    public void clear() {
        partiesById.clear();
        partyByMember.clear();
    }

    // ---------------------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------------------

    /** Adds an event only if it has recipients. */
    private void emit(List<PartyEvent> out, PartyEventType type, PartyImpl p, @Nullable UUID subject, @Nullable String subjectName,
                      @Nullable UUID other, @Nullable String otherName, Collection<UUID> recipients) {
        if (recipients.isEmpty()) {
            return;
        }
        out.add(new PartyEvent(type, p.getId(), p.getLeader(), p.getLeaderName(), subject, subjectName, other, otherName, List.copyOf(recipients)));
    }

    /** Drops the party without events. */
    private void removeParty(PartyImpl p) {
        for (var m : p.getMembers()) {
            partyByMember.remove(m);
        }
        partiesById.remove(p.getId());
    }

    /** Size {@code <= 1} and no invites -> disband (Hypixel: "disbanded because all invites expired and the party was empty"). */
    private void disbandIfEmpty(PartyImpl p, List<PartyEvent> out) {
        if (!partiesById.containsKey(p.getId())) {
            return;
        }
        if (p.size() <= 1 && p.getInvites().isEmpty()) {
            emit(out, DISBANDED_EMPTY, p, p.getLeader(), p.getLeaderName(), null, null, p.getMembers());
            removeParty(p);
        }
    }

    private @Nullable PartyImpl aliveOrNull(PartyImpl p) {
        return partiesById.containsKey(p.getId()) ? p : null;
    }

    private @Nullable PartyInvite pickInvite(UUID target, @Nullable String fromName, long now) {
        var list = getInvitesFor(target, now); // newest first
        if (fromName == null || fromName.isBlank()) {
            return list.isEmpty() ? null : list.get(0);
        }
        var wanted = fromName.trim();
        for (var invite : list) {
            var party = partiesById.get(invite.partyId());
            if (invite.inviterName().equalsIgnoreCase(wanted) || (party != null && party.getLeaderName().equalsIgnoreCase(wanted))) {
                return invite;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------
    // operations
    // ---------------------------------------------------------------------------------------------------------

    /**
     * @param maxSize    size limit, {@code <= 0} = unlimited
     * @param expireMillis invite lifetime
     */
    public @NotNull PartyResult invite(@NotNull UUID inviter, @NotNull String inviterName, @NotNull UUID target, @NotNull String targetName,
                                       int maxSize, boolean membersCanInvite, long now, long expireMillis) {
        if (inviter.equals(target)) {
            return PartyResult.fail(CANNOT_INVITE_SELF);
        }
        var party = partyByMember.get(inviter);
        if (party != null && !party.isLeader(inviter) && !membersCanInvite) {
            return PartyResult.fail(NOT_LEADER);
        }
        if (party != null && party.isMember(target)) {
            return PartyResult.fail(ALREADY_MEMBER);
        }
        if (partyByMember.containsKey(target)) {
            return PartyResult.fail(TARGET_IN_PARTY);
        }
        if (party != null && party.getInvite(target).filter(i -> !i.isExpired(now)).isPresent()) {
            return PartyResult.fail(ALREADY_INVITED);
        }
        int currentSize = party == null ? 1 : party.size();
        if (maxSize > 0 && currentSize >= maxSize) {
            return PartyResult.fail(PARTY_FULL);
        }
        var out = new ArrayList<PartyEvent>();
        if (party == null) { // create AFTER all checks
            party = new PartyImpl(idSupplier.get(), inviter, inviterName, now);
            partiesById.put(party.getId(), party);
            partyByMember.put(inviter, party);
        }
        party.removeInvite(target); // replaces an expired, not yet swept invite
        party.putInvite(new PartyInvite(party.getId(), inviter, inviterName, target, targetName, now, now + expireMillis, maxSize));
        emit(out, INVITE_SENT, party, target, targetName, inviter, inviterName, List.of(inviter));
        emit(out, INVITE_SENT_OTHERS, party, target, targetName, inviter, inviterName, party.getMembersExcept(inviter));
        emit(out, INVITE_RECEIVED, party, target, targetName, inviter, inviterName, List.of(target));
        return PartyResult.ok(party, out);
    }

    /**
     * @param fromName optional inviter or leader name; null / blank = the newest invite
     */
    public @NotNull PartyResult accept(@NotNull UUID target, @NotNull String targetName, @Nullable String fromName, long now) {
        if (partyByMember.containsKey(target)) {
            return PartyResult.fail(ALREADY_IN_PARTY);
        }
        var invite = pickInvite(target, fromName, now);
        var missing = fromName == null || fromName.isBlank() ? NO_INVITE : NO_INVITE_FROM;
        if (invite == null) {
            return PartyResult.fail(missing);
        }
        var party = partiesById.get(invite.partyId());
        if (party == null) {
            return PartyResult.fail(missing);
        }
        if (invite.maxSize() > 0 && party.size() >= invite.maxSize()) {
            return PartyResult.fail(PARTY_FULL); // invite kept (someone may leave)
        }
        var out = new ArrayList<PartyEvent>();
        // drop every other invite of this player; those parties may become empty
        for (var other : new ArrayList<>(partiesById.values())) {
            if (other.removeInvite(target) != null && other != party) {
                disbandIfEmpty(other, out);
            }
        }
        party.addMember(target, targetName);
        partyByMember.put(target, party);
        emit(out, MEMBER_JOINED, party, target, targetName, null, null, party.getMembersExcept(target));
        emit(out, MEMBER_JOINED_SELF, party, target, targetName, null, null, List.of(target));
        return PartyResult.ok(party, out);
    }

    public @NotNull PartyResult deny(@NotNull UUID target, @Nullable String fromName, long now) {
        var invite = pickInvite(target, fromName, now);
        if (invite == null) {
            return PartyResult.fail(fromName == null || fromName.isBlank() ? NO_INVITE : NO_INVITE_FROM);
        }
        var party = partiesById.get(invite.partyId());
        if (party == null) {
            return PartyResult.fail(NO_INVITE);
        }
        party.removeInvite(target);
        var out = new ArrayList<PartyEvent>();
        var notify = party.isMember(invite.inviter()) ? invite.inviter() : party.getLeader();
        emit(out, INVITE_DENIED_INVITER, party, target, invite.targetName(), invite.inviter(), invite.inviterName(), List.of(notify));
        emit(out, INVITE_DENIED_TARGET, party, target, invite.targetName(), invite.inviter(), invite.inviterName(), List.of(target));
        disbandIfEmpty(party, out);
        return PartyResult.ok(aliveOrNull(party), out);
    }

    /**
     * @param online a leader who leaves hands the party to the first ONLINE remaining member (join order)
     */
    public @NotNull PartyResult leave(@NotNull UUID player, @NotNull Predicate<UUID> online) {
        var party = partyByMember.get(player);
        if (party == null) {
            return PartyResult.fail(NOT_IN_PARTY);
        }
        var name = party.getName(player);
        var remaining = party.getMembersExcept(player);
        var out = new ArrayList<PartyEvent>();
        // every emit happens while PartyImpl#getLeaderName() is still valid (the leader is never removed before setLeader)
        emit(out, MEMBER_LEFT_SELF, party, player, name, null, null, List.of(player));
        if (party.isLeader(player)) {
            if (remaining.isEmpty()) {
                removeParty(party);
                return PartyResult.ok(null, out);
            }
            var successor = remaining.stream().filter(online).findFirst();
            if (successor.isEmpty()) {
                emit(out, DISBANDED_LEADER_LEFT, party, player, name, null, null, remaining);
                removeParty(party); // also unmaps the leaver
                return PartyResult.ok(null, out);
            }
            party.setLeader(successor.get()); // BEFORE removing the old leader
            party.removeMember(player);
            partyByMember.remove(player);
            emit(out, MEMBER_LEFT, party, player, name, null, null, remaining);
            emit(out, LEADER_CHANGED, party, successor.get(), party.getName(successor.get()), player, name, remaining);
        } else {
            party.removeMember(player);
            partyByMember.remove(player);
            emit(out, MEMBER_LEFT, party, player, name, null, null, remaining);
        }
        disbandIfEmpty(party, out);
        return PartyResult.ok(aliveOrNull(party), out);
    }

    /**
     * @param targetName name typed by the leader; offline members can be kicked
     */
    public @NotNull PartyResult kick(@NotNull UUID actor, @NotNull String targetName) {
        var party = partyByMember.get(actor);
        if (party == null) {
            return PartyResult.fail(NOT_IN_PARTY);
        }
        if (!party.isLeader(actor)) {
            return PartyResult.fail(NOT_LEADER);
        }
        var found = party.findMemberByName(targetName);
        if (found.isEmpty()) {
            return PartyResult.fail(NOT_A_MEMBER);
        }
        if (found.get().equals(actor)) {
            return PartyResult.fail(CANNOT_KICK_SELF);
        }
        var target = found.get();
        var tName = party.getName(target);
        var aName = party.getName(actor);
        var out = new ArrayList<PartyEvent>();
        emit(out, MEMBER_KICKED_SELF, party, target, tName, actor, aName, List.of(target));
        party.removeMember(target);
        partyByMember.remove(target);
        emit(out, MEMBER_KICKED, party, target, tName, actor, aName, party.getMembers());
        disbandIfEmpty(party, out);
        return PartyResult.ok(aliveOrNull(party), out);
    }

    public @NotNull PartyResult disband(@NotNull UUID actor) {
        var party = partyByMember.get(actor);
        if (party == null) {
            return PartyResult.fail(NOT_IN_PARTY);
        }
        if (!party.isLeader(actor)) {
            return PartyResult.fail(NOT_LEADER);
        }
        var out = new ArrayList<PartyEvent>();
        emit(out, DISBANDED_BY_LEADER, party, actor, party.getName(actor), null, null, party.getMembers());
        removeParty(party); // invites vanish with the party
        return PartyResult.ok(null, out);
    }

    /**
     * @param targetName must be an ONLINE member
     */
    public @NotNull PartyResult transfer(@NotNull UUID actor, @NotNull String targetName, @NotNull Predicate<UUID> online) {
        var party = partyByMember.get(actor);
        if (party == null) {
            return PartyResult.fail(NOT_IN_PARTY);
        }
        if (!party.isLeader(actor)) {
            return PartyResult.fail(NOT_LEADER);
        }
        var found = party.findMemberByName(targetName);
        if (found.isEmpty()) {
            return PartyResult.fail(NOT_A_MEMBER);
        }
        if (found.get().equals(actor)) {
            return PartyResult.fail(CANNOT_TRANSFER_SELF);
        }
        if (!online.test(found.get())) {
            return PartyResult.fail(TARGET_OFFLINE);
        }
        party.setLeader(found.get());
        var out = new ArrayList<PartyEvent>();
        emit(out, LEADER_TRANSFERRED, party, found.get(), party.getName(found.get()), actor, party.getName(actor), party.getMembers());
        return PartyResult.ok(party, out);
    }

    public @NotNull PartyResult markOffline(@NotNull UUID player, long now) {
        var party = partyByMember.get(player);
        if (party == null || party.getOfflineSince(player).isPresent()) { // not in a party, or already marked: keep the first timestamp
            return PartyResult.ok(party, List.of());
        }
        party.markOffline(player, now);
        var out = new ArrayList<PartyEvent>();
        emit(out, party.isLeader(player) ? LEADER_DISCONNECTED : MEMBER_DISCONNECTED, party, player, party.getName(player), null, null, party.getMembersExcept(player));
        return PartyResult.ok(party, out);
    }

    public @NotNull PartyResult markOnline(@NotNull UUID player, @NotNull String currentName) {
        var party = partyByMember.get(player);
        if (party == null) {
            return PartyResult.ok(null, List.of());
        }
        party.updateName(player, currentName); // names change between sessions
        var out = new ArrayList<PartyEvent>();
        if (party.markOnline(player)) {
            emit(out, MEMBER_RECONNECTED, party, player, currentName, null, null, party.getMembersExcept(player));
        }
        return PartyResult.ok(party, out);
    }

    /**
     * Called once per second by the service; the order of the steps per party matters.
     */
    public @NotNull PartyResult tick(long now, @NotNull Predicate<UUID> online, @NotNull PartyRules rules) {
        var out = new ArrayList<PartyEvent>();
        for (var party : new ArrayList<>(partiesById.values())) {
            // 1. invite expiry
            for (var invite : party.getInvites()) {
                if (!invite.isExpired(now)) {
                    continue;
                }
                party.removeInvite(invite.target());
                var notify = party.isMember(invite.inviter()) ? invite.inviter() : party.getLeader();
                emit(out, INVITE_EXPIRED_INVITER, party, invite.target(), invite.targetName(), invite.inviter(), invite.inviterName(), List.of(notify));
                emit(out, INVITE_EXPIRED_TARGET, party, invite.target(), invite.targetName(), invite.inviter(), invite.inviterName(), List.of(invite.target()));
            }
            // 2. leader offline too long
            var leader = party.getLeader();
            OptionalLong leaderOffline = party.getOfflineSince(leader);
            if (leaderOffline.isPresent() && rules.leaderGraceMillis() >= 0 && now - leaderOffline.getAsLong() >= rules.leaderGraceMillis()) {
                var oldName = party.getName(leader);
                var successor = party.getMembersExcept(leader).stream().filter(online).findFirst();
                if (rules.leaderOfflineAction() == LeaderOfflineAction.TRANSFER && successor.isPresent()) {
                    party.setLeader(successor.get()); // the old leader stays as (offline) member
                    emit(out, LEADER_TRANSFERRED_OFFLINE, party, successor.get(), party.getName(successor.get()), leader, oldName, party.getMembers());
                } else {
                    emit(out, DISBANDED_LEADER_OFFLINE, party, leader, oldName, null, null, party.getMembers());
                    removeParty(party);
                    continue;
                }
            }
            // 3. members offline too long (never the current leader)
            if (rules.memberOfflineMillis() >= 0) {
                for (var member : party.getMembersExcept(party.getLeader())) {
                    OptionalLong offline = party.getOfflineSince(member);
                    if (offline.isPresent() && now - offline.getAsLong() >= rules.memberOfflineMillis()) {
                        var memberName = party.getName(member);
                        party.removeMember(member);
                        partyByMember.remove(member);
                        emit(out, MEMBER_REMOVED_OFFLINE, party, member, memberName, null, null, party.getMembers());
                    }
                }
            }
            // 4. empty party
            disbandIfEmpty(party, out);
        }
        return PartyResult.ok(null, out);
    }
}
