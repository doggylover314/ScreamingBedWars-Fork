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

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnerCycleMathTest {
    @Test
    void rebaseExamples() {
        assertEquals(7200, SpawnerCycleMath.rebaseCountdownDelay(7201, 0, 600, 460)); // next run local 1, spawn 459 runs later
        assertEquals(6740, SpawnerCycleMath.rebaseCountdownDelay(7000, 0, 600, 460)); // keeps 200 ticks left
        assertEquals(7200, SpawnerCycleMath.rebaseCountdownDelay(7200, 0, 600, 460)); // the next run spawns
        assertEquals(0, SpawnerCycleMath.rebaseCountdownDelay(0, 0, 600, 1300));
        assertEquals(-20, SpawnerCycleMath.rebaseCountdownDelay(15, 0, 20, 40)); // local 35, spawn after 5 runs
    }

    @Test
    void rebaseKeepsTheDelayForInvalidCycles() {
        assertEquals(5, SpawnerCycleMath.rebaseCountdownDelay(100, 5, 0, 20));
        assertEquals(5, SpawnerCycleMath.rebaseCountdownDelay(100, 5, 20, 0));
        assertEquals(5, SpawnerCycleMath.rebaseCountdownDelay(100, 5, -1, 20));
    }

    @Test
    void remainingSecondsRoundsUp() {
        assertEquals(30, SpawnerCycleMath.remainingSeconds(0, 600));
        assertEquals(30, SpawnerCycleMath.remainingSeconds(1, 600));
        assertEquals(1, SpawnerCycleMath.remainingSeconds(581, 600));
        assertEquals(30, SpawnerCycleMath.remainingSeconds(1800, 600));
        assertEquals(1, SpawnerCycleMath.remainingSeconds(-20, 40)); // floorMod
        assertEquals(0, SpawnerCycleMath.remainingSeconds(10, 0));
        assertEquals(23, SpawnerCycleMath.remainingSeconds(0, 460));
        assertEquals(1, SpawnerCycleMath.remainingSeconds(0, 20));
    }

    @Test
    void countdownIsShownFromTwoSeconds() {
        assertFalse(SpawnerCycleMath.showsCountdown(39));
        assertTrue(SpawnerCycleMath.showsCountdown(40));
        assertFalse(SpawnerCycleMath.showsCountdown(20));
        assertTrue(SpawnerCycleMath.showsCountdown(600));
    }

    /**
     * Simulates the spawner loop (each run: L = raw - delay; spawn if L % cycle == 0; raw++) after a rebase.
     */
    @Test
    void rebaseProperty() {
        var random = new Random(0xB3D5L);
        for (int caseNo = 0; caseNo < 10_000; caseNo++) {
            long raw = random.nextInt(100_001);
            long delay = (long) (random.nextDouble() * (raw + 1));
            if (delay > raw) {
                delay = raw;
            }
            long oldCycle = 1 + random.nextInt(2000);
            long newCycle = 1 + random.nextInt(2000);

            long before = Math.floorMod(-(raw - delay), oldCycle); // non-spawning runs before the next spawn under the old cycle
            long rebased = SpawnerCycleMath.rebaseCountdownDelay(raw, delay, oldCycle, newCycle);

            String context = "case " + caseNo + " raw=" + raw + " delay=" + delay + " old=" + oldCycle + " new=" + newCycle;
            long expectedWait = Math.min(before, newCycle - 1);

            long simulatedRaw = raw;
            long nonSpawningBeforeFirst = -1;
            long runsSinceFirst = 0;
            boolean firstSeen = false;
            int spawnsChecked = 0;
            for (long run = 0; run < expectedWait + 1 + 3 * newCycle && spawnsChecked < 3; run++) {
                long local = simulatedRaw - rebased;
                assertTrue(local >= 0, "negative local elapsed value, " + context);
                boolean spawns = local % newCycle == 0;
                if (!firstSeen) {
                    if (spawns) {
                        firstSeen = true;
                        nonSpawningBeforeFirst = run;
                        runsSinceFirst = 0;
                    }
                } else {
                    runsSinceFirst++;
                    if (spawns) {
                        assertEquals(newCycle, runsSinceFirst, "spawns must be exactly one cycle apart, " + context);
                        runsSinceFirst = 0;
                        spawnsChecked++;
                    }
                }
                simulatedRaw++;
            }
            assertTrue(firstSeen, "no spawn happened, " + context);
            assertEquals(expectedWait, nonSpawningBeforeFirst, "wait before the first spawn, " + context);
        }
    }
}
