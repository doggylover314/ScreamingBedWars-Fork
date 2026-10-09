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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkedBoxIteratorTest {
    private static List<int[]> all(BlockBox box) {
        var it = new ChunkedBoxIterator(box);
        var out = new ArrayList<int[]>();
        while (it.hasNext()) {
            it.nextBatch(37, (x, y, z) -> out.add(new int[]{x, y, z}));
        }
        return out;
    }

    @Test
    void orderInsideOneColumnIsYThenZThenX() {
        var list = all(new BlockBox(0, 0, 0, 1, 1, 1));
        int[][] expected = {
                {0, 0, 0}, {1, 0, 0}, {0, 0, 1}, {1, 0, 1},
                {0, 1, 0}, {1, 1, 0}, {0, 1, 1}, {1, 1, 1}
        };
        assertEquals(expected.length, list.size());
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], list.get(i), "position " + i);
        }
        assertEquals(8L, new ChunkedBoxIterator(new BlockBox(0, 0, 0, 1, 1, 1)).total());
    }

    @Test
    void negativeCoordinatesUseArithmeticChunkMath() {
        var list = all(new BlockBox(-18, 5, 3, -14, 5, 3));
        var xs = list.stream().mapToInt(p -> p[0]).toArray();
        assertArrayEquals(new int[]{-18, -17, -16, -15, -14}, xs);
    }

    @Test
    void chunkColumnsAreVisitedXOuterZInner() {
        var list = all(new BlockBox(0, 0, 0, 31, 0, 31));
        assertEquals(1024, list.size());
        for (int i = 0; i < list.size(); i++) {
            var p = list.get(i);
            int block = i / 256;
            int expectedXBase = block >= 2 ? 16 : 0;
            int expectedZBase = block == 1 || block == 3 ? 16 : 0;
            assertTrue(p[0] >= expectedXBase && p[0] < expectedXBase + 16, "x of position " + i);
            assertTrue(p[2] >= expectedZBase && p[2] < expectedZBase + 16, "z of position " + i);
        }
    }

    @Test
    void largeBoxVisitsEveryPositionOnceAndColumnsAreContiguous() {
        var box = new BlockBox(-20, 0, -20, 20, 3, 20);
        var it = new ChunkedBoxIterator(box);
        var seen = new HashSet<String>();
        var finishedColumns = new HashSet<Long>();
        long[] lastColumn = {Long.MIN_VALUE};
        boolean[] ok = {true};
        while (it.hasNext()) {
            it.nextBatch(100, (x, y, z) -> {
                if (!box.contains(x, y, z)) {
                    ok[0] = false;
                }
                if (!seen.add(x + "," + y + "," + z)) {
                    ok[0] = false; // duplicate
                }
                long column = ((long) (x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL);
                if (column != lastColumn[0]) {
                    if (finishedColumns.contains(column)) {
                        ok[0] = false; // revisited
                    }
                    if (lastColumn[0] != Long.MIN_VALUE) {
                        finishedColumns.add(lastColumn[0]);
                    }
                    lastColumn[0] = column;
                }
            });
        }
        assertTrue(ok[0]);
        assertEquals(41L * 4 * 41, box.volume());
        assertEquals(6724L, box.volume());
        assertEquals(6724, seen.size());
        assertEquals(it.total(), it.processed());
        assertFalse(it.hasNext());
    }

    @Test
    void batchesRespectTheLimit() {
        var box = new BlockBox(0, 0, 0, 4, 1, 0); // volume 10
        var it = new ChunkedBoxIterator(box);
        assertEquals(0, it.nextBatch(0, (x, y, z) -> { }));
        assertEquals(0L, it.processed());
        assertEquals(3, it.nextBatch(3, (x, y, z) -> { }));
        assertEquals(3L, it.processed());
        assertEquals(7, it.nextBatch(100, (x, y, z) -> { }));
        assertFalse(it.hasNext());
        assertEquals(0, it.nextBatch(5, (x, y, z) -> { }));
        assertEquals(10L, it.processed());
        assertEquals(10L, it.total());
    }

    @Test
    void negativeMaxFeedsNothing() {
        var it = new ChunkedBoxIterator(new BlockBox(0, 0, 0, 1, 1, 1));
        assertEquals(0, it.nextBatch(-5, (x, y, z) -> { }));
        assertTrue(it.hasNext());
    }

    @Test
    void singleBlockBox() {
        var list = all(new BlockBox(7, 7, 7, 7, 7, 7));
        assertEquals(1, list.size());
        assertArrayEquals(new int[]{7, 7, 7}, list.get(0));
    }

    @Test
    void batchBoundariesDoNotChangeTheOrder() {
        var box = new BlockBox(-3, 0, -3, 20, 2, 5);
        var reference = all(box);
        var it = new ChunkedBoxIterator(box);
        var again = new ArrayList<int[]>();
        int n = 1;
        while (it.hasNext()) {
            it.nextBatch(n, (x, y, z) -> again.add(new int[]{x, y, z}));
            n = n % 13 + 1;
        }
        assertEquals(reference.size(), again.size());
        Set<String> a = new HashSet<>();
        for (int i = 0; i < reference.size(); i++) {
            assertArrayEquals(reference.get(i), again.get(i));
            a.add(reference.get(i)[0] + "," + reference.get(i)[1] + "," + reference.get(i)[2]);
        }
        assertEquals(reference.size(), a.size());
    }
}
