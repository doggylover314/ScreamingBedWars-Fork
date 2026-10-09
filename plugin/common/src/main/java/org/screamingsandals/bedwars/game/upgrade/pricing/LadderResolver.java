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

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the price of the next purchase of a laddered upgrade. This is the single source of truth used both when the
 * shop lore is rendered and when the purchase is charged. Pure.
 */
public final class LadderResolver {
    /** Rows shown for upgrades without a maximum level. */
    public static final int MAX_ROWS = 10;
    private static final double EPS = 1e-9;

    private LadderResolver() {
    }

    /**
     * @return purchases already made, {@code floor((level - initialLevel) / levelsPerPurchase)}, never negative
     */
    public static int ownedTiers(double level, double initialLevel, double levelsPerPurchase) {
        double step = levelsPerPurchase > 0 ? levelsPerPurchase : 1;
        return Math.max(0, (int) Math.floor((level - initialLevel) / step + EPS));
    }

    /**
     * Same condition the shop uses to refuse a purchase: the maximum level is below {@code level + step}.
     */
    public static boolean isMaxed(double level, @Nullable Double maxLevel, double levelsPerPurchase) {
        double step = levelsPerPurchase > 0 ? levelsPerPurchase : 1;
        return maxLevel != null && maxLevel + EPS < level + step;
    }

    /**
     * @return the price at the (0-based) index; a ladder shorter than the index repeats its last entry, an empty
     * ladder gives {@code fallback}
     */
    public static @NotNull PriceSpec priceAt(@NotNull List<PriceSpec> ladder, int index, @NotNull PriceSpec fallback) {
        if (ladder.isEmpty()) {
            return fallback;
        }
        return ladder.get(Math.max(0, Math.min(index, ladder.size() - 1)));
    }

    public static @NotNull LadderView resolve(double level, double initialLevel, @Nullable Double maxLevel,
                                              double levelsPerPurchase, @NotNull List<PriceSpec> ladder,
                                              @NotNull PriceSpec fallback) {
        int owned = ownedTiers(level, initialLevel, levelsPerPurchase);
        Integer maxTiers = maxLevel == null ? null : ownedTiers(maxLevel, initialLevel, levelsPerPurchase);
        boolean maxed = isMaxed(level, maxLevel, levelsPerPurchase);
        PriceSpec next = maxed ? null : priceAt(ladder, owned, fallback);
        int count = maxTiers != null ? maxTiers : Math.max(ladder.size(), owned + 1);
        count = Math.min(count, MAX_ROWS);
        var rows = new ArrayList<LadderView.TierRow>(count);
        for (int tier = 1; tier <= count; tier++) {
            LadderView.TierState state;
            if (tier <= owned) {
                state = LadderView.TierState.OWNED;
            } else if (tier == owned + 1 && !maxed) {
                state = LadderView.TierState.NEXT;
            } else {
                state = LadderView.TierState.LOCKED;
            }
            rows.add(new LadderView.TierRow(tier, priceAt(ladder, tier - 1, fallback), state));
        }
        return new LadderView(owned, maxTiers, maxed, next, List.copyOf(rows));
    }
}
