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

package org.screamingsandals.bedwars.game.mode;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeQueueTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);

    @Test
    void positionsAreKeptAndMovedBetweenModes() {
        var queue = new ModeQueue();
        assertEquals(1, queue.enqueue(A, "4v4", 100));
        assertEquals(2, queue.enqueue(B, "4v4", 200));
        assertEquals(1, queue.enqueue(A, "4v4", 300)); // keeps the original position
        assertEquals(1, queue.positionOf(A));

        assertEquals(1, queue.enqueue(A, "2v2", 400)); // moves to another mode
        assertEquals(Optional.of("2v2"), queue.modeOf(A));
        assertEquals(1, queue.positionOf(B));
        assertEquals(List.of(B), queue.entries("4v4").stream().map(ModeQueue.Entry::player).toList());
    }

    @Test
    void removeReturnsTheModeAndDropsEmptyModes() {
        var queue = new ModeQueue();
        queue.enqueue(A, "4v4", 0);
        queue.enqueue(B, "4v4", 0);
        assertEquals(Optional.of("4v4"), queue.remove(B));
        assertEquals(Optional.empty(), queue.remove(B));
        assertEquals(0, queue.positionOf(B));
        assertEquals(List.of("4v4"), queue.modesWithEntries());
        queue.remove(A);
        assertTrue(queue.modesWithEntries().isEmpty());
        assertTrue(queue.isEmpty());
    }

    @Test
    void expiry() {
        var queue = new ModeQueue();
        queue.enqueue(A, "4v4", 400);
        queue.enqueue(B, "4v4", 500);
        var expired = queue.removeExpired(1000, 500);
        assertEquals(2, expired.size()); // waited 600 and 500 millis
        assertTrue(queue.isEmpty());

        queue.enqueue(A, "4v4", 0);
        queue.enqueue(B, "4v4", 600);
        expired = queue.removeExpired(1000, 500);
        assertEquals(List.of(A), expired.stream().map(ModeQueue.Entry::player).toList());
        assertEquals(1, queue.positionOf(B));
    }

    @Test
    void zeroOrNegativeWaitNeverExpires() {
        var queue = new ModeQueue();
        queue.enqueue(A, "4v4", 0);
        assertTrue(queue.removeExpired(Long.MAX_VALUE / 2, 0).isEmpty());
        assertTrue(queue.removeExpired(Long.MAX_VALUE / 2, -5).isEmpty());
        assertFalse(queue.isEmpty());
    }

    @Test
    void removeModeAndClear() {
        var queue = new ModeQueue();
        queue.enqueue(A, "4v4", 0);
        queue.enqueue(B, "2v2", 0);
        queue.removeMode("4v4");
        assertEquals(Optional.empty(), queue.modeOf(A));
        assertEquals(Optional.of("2v2"), queue.modeOf(B));
        queue.clear();
        assertTrue(queue.isEmpty());
        assertTrue(queue.modesWithEntries().isEmpty());
    }
}
