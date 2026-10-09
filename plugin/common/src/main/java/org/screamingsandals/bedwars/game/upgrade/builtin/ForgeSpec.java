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
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parsed form of a {@code forge} upgrade (Iron / Gold / Emerald / Molten Forge). Pure value class.
 *
 * @param spawnerTypes            spawner type config keys boosted by the forge
 * @param autoDiscover            use the closest unlinked spawner when the team has none linked
 * @param autoDiscoverMaxDistance blocks; {@code <= 0} means unlimited
 * @param emeraldSpawnerType      spawner type dropped by emerald tiers
 * @param tiers                   index 0 = level 1
 */
public record ForgeSpec(@NotNull List<String> spawnerTypes, boolean autoDiscover, double autoDiscoverMaxDistance,
                        @NotNull String emeraldSpawnerType, @NotNull List<ForgeTier> tiers) {

    public record ForgeTier(double resourceBonus, int emeraldIntervalSeconds, int emeraldAmount) {
        public static final ForgeTier NONE = new ForgeTier(0, 0, 0);

        public boolean spawnsEmeralds() {
            return emeraldIntervalSeconds > 0 && emeraldAmount > 0;
        }
    }

    public int maxLevel() {
        return tiers.size();
    }

    public @NotNull ForgeTier tierFor(int level) {
        if (level <= 0 || tiers.isEmpty()) {
            return ForgeTier.NONE;
        }
        return tiers.get(Math.min(level, tiers.size()) - 1);
    }

    public static @NotNull ForgeSpec parse(@NotNull ConfigurationNode node, @NotNull List<String> warnings) throws ConfigurateException {
        var types = new ArrayList<String>();
        for (var t : node.node("spawner-types").getList(String.class, List.of())) {
            if (t != null && !t.isBlank()) {
                types.add(t.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (types.isEmpty()) {
            types.add("iron");
            types.add("gold");
        }
        boolean autoDiscover = node.node("auto-discover-spawners-if-not-linked").getBoolean(true);
        double maxDistance = node.node("auto-discover-max-distance").getDouble(25);
        var emeraldType = node.node("emerald-spawner-type").getString("emerald").trim().toLowerCase(Locale.ROOT);
        if (emeraldType.isEmpty()) {
            emeraldType = "emerald";
        }

        var tiersNode = node.node("tiers");
        if (!tiersNode.isList() || tiersNode.childrenList().isEmpty()) {
            throw new ConfigurateException(node, "forge upgrade needs a non-empty 'tiers' list");
        }
        var tiers = new ArrayList<ForgeTier>();
        var children = tiersNode.childrenList();
        for (int i = 0; i < children.size(); i++) {
            var t = children.get(i);
            if (!t.isMap()) {
                warnings.add("tiers[" + i + "] must be a map");
                tiers.add(ForgeTier.NONE);
                continue;
            }
            double bonus = t.node("resource-bonus").getDouble(0);
            if (bonus < 0) {
                warnings.add("tiers[" + i + "].resource-bonus must be >= 0");
                bonus = 0;
            }
            int interval = t.node("emerald-interval").getInt(0);
            if (interval < 0) {
                warnings.add("tiers[" + i + "].emerald-interval must be >= 0");
                interval = 0;
            }
            int amount = t.node("emerald-amount").getInt(1);
            if (amount < 1) {
                warnings.add("tiers[" + i + "].emerald-amount must be >= 1");
                amount = 1;
            }
            tiers.add(new ForgeTier(bonus, interval, amount));
        }
        return new ForgeSpec(List.copyOf(types), autoDiscover, maxDistance, emeraldType, List.copyOf(tiers));
    }
}
