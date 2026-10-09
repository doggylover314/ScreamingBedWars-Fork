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

import java.util.Locale;
import java.util.Optional;

/**
 * What happens when the game time runs out ({@code game-end-by-time.mode}).
 */
public enum TimeEndMode {
    /**
     * Nobody wins.
     */
    DRAW,
    /**
     * The first tie-break criterion that leaves exactly one team decides the winner; a remaining tie is a draw.
     */
    TIE_BREAK;

    /**
     * "draw" gives {@link #DRAW}; "tie-break" / "tie_break" / "tiebreak" (any case, trimmed) give {@link #TIE_BREAK};
     * anything else (or null) gives an empty result.
     */
    public static @NotNull Optional<TimeEndMode> fromConfig(@Nullable String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "draw" -> Optional.of(DRAW);
            case "tie-break", "tie_break", "tiebreak" -> Optional.of(TIE_BREAK);
            default -> Optional.empty();
        };
    }
}
