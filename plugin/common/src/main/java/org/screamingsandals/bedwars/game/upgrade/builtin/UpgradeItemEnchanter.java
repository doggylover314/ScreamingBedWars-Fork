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
import org.screamingsandals.bedwars.api.game.upgrade.Upgrade;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.lib.item.ItemStack;
import org.screamingsandals.lib.item.meta.Enchantment;

/**
 * Applies the enchantment upgrades of a team to items. Shared by the shop (bought items), the upgrade handler
 * (level changed, respawn) and {@link UpgradeItemListener} (pickup, containers).
 */
public final class UpgradeItemEnchanter {
    private UpgradeItemEnchanter() {
    }

    /**
     * @return true when the team owns at least one enchantment upgrade level
     */
    public static boolean hasAnyEnchantUpgrade(@NotNull TeamImpl team) {
        var game = team.getGame();
        if (game == null) {
            return false;
        }
        for (var entry : game.getGameVariant().getUpgrades().entrySet()) {
            if (entry.getValue() instanceof EnchantmentUpgradeDefinition) {
                var upgrade = team.getUpgrade(entry.getKey());
                if (upgrade != null && levelOf(upgrade) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Enchants the stack with every enchantment upgrade of the team whose {@code apply-to} matches it.
     * Never downgrades an enchantment the item already has.
     *
     * @return {@code stack} itself when nothing changes, otherwise the enchanted copy
     */
    public static @NotNull ItemStack enchant(@NotNull TeamImpl team, @NotNull ItemStack stack) {
        var game = team.getGame();
        if (stack.isAir() || game == null) {
            return stack;
        }
        var result = stack;
        for (var entry : game.getGameVariant().getUpgrades().entrySet()) {
            if (!(entry.getValue() instanceof EnchantmentUpgradeDefinition)) {
                continue;
            }
            var definition = (EnchantmentUpgradeDefinition) entry.getValue();
            var upgrade = team.getUpgrade(entry.getKey());
            if (upgrade == null) {
                continue;
            }
            int level = levelOf(upgrade);
            if (level <= 0 || !result.is(definition.getApplyTo().toArray())) {
                continue;
            }
            int existing = result.getEnchantments().stream()
                    .filter(enchantment -> enchantment.type().equals(definition.getType()))
                    .mapToInt(Enchantment::level)
                    .max()
                    .orElse(0);
            if (existing >= level) {
                continue;
            }
            result = result.withEnchantment(definition.getType().asEnchantment(level)); // replaces the level of this enchantment
        }
        return result;
    }

    /**
     * Enchants every slot of the player inventory (storage, armor, offhand).
     *
     * @return the number of changed slots
     */
    public static int enchantInventory(@NotNull BedWarsPlayer player, @NotNull TeamImpl team) {
        if (!hasAnyEnchantUpgrade(team)) {
            return 0;
        }
        var inventory = player.getPlayerInventory();
        var contents = inventory.getContents();
        int changed = 0;
        for (int i = 0; i < contents.length; i++) {
            var item = contents[i];
            if (item == null || item.isAir()) {
                continue;
            }
            var enchanted = enchant(team, item);
            if (enchanted != item) {
                inventory.setItem(i, enchanted);
                changed++;
            }
        }
        return changed;
    }

    static int levelOf(@NotNull Upgrade upgrade) {
        return (int) Math.floor(upgrade.getLevel() - upgrade.getInitialLevel() + 1e-9);
    }
}
