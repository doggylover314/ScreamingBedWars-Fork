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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Picks the prey of a dragon.
 */
public final class DragonTargeting {

    private DragonTargeting() {
    }

    public record Candidate(@NotNull UUID id, @NotNull Vec3 position) {
    }

    /**
     * Nearest candidate; keeps {@code current} while it is at most {@code stickiness} times farther than the nearest
     * one (avoids flip-flopping between two similarly distant players).
     *
     * @return the id of the chosen candidate, null when there is none
     */
    public static @Nullable UUID choose(@NotNull Vec3 dragon, @NotNull List<Candidate> candidates, @Nullable UUID current, double stickiness) {
        if (candidates.isEmpty()) {
            return null;
        }
        Candidate nearest = null;
        Candidate currentCandidate = null;
        double nearestDistance = Double.MAX_VALUE;
        double currentDistance = Double.MAX_VALUE;
        for (var candidate : candidates) {
            double distance = dragon.distanceSquared(candidate.position());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
            if (candidate.id().equals(current)) {
                currentCandidate = candidate;
                currentDistance = distance;
            }
        }
        if (currentCandidate != null && currentDistance <= nearestDistance * stickiness * stickiness) {
            return currentCandidate.id();
        }
        return nearest.id();
    }
}
