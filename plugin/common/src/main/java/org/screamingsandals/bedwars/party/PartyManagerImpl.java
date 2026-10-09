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
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FOUNDATION-SKELETON: replaced by package P5 (PLAN.md).
 */
@Service
public class PartyManagerImpl {

    public static @NotNull PartyManagerImpl getInstance() {
        return ServiceManager.get(PartyManagerImpl.class);
    }

    public boolean isEnabled() {
        return false;
    }

    public @NotNull Optional<PartyImpl> getParty(@NotNull UUID player) {
        return Optional.empty();
    }

    public int getOnlineSize(@NotNull PartyImpl party) {
        return party.size();
    }

    /**
     * Groups the players by party (parties keep the order of their first member); the skeleton returns singletons in order.
     */
    public @NotNull List<List<UUID>> groupByParty(@NotNull List<UUID> players) {
        var result = new ArrayList<List<UUID>>(players.size());
        for (var player : players) {
            result.add(List.of(player));
        }
        return result;
    }

    /**
     * Whether the command (with its slash, for example {@code /party}) is a party command that stays usable in a game.
     */
    public boolean isPartyCommandAllowedInGame(@Nullable String commandPref) {
        return false;
    }
}
