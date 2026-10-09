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
import org.screamingsandals.bedwars.api.config.ConfigurationKey;
import org.screamingsandals.bedwars.api.config.ConfigurationListKey;
import org.screamingsandals.bedwars.config.ConfigurationContainerImpl;

import java.util.List;

/**
 * Arena/variant-overridable keys of the game modes feature.
 */
public final class ModeConfigKeys {
    public static final List<Integer> DEFAULT_ALLOWED_TEAM_SIZES = List.of(1, 2, 3, 4);
    public static final ConfigurationListKey<Integer> ALLOWED_TEAM_SIZES = ConfigurationListKey.of(Integer.class, "modes", "allowed-team-sizes");
    public static final ConfigurationListKey<Integer> PREFERRED_TEAM_SIZES = ConfigurationListKey.of(Integer.class, "modes", "preferred-team-sizes");
    public static final ConfigurationListKey<String> TEAM_PRIORITY = ConfigurationListKey.of(String.class, "modes", "team-priority");
    public static final ConfigurationKey<Boolean> ONLY_VIA_MODE_SELECTION = ConfigurationKey.of(Boolean.class, "modes", "only-via-mode-selection");
    public static final ConfigurationKey<Boolean> DISABLE_TEAM_SELECTION = ConfigurationKey.of(Boolean.class, "modes", "disable-team-selection");
    public static final ConfigurationKey<Boolean> KEEP_PARTIES_TOGETHER = ConfigurationKey.of(Boolean.class, "modes", "keep-parties-together");

    private ModeConfigKeys() {
    }

    public static void register(@NotNull ConfigurationContainerImpl container) {
        container.registerGlobal(ALLOWED_TEAM_SIZES);
        container.registerGlobal(PREFERRED_TEAM_SIZES);
        container.registerGlobal(TEAM_PRIORITY);
        container.registerGlobal(ONLY_VIA_MODE_SELECTION);
        container.registerGlobal(DISABLE_TEAM_SELECTION);
        container.registerGlobal(KEEP_PARTIES_TOGETHER);
    }
}
