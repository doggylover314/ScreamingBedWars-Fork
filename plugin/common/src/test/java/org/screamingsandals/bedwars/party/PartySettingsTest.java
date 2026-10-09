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
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.screamingsandals.bedwars.party.LeaderOfflineAction.DISBAND;
import static org.screamingsandals.bedwars.party.LeaderOfflineAction.TRANSFER;

class PartySettingsTest {
    private static ConfigurationNode yaml(String yaml) throws Exception {
        return YamlConfigurationLoader.builder().buildAndLoadString(yaml);
    }

    @Test
    void emptyNodeGivesTheDocumentedDefaults() {
        var s = PartySettings.fromNode(BasicConfigurationNode.root());
        assertTrue(s.enabled());
        assertTrue(s.autojoinMembers());
        assertTrue(s.notifyWhenWarped());
        assertTrue(s.requireLeaderToJoin());
        assertEquals(4, s.maxSize());
        assertFalse(s.membersCanInvite());
        assertEquals(60, s.inviteExpireSeconds());
        assertEquals(300, s.leaderGraceSeconds());
        assertEquals(TRANSFER, s.leaderOfflineAction());
        assertEquals(300, s.memberOfflineTimeoutSeconds());
        assertTrue(s.mustFitOneTeam());
        assertFalse(s.pullFromRunningGames());
        assertTrue(s.warpEnabled());
        assertTrue(s.warpTeleportOutsideGames());
        assertEquals(5, s.warpTeleportDelayTicks());
        assertTrue(s.chatEnabled());
        assertEquals("!", s.chatBypassPrefix());
        assertTrue(s.chatLogToConsole());
        assertTrue(s.rootCommand());
        assertEquals("party", s.label());
        assertEquals(List.of("p"), s.aliases());
        assertTrue(s.allowInGame());
        assertTrue(s.inviteShortcut());
    }

    @Test
    void sizeAndExpiryAreClamped() throws Exception {
        assertEquals(0, PartySettings.fromNode(yaml("max-size: -5")).maxSize());
        assertEquals(0, PartySettings.fromNode(yaml("max-size: 0")).maxSize());
        assertEquals(60, PartySettings.fromNode(yaml("invite-expire-seconds: 0")).inviteExpireSeconds());
        assertEquals(60, PartySettings.fromNode(yaml("invite-expire-seconds: -3")).inviteExpireSeconds());
        assertEquals(5, PartySettings.fromNode(yaml("invite-expire-seconds: 2")).inviteExpireSeconds());
        assertEquals(3600, PartySettings.fromNode(yaml("invite-expire-seconds: 99999")).inviteExpireSeconds());
        assertEquals(90, PartySettings.fromNode(yaml("invite-expire-seconds: 90")).inviteExpireSeconds());
    }

    @Test
    void leaderOfflineAction() throws Exception {
        assertEquals(DISBAND, PartySettings.fromNode(yaml("leader-offline:\n  action: DISBAND")).leaderOfflineAction());
        assertEquals(TRANSFER, PartySettings.fromNode(yaml("leader-offline:\n  action: foo")).leaderOfflineAction());
    }

    @Test
    void labelAndAliasesAreSanitised() throws Exception {
        assertEquals("party", PartySettings.fromNode(yaml("commands:\n  label: \"Bw\"")).label());
        assertEquals("grp", PartySettings.fromNode(yaml("commands:\n  label: \"Grp\"")).label());
        assertEquals(List.of("p"), PartySettings.fromNode(yaml("commands:\n  aliases: [\" P \", \"party\", \"x y\"]")).aliases());
        assertEquals(List.of(), PartySettings.fromNode(yaml("commands:\n  aliases: []")).aliases());
        // an alias equal to a custom label is dropped
        var custom = PartySettings.fromNode(yaml("commands:\n  label: grp\n  aliases: [grp, g, party]"));
        assertEquals(List.of("g", "party"), custom.aliases());
    }

    @Test
    void warpDelayIsClamped() throws Exception {
        assertEquals(1, PartySettings.fromNode(yaml("warp:\n  teleport-delay-ticks: 0")).warpTeleportDelayTicks());
        assertEquals(100, PartySettings.fromNode(yaml("warp:\n  teleport-delay-ticks: 500")).warpTeleportDelayTicks());
        assertEquals(20, PartySettings.fromNode(yaml("warp:\n  teleport-delay-ticks: 20")).warpTeleportDelayTicks());
    }

    @Test
    void chatBypassPrefixMayBeEmpty() throws Exception {
        assertEquals("", PartySettings.fromNode(yaml("chat:\n  bypass-prefix: ''")).chatBypassPrefix());
        assertEquals("@", PartySettings.fromNode(yaml("chat:\n  bypass-prefix: '@'")).chatBypassPrefix());
    }

    @Test
    void switchesAreRead() throws Exception {
        var s = PartySettings.fromNode(yaml(String.join("\n",
                "enabled: false",
                "autojoin-members: false",
                "require-leader-to-join: false",
                "members-can-invite: true",
                "join:",
                "  must-fit-one-team: false",
                "  pull-from-running-games: true",
                "commands:",
                "  root-command: false",
                "  invite-shortcut: false",
                "  allow-in-game: false")));
        assertFalse(s.enabled());
        assertFalse(s.autojoinMembers());
        assertFalse(s.requireLeaderToJoin());
        assertTrue(s.membersCanInvite());
        assertFalse(s.mustFitOneTeam());
        assertTrue(s.pullFromRunningGames());
        assertFalse(s.rootCommand());
        assertFalse(s.inviteShortcut());
        assertFalse(s.allowInGame());
    }

    @Test
    void toRules() throws Exception {
        assertEquals(new PartyRules(4, false, 60_000, 300_000, TRANSFER, 300_000), PartySettings.fromNode(BasicConfigurationNode.root()).toRules());
        var never = PartySettings.fromNode(yaml("leader-offline:\n  grace-seconds: -1\nmember-offline-timeout-seconds: -1"));
        assertEquals(-1, never.toRules().leaderGraceMillis());
        assertEquals(-1, never.toRules().memberOfflineMillis());
        var zero = PartySettings.fromNode(yaml("leader-offline:\n  grace-seconds: 0\nmember-offline-timeout-seconds: 0"));
        assertEquals(0, zero.toRules().leaderGraceMillis());
        assertEquals(0, zero.toRules().memberOfflineMillis());
    }

    @Test
    void leaderOfflineActionParse() {
        assertEquals(TRANSFER, LeaderOfflineAction.parse(" transfer ", DISBAND));
        assertEquals(DISBAND, LeaderOfflineAction.parse("DISBAND", TRANSFER));
        assertEquals(TRANSFER, LeaderOfflineAction.parse(null, TRANSFER));
        assertEquals(DISBAND, LeaderOfflineAction.parse("x", DISBAND));
    }
}
