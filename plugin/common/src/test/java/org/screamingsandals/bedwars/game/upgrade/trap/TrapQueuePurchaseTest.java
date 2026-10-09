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

import org.junit.jupiter.api.Test;
import org.screamingsandals.bedwars.game.upgrade.pricing.PriceSpec;
import org.screamingsandals.bedwars.game.upgrade.pricing.PurchaseCheck;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrapQueuePurchaseTest {
    private static final TrapQueueSettings SETTINGS = new TrapQueueSettings(true, 3, "diamond", List.of(1, 2, 4), 15, 7, true);
    private static final TrapQueueSettings STRICT = new TrapQueueSettings(true, 3, "diamond", List.of(1, 2, 4), 15, 7, false);

    @Test
    void firstTrapCostsTheFirstEntry() {
        var d = TrapQueuePurchase.decide(SETTINGS, true, List.of(), "a");
        assertEquals(PurchaseCheck.OK, d.check());
        assertEquals(PriceSpec.of(1, "diamond"), d.price());
    }

    @Test
    void costGrowsWithQueuedTraps() {
        var d = TrapQueuePurchase.decide(SETTINGS, true, List.of("a"), "b");
        assertEquals(PurchaseCheck.OK, d.check());
        assertEquals(PriceSpec.of(2, "diamond"), d.price());
        assertEquals(PriceSpec.of(4, "diamond"), TrapQueuePurchase.decide(SETTINGS, true, List.of("a", "b"), "c").price());
    }

    @Test
    void fullQueueIsRefused() {
        var d = TrapQueuePurchase.decide(SETTINGS, true, List.of("a", "b", "c"), "a");
        assertEquals(PurchaseCheck.QUEUE_FULL, d.check());
        assertNull(d.price());
    }

    @Test
    void duplicatesAreRefusedOnlyWhenDisallowed() {
        var strict = TrapQueuePurchase.decide(STRICT, true, List.of("a"), "a");
        assertEquals(PurchaseCheck.ALREADY_QUEUED, strict.check());
        assertEquals(PriceSpec.of(2, "diamond"), strict.price());

        assertEquals(PurchaseCheck.OK, TrapQueuePurchase.decide(SETTINGS, true, List.of("a"), "a").check());
    }

    @Test
    void unknownTrapOrDisabledQueueIsUnavailable() {
        var unknown = TrapQueuePurchase.decide(SETTINGS, false, List.of(), "a");
        assertEquals(PurchaseCheck.UNAVAILABLE, unknown.check());
        assertNull(unknown.price());

        assertEquals(PurchaseCheck.UNAVAILABLE, TrapQueuePurchase.decide(TrapQueueSettings.DISABLED, true, List.of(), "a").check());
    }
}
