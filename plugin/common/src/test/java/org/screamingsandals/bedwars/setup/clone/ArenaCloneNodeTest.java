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
import org.screamingsandals.bedwars.utils.ConfigurateUtils;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The clone serializes the arena into an in-memory node and writes it with the arena file loader at the end of the
 * job. The in-memory node must therefore be created like the one saveGame uses (enums become strings when set).
 */
class ArenaCloneNodeTest {
    enum Sample {
        Y
    }

    @Test
    void loaderCreatedNodeConvertsEnumsAndCanBeWrittenToAnotherLoaderNode() throws ConfigurateException {
        var loader = ConfigurateUtils.getConfigurationLoaderForFile(new File("arena.json"));
        var node = loader.createNode(); // what LocalGameLoaderImpl#serializeGame does
        var spawner = node.node("spawners").appendListNode();
        spawner.node("rotationMode").set(Sample.Y);
        spawner.node("initialInterval").set(TaskerTime.SECONDS);

        var copy = loader.createNode(); // what LocalGameLoaderImpl#writeNewArenaFile does
        copy.from(node);

        assertEquals("Y", copy.node("spawners").childrenList().get(0).node("rotationMode").getString());
        assertEquals(TaskerTime.SECONDS.name(), copy.node("spawners").childrenList().get(0).node("initialInterval").getString());
    }

    @Test
    void relocationKeepsConvertedValues() throws ConfigurateException {
        var loader = ConfigurateUtils.getConfigurationLoaderForFile(new File("arena.json"));
        var node = loader.createNode();
        node.node("pos1").set("0.0;60.0;0.0;0.0;0.0");
        node.node("spawners").appendListNode().node("rotationMode").set(Sample.Y);

        ArenaNodeRelocator.relocate(node, new ArenaNodeRelocator.Options(new BlockOffset(5, 0, 5), "w2",
                ClonePlanner.LobbyPolicy.KEEP_SHARED, "new-uuid", "Copy"));

        var copy = loader.createNode();
        copy.from(node);
        assertEquals("Copy", copy.node("name").getString());
        assertEquals("Y", copy.node("spawners").childrenList().get(0).node("rotationMode").getString());
    }

    @Test
    void plainBasicNodeKeepsRawEnumsAndTheLoaderNodeRefusesThem() throws ConfigurateException {
        // documents why serializeGame must not use BasicConfigurationNode.root()
        var loader = ConfigurateUtils.getConfigurationLoaderForFile(new File("arena.json"));
        var basic = BasicConfigurationNode.root();
        basic.node("rotationMode").set(Sample.Y);

        var target = loader.createNode();
        assertThrows(IllegalArgumentException.class, () -> target.from(basic));
    }
}
