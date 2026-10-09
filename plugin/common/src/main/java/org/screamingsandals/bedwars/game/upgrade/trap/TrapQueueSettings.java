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
import org.screamingsandals.bedwars.game.upgrade.pricing.PriceSpec;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Settings of the {@code trap-queue:} variant section. Pure value class.
 *
 * @param costs cost of the next trap by number of traps already queued (0, 1, 2 ...); the last value repeats
 */
public record TrapQueueSettings(boolean enabled, int maxSize, @NotNull String currency, @NotNull List<Integer> costs,
                                long cooldownSeconds, double defaultDetectionRange, boolean allowDuplicates) {
    public static final TrapQueueSettings DISABLED = new TrapQueueSettings(false, 0, "diamond", List.of(), 0, 7, true);

    /**
     * Cost of the next trap when {@code queued} traps are already queued; empty when the queue is full.
     */
    public @NotNull OptionalInt costFor(int queued) {
        if (queued >= maxSize) {
            return OptionalInt.empty();
        }
        if (costs.isEmpty()) {
            return OptionalInt.of(0);
        }
        return OptionalInt.of(costs.get(Math.max(0, Math.min(queued, costs.size() - 1))));
    }

    public @NotNull Optional<PriceSpec> priceFor(int queued) {
        var c = costFor(queued);
        return c.isPresent() ? Optional.of(PriceSpec.of(c.getAsInt(), currency)) : Optional.empty();
    }
}
