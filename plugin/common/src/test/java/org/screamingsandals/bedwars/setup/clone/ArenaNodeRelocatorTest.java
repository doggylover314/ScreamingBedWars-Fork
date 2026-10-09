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

package org.screamingsandals.bedwars.setup.clone;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaNodeRelocatorTest {
    private static ConfigurationNode sample() throws SerializationException {
        var root = BasicConfigurationNode.root();
        root.node("uuid").set("old");
        root.node("name").set("A");
        root.node("world").set("w1");
        root.node("pos1").set("0.0;60.0;0.0;0.0;0.0");
        root.node("pos2").set("99.5;100.0;99.5;0.0;0.0");
        root.node("specSpawn").set("50.5;80.0;50.5;90.0;10.0");
        root.node("lobbySpawn").set("50.5;95.0;50.5;0.0;0.0");
        root.node("lobbyPos1").set("40.0;94.0;40.0;0.0;0.0");
        root.node("lobbyPos2").set("60.0;99.0;60.0;0.0;0.0");
        root.node("lobbySpawnWorld").set("w1");

        root.node("teams", "Red", "color").set("RED");
        root.node("teams", "Red", "maxPlayers").set(4);
        root.node("teams", "Red", "target", "type").set("block");
        root.node("teams", "Red", "target", "loc").set("10.0;65.0;10.0;0.0;0.0");
        root.node("teams", "Red", "spawns").appendListNode().set("12.5;65.0;12.5;180.0;0.0");
        root.node("teams", "Blue", "color").set("BLUE");
        root.node("teams", "Blue", "target", "type").set("countdown");
        root.node("teams", "Blue", "target", "countdown").set(60);
        root.node("teams", "Blue", "spawns").appendListNode().set("80.5;65.0;80.5;0.0;0.0");

        var spawner = root.node("spawners").appendListNode();
        spawner.node("location").set("11.5;65.0;11.5;0.0;0.0");
        spawner.node("type").set("iron");
        spawner.node("team").set("Red");
        spawner.node("startLevel").set(1.0);

        var store = root.node("stores").appendListNode();
        store.node("loc").set("13.5;65.0;13.5;45.0;0.0");
        store.node("shop").set("x.yml");
        store.node("team").set("Red");
        root.node("stores").appendListNode().set("1.0;2.0;3.0;0.0;0.0"); // legacy store

        root.node("constant", "prefix").set("P");
        root.node("variant").set("certain-popular-server");
        root.node("game-display-name").set("<red>A");
        return root;
    }

    private static ArenaNodeRelocator.Options options(ClonePlanner.LobbyPolicy policy) {
        return new ArenaNodeRelocator.Options(new BlockOffset(1000, 0, -5), "w2", policy, "new", "B");
    }

    @Test
    void relocateShiftsEveryLocation() throws SerializationException {
        var root = sample();
        var result = ArenaNodeRelocator.relocate(root, options(ClonePlanner.LobbyPolicy.RELOCATE));

        assertEquals(12, result.relocated());
        assertTrue(result.warnings().isEmpty());

        assertEquals("new", root.node("uuid").getString());
        assertEquals("B", root.node("name").getString());
        assertEquals("w2", root.node("world").getString());
        assertEquals("w2", root.node("lobbySpawnWorld").getString());

        assertEquals("1000.0;60.0;-5.0;0.0;0.0", root.node("pos1").getString());
        assertEquals("1099.5;100.0;94.5;0.0;0.0", root.node("pos2").getString());
        assertEquals("1050.5;80.0;45.5;90.0;10.0", root.node("specSpawn").getString());
        assertEquals("1050.5;95.0;45.5;0.0;0.0", root.node("lobbySpawn").getString());
        assertEquals("1040.0;94.0;35.0;0.0;0.0", root.node("lobbyPos1").getString());
        assertEquals("1060.0;99.0;55.0;0.0;0.0", root.node("lobbyPos2").getString());

        assertEquals("1010.0;65.0;5.0;0.0;0.0", root.node("teams", "Red", "target", "loc").getString());
        assertEquals("1012.5;65.0;7.5;180.0;0.0", root.node("teams", "Red", "spawns").childrenList().get(0).getString());
        assertEquals("1080.5;65.0;75.5;0.0;0.0", root.node("teams", "Blue", "spawns").childrenList().get(0).getString());
        assertEquals("1011.5;65.0;6.5;0.0;0.0", root.node("spawners").childrenList().get(0).node("location").getString());
        assertEquals("1013.5;65.0;8.5;45.0;0.0", root.node("stores").childrenList().get(0).node("loc").getString());
        assertEquals("1001.0;2.0;-2.0;0.0;0.0", root.node("stores").childrenList().get(1).getString());
    }

    @Test
    void relocateKeepsEverythingElse() throws SerializationException {
        var root = sample();
        ArenaNodeRelocator.relocate(root, options(ClonePlanner.LobbyPolicy.RELOCATE));

        assertEquals("countdown", root.node("teams", "Blue", "target", "type").getString());
        assertEquals(60, root.node("teams", "Blue", "target", "countdown").getInt());
        assertTrue(root.node("teams", "Blue", "target", "loc").virtual());
        assertEquals("RED", root.node("teams", "Red", "color").getString());
        assertEquals(4, root.node("teams", "Red", "maxPlayers").getInt());
        assertEquals("P", root.node("constant", "prefix").getString());
        assertEquals("certain-popular-server", root.node("variant").getString());
        assertEquals("<red>A", root.node("game-display-name").getString());
        assertEquals(1.0, root.node("spawners").childrenList().get(0).node("startLevel").getDouble());
        assertEquals("iron", root.node("spawners").childrenList().get(0).node("type").getString());
        assertEquals("x.yml", root.node("stores").childrenList().get(0).node("shop").getString());
        assertEquals("Red", root.node("stores").childrenList().get(0).node("team").getString());
    }

    @Test
    void keepSharedLeavesTheLobbyAlone() throws SerializationException {
        var root = sample();
        var result = ArenaNodeRelocator.relocate(root, options(ClonePlanner.LobbyPolicy.KEEP_SHARED));

        assertEquals(9, result.relocated());
        assertEquals("50.5;95.0;50.5;0.0;0.0", root.node("lobbySpawn").getString());
        assertEquals("40.0;94.0;40.0;0.0;0.0", root.node("lobbyPos1").getString());
        assertEquals("60.0;99.0;60.0;0.0;0.0", root.node("lobbyPos2").getString());
        assertEquals("w1", root.node("lobbySpawnWorld").getString());
        assertEquals("w2", root.node("world").getString());
    }

    @Test
    void keepSharedDropRegionRemovesTheRegion() throws SerializationException {
        var root = sample();
        var result = ArenaNodeRelocator.relocate(root, options(ClonePlanner.LobbyPolicy.KEEP_SHARED_DROP_REGION));

        assertEquals(9, result.relocated());
        assertTrue(root.node("lobbyPos1").virtual());
        assertTrue(root.node("lobbyPos2").virtual());
        assertEquals("50.5;95.0;50.5;0.0;0.0", root.node("lobbySpawn").getString());
        assertEquals("w1", root.node("lobbySpawnWorld").getString());
    }

    @Test
    void unparsableLocationGivesAWarningAndStaysUnchanged() throws SerializationException {
        var root = sample();
        root.node("pos1").set("x;y");
        var result = ArenaNodeRelocator.relocate(root, options(ClonePlanner.LobbyPolicy.RELOCATE));

        assertEquals(11, result.relocated());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("pos1"));
        assertEquals("x;y", root.node("pos1").getString());
    }

    @Test
    void legacyTeamBedIsShifted() throws SerializationException {
        var root = sample();
        root.node("teams", "Red", "bed").set("5.0;65.0;5.0;0.0;0.0");
        var result = ArenaNodeRelocator.relocate(root, options(ClonePlanner.LobbyPolicy.RELOCATE));

        assertEquals(13, result.relocated());
        assertEquals("1005.0;65.0;0.0;0.0;0.0", root.node("teams", "Red", "bed").getString());
    }

    @Test
    void legacySingleSpawnIsShifted() throws SerializationException {
        var root = sample();
        root.node("teams", "Blue", "spawn").set("70.0;65.0;70.0;0.0;0.0");
        var result = ArenaNodeRelocator.relocate(root, options(ClonePlanner.LobbyPolicy.KEEP_SHARED));

        assertEquals(10, result.relocated());
        assertEquals("1070.0;65.0;65.0;0.0;0.0", root.node("teams", "Blue", "spawn").getString());
    }

    @Test
    void emptyArenaNodeOnlyGetsIdentity() throws SerializationException {
        var root = BasicConfigurationNode.root();
        var result = ArenaNodeRelocator.relocate(root, options(ClonePlanner.LobbyPolicy.RELOCATE));

        assertEquals(0, result.relocated());
        assertEquals("new", root.node("uuid").getString());
        assertEquals("B", root.node("name").getString());
        assertEquals("w2", root.node("world").getString());
        assertFalse(root.node("teams").isMap());
    }

    @Test
    void zeroOffsetKeepsLocationsButStillRenames() throws SerializationException {
        var root = sample();
        var opts = new ArenaNodeRelocator.Options(new BlockOffset(0, 0, 0), "w2", ClonePlanner.LobbyPolicy.RELOCATE, "new", "B");
        ArenaNodeRelocator.relocate(root, opts);

        assertEquals("0.0;60.0;0.0;0.0;0.0", root.node("pos1").getString());
        assertEquals("w2", root.node("world").getString());
        assertEquals("B", root.node("name").getString());
    }
}
