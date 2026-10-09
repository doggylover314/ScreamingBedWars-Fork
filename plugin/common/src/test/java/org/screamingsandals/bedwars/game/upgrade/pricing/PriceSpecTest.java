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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceSpecTest {
    private static ConfigurationNode yaml(String text) throws Exception {
        return YamlConfigurationLoader.builder().buildAndLoadString(text);
    }

    @Test
    void parsesTextForms() {
        assertEquals(PriceSpec.of(4, "diamond"), PriceSpec.parse("4 of diamond", null).orElseThrow());
        assertEquals(PriceSpec.of(4, "diamond"), PriceSpec.parse("4 diamond", null).orElseThrow());
        assertEquals(PriceSpec.of(12, "iron"), PriceSpec.parse("  12   of   Iron ", null).orElseThrow());
        assertEquals(PriceSpec.of(0, "diamond"), PriceSpec.parse("0 of diamond", null).orElseThrow());
    }

    @Test
    void usesTheDefaultCurrencyOnlyWhenNoneIsGiven() {
        assertEquals(PriceSpec.of(4, "emerald"), PriceSpec.parse("4", "emerald").orElseThrow());
        assertEquals(PriceSpec.of(4, "diamond"), PriceSpec.parse("4 of diamond", "emerald").orElseThrow());
        assertTrue(PriceSpec.parse("4", null).isEmpty());
        assertTrue(PriceSpec.parse("4", "  ").isEmpty());
    }

    @Test
    void rejectsInvalidText() {
        assertTrue(PriceSpec.parse("abc", "iron").isEmpty());
        assertTrue(PriceSpec.parse("-1 of diamond", "iron").isEmpty());
        assertTrue(PriceSpec.parse("9999999999 of diamond", "iron").isEmpty()); // 10 digits
        assertTrue(PriceSpec.parse((String) null, "iron").isEmpty());
        assertTrue(PriceSpec.parse("", "iron").isEmpty());
    }

    @Test
    void parsesNodes() throws Exception {
        assertEquals(PriceSpec.of(3, "gold"), PriceSpec.parse(yaml("price: 3 of gold").node("price"), null).orElseThrow());
        assertEquals(PriceSpec.of(3, "gold"), PriceSpec.parse(yaml("price: {amount: 3, currency: Gold}").node("price"), null).orElseThrow());
        assertEquals(PriceSpec.of(3, "iron"), PriceSpec.parse(yaml("price: {amount: 3}").node("price"), "iron").orElseThrow());
        assertTrue(PriceSpec.parse(yaml("price: {amount: -2}").node("price"), "iron").isEmpty());
        assertTrue(PriceSpec.parse(yaml("price: {amount: 3}").node("price"), null).isEmpty());
    }

    @Test
    void parsesNumericScalarNodes() throws Exception {
        assertEquals(PriceSpec.of(5, "iron"), PriceSpec.parse(yaml("price: 5").node("price"), "iron").orElseThrow());
    }

    @Test
    void constructorValidates() {
        assertThrows(IllegalArgumentException.class, () -> new PriceSpec(-1, "x"));
        assertThrows(IllegalArgumentException.class, () -> new PriceSpec(1, "  "));
        assertEquals("diamond", new PriceSpec(1, " Diamond ").currency());
    }
}
