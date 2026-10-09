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

package org.screamingsandals.bedwars.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiscUtilsFullestPickTest {
    /** arena name -> players */
    private static final Map<String, Integer> PLAYERS = Map.of("A", 6, "B", 2, "C", 6, "D", 0);
    private static final IntUnaryOperator FIRST = bound -> 0;
    private static final IntUnaryOperator LAST = bound -> bound - 1;

    private static Optional<String> pick(List<String> candidates, Set<String> fitting, IntUnaryOperator random) {
        return MiscUtils.FullestPick.pick(candidates, PLAYERS::get, fitting == null ? null : fitting::contains, random);
    }

    @Test
    void emptyCandidatesGiveNothing() {
        assertEquals(Optional.empty(), pick(List.of(), null, FIRST));
        assertEquals(Optional.empty(), pick(List.of(), Set.of("A"), FIRST));
    }

    @Test
    void withoutPreferenceTheFullestArenaWins() {
        assertEquals(Optional.of("A"), pick(List.of("B", "A", "D"), null, FIRST));
        assertEquals(Optional.of("B"), pick(List.of("B", "D"), null, FIRST));
    }

    @Test
    void equallyFullArenasAreChosenByTheRandomSource() {
        var candidates = List.of("A", "B", "C");
        assertEquals(Optional.of("A"), pick(candidates, null, FIRST));
        assertEquals(Optional.of("C"), pick(candidates, null, LAST));
    }

    @Test
    void randomSourceOnlySeesTheEquallyFullArenas() {
        var bounds = new ArrayList<Integer>();
        pick(List.of("A", "B", "C", "D"), null, bound -> {
            bounds.add(bound);
            return 0;
        });
        assertEquals(List.of(2), bounds);
    }

    @Test
    void aSingleBestArenaNeedsNoRandomness() {
        assertEquals(Optional.of("A"), pick(List.of("A", "B"), null, bound -> {
            throw new AssertionError("no tie, no random number");
        }));
    }

    @Test
    void arenasThatFitThePartyArePreferredOverTheFullestOne() {
        // A (6 players) cannot take the party, B (2) and D (0) can: the fullest of those wins
        assertEquals(Optional.of("B"), pick(List.of("A", "B", "D"), Set.of("B", "D"), FIRST));
    }

    @Test
    void tieBreakOnlyConsidersFittingArenas() {
        // A and C are equally full, but only C fits
        for (var random : List.of(FIRST, LAST)) {
            assertEquals(Optional.of("C"), pick(List.of("A", "B", "C"), Set.of("C", "B"), random));
        }
    }

    @Test
    void nothingFittingFallsBackToTheNormalPick() {
        assertEquals(Optional.of("A"), pick(List.of("A", "B", "D"), Set.of(), FIRST));
        assertEquals(Optional.of("C"), pick(List.of("A", "B", "C"), Set.of(), LAST));
    }

    @Test
    void everyEquallyFullArenaCanBePickedWhenAllFit() {
        var candidates = List.of("A", "B", "C");
        var picked = IntStream.range(0, 2)
                .mapToObj(i -> pick(candidates, Set.of("A", "B", "C"), bound -> i).orElseThrow())
                .collect(Collectors.toSet());
        assertEquals(Set.of("A", "C"), picked);
    }
}
