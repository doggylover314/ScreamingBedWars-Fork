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

package org.screamingsandals.bedwars.setup;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TeamNamingTest {
    /** Declaration order of TeamColorImpl. */
    static final List<String> ALL = List.of("BLACK", "BLUE", "GREEN", "RED", "MAGENTA", "ORANGE", "LIGHT_GRAY", "GRAY",
            "LIGHT_BLUE", "LIME", "CYAN", "PINK", "YELLOW", "WHITE", "BROWN");

    @Test
    void defaultNamesAreSingleWords() {
        assertEquals("Red", TeamNaming.defaultName("RED"));
        assertEquals("LightBlue", TeamNaming.defaultName("LIGHT_BLUE"));
        assertEquals("LightGray", TeamNaming.defaultName("light_gray"));
        assertEquals("Magenta", TeamNaming.defaultName("MAGENTA"));
    }

    @Test
    void preferredColoursComeFirst() {
        assertEquals(List.of("RED", "BLUE", "GREEN", "YELLOW", "CYAN", "WHITE", "PINK", "GRAY",
                        "BLACK", "MAGENTA", "ORANGE", "LIGHT_GRAY", "LIGHT_BLUE", "LIME", "BROWN"),
                TeamNaming.orderByPreference(ALL));
    }

    @Test
    void preferredColoursMissingFromTheInputAreSkipped() {
        assertEquals(List.of("BLUE", "GRAY", "LIME"), TeamNaming.orderByPreference(List.of("LIME", "GRAY", "BLUE")));
    }

    @Test
    void firstUnusedIsCaseInsensitive() {
        assertEquals("GREEN", TeamNaming.firstUnused(ALL, Set.of("red", "BLUE")));
        assertEquals("RED", TeamNaming.firstUnused(ALL, List.of()));
    }

    @Test
    void allUsedGivesNull() {
        assertNull(TeamNaming.firstUnused(ALL, ALL));
    }
}
