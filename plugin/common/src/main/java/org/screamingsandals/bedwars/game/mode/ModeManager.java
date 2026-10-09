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

package org.screamingsandals.bedwars.game.mode;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.UUID;

/**
 * FOUNDATION-SKELETON: replaced by package P6 (PLAN.md).
 */
@Service
public class ModeManager {

    public static @NotNull ModeManager getInstance() {
        return ServiceManager.get(ModeManager.class);
    }

    public static boolean isModesEnabled() {
        return false;
    }

    /**
     * Largest party (or other group) led by {@code partyLeader} that can still join the WAITING mode lobby of the game;
     * -1 = no mode limit.
     */
    public static int largestJoinableGroup(@NotNull GameImpl game, @NotNull UUID partyLeader) {
        return -1;
    }
}
