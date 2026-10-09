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

package org.screamingsandals.bedwars.game.upgrade.trap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.api.game.target.TargetBlock;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.lib.world.Location;

/**
 * Locates the "base" of a team. Used by the trap processor (detection centre) and the forge handler (closest generator).
 */
public final class TeamBase {
    private TeamBase() {
    }

    /**
     * The target block location; teams without a locatable target use the first team spawn.
     *
     * @return the centre of the team base, or {@code null} when the team has neither a target block nor a spawn
     */
    public static @Nullable Location center(@NotNull TeamImpl team) {
        var target = team.getTarget();
        if (target instanceof TargetBlock) {
            return ((TargetBlock) target).getTargetBlock().as(Location.class);
        }
        return team.getTeamSpawns().isEmpty() ? null : team.getTeamSpawns().get(0);
    }
}
