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
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceLadderTest {
    private static ConfigurationNode yaml(String text) throws Exception {
        return YamlConfigurationLoader.builder().buildAndLoadString(text);
    }

    private static PriceSpec diamond(int amount) {
        return PriceSpec.of(amount, "diamond");
    }

    @Test
    void readsBaseAndTeamSizeLadders() throws Exception {
        var warnings = new ArrayList<String>();
        var ladder = PriceLadder.parse(yaml(
                "prices: [2 of diamond, 4 of diamond, 8 of diamond, 16 of diamond]\n"
                        + "prices-by-team-size:\n"
                        + "  \"3\": [5 of diamond, 10 of diamond]\n"
                        + "  4: [6 of diamond]\n"), null, warnings);

        var base = List.of(diamond(2), diamond(4), diamond(8), diamond(16));
        assertEquals(base, ladder.base());
        assertEquals(base, ladder.forTeamSize(1));
        assertEquals(base, ladder.forTeamSize(2));
        assertEquals(List.of(diamond(5), diamond(10)), ladder.forTeamSize(3));
        assertEquals(List.of(diamond(6)), ladder.forTeamSize(4));
        assertEquals(List.of(diamond(6)), ladder.forTeamSize(8));
        assertTrue(warnings.isEmpty(), warnings::toString);
        assertTrue(!ladder.isEmpty());
    }

    @Test
    void scalarPricesBecomeOneEntry() throws Exception {
        var warnings = new ArrayList<String>();
        var ladder = PriceLadder.parse(yaml("prices: 4 of diamond"), null, warnings);

        assertEquals(List.of(diamond(4)), ladder.base());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void usesTheDefaultCurrencyForBareNumbers() throws Exception {
        var warnings = new ArrayList<String>();
        var ladder = PriceLadder.parse(yaml("prices: [4]"), "iron", warnings);

        assertEquals(List.of(PriceSpec.of(4, "iron")), ladder.base());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void mapFormEntriesAreAccepted() throws Exception {
        var warnings = new ArrayList<String>();
        var ladder = PriceLadder.parse(yaml("prices:\n  - {amount: 3, currency: Gold}\n  - {amount: 5}\n"), "iron", warnings);

        assertEquals(List.of(PriceSpec.of(3, "gold"), PriceSpec.of(5, "iron")), ladder.base());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void invalidEntriesAreSkippedWithAWarning() throws Exception {
        var warnings = new ArrayList<String>();
        var ladder = PriceLadder.parse(yaml("prices: [x, 2 of diamond]"), null, warnings);

        assertEquals(List.of(diamond(2)), ladder.base());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("x"), warnings.get(0));
    }

    @Test
    void invalidTeamSizeKeysAreSkipped() throws Exception {
        var warnings = new ArrayList<String>();
        var ladder = PriceLadder.parse(yaml(
                "prices: [1 of diamond]\n"
                        + "prices-by-team-size:\n"
                        + "  abc: [1 of diamond]\n"
                        + "  \"0\": [1 of diamond]\n"), null, warnings);

        assertEquals(2, warnings.size(), warnings::toString);
        assertEquals(List.of(diamond(1)), ladder.forTeamSize(3));
    }

    @Test
    void teamSizeSectionThatIsNotAMapIsReported() throws Exception {
        var warnings = new ArrayList<String>();
        var ladder = PriceLadder.parse(yaml("prices: [1 of diamond]\nprices-by-team-size: [1, 2]\n"), null, warnings);

        assertEquals(1, warnings.size());
        assertEquals(List.of(diamond(1)), ladder.forTeamSize(4));
    }

    @Test
    void emptyTeamSizeListFallsBackToTheBaseLadder() throws Exception {
        var warnings = new ArrayList<String>();
        var ladder = PriceLadder.parse(yaml("prices: [1 of diamond]\nprices-by-team-size:\n  \"3\": []\n"), null, warnings);

        assertEquals(List.of(diamond(1)), ladder.forTeamSize(3));
    }

    @Test
    void noKeysGiveAnEmptyLadder() throws Exception {
        var warnings = new ArrayList<String>();
        var ladder = PriceLadder.parse(yaml("name: upgrade\n"), "diamond", warnings);

        assertTrue(ladder.isEmpty());
        assertTrue(ladder.base().isEmpty());
        assertEquals(List.of(), ladder.forTeamSize(4));
        assertTrue(warnings.isEmpty());
        assertTrue(PriceLadder.EMPTY.isEmpty());
    }

    @Test
    void ladderIsImmutable() throws Exception {
        var ladder = PriceLadder.parse(yaml("prices: [1 of diamond]\nprices-by-team-size:\n  \"3\": [2 of diamond]\n"), null, new ArrayList<>());

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> ladder.base().add(diamond(9)));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> ladder.forTeamSize(3).add(diamond(9)));
    }
}
