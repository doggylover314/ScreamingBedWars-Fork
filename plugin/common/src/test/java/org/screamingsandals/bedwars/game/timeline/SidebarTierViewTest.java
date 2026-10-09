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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SidebarTierViewTest {
    private static TimelineEventDefinition event(String id, long time, boolean showOnSidebar) {
        return new TimelineEventDefinition(id, TimelineEventType.GAME_END, time, null, 0, null, false,
                null, showOnSidebar, null, null, null, null, null);
    }

    private static TimelineState advanced(int seconds, TimelineEventDefinition... events) {
        var state = new TimelineState(List.of(events));
        for (int i = 0; i < seconds; i++) {
            state.advance();
        }
        return state;
    }

    @Test
    void showsTheNextEventWithItsCountdown() {
        var next = event("diamond-2", 360, true);
        var view = SidebarTierView.resolve(advanced(28, next), true, 3572);
        assertEquals(SidebarTierView.Kind.NEXT_EVENT, view.kind());
        assertSame(next, view.event());
        assertEquals(332, view.seconds());
    }

    @Test
    void showsTheTimeLimitWhenItEndsTheGameBeforeTheNextEvent() {
        var view = SidebarTierView.resolve(advanced(1700, event("game-end", 3000, true)), true, 100);
        assertEquals(SidebarTierView.Kind.TIME_LIMIT, view.kind());
        assertNull(view.event());
        assertEquals(100, view.seconds());
    }

    @Test
    void showsTheTimeLimitWithoutATimeline() {
        var view = SidebarTierView.resolve(null, true, 50);
        assertEquals(SidebarTierView.Kind.TIME_LIMIT, view.kind());
        assertEquals(50, view.seconds());
    }

    @Test
    void showsNothingWhenThereIsNothingToShow() {
        var view = SidebarTierView.resolve(advanced(0), true, 0);
        assertEquals(SidebarTierView.Kind.NONE, view.kind());
        assertEquals(0, view.seconds());
        assertEquals(SidebarTierView.Kind.NONE, SidebarTierView.resolve(null, true, 0).kind());
    }

    @Test
    void showsGameOverWhenNotRunning() {
        var view = SidebarTierView.resolve(advanced(0, event("e", 10, true)), false, 100);
        assertEquals(SidebarTierView.Kind.GAME_OVER, view.kind());
        assertEquals(SidebarTierView.Kind.GAME_OVER, SidebarTierView.resolve(null, false, 0).kind());
    }

    @Test
    void countsDownToTheLastSecond() {
        var view = SidebarTierView.resolve(advanced(4, event("e", 5, true)), true, 1000);
        assertEquals(SidebarTierView.Kind.NEXT_EVENT, view.kind());
        assertEquals(1, view.seconds());
    }

    @Test
    void anUnknownTimeLimitNeverHidesAPendingEvent() {
        var view = SidebarTierView.resolve(advanced(0, event("e", 600, true)), true, 0);
        assertEquals(SidebarTierView.Kind.NEXT_EVENT, view.kind());
        assertEquals(600, view.seconds());
    }

    @Test
    void hiddenEventsAreIgnored() {
        var view = SidebarTierView.resolve(advanced(0, event("hidden", 10, false)), true, 30);
        assertEquals(SidebarTierView.Kind.TIME_LIMIT, view.kind());
        assertEquals(30, view.seconds());
    }

    @Test
    void anEventExactlyAtTheTimeLimitIsStillShown() {
        var view = SidebarTierView.resolve(advanced(0, event("e", 100, true)), true, 100);
        assertEquals(SidebarTierView.Kind.NEXT_EVENT, view.kind());
        assertEquals(100, view.seconds());
    }
}
