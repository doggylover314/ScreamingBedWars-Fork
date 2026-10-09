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

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonSteeringTest {
    private static final double EPS = 1e-9;
    private static final DragonSteering.Params P = DragonSteering.Params.of(1.0, 10.0);
    private static final FlightBox B = FlightBox.of(-50, 60, -50, 50, 120, 50);

    @Test
    void paramsOf() {
        assertEquals(1.0, P.speed(), EPS);
        assertEquals(10.0, P.maxTurnPerTick(), EPS);
        assertEquals(3.0, P.passDistance(), EPS);
        assertEquals(25, P.overshootTicks());
        assertEquals(0.6, P.maxClimbPerTick(), EPS);
        assertEquals(5.0, P.wanderReachDistance(), EPS);
        assertEquals(200, P.wanderMaxTicks());
        assertEquals(160, P.maxChaseTicks());
        assertEquals(0.35, DragonSteering.Params.of(0.5, 10.0).maxClimbPerTick(), EPS);
    }

    @Test
    void yawTowards() {
        assertEquals(0, DragonSteering.yawTowards(0, 1), EPS);
        assertEquals(90, DragonSteering.yawTowards(-1, 0), EPS);
        assertEquals(-90, DragonSteering.yawTowards(1, 0), EPS);
        assertEquals(-180, DragonSteering.yawTowards(0, -1), EPS);
    }

    @Test
    void forward() {
        assertVec(DragonSteering.forward(0), 0, 0, 1);
        assertVec(DragonSteering.forward(90), -1, 0, 0);
        assertVec(DragonSteering.forward(-90), 1, 0, 0);
    }

    @Test
    void wrapDegrees() {
        assertEquals(-180, DragonSteering.wrapDegrees(180), EPS);
        assertEquals(170, DragonSteering.wrapDegrees(-190), EPS);
        assertEquals(-180, DragonSteering.wrapDegrees(540), EPS);
        assertEquals(0, DragonSteering.wrapDegrees(360), EPS);
        assertEquals(-180, DragonSteering.wrapDegrees(-180), EPS);
    }

    @Test
    void turnTowards() {
        assertEquals(10, DragonSteering.turnTowards(0, 30, 10), EPS);
        assertEquals(-180, DragonSteering.turnTowards(170, -170, 10), EPS);
        assertEquals(5, DragonSteering.turnTowards(0, 5, 10), EPS);
        assertEquals(-10, DragonSteering.turnTowards(0, -30, 10), EPS);
    }

    @Test
    void entityYawFacesTheTravelDirectionHeadFirst() {
        assertEquals(-180f, DragonSteering.entityYaw(0), 1e-4f);
        assertEquals(-90f, DragonSteering.entityYaw(90), 1e-4f);
    }

    @Test
    void straightChase() {
        var state = new DragonSteering.State(0);
        var step = DragonSteering.step(state, new Vec3(0, 100, 0), new Vec3(0, 100, 40), B, P, new Random(42));
        assertEquals(DragonSteering.Mode.CHASE, step.mode());
        assertVec(step.position(), 0, 100, 1);
        assertEquals(0, step.headingYaw(), EPS);
        assertFalse(step.clampedHorizontally());
    }

    @Test
    void turnRateLimitsTheHeadingChange() {
        var state = new DragonSteering.State(0);
        var target = new Vec3(0, 100, -40);
        var position = new Vec3(0, 100, 0);
        var random = new Random(42);

        var step = DragonSteering.step(state, position, target, B, P, random);
        assertEquals(-10, step.headingYaw(), EPS);
        position = step.position();

        double previous = position.horizontalDistance(target);
        boolean decreased = false;
        for (int i = 0; i < 40 && !decreased; i++) {
            step = DragonSteering.step(state, position, target, B, P, random);
            position = step.position();
            double distance = position.horizontalDistance(target);
            decreased = distance < previous;
            previous = distance;
        }
        assertTrue(decreased, "the dragon must turn around and approach the target");
    }

    @Test
    void climbIsLimited() {
        var state = new DragonSteering.State(0);
        var step = DragonSteering.step(state, new Vec3(0, 80, 0), new Vec3(0, 110, 30), B, P, new Random(42));
        assertEquals(80.6, step.position().y(), EPS);
    }

    @Test
    void passingTheTargetStartsAnOvershoot() {
        var state = new DragonSteering.State(0);
        var target = new Vec3(0, 100, 2);
        var random = new Random(42);

        var step = DragonSteering.step(state, new Vec3(0, 100, 0), target, B, P, random);
        assertEquals(DragonSteering.Mode.OVERSHOOT, step.mode());
        assertEquals(0, step.headingYaw(), EPS);
        assertVec(step.position(), 0, 100.3, 1);

        var position = step.position();
        for (int i = 2; i <= 26; i++) {
            step = DragonSteering.step(state, position, target, B, P, random);
            assertEquals(DragonSteering.Mode.OVERSHOOT, step.mode(), "step " + i);
            assertEquals(i - 1, state.modeTicks(), "step " + i);
            position = step.position();
        }
        step = DragonSteering.step(state, position, target, B, P, random);
        assertEquals(DragonSteering.Mode.CHASE, step.mode());
    }

    @Test
    void orbitBreakerEndsAChaseThatNeverReachesTheTarget() {
        // target on the inside of the turning circle: the dragon keeps circling around it
        var state = new DragonSteering.State(0);
        var target = new Vec3(0, 100, 0);
        var position = new Vec3(6.5, 100, 0);
        var random = new Random(42);

        int chaseSteps = 0;
        DragonSteering.Mode mode = DragonSteering.Mode.CHASE;
        while (mode != DragonSteering.Mode.OVERSHOOT && chaseSteps < 1000) {
            var step = DragonSteering.step(state, position, target, B, P, random);
            position = step.position();
            mode = step.mode();
            chaseSteps++;
        }
        assertEquals(DragonSteering.Mode.OVERSHOOT, mode);
        assertTrue(chaseSteps <= 161, "chase steps: " + chaseSteps);
    }

    @Test
    void wallClampsThePositionAndTurnsTowardsTheCentre() {
        var state = new DragonSteering.State(-90); // flying east
        var step = DragonSteering.step(state, new Vec3(49.5, 100, 0), new Vec3(200, 100, 0), B, P, new Random(42));
        assertVec(step.position(), 50, 100, 0);
        assertTrue(step.clampedHorizontally());
        // turns 3 * max turn (30 degrees) from east towards the centre; the 180 degree turn wraps to -30
        assertEquals(-120.0, step.headingYaw(), EPS);
    }

    @Test
    void verticalClampDoesNotCountAsHorizontal() {
        var state = new DragonSteering.State(0);
        var step = DragonSteering.step(state, new Vec3(0, 119.8, 0), new Vec3(0, 200, 40), B, P, new Random(42));
        assertEquals(120, step.position().y(), EPS);
        assertFalse(step.clampedHorizontally());
        assertEquals(0, step.headingYaw(), EPS);
    }

    @Test
    void withoutATargetTheDragonWandersInsideTheBox() {
        var state = new DragonSteering.State(0);
        var random = new Random(42);
        var position = new Vec3(0, 100, 0);

        var step = DragonSteering.step(state, position, null, B, P, random);
        assertEquals(DragonSteering.Mode.WANDER, step.mode());
        var firstPoint = state.wanderPoint();
        assertNotNull(firstPoint);
        assertTrue(B.contains(firstPoint));
        position = step.position();

        boolean newPointChosen = false;
        for (int i = 0; i < 500 && !newPointChosen; i++) {
            step = DragonSteering.step(state, position, null, B, P, random);
            position = step.position();
            assertTrue(B.contains(position));
            newPointChosen = !firstPoint.equals(state.wanderPoint());
        }
        assertTrue(newPointChosen, "a new wander point must be chosen after reaching the first one");
    }

    @Test
    void losingTheTargetSwitchesBackToWander() {
        var state = new DragonSteering.State(0);
        var random = new Random(1);
        var step = DragonSteering.step(state, new Vec3(0, 100, 0), new Vec3(0, 100, 40), B, P, random);
        assertEquals(DragonSteering.Mode.CHASE, step.mode());
        step = DragonSteering.step(state, step.position(), null, B, P, random);
        assertEquals(DragonSteering.Mode.WANDER, step.mode());
    }

    @Test
    void positionsNeverLeaveTheBox() {
        var state = new DragonSteering.State(35);
        var random = new Random(7);
        var targets = new Random(11);
        var position = new Vec3(0, 100, 0);
        for (int i = 0; i < 10_000; i++) {
            Vec3 target;
            int kind = targets.nextInt(4);
            if (kind == 0) {
                target = null;
            } else if (kind == 1) {
                target = new Vec3(targets.nextDouble() * 400 - 200, targets.nextDouble() * 300 - 100, targets.nextDouble() * 400 - 200); // mostly far outside
            } else {
                target = new Vec3(targets.nextDouble() * 100 - 50, 60 + targets.nextDouble() * 60, targets.nextDouble() * 100 - 50); // inside
            }
            var step = DragonSteering.step(state, position, target, B, P, random);
            position = step.position();
            assertTrue(B.contains(position), "step " + i + ": " + position);
            assertTrue(step.headingYaw() >= -180.0 && step.headingYaw() < 180.0, "heading " + step.headingYaw());
            assertNotEquals(null, step.mode());
        }
    }

    private static void assertVec(Vec3 actual, double x, double y, double z) {
        assertEquals(x, actual.x(), 1e-9);
        assertEquals(y, actual.y(), 1e-9);
        assertEquals(z, actual.z(), 1e-9);
    }
}
