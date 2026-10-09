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

package org.screamingsandals.bedwars.game.endgame;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.events.GameTickEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.utils.annotations.Service;

/**
 * FOUNDATION-SKELETON: replaced by package P2 (PLAN.md). Every method is a no-op until then.
 */
@Service
public class GameEndgameService {

    public static @NotNull GameEndgameService getInstance() {
        return ServiceManager.get(GameEndgameService.class);
    }

    /**
     * Timeline BED_DESTRUCTION event.
     */
    public void destroyAllTargets(@NotNull GameImpl game) {
    }

    /**
     * Timeline SUDDEN_DEATH event.
     */
    public void startSuddenDeath(@NotNull GameImpl game) {
    }

    /**
     * Timeline GAME_END event.
     */
    public void endGameByTime(@NotNull GameImpl game) {
        endGameByTime(game, null);
    }

    /**
     * Also called from GameCycleImpl.runCycle when the arena time limit runs out (tick != null).
     */
    public void endGameByTime(@NotNull GameImpl game, @Nullable GameTickEventImpl tick) {
    }
}
