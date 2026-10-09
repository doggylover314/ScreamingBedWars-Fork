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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Per-team queue of purchased traps (head fires first) together with the team trap cooldown. Pure state machine.
 * <p>
 * All methods are synchronized: shop clicks and the game cycle may run on different threads (Folia).
 * Time values are game-elapsed seconds.
 */
public final class TrapQueue {
    public enum EnqueueResult {
        OK,
        FULL,
        DUPLICATE
    }

    private final List<String> entries = new ArrayList<>();
    private long cooldownUntil = Long.MIN_VALUE;

    public synchronized int size() {
        return entries.size();
    }

    public synchronized boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @return immutable copy of the queue, head first
     */
    public synchronized @NotNull List<String> snapshot() {
        return List.copyOf(entries);
    }

    public synchronized @NotNull Optional<String> peek() {
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(0));
    }

    public synchronized boolean contains(@NotNull String trapId) {
        return entries.contains(trapId);
    }

    public synchronized @NotNull EnqueueResult tryEnqueue(@NotNull String trapId, int maxSize, boolean allowDuplicates) {
        if (entries.size() >= maxSize) {
            return EnqueueResult.FULL;
        }
        if (!allowDuplicates && entries.contains(trapId)) {
            return EnqueueResult.DUPLICATE;
        }
        entries.add(trapId);
        return EnqueueResult.OK;
    }

    public synchronized boolean isReady(long nowSeconds) {
        return nowSeconds >= cooldownUntil;
    }

    /**
     * Fires the head: removes it and starts the cooldown. An empty queue is left unchanged.
     */
    public synchronized @NotNull Optional<String> consume(long nowSeconds, long cooldownSeconds) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        var id = entries.remove(0);
        cooldownUntil = nowSeconds + Math.max(0, cooldownSeconds);
        return Optional.of(id);
    }

    /**
     * Removes the head without starting a cooldown (stale id).
     */
    public synchronized @NotNull Optional<String> dropHead() {
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.remove(0));
    }

    public synchronized long getCooldownUntil() {
        return cooldownUntil;
    }

    public synchronized void clear() {
        entries.clear();
        cooldownUntil = Long.MIN_VALUE;
    }
}
