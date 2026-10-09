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

package org.screamingsandals.bedwars.inventories.upgrade;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.upgrade.pricing.LadderView;
import org.screamingsandals.bedwars.game.upgrade.pricing.PriceSpec;
import org.screamingsandals.bedwars.game.upgrade.pricing.PurchaseCheck;

/**
 * What the upgrade shop knows about one item for one team at one moment. Computed by
 * {@link UpgradeItemStateResolver} and used both for the lore and for the purchase, so the shown price is the charged price.
 *
 * @param kind     what kind of upgrade the item sells
 * @param check    whether a purchase is possible right now
 * @param price    price of the next purchase, {@code null} when none (maxed, full queue, misconfigured)
 * @param ladder   tier view of a team level upgrade, otherwise {@code null}
 * @param trapId   trap sold by a trap-queue item, otherwise {@code null}
 * @param queued   traps currently in the team's queue (trap items)
 * @param queueMax capacity of the trap queue (trap items)
 */
public record UpgradeItemState(@NotNull Kind kind, @NotNull PurchaseCheck check, @Nullable PriceSpec price,
                               @Nullable LadderView ladder, @Nullable String trapId, int queued, int queueMax) {
    public enum Kind {
        TEAM_LEVEL, TRAP, LEGACY, UNAVAILABLE
    }

    public static @NotNull UpgradeItemState unavailable() {
        return new UpgradeItemState(Kind.UNAVAILABLE, PurchaseCheck.UNAVAILABLE, null, null, null, 0, 0);
    }
}
