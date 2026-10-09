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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamOrderTest {
    private static final List<String> TEAMS = List.of("Red", "Blue", "Green", "Yellow");

    @Test
    void priorityFirstThenArenaOrder() {
        assertEquals(List.of("Green", "Red", "Blue", "Yellow"),
                TeamOrder.order(TEAMS, List.of("green", "red", "Purple", "RED")));
    }

    @Test
    void emptyPriorityKeepsArenaOrder() {
        assertEquals(TEAMS, TeamOrder.order(TEAMS, List.of()));
    }

    @Test
    void nullAndWhitespaceEntries() {
        var priority = new ArrayList<String>();
        priority.add(null);
        priority.add("  yellow ");
        assertEquals(List.of("Yellow", "Red", "Blue", "Green"), TeamOrder.order(TEAMS, priority));
    }
}
