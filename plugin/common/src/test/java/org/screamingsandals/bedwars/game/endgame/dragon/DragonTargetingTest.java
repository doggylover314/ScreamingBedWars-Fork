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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DragonTargetingTest {
    private static final Vec3 DRAGON = new Vec3(0, 0, 0);
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes());
    private static final UUID B = UUID.nameUUIDFromBytes("b".getBytes());

    @Test
    void emptyListGivesNoTarget() {
        assertNull(DragonTargeting.choose(DRAGON, List.of(), null, 1.5));
        assertNull(DragonTargeting.choose(DRAGON, List.of(), A, 1.5));
    }

    @Test
    void picksTheNearestWithoutCurrentTarget() {
        var list = List.of(
                new DragonTargeting.Candidate(A, new Vec3(10, 0, 0)),
                new DragonTargeting.Candidate(B, new Vec3(5, 0, 0))
        );
        assertEquals(B, DragonTargeting.choose(DRAGON, list, null, 1.5));
    }

    @Test
    void keepsTheCurrentTargetWhileItIsNotMuchFarther() {
        var list = List.of(
                new DragonTargeting.Candidate(A, new Vec3(10, 0, 0)),
                new DragonTargeting.Candidate(B, new Vec3(8, 0, 0))
        );
        assertEquals(A, DragonTargeting.choose(DRAGON, list, A, 1.5));
    }

    @Test
    void switchesWhenTheCurrentTargetIsTooFar() {
        var list = List.of(
                new DragonTargeting.Candidate(A, new Vec3(10, 0, 0)),
                new DragonTargeting.Candidate(B, new Vec3(6, 0, 0))
        );
        assertEquals(B, DragonTargeting.choose(DRAGON, list, A, 1.5));
    }

    @Test
    void ignoresACurrentTargetThatIsNotInTheList() {
        var list = List.of(new DragonTargeting.Candidate(B, new Vec3(50, 0, 0)));
        assertEquals(B, DragonTargeting.choose(DRAGON, list, A, 1.5));
    }
}
