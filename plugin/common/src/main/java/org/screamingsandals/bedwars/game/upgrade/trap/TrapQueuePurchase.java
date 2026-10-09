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
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.upgrade.pricing.PriceSpec;
import org.screamingsandals.bedwars.game.upgrade.pricing.PurchaseCheck;

import java.util.List;

/**
 * Decides whether a trap may be queued and what it costs. Pure.
 */
public final class TrapQueuePurchase {
    private TrapQueuePurchase() {
    }

    public record Decision(@NotNull PurchaseCheck check, @Nullable PriceSpec price) {
    }

    public static @NotNull Decision decide(@NotNull TrapQueueSettings settings, boolean trapKnown,
                                           @NotNull List<String> queued, @NotNull String trapId) {
        if (!settings.enabled() || !trapKnown) {
            return new Decision(PurchaseCheck.UNAVAILABLE, null);
        }
        var price = settings.priceFor(queued.size()).orElse(null);
        if (price == null) {
            return new Decision(PurchaseCheck.QUEUE_FULL, null);
        }
        if (!settings.allowDuplicates() && queued.contains(trapId)) {
            return new Decision(PurchaseCheck.ALREADY_QUEUED, price);
        }
        return new Decision(PurchaseCheck.OK, price);
    }
}
