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

package org.screamingsandals.bedwars.game.upgrade.pricing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What a team sees for a laddered upgrade: how many tiers it owns, the price of the next tier and one row per tier.
 * Pure value object produced by {@link LadderResolver}.
 *
 * @param owned    purchases already made
 * @param maxTiers number of purchasable tiers, {@code null} when the upgrade has no maximum level
 * @param maxed    true when no further purchase is possible
 * @param next     price of the next purchase, {@code null} when maxed
 * @param rows     one row per displayed tier (at most {@link LadderResolver#MAX_ROWS})
 */
public record LadderView(int owned, @Nullable Integer maxTiers, boolean maxed, @Nullable PriceSpec next,
                         @NotNull List<TierRow> rows) {
    public enum TierState {
        OWNED, NEXT, LOCKED
    }

    public record TierRow(int tier, @NotNull PriceSpec price, @NotNull TierState state) {
    }

    /**
     * One-tier upgrades (Sharpness, Heal Pool, Dragon Buff) show a simple "Cost:" line instead of tier rows.
     */
    public boolean singleTier() {
        return maxTiers != null ? maxTiers <= 1 : rows.size() <= 1;
    }
}
