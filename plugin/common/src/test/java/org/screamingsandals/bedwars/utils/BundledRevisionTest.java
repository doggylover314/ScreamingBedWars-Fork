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

package org.screamingsandals.bedwars.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledRevisionTest {
    @Test
    void parseFindsMarker() {
        assertEquals(3, BundledRevision.parse(List.of("#", "# fork-revision: 3", "name: x")));
        assertEquals(12, BundledRevision.parse(List.of("#fork-revision:12")));
        assertEquals(2, BundledRevision.parse(List.of("  # fork-revision: 2  ")));
    }

    @Test
    void parseReturnsMinusOneWithoutValidMarker() {
        assertEquals(-1, BundledRevision.parse(List.of("name: x")));
        assertEquals(-1, BundledRevision.parse(List.of()));
        assertEquals(-1, BundledRevision.parse(List.of("# fork-revision: abc")));
    }

    @Test
    void parseOnlyScansTheFirstLines() {
        List<String> late = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            late.add("line " + i);
        }
        late.add("# fork-revision: 7"); // index 64 = 65th line
        assertEquals(-1, BundledRevision.parse(late));

        List<String> border = new ArrayList<>();
        for (int i = 0; i < 63; i++) {
            border.add("line " + i);
        }
        border.add("# fork-revision: 7"); // index 63 = 64th line
        assertEquals(7, BundledRevision.parse(border));
    }

    @Test
    void parseFirstMarkerWins() {
        assertEquals(1, BundledRevision.parse(List.of("# fork-revision: 1", "# fork-revision: 5")));
    }

    @Test
    void isOutdated() {
        assertTrue(BundledRevision.isOutdated(-1, 1));
        assertTrue(BundledRevision.isOutdated(0, 1));
        assertFalse(BundledRevision.isOutdated(1, 1));
        assertFalse(BundledRevision.isOutdated(2, 1));
        assertFalse(BundledRevision.isOutdated(5, -1));
    }
}
