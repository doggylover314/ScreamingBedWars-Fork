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

package org.screamingsandals.bedwars.config;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MainConfigMainLobbyMigrationTest {

    @Test
    void oldLobbySetBeforeTheUpgradeIsCopiedOverTheGeneratedDefaults() throws ConfigurateException {
        var root = BasicConfigurationNode.root();
        root.node("mainlobby", "enabled").set(true);
        root.node("mainlobby", "location").set("10.5;64;-3.5;90;0");
        root.node("mainlobby", "world").set("hub");
        // defaults the generator wrote on the first start
        root.node("main-lobby", "enabled").set(false);
        root.node("main-lobby", "location").set("");
        root.node("main-lobby", "world").set("");

        MainConfig.migrateOldMainLobby(root);

        assertEquals(true, root.node("main-lobby", "enabled").getBoolean());
        assertEquals("10.5;64;-3.5;90;0", root.node("main-lobby", "location").getString());
        assertEquals("hub", root.node("main-lobby", "world").getString());
    }

    @Test
    void lobbyThatWasAlreadySetWithTheNewCommandIsKept() throws ConfigurateException {
        var root = BasicConfigurationNode.root();
        root.node("mainlobby", "enabled").set(true);
        root.node("mainlobby", "location").set("1;2;3;0;0");
        root.node("mainlobby", "world").set("old");
        root.node("main-lobby", "enabled").set(true);
        root.node("main-lobby", "location").set("7;8;9;0;0");
        root.node("main-lobby", "world").set("new");

        MainConfig.migrateOldMainLobby(root);

        assertEquals("7;8;9;0;0", root.node("main-lobby", "location").getString());
        assertEquals("new", root.node("main-lobby", "world").getString());
    }

    @Test
    void oldLobbyWithoutLocationIsIgnored() throws ConfigurateException {
        var root = BasicConfigurationNode.root();
        root.node("mainlobby", "enabled").set(true); // enabled with the old command but never set
        root.node("main-lobby", "enabled").set(false);
        root.node("main-lobby", "location").set("");

        MainConfig.migrateOldMainLobby(root);

        assertEquals(false, root.node("main-lobby", "enabled").getBoolean());
        assertEquals("", root.node("main-lobby", "location").getString());
    }

    @Test
    void missingOldEnabledKeepsTheNewValue() throws ConfigurateException {
        var root = BasicConfigurationNode.root();
        root.node("mainlobby", "location").set("1;2;3;0;0");
        root.node("mainlobby", "world").set("hub");
        root.node("main-lobby", "enabled").set(true);
        root.node("main-lobby", "location").set("");

        MainConfig.migrateOldMainLobby(root);

        assertEquals(true, root.node("main-lobby", "enabled").getBoolean());
        assertEquals("hub", root.node("main-lobby", "world").getString());
    }

    @Test
    void nothingToMigrateLeavesTheConfigUntouched() {
        var root = BasicConfigurationNode.root();

        MainConfig.migrateOldMainLobby(root);

        assertNull(root.node("main-lobby", "enabled").raw());
        assertNull(root.node("main-lobby", "location").raw());
        assertNull(root.node("main-lobby", "world").raw());
    }
}
