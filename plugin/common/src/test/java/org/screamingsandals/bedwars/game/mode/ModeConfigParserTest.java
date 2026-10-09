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

package org.screamingsandals.bedwars.game.mode;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeConfigParserTest {

    private static ConfigurationNode yaml(String text) throws Exception {
        return YamlConfigurationLoader.builder().buildAndLoadString(text).node("modes", "list");
    }

    @Test
    void defaultsProduceTwelveModes() throws Exception {
        var node = BasicConfigurationNode.root().set(ModeDefaults.defaultModeList());
        var result = ModeConfigParser.parse(node);
        assertTrue(result.warnings().isEmpty(), result.warnings().toString());
        assertEquals(List.of("1v1", "2v2", "3v3", "4v4", "1v1v1", "2v2v2", "3v3v3", "4v4v4",
                        "1v1v1v1", "2v2v2v2", "3v3v3v3", "4v4v4v4"),
                result.modes().stream().map(ModeDefinition::id).collect(Collectors.toList()));
        var fourVsFour = result.modes().get(3);
        assertEquals(2, fourVsFour.teamCount());
        assertEquals(4, fourVsFour.teamSize());
        assertEquals(8, fourVsFour.minPlayers());
        assertEquals("<yellow>4v4", fourVsFour.displayName());
        assertEquals(2, result.modes().get(4).minPlayers()); // 1v1v1
    }

    @Test
    void mixedEntries() throws Exception {
        var node = yaml(String.join("\n",
                "modes:",
                "  list:",
                "  - id: custom3",
                "    display-name: '<red>Custom'",
                "    team-count: 3",
                "    team-size: 3",
                "  - id: '4V4 '",
                "    team-count: 2",
                "    team-size: 4",
                "  - id: custom3",
                "    team-count: 2",
                "    team-size: 2",
                "  - id: bad id",
                "    team-count: 2",
                "    team-size: 2",
                "  - id: oneteam",
                "    team-count: 1",
                "    team-size: 2",
                "  - id: nosize",
                "    team-count: 2",
                "    team-size: 0",
                "  - id: toomany",
                "    team-count: 2",
                "    team-size: 2",
                "    min-players: 99",
                "  - id: off",
                "    enabled: false",
                "    team-count: 2",
                "    team-size: 2",
                "  - id: listed",
                "    team-count: 2",
                "    team-size: 2",
                "    arenas: [A, B]",
                "  - plain",
                ""));
        var result = ModeConfigParser.parse(node);
        assertEquals(List.of("custom3", "4v4", "toomany", "listed"),
                result.modes().stream().map(ModeDefinition::id).collect(Collectors.toList()));
        assertEquals(6, result.modes().get(0).minPlayers()); // default 2 x team-size
        assertEquals("<red>Custom", result.modes().get(0).displayName());
        assertEquals("4v4", result.modes().get(1).displayName()); // display name defaults to the id
        assertEquals(4, result.modes().get(2).minPlayers()); // clamped from 99
        assertEquals(List.of("A", "B"), result.modes().get(3).arenas());

        var warnings = result.warnings();
        assertEquals(6, warnings.size(), warnings.toString());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("duplicate id 'custom3'")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("invalid or missing id 'bad id'")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("oneteam") && w.contains("team-count")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("nosize") && w.contains("team-size")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("toomany") && w.contains("clamped to 4")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("is not a map")));
    }

    @Test
    void virtualNodeIsEmptyWithoutWarnings() {
        var result = ModeConfigParser.parse(BasicConfigurationNode.root().node("modes", "list"));
        assertTrue(result.modes().isEmpty());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void scalarNodeGivesOneWarning() throws Exception {
        var result = ModeConfigParser.parse(BasicConfigurationNode.root().set("not a list"));
        assertTrue(result.modes().isEmpty());
        assertEquals(1, result.warnings().size());
    }
}
