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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeRulesTest {
    @Test
    void defaultId() {
        assertEquals("4v4", ModeRules.defaultId(2, 4));
        assertEquals("1v1v1", ModeRules.defaultId(3, 1));
        assertEquals("2v2v2v2", ModeRules.defaultId(4, 2));
    }

    @Test
    void defaultMinPlayers() {
        assertEquals(2, ModeRules.defaultMinPlayers(1));
        assertEquals(6, ModeRules.defaultMinPlayers(3));
        assertEquals(8, ModeRules.defaultMinPlayers(4));
    }

    @Test
    void clampMinPlayers() {
        assertEquals(8, ModeRules.clampMinPlayers(8, 2, 4));
        assertEquals(8, ModeRules.clampMinPlayers(20, 2, 4));
        assertEquals(2, ModeRules.clampMinPlayers(1, 2, 4));
        assertEquals(2, ModeRules.clampMinPlayers(0, 4, 4));
        assertEquals(2, ModeRules.clampMinPlayers(2, 2, 1));
        assertEquals(2, ModeRules.clampMinPlayers(5, 2, 1));
    }

    @Test
    void ceilDiv() {
        assertEquals(3, ModeRules.ceilDiv(9, 4));
        assertEquals(2, ModeRules.ceilDiv(8, 4));
        assertEquals(0, ModeRules.ceilDiv(0, 4));
    }

    @Test
    void partyFits() {
        var mode = new ModeDefinition("4v4", "4v4", 2, 4, 8, List.of());
        assertTrue(ModeRules.partyFits(mode, 4));
        assertFalse(ModeRules.partyFits(mode, 5));
    }

    @Test
    void definitionValidation() {
        assertThrows(IllegalArgumentException.class, () -> new ModeDefinition(" ", "x", 2, 2, 4, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ModeDefinition("x", "x", 0, 2, 4, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ModeDefinition("x", "x", 2, 0, 4, List.of()));
        var mode = new ModeDefinition("x", "x", 3, 2, 4, List.of("Alpha"));
        assertEquals(6, mode.maxPlayers());
        assertTrue(mode.allowsArena("alpha"));
        assertFalse(mode.allowsArena("Beta"));
        assertTrue(new ModeDefinition("y", "y", 2, 2, 4, List.of()).allowsArena("anything"));
    }
}
