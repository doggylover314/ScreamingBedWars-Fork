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
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.utils.annotations.Service;

/**
 * FOUNDATION-SKELETON: replaced by package P5 (PLAN.md).
 */
@Service
public class PartyJoinCoordinator {

    public static @NotNull PartyJoinCoordinator getInstance() {
        return ServiceManager.get(PartyJoinCoordinator.class);
    }

    /**
     * Called at the start of GameImpl.joinToGame; {@code true} = continue the normal join.
     */
    public boolean handleJoinRequest(@NotNull BedWarsPlayer player, @NotNull GameImpl game) {
        return true;
    }

    /**
     * Moves the leader and his party members to the game; @return whether the player ended up in the game.
     */
    public boolean joinWithParty(@NotNull BedWarsPlayer player, @NotNull GameImpl game) {
        game.joinToGame(player);
        return player.getGame() == game;
    }

    /**
     * Whether the player may join a game without his party leader.
     */
    public boolean mayJoinOnOwn(@NotNull BedWarsPlayer player, @Nullable GameImpl game, boolean notify) {
        return true;
    }

    /**
     * Free slots the join of this player (and his party) needs in the game.
     */
    public int requiredSlots(@NotNull BedWarsPlayer player, @NotNull GameImpl game) {
        return player.getGame() == game ? 0 : 1;
    }

    /**
     * Seats a mode join reserves for this player (1 + members who would really be moved).
     */
    public int seatsFor(@NotNull BedWarsPlayer player) {
        return 1;
    }

    /**
     * Runs the action without the party join hook.
     */
    public void runBypassing(@NotNull Runnable action) {
        action.run();
    }

    /**
     * Moves a single player to the target game without the party join hook.
     */
    public static boolean moveToGame(@NotNull BedWarsPlayer player, @NotNull GameImpl target) {
        target.joinToGame(player);
        return player.getGame() == target;
    }
}
