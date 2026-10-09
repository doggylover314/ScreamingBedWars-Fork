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

package org.screamingsandals.bedwars.inventories.upgrade;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.BedWarsPlugin;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.inventories.ShopInventory;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.simpleinventories.SimpleInventoriesCore;

import java.util.List;

/**
 * Re-renders the open shop inventories of a team so that teammates see the new level, price and queue state after a
 * purchase or a trap trigger.
 */
public final class UpgradeShopRefresher {
    private UpgradeShopRefresher() {
    }

    /**
     * Re-renders open shop inventories of the team's members (except {@code except}, which SimpleInventories re-renders itself).
     */
    public static void refreshTeam(@NotNull TeamImpl team, @Nullable BedWarsPlayer except) {
        if (!MainConfig.getInstance().node("upgrades", "refresh-open-shops").getBoolean(true)) {
            return;
        }
        var shops = ShopInventory.getInstance().getLoadedShops();
        for (var member : List.copyOf(team.getPlayers())) {
            if (member == except) {
                continue;
            }
            try {
                SimpleInventoriesCore.getInventoryRenderer(member).ifPresent(renderer -> {
                    if (renderer.isOpened() && shops.contains(renderer.getSubInventory().getInventorySet())) {
                        renderer.render();
                    }
                });
            } catch (Throwable throwable) {
                BedWarsPlugin.getInstance().getLogger().warn("Could not refresh the open shop of {}", member.getName(), throwable);
            }
        }
    }
}
