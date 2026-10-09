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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TargetKindTest {
    @Test
    void emptyKeepsTheClassicBedWording() {
        assertEquals(TargetKind.BED, TargetKind.aggregate(List.of()));
    }

    @Test
    void sameKindsAreKept() {
        assertEquals(TargetKind.BED, TargetKind.aggregate(List.of(TargetKind.BED, TargetKind.BED)));
        assertEquals(TargetKind.ANCHOR, TargetKind.aggregate(List.of(TargetKind.ANCHOR)));
        assertEquals(TargetKind.CAKE, TargetKind.aggregate(List.of(TargetKind.CAKE, TargetKind.CAKE)));
        assertEquals(TargetKind.DOOR, TargetKind.aggregate(List.of(TargetKind.DOOR)));
        assertEquals(TargetKind.OTHER, TargetKind.aggregate(List.of(TargetKind.OTHER, TargetKind.OTHER)));
    }

    @Test
    void mixedKindsAreOther() {
        assertEquals(TargetKind.OTHER, TargetKind.aggregate(List.of(TargetKind.BED, TargetKind.ANCHOR)));
        assertEquals(TargetKind.OTHER, TargetKind.aggregate(List.of(TargetKind.CAKE, TargetKind.BED, TargetKind.CAKE)));
    }

    @Test
    void nonBlockTargetsAreOther() {
        assertEquals(TargetKind.OTHER, TargetKind.aggregate(List.of(TargetKind.NON_BLOCK)));
        assertEquals(TargetKind.OTHER, TargetKind.aggregate(List.of(TargetKind.BED, TargetKind.NON_BLOCK)));
        assertEquals(TargetKind.OTHER, TargetKind.aggregate(List.of(TargetKind.NON_BLOCK, TargetKind.BED)));
    }
}
