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
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.upgrade.EffectSpec;
import org.screamingsandals.lib.item.meta.PotionEffect;
import org.screamingsandals.lib.item.meta.PotionEffectType;
import org.screamingsandals.lib.spectator.sound.SoundStart;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Runtime form of the {@code trap-queue:} variant section (settings + resolved trap definitions).
 * Variants without the section use {@link #DISABLED}.
 */
public final class TrapQueueDefinition {
    public static final TrapQueueDefinition DISABLED = new TrapQueueDefinition(TrapQueueSettings.DISABLED, Map.of());

    private final @NotNull TrapQueueSettings settings;
    private final @NotNull Map<String, QueuedTrapDefinition> traps;

    public TrapQueueDefinition(@NotNull TrapQueueSettings settings, @NotNull Map<String, QueuedTrapDefinition> traps) {
        this.settings = settings;
        this.traps = traps;
    }

    public @NotNull TrapQueueSettings settings() {
        return settings;
    }

    /**
     * @return unmodifiable, insertion-ordered map of trap id to definition
     */
    public @NotNull Map<String, QueuedTrapDefinition> traps() {
        return traps;
    }

    public boolean enabled() {
        return settings.enabled() && !traps.isEmpty();
    }

    public @Nullable QueuedTrapDefinition trap(@Nullable String id) {
        return id == null ? null : traps.get(id.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Resolves effects and sounds of the parsed config. Unknown potion effects and invalid sounds are reported to
     * {@code warn} and skipped.
     *
     * @param section the original {@code trap-queue} node (used to read {@code trigger-sound})
     */
    public static @NotNull TrapQueueDefinition resolve(@NotNull TrapQueueConfig config, @NotNull ConfigurationNode section,
                                                       @NotNull Consumer<String> warn) {
        var map = new LinkedHashMap<String, QueuedTrapDefinition>();
        config.traps().forEach((id, spec) -> {
            SoundStart sound = null;
            var soundNode = section.node("traps", spec.sourceKey(), "trigger-sound");
            if (!soundNode.virtual()) {
                try {
                    sound = soundNode.get(SoundStart.class);
                } catch (SerializationException e) {
                    warn.accept("trap " + id + ": invalid trigger-sound (" + e.getMessage() + ")");
                }
            }
            map.put(id, new QueuedTrapDefinition(
                    spec,
                    resolveEffects(spec.enemyEffects(), id, warn),
                    resolveEffects(spec.teamEffects(), id, warn),
                    sound
            ));
        });
        return new TrapQueueDefinition(config.settings(), Collections.unmodifiableMap(map));
    }

    static @NotNull List<PotionEffect> resolveEffects(@NotNull List<EffectSpec> specs, @NotNull String trapId, @NotNull Consumer<String> warn) {
        var out = new ArrayList<PotionEffect>();
        for (var s : specs) {
            var type = PotionEffectType.ofNullable(s.effect());
            if (type == null) {
                warn.accept("trap " + trapId + ": unknown potion effect " + s.effect() + " (not available on this server version?)");
                continue;
            }
            out.add(type.asEffect(s.durationTicks(), s.amplifier(), s.ambient(), s.particles(), s.icon()));
        }
        return List.copyOf(out);
    }
}
