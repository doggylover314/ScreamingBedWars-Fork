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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockOffsetTest {
    @Test
    void toMinCornerSubtractsTheSourceMinimum() {
        var source = BlockBox.ofDoubles(100.7, 64.2, -50.3, 20.1, 100.0, 30.9);
        assertEquals(new BlockBox(20, 64, -51, 100, 100, 30), source);

        var offset = BlockOffset.toMinCorner(source, 1000, 70, 1000);
        assertEquals(new BlockOffset(980, 6, 1051), offset);
        assertFalse(offset.isZero());
    }

    @Test
    void sameCornerIsZero() {
        var source = new BlockBox(5, 6, 7, 10, 10, 10);
        assertTrue(BlockOffset.toMinCorner(source, 5, 6, 7).isZero());
    }

    @Test
    void shiftingTheSourceByTheOffsetPutsItsMinimumOnTheTarget() {
        var source = new BlockBox(-30, 60, 12, 40, 90, 80);
        var moved = source.shift(BlockOffset.toMinCorner(source, 500, -10, -200));
        assertEquals(500, moved.minX());
        assertEquals(-10, moved.minY());
        assertEquals(-200, moved.minZ());
        assertEquals(source.volume(), moved.volume());
    }

    @Test
    void extremeTargetsSaturate() {
        var source = new BlockBox(-100, 0, 0, 0, 10, 10);
        var offset = BlockOffset.toMinCorner(source, Integer.MAX_VALUE, 0, 0);
        assertEquals(Integer.MAX_VALUE, offset.dx());
    }
}
