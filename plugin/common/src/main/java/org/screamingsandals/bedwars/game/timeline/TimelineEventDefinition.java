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
 * One parsed entry of the variant {@code timeline.events} list (pure, immutable).
 *
 * @param timeSeconds         elapsed RUNNING time at which the event fires (&gt;= 0)
 * @param spawnerType         lower-case spawner type config key; SPAWNER_TIER only
 * @param tier                &gt;= 1 for SPAWNER_TIER, 0 otherwise
 * @param intervalTicksOverride explicit interval of a SPAWNER_TIER event, in ticks
 * @param includeTeamSpawners SPAWNER_TIER only: also affect spawners linked to a team
 * @param name                raw MiniMessage text or {@code "@dotted.lang.key"}
 * @param announce            {@code null} = default
 * @param sound               {@code null} = inherit, {@link SoundSpec#NONE} = disabled
 */
public record TimelineEventDefinition(
        @NotNull String id,
        @NotNull TimelineEventType type,
        long timeSeconds,
        @Nullable String spawnerType,
        int tier,
        @Nullable Long intervalTicksOverride,
        boolean includeTeamSpawners,
        @Nullable String name,
        boolean showOnSidebar,
        @Nullable Boolean announce,
        @Nullable String message,
        @Nullable String title,
        @Nullable String subtitle,
        @Nullable SoundSpec sound
) {
}
