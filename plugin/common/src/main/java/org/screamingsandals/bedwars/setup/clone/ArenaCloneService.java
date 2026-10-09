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

package org.screamingsandals.bedwars.setup.clone;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.UUID;

/**
 * FOUNDATION-SKELETON: replaced by package P9 (PLAN.md).
 */
@Service
public class ArenaCloneService {

    public static @NotNull ArenaCloneService getInstance() {
        return ServiceManager.get(ArenaCloneService.class);
    }

    /**
     * Whether an arena name is reserved by a clone job in progress.
     */
    public boolean isNameReserved(@NotNull String name) {
        return false;
    }

    /**
     * Whether the arena is locked by a clone job in progress (source or target).
     */
    public boolean isLocked(@NotNull UUID gameUuid) {
        return false;
    }
}
