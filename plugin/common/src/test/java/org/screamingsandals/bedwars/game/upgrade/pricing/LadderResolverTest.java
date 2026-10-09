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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.screamingsandals.bedwars.game.upgrade.pricing.LadderView.TierState.LOCKED;
import static org.screamingsandals.bedwars.game.upgrade.pricing.LadderView.TierState.NEXT;
import static org.screamingsandals.bedwars.game.upgrade.pricing.LadderView.TierState.OWNED;

class LadderResolverTest {
    private static final PriceSpec FALLBACK = PriceSpec.of(3, "diamond");
    private static final List<PriceSpec> LADDER = List.of(
            PriceSpec.of(2, "diamond"), PriceSpec.of(4, "diamond"), PriceSpec.of(8, "diamond"), PriceSpec.of(16, "diamond"));

    private static PriceSpec diamond(int amount) {
        return PriceSpec.of(amount, "diamond");
    }

    private static List<LadderView.TierState> states(LadderView view) {
        return view.rows().stream().map(LadderView.TierRow::state).toList();
    }

    private static List<Integer> amounts(LadderView view) {
        return view.rows().stream().map(r -> r.price().amount()).toList();
    }

    @Test
    void freshUpgradeOffersTheFirstTier() {
        var view = LadderResolver.resolve(0, 0, 4.0, 1, LADDER, FALLBACK);

        assertEquals(0, view.owned());
        assertEquals(4, view.maxTiers());
        assertFalse(view.maxed());
        assertEquals(diamond(2), view.next());
        assertEquals(List.of(NEXT, LOCKED, LOCKED, LOCKED), states(view));
        assertEquals(List.of(2, 4, 8, 16), amounts(view));
        assertFalse(view.singleTier());
    }

    @Test
    void halfwayUpgradeMarksOwnedNextAndLocked() {
        var view = LadderResolver.resolve(2, 0, 4.0, 1, LADDER, FALLBACK);

        assertEquals(2, view.owned());
        assertEquals(diamond(8), view.next());
        assertEquals(List.of(OWNED, OWNED, NEXT, LOCKED), states(view));
    }

    @Test
    void maxedUpgradeHasNoNextPrice() {
        var view = LadderResolver.resolve(4, 0, 4.0, 1, LADDER, FALLBACK);

        assertTrue(view.maxed());
        assertNull(view.next());
        assertEquals(List.of(OWNED, OWNED, OWNED, OWNED), states(view));
    }

    @Test
    void shortLadderRepeatsItsLastEntry() {
        var view = LadderResolver.resolve(3, 0, 4.0, 1, List.of(diamond(2), diamond(4)), FALLBACK);

        assertEquals(diamond(4), view.next());
        assertEquals(List.of(2, 4, 4, 4), amounts(view));
    }

    @Test
    void emptyLadderUsesTheFallbackAndIsSingleTier() {
        var view = LadderResolver.resolve(0, 0, 1.0, 1, List.of(), FALLBACK);

        assertEquals(FALLBACK, view.next());
        assertTrue(view.singleTier());
        assertEquals(1, view.rows().size());
        assertEquals(new LadderView.TierRow(1, FALLBACK, NEXT), view.rows().get(0));
    }

    @Test
    void upgradeWithoutMaximumShowsOwnedPlusOneRows() {
        var view = LadderResolver.resolve(5, 0, null, 1, List.of(diamond(1), diamond(2)), FALLBACK);

        assertNull(view.maxTiers());
        assertFalse(view.maxed());
        assertEquals(diamond(2), view.next());
        assertEquals(6, view.rows().size());
        assertEquals(List.of(1, 2, 2, 2, 2, 2), amounts(view));
    }

    @Test
    void upgradeWithoutMaximumCapsTheRows() {
        var view = LadderResolver.resolve(20, 0, null, 1, List.of(diamond(1), diamond(2)), FALLBACK);

        assertEquals(LadderResolver.MAX_ROWS, view.rows().size());
    }

    @Test
    void multiLevelStepsCountPurchasesNotLevels() {
        var view = LadderResolver.resolve(4, 0, 8.0, 2, List.of(diamond(1), diamond(2), diamond(3), diamond(4)), FALLBACK);

        assertEquals(2, view.owned());
        assertEquals(diamond(3), view.next());
        assertEquals(4, view.maxTiers());
    }

    @Test
    void initialLevelIsNotAPurchase() {
        var view = LadderResolver.resolve(1, 1, 3.0, 1, LADDER, FALLBACK);

        assertEquals(0, view.owned());
        assertEquals(2, view.maxTiers());
        assertEquals(2, view.rows().size());
    }

    @Test
    void floatingPointDriftIsTolerated() {
        assertEquals(1, LadderResolver.ownedTiers(0.9999999999, 0, 1));
        assertFalse(LadderResolver.isMaxed(3.0000000001, 4.0, 1));
        assertFalse(LadderResolver.isMaxed(2.9999999999, 4.0, 1));
        assertTrue(LadderResolver.isMaxed(3.9999999999, 4.0, 1));
        assertTrue(LadderResolver.isMaxed(4.0, 4.0, 1));
        assertFalse(LadderResolver.isMaxed(100, null, 1));
    }

    @Test
    void maximumEqualToInitialLevelIsMaxedWithoutRows() {
        var view = LadderResolver.resolve(0, 0, 0.0, 1, LADDER, FALLBACK);

        assertTrue(view.maxed());
        assertEquals(0, view.maxTiers());
        assertTrue(view.rows().isEmpty());
        assertNull(view.next());
    }

    @Test
    void nonPositiveStepsAreTreatedAsOne() {
        assertEquals(2, LadderResolver.ownedTiers(2, 0, 0));
        assertEquals(2, LadderResolver.ownedTiers(2, 0, -3));
        assertTrue(LadderResolver.isMaxed(1, 1.0, 0));
    }

    @Test
    void priceAtClampsTheIndex() {
        assertEquals(diamond(2), LadderResolver.priceAt(LADDER, -5, FALLBACK));
        assertEquals(diamond(16), LadderResolver.priceAt(LADDER, 99, FALLBACK));
        assertEquals(FALLBACK, LadderResolver.priceAt(List.of(), 0, FALLBACK));
    }

    @Test
    void singleTierFollowsTheMaximum() {
        assertTrue(LadderResolver.resolve(0, 0, 1.0, 1, List.of(diamond(5)), FALLBACK).singleTier());
        assertFalse(LadderResolver.resolve(0, 0, 2.0, 1, List.of(diamond(5)), FALLBACK).singleTier());
    }
}
