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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.api.game.upgrade.Upgradable;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.lib.debug.Debug;
import org.screamingsandals.lib.item.meta.PotionEffect;
import org.screamingsandals.lib.item.meta.PotionEffectType;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;

/**
 * Team upgrade that grants a potion effect to all alive members (Maniac Miner = Haste I/II).
 * Applied by {@link TeamEffectUpgradeHandler}.
 */
@Getter
@RequiredArgsConstructor
public class EffectUpgradeDefinition implements BuiltInUpgradeDefinition {
    private final @NotNull EffectUpgradeSpec spec;
    private final @NotNull PotionEffectType type;

    @Override
    public double getInitialLevel() {
        return 0;
    }

    @Override
    public @Nullable Double getMaximalLevel() {
        return (double) spec.maxLevel();
    }

    @Override
    public boolean isApplicable(@NotNull Upgradable upgradable) {
        return upgradable instanceof TeamImpl;
    }

    /**
     * @return the effect for this level, or {@code null} when the level gives no effect
     */
    public @Nullable PotionEffect effectForLevel(int level) {
        int amplifier = spec.amplifierFor(level);
        return amplifier < 0 ? null : type.asEffect(spec.appliedDuration(), amplifier, spec.ambient(), spec.particles(), spec.icon());
    }

    public static class Loader implements BuiltInUpgradeDefinition.Loader<EffectUpgradeDefinition> {
        public static final @NotNull Loader INSTANCE = new Loader();

        @Override
        public @NotNull EffectUpgradeDefinition load(@NotNull ConfigurationNode node) throws ConfigurateException {
            var warnings = new ArrayList<String>();
            var spec = EffectUpgradeSpec.parse(node, warnings);
            warnings.forEach(w -> Debug.warn("Effect upgrade " + node.key() + ": " + w, true));
            var type = PotionEffectType.ofNullable(spec.effect());
            if (type == null) {
                throw new ConfigurateException("Unknown potion effect " + spec.effect() + " (not available on this server version?)");
            }
            return new EffectUpgradeDefinition(spec, type);
        }
    }
}
