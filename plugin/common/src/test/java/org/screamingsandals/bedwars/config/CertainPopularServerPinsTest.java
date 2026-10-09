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

package org.screamingsandals.bedwars.config;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A value set in a variant's {@code config:} block beats config.yml, so the bundled certain-popular-server variant
 * must not pin values that config.yml controls. Only values that differ from the config.yml defaults are pinned.
 */
class CertainPopularServerPinsTest {
    /** Paths under {@code config:} that must follow config.yml (the variant leaves them out). */
    private static final List<List<String>> FOLLOWS_CONFIG_YML = List.of(
            List.of("modes", "only-via-mode-selection"),
            List.of("bed-destruction", "announce"),
            List.of("sudden-death", "enabled"),
            List.of("sudden-death", "destroy-targets"),
            List.of("sudden-death", "bossbar-message"),
            List.of("sudden-death", "dragon", "per-team"),
            List.of("sudden-death", "dragon", "turn-rate"),
            List.of("sudden-death", "dragon", "invulnerable"),
            List.of("sudden-death", "dragon", "damage-own-team"),
            List.of("sudden-death", "dragon", "block-destruction"),
            List.of("game-end-by-time", "mode"),
            List.of("setup", "diamond-spawner-type"),
            List.of("setup", "emerald-spawner-type")
    );

    private static ConfigurationNode load() throws IOException {
        try (var stream = CertainPopularServerPinsTest.class.getResourceAsStream("/variants/certain-popular-server.yml")) {
            assertNotNull(stream, "bundled certain-popular-server.yml not found on the classpath");
            return YamlConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
                    .build()
                    .load();
        }
    }

    @Test
    void doesNotPinConfigYmlDefaults() throws IOException {
        var config = load().node("config");
        for (var path : FOLLOWS_CONFIG_YML) {
            assertTrue(config.node(path.toArray()).virtual(),
                    "config." + String.join(".", path) + " must follow config.yml, so it must not be pinned in the variant");
        }
    }

    @Test
    void pinsTheValuesThatDifferFromConfigYml() throws IOException {
        var config = load().node("config");
        assertEquals(0.8, config.node("sudden-death", "dragon", "speed").getDouble());
        assertFalse(config.node("setup", "team-generator-hologram").getBoolean(true));
        assertEquals("certain-popular-server/upgrade-shop.yml", config.node("setup", "upgrade-shop-file").getString());
        assertEquals(List.of("iron", "gold"), config.node("setup", "team-generator-types").childrenList().stream()
                .map(ConfigurationNode::getString)
                .toList());
    }
}
