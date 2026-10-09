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

package org.screamingsandals.bedwars.game.upgrade.builtin;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeSpecTest {
    private static ConfigurationNode yaml(String text) throws Exception {
        return YamlConfigurationLoader.builder().buildAndLoadString(text);
    }

    private static final String SHIPPED = String.join("\n",
            "spawner-types: [iron, gold]",
            "auto-discover-spawners-if-not-linked: true",
            "auto-discover-max-distance: 25",
            "emerald-spawner-type: emerald",
            "tiers:",
            "  - resource-bonus: 0.5",
            "  - resource-bonus: 1.0",
            "  - resource-bonus: 1.0",
            "    emerald-interval: 60",
            "    emerald-amount: 1",
            "  - resource-bonus: 2.0",
            "    emerald-interval: 60",
            "    emerald-amount: 1",
            "");

    @Test
    void shippedLikeForge() throws Exception {
        var warnings = new ArrayList<String>();
        var spec = ForgeSpec.parse(yaml(SHIPPED), warnings);
        assertTrue(warnings.isEmpty());
        assertEquals(List.of("iron", "gold"), spec.spawnerTypes());
        assertEquals(4, spec.maxLevel());
        assertEquals(List.of(0.5, 1.0, 1.0, 2.0), spec.tiers().stream().map(ForgeSpec.ForgeTier::resourceBonus).toList());
        assertTrue(spec.tierFor(3).spawnsEmeralds());
        assertEquals(60, spec.tierFor(3).emeraldIntervalSeconds());
        assertEquals(1, spec.tierFor(3).emeraldAmount());
        assertFalse(spec.tierFor(2).spawnsEmeralds());
        assertSame(ForgeSpec.ForgeTier.NONE, spec.tierFor(0));
        assertSame(ForgeSpec.ForgeTier.NONE, spec.tierFor(-2));
        assertSame(spec.tierFor(4), spec.tierFor(9)); // clamped to the last tier
    }

    @Test
    void defaults() throws Exception {
        var spec = ForgeSpec.parse(yaml("tiers: [{resource-bonus: 0.5}]"), new ArrayList<>());
        assertEquals(List.of("iron", "gold"), spec.spawnerTypes());
        assertTrue(spec.autoDiscover());
        assertEquals(25.0, spec.autoDiscoverMaxDistance());
        assertEquals("emerald", spec.emeraldSpawnerType());
        assertEquals(1, spec.maxLevel());
    }

    @Test
    void spawnerTypesAreNormalised() throws Exception {
        var spec = ForgeSpec.parse(yaml("spawner-types: [IRON, ' Gold ', '']\ntiers: [{resource-bonus: 1}]"), new ArrayList<>());
        assertEquals(List.of("iron", "gold"), spec.spawnerTypes());
    }

    @Test
    void missingOrEmptyTiersFail() {
        assertThrows(ConfigurateException.class, () -> ForgeSpec.parse(yaml("spawner-types: [iron]"), new ArrayList<>()));
        assertThrows(ConfigurateException.class, () -> ForgeSpec.parse(yaml("tiers: []"), new ArrayList<>()));
        assertThrows(ConfigurateException.class, () -> ForgeSpec.parse(yaml("tiers: 3"), new ArrayList<>()));
    }

    @Test
    void invalidTierValuesAreCorrected() throws Exception {
        var warnings = new ArrayList<String>();
        var spec = ForgeSpec.parse(yaml(String.join("\n",
                "tiers:",
                "  - resource-bonus: -1",
                "  - emerald-interval: -5",
                "  - emerald-amount: 0",
                "  - 7",
                "")), warnings);
        assertEquals(4, warnings.size());
        assertEquals(0.0, spec.tierFor(1).resourceBonus());
        assertEquals(0, spec.tierFor(2).emeraldIntervalSeconds());
        assertEquals(1, spec.tierFor(3).emeraldAmount());
        assertEquals(ForgeSpec.ForgeTier.NONE, spec.tierFor(4));
    }
}
