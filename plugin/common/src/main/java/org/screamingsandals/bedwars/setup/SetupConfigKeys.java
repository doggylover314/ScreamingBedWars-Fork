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

package org.screamingsandals.bedwars.setup;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.api.config.ConfigurationKey;
import org.screamingsandals.bedwars.api.config.ConfigurationListKey;
import org.screamingsandals.bedwars.config.ConfigurationContainerImpl;
import org.screamingsandals.bedwars.config.MainConfig;

import java.util.List;

/**
 * Arena/variant-overridable keys of the easy arena setup, and readers of its global-only keys.
 */
public final class SetupConfigKeys {
    public static final ConfigurationListKey<String> TEAM_GENERATOR_TYPES = ConfigurationListKey.of(String.class, "setup", "team-generator-types");
    public static final ConfigurationKey<Boolean> TEAM_GENERATOR_HOLOGRAM = ConfigurationKey.of(Boolean.class, "setup", "team-generator-hologram");
    public static final ConfigurationKey<String> DIAMOND_SPAWNER_TYPE = ConfigurationKey.of(String.class, "setup", "diamond-spawner-type");
    public static final ConfigurationKey<String> EMERALD_SPAWNER_TYPE = ConfigurationKey.of(String.class, "setup", "emerald-spawner-type");
    public static final ConfigurationKey<String> SHOP_FILE = ConfigurationKey.of(String.class, "setup", "shop-file");
    public static final ConfigurationKey<String> UPGRADE_SHOP_FILE = ConfigurationKey.of(String.class, "setup", "upgrade-shop-file");
    public static final List<String> DEFAULT_TEAM_GENERATOR_TYPES = List.of("iron", "gold");

    private SetupConfigKeys() {
    }

    public static void register(@NotNull ConfigurationContainerImpl c) {
        c.register(TEAM_GENERATOR_TYPES, "setup", "team-generator-types");
        c.register(TEAM_GENERATOR_HOLOGRAM, "setup", "team-generator-hologram");
        c.register(DIAMOND_SPAWNER_TYPE, "setup", "diamond-spawner-type");
        c.register(EMERALD_SPAWNER_TYPE, "setup", "emerald-spawner-type");
        c.register(SHOP_FILE, "setup", "shop-file");
        c.register(UPGRADE_SHOP_FILE, "setup", "upgrade-shop-file");
    }

    // global-only readers (method bodies only -> the class stays loadable in tests)
    public static int defaultTeamSize() {
        return Math.max(1, MainConfig.getInstance().node("setup", "default-team-size").getInt(4));
    }

    public static boolean snapToBlockCenter() {
        return MainConfig.getInstance().node("setup", "snap-to-block-center").getBoolean(true);
    }

    public static boolean showNextStepHint() {
        return MainConfig.getInstance().node("setup", "show-next-step-hint").getBoolean(true);
    }
}
