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
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.upgrade.pricing.LadderResolver;
import org.screamingsandals.bedwars.game.upgrade.pricing.PriceLadder;
import org.screamingsandals.bedwars.game.upgrade.pricing.PriceSpec;
import org.screamingsandals.bedwars.game.upgrade.pricing.PurchaseCheck;
import org.screamingsandals.bedwars.game.upgrade.trap.TrapQueuePurchase;
import org.screamingsandals.simpleinventories.inventory.Price;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;
import java.util.Locale;

/**
 * Runtime adapter that maps a shop item and a team to an {@link UpgradeItemState}. It is the single source of the price
 * for both the item lore and the trade.
 */
public final class UpgradeItemStateResolver {
    private UpgradeItemStateResolver() {
    }

    /**
     * @param upgradeProperty the whole {@code properties:} map of the item (the one named {@code upgrade})
     * @param staticPrice     the fixed {@code price:} of the item, fallback and default currency of ladder entries
     * @param warnings        receives configuration problems (the caller decides whether to log them)
     */
    public static @NotNull UpgradeItemState resolve(@NotNull GameImpl game, @NotNull TeamImpl team,
                                                    @NotNull ConfigurationNode upgradeProperty, @Nullable PriceSpec staticPrice,
                                                    @NotNull List<String> warnings) {
        var entities = upgradeProperty.node("entities").childrenList();
        for (var entity : entities) { // 1) a trap entity wins
            if (!"trap-queue".equalsIgnoreCase(entity.node("type").getString(""))) {
                continue;
            }
            var trapId = entity.node("trap").getString("").trim().toLowerCase(Locale.ROOT);
            var definition = game.getGameVariant().getTrapQueue();
            var queued = team.getTrapQueue().snapshot();
            var decision = TrapQueuePurchase.decide(definition.settings(), definition.enabled() && definition.trap(trapId) != null, queued, trapId);
            return new UpgradeItemState(UpgradeItemState.Kind.TRAP, decision.check(), decision.price(), null, trapId,
                    queued.size(), definition.settings().maxSize());
        }

        for (var entity : entities) { // 2) the first team entity
            if (!"team".equalsIgnoreCase(entity.node("type").getString(""))) {
                continue;
            }
            var name = entity.node("upgrade-name").getString();
            var upgrade = name == null ? null : team.getUpgrade(name);
            if (upgrade == null) {
                return UpgradeItemState.unavailable();
            }
            var ladder = PriceLadder.parse(upgradeProperty, staticPrice == null ? null : staticPrice.currency(), warnings);
            var prices = ladder.forTeamSize(team.getMaxPlayers());
            var fallback = staticPrice != null ? staticPrice : (prices.isEmpty() ? null : prices.get(0));
            if (fallback == null) {
                return UpgradeItemState.unavailable();
            }
            var view = LadderResolver.resolve(upgrade.getLevel(), upgrade.getInitialLevel(), upgrade.getMaximalLevel(),
                    entity.node("levels").getDouble(1), prices, fallback);
            return new UpgradeItemState(UpgradeItemState.Kind.TEAM_LEVEL, view.maxed() ? PurchaseCheck.MAXED : PurchaseCheck.OK,
                    view.next(), view, null, 0, 0);
        }

        // 3) legacy storage entities only
        return new UpgradeItemState(UpgradeItemState.Kind.LEGACY, PurchaseCheck.OK, staticPrice, null, null, 0, 0);
    }

    /**
     * @return the first price of the item as a {@link PriceSpec}, {@code null} when the item has no priced currency
     */
    public static @Nullable PriceSpec staticPrice(@NotNull List<Price> prices) {
        if (prices.isEmpty() || prices.get(0).getCurrency() == null || prices.get(0).getCurrency().isBlank()) {
            return null;
        }
        return PriceSpec.of(Math.max(0, prices.get(0).getAmount()), prices.get(0).getCurrency());
    }
}
