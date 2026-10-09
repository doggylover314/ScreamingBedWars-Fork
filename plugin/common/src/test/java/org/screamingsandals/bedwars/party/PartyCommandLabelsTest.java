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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyCommandLabelsTest {
    @Test
    void normalize() {
        assertEquals("party", PartyCommandLabels.normalize(" Party "));
        assertEquals("my-party_1", PartyCommandLabels.normalize("My-Party_1"));
        assertNull(PartyCommandLabels.normalize("bw"));
        assertNull(PartyCommandLabels.normalize("BedWars"));
        assertNull(PartyCommandLabels.normalize("par ty"));
        assertNull(PartyCommandLabels.normalize("a:b"));
        assertNull(PartyCommandLabels.normalize(""));
        assertNull(PartyCommandLabels.normalize(null));
        assertNull(PartyCommandLabels.normalize("a".repeat(33)));
        assertEquals("a".repeat(32), PartyCommandLabels.normalize("a".repeat(32)));
    }

    @Test
    void labelOrDefault() {
        assertEquals("party", PartyCommandLabels.labelOrDefault("bw"));
        assertEquals("party", PartyCommandLabels.labelOrDefault(null));
        assertEquals("grp", PartyCommandLabels.labelOrDefault(" GRP "));
    }

    @Test
    void sanitizeAliases() {
        assertEquals(List.of("p", "pp"), PartyCommandLabels.sanitizeAliases("party", Arrays.asList("p", " P ", "party", "", "pa:rty", "pp", "p")));
        assertEquals(List.of(), PartyCommandLabels.sanitizeAliases("party", null));
        assertEquals(List.of("p"), PartyCommandLabels.sanitizeAliases("party", Arrays.asList(null, "p", "bw")));
    }

    @Test
    void inGameLabels() {
        var labels = PartyCommandLabels.inGameLabels("party", List.of("p"), List.of("ScreamingBedWars", "screamingbedwars"));
        assertEquals(Set.of("/party", "/p", "/screamingbedwars:party", "/screamingbedwars:p"), labels);
        assertEquals(Set.of("/party", "/p"), PartyCommandLabels.inGameLabels("party", List.of("p"), List.of(" ", "")));
    }

    @Test
    void matches() {
        var labels = PartyCommandLabels.inGameLabels("party", List.of("p"), List.of("screamingbedwars"));
        assertTrue(PartyCommandLabels.matches("/PARTY", labels));
        assertTrue(PartyCommandLabels.matches("/p", labels));
        assertTrue(PartyCommandLabels.matches("/ScreamingBedWars:Party", labels));
        assertFalse(PartyCommandLabels.matches("/partyx", labels));
        assertFalse(PartyCommandLabels.matches("party", labels));
        assertFalse(PartyCommandLabels.matches(null, labels));
        assertFalse(PartyCommandLabels.matches("/party", Set.of()));
    }

    @Test
    void clickCommand() {
        assertEquals("/party accept Bob", PartyCommandLabels.clickCommand(true, "party", "accept", "Bob"));
        assertEquals("/bw party accept Bob", PartyCommandLabels.clickCommand(false, "party", "accept", "Bob"));
        assertEquals("/party list", PartyCommandLabels.clickCommand(true, "party", "list", null));
        assertEquals("/grp deny Bob", PartyCommandLabels.clickCommand(true, "grp", "deny", "Bob"));
    }

    @Test
    void commandPrefix() {
        assertEquals("/bw party", PartyCommandLabels.commandPrefix(false, "x"));
        assertEquals("/x", PartyCommandLabels.commandPrefix(true, "x"));
    }
}
