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

package org.screamingsandals.bedwars.game.upgrade;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectSpecTest {
    private static ConfigurationNode yaml(String text) throws Exception {
        return YamlConfigurationLoader.builder().buildAndLoadString(text);
    }

    @Test
    void parsesTheMapFormWithDefaults() throws Exception {
        var warnings = new ArrayList<String>();
        var spec = EffectSpec.parse(yaml("e: {effect: minecraft:blindness, duration: 160, amplifier: 0}").node("e"), "ctx", warnings).orElseThrow();
        assertEquals(new EffectSpec("minecraft:blindness", 160, 0, false, true, true), spec);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void parsesAllFlagsAndAliases() throws Exception {
        var warnings = new ArrayList<String>();
        var spec = EffectSpec.parse(yaml("e: {effect: SPEED, duration: 20, amplifier: 1, ambient: true, has-particles: false, has-icon: false}").node("e"), "ctx", warnings).orElseThrow();
        assertEquals(new EffectSpec("speed", 20, 1, true, false, false), spec);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void particlesAndIconKeysWinOverTheirAliases() throws Exception {
        var spec = EffectSpec.parse(yaml("e: {effect: speed, duration: 20, particles: false, has-particles: true, icon: false, has-icon: true}").node("e"), "ctx", new ArrayList<>()).orElseThrow();
        assertFalse(spec.particles());
        assertFalse(spec.icon());
    }

    @Test
    void rejectsTheScalarForm() throws Exception {
        var warnings = new ArrayList<String>();
        assertTrue(EffectSpec.parse(yaml("e: blindness 2").node("e"), "trap x", warnings).isEmpty());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).startsWith("trap x"));
    }

    @Test
    void rejectsMissingEffectAndInvalidDuration() throws Exception {
        var warnings = new ArrayList<String>();
        assertTrue(EffectSpec.parse(yaml("e: {duration: 20}").node("e"), "ctx", warnings).isEmpty());
        assertTrue(EffectSpec.parse(yaml("e: {effect: speed, duration: 0}").node("e"), "ctx", warnings).isEmpty());
        assertTrue(EffectSpec.parse(yaml("e: {effect: speed}").node("e"), "ctx", warnings).isEmpty());
        assertEquals(3, warnings.size());
    }

    @Test
    void negativeAmplifierBecomesZero() throws Exception {
        var warnings = new ArrayList<String>();
        var spec = EffectSpec.parse(yaml("e: {effect: speed, duration: 20, amplifier: -3}").node("e"), "ctx", warnings).orElseThrow();
        assertEquals(0, spec.amplifier());
        assertEquals(1, warnings.size());
    }

    @Test
    void parseListHandlesAllNodeShapes() throws Exception {
        var warnings = new ArrayList<String>();
        var root = yaml("list:\n  - {effect: speed, duration: 20}\n  - bad\n  - {effect: slowness, duration: 40, amplifier: 2}\nnotalist: 5\n");

        List<EffectSpec> list = EffectSpec.parseList(root.node("list"), "ctx", warnings);
        assertEquals(2, list.size());
        assertEquals("speed", list.get(0).effect());
        assertEquals("slowness", list.get(1).effect());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("ctx[1]"));

        warnings.clear();
        assertTrue(EffectSpec.parseList(root.node("notalist"), "ctx", warnings).isEmpty());
        assertEquals(1, warnings.size());

        warnings.clear();
        assertTrue(EffectSpec.parseList(root.node("missing"), "ctx", warnings).isEmpty());
        assertTrue(warnings.isEmpty());
    }
}
