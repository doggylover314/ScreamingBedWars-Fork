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

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Platform independent description of a potion effect read from a variant file. It is resolved to a slib
 * {@code PotionEffect} by the runtime classes (which know whether the effect exists on this server version).
 * Pure value class.
 *
 * @param effect        lower-cased effect name (e.g. {@code minecraft:blindness})
 * @param durationTicks duration in ticks, always greater than 0
 * @param amplifier     amplifier (0 = level I)
 */
public record EffectSpec(@NotNull String effect, int durationTicks, int amplifier, boolean ambient, boolean particles, boolean icon) {

    /**
     * Parses the map form {@code {effect, duration, amplifier, ambient, particles|has-particles, icon|has-icon}}.
     * The slib string form ({@code "effect N"} where N means ticks) is rejected on purpose.
     */
    public static @NotNull Optional<EffectSpec> parse(@NotNull ConfigurationNode node, @NotNull String context, @NotNull List<String> warnings) {
        if (!node.isMap()) {
            warnings.add(context + ": potion effects must use the map form {effect, duration, amplifier}");
            return Optional.empty();
        }
        var effect = node.node("effect").getString();
        if (effect == null || effect.isBlank()) {
            warnings.add(context + ": missing 'effect'");
            return Optional.empty();
        }
        int duration = node.node("duration").getInt(0);
        if (duration <= 0) {
            warnings.add(context + ": duration (ticks) must be > 0");
            return Optional.empty();
        }
        int amplifier = node.node("amplifier").getInt(0);
        if (amplifier < 0) {
            warnings.add(context + ": amplifier must be >= 0");
            amplifier = 0;
        }
        boolean ambient = node.node("ambient").getBoolean(false);
        boolean particles = node.node("particles").getBoolean(node.node("has-particles").getBoolean(true));
        boolean icon = node.node("icon").getBoolean(node.node("has-icon").getBoolean(true));
        return Optional.of(new EffectSpec(effect.trim().toLowerCase(Locale.ROOT), duration, amplifier, ambient, particles, icon));
    }

    /**
     * Parses a list of effects. A virtual node gives an empty list; a node that is not a list gives an empty list and a warning.
     */
    public static @NotNull List<EffectSpec> parseList(@NotNull ConfigurationNode listNode, @NotNull String context, @NotNull List<String> warnings) {
        if (listNode.virtual()) {
            return List.of();
        }
        if (!listNode.isList()) {
            warnings.add(context + ": must be a list of potion effects");
            return List.of();
        }
        var result = new ArrayList<EffectSpec>();
        var children = listNode.childrenList();
        for (int i = 0; i < children.size(); i++) {
            parse(children.get(i), context + "[" + i + "]", warnings).ifPresent(result::add);
        }
        return List.copyOf(result);
    }
}
