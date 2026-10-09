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

import org.jetbrains.annotations.NotNull;

/**
 * Walks a {@link BlockBox} chunk column by chunk column (pure, allocation-free per block).
 * <p>
 * Order: chunk columns {@code cx} (outer, ascending) then {@code cz} (ascending); inside a column {@code y} ascending
 * (supports before supported blocks), then {@code z}, then {@code x}, all restricted to the box. {@code >> 4} is an
 * arithmetic shift, so negative coordinates map to the right chunk (-18 >> 4 = -2).
 */
public final class ChunkedBoxIterator {

    @FunctionalInterface
    public interface PositionConsumer {
        void accept(int x, int y, int z);
    }

    private final BlockBox box;
    private final long total;
    private final int minCZ;
    private final int maxCX;
    private final int maxCZ;

    private int cx;
    private int cz;
    private int colMinX;
    private int colMaxX;
    private int colMinZ;
    private int colMaxZ;
    private int x;
    private int y;
    private int z;
    private boolean done;
    private long processed;

    public ChunkedBoxIterator(@NotNull BlockBox box) {
        this.box = box;
        this.total = box.volume();
        this.cx = box.minX() >> 4;
        this.cz = box.minZ() >> 4;
        this.minCZ = this.cz;
        this.maxCX = box.maxX() >> 4;
        this.maxCZ = box.maxZ() >> 4;
        this.y = box.minY();
        setColumn();
    }

    private void setColumn() {
        colMinX = Math.max(box.minX(), cx << 4);
        colMaxX = Math.min(box.maxX(), (cx << 4) + 15);
        colMinZ = Math.max(box.minZ(), cz << 4);
        colMaxZ = Math.min(box.maxZ(), (cz << 4) + 15);
        x = colMinX;
        z = colMinZ;
    }

    public boolean hasNext() {
        return !done;
    }

    /**
     * Feeds up to {@code max} positions to the consumer; returns how many were fed (0 when exhausted or max <= 0).
     */
    public int nextBatch(int max, @NotNull PositionConsumer consumer) {
        int fed = 0;
        while (fed < max && !done) {
            consumer.accept(x, y, z);
            fed++;
            processed++;
            advance();
        }
        return fed;
    }

    private void advance() {
        if (++x <= colMaxX) {
            return;
        }
        x = colMinX;
        if (++z <= colMaxZ) {
            return;
        }
        z = colMinZ;
        if (++y <= box.maxY()) {
            return;
        }
        y = box.minY();
        // next chunk column
        if (++cz <= maxCZ) {
            setColumn();
            return;
        }
        cz = minCZ;
        if (++cx <= maxCX) {
            setColumn();
            return;
        }
        done = true;
    }

    public long processed() {
        return processed;
    }

    public long total() {
        return total;
    }
}
