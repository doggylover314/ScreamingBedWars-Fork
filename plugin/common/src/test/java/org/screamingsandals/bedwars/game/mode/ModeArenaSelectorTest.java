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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeArenaSelectorTest {
    private static final ModeDefinition MODE_4V4 = new ModeDefinition("4v4", "4v4", 2, 4, 8, List.of());
    private static final ModeDefinition MODE_2V2 = new ModeDefinition("2v2", "2v2", 2, 2, 4, List.of());
    private static final ModeDefinition MODE_3V3 = new ModeDefinition("3v3", "3v3", 2, 3, 6, List.of());

    /** Running lobby: every player is a single group. */
    static ArenaSnapshot lobby(String name, int players, String modeId) {
        return new ArenaSnapshot(name, name, ArenaState.WAITING, players, modeId, 2, Set.of(1, 2, 3, 4), Set.of(),
                Collections.nCopies(players, 1));
    }

    static ArenaSnapshot lobby(String name, int players, String modeId, List<Integer> groupSizes) {
        return new ArenaSnapshot(name, name, ArenaState.WAITING, players, modeId, 2, Set.of(1, 2, 3, 4), Set.of(), groupSizes);
    }

    static ArenaSnapshot idle(String name) {
        return idle(name, 2, Set.of(1, 2, 3, 4), Set.of());
    }

    static ArenaSnapshot idle(String name, int teamCount, Set<Integer> allowed, Set<Integer> preferred) {
        return new ArenaSnapshot(name, name, ArenaState.WAITING, 0, null, teamCount, allowed, preferred, List.of());
    }

    static ArenaSnapshot other(String name, ArenaState state, int players, String modeId) {
        return new ArenaSnapshot(name, name, state, players, modeId, 2, Set.of(1, 2, 3, 4), Set.of(), List.of());
    }

    @Test
    void fullestRunningLobbyWins() {
        var a = lobby("A", 3, "4v4");
        var b = lobby("B", 5, "4v4");
        var c = idle("C");
        var result = ModeArenaSelector.select(List.of(a, b, c), MODE_4V4, 1, new Random(1));
        assertEquals(SelectionResult.Outcome.JOIN_EXISTING, result.outcome());
        assertEquals("B", result.arenaId());

        result = ModeArenaSelector.select(List.of(a, b, c), MODE_4V4, 3, new Random(1));
        assertEquals("B", result.arenaId()); // B has room for exactly 3

        result = ModeArenaSelector.select(List.of(a, b, c), MODE_4V4, 4, new Random(1));
        assertEquals("A", result.arenaId()); // B has only room for 3
    }

    @Test
    void idleArenaIsClaimedOnlyWhenNoRunningLobbyFits() {
        var a = lobby("A", 6, "4v4");
        var c = idle("C");
        var result = ModeArenaSelector.select(List.of(a, c), MODE_4V4, 4, new Random(1));
        assertEquals(SelectionResult.Outcome.CLAIM_IDLE, result.outcome());
        assertEquals("C", result.arenaId());
    }

    @Test
    void excludedIdleCandidates() {
        var wrongTeamCount = idle("Four", 4, Set.of(1, 2, 3, 4), Set.of());
        var wrongSize = idle("Sizes", 2, Set.of(1, 2), Set.of());
        var busy = other("Busy", ArenaState.BUSY, 0, null);
        var unavailable = other("Off", ArenaState.UNAVAILABLE, 0, null);
        var waitingWithPlayersNoMode = other("Classic", ArenaState.WAITING, 2, null);
        var otherMode = lobby("Other", 2, "2v2");
        var all = List.of(wrongTeamCount, wrongSize, busy, unavailable, waitingWithPlayersNoMode, otherMode);
        for (int seed = 0; seed < 10; seed++) {
            var result = ModeArenaSelector.select(all, MODE_4V4, 1, new Random(seed));
            assertEquals(SelectionResult.Outcome.NONE_AVAILABLE, result.outcome());
            assertNull(result.arenaId());
        }
    }

    @Test
    void preferredSizesFirst() {
        var x = idle("X", 2, Set.of(1, 2, 3, 4), Set.of(1, 2));
        var y = idle("Y", 2, Set.of(1, 2, 3, 4), Set.of());
        for (int seed = 0; seed < 20; seed++) {
            assertEquals("X", ModeArenaSelector.select(List.of(x, y), MODE_2V2, 1, new Random(seed)).arenaId());
        }
        // small consecutive seeds give strongly correlated first values, so one generator is used for all draws
        var random = new Random(12345L);
        var seen = new HashSet<String>();
        for (int i = 0; i < 40; i++) {
            seen.add(ModeArenaSelector.select(List.of(x, y), MODE_4V4, 1, random).arenaId());
        }
        assertEquals(Set.of("X", "Y"), seen);
    }

    @Test
    void arenaWhitelist() {
        var mode = new ModeDefinition("4v4", "4v4", 2, 4, 8, List.of("Y"));
        var x = idle("X");
        var y = idle("Y");
        for (int seed = 0; seed < 10; seed++) {
            assertEquals("Y", ModeArenaSelector.select(List.of(x, y), mode, 1, new Random(seed)).arenaId());
        }
    }

    @Test
    void partyTooLarge() {
        assertEquals(SelectionResult.Outcome.PARTY_TOO_LARGE,
                ModeArenaSelector.select(List.of(idle("C")), MODE_4V4, 5, new Random(1)).outcome());
        assertEquals(SelectionResult.Outcome.PARTY_TOO_LARGE,
                ModeArenaSelector.select(List.of(idle("C")), MODE_2V2, 3, new Random(1)).outcome());
    }

    @Test
    void nothingAvailable() {
        var result = ModeArenaSelector.select(List.of(other("A", ArenaState.BUSY, 8, "4v4")), MODE_4V4, 1, new Random(1));
        assertEquals(SelectionResult.Outcome.NONE_AVAILABLE, result.outcome());
        assertNull(result.arenaId());
    }

    @Test
    void tieAmongFullestLobbiesIsRandom() {
        var a = lobby("A", 5, "4v4");
        var b = lobby("B", 5, "4v4");
        var random = new Random(12345L);
        var seen = new HashSet<String>();
        for (int i = 0; i < 40; i++) {
            var result = ModeArenaSelector.select(List.of(a, b), MODE_4V4, 1, random);
            assertEquals(SelectionResult.Outcome.JOIN_EXISTING, result.outcome());
            seen.add(result.arenaId());
        }
        assertEquals(Set.of("A", "B"), seen);
    }

    @Test
    void countJoinable() {
        var a = lobby("A", 3, "4v4");
        var c = idle("C");
        var full = lobby("D", 8, "4v4");
        assertEquals(2, ModeArenaSelector.countJoinable(List.of(a, c, full), MODE_4V4));
    }

    // ---------- packing (D49) ----------

    @Test
    void lobbyWithTwoPartiesOfThreeRefusesAPartyOfTwo() {
        var r = lobby("R", 6, "4v4", List.of(3, 3));
        var i = idle("I");
        var result = ModeArenaSelector.select(List.of(r, i), MODE_4V4, 2, new Random(1));
        assertEquals(SelectionResult.Outcome.CLAIM_IDLE, result.outcome());
        assertEquals("I", result.arenaId());

        result = ModeArenaSelector.select(List.of(r, i), MODE_4V4, 1, new Random(1));
        assertEquals(SelectionResult.Outcome.JOIN_EXISTING, result.outcome());
        assertEquals("R", result.arenaId());

        result = ModeArenaSelector.select(List.of(r), MODE_4V4, 2, new Random(1));
        assertEquals(SelectionResult.Outcome.NONE_AVAILABLE, result.outcome());
    }

    @Test
    void lobbyWithSinglesStillTakesAPartyOfTwo() {
        var r2 = lobby("R2", 6, "4v4", List.of(3, 1, 1, 1));
        var result = ModeArenaSelector.select(List.of(r2), MODE_4V4, 2, new Random(1));
        assertEquals(SelectionResult.Outcome.JOIN_EXISTING, result.outcome());
        assertEquals("R2", result.arenaId());
    }

    @Test
    void threeVersusThreeWithTwoPairsRefusesAnotherPair() {
        var r = lobby("R", 4, "3v3", List.of(2, 2));
        var result = ModeArenaSelector.select(List.of(r), MODE_3V3, 2, new Random(1));
        assertEquals(SelectionResult.Outcome.NONE_AVAILABLE, result.outcome());
    }

    @Test
    void snapshotCopiesItsCollections() {
        var sizes = new java.util.ArrayList<>(List.of(1, 2));
        var snapshot = new ArenaSnapshot("id", "name", ArenaState.WAITING, 3, null, 2, Set.of(1), Set.of(), sizes);
        sizes.add(5);
        assertEquals(List.of(1, 2), snapshot.groupSizes());
        assertTrue(snapshot.preferredTeamSizes().isEmpty());
    }
}
