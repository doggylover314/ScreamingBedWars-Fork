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

import java.util.ArrayList;
import java.util.List;

/**
 * Elapsed-seconds counter plus the index of the next timeline event (pure, one per game run).
 * <p>
 * Single writer (the game-cycle thread); readers (sidebar) may be on any thread.
 */
public final class TimelineState {
    private final @NotNull List<TimelineEventDefinition> events;
    private volatile long elapsedSeconds = 0;
    private volatile int nextIndex = 0;

    public TimelineState(@NotNull List<TimelineEventDefinition> sortedEvents) {
        this.events = List.copyOf(sortedEvents);
    }

    /**
     * Advances the clock by one second.
     *
     * @return the events that became due, in order; every event is returned exactly once
     */
    public @NotNull List<TimelineEventDefinition> advance() {
        long now = elapsedSeconds + 1;
        elapsedSeconds = now;
        int i = nextIndex;
        List<TimelineEventDefinition> due = null;
        while (i < events.size() && events.get(i).timeSeconds() <= now) {
            if (due == null) {
                due = new ArrayList<>(2);
            }
            due.add(events.get(i++));
        }
        nextIndex = i;
        return due == null ? List.of() : due;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getFiredCount() {
        return nextIndex;
    }

    public boolean isFinished() {
        return nextIndex >= events.size();
    }

    public @NotNull List<TimelineEventDefinition> getEvents() {
        return events;
    }

    /**
     * @return the first not-yet-fired event that is shown on the sidebar, or null (safe from any thread)
     */
    public @Nullable TimelineEventDefinition peekNextVisible() {
        for (int i = nextIndex; i < events.size(); i++) {
            var event = events.get(i);
            if (event.showOnSidebar()) {
                return event;
            }
        }
        return null;
    }
}
