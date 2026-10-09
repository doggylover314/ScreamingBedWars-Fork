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

package org.screamingsandals.bedwars.setup.clone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CloneMathTest {
    @Test
    void percent() {
        assertEquals(100, CloneMath.percent(0, 0));
        assertEquals(25, CloneMath.percent(50, 200));
        assertEquals(99, CloneMath.percent(199, 200));
        assertEquals(100, CloneMath.percent(200, 200));
        assertEquals(100, CloneMath.percent(300, 200));
        assertEquals(0, CloneMath.percent(-5, 200));
        assertEquals(0, CloneMath.percent(1, 1_000_000));
    }

    @Test
    void etaSeconds() {
        assertEquals(1, CloneMath.etaSeconds(0, 1000, 0, 100));
        assertEquals(1, CloneMath.etaSeconds(500, 1000, 1000, 100));
        assertEquals(18, CloneMath.etaSeconds(100, 1000, 2000, 100));
        assertEquals(0, CloneMath.etaSeconds(100, 100, 5000, 100));
        assertEquals(0, CloneMath.etaSeconds(200, 100, 5000, 100));
    }

    @Test
    void etaUsesTheConfiguredBudgetBeforeTheFirstSecond() {
        // 40000 blocks left at 100 blocks/tick = 2000 blocks/s -> 20 s, even though 999 ms measured something else
        assertEquals(20, CloneMath.etaSeconds(10, 40010, 999, 100));
    }

    @Test
    void minimumSeconds() {
        assertEquals(6, CloneMath.minimumSeconds(410000, 4096));
        assertEquals(0, CloneMath.minimumSeconds(0, 4096));
        assertEquals(1, CloneMath.minimumSeconds(1, 4096));
    }
}
