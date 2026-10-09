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

package org.screamingsandals.bedwars.setup;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaNamesTest {
    private static Optional<ArenaNames.Problem> check(String name, List<String> taken) {
        return ArenaNames.validate(name, taken, ArenaNames.RESERVED);
    }

    @Test
    void acceptsNormalNames() {
        assertEquals(Optional.empty(), check("Arena_1", List.of()));
        assertEquals(Optional.empty(), check("my-arena", List.of("other")));
    }

    @Test
    void emptyNames() {
        assertEquals(Optional.of(ArenaNames.Problem.EMPTY), check("", List.of()));
        assertEquals(Optional.of(ArenaNames.Problem.EMPTY), check("  ", List.of()));
        assertEquals(Optional.of(ArenaNames.Problem.EMPTY), check(null, List.of()));
    }

    @Test
    void invalidCharacters() {
        assertEquals(Optional.of(ArenaNames.Problem.INVALID_CHARS), check("a b", List.of()));
        assertEquals(Optional.of(ArenaNames.Problem.INVALID_CHARS), check("a.b", List.of()));
        assertEquals(Optional.of(ArenaNames.Problem.INVALID_CHARS), check("\u00fc", List.of()));
    }

    @Test
    void tooLong() {
        assertEquals(Optional.empty(), check("a".repeat(ArenaNames.MAX_LENGTH), List.of()));
        assertEquals(Optional.of(ArenaNames.Problem.TOO_LONG), check("a".repeat(ArenaNames.MAX_LENGTH + 1), List.of()));
        assertEquals(Optional.of(ArenaNames.Problem.TOO_LONG), check("a".repeat(49), List.of()));
    }

    @Test
    void uuidLookingNamesAreRefused() {
        assertEquals(Optional.of(ArenaNames.Problem.LOOKS_LIKE_UUID), check("123e4567-e89b-12d3-a456-426614174000", List.of()));
    }

    @Test
    void reservedWordsAreRefusedCaseInsensitively() {
        assertEquals(Optional.of(ArenaNames.Problem.RESERVED), check("status", List.of()));
        assertEquals(Optional.of(ArenaNames.Problem.RESERVED), check("STATUS", List.of()));
        assertEquals(Optional.of(ArenaNames.Problem.RESERVED), check("confirm", List.of()));
    }

    @Test
    void takenNamesAreMatchedCaseInsensitively() {
        assertEquals(Optional.of(ArenaNames.Problem.TAKEN), check("arena1", List.of("Arena1")));
    }

    @Test
    void reservedWinsOverTaken() {
        assertEquals(Optional.of(ArenaNames.Problem.RESERVED), check("status", List.of("status")));
    }
}
