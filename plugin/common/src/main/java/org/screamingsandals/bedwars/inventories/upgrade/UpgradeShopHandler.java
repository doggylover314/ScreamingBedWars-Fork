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
import org.screamingsandals.bedwars.api.PurchaseType;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.bedwars.events.PurchaseFailedEventImpl;
import org.screamingsandals.bedwars.events.StorePostPurchaseEventImpl;
import org.screamingsandals.bedwars.events.StorePrePurchaseEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.upgrade.pricing.PriceSpec;
import org.screamingsandals.bedwars.game.upgrade.pricing.PurchaseCheck;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.bedwars.utils.RomanNumerals;
import org.screamingsandals.lib.event.EventManager;
import org.screamingsandals.lib.item.ItemStack;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.spectator.sound.SoundSource;
import org.screamingsandals.lib.spectator.sound.SoundStart;
import org.screamingsandals.lib.utils.ResourceLocation;
import org.screamingsandals.simpleinventories.events.OnTradeEvent;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the purchase of an upgrade shop item (an item with the {@code upgrade} property). The flow is: build the
 * actions, resolve ONE price (the one shown in the lore), validate everything (nothing is charged on failure), fire the
 * pre-purchase event, apply the actions, charge exactly once, tell the team and refresh the open shops of the team.
 */
public final class UpgradeShopHandler {
    private UpgradeShopHandler() {
    }

    public static void handle(@NotNull OnTradeEvent event) {
        var mainConfig = MainConfig.getInstance();
        var logger = BedWarsPlugin.getInstance().getLogger();

        var buyer = PlayerManagerImpl.getInstance().getPlayer(event.getPlayer().getUuid()).orElse(null);
        if (buyer == null || !buyer.isInGame() || buyer.isSpectator()) {
            return;
        }
        var game = buyer.getGame();
        if (game.getStatus() != GameStatus.RUNNING) {
            return;
        }
        var team = game.getPlayerTeam(buyer);
        if (team == null) {
            return;
        }

        var data = event.getItem().getFirstPropertyByName("upgrade").orElseThrow().getPropertyData();
        var itemName = itemName(data, event.getStack(), buyer);
        var warnings = new ArrayList<String>();
        var state = UpgradeItemStateResolver.resolve(game, team, data, UpgradeItemStateResolver.staticPrice(event.getPrices()), warnings);
        var itemKey = event.getStack().getMaterial().location().asString();
        warnings.forEach(warning -> logger.warn("Upgrade item {}: {}", itemKey, warning));

        var actions = UpgradeActionFactory.create(game, team, data, logger);
        if (state.kind() == UpgradeItemState.Kind.UNAVAILABLE || actions.isEmpty()
                || (state.price() == null && state.check() == PurchaseCheck.OK)) {
            fail(PurchaseCheck.UNAVAILABLE, game, buyer, itemName, state, null);
            return;
        }
        if (state.check() != PurchaseCheck.OK) {
            fail(state.check(), game, buyer, itemName, state, null);
            return;
        }
        for (var action : actions) {
            var check = action.check();
            if (check != PurchaseCheck.OK) {
                fail(check, game, buyer, itemName, state, action);
                return;
            }
        }

        var price = state.price();
        if (price == null) { // cannot happen (checked above); keeps the nullness analysis honest
            fail(PurchaseCheck.UNAVAILABLE, game, buyer, itemName, state, null);
            return;
        }
        var type = game.getGameVariant().getItemSpawnerType(price.currency());
        if (type == null) {
            logger.warn("Upgrade item {} uses unknown currency {}", itemKey, price.currency());
            fail(PurchaseCheck.UNAVAILABLE, game, buyer, itemName, state, null);
            return;
        }

        var materialItem = type.getItem(Math.max(1, price.amount()));
        if (price.amount() > 0 && !event.hasPlayerInInventory(materialItem)) {
            var failed = new PurchaseFailedEventImpl(game, buyer, PurchaseType.UPGRADES, event);
            EventManager.fire(failed);
            if (failed.isCancelled()) {
                return;
            }
            if (!mainConfig.node("removePurchaseFailedMessages").getBoolean()) {
                Message.of(LangKeys.IN_GAME_SHOP_BUY_FAILED)
                        .prefixOrDefault(game.getCustomPrefixComponent())
                        .placeholder("item", itemName)
                        .placeholder("material", Component.text(price.amount() + " ").withAppendix(type.getItemName()))
                        .send(buyer);
            }
            return;
        }

        var prePurchase = new StorePrePurchaseEventImpl(game, buyer, materialItem, null, type, PurchaseType.UPGRADES, event);
        EventManager.fire(prePurchase);
        if (prePurchase.isCancelled()) {
            return;
        }

        var applied = new ArrayList<AppliedUpgrade>();
        boolean notifyTeam = data.node("notify-team").getBoolean(false);
        for (var action : actions) {
            var result = action.apply(buyer);
            if (result != null) {
                applied.add(result);
                notifyTeam |= action.notifyTeam();
            }
        }
        if (applied.isEmpty()) { // an event cancelled every action: nothing changed, nothing is charged
            fail(null, game, buyer, itemName, state, null);
            return;
        }

        if (price.amount() > 0) {
            event.sellStack(materialItem); // charged exactly once per purchase
        }

        List<BedWarsPlayer> receivers = notifyTeam ? List.copyOf(team.getPlayers()) : List.of(buyer);
        if (!mainConfig.node("removeUpgradeMessages").getBoolean()) {
            for (var result : applied) {
                successMessage(result, game, buyer, itemName, state).send(receivers);
            }
        }
        var sound = SoundStart.sound(
                ResourceLocation.of(mainConfig.node("sounds", "upgrade_buy", "sound").getString("entity.experience_orb.pickup")),
                SoundSource.PLAYER,
                (float) mainConfig.node("sounds", "upgrade_buy", "volume").getDouble(1),
                (float) mainConfig.node("sounds", "upgrade_buy", "pitch").getDouble(1)
        );
        receivers.forEach(receiver -> receiver.playSound(sound)); // once per recipient

        EventManager.fire(new StorePostPurchaseEventImpl(game, buyer, PurchaseType.UPGRADES, event));
        UpgradeShopRefresher.refreshTeam(team, buyer); // the buyer is re-rendered by SimpleInventories itself
    }

    /**
     * The name of the upgrade used in messages: the {@code shop-name} property, else the display name of the configured
     * item, else the translated word "upgrade".
     */
    public static @NotNull Component itemName(@NotNull ConfigurationNode data, @Nullable ItemStack configuredStack, @NotNull BedWarsPlayer viewer) {
        var shopName = data.node("shop-name").getString();
        if (shopName != null && !shopName.isBlank()) {
            return Component.fromLegacy(shopName);
        }
        if (configuredStack != null) {
            try {
                var displayName = configuredStack.getDisplayName();
                if (displayName != null) {
                    return displayName;
                }
            } catch (Throwable ignored) {
                // an item without meta: use the translated fallback
            }
        }
        return Message.of(LangKeys.IN_GAME_SHOP_UPGRADE_TRANSLATE).asComponent(viewer);
    }

    /**
     * "4 Diamond" (with the translated currency name) or, for an unknown currency, "4 currency".
     */
    public static @NotNull Component priceComponent(@NotNull PriceSpec price, @NotNull GameImpl game) {
        var type = game.getGameVariant().getItemSpawnerType(price.currency());
        if (type != null) {
            return Component.text(price.amount() + " ").withAppendix(type.getItemName());
        }
        return Component.text(price.amount() + " " + price.currency());
    }

    private static @NotNull Message successMessage(@NotNull AppliedUpgrade result, @NotNull GameImpl game, @NotNull BedWarsPlayer buyer,
                                                   @NotNull Component itemName, @NotNull UpgradeItemState state) {
        switch (result.kind()) {
            case TEAM_LEVEL: {
                boolean multi = state.ladder() != null && !state.ladder().singleTier();
                return Message.of(multi ? ForkLangKeys.IN_GAME_UPGRADES_PURCHASED_TIER : ForkLangKeys.IN_GAME_UPGRADES_PURCHASED)
                        .prefixOrDefault(game.getCustomPrefixComponent())
                        .placeholder("player", buyer.getDisplayName())
                        .placeholder("upgrade", itemName)
                        .placeholder("tier", RomanNumerals.toRoman(result.tier()));
            }
            case TRAP: {
                var trap = result.trap();
                return Message.of(ForkLangKeys.IN_GAME_UPGRADES_TRAP_PURCHASED)
                        .prefixOrDefault(game.getCustomPrefixComponent())
                        .placeholder("player", buyer.getDisplayName())
                        .placeholder("trap", trap != null ? trap.displayName() : Message.ofPlainText(String.valueOf(state.trapId())))
                        .placeholder("queued", result.queued())
                        .placeholder("max", result.queueMax());
            }
            default:
                return Message.of(LangKeys.IN_GAME_SHOP_UPGRADE_SUCCESS)
                        .prefixOrDefault(game.getCustomPrefixComponent())
                        .placeholder("name", buyer.getDisplayName())
                        .placeholder("spawner", itemName)
                        .placeholder("level", result.newLevel());
        }
    }

    /**
     * Tells the buyer why nothing happened. {@code check == null} means an event cancelled the purchase.
     */
    static void fail(@Nullable PurchaseCheck check, @NotNull GameImpl game, @NotNull BedWarsPlayer buyer, @NotNull Component itemName,
                     @NotNull UpgradeItemState state, @Nullable UpgradeAction action) {
        Message message;
        if (check == null) {
            message = Message.of(ForkLangKeys.IN_GAME_UPGRADES_CANCELLED).placeholder("upgrade", itemName);
        } else {
            switch (check) {
                case MAXED:
                    message = Message.of(ForkLangKeys.IN_GAME_UPGRADES_MAXED).placeholder("upgrade", itemName);
                    break;
                case LEGACY_MAX_LEVEL: {
                    var price = state.price();
                    Component material = price != null ? priceComponent(price, game) : Component.empty();
                    message = Message.of(LangKeys.IN_GAME_SPAWNER_REACHED_MAXIMUM_LEVEL)
                            .placeholder("item", itemName)
                            .placeholder("material", material)
                            .placeholder("max_level", action instanceof LegacyStorageAction ? ((LegacyStorageAction) action).maxLevel() : 0);
                    break;
                }
                case QUEUE_FULL:
                    message = Message.of(ForkLangKeys.IN_GAME_UPGRADES_TRAP_QUEUE_FULL).placeholder("max", state.queueMax());
                    break;
                case ALREADY_QUEUED: {
                    var trap = game.getGameVariant().getTrapQueue().trap(state.trapId());
                    message = Message.of(ForkLangKeys.IN_GAME_UPGRADES_TRAP_ALREADY_QUEUED)
                            .placeholder("trap", trap != null ? trap.displayName() : Message.ofPlainText(String.valueOf(state.trapId())));
                    break;
                }
                case NOTHING_TO_UPGRADE:
                    message = Message.of(ForkLangKeys.IN_GAME_UPGRADES_NOTHING_TO_UPGRADE).placeholder("upgrade", itemName);
                    break;
                case OK:
                case UNAVAILABLE:
                default:
                    message = Message.of(ForkLangKeys.IN_GAME_UPGRADES_UNAVAILABLE).placeholder("upgrade", itemName);
                    break;
            }
        }
        message.prefixOrDefault(game.getCustomPrefixComponent()).send(buyer);
    }
}
