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
import org.screamingsandals.bedwars.api.config.ConfigurationKey;
import org.screamingsandals.bedwars.api.config.ConfigurationListKey;
import org.screamingsandals.bedwars.api.config.GameConfigurationContainer;
import org.screamingsandals.bedwars.game.mode.ModeConfigKeys;
import org.screamingsandals.bedwars.party.PartyConfigKeys;
import org.screamingsandals.bedwars.setup.SetupConfigKeys;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameConfigurationContainerImplTest {

    private static List<List<String>> keyPaths(GameConfigurationContainerImpl container) {
        return container.getRegisteredKeys().stream().map(ConfigurationKey::getKey).collect(Collectors.toList());
    }

    private static List<List<String>> listKeyPaths(GameConfigurationContainerImpl container) {
        return container.getRegisteredListKeys().stream().map(ConfigurationListKey::getKey).collect(Collectors.toList());
    }

    @Test
    void kickPlayersDelayIsRegisteredSeparatelyFromEnabled() {
        var keys = keyPaths(new GameConfigurationContainerImpl());
        assertTrue(keys.contains(GameConfigurationContainer.KICK_PLAYERS_UPON_FINAL_DEATH_ENABLED.getKey()));
        assertTrue(keys.contains(GameConfigurationContainer.KICK_PLAYERS_UPON_FINAL_DEATH_DELAY.getKey()));
    }

    @Test
    void timelineAndEndgameKeysAreRegistered() {
        var container = new GameConfigurationContainerImpl();
        var keys = keyPaths(container);
        assertTrue(keys.contains(GameConfigurationContainer.TIMELINE_ENABLED.getKey()));
        assertTrue(keys.contains(GameConfigurationContainer.BED_DESTRUCTION_ANNOUNCE.getKey()));
        assertTrue(keys.contains(GameConfigurationContainer.SUDDEN_DEATH_DRAGON_BLOCK_DESTRUCTION.getKey()));
        assertTrue(keys.contains(GameConfigurationContainer.GAME_END_BY_TIME_MODE.getKey()));
        assertTrue(listKeyPaths(container).contains(GameConfigurationContainer.SUDDEN_DEATH_DRAGON_IMMUNE_BLOCKS.getKey()));
        assertTrue(listKeyPaths(container).contains(GameConfigurationContainer.GAME_END_BY_TIME_TIE_BREAK.getKey()));
    }

    @Test
    void partyKeysAreRegistered() {
        var keys = keyPaths(new GameConfigurationContainerImpl());
        assertTrue(keys.contains(PartyConfigKeys.REQUIRE_LEADER_TO_JOIN.getKey()));
        assertTrue(keys.contains(PartyConfigKeys.AUTOJOIN_MEMBERS.getKey()));
    }

    @Test
    void modeKeysAreRegistered() {
        var container = new GameConfigurationContainerImpl();
        var listKeys = listKeyPaths(container);
        assertTrue(listKeys.contains(ModeConfigKeys.ALLOWED_TEAM_SIZES.getKey()));
        assertTrue(listKeys.contains(ModeConfigKeys.PREFERRED_TEAM_SIZES.getKey()));
        assertTrue(listKeys.contains(ModeConfigKeys.TEAM_PRIORITY.getKey()));
        var keys = keyPaths(container);
        assertTrue(keys.contains(ModeConfigKeys.ONLY_VIA_MODE_SELECTION.getKey()));
        assertTrue(keys.contains(ModeConfigKeys.DISABLE_TEAM_SELECTION.getKey()));
        assertTrue(keys.contains(ModeConfigKeys.KEEP_PARTIES_TOGETHER.getKey()));
    }

    @Test
    void setupKeysAreRegistered() {
        var container = new GameConfigurationContainerImpl();
        assertTrue(listKeyPaths(container).contains(SetupConfigKeys.TEAM_GENERATOR_TYPES.getKey()));
        var keys = keyPaths(container);
        assertTrue(keys.contains(SetupConfigKeys.TEAM_GENERATOR_HOLOGRAM.getKey()));
        assertTrue(keys.contains(SetupConfigKeys.DIAMOND_SPAWNER_TYPE.getKey()));
        assertTrue(keys.contains(SetupConfigKeys.EMERALD_SPAWNER_TYPE.getKey()));
        assertTrue(keys.contains(SetupConfigKeys.SHOP_FILE.getKey()));
        assertTrue(keys.contains(SetupConfigKeys.UPGRADE_SHOP_FILE.getKey()));
    }

    @Test
    void registeringTheSameKeyTwiceIsHarmless() {
        var container = new GameConfigurationContainerImpl();
        int before = container.getRegisteredKeys().size() + container.getRegisteredListKeys().size();
        PartyConfigKeys.register(container);
        ModeConfigKeys.register(container);
        SetupConfigKeys.register(container);
        assertEquals(before, container.getRegisteredKeys().size() + container.getRegisteredListKeys().size());
    }
}
