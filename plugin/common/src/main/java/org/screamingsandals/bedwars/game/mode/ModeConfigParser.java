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

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pure parser of the {@code modes.list} config node.
 */
public final class ModeConfigParser {
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]+");

    private ModeConfigParser() {
    }

    public record Result(@NotNull List<ModeDefinition> modes, @NotNull List<String> warnings) {
    }

    public static @NotNull Result parse(@NotNull ConfigurationNode listNode) {
        var modes = new ArrayList<ModeDefinition>();
        var warnings = new ArrayList<String>();
        var seen = new HashSet<String>();
        if (listNode.virtual() || listNode.empty()) {
            return new Result(List.of(), List.of());
        }
        if (!listNode.isList()) {
            return new Result(List.of(), List.of("modes.list must be a list of mode definitions"));
        }
        var children = listNode.childrenList();
        for (int i = 0; i < children.size(); i++) {
            var node = children.get(i);
            var where = "modes.list[" + i + "]";
            if (!node.isMap()) {
                warnings.add(where + " is not a map - skipped");
                continue;
            }
            if (!node.node("enabled").getBoolean(true)) {
                continue;
            }
            var id = Objects.requireNonNullElse(node.node("id").getString(), "").trim().toLowerCase(Locale.ROOT);
            if (!ID.matcher(id).matches()) {
                warnings.add(where + ": invalid or missing id '" + id + "' (allowed a-z 0-9 _ -) - skipped");
                continue;
            }
            if (!seen.add(id)) {
                warnings.add(where + ": duplicate id '" + id + "' - skipped");
                continue;
            }
            int teamCount = node.node("team-count").getInt(0);
            int teamSize = node.node("team-size").getInt(0);
            if (teamCount < 2) {
                warnings.add(where + " (" + id + "): team-count must be >= 2 - skipped");
                continue;
            }
            if (teamSize < 1) {
                warnings.add(where + " (" + id + "): team-size must be >= 1 - skipped");
                continue;
            }
            var displayName = node.node("display-name").getString(id);
            int def = ModeRules.defaultMinPlayers(teamSize);
            int configuredMin = node.node("min-players").virtual() ? def : node.node("min-players").getInt(def);
            int min = ModeRules.clampMinPlayers(configuredMin, teamCount, teamSize);
            if (min != configuredMin) {
                warnings.add(where + " (" + id + "): min-players " + configuredMin + " clamped to " + min);
            }
            List<String> arenas;
            try {
                arenas = node.node("arenas").getList(String.class, List.of());
            } catch (SerializationException ex) {
                warnings.add(where + " (" + id + "): arenas must be a list of arena names - ignored");
                arenas = List.of();
            }
            modes.add(new ModeDefinition(id, displayName, teamCount, teamSize, min,
                    arenas.stream().filter(Objects::nonNull).collect(Collectors.toList())));
        }
        return new Result(List.copyOf(modes), List.copyOf(warnings));
    }
}
