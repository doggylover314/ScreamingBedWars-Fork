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
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Parses the variant {@code timeline:} section (pure; Configurate only).
 * <p>
 * Never throws for bad configuration: invalid entries are skipped and described in {@link Result#warnings()}.
 */
public final class TimelineParser {
    private TimelineParser() {
    }

    public record Result(@NotNull TimelineDefinition definition, @NotNull List<String> warnings) {
    }

    private record EventEntry(int index, @Nullable String defaultId, @NotNull ConfigurationNode node) {
    }

    public static @NotNull Result parse(@NotNull ConfigurationNode timelineNode) {
        var warnings = new ArrayList<String>();
        if (timelineNode.virtual() || timelineNode.empty()) {
            return new Result(TimelineDefinition.EMPTY, List.of());
        }

        boolean enabled = timelineNode.node("enabled").getBoolean(true);
        var announcements = parseAnnouncements(timelineNode.node("announcements"), warnings);
        var tiers = parseTiers(timelineNode.node("spawner-tiers"), warnings);

        var entries = new ArrayList<EventEntry>();
        var eventsNode = timelineNode.node("events");
        if (eventsNode.isList()) {
            int i = 0;
            for (var child : eventsNode.childrenList()) {
                entries.add(new EventEntry(i++, null, child));
            }
        } else if (eventsNode.isMap()) {
            int i = 0;
            for (var entry : eventsNode.childrenMap().entrySet()) {
                entries.add(new EventEntry(i++, String.valueOf(entry.getKey()), entry.getValue()));
            }
        } else if (!eventsNode.virtual()) {
            warnings.add("events must be a list");
        }

        var ids = new HashSet<String>();
        var events = new ArrayList<TimelineEventDefinition>();
        for (var entry : entries) {
            var event = parseEvent(entry, tiers, ids, warnings);
            if (event != null) {
                events.add(event);
            }
        }
        events.sort(Comparator.comparingLong(TimelineEventDefinition::timeSeconds)); // List.sort is stable

        var frozenTiers = new LinkedHashMap<String, NavigableMap<Integer, Long>>();
        tiers.forEach((type, table) -> frozenTiers.put(type, Collections.unmodifiableNavigableMap(new TreeMap<>(table))));

        return new Result(
                new TimelineDefinition(enabled, announcements, Collections.unmodifiableMap(frozenTiers), List.copyOf(events)),
                List.copyOf(warnings)
        );
    }

    private static @NotNull AnnouncementSettings parseAnnouncements(@NotNull ConfigurationNode node, @NotNull List<String> warnings) {
        if (node.virtual()) {
            return AnnouncementSettings.DEFAULT;
        }
        boolean chat = node.node("chat").getBoolean(true);
        boolean title = node.node("title").getBoolean(true);
        var sound = parseSound(node.node("sound"), warnings);
        return new AnnouncementSettings(chat, title, sound != null ? sound : SoundSpec.DEFAULT_ANNOUNCEMENT);
    }

    private static @NotNull Map<String, NavigableMap<Integer, Long>> parseTiers(@NotNull ConfigurationNode tiersNode, @NotNull List<String> warnings) {
        var tiers = new LinkedHashMap<String, NavigableMap<Integer, Long>>();
        if (tiersNode.virtual()) {
            return tiers;
        }
        if (!tiersNode.isMap()) {
            warnings.add("spawner-tiers must be a map");
            return tiers;
        }
        for (var typeEntry : tiersNode.childrenMap().entrySet()) {
            var type = String.valueOf(typeEntry.getKey()).toLowerCase(Locale.ROOT);
            var typeNode = typeEntry.getValue();
            if (!typeNode.isMap()) {
                warnings.add("spawner-tiers." + type + " must be a map tier -> interval");
                continue;
            }
            for (var tierEntry : typeNode.childrenMap().entrySet()) {
                int tier = TimeParsing.parseTierKey(tierEntry.getKey());
                var ticks = TimeParsing.parseIntervalTicks(tierEntry.getValue().raw());
                if (tier < 1 || ticks.isEmpty()) {
                    warnings.add("invalid tier entry spawner-tiers." + type + "." + tierEntry.getKey());
                    continue;
                }
                tiers.computeIfAbsent(type, k -> new TreeMap<>()).put(tier, ticks.getAsLong());
            }
        }
        return tiers;
    }

    private static @Nullable TimelineEventDefinition parseEvent(
            @NotNull EventEntry entry,
            @NotNull Map<String, NavigableMap<Integer, Long>> tiers,
            @NotNull HashSet<String> ids,
            @NotNull List<String> warnings
    ) {
        int i = entry.index();
        var eNode = entry.node();

        var rawType = eNode.node("type").getString();
        var typeOpt = TimelineEventType.fromConfig(rawType);
        if (typeOpt.isEmpty()) {
            warnings.add("event #" + i + ": unknown type '" + rawType + "', skipped");
            return null;
        }
        var type = typeOpt.get();

        var timeOpt = TimeParsing.parseDurationSeconds(eNode.node("time").raw());
        if (timeOpt.isEmpty()) {
            warnings.add("event #" + i + ": missing/invalid time, skipped");
            return null;
        }

        var id = eNode.node("id").getString(entry.defaultId() != null ? entry.defaultId() : type.configName() + "-" + i);
        if (!ids.add(id)) {
            warnings.add("duplicate event id '" + id + "'");
            var unique = id + "-" + i;
            for (int n = 2; !ids.add(unique); n++) {
                unique = id + "-" + i + "-" + n;
            }
            id = unique;
        }

        String spawner = null;
        int tier = 0;
        Long override = null;
        boolean includeTeam = false;
        if (type == TimelineEventType.SPAWNER_TIER) {
            spawner = eNode.node("spawner").getString();
            if (spawner == null) {
                spawner = eNode.node("spawner-type").getString();
            }
            tier = TimeParsing.parseTierKey(eNode.node("tier").raw());
            if (spawner == null || spawner.isBlank() || tier < 1) {
                warnings.add("event '" + id + "': spawner-tier needs spawner and tier >= 1, skipped");
                return null;
            }
            spawner = spawner.trim().toLowerCase(Locale.ROOT);
            var intervalNode = eNode.node("interval");
            if (!intervalNode.virtual()) {
                var ov = TimeParsing.parseIntervalTicks(intervalNode.raw());
                if (ov.isEmpty()) {
                    warnings.add("event '" + id + "': invalid interval ignored");
                } else {
                    override = ov.getAsLong();
                }
            }
            var table = tiers.get(spawner);
            if (override == null && (table == null || table.get(tier) == null)) {
                warnings.add("event '" + id + "': no interval for " + spawner + " tier " + tier + "; only the displayed tier will change");
            }
            includeTeam = eNode.node("include-team-spawners").getBoolean(false);
        }

        var name = eNode.node("name").getString();
        boolean showOnSidebar = eNode.node("show-on-sidebar").getBoolean(type != TimelineEventType.ANNOUNCEMENT);
        var announceNode = eNode.node("announce");
        Boolean announce = announceNode.virtual() ? null : announceNode.getBoolean(true);
        var message = eNode.node("message").getString();
        var title = eNode.node("title").getString();
        var subtitle = eNode.node("subtitle").getString();
        var sound = parseSound(eNode.node("sound"), warnings);

        if (type == TimelineEventType.ANNOUNCEMENT && message == null && title == null && subtitle == null) {
            warnings.add("event '" + id + "': announcement needs message or title, skipped");
            return null;
        }

        return new TimelineEventDefinition(
                id, type, timeOpt.getAsLong(), spawner, tier, override, includeTeam,
                name, showOnSidebar, announce, message, title, subtitle, sound
        );
    }

    /**
     * @return null when the node is virtual or unusable (= inherit / default)
     */
    private static @Nullable SoundSpec parseSound(@NotNull ConfigurationNode node, @NotNull List<String> warnings) {
        if (node.virtual()) {
            return null;
        }
        var raw = node.raw();
        if (raw instanceof Boolean b) {
            return b ? SoundSpec.DEFAULT_ANNOUNCEMENT : SoundSpec.NONE;
        }
        if (node.isMap()) {
            var name = node.node("name").getString();
            if (name == null || name.isBlank()) {
                warnings.add("sound without name ignored");
                return null;
            }
            return new SoundSpec(
                    name.trim(),
                    node.node("source").getString("master").toLowerCase(Locale.ROOT),
                    node.node("volume").getFloat(1f),
                    node.node("pitch").getFloat(1f)
            );
        }
        var s = node.getString();
        return (s == null || s.isBlank()) ? null : new SoundSpec(s.trim(), "master", 1f, 1f);
    }
}
