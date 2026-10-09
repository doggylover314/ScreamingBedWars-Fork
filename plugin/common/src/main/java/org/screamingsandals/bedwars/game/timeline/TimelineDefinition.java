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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Immutable result of parsing a variant {@code timeline:} section (pure).
 *
 * @param spawnerTiers unmodifiable: spawner type (lower-case) -&gt; tier -&gt; interval in ticks
 * @param events       unmodifiable, sorted by {@link TimelineEventDefinition#timeSeconds()} (stable)
 */
public record TimelineDefinition(
        boolean enabled,
        @NotNull AnnouncementSettings announcements,
        @NotNull Map<String, NavigableMap<Integer, Long>> spawnerTiers,
        @NotNull List<TimelineEventDefinition> events
) {
    public static final TimelineDefinition EMPTY = new TimelineDefinition(false, AnnouncementSettings.DEFAULT, Map.of(), List.of());

    public boolean isEmpty() {
        return !enabled || (events.isEmpty() && spawnerTiers.isEmpty());
    }

    /**
     * @return interval for (type, tier) from the table (type compared in lower case), or null if absent
     */
    public @Nullable Long intervalTicks(@NotNull String spawnerType, int tier) {
        var tiers = spawnerTiers.get(spawnerType.toLowerCase(Locale.ROOT));
        return tiers == null ? null : tiers.get(tier);
    }

    /**
     * @return the interval the event should apply: its own override first, then the table; null = only the displayed tier changes
     */
    public @Nullable Long effectiveIntervalTicks(@NotNull TimelineEventDefinition event) {
        if (event.intervalTicksOverride() != null) {
            return event.intervalTicksOverride();
        }
        if (event.spawnerType() == null) {
            return null;
        }
        return intervalTicks(event.spawnerType(), event.tier());
    }

    /**
     * Decides what the timeline itself announces for this event.
     */
    public @NotNull AnnouncementPlan planFor(@NotNull TimelineEventDefinition e) {
        if (Boolean.FALSE.equals(e.announce())) {
            return AnnouncementPlan.NOTHING;
        }
        if (e.type().isEndgameType()) {
            // the endgame area announces these itself; only explicitly configured extras are sent here
            boolean chat = e.message() != null;
            boolean title = e.title() != null || e.subtitle() != null;
            var sound = (e.sound() != null && !e.sound().isNone()) ? e.sound() : null;
            return new AnnouncementPlan(chat, title, sound);
        }
        // SPAWNER_TIER, ANNOUNCEMENT
        boolean forced = Boolean.TRUE.equals(e.announce());
        boolean tierEvent = e.type() == TimelineEventType.SPAWNER_TIER;
        boolean chat = e.message() != null || (announcements.chat() && (tierEvent || forced));
        boolean title = e.title() != null || e.subtitle() != null || (announcements.title() && (tierEvent || forced));
        var s = e.sound() != null ? e.sound() : announcements.sound();
        return new AnnouncementPlan(chat, title, s.isNone() ? null : s);
    }
}
