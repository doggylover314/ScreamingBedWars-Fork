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

import org.junit.jupiter.api.Test;
import org.screamingsandals.bedwars.game.mode.ClassicTeamFiller.TeamSlot;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassicTeamFillerTest {

    static UUID u(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    static PlayerGroup group(String id, int n, String preferred) {
        var members = new ArrayList<UUID>();
        for (int i = 0; i < n; i++) {
            members.add(u(id + i));
        }
        return new PlayerGroup(id, members, preferred, false);
    }

    static List<TeamSlot> emptyTeams(int max) {
        return List.of(new TeamSlot("Red", 0, max, -1), new TeamSlot("Blue", 0, max, -1),
                new TeamSlot("Green", 0, max, -1), new TeamSlot("Yellow", 0, max, -1));
    }

    static Map<UUID, String> teamOf(ClassicTeamFiller.Result result) {
        return result.placements().stream().collect(Collectors.toMap(ClassicTeamFiller.Placement::member, ClassicTeamFiller.Placement::team));
    }

    @Test
    void singlesMirrorTheOldRandomTeamRule() {
        var groups = new ArrayList<PlayerGroup>();
        for (int i = 0; i < 5; i++) {
            groups.add(group("s" + i, 1, null));
        }
        var result = ClassicTeamFiller.fill(emptyTeams(2), groups);
        var teams = groups.stream().map(g -> teamOf(result).get(g.members().get(0))).collect(Collectors.toList());
        assertEquals(List.of("Red", "Blue", "Red", "Blue", "Green"), teams);
        assertTrue(result.unplaced().isEmpty());
    }

    @Test
    void chooseTeamPrefersTheLowestActiveTeam() {
        var slots = List.of(new TeamSlot("Red", 2, 2, 0), new TeamSlot("Blue", 1, 2, 1),
                new TeamSlot("Green", 0, 2, -1));
        assertEquals("Blue", ClassicTeamFiller.chooseTeam(slots, 1));

        var equal = List.of(new TeamSlot("Red", 1, 2, 0), new TeamSlot("Blue", 1, 2, 1),
                new TeamSlot("Green", 0, 2, -1));
        assertEquals("Red", ClassicTeamFiller.chooseTeam(equal, 1)); // activation order wins ties
    }

    @Test
    void chooseTeamNeedsRoomForTheWholeGroup() {
        var slots = List.of(new TeamSlot("Red", 1, 2, 0), new TeamSlot("Blue", 1, 2, 1),
                new TeamSlot("Green", 0, 2, -1));
        assertEquals("Green", ClassicTeamFiller.chooseTeam(slots, 2));
        assertNull(ClassicTeamFiller.chooseTeam(slots, 3));
    }

    @Test
    void oneActiveTeamStartsTheNextInactiveOne() {
        var slots = List.of(new TeamSlot("Red", 1, 2, 0), new TeamSlot("Blue", 0, 2, -1));
        assertEquals("Blue", ClassicTeamFiller.chooseTeam(slots, 1));
    }

    @Test
    void groupStaysTogetherAndSinglesGoElsewhere() {
        var party = group("party", 2, null);
        var a = group("a", 1, null);
        var b = group("b", 1, null);
        var result = ClassicTeamFiller.fill(emptyTeams(2), List.of(party, a, b));
        var teams = teamOf(result);
        assertEquals("Red", teams.get(party.members().get(0)));
        assertEquals("Red", teams.get(party.members().get(1)));
        assertEquals("Blue", teams.get(a.members().get(0)));
        assertEquals("Blue", teams.get(b.members().get(0)));
    }

    @Test
    void preferredTeamWinsIfItHasRoom() {
        var slots = List.of(new TeamSlot("Red", 0, 2, 0), new TeamSlot("Blue", 1, 2, 1),
                new TeamSlot("Green", 0, 2, -1));
        var single = group("x", 1, "Blue");
        var result = ClassicTeamFiller.fill(slots, List.of(single));
        assertEquals("Blue", teamOf(result).get(single.members().get(0)));
    }

    @Test
    void fullPreferredTeamFallsBackToTheNormalRule() {
        var slots = List.of(new TeamSlot("Red", 0, 2, 0), new TeamSlot("Blue", 2, 2, 1),
                new TeamSlot("Green", 0, 2, -1));
        var single = group("x", 1, "Blue");
        var result = ClassicTeamFiller.fill(slots, List.of(single));
        assertEquals("Red", teamOf(result).get(single.members().get(0)));
    }

    @Test
    void oversizedGroupIsSplit() {
        var big = group("big", 3, null);
        var result = ClassicTeamFiller.fill(emptyTeams(2), List.of(big));
        assertTrue(result.splitGroups().contains("big"));
        assertEquals(3, result.placements().size());
        assertTrue(result.unplaced().isEmpty());
        var teams = teamOf(result);
        assertEquals(2, teams.values().stream().distinct().count()); // spread over at least two teams
    }

    @Test
    void fullTeamsLeaveMembersUnplaced() {
        var slots = List.of(new TeamSlot("Red", 2, 2, 0), new TeamSlot("Blue", 2, 2, 1));
        var single = group("x", 1, null);
        var result = ClassicTeamFiller.fill(slots, List.of(single));
        assertEquals(List.of(single.members().get(0)), result.unplaced());
        assertTrue(result.placements().isEmpty());
    }

    @Test
    void partyThatJoinedAfterSoloPlayersIsStillKeptTogether() {
        // 2 teams x 2: Alice, Bob, then a party of 2 -> the party must not be split
        var twoTeams = List.of(new TeamSlot("Red", 0, 2, -1), new TeamSlot("Blue", 0, 2, -1));
        var alice = group("alice", 1, null);
        var bob = group("bob", 1, null);
        var party = group("party", 2, null);
        var result = ClassicTeamFiller.fill(twoTeams, List.of(alice, bob, party));
        var teams = teamOf(result);
        assertTrue(result.splitGroups().isEmpty());
        assertTrue(result.unplaced().isEmpty());
        assertEquals(teams.get(party.members().get(0)), teams.get(party.members().get(1)));
        assertEquals(teams.get(alice.members().get(0)), teams.get(bob.members().get(0)));
        assertNotEquals(teams.get(alice.members().get(0)), teams.get(party.members().get(0)));
    }

    @Test
    void partyOfThreeIsKeptTogetherAfterTwoSolosInTeamsOfThree() {
        var twoTeams = List.of(new TeamSlot("Red", 0, 3, -1), new TeamSlot("Blue", 0, 3, -1));
        var a = group("a", 1, null);
        var b = group("b", 1, null);
        var party = group("party", 3, null);
        var result = ClassicTeamFiller.fill(twoTeams, List.of(a, b, party));
        var teams = teamOf(result);
        assertTrue(result.splitGroups().isEmpty());
        assertEquals(1, party.members().stream().map(teams::get).distinct().count());
        assertNotEquals(teams.get(party.members().get(0)), teams.get(a.members().get(0)));
    }

    @Test
    void biggerPartiesArePlacedFirstAndSolosFillTheRest() {
        var twoTeams = List.of(new TeamSlot("Red", 0, 3, -1), new TeamSlot("Blue", 0, 3, -1));
        var solo = group("solo", 1, null);
        var duo = group("duo", 2, null);
        var trio = group("trio", 3, null);
        var result = ClassicTeamFiller.fill(twoTeams, List.of(solo, duo, trio));
        var teams = teamOf(result);
        assertTrue(result.splitGroups().isEmpty());
        assertEquals(1, trio.members().stream().map(teams::get).distinct().count());
        assertEquals(1, duo.members().stream().map(teams::get).distinct().count());
        assertEquals(teams.get(duo.members().get(0)), teams.get(solo.members().get(0)));
    }

    @Test
    void memberFollowingHisPartyTeamIsPlacedBeforeOtherSolos() {
        var slots = List.of(new TeamSlot("Red", 1, 2, 0), new TeamSlot("Blue", 1, 2, 1));
        var stranger = group("stranger", 1, null);
        var follower = group("follower", 1, "Red");
        var result = ClassicTeamFiller.fill(slots, List.of(stranger, follower));
        var teams = teamOf(result);
        assertEquals("Red", teams.get(follower.members().get(0)));
        assertEquals("Blue", teams.get(stranger.members().get(0)));
    }

    @Test
    void oversizedGroupIsSplitAfterTheGroupsThatFit() {
        var twoTeams = List.of(new TeamSlot("Red", 0, 3, -1), new TeamSlot("Blue", 0, 3, -1));
        var big = group("big", 4, null);
        var duo = group("duo", 2, null);
        var result = ClassicTeamFiller.fill(twoTeams, List.of(big, duo));
        var teams = teamOf(result);
        assertEquals(Set.of("big"), result.splitGroups());
        assertTrue(result.unplaced().isEmpty());
        assertEquals(1, duo.members().stream().map(teams::get).distinct().count());
    }
}
