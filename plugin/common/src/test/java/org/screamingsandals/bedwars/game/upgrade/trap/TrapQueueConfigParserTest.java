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
import org.screamingsandals.bedwars.game.upgrade.EffectSpec;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapQueueConfigParserTest {
    private static ConfigurationNode section(String yaml) throws Exception {
        return YamlConfigurationLoader.builder().buildAndLoadString(yaml).node("trap-queue");
    }

    private static final String SHIPPED = String.join("\n",
            "trap-queue:",
            "  enabled: true",
            "  max-size: 3",
            "  currency: diamond",
            "  costs: [1, 2, 4]",
            "  cooldown-seconds: 15",
            "  detection-range: 10",
            "  allow-duplicates: true",
            "  traps:",
            "    its-a-trap:",
            "      name: \"@fork.in_game.traps.names.its_a_trap\"",
            "      icon: tripwire_hook",
            "      enemy-effects:",
            "        - effect: minecraft:blindness",
            "          duration: 160",
            "          amplifier: 0",
            "        - effect: minecraft:slowness",
            "          duration: 160",
            "          amplifier: 0",
            "    counter-offensive:",
            "      name: \"@fork.in_game.traps.names.counter_offensive\"",
            "      icon: feather",
            "      team-effect-range: 20",
            "      team-effects:",
            "        - effect: minecraft:speed",
            "          duration: 300",
            "          amplifier: 1",
            "        - effect: minecraft:jump_boost",
            "          duration: 300",
            "          amplifier: 1",
            "    alarm:",
            "      name: \"@fork.in_game.traps.names.alarm\"",
            "      icon: redstone_torch",
            "      reveal-invisible: true",
            "      affect-all-intruders: true",
            "    miner-fatigue:",
            "      name: \"@fork.in_game.traps.names.miner_fatigue\"",
            "      icon: iron_pickaxe",
            "      enemy-effects:",
            "        - effect: minecraft:mining_fatigue",
            "          duration: 200",
            "          amplifier: 0",
            "");

    @Test
    void shippedLikeSection() throws Exception {
        var result = TrapQueueConfigParser.parse(section(SHIPPED));
        assertTrue(result.warnings().isEmpty(), () -> result.warnings().toString());

        var settings = result.config().settings();
        assertTrue(settings.enabled());
        assertEquals(3, settings.maxSize());
        assertEquals("diamond", settings.currency());
        assertEquals(List.of(1, 2, 4), settings.costs());
        assertEquals(15, settings.cooldownSeconds());
        assertEquals(10.0, settings.defaultDetectionRange());
        assertTrue(settings.allowDuplicates());

        var traps = result.config().traps();
        assertEquals(List.of("its-a-trap", "counter-offensive", "alarm", "miner-fatigue"), List.copyOf(traps.keySet()));

        var itsATrap = traps.get("its-a-trap");
        assertEquals(2, itsATrap.enemyEffects().size());
        assertEquals(new EffectSpec("minecraft:blindness", 160, 0, false, true, true), itsATrap.enemyEffects().get(0));
        assertEquals("minecraft:slowness", itsATrap.enemyEffects().get(1).effect());
        assertEquals(10.0, itsATrap.detectionRange()); // inherited
        assertEquals("tripwire_hook", itsATrap.icon());
        assertTrue(itsATrap.messageIntruder());
        assertFalse(itsATrap.affectAllIntruders());

        var counter = traps.get("counter-offensive");
        assertEquals(20.0, counter.teamEffectRange());
        assertEquals(2, counter.teamEffects().size());
        assertTrue(counter.enemyEffects().isEmpty());

        var alarm = traps.get("alarm");
        assertTrue(alarm.revealInvisible());
        assertTrue(alarm.affectAllIntruders());
        assertEquals("@fork.in_game.traps.names.alarm", alarm.name());
    }

    @Test
    void virtualSectionDisablesTheFeature() throws Exception {
        var result = TrapQueueConfigParser.parse(section("other: 1"));
        assertSame(TrapQueueConfig.DISABLED, result.config());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void emptySectionHasNoTraps() throws Exception {
        var result = TrapQueueConfigParser.parse(section("trap-queue: {}"));
        assertFalse(result.config().settings().enabled());
        assertTrue(result.config().traps().isEmpty());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("no traps"));
    }

    @Test
    void invalidSettingsAreCorrected() throws Exception {
        var yaml = String.join("\n",
                "trap-queue:",
                "  max-size: 0",
                "  costs: []",
                "  cooldown-seconds: -1",
                "  detection-range: 0",
                "  traps:",
                "    a: {reveal-invisible: true}",
                "");
        var result = TrapQueueConfigParser.parse(section(yaml));
        var settings = result.config().settings();
        assertEquals(1, settings.maxSize());
        assertEquals(List.of(1), settings.costs());
        assertEquals(0, settings.cooldownSeconds());
        assertEquals(7.0, settings.defaultDetectionRange());
        assertEquals(4, result.warnings().size(), () -> result.warnings().toString());
        assertTrue(settings.enabled());
    }

    @Test
    void invalidCostsAreSkipped() throws Exception {
        var yaml = String.join("\n",
                "trap-queue:",
                "  costs: [1, x, -2]",
                "  traps:",
                "    a: {reveal-invisible: true}",
                "");
        var result = TrapQueueConfigParser.parse(section(yaml));
        assertEquals(List.of(1), result.config().settings().costs());
        assertEquals(2, result.warnings().size(), () -> result.warnings().toString());
    }

    @Test
    void scalarCostIsAccepted() throws Exception {
        var yaml = String.join("\n",
                "trap-queue:",
                "  costs: 3",
                "  traps:",
                "    a: {reveal-invisible: true}",
                "");
        assertEquals(List.of(3), TrapQueueConfigParser.parse(section(yaml)).config().settings().costs());
    }

    @Test
    void defaultsWhenKeysAreMissing() throws Exception {
        var yaml = String.join("\n",
                "trap-queue:",
                "  traps:",
                "    a: {reveal-invisible: true}",
                "");
        var result = TrapQueueConfigParser.parse(section(yaml));
        var settings = result.config().settings();
        assertTrue(settings.enabled());
        assertEquals(3, settings.maxSize());
        assertEquals("diamond", settings.currency());
        assertEquals(List.of(1, 2, 4), settings.costs());
        assertEquals(15, settings.cooldownSeconds());
        assertEquals(7.0, settings.defaultDetectionRange());
        assertTrue(settings.allowDuplicates());
        assertEquals("a", result.config().traps().get("a").name()); // name defaults to the id
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void trapIdsAreValidatedAndNormalised() throws Exception {
        var yaml = String.join("\n",
                "trap-queue:",
                "  traps:",
                "    'Bad Id!': {reveal-invisible: true}",
                "    MyTrap: {reveal-invisible: true}",
                "");
        var result = TrapQueueConfigParser.parse(section(yaml));
        assertEquals(List.of("mytrap"), List.copyOf(result.config().traps().keySet()));
        assertEquals("MyTrap", result.config().traps().get("mytrap").sourceKey());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void duplicateIdsAfterNormalisationAreSkipped() throws Exception {
        var yaml = String.join("\n",
                "trap-queue:",
                "  traps:",
                "    MyTrap: {reveal-invisible: true}",
                "    mytrap: {reveal-invisible: true}",
                "");
        var result = TrapQueueConfigParser.parse(section(yaml));
        assertEquals(1, result.config().traps().size());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void trapWithoutEffectsIsKeptWithAWarning() throws Exception {
        var yaml = String.join("\n",
                "trap-queue:",
                "  traps:",
                "    quiet: {name: Quiet}",
                "");
        var result = TrapQueueConfigParser.parse(section(yaml));
        assertEquals(1, result.config().traps().size());
        assertEquals(1, result.warnings().size());
        assertTrue(result.config().settings().enabled());
    }

    @Test
    void scalarEffectsAreSkippedWithAWarning() throws Exception {
        var yaml = String.join("\n",
                "trap-queue:",
                "  traps:",
                "    t:",
                "      enemy-effects:",
                "        - blindness 2",
                "        - {effect: slowness, duration: 20}",
                "");
        var result = TrapQueueConfigParser.parse(section(yaml));
        assertEquals(1, result.config().traps().get("t").enemyEffects().size());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void perTrapRangeOverridesTheDefault() throws Exception {
        var yaml = String.join("\n",
                "trap-queue:",
                "  detection-range: 10",
                "  traps:",
                "    near: {detection-range: 4, reveal-invisible: true}",
                "    far: {reveal-invisible: true}",
                "    bad: {detection-range: -1, reveal-invisible: true}",
                "");
        var result = TrapQueueConfigParser.parse(section(yaml));
        assertEquals(4.0, result.config().traps().get("near").detectionRange());
        assertEquals(10.0, result.config().traps().get("far").detectionRange());
        assertEquals(10.0, result.config().traps().get("bad").detectionRange());
        assertEquals(1, result.warnings().size());
    }
}
