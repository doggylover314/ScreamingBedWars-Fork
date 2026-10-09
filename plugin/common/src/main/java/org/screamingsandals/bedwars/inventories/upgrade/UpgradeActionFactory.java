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
import org.screamingsandals.bedwars.api.upgrades.UpgradeRegistry;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.lib.utils.logger.Logger;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the {@link UpgradeAction}s of an upgrade shop item from its {@code entities:} list.
 */
final class UpgradeActionFactory {
    private UpgradeActionFactory() {
    }

    /**
     * @param data the whole {@code properties:} map of the item
     * @return the actions in configuration order; an empty list means the item is misconfigured
     */
    static @NotNull List<UpgradeAction> create(@NotNull GameImpl game, @NotNull TeamImpl team, @NotNull ConfigurationNode data,
                                               @NotNull Logger logger) {
        var actions = new ArrayList<UpgradeAction>();
        for (var entity : data.node("entities").childrenList()) {
            var type = entity.node("type").getString();
            if (type == null) {
                logger.warn("Upgrade configuration is invalid: entity without 'type'");
                return List.of();
            }

            switch (type.toLowerCase(Locale.ROOT)) {
                case "team": {
                    var name = entity.node("upgrade-name").getString();
                    if (name == null) {
                        logger.warn("Upgrade configuration is invalid, team upgrade name is missing!");
                        return List.of();
                    }
                    if (team.getUpgrade(name) == null) {
                        logger.warn("Upgrade configuration is invalid, team upgrade name {} is not registered!", name);
                    }
                    actions.add(new TeamLevelAction(game, team, name, entity.node("levels").getDouble(1),
                            entity.node("notify-team").getBoolean(false)));
                    break;
                }
                case "trap-queue": {
                    var trapId = entity.node("trap").getString("").trim().toLowerCase(Locale.ROOT);
                    actions.add(new TrapQueueAction(game, team, trapId, entity.node("notify-team").getBoolean(false)));
                    break;
                }
                default: {
                    var storage = UpgradeRegistry.getUpgrade(type);
                    if (storage == null) {
                        logger.warn("Unknown upgrade entity type {}", type);
                        break;
                    }
                    actions.add(new LegacyStorageAction(game, team, storage, entity, logger));
                    break;
                }
            }
        }
        return actions;
    }
}
