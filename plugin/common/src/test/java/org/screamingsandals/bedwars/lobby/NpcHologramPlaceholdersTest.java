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
import org.screamingsandals.bedwars.game.mode.ModeStats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NpcHologramPlaceholdersTest {
    @Test
    void signatureOfNonModeNpcOnlyDependsOnTotalPlayers() {
        var base = NpcHologramPlaceholders.signature("OPEN_GAMES_INVENTORY", "all", 10, null);
        assertEquals(base, NpcHologramPlaceholders.signature("OPEN_GAMES_INVENTORY", "all", 10, null));
        assertNotEquals(base, NpcHologramPlaceholders.signature("OPEN_GAMES_INVENTORY", "all", 11, null));
    }

    @Test
    void signatureOfModeNpcChangesWithEveryModeCount() {
        var base = NpcHologramPlaceholders.signature("JOIN_MODE", "4v4", 10, new ModeStats(2, 3, 4));
        assertEquals(base, NpcHologramPlaceholders.signature("JOIN_MODE", "4v4", 10, new ModeStats(2, 3, 4)));
        assertNotEquals(base, NpcHologramPlaceholders.signature("JOIN_MODE", "4v4", 10, new ModeStats(3, 3, 4)));
        assertNotEquals(base, NpcHologramPlaceholders.signature("JOIN_MODE", "4v4", 10, new ModeStats(2, 4, 4)));
        assertNotEquals(base, NpcHologramPlaceholders.signature("JOIN_MODE", "4v4", 10, new ModeStats(2, 3, 5)));
        assertNotEquals(base, NpcHologramPlaceholders.signature("JOIN_MODE", "4v4", 11, new ModeStats(2, 3, 4)));
    }
}
