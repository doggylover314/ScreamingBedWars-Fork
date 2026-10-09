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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimelineDefinitionTest {
    private static final SoundSpec CUSTOM_SOUND = new SoundSpec("entity.player.levelup", "master", 1f, 1f);

    private static TimelineEventDefinition event(TimelineEventType type, Boolean announce, String message, String title, String subtitle, SoundSpec sound) {
        return new TimelineEventDefinition(
                "e", type, 360, type == TimelineEventType.SPAWNER_TIER ? "diamond" : null,
                type == TimelineEventType.SPAWNER_TIER ? 2 : 0, null, false, null, true,
                announce, message, title, subtitle, sound
        );
    }

    private static TimelineDefinition definition(AnnouncementSettings settings) {
        return new TimelineDefinition(true, settings, Map.of(), List.of());
    }

    private static final TimelineDefinition DEFAULTS = definition(AnnouncementSettings.DEFAULT);

    @Test
    void spawnerTierAnnouncesWithDefaults() {
        var plan = DEFAULTS.planFor(event(TimelineEventType.SPAWNER_TIER, null, null, null, null, null));
        assertTrue(plan.chat());
        assertTrue(plan.title());
        assertEquals(SoundSpec.DEFAULT_ANNOUNCEMENT, plan.sound());
        assertFalse(plan.isEmpty());
    }

    @Test
    void announceFalseSilencesEverything() {
        var plan = DEFAULTS.planFor(event(TimelineEventType.SPAWNER_TIER, false, "text", "title", null, CUSTOM_SOUND));
        assertSame(AnnouncementPlan.NOTHING, plan);
        assertTrue(plan.isEmpty());
        assertSame(AnnouncementPlan.NOTHING, DEFAULTS.planFor(event(TimelineEventType.BED_DESTRUCTION, false, "text", null, null, null)));
    }

    @Test
    void eventSoundFalseRemovesOnlyTheSound() {
        var plan = DEFAULTS.planFor(event(TimelineEventType.SPAWNER_TIER, null, null, null, null, SoundSpec.NONE));
        assertTrue(plan.chat());
        assertTrue(plan.title());
        assertNull(plan.sound());
    }

    @Test
    void eventSoundOverridesTheDefaultSound() {
        var plan = DEFAULTS.planFor(event(TimelineEventType.SPAWNER_TIER, null, null, null, null, CUSTOM_SOUND));
        assertEquals(CUSTOM_SOUND, plan.sound());
    }

    @Test
    void settingsCanDisableChatAndTitle() {
        var plan = definition(new AnnouncementSettings(false, false, SoundSpec.DEFAULT_ANNOUNCEMENT))
                .planFor(event(TimelineEventType.SPAWNER_TIER, null, null, null, null, null));
        assertFalse(plan.chat());
        assertFalse(plan.title());
        assertEquals(SoundSpec.DEFAULT_ANNOUNCEMENT, plan.sound());
    }

    @Test
    void settingsSoundNoneMeansNoSound() {
        var plan = definition(new AnnouncementSettings(true, true, SoundSpec.NONE))
                .planFor(event(TimelineEventType.SPAWNER_TIER, null, null, null, null, null));
        assertTrue(plan.chat());
        assertNull(plan.sound());
    }

    @Test
    void customTextForcesTheMatchingChannelEvenWhenDisabledInSettings() {
        var plan = definition(new AnnouncementSettings(false, false, SoundSpec.NONE))
                .planFor(event(TimelineEventType.SPAWNER_TIER, null, "custom", null, "sub", null));
        assertTrue(plan.chat());
        assertTrue(plan.title());
        assertNull(plan.sound());
    }

    @Test
    void endgameEventsAreNotAnnouncedByDefault() {
        for (var type : new TimelineEventType[]{TimelineEventType.BED_DESTRUCTION, TimelineEventType.SUDDEN_DEATH, TimelineEventType.GAME_END}) {
            var plan = DEFAULTS.planFor(event(type, null, null, null, null, null));
            assertTrue(plan.isEmpty(), type + " must not be announced by the timeline");
        }
    }

    @Test
    void endgameEventWithMessageSendsChatOnly() {
        var plan = DEFAULTS.planFor(event(TimelineEventType.BED_DESTRUCTION, null, "Beds are gone", null, null, null));
        assertTrue(plan.chat());
        assertFalse(plan.title());
        assertNull(plan.sound());
    }

    @Test
    void endgameEventWithTitleAndSoundSendsBoth() {
        var plan = DEFAULTS.planFor(event(TimelineEventType.SUDDEN_DEATH, null, null, null, "subtitle only", CUSTOM_SOUND));
        assertFalse(plan.chat());
        assertTrue(plan.title());
        assertEquals(CUSTOM_SOUND, plan.sound());
    }

    @Test
    void announcementWithTitleOnlyDoesNotSendChat() {
        var plan = DEFAULTS.planFor(event(TimelineEventType.ANNOUNCEMENT, null, null, "Title", null, null));
        assertFalse(plan.chat());
        assertTrue(plan.title());
        assertEquals(SoundSpec.DEFAULT_ANNOUNCEMENT, plan.sound());
    }

    @Test
    void announcementWithMessageOnlyDoesNotSendTitle() {
        var plan = DEFAULTS.planFor(event(TimelineEventType.ANNOUNCEMENT, null, "Message", null, null, null));
        assertTrue(plan.chat());
        assertFalse(plan.title());
    }

    @Test
    void announceTrueForcesTheDefaultChannelsOfAnAnnouncement() {
        var plan = DEFAULTS.planFor(event(TimelineEventType.ANNOUNCEMENT, true, null, "Title", null, null));
        assertTrue(plan.chat());
        assertTrue(plan.title());
    }

    @Test
    void intervalLookupIsCaseInsensitiveAndNullWhenAbsent() {
        var diamond = new TreeMap<Integer, Long>();
        diamond.put(1, 600L);
        diamond.put(2, 460L);
        var definition = new TimelineDefinition(true, AnnouncementSettings.DEFAULT, Map.of("diamond", diamond), List.of());
        assertEquals(600L, definition.intervalTicks("diamond", 1));
        assertEquals(460L, definition.intervalTicks("DIAMOND", 2));
        assertNull(definition.intervalTicks("diamond", 3));
        assertNull(definition.intervalTicks("emerald", 1));
    }

    @Test
    void effectiveIntervalPrefersTheOverride() {
        var diamond = new TreeMap<Integer, Long>();
        diamond.put(2, 460L);
        var definition = new TimelineDefinition(true, AnnouncementSettings.DEFAULT, Map.of("diamond", diamond), List.of());

        var plain = event(TimelineEventType.SPAWNER_TIER, null, null, null, null, null);
        assertEquals(460L, definition.effectiveIntervalTicks(plain));

        var overridden = new TimelineEventDefinition("o", TimelineEventType.SPAWNER_TIER, 10, "diamond", 2, 200L, false,
                null, true, null, null, null, null, null);
        assertEquals(200L, definition.effectiveIntervalTicks(overridden));

        var missing = new TimelineEventDefinition("m", TimelineEventType.SPAWNER_TIER, 10, "emerald", 2, null, false,
                null, true, null, null, null, null, null);
        assertNull(definition.effectiveIntervalTicks(missing));

        assertNull(definition.effectiveIntervalTicks(event(TimelineEventType.GAME_END, null, null, null, null, null)));
    }

    @Test
    void emptinessRules() {
        assertTrue(TimelineDefinition.EMPTY.isEmpty());
        assertFalse(TimelineDefinition.EMPTY.enabled());
        assertTrue(new TimelineDefinition(true, AnnouncementSettings.DEFAULT, Map.of(), List.of()).isEmpty());
        var withEvent = new TimelineDefinition(true, AnnouncementSettings.DEFAULT, Map.of(),
                List.of(event(TimelineEventType.GAME_END, null, null, null, null, null)));
        assertFalse(withEvent.isEmpty());
        var disabled = new TimelineDefinition(false, AnnouncementSettings.DEFAULT, Map.of(), withEvent.events());
        assertTrue(disabled.isEmpty());
    }

    @Test
    void eventTypeNamesRoundTrip() {
        for (var type : TimelineEventType.values()) {
            assertEquals(type, TimelineEventType.fromConfig(type.configName()).orElseThrow());
        }
        assertEquals(TimelineEventType.SPAWNER_TIER, TimelineEventType.fromConfig("SPAWNER_TIER").orElseThrow());
        assertEquals(TimelineEventType.SUDDEN_DEATH, TimelineEventType.fromConfig(" sudden death ").orElseThrow());
        assertTrue(TimelineEventType.fromConfig("nope").isEmpty());
        assertTrue(TimelineEventType.fromConfig(null).isEmpty());
        assertTrue(TimelineEventType.BED_DESTRUCTION.isEndgameType());
        assertTrue(TimelineEventType.SUDDEN_DEATH.isEndgameType());
        assertTrue(TimelineEventType.GAME_END.isEndgameType());
        assertFalse(TimelineEventType.SPAWNER_TIER.isEndgameType());
        assertFalse(TimelineEventType.ANNOUNCEMENT.isEndgameType());
    }
}
