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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TeamAssignmentTest {

    @Test
    void wholePartyReservesRoomWhenATeamCanHoldIt() {
        // Red 2/4, Blue 1/4: a party of 3 is sent to Blue, not to the lowest team that could hold one player
        var slots = List.of(new TeamSlot("Red", 2, 4, 0), new TeamSlot("Blue", 1, 4, 1), new TeamSlot("Green", 0, 4, -1));
        assertEquals("Blue", TeamAssignment.chooseTeamForGroup(slots, 3, 1));
    }

    @Test
    void wantedSizeBiggerThanEveryTeamFallsBackToWhoIsReallyHere() {
        // teams of 2, party of 3 online but only the joining player is in this game
        var slots = List.of(new TeamSlot("Red", 0, 2, -1), new TeamSlot("Blue", 0, 2, -1));
        assertEquals("Red", TeamAssignment.chooseTeamForGroup(slots, 3, 1));
        assertNull(TeamAssignment.chooseTeamForGroup(slots, 3, 3));
    }

    @Test
    void fallbackPrefersTheBiggestGroupThatFits() {
        // Red has 1 free seat, Blue 2: a wanted group of 4 with 2 really present goes to Blue (room for 2)
        var slots = List.of(new TeamSlot("Red", 1, 2, 0), new TeamSlot("Blue", 0, 2, 1));
        assertEquals("Blue", TeamAssignment.chooseTeamForGroup(slots, 4, 2));
    }

    @Test
    void minimumSizeIsNeverUndercutAndAtLeastOne() {
        var slots = List.of(new TeamSlot("Red", 2, 2, 0), new TeamSlot("Blue", 2, 2, 1));
        assertNull(TeamAssignment.chooseTeamForGroup(slots, 1, 1)); // everything full
        assertNull(TeamAssignment.chooseTeamForGroup(slots, 0, 0));
    }

    @Test
    void wantedSmallerThanMinimumUsesTheMinimum() {
        var slots = List.of(new TeamSlot("Red", 1, 2, 0), new TeamSlot("Blue", 0, 3, 1));
        assertEquals("Blue", TeamAssignment.chooseTeamForGroup(slots, 1, 2));
    }
}
