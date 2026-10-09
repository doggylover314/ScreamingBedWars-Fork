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
import org.screamingsandals.bedwars.events.TrapQueuedEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.upgrade.pricing.PurchaseCheck;
import org.screamingsandals.bedwars.game.upgrade.trap.TrapQueue;
import org.screamingsandals.bedwars.game.upgrade.trap.TrapQueuePurchase;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.lib.event.EventManager;

/**
 * Appends a trap to the team's trap queue ({@code type: trap-queue} entity).
 */
final class TrapQueueAction implements UpgradeAction {
    private final @NotNull GameImpl game;
    private final @NotNull TeamImpl team;
    private final @NotNull String trapId;
    private final boolean notifyTeam;

    TrapQueueAction(@NotNull GameImpl game, @NotNull TeamImpl team, @NotNull String trapId, boolean notifyTeam) {
        this.game = game;
        this.team = team;
        this.trapId = trapId;
        this.notifyTeam = notifyTeam;
    }

    @Override
    public @NotNull PurchaseCheck check() {
        var definition = game.getGameVariant().getTrapQueue();
        return TrapQueuePurchase.decide(definition.settings(), definition.enabled() && definition.trap(trapId) != null,
                team.getTrapQueue().snapshot(), trapId).check();
    }

    @Override
    public @Nullable AppliedUpgrade apply(@NotNull BedWarsPlayer buyer) {
        var definition = game.getGameVariant().getTrapQueue();
        var trap = definition.trap(trapId);
        if (trap == null) {
            return null;
        }

        var event = new TrapQueuedEventImpl(game, team, buyer, trapId);
        EventManager.fire(event);
        if (event.isCancelled()) {
            return null;
        }

        var settings = definition.settings();
        var queue = team.getTrapQueue();
        if (queue.tryEnqueue(trapId, settings.maxSize(), settings.allowDuplicates()) != TrapQueue.EnqueueResult.OK) {
            return null; // lost a race with a teammate; nothing was queued, so nothing is charged
        }
        return new AppliedUpgrade(AppliedUpgrade.Kind.TRAP, 0, 0, trap, queue.size(), settings.maxSize());
    }

    @Override
    public boolean notifyTeam() {
        return notifyTeam;
    }
}
