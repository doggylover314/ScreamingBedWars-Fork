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

class FlightGeometryTest {
    private static final double EPS = 1e-9;

    @Test
    void largeArena() {
        var arena = FlightBox.of(0, 0, 0, 100, 255, 100);
        var box = FlightGeometry.flightBox(arena, 64, 4, 15);
        assertEquals(4, box.minX(), EPS);
        assertEquals(96, box.maxX(), EPS);
        assertEquals(52, box.minY(), EPS);
        assertEquals(94, box.maxY(), EPS);
        assertEquals(4, box.minZ(), EPS);
        assertEquals(96, box.maxZ(), EPS);
        assertEquals(79, FlightGeometry.cruiseY(box, 64, 15), EPS);
        assertEquals(12, FlightGeometry.ringRadius(box), EPS);
    }

    @Test
    void smallArena() {
        var arena = FlightBox.of(0, 60, 0, 10, 70, 10);
        var box = FlightGeometry.flightBox(arena, 64, 4, 15);
        assertEquals(4, box.minX(), EPS);
        assertEquals(6, box.maxX(), EPS);
        assertEquals(62, box.minY(), EPS);
        assertEquals(68, box.maxY(), EPS);
        assertEquals(4, box.minZ(), EPS);
        assertEquals(6, box.maxZ(), EPS);
        assertEquals(68, FlightGeometry.cruiseY(box, 64, 15), EPS);
        assertEquals(0.5, FlightGeometry.ringRadius(box), EPS);
    }

    @Test
    void negativeSettingsAreTreatedAsZero() {
        var arena = FlightBox.of(0, 0, 0, 100, 255, 100);
        var box = FlightGeometry.flightBox(arena, 64, -10, -5);
        assertEquals(0, box.minX(), EPS);
        assertEquals(100, box.maxX(), EPS);
        assertEquals(52, box.minY(), EPS);
        assertEquals(79, box.maxY(), EPS); // 64 + 0 + ABOVE_CRUISE
    }

    @Test
    void degenerateBoxHasNoRing() {
        var box = FlightBox.of(5, 5, 5, 5, 5, 5);
        assertEquals(0, FlightGeometry.ringRadius(box), EPS);
    }
}
