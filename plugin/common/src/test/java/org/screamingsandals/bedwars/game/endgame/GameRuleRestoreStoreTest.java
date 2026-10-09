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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRuleRestoreStoreTest {
    @TempDir
    Path dir;

    @Test
    void missingFileIsEmpty() throws Exception {
        var store = new GameRuleRestoreStore(dir.resolve("gamerules.properties"));
        assertTrue(store.load().isEmpty());
        assertNull(store.get("arena"));
        store.remove("arena"); // no-op, creates nothing
        assertFalse(Files.exists(dir.resolve("gamerules.properties")));
    }

    @Test
    void putSurvivesANewInstance() throws Exception {
        var file = dir.resolve("data").resolve("gamerules.properties"); // the parent folder is created
        new GameRuleRestoreStore(file).put("arena", false);

        var reopened = new GameRuleRestoreStore(file);
        assertEquals(false, reopened.get("arena"));
        assertEquals(Map.of("arena", false), reopened.load());
        assertFalse(Files.exists(dir.resolve("data").resolve("gamerules.properties.tmp")), "the temp file is moved over the real one");
    }

    @Test
    void worldNamesWithSpecialCharacters() throws Exception {
        var store = new GameRuleRestoreStore(dir.resolve("gamerules.properties"));
        store.put("my arena: 1=a#b", false);
        store.put("lobby", true);
        assertEquals(Map.of("my arena: 1=a#b", false, "lobby", true), new GameRuleRestoreStore(dir.resolve("gamerules.properties")).load());
    }

    @Test
    void putReplacesAndRemoveKeepsOtherWorlds() throws Exception {
        var store = new GameRuleRestoreStore(dir.resolve("gamerules.properties"));
        store.put("a", false);
        store.put("b", false);
        store.put("a", true);
        assertEquals(Map.of("a", true, "b", false), store.load());

        store.remove("a");
        assertEquals(Map.of("b", false), store.load());
        assertTrue(Files.exists(dir.resolve("gamerules.properties")));
    }

    @Test
    void lastRemoveDeletesTheFile() throws Exception {
        var file = dir.resolve("gamerules.properties");
        var store = new GameRuleRestoreStore(file);
        store.put("a", false);
        assertTrue(Files.exists(file));
        store.remove("a");
        assertFalse(Files.exists(file));
        assertTrue(store.load().isEmpty());
    }

    @Test
    void damagedEntriesAreIgnored() throws Exception {
        var file = dir.resolve("gamerules.properties");
        Files.writeString(file, "good=false\nbad=maybe\nempty=\n");
        var store = new GameRuleRestoreStore(file);
        assertEquals(Map.of("good", false), store.load());

        store.put("other", true); // the next write drops the damaged entries
        assertEquals(Map.of("good", false, "other", true), store.load());
    }
}
