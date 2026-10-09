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

package org.screamingsandals.bedwars.lobby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcHologramTextTest {
    @Test
    void ownPlaceholdersAreDetected() {
        assertTrue(NpcHologramText.hasOwnPlaceholders("<mode>"));
        assertTrue(NpcHologramText.hasOwnPlaceholders("<gray><mode-players> playing"));
        assertTrue(NpcHologramText.hasOwnPlaceholders("<all-players>"));
        assertTrue(NpcHologramText.hasOwnPlaceholders("x <mode-id> y"));
        assertTrue(NpcHologramText.hasOwnPlaceholders("<mode-waiting>/<mode-playing>/<mode-arenas>"));
    }

    @Test
    void lookalikesAreNotOwnPlaceholders() {
        assertFalse(NpcHologramText.hasOwnPlaceholders("<model>"));
        assertFalse(NpcHologramText.hasOwnPlaceholders("<modes>"));
        assertFalse(NpcHologramText.hasOwnPlaceholders("plain"));
        assertFalse(NpcHologramText.hasOwnPlaceholders("<mode"));
        assertFalse(NpcHologramText.hasOwnPlaceholders(""));
    }

    @Test
    void papiPlaceholdersAreDetected() {
        assertTrue(NpcHologramText.hasPapiPlaceholders("%bedwars_mode_4v4_players%"));
        assertTrue(NpcHologramText.hasPapiPlaceholders("<gray>Online: %server_online%"));
    }

    @Test
    void percentSignsAloneAreNotPapi() {
        assertFalse(NpcHologramText.hasPapiPlaceholders("100%"));
        assertFalse(NpcHologramText.hasPapiPlaceholders("50 % off %"));
        assertFalse(NpcHologramText.hasPapiPlaceholders("plain"));
    }

    @Test
    void isDynamicCombinesBoth() {
        assertTrue(NpcHologramText.isDynamic("<mode>"));
        assertTrue(NpcHologramText.isDynamic("%x%"));
        assertFalse(NpcHologramText.isDynamic("<yellow><bold>CLICK TO PLAY"));
    }

    @Test
    void nullIsNeverDynamic() {
        assertFalse(NpcHologramText.hasOwnPlaceholders(null));
        assertFalse(NpcHologramText.hasPapiPlaceholders(null));
        assertFalse(NpcHologramText.isDynamic(null));
    }

    @Test
    void ownPlaceholderListMatchesTheDocumentedNames() {
        assertEquals(7, NpcHologramText.OWN_PLACEHOLDERS.size());
        for (var name : NpcHologramText.OWN_PLACEHOLDERS) {
            assertTrue(NpcHologramText.hasOwnPlaceholders("<" + name + ">"), name);
        }
    }
}
