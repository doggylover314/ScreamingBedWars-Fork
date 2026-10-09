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

package org.screamingsandals.bedwars.game.endgame;

import org.junit.jupiter.api.Test;
import org.screamingsandals.bedwars.game.endgame.TimeEndResolver.Criterion;
import org.screamingsandals.bedwars.game.endgame.TimeEndResolver.Standing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeEndResolverTest {
    private static final List<Criterion> DEFAULT = List.of(Criterion.TARGET, Criterion.PLAYERS);

    @Test
    void aStandingBedBeatsAnInvalidOne() {
        var result = TimeEndResolver.resolve(List.of(
                new Standing("A", true, 1, 0, 0),
                new Standing("B", false, 4, 0, 0)
        ), DEFAULT);
        assertEquals("A", result.winnerId());
        assertEquals(Criterion.TARGET, result.decidedBy());
        assertFalse(result.isDraw());
    }

    @Test
    void morePlayersDecideWhenTheTargetsAreEqual() {
        var result = TimeEndResolver.resolve(List.of(
                new Standing("A", true, 3, 0, 0),
                new Standing("B", true, 2, 0, 0)
        ), DEFAULT);
        assertEquals("A", result.winnerId());
        assertEquals(Criterion.PLAYERS, result.decidedBy());
    }

    @Test
    void anUnresolvedTieIsADraw() {
        var result = TimeEndResolver.resolve(List.of(
                new Standing("A", true, 2, 0, 0),
                new Standing("B", true, 2, 0, 0)
        ), DEFAULT);
        assertTrue(result.isDraw());
        assertNull(result.winnerId());
        assertNull(result.decidedBy());
        assertEquals(List.of("A", "B"), result.tied());
    }

    @Test
    void onlyTheTeamsStillTiedAreReported() {
        var result = TimeEndResolver.resolve(List.of(
                new Standing("A", true, 2, 0, 0),
                new Standing("B", true, 2, 0, 0),
                new Standing("C", false, 4, 0, 0)
        ), DEFAULT);
        assertTrue(result.isDraw());
        assertEquals(List.of("A", "B"), result.tied());
    }

    @Test
    void noCriteriaMeansDraw() {
        var result = TimeEndResolver.resolve(List.of(
                new Standing("A", true, 5, 0, 0),
                new Standing("B", false, 1, 0, 0)
        ), List.of());
        assertTrue(result.isDraw());
        assertEquals(List.of("A", "B"), result.tied());
    }

    @Test
    void aSingleStandingWins() {
        var result = TimeEndResolver.resolve(List.of(new Standing("A", false, 0, 0, 0)), DEFAULT);
        assertEquals("A", result.winnerId());
        assertNull(result.decidedBy());
        assertTrue(result.tied().isEmpty());
    }

    @Test
    void noStandingsIsADraw() {
        var result = TimeEndResolver.resolve(List.of(), DEFAULT);
        assertTrue(result.isDraw());
        assertTrue(result.tied().isEmpty());
    }

    @Test
    void killsAndFinalKills() {
        var kills = TimeEndResolver.resolve(List.of(
                new Standing("A", true, 2, 5, 0),
                new Standing("B", true, 2, 7, 0)
        ), List.of(Criterion.KILLS));
        assertEquals("B", kills.winnerId());
        assertEquals(Criterion.KILLS, kills.decidedBy());

        var finals = TimeEndResolver.resolve(List.of(
                new Standing("A", true, 2, 5, 2),
                new Standing("B", true, 2, 7, 2)
        ), List.of(Criterion.FINAL_KILLS));
        assertTrue(finals.isDraw());
    }

    @Test
    void criteriaAreAppliedInOrderAndLaterOnesBreakTies() {
        var result = TimeEndResolver.resolve(List.of(
                new Standing("A", true, 2, 3, 1),
                new Standing("B", true, 2, 3, 2),
                new Standing("C", true, 1, 9, 9)
        ), List.of(Criterion.TARGET, Criterion.PLAYERS, Criterion.KILLS, Criterion.FINAL_KILLS));
        assertEquals("B", result.winnerId());
        assertEquals(Criterion.FINAL_KILLS, result.decidedBy());
    }

    @Test
    void parseCriteria() {
        var warnings = new ArrayList<String>();
        var criteria = TimeEndResolver.parseCriteria(Arrays.asList("Target", "final_kills", "bogus", "target"), warnings);
        assertEquals(List.of(Criterion.TARGET, Criterion.FINAL_KILLS), criteria);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("bogus"), warnings.get(0));
    }

    @Test
    void parseCriteriaNullAndEmpty() {
        var warnings = new ArrayList<String>();
        assertEquals(List.of(Criterion.TARGET, Criterion.PLAYERS), TimeEndResolver.parseCriteria(null, warnings));
        assertEquals(List.of(), TimeEndResolver.parseCriteria(List.of(), warnings));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void criterionFromConfig() {
        assertEquals(Criterion.FINAL_KILLS, Criterion.fromConfig("final-kills").orElseThrow());
        assertEquals(Criterion.FINAL_KILLS, Criterion.fromConfig("FINAL_KILLS").orElseThrow());
        assertEquals(Criterion.FINAL_KILLS, Criterion.fromConfig("final kills").orElseThrow());
        assertEquals(Criterion.PLAYERS, Criterion.fromConfig(" players ").orElseThrow());
        assertEquals("final-kills", Criterion.FINAL_KILLS.configName());
        assertTrue(Criterion.fromConfig("bogus").isEmpty());
        assertTrue(Criterion.fromConfig(null).isEmpty());
    }
}
