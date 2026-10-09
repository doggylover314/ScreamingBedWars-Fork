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
import org.screamingsandals.bedwars.game.upgrade.pricing.PurchaseCheck;
import org.screamingsandals.bedwars.player.BedWarsPlayer;

/**
 * One effect of buying an upgrade shop item (an item can have several entities, hence several actions). The handler
 * first {@link #check() checks} every action, charges nothing until at least one action was
 * {@link #apply(BedWarsPlayer) applied}, and charges exactly once.
 */
interface UpgradeAction {
    /**
     * Validates the purchase without side effects.
     */
    @NotNull PurchaseCheck check();

    /**
     * Performs the change.
     *
     * @return what changed, or {@code null} when an event cancelled it and nothing was changed
     */
    @Nullable AppliedUpgrade apply(@NotNull BedWarsPlayer buyer);

    /**
     * @return true if every member of the team (not only the buyer) is told about the purchase
     */
    boolean notifyTeam();
}
