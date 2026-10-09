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

package org.screamingsandals.bedwars.game.upgrade.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeMathTest {
    private static final double EPS = 1e-9;

    @Test
    void delta() {
        assertEquals(0.5, ForgeMath.delta(1, 0.5), EPS);
        assertEquals(4.0, ForgeMath.delta(2, 2.0), EPS);
        assertEquals(0.0, ForgeMath.delta(1, -1), EPS);
    }

    @Test
    void rebaseKeepsOtherChanges() {
        assertEquals(1.5, ForgeMath.rebase(1.0, 0, 0.5), EPS);
        assertEquals(1.7, ForgeMath.rebase(1.2, 0, 0.5), EPS); // a legacy +0.2 is kept
        assertEquals(2.2, ForgeMath.rebase(1.7, 0.5, 1.0), EPS);
        assertEquals(1.2, ForgeMath.rebase(2.2, 1.0, 0), EPS);
    }

    @Test
    void emeraldInterval() {
        assertTrue(ForgeMath.shouldSpawnEmerald(60, 60));
        assertFalse(ForgeMath.shouldSpawnEmerald(59, 60));
        assertTrue(ForgeMath.shouldSpawnEmerald(120, 60));
        assertFalse(ForgeMath.shouldSpawnEmerald(0, 60));
        assertFalse(ForgeMath.shouldSpawnEmerald(60, 0));
        assertFalse(ForgeMath.shouldSpawnEmerald(60, -5));
    }
}
