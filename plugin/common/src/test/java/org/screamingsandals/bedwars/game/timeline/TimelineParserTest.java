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

package org.screamingsandals.bedwars.game.timeline;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimelineParserTest {
    /**
     * The timeline block of the bundled certain-popular-server variant.
     */
    private static final String CPS_TIMELINE = """
            timeline:
              enabled: true
              announcements:
                chat: true
                title: true
                sound:
                  name: block.note_block.pling
                  source: master
                  volume: 1
                  pitch: 1
              spawner-tiers:
                diamond:
                  1: 30 seconds
                  2: 23 seconds
                  3: 12 seconds
                emerald:
                  1: 65 seconds
                  2: 50 seconds
                  3: 35 seconds
              events:
                - id: diamond-2
                  time: "6:00"
                  type: spawner-tier
                  spawner: diamond
                  tier: 2
                - id: emerald-2
                  time: "12:00"
                  type: spawner-tier
                  spawner: emerald
                  tier: 2
                - id: diamond-3
                  time: "18:00"
                  type: spawner-tier
                  spawner: diamond
                  tier: 3
                - id: emerald-3
                  time: "24:00"
                  type: spawner-tier
                  spawner: emerald
                  tier: 3
                - id: bed-destruction
                  time: "30:00"
                  type: bed-destruction
                - id: sudden-death
                  time: "40:00"
                  type: sudden-death
                - id: game-end
                  time: "50:00"
                  type: game-end
            """;

    private static ConfigurationNode yaml(String text) throws IOException {
        return YamlConfigurationLoader.builder().buildAndLoadString(text);
    }

    private static TimelineParser.Result parse(String timelineBody) throws IOException {
        return TimelineParser.parse(yaml(timelineBody).node("timeline"));
    }

    private static List<String> ids(TimelineDefinition definition) {
        return definition.events().stream().map(TimelineEventDefinition::id).collect(Collectors.toList());
    }

    @Test
    void parsesTheCertainPopularServerTimeline() throws IOException {
        var result = TimelineParser.parse(yaml(CPS_TIMELINE).node("timeline"));
        assertEquals(List.of(), result.warnings());

        var definition = result.definition();
        assertTrue(definition.enabled());
        assertFalse(definition.isEmpty());
        assertEquals(AnnouncementSettings.DEFAULT, definition.announcements());

        assertEquals(List.of("diamond-2", "emerald-2", "diamond-3", "emerald-3", "bed-destruction", "sudden-death", "game-end"), ids(definition));
        assertEquals(List.of(360L, 720L, 1080L, 1440L, 1800L, 2400L, 3000L),
                definition.events().stream().map(TimelineEventDefinition::timeSeconds).collect(Collectors.toList()));
        assertEquals(List.of(
                        TimelineEventType.SPAWNER_TIER, TimelineEventType.SPAWNER_TIER, TimelineEventType.SPAWNER_TIER,
                        TimelineEventType.SPAWNER_TIER, TimelineEventType.BED_DESTRUCTION, TimelineEventType.SUDDEN_DEATH,
                        TimelineEventType.GAME_END),
                definition.events().stream().map(TimelineEventDefinition::type).collect(Collectors.toList()));
        assertEquals(List.of("diamond", "emerald", "diamond", "emerald"),
                definition.events().subList(0, 4).stream().map(TimelineEventDefinition::spawnerType).collect(Collectors.toList()));
        assertEquals(List.of(2, 2, 3, 3),
                definition.events().subList(0, 4).stream().map(TimelineEventDefinition::tier).collect(Collectors.toList()));
        for (var event : definition.events().subList(4, 7)) {
            assertNull(event.spawnerType());
            assertEquals(0, event.tier());
            assertTrue(event.showOnSidebar());
        }

        assertEquals(600L, definition.intervalTicks("diamond", 1));
        assertEquals(460L, definition.intervalTicks("diamond", 2));
        assertEquals(240L, definition.intervalTicks("diamond", 3));
        assertEquals(1300L, definition.intervalTicks("emerald", 1));
        assertEquals(1000L, definition.intervalTicks("emerald", 2));
        assertEquals(700L, definition.intervalTicks("emerald", 3));
        assertEquals(460L, definition.effectiveIntervalTicks(definition.events().get(0)));
        assertEquals(700L, definition.effectiveIntervalTicks(definition.events().get(3)));
        assertNull(definition.effectiveIntervalTicks(definition.events().get(4)));
    }

    @Test
    void parsesTheShippedCertainPopularServerVariant() throws IOException {
        try (var stream = TimelineParserTest.class.getResourceAsStream("/variants/certain-popular-server.yml")) {
            assertNotNull(stream, "bundled certain-popular-server.yml not found on the classpath");
            var root = YamlConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
                    .build()
                    .load();
            var result = TimelineParser.parse(root.node("timeline"));
            assertEquals(List.of(), result.warnings());
            var definition = result.definition();
            assertFalse(definition.isEmpty());
            assertEquals(List.of("diamond-2", "emerald-2", "diamond-3", "emerald-3", "bed-destruction", "sudden-death", "game-end"), ids(definition));
            assertEquals(600L, definition.intervalTicks("diamond", 1));
            assertEquals(1300L, definition.intervalTicks("emerald", 1));
            // every tier event finds its interval in the table
            for (var event : definition.events()) {
                if (event.type() == TimelineEventType.SPAWNER_TIER) {
                    assertNotNull(definition.effectiveIntervalTicks(event), event.id());
                }
            }
            // the sidebar line the timeline feeds must be present
            assertTrue(root.node("config", "sidebar", "game", "additional-content", "content").childrenList().stream()
                    .anyMatch(line -> String.valueOf(line.raw()).contains("<final-kills>")));
        }
    }

    @Test
    void emptyOrVirtualNodesGiveTheEmptyDefinition() throws IOException {
        var virtual = TimelineParser.parse(yaml("other: 1").node("timeline"));
        assertSame(TimelineDefinition.EMPTY, virtual.definition());
        assertEquals(List.of(), virtual.warnings());

        var empty = TimelineParser.parse(yaml("timeline: {}").node("timeline"));
        assertSame(TimelineDefinition.EMPTY, empty.definition());
        assertEquals(List.of(), empty.warnings());
    }

    @Test
    void enabledFalseMakesTheDefinitionEmpty() throws IOException {
        var result = parse("""
                timeline:
                  enabled: false
                  events:
                    - {time: "1:00", type: game-end}
                """);
        assertFalse(result.definition().enabled());
        assertTrue(result.definition().isEmpty());
        assertEquals(1, result.definition().events().size());
    }

    @Test
    void unknownTypeIsSkippedWithOneWarning() throws IOException {
        var result = parse("""
                timeline:
                  events:
                    - {time: "1:00", type: explode-everything}
                    - {time: "2:00", type: game-end}
                """);
        assertEquals(1, result.warnings().size());
        assertEquals(1, result.definition().events().size());
        assertEquals(TimelineEventType.GAME_END, result.definition().events().get(0).type());
    }

    @Test
    void missingOrInvalidTimeIsSkipped() throws IOException {
        var result = parse("""
                timeline:
                  events:
                    - {type: game-end}
                    - {time: "6:99", type: game-end}
                    - {time: "5:00", type: sudden-death}
                """);
        assertEquals(2, result.warnings().size());
        assertEquals(List.of("sudden-death-2"), ids(result.definition()));
    }

    @Test
    void spawnerTierNeedsSpawnerAndTier() throws IOException {
        var result = parse("""
                timeline:
                  events:
                    - {time: "1:00", type: spawner-tier, spawner: diamond}
                    - {time: "2:00", type: spawner-tier, tier: 2}
                    - {time: "3:00", type: spawner-tier, spawner: diamond, tier: 0}
                """);
        assertTrue(result.definition().events().isEmpty());
        assertEquals(3, result.warnings().size());
    }

    @Test
    void romanTierAndSpawnerTypeAliasAreAccepted() throws IOException {
        var result = parse("""
                timeline:
                  spawner-tiers:
                    Diamond:
                      I: 30
                      II: 23 seconds
                  events:
                    - {time: "1:00", type: spawner-tier, spawner-type: DIAMOND, tier: II}
                """);
        assertEquals(List.of(), result.warnings());
        var event = result.definition().events().get(0);
        assertEquals("diamond", event.spawnerType());
        assertEquals(2, event.tier());
        assertEquals(460L, result.definition().effectiveIntervalTicks(event));
        assertEquals(600L, result.definition().intervalTicks("diamond", 1));
    }

    @Test
    void tierEventWithoutAnyIntervalIsKeptWithAWarning() throws IOException {
        var result = parse("""
                timeline:
                  events:
                    - {time: "1:00", type: spawner-tier, spawner: diamond, tier: 2}
                """);
        assertEquals(1, result.warnings().size());
        var event = result.definition().events().get(0);
        assertNull(result.definition().effectiveIntervalTicks(event));
    }

    @Test
    void intervalOverrideBeatsTheTable() throws IOException {
        var result = parse("""
                timeline:
                  spawner-tiers:
                    diamond:
                      2: 23 seconds
                  events:
                    - {time: "1:00", type: spawner-tier, spawner: diamond, tier: 2, interval: 10 seconds}
                    - {time: "2:00", type: spawner-tier, spawner: gold, tier: 2, interval: 5}
                    - {time: "3:00", type: spawner-tier, spawner: diamond, tier: 2, interval: nonsense}
                """);
        assertEquals(1, result.warnings().size()); // only the nonsense interval
        var events = result.definition().events();
        assertEquals(200L, events.get(0).intervalTicksOverride());
        assertEquals(200L, result.definition().effectiveIntervalTicks(events.get(0)));
        assertEquals(100L, result.definition().effectiveIntervalTicks(events.get(1)));
        assertNull(events.get(2).intervalTicksOverride());
        assertEquals(460L, result.definition().effectiveIntervalTicks(events.get(2)));
    }

    @Test
    void duplicateIdsGetTheIndexAppended() throws IOException {
        var result = parse("""
                timeline:
                  events:
                    - {id: boom, time: "1:00", type: bed-destruction}
                    - {id: boom, time: "2:00", type: sudden-death}
                """);
        assertEquals(1, result.warnings().size());
        assertEquals(List.of("boom", "boom-1"), ids(result.definition()));
    }

    @Test
    void defaultIdsUseTypeAndIndex() throws IOException {
        var result = parse("""
                timeline:
                  events:
                    - {time: "1:00", type: bed-destruction}
                    - {time: "2:00", type: sudden-death}
                """);
        assertEquals(List.of("bed-destruction-0", "sudden-death-1"), ids(result.definition()));
    }

    @Test
    void eventsAreSortedByTimeKeepingDeclarationOrder() throws IOException {
        var result = parse("""
                timeline:
                  events:
                    - {id: late, time: "12:00", type: game-end}
                    - {id: a, time: "6:00", type: sudden-death}
                    - {id: b, time: 360, type: bed-destruction}
                """);
        assertEquals(List.of("a", "b", "late"), ids(result.definition()));
    }

    @Test
    void unquotedClockTimesAreSecondsToo() throws IOException {
        // YAML 1.1 reads an unquoted 6:00 as the sexagesimal integer 360
        var result = parse("""
                timeline:
                  events:
                    - {id: unquoted, time: 6:00, type: game-end}
                """);
        assertEquals(360L, result.definition().events().get(0).timeSeconds());
    }

    @Test
    void eventsGivenAsMapUseTheKeysAsIds() throws IOException {
        var result = parse("""
                timeline:
                  events:
                    first: {time: "2:00", type: game-end}
                    second: {time: "1:00", type: sudden-death}
                """);
        assertEquals(List.of("second", "first"), ids(result.definition()));
    }

    @Test
    void eventsThatAreNeitherListNorMapWarn() throws IOException {
        var result = parse("""
                timeline:
                  enabled: true
                  events: nothing
                """);
        assertEquals(1, result.warnings().size());
        assertTrue(result.definition().events().isEmpty());
    }

    @Test
    void soundForms() throws IOException {
        var result = parse("""
                timeline:
                  events:
                    - {id: off, time: "1:00", type: spawner-tier, spawner: a, tier: 2, interval: 5, sound: false}
                    - {id: on, time: "2:00", type: spawner-tier, spawner: a, tier: 3, interval: 5, sound: true}
                    - {id: text, time: "3:00", type: spawner-tier, spawner: a, tier: 4, interval: 5, sound: "entity.player.levelup"}
                    - id: map
                      time: "4:00"
                      type: spawner-tier
                      spawner: a
                      tier: 5
                      interval: 5
                      sound: {name: block.anvil.land, source: Players, volume: 0.5, pitch: 2}
                    - id: broken
                      time: "5:00"
                      type: spawner-tier
                      spawner: a
                      tier: 6
                      interval: 5
                      sound: {volume: 3}
                    - {id: none, time: "6:00", type: spawner-tier, spawner: a, tier: 7, interval: 5}
                """);
        var events = result.definition().events();
        assertSame(SoundSpec.NONE, events.get(0).sound());
        assertTrue(events.get(0).sound().isNone());
        assertEquals(SoundSpec.DEFAULT_ANNOUNCEMENT, events.get(1).sound());
        assertEquals(new SoundSpec("entity.player.levelup", "master", 1f, 1f), events.get(2).sound());
        assertEquals(new SoundSpec("block.anvil.land", "players", 0.5f, 2f), events.get(3).sound());
        assertNull(events.get(4).sound());
        assertNull(events.get(5).sound());
        assertEquals(1, result.warnings().size()); // the sound without a name
    }

    @Test
    void announcementSettings() throws IOException {
        var disabled = parse("""
                timeline:
                  announcements:
                    chat: false
                    title: false
                    sound: false
                  events:
                    - {time: "1:00", type: game-end}
                """);
        assertEquals(new AnnouncementSettings(false, false, SoundSpec.NONE), disabled.definition().announcements());

        var defaults = parse("""
                timeline:
                  announcements:
                    chat: true
                  events:
                    - {time: "1:00", type: game-end}
                """);
        assertEquals(AnnouncementSettings.DEFAULT, defaults.definition().announcements());

        var custom = parse("""
                timeline:
                  announcements:
                    sound: block.bell.use
                  events:
                    - {time: "1:00", type: game-end}
                """);
        assertEquals(new AnnouncementSettings(true, true, new SoundSpec("block.bell.use", "master", 1f, 1f)), custom.definition().announcements());
    }

    @Test
    void announcementNeedsTextAndIsHiddenFromTheSidebarByDefault() throws IOException {
        var result = parse("""
                timeline:
                  events:
                    - {id: empty, time: "1:00", type: announcement}
                    - {id: msg, time: "2:00", type: announcement, message: "Hello"}
                    - {id: ttl, time: "3:00", type: announcement, title: "Hi", subtitle: "there"}
                    - {id: shown, time: "4:00", type: announcement, message: "Visible", show-on-sidebar: true}
                    - {id: hidden-end, time: "5:00", type: game-end, show-on-sidebar: false}
                    - {id: end, time: "6:00", type: game-end}
                """);
        assertEquals(1, result.warnings().size());
        assertEquals(List.of("msg", "ttl", "shown", "hidden-end", "end"), ids(result.definition()));
        var events = result.definition().events();
        assertFalse(events.get(0).showOnSidebar());
        assertFalse(events.get(1).showOnSidebar());
        assertEquals("Hi", events.get(1).title());
        assertEquals("there", events.get(1).subtitle());
        assertTrue(events.get(2).showOnSidebar());
        assertFalse(events.get(3).showOnSidebar());
        assertTrue(events.get(4).showOnSidebar());
    }

    @Test
    void optionalFieldsAreRead() throws IOException {
        var result = parse("""
                timeline:
                  spawner-tiers:
                    emerald:
                      2: 50
                  events:
                    - id: custom
                      time: 90s
                      type: spawner-tier
                      spawner: emerald
                      tier: 2
                      include-team-spawners: true
                      name: "<green>Emerald II"
                      announce: false
                      message: "@fork.custom.message"
                      title: "T"
                      subtitle: "S"
                """);
        assertEquals(List.of(), result.warnings());
        var event = result.definition().events().get(0);
        assertEquals(90L, event.timeSeconds());
        assertTrue(event.includeTeamSpawners());
        assertEquals("<green>Emerald II", event.name());
        assertEquals(Boolean.FALSE, event.announce());
        assertEquals("@fork.custom.message", event.message());
        assertEquals("T", event.title());
        assertEquals("S", event.subtitle());
    }

    @Test
    void invalidTierTableEntriesWarn() throws IOException {
        var result = parse("""
                timeline:
                  spawner-tiers:
                    diamond:
                      0: 10
                      2: soon
                      3: 5
                    emerald: nope
                  events:
                    - {time: "1:00", type: game-end}
                """);
        assertEquals(3, result.warnings().size());
        assertEquals(5 * 20L, result.definition().intervalTicks("diamond", 3));
        assertNull(result.definition().intervalTicks("diamond", 2));
        assertNull(result.definition().intervalTicks("emerald", 1));
    }

    @Test
    void spawnerTiersThatAreNotAMapWarn() throws IOException {
        var result = parse("""
                timeline:
                  spawner-tiers: [1, 2]
                  events:
                    - {time: "1:00", type: game-end}
                """);
        assertEquals(1, result.warnings().size());
        assertEquals(1, result.definition().events().size());
    }

    @Test
    void theParsedDefinitionIsImmutable() throws IOException {
        var definition = TimelineParser.parse(yaml(CPS_TIMELINE).node("timeline")).definition();
        var tiers = definition.spawnerTiers().get("diamond");
        assertThrows(UnsupportedOperationException.class, () -> definition.events().clear());
        assertThrows(UnsupportedOperationException.class, () -> definition.spawnerTiers().clear());
        assertThrows(UnsupportedOperationException.class, () -> tiers.put(9, 1L));
    }
}
