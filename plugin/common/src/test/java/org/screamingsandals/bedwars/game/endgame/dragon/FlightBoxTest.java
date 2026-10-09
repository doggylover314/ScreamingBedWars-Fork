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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightBoxTest {
    private static final double EPS = 1e-9;

    @Test
    void ofNormalisesCorners() {
        var box = FlightBox.of(10, 5, 10, 0, 0, 0);
        assertEquals(new FlightBox(0, 0, 0, 10, 5, 10), box);
    }

    @Test
    void shrinkMovesFacesInwards() {
        var box = FlightBox.of(0, 0, 0, 10, 10, 10).shrink(4, 2);
        assertEquals(4, box.minX(), EPS);
        assertEquals(6, box.maxX(), EPS);
        assertEquals(2, box.minY(), EPS);
        assertEquals(8, box.maxY(), EPS);
        assertEquals(4, box.minZ(), EPS);
        assertEquals(6, box.maxZ(), EPS);
    }

    @Test
    void shrinkCollapsesAnInvertedAxisToItsMiddle() {
        var box = FlightBox.of(0, 0, 0, 10, 10, 10).shrink(6, 0);
        assertEquals(5, box.minX(), EPS);
        assertEquals(5, box.maxX(), EPS);
        assertEquals(5, box.minZ(), EPS);
        assertEquals(5, box.maxZ(), EPS);
        assertEquals(0, box.minY(), EPS);
        assertEquals(10, box.maxY(), EPS);
    }

    @Test
    void withYRangeIntersects() {
        var box = FlightBox.of(0, 0, 0, 10, 100, 10).withYRange(20, 30);
        assertEquals(20, box.minY(), EPS);
        assertEquals(30, box.maxY(), EPS);
        assertEquals(0, box.minX(), EPS);
        assertEquals(10, box.maxX(), EPS);
    }

    @Test
    void withYRangeCollapsesWhenTheIntersectionIsEmpty() {
        var box = FlightBox.of(0, 0, 0, 10, 10, 10).withYRange(20, 30);
        assertEquals(10, box.minY(), EPS); // clamp of the middle (25) into [0, 10]
        assertEquals(10, box.maxY(), EPS);

        var low = FlightBox.of(0, 50, 0, 10, 60, 10).withYRange(0, 10);
        assertEquals(50, low.minY(), EPS);
        assertEquals(50, low.maxY(), EPS);
    }

    @Test
    void clampPullsPointsIntoTheBox() {
        var box = FlightBox.of(0, 0, 0, 10, 10, 10);
        assertEquals(new Vec3(0, 10, 10), box.clamp(new Vec3(-5, 50, 200)));
        assertEquals(new Vec3(3, 4, 5), box.clamp(new Vec3(3, 4, 5)));
    }

    @Test
    void containsIsInclusive() {
        var box = FlightBox.of(0, 0, 0, 10, 10, 10);
        assertTrue(box.contains(new Vec3(0, 0, 0)));
        assertTrue(box.contains(new Vec3(10, 10, 10)));
        assertTrue(box.contains(new Vec3(5, 5, 5)));
        assertFalse(box.contains(new Vec3(10.01, 5, 5)));
        assertFalse(box.contains(new Vec3(5, -0.01, 5)));
    }

    @Test
    void centreAndExtent() {
        var box = FlightBox.of(0, 10, 20, 10, 30, 60);
        assertEquals(new Vec3(5, 20, 40), box.centre());
        assertEquals(10, box.width(), EPS);
        assertEquals(40, box.depth(), EPS);
    }
}
