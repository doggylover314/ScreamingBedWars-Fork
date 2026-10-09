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
import org.screamingsandals.bedwars.api.game.ItemSpawnerType;
import org.screamingsandals.bedwars.api.upgrades.Upgrade;
import org.screamingsandals.bedwars.api.upgrades.UpgradeStorage;
import org.screamingsandals.bedwars.events.UpgradeBoughtEventImpl;
import org.screamingsandals.bedwars.events.UpgradeImprovedEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.upgrade.pricing.PurchaseCheck;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.lib.event.EventManager;
import org.screamingsandals.lib.utils.logger.Logger;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy {@link UpgradeStorage} entity of an upgrade item (for example {@code type: spawner}): raises the level of the
 * spawners that match the entity configuration. The selection logic is the one the shop always had; unknown spawner
 * types are skipped with a warning, an item that matches nothing is not charged and cancellations are honoured.
 */
final class LegacyStorageAction implements UpgradeAction {
    private final @NotNull GameImpl game;
    private final @NotNull TeamImpl team;
    private final @NotNull UpgradeStorage storage;
    private final @NotNull Logger logger;
    private final boolean notifyTeam;
    private final double addLevels;
    private final double maxLevel;
    private boolean valid = true;
    private final @NotNull List<Upgrade> upgrades = new ArrayList<>();

    LegacyStorageAction(@NotNull GameImpl game, @NotNull TeamImpl team, @NotNull UpgradeStorage storage,
                        @NotNull ConfigurationNode entity, @NotNull Logger logger) {
        this.game = game;
        this.team = team;
        this.storage = storage;
        this.logger = logger;
        this.notifyTeam = entity.node("notify-team").getBoolean(false);
        this.addLevels = entity.node("add-levels").getDouble(entity.node("levels").getDouble(0));
        this.maxLevel = entity.node("max-level").getDouble();
        resolveUpgrades(entity);
    }

    private void resolveUpgrades(@NotNull ConfigurationNode entity) {
        var spawnerNameNode = entity.node("spawner-name");
        var spawnerTypeNode = entity.node("spawner-type");
        var teamUpgradeNode = entity.node("team-upgrade");
        var customNameNode = entity.node("customName");

        if (!spawnerNameNode.empty()) {
            addByInstanceName(spawnerNameNode.getString());
        } else if (!spawnerTypeNode.empty()) {
            var types = new ArrayList<ItemSpawnerType>();
            var names = new ArrayList<String>();
            if (spawnerTypeNode.isList()) {
                spawnerTypeNode.childrenList().forEach(child -> names.add(child.getString()));
            } else {
                names.add(spawnerTypeNode.getString());
            }

            for (var name : names) {
                var spawnerType = name == null ? null : game.getGameVariant().getItemSpawnerType(name);
                if (spawnerType == null) {
                    logger.warn("Upgrade item references unknown spawner type {}", name);
                    continue;
                }
                types.add(spawnerType);
                upgrades.addAll(storage.findItemSpawnerUpgrades(game, team, spawnerType));
            }

            if (upgrades.isEmpty() && entity.node("auto-discover-spawners-if-not-linked").getBoolean()) {
                discoverClosestSpawners(types);
            }
        } else if (!teamUpgradeNode.empty()) {
            if (teamUpgradeNode.getBoolean()) {
                upgrades.addAll(storage.findItemSpawnerUpgrades(game, team));
            }
        } else if (!customNameNode.empty()) { // old configuration
            addByInstanceName(customNameNode.getString());
        } else {
            valid = false;
            logger.warn("Spawner upgrade configuration is invalid.");
        }
    }

    private void addByInstanceName(@Nullable String instanceName) {
        if (instanceName == null) {
            valid = false;
            logger.warn("Spawner upgrade configuration is invalid, the spawner name is not a text.");
            return;
        }
        upgrades.addAll(storage.findItemSpawnerUpgrades(game, instanceName));
    }

    private void discoverClosestSpawners(@NotNull List<ItemSpawnerType> types) {
        if (team.getTeamSpawns().isEmpty()) {
            return;
        }
        for (var spawnerType : types) {
            double closestDistance = Double.MAX_VALUE;
            Upgrade closestSpawner = null;
            for (var spawner : game.getItemSpawners()) {
                if (spawner.getItemSpawnerType().toSpawnerType(game) == spawnerType) {
                    double distance = team.getRandomSpawn().getDistanceSquared(spawner.getLocation());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestSpawner = spawner;
                    }
                }
            }
            if (closestSpawner != null) {
                upgrades.add(closestSpawner);
            }
        }
    }

    /**
     * @return the configured {@code max-level}; {@code 0} means unlimited
     */
    double maxLevel() {
        return maxLevel;
    }

    @Override
    public @NotNull PurchaseCheck check() {
        if (!valid) {
            return PurchaseCheck.UNAVAILABLE;
        }
        if (upgrades.isEmpty()) {
            return PurchaseCheck.NOTHING_TO_UPGRADE;
        }
        for (var upgrade : upgrades) {
            if (upgrade.getLevel() + addLevels > maxLevel && maxLevel > 0) {
                return PurchaseCheck.LEGACY_MAX_LEVEL;
            }
        }
        return PurchaseCheck.OK;
    }

    @Override
    public @Nullable AppliedUpgrade apply(@NotNull BedWarsPlayer buyer) {
        var bought = new UpgradeBoughtEventImpl(game, buyer, upgrades, addLevels, storage);
        EventManager.fire(bought);
        if (bought.isCancelled()) {
            return null;
        }

        int applied = 0;
        double newLevel = Double.NEGATIVE_INFINITY;
        for (var upgrade : upgrades) {
            double oldLevel = upgrade.getLevel();
            var improved = new UpgradeImprovedEventImpl(game, upgrade, storage, oldLevel, oldLevel + addLevels);
            improved.setNewLevel(oldLevel + addLevels);
            EventManager.fire(improved);
            if (improved.isCancelled()) {
                upgrade.setLevel(oldLevel);
                continue;
            }
            applied++;
            newLevel = Math.max(newLevel, upgrade.getLevel());
        }
        if (applied == 0) {
            return null;
        }
        return new AppliedUpgrade(AppliedUpgrade.Kind.LEGACY, newLevel, 0, null, 0, 0);
    }

    @Override
    public boolean notifyTeam() {
        return notifyTeam;
    }
}
