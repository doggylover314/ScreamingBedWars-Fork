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

package org.screamingsandals.bedwars.events;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.mode.ModeDefinition;
import org.screamingsandals.lib.event.Event;

/**
 * Fired when an arena gets a game mode assigned or loses it. Named "Arena..." to avoid confusion with player game modes.
 */
@Data
public class ArenaModeChangedEventImpl implements Event {
    private final @NotNull GameImpl game;
    private final @Nullable ModeDefinition previousMode;
    /**
     * null = the mode was cleared
     */
    private final @Nullable ModeDefinition newMode;
}
