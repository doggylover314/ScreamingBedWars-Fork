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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapQueueSettingsTest {
    private static TrapQueueSettings settings(int max, List<Integer> costs) {
        return new TrapQueueSettings(true, max, "diamond", costs, 15, 7, true);
    }

    @Test
    void costEscalatesWithTheQueueSize() {
        var s = settings(3, List.of(1, 2, 4));
        assertEquals(1, s.costFor(0).getAsInt());
        assertEquals(2, s.costFor(1).getAsInt());
        assertEquals(4, s.costFor(2).getAsInt());
        assertTrue(s.costFor(3).isEmpty());
        assertEquals(1, s.costFor(-1).getAsInt());
    }

    @Test
    void lastCostRepeats() {
        var s = settings(3, List.of(1));
        assertEquals(1, s.costFor(0).getAsInt());
        assertEquals(1, s.costFor(1).getAsInt());
        assertEquals(1, s.costFor(2).getAsInt());
    }

    @Test
    void maxSizeWinsOverLongerCostList() {
        var s = settings(2, List.of(1, 2, 4, 8));
        assertEquals(2, s.costFor(1).getAsInt());
        assertTrue(s.costFor(2).isEmpty());
    }

    @Test
    void noCostsMeansFree() {
        assertEquals(0, settings(3, List.of()).costFor(0).getAsInt());
    }

    @Test
    void priceUsesTheCurrency() {
        var s = settings(3, List.of(1, 2, 4));
        assertEquals(PriceSpec.of(2, "diamond"), s.priceFor(1).orElseThrow());
        assertTrue(s.priceFor(3).isEmpty());
    }

    @Test
    void disabledSettingsOfferNothing() {
        assertTrue(TrapQueueSettings.DISABLED.costFor(0).isEmpty());
        assertTrue(TrapQueueSettings.DISABLED.priceFor(0).isEmpty());
    }
}
