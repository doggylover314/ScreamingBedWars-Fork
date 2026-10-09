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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

/**
 * Pure flight AI of a sudden death dragon: chase the prey, swoop past it, turn back; wander when there is nobody to
 * hunt; never leave the {@link FlightBox}.
 * <p>
 * Yaw convention = Minecraft: 0 = +Z (south), 90 = -X (west), -90 = +X (east); range [-180, 180).
 */
public final class DragonSteering {
    /**
     * Vanilla moves the dragon opposite to its yaw, so the entity yaw is the travel yaw plus this offset.
     * If dragons fly tail-first on the server, change this constant from 180 to 0.
     */
    public static final double MODEL_YAW_OFFSET = 180.0;

    private DragonSteering() {
    }

    public enum Mode {
        WANDER,
        CHASE,
        OVERSHOOT
    }

    public record Params(double speed, double maxTurnPerTick, double passDistance, int overshootTicks,
                         double maxClimbPerTick, double wanderReachDistance, int wanderMaxTicks, int maxChaseTicks) {
        public static @NotNull Params of(double speed, double maxTurnPerTick) {
            return new Params(speed, maxTurnPerTick, 3.0, 25, Math.min(0.6, speed * 0.7), 5.0, 200, 160);
        }
    }

    public static final class State {
        Mode mode = Mode.WANDER;
        double headingYaw;
        int modeTicks;
        @Nullable Vec3 wanderPoint;

        public State(double initialHeadingYaw) {
            this.headingYaw = wrapDegrees(initialHeadingYaw);
        }

        public @NotNull Mode mode() {
            return mode;
        }

        public double headingYaw() {
            return headingYaw;
        }

        public int modeTicks() {
            return modeTicks;
        }

        public @Nullable Vec3 wanderPoint() {
            return wanderPoint;
        }
    }

    public record Step(@NotNull Vec3 position, double headingYaw, @NotNull Mode mode, boolean clampedHorizontally) {
    }

    /**
     * Advances the dragon by one tick.
     *
     * @param st      mutable steering state of this dragon
     * @param current current position
     * @param target  position of the prey, null when there is none
     * @param box     flight box (hard limit)
     * @param p       parameters
     * @param random  random source for wander points
     */
    public static @NotNull Step step(@NotNull State st, @NotNull Vec3 current, @Nullable Vec3 target,
                                     @NotNull FlightBox box, @NotNull Params p, @NotNull RandomGenerator random) {
        // 1. mode transitions
        if (target != null && st.mode == Mode.WANDER) {
            set(st, Mode.CHASE);
        }
        if (target == null && st.mode == Mode.CHASE) {
            set(st, Mode.WANDER);
        }
        st.modeTicks++;

        Vec3 desired = null;
        switch (st.mode) {
            case CHASE -> {
                boolean passing = current.horizontalDistance(target) <= p.passDistance()
                        && Math.abs(target.y() - current.y()) <= p.passDistance() + 2;
                if (passing || st.modeTicks > p.maxChaseTicks()) {
                    set(st, Mode.OVERSHOOT); // swoop past / break an orbit
                } else {
                    desired = target;
                }
            }
            case OVERSHOOT -> {
                if (st.modeTicks > p.overshootTicks()) {
                    set(st, target != null ? Mode.CHASE : Mode.WANDER);
                    desired = target != null ? target : wanderPoint(st, current, box, p, random);
                }
            }
            case WANDER -> desired = wanderPoint(st, current, box, p, random);
        }

        // 2. heading + vertical speed
        double dy;
        if (desired != null) {
            double dx = desired.x() - current.x();
            double dz = desired.z() - current.z();
            if (Math.abs(dx) > 1e-9 || Math.abs(dz) > 1e-9) {
                st.headingYaw = turnTowards(st.headingYaw, yawTowards(dx, dz), p.maxTurnPerTick());
            }
            dy = clamp(desired.y() - current.y(), -p.maxClimbPerTick(), p.maxClimbPerTick());
        } else {
            dy = st.mode == Mode.OVERSHOOT ? p.maxClimbPerTick() * 0.5 : 0; // climb gently while swooping away
        }

        // 3. move
        var forward = forward(st.headingYaw);
        var next = new Vec3(
                current.x() + forward.x() * p.speed(),
                current.y() + dy,
                current.z() + forward.z() * p.speed()
        );

        // 4. hard bounds
        var clamped = box.clamp(next);
        boolean horizontal = Math.abs(clamped.x() - next.x()) > 1e-9 || Math.abs(clamped.z() - next.z()) > 1e-9;
        if (horizontal) {
            var centre = box.centre();
            st.headingYaw = turnTowards(st.headingYaw, yawTowards(centre.x() - clamped.x(), centre.z() - clamped.z()), p.maxTurnPerTick() * 3);
            if (st.mode == Mode.OVERSHOOT) {
                set(st, target != null ? Mode.CHASE : Mode.WANDER);
            }
        }
        return new Step(clamped, st.headingYaw, st.mode, horizontal);
    }

    static @NotNull Vec3 wanderPoint(State st, Vec3 current, FlightBox box, Params p, RandomGenerator random) {
        if (st.wanderPoint == null
                || !box.contains(st.wanderPoint)
                || current.horizontalDistance(st.wanderPoint) <= p.wanderReachDistance()
                || st.modeTicks > p.wanderMaxTicks()) {
            st.wanderPoint = new Vec3(
                    lerp(box.minX(), box.maxX(), random.nextDouble()),
                    lerp(box.minY(), box.maxY(), random.nextDouble()),
                    lerp(box.minZ(), box.maxZ(), random.nextDouble())
            );
            st.modeTicks = 0;
        }
        return st.wanderPoint;
    }

    private static void set(State st, Mode mode) {
        st.mode = mode;
        st.modeTicks = 0;
    }

    /**
     * Wraps an angle in degrees into [-180, 180).
     */
    public static double wrapDegrees(double degrees) {
        degrees %= 360.0;
        if (degrees >= 180.0) {
            degrees -= 360.0;
        }
        if (degrees < -180.0) {
            degrees += 360.0;
        }
        return degrees;
    }

    /**
     * Yaw (Minecraft convention) of the direction (dx, dz).
     */
    public static double yawTowards(double dx, double dz) {
        return wrapDegrees(Math.toDegrees(-Math.atan2(dx, dz)));
    }

    /**
     * Unit vector (y = 0) a body with the given yaw travels along.
     */
    public static @NotNull Vec3 forward(double yaw) {
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(radians), 0, Math.cos(radians));
    }

    public static double turnTowards(double current, double target, double maxStep) {
        return wrapDegrees(current + clamp(wrapDegrees(target - current), -maxStep, maxStep));
    }

    /**
     * Yaw to give the entity so that it flies head first along {@code headingYaw}.
     */
    public static float entityYaw(double headingYaw) {
        return (float) wrapDegrees(headingYaw + MODEL_YAW_OFFSET);
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
