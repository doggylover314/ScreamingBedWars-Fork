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

package org.screamingsandals.bedwars.party;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyGroupingTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);
    private static final UUID D = new UUID(0, 4);
    private static final UUID E = new UUID(0, 5);
    private static final UUID P1 = new UUID(9, 1);
    private static final UUID P2 = new UUID(9, 2);

    @Test
    void groupsPartiesTogetherInOrderOfFirstAppearance() {
        Map<UUID, UUID> party = new HashMap<>();
        party.put(A, P1);
        party.put(C, P1);
        party.put(D, P2);
        party.put(E, P2);
        var groups = PartyGrouping.group(List.of(A, B, C, D, E), u -> Optional.ofNullable(party.get(u)));
        assertEquals(List.of(List.of(A, C), List.of(B), List.of(D, E)), groups);
    }

    @Test
    void emptyInput() {
        assertTrue(PartyGrouping.group(List.of(), u -> Optional.empty()).isEmpty());
    }

    @Test
    void soloPlayersAreSingletons() {
        var groups = PartyGrouping.group(List.of(A, B), u -> Optional.empty());
        assertEquals(List.of(List.of(A), List.of(B)), groups);
    }

    @Test
    void everybodyInOneParty() {
        var groups = PartyGrouping.group(List.of(C, A, B), u -> Optional.of(P1));
        assertEquals(List.of(List.of(C, A, B)), groups);
    }

    @Test
    void resultGroupsAreImmutable() {
        var groups = PartyGrouping.group(List.of(A, B), u -> Optional.of(P1));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> groups.get(0).add(C));
    }
}
