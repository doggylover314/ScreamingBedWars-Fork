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

package org.screamingsandals.bedwars.game.upgrade.builtin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parsed form of an {@code effect} upgrade (for example Maniac Miner = Haste I/II). Pure value class.
 *
 * @param effect        lower-cased potion effect name
 * @param maxLevel      highest level of the upgrade
 * @param amplifiers    amplifier per level (index 0 = level 1); empty means amplifier = level - 1
 * @param durationTicks {@link #PERMANENT} or a positive number of ticks
 */
public record EffectUpgradeSpec(@NotNull String effect, int maxLevel, @NotNull List<Integer> amplifiers,
                                int durationTicks, boolean ambient, boolean particles, boolean icon) {
    public static final int PERMANENT = -1;
    /** Effects whose remaining duration is at least this long are considered "permanent". */
    public static final int PERMANENT_THRESHOLD_TICKS = 1_000_000;

    public static @NotNull EffectUpgradeSpec parse(@NotNull ConfigurationNode node, @NotNull List<String> warnings)
            throws ConfigurateException {
        var effect = node.node("effect").getString();
        if (effect == null || effect.isBlank()) {
            throw new ConfigurateException(node, "Missing 'effect' on effect upgrade");
        }
        var amps = new ArrayList<Integer>();
        for (Integer a : node.node("amplifiers").getList(Integer.class, List.of())) {
            if (a == null || a < 0) {
                warnings.add("negative amplifier replaced by 0");
                amps.add(0);
            } else {
                amps.add(a);
            }
        }
        int max = node.node("max-level").getInt(amps.isEmpty() ? 1 : amps.size());
        if (max < 1) {
            warnings.add("max-level must be >= 1");
            max = 1;
        }
        int duration = node.node("duration").getInt(PERMANENT);
        if (duration == 0 || duration < PERMANENT) {
            warnings.add("duration must be -1 (permanent) or > 0 ticks");
            duration = PERMANENT;
        }
        return new EffectUpgradeSpec(effect.trim().toLowerCase(Locale.ROOT), max, List.copyOf(amps), duration,
                node.node("ambient").getBoolean(true), node.node("particles").getBoolean(false), node.node("icon").getBoolean(true));
    }

    /**
     * Amplifier for the given level, or -1 when the level gives no effect.
     */
    public int amplifierFor(int level) {
        if (level <= 0) {
            return -1;
        }
        int l = Math.min(level, maxLevel);
        if (amplifiers.isEmpty()) {
            return l - 1;
        }
        return amplifiers.get(Math.min(l, amplifiers.size()) - 1);
    }

    public boolean permanent() {
        return durationTicks == PERMANENT;
    }

    public int appliedDuration() {
        return permanent() ? Integer.MAX_VALUE : durationTicks;
    }

    /**
     * Decides whether the effect on a player has to be (re)applied.
     *
     * @param currentAmplifier amplifier of the active effect, or {@code null} if the player does not have the effect
     * @param currentDuration  remaining ticks of the active effect, or {@code null} if absent; -1 is vanilla "infinite"
     */
    public static boolean needsReapply(@Nullable Integer currentAmplifier, @Nullable Integer currentDuration,
                                       int desiredAmplifier, boolean permanent, int refreshTicks) {
        if (currentAmplifier == null || currentDuration == null) {
            return true;
        }
        if (currentAmplifier != desiredAmplifier) {
            return true;
        }
        if (currentDuration == -1) {
            return false;
        }
        if (permanent) {
            return currentDuration < PERMANENT_THRESHOLD_TICKS;
        }
        return currentDuration <= refreshTicks + 20;
    }
}
