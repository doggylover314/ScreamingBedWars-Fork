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
import org.screamingsandals.bedwars.game.upgrade.trap.QueuedTrapDefinition;

/**
 * What one {@link UpgradeAction} changed; used to build the success message.
 *
 * @param kind     what was changed
 * @param newLevel the level after a team or legacy upgrade
 * @param tier     purchases made so far (team level upgrades)
 * @param trap     the queued trap (trap items)
 * @param queued   size of the trap queue after the purchase
 * @param queueMax capacity of the trap queue
 */
record AppliedUpgrade(@NotNull Kind kind, double newLevel, int tier, @Nullable QueuedTrapDefinition trap, int queued,
                      int queueMax) {
    enum Kind {
        TEAM_LEVEL, TRAP, LEGACY
    }
}
