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
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.party.PartyJoinCoordinator;
import org.screamingsandals.bedwars.party.PartyManagerImpl;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.lib.plugin.ServiceManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The ONLY class of the modes area that talks to the party API (contract C3).
 */
final class ModePartyBridge {
    record PartyInfo(@NotNull UUID leader, @NotNull List<UUID> members, int onlineSize) {
    }

    private ModePartyBridge() {
    }

    /**
     * The party of the player; empty when parties are not loaded or the player is in none.
     */
    static @NotNull Optional<PartyInfo> partyOf(@NotNull UUID player) {
        var manager = ServiceManager.getOptional(PartyManagerImpl.class);
        if (manager.isEmpty()) {
            return Optional.empty();
        }
        return manager.get().getParty(player)
                .map(p -> new PartyInfo(p.getLeader(), List.copyOf(p.getMembers()), manager.get().getOnlineSize(p)));
    }

    /**
     * C3 / D32: whether a party member may join a mode on his own (party rule, bypass permission, leader offline).
     */
    static boolean mayJoinOnOwn(@NotNull BedWarsPlayer player) {
        return ServiceManager.getOptional(PartyJoinCoordinator.class)
                .map(c -> c.mayJoinOnOwn(player, null, false))
                .orElse(true);
    }

    /**
     * C3: the leader joins and pulls the online party (capacity check and messages are done by the coordinator).
     */
    static void joinWithParty(@NotNull BedWarsPlayer leader, @NotNull GameImpl game) {
        var coordinator = ServiceManager.getOptional(PartyJoinCoordinator.class);
        if (coordinator.isPresent()) {
            coordinator.get().joinWithParty(leader, game);
        } else {
            game.joinToGame(leader);
        }
    }

    /**
     * D50: seats a mode join reserves for this player (1 + members who would really be moved).
     */
    static int seatsFor(@NotNull BedWarsPlayer leader) {
        return ServiceManager.getOptional(PartyJoinCoordinator.class).map(c -> c.seatsFor(leader)).orElse(1);
    }
}
