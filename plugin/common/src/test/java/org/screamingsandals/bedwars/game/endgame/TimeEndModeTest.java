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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeEndModeTest {
    @Test
    void draw() {
        assertEquals(TimeEndMode.DRAW, TimeEndMode.fromConfig("draw").orElseThrow());
        assertEquals(TimeEndMode.DRAW, TimeEndMode.fromConfig(" DRAW ").orElseThrow());
    }

    @Test
    void tieBreakSpellings() {
        assertEquals(TimeEndMode.TIE_BREAK, TimeEndMode.fromConfig("Tie-Break").orElseThrow());
        assertEquals(TimeEndMode.TIE_BREAK, TimeEndMode.fromConfig("tie_break").orElseThrow());
        assertEquals(TimeEndMode.TIE_BREAK, TimeEndMode.fromConfig("tiebreak").orElseThrow());
        assertEquals(TimeEndMode.TIE_BREAK, TimeEndMode.fromConfig("  tie-break").orElseThrow());
    }

    @Test
    void unknownValuesAreEmpty() {
        assertTrue(TimeEndMode.fromConfig("x").isEmpty());
        assertTrue(TimeEndMode.fromConfig("").isEmpty());
        assertTrue(TimeEndMode.fromConfig(null).isEmpty());
    }
}
