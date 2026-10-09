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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimelineStateTest {
    private static TimelineEventDefinition event(String id, long time, boolean showOnSidebar) {
        return new TimelineEventDefinition(id, TimelineEventType.ANNOUNCEMENT, time, null, 0, null, false,
                null, showOnSidebar, null, "message", null, null, null);
    }

    private static TimelineEventDefinition event(String id, long time) {
        return event(id, time, true);
    }

    @Test
    void eventsFireAtTheirSecondInDeclarationOrder() {
        var e0 = event("e0", 0);
        var a = event("A", 360);
        var b = event("B", 360);
        var e720 = event("e720", 720);
        var state = new TimelineState(List.of(e0, a, b, e720));

        assertEquals(List.of(e0), state.advance()); // #1
        for (int i = 2; i <= 359; i++) {
            assertTrue(state.advance().isEmpty(), "advance #" + i);
        }
        assertEquals(List.of(a, b), state.advance()); // #360
        for (int i = 361; i <= 719; i++) {
            assertTrue(state.advance().isEmpty(), "advance #" + i);
        }
        assertFalse(state.isFinished());
        assertEquals(List.of(e720), state.advance()); // #720
        assertTrue(state.isFinished());
        assertTrue(state.advance().isEmpty());
        assertTrue(state.advance().isEmpty());
        assertTrue(state.isFinished());
    }

    @Test
    void everyEventIsReturnedExactlyOnce() {
        var events = new ArrayList<TimelineEventDefinition>();
        for (int i = 0; i < 50; i++) {
            events.add(event("e" + i, i * 17L));
        }
        var state = new TimelineState(events);
        var seen = new HashMap<String, Integer>();
        for (int i = 0; i < 1000; i++) {
            for (var fired : state.advance()) {
                seen.merge(fired.id(), 1, Integer::sum);
            }
        }
        assertEquals(50, seen.size());
        for (Map.Entry<String, Integer> entry : seen.entrySet()) {
            assertEquals(1, entry.getValue(), entry.getKey() + " fired more than once");
        }
        assertEquals(50, state.getFiredCount());
    }

    @Test
    void elapsedSecondsEqualsTheNumberOfAdvances() {
        var state = new TimelineState(List.of());
        assertEquals(0, state.getElapsedSeconds());
        for (int i = 1; i <= 10; i++) {
            state.advance();
            assertEquals(i, state.getElapsedSeconds());
        }
        assertTrue(state.isFinished());
    }

    @Test
    void peekNextVisibleSkipsHiddenEventsAndEndsNull() {
        var hidden = event("hidden", 5, false);
        var shown = event("shown", 10);
        var hiddenLate = event("hidden-late", 20, false);
        var state = new TimelineState(List.of(hidden, shown, hiddenLate));

        assertSame(shown, state.peekNextVisible());
        for (int i = 0; i < 5; i++) {
            state.advance();
        }
        assertSame(shown, state.peekNextVisible()); // hidden already fired
        for (int i = 0; i < 5; i++) {
            state.advance();
        }
        assertNull(state.peekNextVisible()); // only a hidden one is left
        for (int i = 0; i < 20; i++) {
            state.advance();
        }
        assertNull(state.peekNextVisible());
        assertTrue(state.isFinished());
    }

    @Test
    void emptyStateIsFinishedImmediately() {
        var state = new TimelineState(List.of());
        assertTrue(state.isFinished());
        assertNull(state.peekNextVisible());
        assertTrue(state.getEvents().isEmpty());
    }

    @Test
    void eventsScheduledAtTimeZeroAreNotLost() {
        // time 0 is due on the very first advance
        var state = new TimelineState(List.of(event("late", 0), event("next", 2)));
        assertEquals(1, state.advance().size());
        assertEquals(1, state.advance().size());
        assertTrue(state.advance().isEmpty());
    }

    @Test
    void stateCopiesTheEventList() {
        var list = new ArrayList<>(List.of(event("a", 1)));
        var state = new TimelineState(list);
        list.clear();
        assertEquals(1, state.getEvents().size());
    }

    @Test
    void gameEndCountsAsFiredFromTheAdvanceThatConsumesIt() {
        var gameEnd = new TimelineEventDefinition("game-end", TimelineEventType.GAME_END, 3, null, 0, null, false,
                null, true, null, null, null, null, null);
        var state = new TimelineState(List.of(event("a", 1), gameEnd));
        assertFalse(state.hasFiredGameEnd());
        state.advance();
        state.advance();
        assertFalse(state.hasFiredGameEnd());
        state.advance();
        assertTrue(state.hasFiredGameEnd());
        assertFalse(new TimelineState(List.of(event("a", 1))).hasFiredGameEnd());
        assertFalse(new TimelineState(List.of()).hasFiredGameEnd());
    }
}
