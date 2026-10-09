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

package org.screamingsandals.bedwars.game.endgame.dragon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.screamingsandals.bedwars.game.endgame.dragon.DragonBlockPolicy.Decision.DESTROY;
import static org.screamingsandals.bedwars.game.endgame.dragon.DragonBlockPolicy.Decision.DESTROY_AND_RECORD;
import static org.screamingsandals.bedwars.game.endgame.dragon.DragonBlockPolicy.Decision.KEEP;
import static org.screamingsandals.bedwars.game.endgame.dragon.DragonBlockPolicy.Mode;
import static org.screamingsandals.bedwars.game.endgame.dragon.DragonBlockPolicy.decide;

class DragonBlockPolicyTest {
    @Test
    void outsideImmuneAndProtectedBlocksAreAlwaysKept() {
        for (var mode : Mode.values()) {
            for (boolean placed : new boolean[]{false, true}) {
                for (boolean snapshot : new boolean[]{false, true}) {
                    assertEquals(KEEP, decide(mode, false, false, false, placed, snapshot), "outside " + mode);
                    assertEquals(KEEP, decide(mode, true, true, false, placed, snapshot), "immune " + mode);
                    assertEquals(KEEP, decide(mode, true, false, true, placed, snapshot), "protected " + mode);
                }
            }
        }
    }

    @Test
    void noneKeepsEverything() {
        assertEquals(KEEP, decide(Mode.NONE, true, false, false, true, true));
        assertEquals(KEEP, decide(Mode.NONE, true, false, false, false, true));
    }

    @Test
    void placedOnlyDestroysBlocksPlacedDuringTheGame() {
        assertEquals(DESTROY, decide(Mode.PLACED, true, false, false, true, false));
        assertEquals(KEEP, decide(Mode.PLACED, true, false, false, false, true));
    }

    @Test
    void allDestroysAndRecordsOriginalBlocks() {
        assertEquals(DESTROY, decide(Mode.ALL, true, false, false, true, false));
        assertEquals(DESTROY_AND_RECORD, decide(Mode.ALL, true, false, false, false, true));
        assertEquals(KEEP, decide(Mode.ALL, true, false, false, false, false));
    }

    @Test
    void modeFromConfig() {
        assertEquals(Mode.ALL, Mode.fromConfig("ALL").orElseThrow());
        assertEquals(Mode.PLACED, Mode.fromConfig(" placed_only ").orElseThrow());
        assertEquals(Mode.PLACED, Mode.fromConfig("player-placed").orElseThrow());
        assertEquals(Mode.PLACED, Mode.fromConfig("placed").orElseThrow());
        assertEquals(Mode.NONE, Mode.fromConfig("off").orElseThrow());
        assertEquals(Mode.NONE, Mode.fromConfig("None").orElseThrow());
        assertEquals(Mode.NONE, Mode.fromConfig("false").orElseThrow());
        assertTrue(Mode.fromConfig("x").isEmpty());
        assertTrue(Mode.fromConfig("").isEmpty());
        assertTrue(Mode.fromConfig(null).isEmpty());
    }
}
