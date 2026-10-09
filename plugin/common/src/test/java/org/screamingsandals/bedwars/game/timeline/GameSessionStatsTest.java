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

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameSessionStatsTest {
    @Test
    void killsIncludeFinalKills() {
        var stats = new GameSessionStats();
        var a = UUID.randomUUID();
        stats.recordKill(a, false);
        stats.recordKill(a, true);
        assertEquals(2, stats.getKills(a));
        assertEquals(1, stats.getFinalKills(a));
        assertEquals(0, stats.getTargetBlocksDestroyed(a));
    }

    @Test
    void targetBlocksAreCountedSeparately() {
        var stats = new GameSessionStats();
        var a = UUID.randomUUID();
        stats.recordTargetDestroyed(a);
        assertEquals(1, stats.getTargetBlocksDestroyed(a));
        assertEquals(0, stats.getKills(a));
        assertEquals(0, stats.getFinalKills(a));
    }

    @Test
    void unknownAndNullPlayersHaveZero() {
        var stats = new GameSessionStats();
        stats.recordKill(UUID.randomUUID(), true);
        assertEquals(0, stats.getKills(UUID.randomUUID()));
        assertEquals(0, stats.getFinalKills(UUID.randomUUID()));
        assertEquals(0, stats.getTargetBlocksDestroyed(UUID.randomUUID()));
        assertEquals(0, stats.getKills(null));
        assertEquals(0, stats.getFinalKills(null));
        assertEquals(0, stats.getTargetBlocksDestroyed(null));
    }

    @Test
    void playersAreIndependent() {
        var stats = new GameSessionStats();
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        stats.recordKill(a, false);
        stats.recordKill(b, true);
        stats.recordKill(b, true);
        assertEquals(1, stats.getKills(a));
        assertEquals(2, stats.getKills(b));
        assertEquals(0, stats.getFinalKills(a));
        assertEquals(2, stats.getFinalKills(b));
    }

    @Test
    void clearResetsEverything() {
        var stats = new GameSessionStats();
        var a = UUID.randomUUID();
        stats.recordKill(a, true);
        stats.recordTargetDestroyed(a);
        stats.clear();
        assertEquals(0, stats.getKills(a));
        assertEquals(0, stats.getFinalKills(a));
        assertEquals(0, stats.getTargetBlocksDestroyed(a));
    }

    @Test
    void concurrentRecordingLosesNothing() throws Exception {
        var stats = new GameSessionStats();
        var player = UUID.randomUUID();
        int threads = 8;
        int perThread = 1000;
        var pool = Executors.newFixedThreadPool(threads);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<java.util.concurrent.Future<?>>();
        try {
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        stats.recordKill(player, i % 2 == 0);
                        stats.recordTargetDestroyed(player);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (var future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(threads * perThread, stats.getKills(player));
        assertEquals(threads * perThread / 2, stats.getFinalKills(player));
        assertEquals(threads * perThread, stats.getTargetBlocksDestroyed(player));
    }
}
