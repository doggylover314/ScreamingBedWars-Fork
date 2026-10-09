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

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.game.upgrade.EffectSpec;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parses the {@code trap-queue:} variant section. Pure: it reads only the configuration node.
 */
public final class TrapQueueConfigParser {
    private static final Pattern ID = Pattern.compile("[a-z0-9_\\-]+");

    private TrapQueueConfigParser() {
    }

    public record Result(@NotNull TrapQueueConfig config, @NotNull List<String> warnings) {
    }

    public static @NotNull Result parse(@NotNull ConfigurationNode section) {
        var warnings = new ArrayList<String>();
        if (section.virtual()) {
            return new Result(TrapQueueConfig.DISABLED, List.of());
        }

        boolean enabled = section.node("enabled").getBoolean(true);

        int maxSize = section.node("max-size").getInt(3);
        if (maxSize < 1) {
            warnings.add("max-size must be >= 1");
            maxSize = 1;
        }

        var currency = section.node("currency").getString("diamond").trim().toLowerCase(Locale.ROOT);
        if (currency.isBlank()) {
            warnings.add("currency must not be blank");
            currency = "diamond";
        }

        var costs = new ArrayList<Integer>();
        var costsNode = section.node("costs");
        if (costsNode.virtual()) {
            costs.addAll(List.of(1, 2, 4));
        } else {
            for (var child : costsNode.isList() ? costsNode.childrenList() : List.of(costsNode)) {
                int value = child.getInt(-1);
                if (value < 0) {
                    warnings.add("invalid cost '" + child.raw() + "'");
                    continue;
                }
                costs.add(value);
            }
            if (costs.isEmpty()) {
                warnings.add("costs is empty, using [1]");
                costs.add(1);
            }
        }

        long cooldown = section.node("cooldown-seconds").getLong(15);
        if (cooldown < 0) {
            warnings.add("cooldown-seconds must be >= 0");
            cooldown = 0;
        }
        double range = section.node("detection-range").getDouble(7);
        if (range <= 0) {
            warnings.add("detection-range must be > 0");
            range = 7;
        }
        boolean allowDuplicates = section.node("allow-duplicates").getBoolean(true);

        var traps = new LinkedHashMap<String, QueuedTrapSpec>();
        var trapsNode = section.node("traps");
        if (!trapsNode.isMap() || trapsNode.childrenMap().isEmpty()) {
            warnings.add("no traps defined - trap queue disabled");
        } else {
            for (var entry : trapsNode.childrenMap().entrySet()) {
                var key = String.valueOf(entry.getKey());
                var t = entry.getValue();
                var id = key.trim().toLowerCase(Locale.ROOT);
                if (!ID.matcher(id).matches()) {
                    warnings.add("invalid trap id '" + key + "' (allowed: a-z, 0-9, _ and -)");
                    continue;
                }
                if (traps.containsKey(id)) {
                    warnings.add("duplicate trap id '" + id + "'");
                    continue;
                }
                double trapRange = t.node("detection-range").getDouble(range);
                if (trapRange <= 0) {
                    warnings.add("trap " + id + ": detection-range must be > 0");
                    trapRange = range;
                }
                var enemy = EffectSpec.parseList(t.node("enemy-effects"), "trap " + id + " enemy-effects", warnings);
                var team = EffectSpec.parseList(t.node("team-effects"), "trap " + id + " team-effects", warnings);
                double teamRange = t.node("team-effect-range").getDouble(0);
                if (teamRange < 0) {
                    warnings.add("trap " + id + ": team-effect-range must be >= 0");
                    teamRange = 0;
                }
                boolean reveal = t.node("reveal-invisible").getBoolean(false);
                if (enemy.isEmpty() && team.isEmpty() && !reveal) {
                    warnings.add("trap " + id + " has no effect besides alerting the team");
                }
                traps.put(id, new QueuedTrapSpec(id, key, t.node("name").getString(id), t.node("icon").getString(),
                        trapRange, t.node("affect-all-intruders").getBoolean(false), List.copyOf(enemy), List.copyOf(team),
                        teamRange, reveal, t.node("message-intruder").getBoolean(true)));
            }
        }
        if (traps.isEmpty()) {
            enabled = false;
        }
        return new Result(
                new TrapQueueConfig(
                        new TrapQueueSettings(enabled, maxSize, currency, List.copyOf(costs), cooldown, range, allowDuplicates),
                        Collections.unmodifiableMap(traps)
                ),
                List.copyOf(warnings)
        );
    }
}
