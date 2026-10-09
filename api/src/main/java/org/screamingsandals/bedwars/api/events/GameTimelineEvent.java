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

package org.screamingsandals.bedwars.api.events;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.api.BedwarsAPI;
import org.screamingsandals.bedwars.api.game.LocalGame;

import java.util.function.Consumer;

/**
 * Fired before an entry of the variant {@code timeline:} section takes effect (generator tier upgrade, bed destruction,
 * sudden death, game end by time or a custom announcement).
 * <p>
 * Fired synchronously on the game-cycle thread. Cancelling skips the effect and the announcement of this entry;
 * the entry is still consumed (it will not fire again in the same game).
 */
@ApiStatus.NonExtendable
public interface GameTimelineEvent extends BWCancellable {
    @NotNull LocalGame getGame();

    /**
     * @return id of the timeline entry (unique within the variant timeline)
     */
    @NotNull String getEventId();

    /**
     * @return {@code "spawner-tier"}, {@code "bed-destruction"}, {@code "sudden-death"}, {@code "game-end"} or {@code "announcement"}
     */
    @NotNull String getEventType();

    /**
     * @return scheduled time in seconds since the game started running
     */
    int getScheduledTime();

    /**
     * @return spawner type key for {@code "spawner-tier"}, otherwise null
     */
    @Nullable String getSpawnerType();

    /**
     * @return new tier for {@code "spawner-tier"}, otherwise 0
     */
    int getTier();

    static void handle(@NotNull Object plugin, @NotNull Consumer<@NotNull GameTimelineEvent> consumer) {
        BedwarsAPI.getInstance().getEventUtils().handle(plugin, GameTimelineEvent.class, consumer);
    }
}
