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

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pure FIFO queue per mode (optional feature, {@code modes.queue.enabled}).
 */
public final class ModeQueue {
    public record Entry(@NotNull UUID player, @NotNull String modeId, long enqueuedAtMillis) {
    }

    private final Map<String, LinkedHashMap<UUID, Entry>> byMode = new HashMap<>();
    private final Map<UUID, String> modeOfPlayer = new HashMap<>();

    /**
     * Returns the 1-based position. Same mode again keeps the original position; another mode moves the player.
     */
    public int enqueue(@NotNull UUID player, @NotNull String modeId, long nowMillis) {
        var current = modeOfPlayer.get(player);
        if (current != null && !current.equals(modeId)) {
            remove(player);
        }
        byMode.computeIfAbsent(modeId, k -> new LinkedHashMap<>()).putIfAbsent(player, new Entry(player, modeId, nowMillis));
        modeOfPlayer.put(player, modeId);
        return positionOf(player);
    }

    public @NotNull Optional<String> remove(@NotNull UUID player) {
        var mode = modeOfPlayer.remove(player);
        if (mode == null) {
            return Optional.empty();
        }
        var q = byMode.get(mode);
        if (q != null) {
            q.remove(player);
            if (q.isEmpty()) {
                byMode.remove(mode);
            }
        }
        return Optional.of(mode);
    }

    public @NotNull Optional<String> modeOf(@NotNull UUID player) {
        return Optional.ofNullable(modeOfPlayer.get(player));
    }

    /**
     * 1-based position, 0 = not queued.
     */
    public int positionOf(@NotNull UUID player) {
        var mode = modeOfPlayer.get(player);
        if (mode == null) {
            return 0;
        }
        int i = 1;
        for (var uuid : byMode.get(mode).keySet()) {
            if (uuid.equals(player)) {
                return i;
            }
            i++;
        }
        return 0;
    }

    public @NotNull List<Entry> entries(@NotNull String modeId) {
        var q = byMode.get(modeId);
        return q == null ? List.of() : List.copyOf(q.values());
    }

    public @NotNull List<String> modesWithEntries() {
        return List.copyOf(byMode.keySet());
    }

    /**
     * maxWaitMillis &lt;= 0 = never expire.
     */
    public @NotNull List<Entry> removeExpired(long nowMillis, long maxWaitMillis) {
        if (maxWaitMillis <= 0) {
            return List.of();
        }
        var expired = byMode.values().stream()
                .flatMap(q -> q.values().stream())
                .filter(e -> nowMillis - e.enqueuedAtMillis() >= maxWaitMillis)
                .collect(Collectors.toList());
        expired.forEach(e -> remove(e.player()));
        return expired;
    }

    public void removeMode(@NotNull String modeId) {
        entries(modeId).forEach(e -> remove(e.player()));
    }

    public boolean isEmpty() {
        return modeOfPlayer.isEmpty();
    }

    public void clear() {
        byMode.clear();
        modeOfPlayer.clear();
    }
}
