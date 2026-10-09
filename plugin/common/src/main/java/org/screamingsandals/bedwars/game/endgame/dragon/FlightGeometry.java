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

package org.screamingsandals.bedwars.game.endgame.dragon;

import org.jetbrains.annotations.NotNull;

/**
 * Derives the dragon flight box, cruise level and spawn ring from the arena bounds and the team spawns.
 */
public final class FlightGeometry {
    /**
     * Keep this many blocks from the pos1/pos2 floor and ceiling.
     */
    public static final double VERTICAL_MARGIN = 2.0;
    /**
     * Lowest flight level below the average team spawn Y.
     */
    public static final double BELOW_SPAWNS = 12.0;
    /**
     * Highest flight level above the cruise height.
     */
    public static final double ABOVE_CRUISE = 15.0;
    public static final double MAX_RING_RADIUS = 12.0;

    private FlightGeometry() {
    }

    public static @NotNull FlightBox flightBox(@NotNull FlightBox arena, double avgSpawnY, double boundsMargin, double cruiseHeight) {
        return arena.shrink(Math.max(0, boundsMargin), VERTICAL_MARGIN)
                .withYRange(avgSpawnY - BELOW_SPAWNS, avgSpawnY + Math.max(0, cruiseHeight) + ABOVE_CRUISE);
    }

    public static double cruiseY(@NotNull FlightBox flight, double avgSpawnY, double cruiseHeight) {
        return Math.max(flight.minY(), Math.min(flight.maxY(), avgSpawnY + cruiseHeight));
    }

    public static double ringRadius(@NotNull FlightBox flight) {
        return Math.max(0, Math.min(MAX_RING_RADIUS, Math.min(flight.width(), flight.depth()) / 4.0));
    }
}
