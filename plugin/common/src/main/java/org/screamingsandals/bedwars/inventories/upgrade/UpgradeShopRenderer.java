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
import org.screamingsandals.bedwars.game.upgrade.pricing.LadderView;
import org.screamingsandals.bedwars.game.upgrade.pricing.PriceSpec;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.bedwars.utils.RomanNumerals;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.simpleinventories.events.ItemRenderEvent;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Renders the lore of upgrade shop items for the viewing player: owned / next / locked tiers with the price for the
 * viewer's team, the trap queue state and a status line. Called from {@code ShopInventory#onGeneratingItem}.
 */
public final class UpgradeShopRenderer {
    private UpgradeShopRenderer() {
    }

    /**
     * @return true if the item was rendered here (the caller then skips the generic "Price:" lore)
     */
    public static boolean render(@NotNull ItemRenderEvent event) {
        var info = event.getItem();
        var slotProperty = info.getFirstPropertyByName("trap-queue-slot");
        var upgradeProperty = info.getFirstPropertyByName("upgrade");
        if (slotProperty.isEmpty() && upgradeProperty.isEmpty()) {
            return false; // fast path for normal items
        }

        var viewer = PlayerManagerImpl.getInstance().getPlayer(event.getPlayer().getUuid()).orElse(null);
        if (viewer == null || !viewer.isInGame()) {
            return false;
        }
        var game = viewer.getGame();
        var team = game.getPlayerTeam(viewer);
        if (team == null) {
            return false;
        }

        if (slotProperty.isPresent()) {
            renderQueueSlot(event, viewer, game, team, slotProperty.get().getPropertyData().node("slot").getInt(1));
            return true;
        }

        var data = upgradeProperty.get().getPropertyData();
        if (!data.node("dynamic-lore").getBoolean(true)) {
            return false;
        }
        var state = UpgradeItemStateResolver.resolve(game, team, data,
                UpgradeItemStateResolver.staticPrice(info.getOriginal().getPrices()), new ArrayList<>()); // warnings are logged on trade only
        if (state.kind() == UpgradeItemState.Kind.LEGACY) {
            return false; // keep the old lore for spawner-only items
        }

        var itemName = UpgradeShopHandler.itemName(data, info.getStack(), viewer);
        var lore = new ArrayList<>(info.getStack().getLore());
        lore.add(Component.empty());
        switch (state.kind()) {
            case TEAM_LEVEL:
                if (state.ladder() != null) {
                    appendLadder(lore, state.ladder(), tierNames(data), itemName, game, viewer);
                }
                break;
            case TRAP:
                lore.add(line(ForkLangKeys.IN_GAME_UPGRADES_LORE_TRAP_QUEUE, viewer,
                        m -> m.placeholder("queued", state.queued()).placeholder("max", state.queueMax())));
                if (state.price() != null) {
                    lore.add(line(ForkLangKeys.IN_GAME_UPGRADES_LORE_COST, viewer,
                            m -> m.placeholder("price", UpgradeShopHandler.priceComponent(state.price(), game))));
                }
                break;
            default:
                break; // UNAVAILABLE
        }
        lore.add(Component.empty());
        lore.add(statusLine(state, viewer, game));
        event.setStack(info.getStack().withItemLore(lore));
        return true;
    }

    private static void appendLadder(@NotNull List<Component> lore, @NotNull LadderView view, @NotNull List<String> tierNames,
                                     @NotNull Component itemName, @NotNull GameImpl game, @NotNull BedWarsPlayer viewer) {
        if (view.singleTier()) {
            if (!view.maxed() && view.next() != null) {
                lore.add(line(ForkLangKeys.IN_GAME_UPGRADES_LORE_COST, viewer,
                        m -> m.placeholder("price", UpgradeShopHandler.priceComponent(view.next(), game))));
            }
            return;
        }

        for (var row : view.rows()) {
            String[] key;
            switch (row.state()) {
                case OWNED:
                    key = ForkLangKeys.IN_GAME_UPGRADES_LORE_TIER_OWNED;
                    break;
                case NEXT:
                    key = ForkLangKeys.IN_GAME_UPGRADES_LORE_TIER_NEXT;
                    break;
                default:
                    key = ForkLangKeys.IN_GAME_UPGRADES_LORE_TIER_LOCKED;
                    break;
            }
            var roman = RomanNumerals.toRoman(row.tier());
            Component name = tierNames.size() >= row.tier()
                    ? Component.fromLegacy(tierNames.get(row.tier() - 1))
                    : itemName.withAppendix(Component.text(" " + roman));
            lore.add(line(key, viewer, m -> m
                    .placeholder("tier", roman)
                    .placeholder("name", name)
                    .placeholder("price", UpgradeShopHandler.priceComponent(row.price(), game))));
        }
    }

    private static @NotNull List<String> tierNames(@NotNull ConfigurationNode data) {
        try {
            return data.node("tier-names").getList(String.class, List.of());
        } catch (SerializationException e) {
            return List.of();
        }
    }

    private static @NotNull Component statusLine(@NotNull UpgradeItemState state, @NotNull BedWarsPlayer viewer, @NotNull GameImpl game) {
        switch (state.check()) {
            case MAXED:
                return line(ForkLangKeys.IN_GAME_UPGRADES_LORE_MAXED, viewer, m -> {
                });
            case QUEUE_FULL:
                return line(ForkLangKeys.IN_GAME_UPGRADES_LORE_TRAP_QUEUE_FULL, viewer, m -> {
                });
            case ALREADY_QUEUED:
                return line(ForkLangKeys.IN_GAME_UPGRADES_LORE_TRAP_ALREADY_QUEUED, viewer, m -> {
                });
            case OK:
                if (!canAfford(viewer, game, state.price())) {
                    var price = state.price();
                    Component currency = price == null ? Component.empty() : currencyName(price, game);
                    return line(ForkLangKeys.IN_GAME_UPGRADES_LORE_CANNOT_AFFORD, viewer, m -> m.placeholder("currency", currency));
                }
                return line(ForkLangKeys.IN_GAME_UPGRADES_LORE_CLICK_TO_BUY, viewer, m -> {
                });
            default:
                return line(ForkLangKeys.IN_GAME_UPGRADES_LORE_UNAVAILABLE, viewer, m -> {
                });
        }
    }

    private static @NotNull Component currencyName(@NotNull PriceSpec price, @NotNull GameImpl game) {
        var type = game.getGameVariant().getItemSpawnerType(price.currency());
        return type != null ? type.getItemName() : Component.text(price.currency());
    }

    private static boolean canAfford(@NotNull BedWarsPlayer viewer, @NotNull GameImpl game, @Nullable PriceSpec price) {
        if (price == null) {
            return false;
        }
        if (price.amount() <= 0) {
            return true;
        }
        var type = game.getGameVariant().getItemSpawnerType(price.currency());
        // the same test as OnTradeEvent#hasPlayerInInventory
        return type != null && viewer.getPlayerInventory().containsAtLeast(type.getItem(price.amount()), price.amount());
    }

    private static void renderQueueSlot(@NotNull ItemRenderEvent event, @NotNull BedWarsPlayer viewer, @NotNull GameImpl game,
                                        @NotNull TeamImpl team, int slot) {
        var definition = game.getGameVariant().getTrapQueue();
        if (!definition.enabled()) {
            return; // leave the configured item
        }

        var queued = team.getTrapQueue().snapshot();
        var trap = slot >= 1 && slot <= queued.size() ? definition.trap(queued.get(slot - 1)) : null;
        Component name;
        List<Component> lore;
        var base = event.getItem().getStack();
        if (trap != null) {
            base = Objects.requireNonNullElse(trap.icon(), base);
            name = line(ForkLangKeys.IN_GAME_UPGRADES_TRAP_QUEUE_SLOT_FILLED_NAME, viewer,
                    m -> m.placeholder("slot", slot).placeholder("trap", trap.displayName()));
            lore = lines(ForkLangKeys.IN_GAME_UPGRADES_TRAP_QUEUE_SLOT_FILLED_LORE, viewer, m -> {
            });
        } else {
            var price = definition.settings().priceFor(queued.size());
            Component priceComponent = price.isPresent()
                    ? UpgradeShopHandler.priceComponent(price.get(), game)
                    : Message.of(ForkLangKeys.IN_GAME_UPGRADES_LORE_TRAP_QUEUE_FULL).asComponent(viewer);
            name = line(ForkLangKeys.IN_GAME_UPGRADES_TRAP_QUEUE_SLOT_EMPTY_NAME, viewer, m -> m.placeholder("slot", slot));
            lore = lines(ForkLangKeys.IN_GAME_UPGRADES_TRAP_QUEUE_SLOT_EMPTY_LORE, viewer, m -> m.placeholder("price", priceComponent));
        }
        event.setStack(base.withDisplayName(name).withItemLore(lore));
    }

    /**
     * Lore defaults to italic in Minecraft; {@code withItalic(false)} keeps the colours readable.
     */
    private static @NotNull Component line(@NotNull String[] key, @NotNull BedWarsPlayer viewer, @NotNull Consumer<Message> configurer) {
        var message = Message.of(key);
        configurer.accept(message);
        return message.asComponent(viewer).withItalic(false);
    }

    private static @NotNull List<Component> lines(@NotNull String[] key, @NotNull BedWarsPlayer viewer, @NotNull Consumer<Message> configurer) {
        var message = Message.of(key);
        configurer.accept(message);
        var result = new ArrayList<Component>();
        for (var component : message.asComponentList(viewer)) {
            result.add(component.withItalic(false));
        }
        return result;
    }
}
