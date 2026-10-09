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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.screamingsandals.bedwars.party.LeaderOfflineAction.DISBAND;
import static org.screamingsandals.bedwars.party.LeaderOfflineAction.TRANSFER;
import static org.screamingsandals.bedwars.party.PartyError.*;
import static org.screamingsandals.bedwars.party.PartyEventType.*;

class PartyRegistryTest {
    static final UUID A = new UUID(0, 1);
    static final UUID B = new UUID(0, 2);
    static final UUID C = new UUID(0, 3);
    static final UUID D = new UUID(0, 4);
    static final UUID E = new UUID(0, 5);
    static final String NA = "Alice";
    static final String NB = "Bob";
    static final String NC = "Carol";
    static final String ND = "Dave";
    static final String NE = "Eve";

    private long seq;
    private PartyRegistry reg;
    private final Set<UUID> onlineSet = new HashSet<>();
    private final Predicate<UUID> online = onlineSet::contains;
    private final PartyRules rules = new PartyRules(4, false, 60_000, 300_000, TRANSFER, 300_000);

    @BeforeEach
    void setUp() {
        seq = 0;
        reg = new PartyRegistry(() -> new UUID(9, seq++));
        onlineSet.clear();
        onlineSet.addAll(List.of(A, B, C, D, E));
    }

    // ---- helpers ----

    private PartyResult invite(UUID from, String fromName, UUID to, String toName, long now) {
        return reg.invite(from, fromName, to, toName, 4, false, now, 60_000);
    }

    /** A leads a party with B (accepted at t=1). */
    private void partyAB() {
        assertTrue(invite(A, NA, B, NB, 0).isSuccess());
        assertTrue(reg.accept(B, NB, null, 1).isSuccess());
    }

    /** A leads a party with B and C. */
    private void partyABC() {
        partyAB();
        assertTrue(invite(A, NA, C, NC, 2).isSuccess());
        assertTrue(reg.accept(C, NC, null, 3).isSuccess());
    }

    private static List<PartyEventType> types(PartyResult result) {
        return result.events().stream().map(PartyEvent::type).collect(Collectors.toList());
    }

    private static List<UUID> recipients(PartyResult result, PartyEventType type) {
        return result.events().stream().filter(e -> e.type() == type).findFirst().orElseThrow().recipients();
    }

    private static PartyEvent event(PartyResult result, PartyEventType type) {
        return result.events().stream().filter(e -> e.type() == type).findFirst().orElseThrow();
    }

    // ---- invite ----

    @Test
    void invite_createsPartyWithLeaderOnly() {
        var result = invite(A, NA, B, NB, 0);
        assertTrue(result.isSuccess());
        assertTrue(reg.getParty(A).isPresent());
        assertEquals(A, reg.getParty(A).get().getLeader());
        assertEquals(List.of(A), reg.getParty(A).get().getMembers());
        assertTrue(reg.getParty(B).isEmpty());
        assertEquals(60_000, reg.getParty(A).get().getInvite(B).orElseThrow().expiresAt());
        assertEquals(List.of(INVITE_SENT, INVITE_RECEIVED), types(result)); // no INVITE_SENT_OTHERS: no other members
        assertEquals(List.of(A), recipients(result, INVITE_SENT));
        assertEquals(List.of(B), recipients(result, INVITE_RECEIVED));
    }

    @Test
    void invite_self() {
        var result = invite(A, NA, A, NA, 0);
        assertEquals(CANNOT_INVITE_SELF, result.error());
        assertTrue(reg.getParties().isEmpty());
    }

    @Test
    void invite_twice() {
        assertTrue(invite(A, NA, B, NB, 0).isSuccess());
        assertEquals(ALREADY_INVITED, invite(A, NA, B, NB, 1_000).error());
        reg.tick(60_000, online, rules); // the invite expires, the leader-only party is disbanded
        assertTrue(reg.getParties().isEmpty());
        assertTrue(invite(A, NA, B, NB, 60_000).isSuccess());
        assertTrue(reg.getParty(A).isPresent());
    }

    @Test
    void invite_expiredButNotSweptIsReplaced() {
        assertTrue(invite(A, NA, B, NB, 0).isSuccess());
        var second = invite(A, NA, B, NB, 70_000);
        assertTrue(second.isSuccess());
        assertEquals(130_000, reg.getParty(A).get().getInvite(B).orElseThrow().expiresAt());
        assertEquals(1, reg.getParty(A).get().getInvites().size());
    }

    @Test
    void invite_targetInOtherParty() {
        partyAB();
        var result = invite(C, NC, B, NB, 10);
        assertEquals(TARGET_IN_PARTY, result.error());
        assertTrue(reg.getParty(C).isEmpty());
    }

    @Test
    void invite_alreadyMember() {
        partyAB();
        assertEquals(ALREADY_MEMBER, invite(A, NA, B, NB, 10).error());
    }

    @Test
    void invite_byMember() {
        partyAB();
        assertEquals(NOT_LEADER, reg.invite(B, NB, C, NC, 4, false, 10, 60_000).error());
        var allowed = reg.invite(B, NB, C, NC, 4, true, 10, 60_000);
        assertTrue(allowed.isSuccess());
        assertEquals(List.of(A), recipients(allowed, INVITE_SENT_OTHERS));
        assertEquals(List.of(B), recipients(allowed, INVITE_SENT));
    }

    @Test
    void invite_full() {
        assertTrue(reg.invite(A, NA, B, NB, 2, false, 0, 60_000).isSuccess());
        assertTrue(reg.accept(B, NB, null, 1).isSuccess());
        assertEquals(PARTY_FULL, reg.invite(A, NA, C, NC, 2, false, 2, 60_000).error());
        assertTrue(reg.getParty(C).isEmpty());

        var unlimited = new PartyRegistry(() -> new UUID(9, seq++));
        var members = List.of(B, C, D, E);
        var names = List.of(NB, NC, ND, NE);
        for (int i = 0; i < members.size(); i++) {
            assertTrue(unlimited.invite(A, NA, members.get(i), names.get(i), 0, false, i, 60_000).isSuccess());
            assertTrue(unlimited.accept(members.get(i), names.get(i), null, i).isSuccess());
        }
        assertEquals(5, unlimited.getParty(A).orElseThrow().size());
        var sixth = unlimited.invite(A, NA, new UUID(0, 6), "Frank", 0, false, 10, 60_000);
        assertTrue(sixth.isSuccess());
    }

    // ---- accept ----

    @Test
    void accept_joins() {
        assertTrue(invite(A, NA, B, NB, 0).isSuccess());
        var result = reg.accept(B, NB, null, 1_000);
        assertTrue(result.isSuccess());
        var party = reg.getParty(A).orElseThrow();
        assertEquals(List.of(A, B), party.getMembers());
        assertSame(party, reg.getParty(B).orElseThrow());
        assertTrue(party.getInvites().isEmpty());
        assertEquals(List.of(A), recipients(result, MEMBER_JOINED));
        assertEquals(List.of(B), recipients(result, MEMBER_JOINED_SELF));
    }

    @Test
    void accept_boundary() {
        assertTrue(invite(A, NA, B, NB, 0).isSuccess());
        assertTrue(reg.accept(B, NB, null, 59_999).isSuccess());

        var other = new PartyRegistry(() -> new UUID(9, seq++));
        assertTrue(other.invite(A, NA, B, NB, 4, false, 0, 60_000).isSuccess());
        assertEquals(NO_INVITE, other.accept(B, NB, null, 60_000).error());
    }

    @Test
    void accept_fullAtAcceptTime() {
        assertTrue(reg.invite(A, NA, B, NB, 2, false, 0, 60_000).isSuccess());
        assertTrue(reg.invite(A, NA, C, NC, 2, false, 0, 60_000).isSuccess());
        assertTrue(reg.accept(B, NB, null, 1).isSuccess());
        assertEquals(PARTY_FULL, reg.accept(C, NC, null, 2).error());
        assertEquals(1, reg.getInvitesFor(C, 2).size()); // the invite is kept
    }

    @Test
    void accept_newestWins() {
        assertTrue(invite(A, NA, D, ND, 0).isSuccess());
        assertTrue(invite(C, NC, D, ND, 10).isSuccess());
        var result = reg.accept(D, ND, null, 20);
        assertTrue(result.isSuccess());
        assertSame(reg.getParty(C).orElseThrow(), reg.getParty(D).orElseThrow());
        assertEquals(List.of(C, D), reg.getParty(D).get().getMembers());
        assertEquals(List.of(A), recipients(result, DISBANDED_EMPTY));
        assertTrue(reg.getParty(A).isEmpty());
    }

    @Test
    void accept_byName() {
        assertTrue(invite(A, NA, D, ND, 0).isSuccess());
        assertTrue(invite(C, NC, D, ND, 10).isSuccess());
        var result = reg.accept(D, "dave", "ALICE", 20);
        assertTrue(result.isSuccess());
        assertSame(reg.getParty(A).orElseThrow(), reg.getParty(D).orElseThrow());
        assertEquals(List.of(C), recipients(result, DISBANDED_EMPTY));
        assertTrue(reg.getParty(C).isEmpty());
    }

    @Test
    void accept_byLeaderName() {
        partyAB();
        assertTrue(reg.invite(B, NB, D, ND, 4, true, 10, 60_000).isSuccess());
        var result = reg.accept(D, ND, "Alice", 20);
        assertTrue(result.isSuccess());
        assertSame(reg.getParty(A).orElseThrow(), reg.getParty(D).orElseThrow());
    }

    @Test
    void accept_unknownName() {
        assertTrue(invite(A, NA, D, ND, 0).isSuccess());
        assertEquals(NO_INVITE_FROM, reg.accept(D, ND, "zed", 1).error());
        assertEquals(NO_INVITE, reg.accept(C, NC, null, 1).error());
        partyAB();
        assertEquals(ALREADY_IN_PARTY, reg.accept(B, NB, null, 5).error());
    }

    // ---- deny ----

    @Test
    void deny_disbandsLeaderOnlyParty() {
        assertTrue(invite(A, NA, B, NB, 0).isSuccess());
        var result = reg.deny(B, null, 5);
        assertTrue(result.isSuccess());
        assertEquals(List.of(INVITE_DENIED_INVITER, INVITE_DENIED_TARGET, DISBANDED_EMPTY), types(result));
        assertEquals(List.of(A), result.events().get(0).recipients());
        assertEquals(List.of(B), result.events().get(1).recipients());
        assertEquals(List.of(A), result.events().get(2).recipients());
        assertTrue(reg.getParty(A).isEmpty());
        assertNull(result.party());
    }

    @Test
    void deny_byNameKeepsTheOtherInvite() {
        assertTrue(invite(A, NA, D, ND, 0).isSuccess());
        assertTrue(invite(C, NC, D, ND, 10).isSuccess());
        var result = reg.deny(D, "alice", 20);
        assertTrue(result.isSuccess());
        assertEquals(1, reg.getInvitesFor(D, 20).size());
        assertEquals(NC, reg.getInvitesFor(D, 20).get(0).inviterName());
        assertEquals(NO_INVITE_FROM, reg.deny(D, "alice", 20).error());
        assertEquals(NO_INVITE, reg.deny(B, null, 20).error());
    }

    // ---- leave ----

    @Test
    void leave_member() {
        partyABC();
        var result = reg.leave(B, online);
        assertTrue(result.isSuccess());
        assertEquals(List.of(A, C), reg.getParty(A).orElseThrow().getMembers());
        assertEquals(List.of(B), recipients(result, MEMBER_LEFT_SELF));
        assertEquals(List.of(A, C), recipients(result, MEMBER_LEFT));
        assertTrue(reg.getParty(B).isEmpty());
    }

    @Test
    void leave_leaderTransfersToFirstOnline() {
        partyABC();
        var result = reg.leave(A, online);
        assertEquals(B, reg.getParty(B).orElseThrow().getLeader());
        var changed = event(result, LEADER_CHANGED);
        assertEquals(B, changed.subject());
        assertEquals(A, changed.other());
        assertEquals(List.of(B, C), changed.recipients());
        assertEquals(List.of(B, C), reg.getParty(B).get().getMembers());
    }

    @Test
    void leave_leaderSkipsOfflineSuccessor() {
        partyABC();
        onlineSet.remove(B);
        reg.leave(A, online);
        assertEquals(C, reg.getParty(C).orElseThrow().getLeader());
        assertTrue(reg.getParty(B).isPresent()); // B stays in the party (offline)
    }

    @Test
    void leave_leaderNobodyOnline() {
        partyAB();
        onlineSet.remove(B);
        var result = reg.leave(A, online);
        assertTrue(reg.getParties().isEmpty());
        var event = event(result, DISBANDED_LEADER_LEFT);
        assertEquals("Alice", event.subjectName());
        assertEquals(List.of(B), event.recipients());
        assertTrue(reg.getParty(B).isEmpty());
    }

    @Test
    void leave_lastMemberDisbands() {
        partyAB();
        var result = reg.leave(B, online);
        assertEquals(List.of(A), recipients(result, DISBANDED_EMPTY));
        assertTrue(reg.getParties().isEmpty());
    }

    @Test
    void leave_leaderAloneWithInvite() {
        assertTrue(invite(A, NA, B, NB, 0).isSuccess());
        var result = reg.leave(A, online);
        assertTrue(reg.getParties().isEmpty());
        assertEquals(List.of(MEMBER_LEFT_SELF), types(result));
        assertEquals(NO_INVITE, reg.accept(B, NB, null, 1).error());
    }

    @Test
    void leave_notInParty() {
        assertEquals(NOT_IN_PARTY, reg.leave(A, online).error());
    }

    // ---- kick / disband / transfer ----

    @Test
    void kick() {
        partyABC();
        var result = reg.kick(A, "bob");
        assertTrue(result.isSuccess());
        assertTrue(reg.getParty(B).isEmpty());
        var self = event(result, MEMBER_KICKED_SELF);
        assertEquals(List.of(B), self.recipients());
        assertEquals(A, self.other());
        assertEquals(List.of(A, C), recipients(result, MEMBER_KICKED));
        assertEquals(NOT_LEADER, reg.kick(C, "alice").error());
        assertEquals(CANNOT_KICK_SELF, reg.kick(A, "Alice").error());
        assertEquals(NOT_A_MEMBER, reg.kick(A, "zed").error());
        assertEquals(NOT_IN_PARTY, reg.kick(D, "alice").error());

        reg.markOffline(C, 5); // an offline member can be kicked
        assertTrue(reg.kick(A, "carol").isSuccess());
        assertTrue(reg.getParty(C).isEmpty());
    }

    @Test
    void disband() {
        partyABC();
        assertTrue(invite(A, NA, D, ND, 4).isSuccess());
        assertEquals(NOT_LEADER, reg.disband(B).error());
        var result = reg.disband(A);
        assertTrue(result.isSuccess());
        assertEquals(List.of(A, B, C), recipients(result, DISBANDED_BY_LEADER));
        assertTrue(reg.getParty(A).isEmpty());
        assertTrue(reg.getParty(B).isEmpty());
        assertTrue(reg.getParty(C).isEmpty());
        assertTrue(reg.getInvitesFor(D, 0).isEmpty());
        assertEquals(NOT_IN_PARTY, reg.disband(A).error());
    }

    @Test
    void transfer() {
        partyAB();
        var result = reg.transfer(A, "BOB", online);
        assertTrue(result.isSuccess());
        assertEquals(B, reg.getParty(A).orElseThrow().getLeader());
        assertEquals(List.of(A, B), recipients(result, LEADER_TRANSFERRED));
        assertEquals(CANNOT_TRANSFER_SELF, reg.transfer(B, "Bob", online).error());
        onlineSet.remove(A);
        assertEquals(TARGET_OFFLINE, reg.transfer(B, "Alice", online).error());
        assertEquals(NOT_A_MEMBER, reg.transfer(B, "zed", online).error());
        assertEquals(NOT_LEADER, reg.transfer(A, "Bob", online).error());
    }

    // ---- offline handling ----

    @Test
    void leaderGrace_transfer() {
        partyABC();
        onlineSet.remove(A);
        var off = reg.markOffline(A, 0);
        assertEquals(List.of(B, C), recipients(off, LEADER_DISCONNECTED));
        assertTrue(reg.tick(299_999, online, rules).events().isEmpty());
        var result = reg.tick(300_000, online, rules);
        assertEquals(List.of(LEADER_TRANSFERRED_OFFLINE, MEMBER_REMOVED_OFFLINE), types(result));
        var party = reg.getParty(B).orElseThrow();
        assertEquals(B, party.getLeader());
        assertFalse(party.isMember(A));
        assertTrue(reg.getParty(A).isEmpty());
    }

    @Test
    void leaderGrace_transfer_memberTimeoutNever() {
        partyABC();
        onlineSet.remove(A);
        reg.markOffline(A, 0);
        var neverRemove = new PartyRules(4, false, 60_000, 300_000, TRANSFER, -1);
        var result = reg.tick(300_000, online, neverRemove);
        assertEquals(List.of(LEADER_TRANSFERRED_OFFLINE), types(result));
        var party = reg.getParty(B).orElseThrow();
        assertEquals(B, party.getLeader());
        assertTrue(party.isMember(A));
    }

    @Test
    void leaderGrace_disband() {
        partyABC();
        reg.markOffline(A, 0);
        var disband = new PartyRules(4, false, 60_000, 300_000, DISBAND, 300_000);
        var result = reg.tick(300_000, online, disband);
        assertEquals(List.of(A, B, C), recipients(result, DISBANDED_LEADER_OFFLINE));
        assertTrue(reg.getParties().isEmpty());
    }

    @Test
    void leaderGrace_transferNobodyOnline() {
        partyAB();
        onlineSet.remove(A);
        onlineSet.remove(B);
        reg.markOffline(A, 0);
        reg.markOffline(B, 0);
        var result = reg.tick(300_000, online, rules);
        assertEquals(List.of(DISBANDED_LEADER_OFFLINE), types(result));
        assertTrue(reg.getParties().isEmpty());
    }

    @Test
    void leaderGrace_never() {
        partyAB();
        onlineSet.remove(A);
        reg.markOffline(A, 0);
        var never = new PartyRules(4, false, 60_000, -1, TRANSFER, 300_000);
        assertTrue(reg.tick(10_000_000, online, never).events().isEmpty());
        assertEquals(A, reg.getParty(A).orElseThrow().getLeader());
    }

    @Test
    void leaderReconnects() {
        partyABC();
        reg.markOffline(A, 0);
        var result = reg.markOnline(A, NA);
        assertEquals(List.of(B, C), recipients(result, MEMBER_RECONNECTED));
        assertTrue(reg.tick(400_000, online, rules).events().isEmpty());
        assertEquals(A, reg.getParty(A).orElseThrow().getLeader());
    }

    @Test
    void memberTimeout() {
        partyABC();
        var off = reg.markOffline(B, 0);
        assertEquals(List.of(A, C), recipients(off, MEMBER_DISCONNECTED));
        var result = reg.tick(300_000, online, rules);
        assertEquals(List.of(A, C), recipients(result, MEMBER_REMOVED_OFFLINE));
        assertTrue(reg.getParty(B).isEmpty());

        // only A and B: the party is disbanded together with the removal
        var small = new PartyRegistry(() -> new UUID(9, seq++));
        assertTrue(small.invite(A, NA, B, NB, 4, false, 0, 60_000).isSuccess());
        assertTrue(small.accept(B, NB, null, 1).isSuccess());
        small.markOffline(B, 0);
        var smallResult = small.tick(300_000, online, rules);
        assertEquals(List.of(MEMBER_REMOVED_OFFLINE, DISBANDED_EMPTY), types(smallResult));
        assertEquals(List.of(A), recipients(smallResult, DISBANDED_EMPTY));
        assertTrue(small.getParties().isEmpty());
    }

    @Test
    void markOnline_updatesName() {
        partyAB();
        reg.markOnline(B, "Bobby");
        assertEquals(B, reg.getParty(A).orElseThrow().findMemberByName("bobby").orElseThrow());
        assertTrue(reg.getParty(A).get().findMemberByName("bob").isEmpty());
    }

    @Test
    void markOffline_noParty() {
        var result = reg.markOffline(A, 0);
        assertTrue(result.isSuccess());
        assertTrue(result.events().isEmpty());
        var online = reg.markOnline(A, NA);
        assertTrue(online.isSuccess());
        assertTrue(online.events().isEmpty());
    }

    @Test
    void markOffline_twiceKeepsTheFirstTimestamp() {
        partyAB();
        reg.markOffline(B, 100);
        var second = reg.markOffline(B, 200);
        assertTrue(second.events().isEmpty());
        assertEquals(100, reg.getParty(B).orElseThrow().getOfflineSince(B).getAsLong());
    }

    // ---- invite expiry ----

    @Test
    void tick_inviteExpiry() {
        assertTrue(invite(A, NA, B, NB, 0).isSuccess());
        var result = reg.tick(60_000, online, rules);
        assertEquals(List.of(INVITE_EXPIRED_INVITER, INVITE_EXPIRED_TARGET, DISBANDED_EMPTY), types(result));
        assertEquals(List.of(A), result.events().get(0).recipients());
        assertEquals(List.of(B), result.events().get(1).recipients());
        assertEquals(List.of(A), result.events().get(2).recipients());
        assertTrue(reg.getParties().isEmpty());

        // the party with another member survives
        assertTrue(invite(A, NA, C, NC, 100_000).isSuccess());
        assertTrue(reg.accept(C, NC, null, 100_001).isSuccess());
        assertTrue(invite(A, NA, B, NB, 100_002).isSuccess());
        var survived = reg.tick(160_002, online, rules);
        assertEquals(List.of(INVITE_EXPIRED_INVITER, INVITE_EXPIRED_TARGET), types(survived));
        assertEquals(2, reg.getParty(A).orElseThrow().size());
    }

    @Test
    void tick_notExpiredBeforeTheBoundary() {
        assertTrue(invite(A, NA, B, NB, 0).isSuccess());
        assertTrue(reg.tick(59_999, online, rules).events().isEmpty());
        assertTrue(reg.getParty(A).isPresent());
    }

    // ---- model ----

    @Test
    void getMembers_isUnmodifiable_andContainsLeader() {
        partyAB();
        var members = reg.getParty(A).orElseThrow().getMembers();
        assertTrue(members.contains(A));
        assertThrows(UnsupportedOperationException.class, () -> members.add(D));
    }

    @Test
    void getInvitesFor_newestFirst_tiesReversed() {
        assertTrue(invite(A, NA, D, ND, 0).isSuccess());
        assertTrue(invite(C, NC, D, ND, 10).isSuccess());
        assertTrue(invite(B, NB, D, ND, 10).isSuccess());
        var invites = reg.getInvitesFor(D, 20);
        assertEquals(List.of(NB, NC, NA), invites.stream().map(PartyInvite::inviterName).collect(Collectors.toList()));
        assertEquals(2, reg.getInvitesFor(D, 60_000).size()); // the invite created at t=0 expired exactly at t=60_000
        assertTrue(reg.getInvitesFor(D, 60_010).isEmpty());
    }

    @Test
    void leaderCannotBeRemoved() {
        partyAB();
        var party = reg.getParty(A).orElseThrow();
        assertThrows(IllegalStateException.class, () -> party.removeMember(A));
        assertNotNull(party.toString());
    }

    @Test
    void ids_comeFromTheSupplier() {
        assertTrue(invite(A, NA, B, NB, 0).isSuccess());
        assertEquals(new UUID(9, 0), reg.getParty(A).orElseThrow().getId());
        assertSame(reg.getParty(A).get(), reg.getPartyById(new UUID(9, 0)).orElseThrow());
        reg.clear();
        assertTrue(reg.getParties().isEmpty());
        assertTrue(reg.getParty(A).isEmpty());
    }
}
