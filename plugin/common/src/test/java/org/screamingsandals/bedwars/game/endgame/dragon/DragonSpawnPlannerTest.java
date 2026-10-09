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

class DragonSpawnPlannerTest {
    private static final double EPS = 1e-9;

    @Test
    void zeroDragonsGiveNoPoints() {
        var box = FlightBox.of(-100, 0, -100, 100, 200, 100);
        assertTrue(DragonSpawnPlanner.plan(new Vec3(0, 100, 0), 0, 10, box).isEmpty());
        assertTrue(DragonSpawnPlanner.plan(new Vec3(0, 100, 0), -3, 10, box).isEmpty());
    }

    @Test
    void oneDragonSpawnsAtTheCentre() {
        var box = FlightBox.of(-100, 0, -100, 100, 200, 100);
        var points = DragonSpawnPlanner.plan(new Vec3(1, 100, 2), 1, 10, box);
        assertEquals(1, points.size());
        assertEquals(new Vec3(1, 100, 2), points.get(0));
    }

    @Test
    void fourDragonsFormARing() {
        var box = FlightBox.of(-100, 0, -100, 100, 200, 100);
        var points = DragonSpawnPlanner.plan(new Vec3(0, 100, 0), 4, 10, box);
        assertEquals(4, points.size());
        assertPoint(points.get(0), 10, 100, 0);
        assertPoint(points.get(1), 0, 100, 10);
        assertPoint(points.get(2), -10, 100, 0);
        assertPoint(points.get(3), 0, 100, -10);
    }

    @Test
    void pointsAreClampedIntoTheBox() {
        var box = FlightBox.of(-2, 0, -2, 2, 200, 2);
        for (var point : DragonSpawnPlanner.plan(new Vec3(0, 100, 0), 7, 10, box)) {
            assertTrue(box.contains(point), point.toString());
        }
    }

    @Test
    void zeroRadiusStacksTheDragonsAtTheCentre() {
        var box = FlightBox.of(-100, 0, -100, 100, 200, 100);
        var points = DragonSpawnPlanner.plan(new Vec3(3, 100, 4), 3, 0, box);
        assertEquals(3, points.size());
        points.forEach(point -> assertEquals(new Vec3(3, 100, 4), point));
    }

    private static void assertPoint(Vec3 actual, double x, double y, double z) {
        assertEquals(x, actual.x(), EPS);
        assertEquals(y, actual.y(), EPS);
        assertEquals(z, actual.z(), EPS);
    }
}
