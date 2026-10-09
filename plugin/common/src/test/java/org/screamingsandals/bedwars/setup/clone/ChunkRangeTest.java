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

package org.screamingsandals.bedwars.setup.clone;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkRangeTest {
    @Test
    void rangeIsComputedLikeTheChunkTicketsOfAnArena() {
        // GameImpl#configureChunkTickets: min/max of the block coordinates >> 4
        var range = ChunkRange.ofBlocks("w", 99, -3, 0, 40);
        assertEquals(new ChunkRange("w", 0, 6, -1, 2), range);
    }

    @Test
    void cornersMayBeGivenInAnyOrder() {
        assertEquals(ChunkRange.ofBlocks("w", 0, 0, 99, 99), ChunkRange.ofBlocks("w", 99, 99, 0, 0));
        assertEquals(ChunkRange.ofBlocks("w", 0, 0, 99, 99), ChunkRange.ofBlocks("w", 0, 99, 99, 0));
    }

    @Test
    void negativeCoordinatesUseTheArithmeticShift() {
        var range = ChunkRange.ofBlocks("w", -18, -1, -14, -16);
        assertEquals(-2, range.minCX());
        assertEquals(-1, range.maxCX());
        assertEquals(-1, range.minCZ());
        assertEquals(-1, range.maxCZ());
    }

    @Test
    void containsIsInclusiveAndWorldSpecific() {
        var range = new ChunkRange("w", 0, 6, -1, 2);
        assertTrue(range.contains("w", 0, -1));
        assertTrue(range.contains("w", 6, 2));
        assertTrue(range.contains("w", 3, 0));
        assertFalse(range.contains("w", 7, 0));
        assertFalse(range.contains("w", -1, 0));
        assertFalse(range.contains("w", 3, 3));
        assertFalse(range.contains("w", 3, -2));
        assertFalse(range.contains("other", 3, 0));
    }

    @Test
    void neighbourSharingAChunkWithTheTargetIsDetected() {
        // D52: neighbour arena x 0..99 (chunks 0..6), clone target starts at x 100 (chunk 6) -> chunk 6 is shared
        var neighbour = ChunkRange.ofBlocks("w", 0, 0, 99, 99);
        assertTrue(ChunkRange.anyContains(List.of(neighbour), "w", 6, 3));
        assertFalse(ChunkRange.anyContains(List.of(neighbour), "w", 7, 3));
        assertFalse(ChunkRange.anyContains(List.of(), "w", 0, 0));
    }

    @Test
    void anyContainsChecksEveryRange() {
        var a = ChunkRange.ofBlocks("w", 0, 0, 15, 15);
        var b = ChunkRange.ofBlocks("w2", 0, 0, 15, 15);
        assertTrue(ChunkRange.anyContains(List.of(a, b), "w2", 0, 0));
        assertFalse(ChunkRange.anyContains(List.of(a, b), "w3", 0, 0));
    }
}
