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

class BlockBoxTest {
    @Test
    void ofNormalisesMinAndMax() {
        var box = BlockBox.of(5, 10, -3, -2, 4, 7);
        assertEquals(new BlockBox(-2, 4, -3, 5, 10, 7), box);
        assertEquals(616L, box.volume()); // 8 * 7 * 11
    }

    @Test
    void ofDoublesFloorsEveryCoordinate() {
        assertEquals(new BlockBox(3, 64, -1, 10, 70, 2), BlockBox.ofDoubles(10.7, 64.2, -0.5, 3.1, 70.9, 2.0));
    }

    @Test
    void intersectsIsInclusive() {
        var a = new BlockBox(0, 0, 0, 10, 10, 10);
        assertTrue(a.intersects(new BlockBox(10, 10, 10, 20, 20, 20))); // touching
        assertFalse(a.intersects(new BlockBox(11, 0, 0, 20, 10, 10)));
        assertFalse(a.intersects(new BlockBox(0, 11, 0, 10, 20, 10)));
        assertFalse(a.intersects(new BlockBox(0, 0, 11, 10, 10, 20)));
        assertTrue(a.intersects(new BlockBox(-5, -5, -5, 0, 0, 0)));
        assertTrue(a.intersects(new BlockBox(3, 3, 3, 4, 4, 4))); // inside
    }

    @Test
    void containsPointFloorsTheCoordinates() {
        var box = new BlockBox(-1, 0, 0, 0, 0, 0);
        assertTrue(box.containsPoint(-0.5, 0.2, 0.9));
        assertTrue(box.containsPoint(0.99, 0, 0));
        assertFalse(box.containsPoint(1.0, 0, 0));
        assertFalse(box.containsPoint(-1.01, 0, 0));
    }

    @Test
    void containsBlockAndBox() {
        var outer = new BlockBox(0, 0, 0, 10, 10, 10);
        assertTrue(outer.contains(new BlockBox(2, 2, 2, 8, 8, 8)));
        assertTrue(outer.contains(outer));
        assertFalse(outer.contains(new BlockBox(2, 2, 2, 8, 8, 11))); // one face sticks out
        assertFalse(outer.contains(new BlockBox(-1, 2, 2, 8, 8, 8)));
        assertTrue(outer.contains(0, 0, 0));
        assertTrue(outer.contains(10, 10, 10));
        assertFalse(outer.contains(11, 0, 0));
    }

    @Test
    void shiftMovesBothCorners() {
        var box = new BlockBox(0, 5, 10, 3, 8, 12).shift(new BlockOffset(1, -2, 3));
        assertEquals(new BlockBox(1, 3, 13, 4, 6, 15), box);
    }

    @Test
    void shiftSaturatesInsteadOfOverflowing() {
        var box = new BlockBox(0, 0, 0, 10, 10, 10).shift(new BlockOffset(Integer.MAX_VALUE, 0, 0));
        assertEquals(Integer.MAX_VALUE, box.maxX());
        assertEquals(Integer.MAX_VALUE, box.minX());
    }

    @Test
    void withYReplacesTheVerticalRange() {
        assertEquals(new BlockBox(0, 4, 0, 5, 9, 5), new BlockBox(0, 0, 0, 5, 300, 5).withY(4, 9));
    }

    @Test
    void stringsAreSlashSeparated() {
        var box = new BlockBox(1, 2, 3, 4, 6, 9);
        assertEquals("1/2/3", box.minString());
        assertEquals("4/6/9", box.maxString());
        assertEquals("4 x 5 x 7", box.sizeString());
    }
}
