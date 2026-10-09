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

package org.screamingsandals.bedwars.game.upgrade.trap;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapQueueTest {
    @Test
    void newQueueIsEmptyAndReady() {
        var queue = new TrapQueue();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertTrue(queue.isReady(0));
        assertTrue(queue.peek().isEmpty());
        assertTrue(queue.consume(5, 15).isEmpty());
        assertTrue(queue.isReady(5)); // consuming an empty queue does not start a cooldown
    }

    @Test
    void enqueueRespectsTheMaximumSize() {
        var queue = new TrapQueue();
        assertEquals(TrapQueue.EnqueueResult.OK, queue.tryEnqueue("a", 3, true));
        assertEquals(TrapQueue.EnqueueResult.OK, queue.tryEnqueue("b", 3, true));
        assertEquals(TrapQueue.EnqueueResult.OK, queue.tryEnqueue("c", 3, true));
        assertEquals(TrapQueue.EnqueueResult.FULL, queue.tryEnqueue("d", 3, true));
        assertEquals(List.of("a", "b", "c"), queue.snapshot());
        assertEquals("a", queue.peek().orElseThrow());
        assertTrue(queue.contains("b"));
        assertFalse(queue.contains("d"));
    }

    @Test
    void duplicatesFollowTheFlag() {
        var strict = new TrapQueue();
        assertEquals(TrapQueue.EnqueueResult.OK, strict.tryEnqueue("a", 3, false));
        assertEquals(TrapQueue.EnqueueResult.DUPLICATE, strict.tryEnqueue("a", 3, false));
        assertEquals(1, strict.size());

        var lenient = new TrapQueue();
        assertEquals(TrapQueue.EnqueueResult.OK, lenient.tryEnqueue("a", 3, true));
        assertEquals(TrapQueue.EnqueueResult.OK, lenient.tryEnqueue("a", 3, true));
        assertEquals(List.of("a", "a"), lenient.snapshot());
    }

    @Test
    void consumeRemovesTheHeadAndStartsTheCooldown() {
        var queue = new TrapQueue();
        queue.tryEnqueue("a", 3, true);
        queue.tryEnqueue("b", 3, true);
        queue.tryEnqueue("c", 3, true);

        assertEquals("a", queue.consume(100, 15).orElseThrow());
        assertEquals(List.of("b", "c"), queue.snapshot());
        assertFalse(queue.isReady(100));
        assertFalse(queue.isReady(114));
        assertTrue(queue.isReady(115));
        assertEquals(115, queue.getCooldownUntil());

        assertEquals("b", queue.consume(200, 0).orElseThrow());
        assertTrue(queue.isReady(200));

        long before = queue.getCooldownUntil();
        assertEquals("c", queue.dropHead().orElseThrow());
        assertEquals(before, queue.getCooldownUntil()); // dropHead does not touch the cooldown
        assertTrue(queue.dropHead().isEmpty());
    }

    @Test
    void negativeCooldownIsTreatedAsZero() {
        var queue = new TrapQueue();
        queue.tryEnqueue("a", 3, true);
        queue.consume(50, -10);
        assertTrue(queue.isReady(50));
    }

    @Test
    void clearResetsEverything() {
        var queue = new TrapQueue();
        queue.tryEnqueue("a", 3, true);
        queue.consume(10, 100);
        queue.tryEnqueue("b", 3, true);
        queue.clear();
        assertTrue(queue.isEmpty());
        assertTrue(queue.isReady(Long.MIN_VALUE));
    }

    @Test
    void trapsAreReBuyableAfterTheQueueWasFull() {
        var queue = new TrapQueue();
        queue.tryEnqueue("a", 2, true);
        queue.tryEnqueue("b", 2, true);
        assertEquals(TrapQueue.EnqueueResult.FULL, queue.tryEnqueue("c", 2, true));
        queue.consume(1, 15);
        assertEquals(TrapQueue.EnqueueResult.OK, queue.tryEnqueue("c", 2, true));
        assertEquals(List.of("b", "c"), queue.snapshot());
    }

    @Test
    void snapshotIsACopy() {
        var queue = new TrapQueue();
        queue.tryEnqueue("a", 3, true);
        var snapshot = queue.snapshot();
        queue.tryEnqueue("b", 3, true);
        assertEquals(List.of("a"), snapshot);
    }
}
