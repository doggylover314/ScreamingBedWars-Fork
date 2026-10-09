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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectUpgradeSpecTest {
    private static ConfigurationNode yaml(String text) throws Exception {
        return YamlConfigurationLoader.builder().buildAndLoadString(text);
    }

    @Test
    void shippedStyleHaste() throws Exception {
        var warnings = new ArrayList<String>();
        var spec = EffectUpgradeSpec.parse(yaml("effect: minecraft:haste\nmax-level: 2\namplifiers: [0, 1]\nduration: -1\nambient: true\nparticles: false\nicon: true\n"), warnings);
        assertTrue(warnings.isEmpty());
        assertEquals("minecraft:haste", spec.effect());
        assertEquals(-1, spec.amplifierFor(0));
        assertEquals(0, spec.amplifierFor(1));
        assertEquals(1, spec.amplifierFor(2));
        assertEquals(1, spec.amplifierFor(3)); // clamped to the max level
        assertTrue(spec.permanent());
        assertEquals(Integer.MAX_VALUE, spec.appliedDuration());
        assertTrue(spec.ambient());
        assertFalse(spec.particles());
        assertTrue(spec.icon());
    }

    @Test
    void defaultsAndImplicitAmplifiers() throws Exception {
        var one = EffectUpgradeSpec.parse(yaml("effect: haste"), new ArrayList<>());
        assertEquals(1, one.maxLevel());
        assertEquals(0, one.amplifierFor(1));
        assertEquals(0, one.amplifierFor(2));

        var three = EffectUpgradeSpec.parse(yaml("effect: haste\nmax-level: 3\n"), new ArrayList<>());
        assertEquals(2, three.amplifierFor(3));
    }

    @Test
    void finiteDurationIsKept() throws Exception {
        var spec = EffectUpgradeSpec.parse(yaml("effect: haste\nduration: 1200\n"), new ArrayList<>());
        assertFalse(spec.permanent());
        assertEquals(1200, spec.appliedDuration());
    }

    @Test
    void invalidValuesAreCorrectedWithWarnings() throws Exception {
        var warnings = new ArrayList<String>();
        var spec = EffectUpgradeSpec.parse(yaml("effect: haste\nduration: 0\nmax-level: 0\namplifiers: [-1, 2]\n"), warnings);
        assertEquals(3, warnings.size());
        assertTrue(spec.permanent());
        assertEquals(1, spec.maxLevel());
        assertEquals(0, spec.amplifiers().get(0));
        assertEquals(2, spec.amplifiers().get(1));
    }

    @Test
    void missingEffectFails() {
        assertThrows(ConfigurateException.class, () -> EffectUpgradeSpec.parse(yaml("max-level: 2"), new ArrayList<>()));
        assertThrows(ConfigurateException.class, () -> EffectUpgradeSpec.parse(yaml("effect: '  '"), new ArrayList<>()));
    }

    @Test
    void needsReapply() {
        int max = Integer.MAX_VALUE;
        assertTrue(EffectUpgradeSpec.needsReapply(null, null, 0, true, 60));      // absent
        assertTrue(EffectUpgradeSpec.needsReapply(0, max, 1, true, 60));          // amplifier differs
        assertFalse(EffectUpgradeSpec.needsReapply(1, max, 1, true, 60));         // permanent and long
        assertTrue(EffectUpgradeSpec.needsReapply(1, 200, 1, true, 60));          // permanent but short
        assertFalse(EffectUpgradeSpec.needsReapply(1, -1, 1, true, 60));          // vanilla infinite
        assertTrue(EffectUpgradeSpec.needsReapply(1, 50, 1, false, 60));          // finite and about to expire
        assertFalse(EffectUpgradeSpec.needsReapply(1, 500, 1, false, 60));        // finite and fresh
        assertTrue(EffectUpgradeSpec.needsReapply(1, 80, 1, false, 60));          // border: refreshTicks + 20
        assertFalse(EffectUpgradeSpec.needsReapply(1, 81, 1, false, 60));
    }
}
