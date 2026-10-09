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

import org.junit.jupiter.api.Test;
import org.screamingsandals.bedwars.party.PartyJoinPlanner.Member;
import org.screamingsandals.bedwars.party.PartyJoinPlanner.MemberState;
import org.screamingsandals.bedwars.party.PartyJoinPlanner.Outcome;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.screamingsandals.bedwars.party.PartyJoinPlanner.MemberState.FREE;
import static org.screamingsandals.bedwars.party.PartyJoinPlanner.MemberState.IN_TARGET;
import static org.screamingsandals.bedwars.party.PartyJoinPlanner.MemberState.OFFLINE;
import static org.screamingsandals.bedwars.party.PartyJoinPlanner.MemberState.PLAYING_ELSEWHERE;

class PartyJoinPlannerTest {
    private static Member m(MemberState state) {
        return new Member(UUID.randomUUID(), "member", state);
    }

    @Test
    void twoFreeMembersFit() {
        var plan = PartyJoinPlanner.plan(false, List.of(m(FREE), m(FREE)), 4, 4, false);
        assertEquals(Outcome.OK, plan.outcome());
        assertEquals(2, plan.movers().size());
        assertEquals(3, plan.needed());
        assertEquals(3, plan.groupSize());
        assertEquals(4, plan.freeSlots());
        assertEquals(4, plan.maxTeamSize());
    }

    @Test
    void roomIsChecked() {
        var two = PartyJoinPlanner.plan(false, List.of(m(FREE), m(FREE)), 2, 4, false);
        assertEquals(Outcome.NO_ROOM, two.outcome());
        assertEquals(3, two.needed());
        assertEquals(Outcome.OK, PartyJoinPlanner.plan(false, List.of(m(FREE), m(FREE)), 3, 4, false).outcome());
    }

    @Test
    void leaderAlreadyInTargetNeedsNoSeat() {
        var plan = PartyJoinPlanner.plan(true, List.of(m(FREE), m(FREE)), 2, 4, false);
        assertEquals(Outcome.OK, plan.outcome());
        assertEquals(2, plan.needed());
    }

    @Test
    void offlineMembersAreIgnored() {
        var plan = PartyJoinPlanner.plan(false, List.of(m(FREE), m(OFFLINE)), 2, 4, false);
        assertEquals(Outcome.OK, plan.outcome());
        assertEquals(1, plan.movers().size());
        assertEquals(2, plan.needed());
        assertEquals(2, plan.groupSize());
    }

    @Test
    void membersAlreadyInTargetCountForTheTeamSize() {
        var plan = PartyJoinPlanner.plan(false, List.of(m(IN_TARGET), m(FREE)), 2, 2, false);
        assertEquals(Outcome.TOO_BIG_FOR_TEAM, plan.outcome());
        assertEquals(3, plan.groupSize());
    }

    @Test
    void playingMembersStayUnlessPulled() {
        var stay = PartyJoinPlanner.plan(false, List.of(m(PLAYING_ELSEWHERE), m(FREE)), 4, 4, false);
        assertEquals(1, stay.busy().size());
        assertEquals(1, stay.movers().size());
        assertEquals(2, stay.needed());
        assertEquals(2, stay.groupSize());

        var pull = PartyJoinPlanner.plan(false, List.of(m(PLAYING_ELSEWHERE), m(FREE)), 4, 4, true);
        assertEquals(2, pull.movers().size());
        assertTrue(pull.busy().isEmpty());
    }

    @Test
    void teamSizeZeroIsNotChecked() {
        var plan = PartyJoinPlanner.plan(false, List.of(m(FREE), m(FREE), m(FREE), m(FREE), m(FREE)), 10, 0, false);
        assertEquals(Outcome.OK, plan.outcome());
    }

    @Test
    void teamSizeWinsOverRoom() {
        var plan = PartyJoinPlanner.plan(false, List.of(m(FREE), m(FREE), m(FREE), m(FREE)), 1, 2, false);
        assertEquals(Outcome.TOO_BIG_FOR_TEAM, plan.outcome());
    }

    @Test
    void edgeCasesOfTheNeededGuard() {
        var overFull = PartyJoinPlanner.plan(false, List.of(), -1, 4, false);
        assertEquals(Outcome.NO_ROOM, overFull.outcome());
        assertEquals(1, overFull.needed());

        var nobodyToMove = PartyJoinPlanner.plan(true, List.of(), -1, 4, false);
        assertEquals(Outcome.OK, nobodyToMove.outcome());
        assertEquals(0, nobodyToMove.needed());

        var present = PartyJoinPlanner.plan(true, List.of(m(IN_TARGET)), 0, 4, false);
        assertEquals(Outcome.OK, present.outcome());
        assertEquals(2, present.groupSize());
    }

    @Test
    void resultListsAreImmutable() {
        var plan = PartyJoinPlanner.plan(false, List.of(m(FREE)), 4, 4, false);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> plan.movers().add(m(FREE)));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> plan.busy().add(m(FREE)));
    }
}
