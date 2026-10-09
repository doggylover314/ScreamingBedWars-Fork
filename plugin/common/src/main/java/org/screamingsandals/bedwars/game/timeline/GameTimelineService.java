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

package org.screamingsandals.bedwars.game.timeline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.utils.annotations.Service;

/**
 * FOUNDATION-SKELETON: replaced by package P1 (PLAN.md).
 */
@Service
public class GameTimelineService {

    public static @NotNull GameTimelineService getInstance() {
        return ServiceManager.get(GameTimelineService.class);
    }

    /**
     * GameCycleImpl.prepareGame (successful start only).
     */
    public void startGame(@NotNull GameImpl game) {
    }

    /**
     * GameCycleImpl.runCycle, once per second while the game stays RUNNING.
     */
    public void tickRunning(@NotNull GameImpl game) {
    }

    public @Nullable GameTimeline getTimeline(@NotNull GameImpl game) {
        return null;
    }

    /**
     * -1 when the game is not running a timeline runtime.
     */
    public long getElapsedSeconds(@NotNull GameImpl game) {
        return -1;
    }

    public @NotNull Component renderSidebarTier(@NotNull GameImpl game, @Nullable CommandSender viewer) {
        return Component.empty();
    }

    public int getKills(@NotNull GameImpl game, @Nullable CommandSender viewer) {
        return 0;
    }

    public int getFinalKills(@NotNull GameImpl game, @Nullable CommandSender viewer) {
        return 0;
    }

    public int getTargetBlocksDestroyed(@NotNull GameImpl game, @Nullable CommandSender viewer) {
        return 0;
    }
}
