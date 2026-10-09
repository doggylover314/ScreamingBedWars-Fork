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
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.lib.event.EventExecutionOrder;
import org.screamingsandals.lib.event.OnEvent;
import org.screamingsandals.lib.event.player.PlayerInventoryClickEvent;
import org.screamingsandals.lib.event.player.PlayerInventoryCloseEvent;
import org.screamingsandals.lib.event.player.PlayerInventoryDragEvent;
import org.screamingsandals.lib.event.player.PlayerLeaveEvent;
import org.screamingsandals.lib.event.player.PlayerPickupItemEvent;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.methods.OnPreDisable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enchants items that enter a player's inventory outside the shop (picked up from the ground, moved from chests,
 * ender chest or team chest) with the team's enchantment upgrades.
 * <p>
 * Click and pickup events fire before the item is moved, so the sweep runs one tick later, at most once per player
 * and tick. Only players in a RUNNING game, not spectators, and only items matching {@code apply-to} are touched.
 */
@Service
public class UpgradeItemListener {
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    @OnEvent(order = EventExecutionOrder.MONITOR, ignoreCancelled = true)
    public void onPickup(@NotNull PlayerPickupItemEvent event) {
        schedule(event.player(), "on-pickup");
    }

    @OnEvent(order = EventExecutionOrder.MONITOR, ignoreCancelled = true)
    public void onClick(@NotNull PlayerInventoryClickEvent event) {
        schedule(event.player(), "on-inventory-change");
    }

    @OnEvent(order = EventExecutionOrder.MONITOR, ignoreCancelled = true)
    public void onDrag(@NotNull PlayerInventoryDragEvent event) {
        schedule(event.player(), "on-inventory-change");
    }

    @OnEvent(order = EventExecutionOrder.MONITOR)
    public void onClose(@NotNull PlayerInventoryCloseEvent event) {
        schedule(event.player(), "on-inventory-change");
    }

    private void schedule(@NotNull Player player, @NotNull String toggle) {
        if (!MainConfig.getInstance().node("upgrades", "enchant-items", toggle).getBoolean(true)) {
            return;
        }
        if (!PlayerManagerImpl.getInstance().isPlayerInGame(player)) {
            return;
        }
        var bw = player.as(BedWarsPlayer.class);
        if (!eligible(bw)) {
            return;
        }
        var uuid = bw.getUniqueId();
        if (!pending.add(uuid)) {
            return;
        }
        try {
            Tasker.runDelayed(bw, () -> {
                pending.remove(uuid);
                if (!eligible(bw)) {
                    return;
                }
                var team = bw.getGame().getPlayerTeam(bw);
                if (team != null) {
                    UpgradeItemEnchanter.enchantInventory(bw, team);
                }
            }, 1L, TaskerTime.TICKS);
        } catch (RuntimeException e) {
            // the entity scheduler is already retired, so the task body will never release the guard
            pending.remove(uuid);
            throw e;
        }
    }

    /**
     * The entity scheduler drops its pending tasks when the player disconnects, so the guard has to be released here.
     */
    @OnEvent
    public void onLeave(@NotNull PlayerLeaveEvent event) {
        pending.remove(event.player().getUniqueId());
    }

    private static boolean eligible(@NotNull BedWarsPlayer bw) {
        if (!bw.isInGame() || bw.isSpectator()) {
            return false;
        }
        var game = bw.getGame();
        if (game.getStatus() != GameStatus.RUNNING) {
            return false;
        }
        var team = game.getPlayerTeam(bw);
        return team != null && UpgradeItemEnchanter.hasAnyEnchantUpgrade(team);
    }

    @OnPreDisable
    public void onPreDisable() {
        pending.clear();
    }
}
