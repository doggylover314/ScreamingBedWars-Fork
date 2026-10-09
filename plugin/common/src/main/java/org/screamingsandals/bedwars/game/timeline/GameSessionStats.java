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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-game-run counters shown on the sidebar (kills, final kills, destroyed target blocks).
 * <p>
 * {@code kills} counts all kills including final kills (consistent with the plugin's own player statistics).
 */
public final class GameSessionStats {
    private final ConcurrentHashMap<UUID, Counters> counters = new ConcurrentHashMap<>();

    private static final class Counters {
        final AtomicInteger kills = new AtomicInteger();
        final AtomicInteger finalKills = new AtomicInteger();
        final AtomicInteger targets = new AtomicInteger();
    }

    /**
     * kills++; if finalKill also finalKills++.
     */
    public void recordKill(@NotNull UUID killer, boolean finalKill) {
        var c = counters.computeIfAbsent(killer, u -> new Counters());
        c.kills.incrementAndGet();
        if (finalKill) {
            c.finalKills.incrementAndGet();
        }
    }

    public void recordTargetDestroyed(@NotNull UUID player) {
        counters.computeIfAbsent(player, u -> new Counters()).targets.incrementAndGet();
    }

    /**
     * 0 for null / unknown players.
     */
    public int getKills(@Nullable UUID player) {
        var c = player == null ? null : counters.get(player);
        return c == null ? 0 : c.kills.get();
    }

    public int getFinalKills(@Nullable UUID player) {
        var c = player == null ? null : counters.get(player);
        return c == null ? 0 : c.finalKills.get();
    }

    public int getTargetBlocksDestroyed(@Nullable UUID player) {
        var c = player == null ? null : counters.get(player);
        return c == null ? 0 : c.targets.get();
    }

    public void clear() {
        counters.clear();
    }
}
