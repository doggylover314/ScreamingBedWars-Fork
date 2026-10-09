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

package org.screamingsandals.bedwars.party;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.api.config.ConfigurationKey;
import org.screamingsandals.bedwars.config.ConfigurationContainerImpl;

/**
 * Arena/variant-overridable keys of the built-in party system.
 */
public final class PartyConfigKeys {
    /**
     * Leader joining a waiting game pulls the online members (config.yml: party.autojoin-members).
     */
    public static final ConfigurationKey<Boolean> AUTOJOIN_MEMBERS = ConfigurationKey.of(Boolean.class, "party", "autojoin-members");
    /**
     * Non-leaders cannot join waiting games on their own (config.yml: party.require-leader-to-join).
     */
    public static final ConfigurationKey<Boolean> REQUIRE_LEADER_TO_JOIN = ConfigurationKey.of(Boolean.class, "party", "require-leader-to-join");

    private PartyConfigKeys() {
    }

    public static void register(@NotNull ConfigurationContainerImpl container) {
        container.register(AUTOJOIN_MEMBERS, "party", "autojoin-members");
        container.register(REQUIRE_LEADER_TO_JOIN, "party", "require-leader-to-join");
    }
}
