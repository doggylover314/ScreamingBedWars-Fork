/*
 * Copyright (C) 2025 ScreamingSandals
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
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.events.PlayerRespawnedEventImpl;
import org.screamingsandals.bedwars.events.UpgradeLevelChangedEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.lib.event.OnEvent;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.List;

@Service
public class EnchantmentUpgradeHandler {
    @OnEvent
    public void handle(@NotNull UpgradeLevelChangedEventImpl event) {
        if (!(event.getUpgradable() instanceof TeamImpl)) {
            return;
        }
        if (!(event.getGame().getGameVariant().getUpgrade(event.getName()) instanceof EnchantmentUpgradeDefinition)) {
            return;
        }
        if (event.getNewLevel() <= 0) {
            return;
        }

        var team = (TeamImpl) event.getUpgradable();
        var game = event.getGame();
        List.copyOf(team.getPlayers()).stream()
                .filter(player -> canEnchant(player, game, team))
                .forEach(player -> UpgradeItemEnchanter.enchantInventory(player, team));
    }

    @OnEvent
    public void handle(@NotNull PlayerRespawnedEventImpl event) {
        var team = event.getGame().getPlayerTeam(event.getPlayer());
        if (team == null || event.getGame().getGameVariant().getUpgrades().isEmpty()) {
            return;
        }

        var player = event.getPlayer();
        var game = event.getGame();
        Tasker.runDelayed(player, () -> {
            // the player may have left in the meantime and got the pre-game inventory back
            if (canEnchant(player, game, team)) {
                UpgradeItemEnchanter.enchantInventory(player, team);
            }
        }, 1L, TaskerTime.TICKS);
    }

    private static boolean canEnchant(@NotNull BedWarsPlayer player, @NotNull GameImpl game, @NotNull TeamImpl team) {
        return game.getStatus() == GameStatus.RUNNING && player.isInGame() && !player.isSpectator()
                && player.getGame() == game && game.getPlayerTeam(player) == team;
    }
}
