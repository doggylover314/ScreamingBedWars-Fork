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

/**
 * Progress arithmetic of the arena clone (pure).
 */
public final class CloneMath {
    private CloneMath() {
    }

    /**
     * {@code total <= 0} -> 100; otherwise floor(processed * 100 / total) clamped to 0..100.
     */
    public static int percent(long processed, long total) {
        if (total <= 0) {
            return 100;
        }
        if (processed <= 0) {
            return 0;
        }
        if (processed >= total) {
            return 100;
        }
        // processed < total: processed * 100 / total < 100; avoid overflow for huge counts
        long p = (long) Math.floor(processed * 100.0 / total);
        return (int) Math.max(0, Math.min(100, p));
    }

    /**
     * Estimated seconds left. {@code remaining == 0} -> 0; before the first second has elapsed (or without progress)
     * the configured budget is used: ceil(remaining / (fallbackBlocksPerTick * 20)); afterwards the measured speed:
     * ceil(remaining / (processed * 1000 / elapsedMillis)).
     */
    public static long etaSeconds(long processed, long total, long elapsedMillis, int fallbackBlocksPerTick) {
        long remaining = Math.max(0, total - processed);
        if (remaining == 0) {
            return 0;
        }
        if (processed <= 0 || elapsedMillis < 1000) {
            return (long) Math.ceil(remaining / (Math.max(1, fallbackBlocksPerTick) * 20.0));
        }
        return (long) Math.ceil(remaining / (processed * 1000.0 / elapsedMillis));
    }

    /**
     * The shortest possible duration at the configured block budget: ceil(total / (blocksPerTick * 20)).
     */
    public static long minimumSeconds(long total, int blocksPerTick) {
        if (total <= 0) {
            return 0;
        }
        return (long) Math.ceil(total / (Math.max(1, blocksPerTick) * 20.0));
    }
}
