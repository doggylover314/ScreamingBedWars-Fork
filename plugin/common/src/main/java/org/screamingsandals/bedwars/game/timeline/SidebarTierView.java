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

/**
 * What the sidebar {@code <tier>} placeholder shows (pure).
 */
public record SidebarTierView(@NotNull Kind kind, @Nullable TimelineEventDefinition event, long seconds) {
    public enum Kind {
        NEXT_EVENT,
        TIME_LIMIT,
        NONE,
        GAME_OVER
    }

    /**
     * @param state            runtime state, null when the game has no timeline runtime
     * @param running          whether the game is in the RUNNING status
     * @param countdownSeconds seconds left until the arena time limit ends the game; &lt;= 0 = no time limit known
     */
    public static @NotNull SidebarTierView resolve(@Nullable TimelineState state, boolean running, long countdownSeconds) {
        if (!running) {
            return new SidebarTierView(Kind.GAME_OVER, null, 0);
        }
        var next = state == null ? null : state.peekNextVisible();
        if (next != null) {
            long secs = Math.max(0, next.timeSeconds() - state.getElapsedSeconds());
            // the arena time limit ends the game before an event that would never happen
            if (countdownSeconds <= 0 || secs <= countdownSeconds) {
                return new SidebarTierView(Kind.NEXT_EVENT, next, secs);
            }
        }
        if (countdownSeconds > 0) {
            return new SidebarTierView(Kind.TIME_LIMIT, null, countdownSeconds);
        }
        return new SidebarTierView(Kind.NONE, null, 0);
    }
}
