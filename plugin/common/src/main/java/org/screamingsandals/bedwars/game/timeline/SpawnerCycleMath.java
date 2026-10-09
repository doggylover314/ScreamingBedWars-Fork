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

package org.screamingsandals.bedwars.game.timeline;

/**
 * Pure arithmetic of the item spawner cycle (used by {@code ItemSpawnerImpl}).
 * <p>
 * The spawner task evaluates {@code L = elapsedTime - countdownDelay} on every run, spawns when {@code L % cycle == 0}
 * and then increments {@code elapsedTime}. After a run, {@code elapsedTime - countdownDelay} is exactly the
 * {@code L} of the next run.
 */
public final class SpawnerCycleMath {
    private SpawnerCycleMath() {
    }

    /**
     * The hologram countdown is shown for intervals of 2 seconds or more; shorter ones show "every second".
     */
    public static boolean showsCountdown(long cycleTicks) {
        return cycleTicks >= 40;
    }

    /**
     * @return seconds (rounded up) until the next spawn, for the local elapsed value evaluated in the current spawner run
     */
    public static long remainingSeconds(long localElapsed, long cycleTicks) {
        if (cycleTicks <= 0) {
            return 0;
        }
        long remainingTicks = cycleTicks - Math.floorMod(localElapsed, cycleTicks); // 1..cycle
        return (remainingTicks + 19) / 20;
    }

    /**
     * New countdownDelay after an interval change, so that the next spawn happens after
     * min(ticks left in the old cycle, one full new cycle). Keeps the local elapsed value of the next run &gt;= 0
     * (the returned delay may be negative; that is fine for the spawner's arithmetic).
     *
     * @param rawElapsed     the spawner's {@code elapsedTime} (value the next run will start from)
     * @param countdownDelay the spawner's current {@code countdownDelay}
     */
    public static long rebaseCountdownDelay(long rawElapsed, long countdownDelay, long oldCycle, long newCycle) {
        if (oldCycle <= 0 || newCycle <= 0) {
            return countdownDelay;
        }
        long nextLocal = rawElapsed - countdownDelay;               // local value the next spawner run evaluates
        long ticksUntilSpawn = Math.floorMod(-nextLocal, oldCycle); // 0 => the next run spawns
        long t = Math.min(ticksUntilSpawn, newCycle - 1);
        long newLocal = (t == 0) ? 0 : newCycle - t;
        return rawElapsed - newLocal;
    }

    /**
     * New countdownDelay for a full restart of the countdown: the next run evaluates the local value 1 (or 0 if the
     * spawner has not run yet, which keeps "spawn resources on game start" working), so the next spawn happens after one
     * full cycle.
     *
     * @param rawElapsed the spawner's {@code elapsedTime} (value the next run will start from)
     */
    public static long restartCountdownDelay(long rawElapsed) {
        return Math.max(0L, rawElapsed - 1);
    }
}
