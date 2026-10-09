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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeTeamDistributorTest {

    static PlayerGroup g(String id, int n) {
        var members = new ArrayList<UUID>();
        for (int i = 0; i < n; i++) {
            members.add(UUID.nameUUIDFromBytes((id + i).getBytes(StandardCharsets.UTF_8)));
        }
        return new PlayerGroup(id, members, null, false);
    }

    static List<PlayerGroup> singles(int n) {
        return IntStream.range(0, n).mapToObj(i -> g("s" + i, 1)).collect(Collectors.toList());
    }

    static List<Integer> sizes(ModeTeamDistributor.Distribution dist) {
        return dist.teamSizes();
    }

    static Set<String> groupIdsOfTeam(ModeTeamDistributor.Distribution dist, int index) {
        return dist.teams().get(index).stream().map(PlayerGroup::id).collect(Collectors.toSet());
    }

    @Test
    void singlesAreEvened() {
        assertEquals(List.of(1, 1), sizes(ModeTeamDistributor.distribute(singles(2), 4, 4)));
        assertEquals(List.of(3, 2), sizes(ModeTeamDistributor.distribute(singles(5), 4, 4)));
        assertEquals(List.of(4, 4), sizes(ModeTeamDistributor.distribute(singles(8), 4, 4)));
        assertEquals(List.of(3, 3, 3), sizes(ModeTeamDistributor.distribute(singles(9), 4, 4)));
        assertEquals(List.of(4, 4, 4), sizes(ModeTeamDistributor.distribute(singles(12), 4, 4)));
        assertEquals(List.of(4, 3, 3, 3), sizes(ModeTeamDistributor.distribute(singles(13), 4, 4)));
        assertEquals(List.of(4, 4, 4, 4), sizes(ModeTeamDistributor.distribute(singles(16), 4, 4)));
        for (int n : new int[]{2, 5, 8, 9, 12, 13, 16}) {
            assertTrue(ModeTeamDistributor.distribute(singles(n), 4, 4).isStartable(), "n=" + n);
        }
    }

    @Test
    void oneSingleIsNotStartable() {
        var dist = ModeTeamDistributor.distribute(singles(1), 4, 4);
        assertFalse(dist.isStartable());
        assertEquals(1, dist.teams().size());
        assertTrue(dist.unassigned().isEmpty());
    }

    @Test
    void noGroups() {
        var dist = ModeTeamDistributor.distribute(List.of(), 4, 4);
        assertTrue(dist.teams().isEmpty());
        assertFalse(dist.isStartable());
    }

    @Test
    void smallModes() {
        assertEquals(List.of(1, 1), sizes(ModeTeamDistributor.distribute(singles(2), 1, 2)));
        assertEquals(List.of(2, 2), sizes(ModeTeamDistributor.distribute(singles(4), 2, 3)));
        assertEquals(List.of(2, 2, 1), sizes(ModeTeamDistributor.distribute(singles(5), 2, 3)));
        assertEquals(List.of(2, 2, 2), sizes(ModeTeamDistributor.distribute(singles(6), 2, 3)));
        assertEquals(List.of(1, 1, 1), sizes(ModeTeamDistributor.distribute(singles(3), 1, 4)));
    }

    @Test
    void partiesNeverSplitAndMoreTeamsAreUsedIfTheyDoNotFit() {
        var groups = List.of(g("a", 3), g("b", 3), g("c", 2));
        var dist = ModeTeamDistributor.distribute(groups, 4, 4);
        assertEquals(List.of(3, 3, 2), sizes(dist));
        assertFalse(dist.partiesSplit());
        for (var team : dist.teams()) {
            assertEquals(1, team.size()); // each party alone in its team
        }
    }

    @Test
    void twoPartiesAndFourSingles() {
        var groups = new ArrayList<PlayerGroup>();
        groups.add(g("p1", 2));
        groups.add(g("p2", 2));
        groups.addAll(singles(4));
        var dist = ModeTeamDistributor.distribute(groups, 4, 4);
        assertEquals(List.of(4, 4), sizes(dist));
        assertTrue(dist.isStartable());
        for (var party : List.of("p1", "p2")) {
            long teamsWithParty = dist.teams().stream().filter(t -> t.stream().anyMatch(x -> x.id().equals(party))).count();
            assertEquals(1, teamsWithParty);
        }
    }

    @Test
    void partyOfThreeAndFiveSingles() {
        var groups = new ArrayList<PlayerGroup>();
        groups.add(g("p", 3));
        groups.addAll(singles(5));
        var dist = ModeTeamDistributor.distribute(groups, 4, 4);
        assertEquals(List.of(4, 4), sizes(dist));
        var partyTeam = dist.teams().stream().filter(t -> t.stream().anyMatch(x -> x.id().equals("p"))).findFirst().orElseThrow();
        assertEquals(2, partyTeam.size()); // the party + one single
    }

    @Test
    void threeTeamsOfThreeWithPairs() {
        var dist = ModeTeamDistributor.distribute(List.of(g("a", 2), g("b", 2), g("c", 2)), 3, 3);
        assertEquals(List.of(2, 2, 2), sizes(dist));
        assertTrue(dist.isStartable());
    }

    @Test
    void fullPartyPlusSingle() {
        var groups = new ArrayList<PlayerGroup>();
        groups.add(g("p", 4));
        groups.add(g("s", 1));
        var dist = ModeTeamDistributor.distribute(groups, 4, 2);
        assertEquals(List.of(4, 1), sizes(dist));
        assertTrue(dist.isStartable());
    }

    @Test
    void tooManyPlayersLeavesSomeUnassigned() {
        var groups = new ArrayList<PlayerGroup>();
        groups.add(g("p1", 4));
        groups.add(g("p2", 4));
        groups.add(g("s", 1));
        var dist = ModeTeamDistributor.distribute(groups, 4, 2);
        assertEquals(List.of(4, 4), sizes(dist));
        assertEquals(1, dist.unassigned().size());
        assertEquals(1, dist.unassigned().get(0).size());
        assertFalse(dist.isStartable());
    }

    @Test
    void oversizedPartyIsSplitIntoFragments() {
        var groups = new ArrayList<PlayerGroup>();
        groups.add(g("big", 5));
        groups.addAll(singles(3));
        var dist = ModeTeamDistributor.distribute(groups, 4, 2);
        assertEquals(List.of(4, 4), sizes(dist));
        assertTrue(dist.partiesSplit());
        var fragments = dist.teams().stream().flatMap(List::stream).filter(PlayerGroup::fragment).collect(Collectors.toList());
        assertEquals(2, fragments.size());
        assertEquals(Set.of("big#1", "big#2"), fragments.stream().map(PlayerGroup::id).collect(Collectors.toSet()));
        assertEquals(Set.of(4, 1), fragments.stream().map(PlayerGroup::size).collect(Collectors.toSet()));
    }

    @Test
    void deterministic() {
        var groups = new ArrayList<PlayerGroup>();
        groups.add(g("p1", 2));
        groups.add(g("p2", 3));
        groups.addAll(singles(6));
        var first = ModeTeamDistributor.distribute(groups, 4, 4);
        var second = ModeTeamDistributor.distribute(groups, 4, 4);
        assertEquals(first.teams(), second.teams());
    }

    @Test
    void invalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> ModeTeamDistributor.distribute(singles(2), 0, 2));
        assertThrows(IllegalArgumentException.class, () -> ModeTeamDistributor.distribute(singles(2), 2, 0));
    }

    @Test
    void fitsCases() {
        assertFalse(ModeTeamDistributor.fits(List.of(3, 3, 2), 4, 2));
        assertTrue(ModeTeamDistributor.fits(List.of(3, 3, 1), 4, 2));
        assertFalse(ModeTeamDistributor.fits(List.of(2, 2, 2), 3, 2));
        assertTrue(ModeTeamDistributor.fits(List.of(2, 2, 2), 3, 3));
        assertTrue(ModeTeamDistributor.fits(List.of(4, 4), 4, 2));
        assertFalse(ModeTeamDistributor.fits(List.of(4, 4, 1), 4, 2));
        assertFalse(ModeTeamDistributor.fits(List.of(5), 4, 4));
        assertTrue(ModeTeamDistributor.fits(List.of(), 4, 2));
    }

    @Test
    void fitsIgnoresInvalidSizes() {
        var sizes = new ArrayList<Integer>();
        sizes.add(null);
        sizes.add(0);
        sizes.add(2);
        assertTrue(ModeTeamDistributor.fits(sizes, 4, 2));
    }

    // ---------- invariants over random inputs ----------

    /** Simple exhaustive checker: can the groups be split into exactly k non-empty teams of at most cap players? */
    private static boolean exists(List<Integer> sizes, int k, int cap) {
        return place(sizes, 0, new int[k], cap);
    }

    private static boolean place(List<Integer> sizes, int index, int[] loads, int cap) {
        if (index == sizes.size()) {
            for (int load : loads) {
                if (load == 0) {
                    return false;
                }
            }
            return true;
        }
        for (int b = 0; b < loads.length; b++) {
            if (loads[b] + sizes.get(index) > cap) {
                continue;
            }
            loads[b] += sizes.get(index);
            boolean ok = place(sizes, index + 1, loads, cap);
            loads[b] -= sizes.get(index);
            if (ok) {
                return true;
            }
        }
        return false;
    }

    @Test
    void randomInvariants() {
        var random = new Random(20260101L);
        int checked = 0;
        for (int iteration = 0; iteration < 200; iteration++) {
            int teamSize = 1 + random.nextInt(4);
            int teamCount = 2 + random.nextInt(3);
            int capacity = teamSize * teamCount;
            var groups = new ArrayList<PlayerGroup>();
            int total = 0;
            int guard = 0;
            while (guard++ < 12) {
                int size = 1 + random.nextInt(teamSize);
                if (total + size > Math.min(capacity, 8)) {
                    break;
                }
                groups.add(g("g" + iteration + "_" + groups.size(), size));
                total += size;
                if (random.nextInt(6) == 0) {
                    break;
                }
            }
            if (total < 2) {
                continue;
            }
            checked++;
            var dist = ModeTeamDistributor.distribute(groups, teamSize, teamCount);
            var sizesList = groups.stream().map(PlayerGroup::size).collect(Collectors.toList());

            // minimal number of teams for which a valid partition exists (brute force)
            int minimal = -1;
            for (int k = 2; k <= teamCount; k++) {
                if (exists(sizesList, k, teamSize)) {
                    minimal = k;
                    break;
                }
            }
            String context = "teamSize=" + teamSize + " teamCount=" + teamCount + " groups=" + sizesList;
            assertEquals(minimal != -1, dist.isStartable(), context);
            if (minimal != -1) {
                assertEquals(minimal, dist.teams().size(), context);
            }

            // no team above capacity, every group in exactly one place, nobody lost
            var seen = new HashSet<String>();
            for (var team : dist.teams()) {
                int teamTotal = team.stream().mapToInt(PlayerGroup::size).sum();
                assertTrue(teamTotal <= teamSize, context);
                for (var group : team) {
                    assertTrue(seen.add(group.id()), "group placed twice: " + context);
                }
            }
            dist.unassigned().forEach(x -> assertTrue(seen.add(x.id()), context));
            assertEquals(groups.size(), seen.size(), context);
            assertFalse(dist.partiesSplit(), context);
        }
        assertTrue(checked > 100, "too few random cases were checked: " + checked);
    }
}
