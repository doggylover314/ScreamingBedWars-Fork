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
import org.screamingsandals.bedwars.events.UpgradeLevelChangeEventImpl;
import org.screamingsandals.bedwars.events.UpgradeLevelChangedEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.upgrade.builtin.DragonBuffUpgradeDefinition;
import org.screamingsandals.bedwars.game.upgrade.pricing.LadderResolver;
import org.screamingsandals.bedwars.game.upgrade.pricing.PurchaseCheck;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.lib.event.EventManager;

/**
 * Raises the level of a team upgrade ({@code type: team} entity).
 */
final class TeamLevelAction implements UpgradeAction {
    private final @NotNull GameImpl game;
    private final @NotNull TeamImpl team;
    private final @NotNull String upgradeName;
    private final double levels;
    private final boolean notifyTeam;

    TeamLevelAction(@NotNull GameImpl game, @NotNull TeamImpl team, @NotNull String upgradeName, double levels, boolean notifyTeam) {
        this.game = game;
        this.team = team;
        this.upgradeName = upgradeName;
        this.levels = levels;
        this.notifyTeam = notifyTeam;
    }

    @Override
    public @NotNull PurchaseCheck check() {
        var upgrade = team.getUpgrade(upgradeName);
        if (upgrade == null) {
            return PurchaseCheck.UNAVAILABLE;
        }
        if (LadderResolver.isMaxed(upgrade.getLevel(), upgrade.getMaximalLevel(), levels)) {
            return PurchaseCheck.MAXED;
        }
        if (DragonBuffUpgradeDefinition.isLockedBySuddenDeath(game, upgradeName)) {
            return PurchaseCheck.UNAVAILABLE; // the dragons are already out, the buff would change nothing
        }
        return PurchaseCheck.OK;
    }

    @Override
    public @Nullable AppliedUpgrade apply(@NotNull BedWarsPlayer buyer) {
        var upgrade = team.getUpgrade(upgradeName);
        if (upgrade == null) {
            return null;
        }
        double oldLevel = upgrade.getLevel();
        upgrade.increaseLevel(levels);

        var change = new UpgradeLevelChangeEventImpl(game, team, upgradeName, upgrade, oldLevel, upgrade.getLevel());
        EventManager.fire(change);
        if (change.isCancelled()) {
            upgrade.setLevel(oldLevel);
            return null;
        }

        EventManager.fire(new UpgradeLevelChangedEventImpl(game, team, upgradeName, upgrade, oldLevel));
        return new AppliedUpgrade(AppliedUpgrade.Kind.TEAM_LEVEL, upgrade.getLevel(),
                LadderResolver.ownedTiers(upgrade.getLevel(), upgrade.getInitialLevel(), levels), null, 0, 0);
    }

    @Override
    public boolean notifyTeam() {
        return notifyTeam;
    }
}
