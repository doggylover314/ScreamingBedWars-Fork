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

package org.screamingsandals.bedwars.lobby;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.bedwars.utils.MiscUtils;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.world.Location;
import org.screamingsandals.lib.world.Worlds;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Single accessor for the main (BedWars) lobby; always uses the {@code main-lobby.*} config keys.
 */
public final class MainLobby {
    private MainLobby() {
    }

    public static boolean isEnabled() {
        return MainConfig.getInstance().node("main-lobby", "enabled").getBoolean(false);
    }

    /**
     * Return-after-game teleport (GameImpl.internalLeavePlayer).
     */
    public static boolean shouldReturnPlayersAfterGame() {
        return isEnabled() && !MainConfig.getInstance().node("bungee", "enabled").getBoolean(false);
    }

    public static @Nullable Location getLocation() {
        var worldName = MainConfig.getInstance().node("main-lobby", "world").getString("");
        var location = MainConfig.getInstance().node("main-lobby", "location").getString("");
        if (worldName.isBlank() || location.isBlank()) {
            return null;
        }
        var world = Worlds.getWorld(worldName);
        if (world == null) {
            return null;
        }
        try {
            return MiscUtils.readLocationFromString(world, location);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void setLocation(@NotNull Location location) throws SerializationException {
        MainConfig.getInstance().node("main-lobby", "location").set(MiscUtils.writeLocationToString(location));
        MainConfig.getInstance().node("main-lobby", "world").set(location.getWorld().getName());
        MainConfig.getInstance().saveConfig();
    }

    public static void setEnabled(boolean enabled) throws SerializationException {
        MainConfig.getInstance().node("main-lobby", "enabled").set(enabled);
        MainConfig.getInstance().saveConfig();
    }

    /**
     * NPC action TELEPORT_TO_LOBBY: works whenever a location is set, independent of main-lobby.enabled.
     */
    public static void teleportFromNpc(@NotNull Player player) {
        if (PlayerManagerImpl.getInstance().isPlayerInGame(player)) {
            player.sendMessage(Message.of(LangKeys.IN_GAME_ERRORS_ALREADY_IN_GAME).defaultPrefix());
            return;
        }
        var location = getLocation();
        if (location == null) {
            player.sendMessage(Message.of(ForkLangKeys.MODES_MAIN_LOBBY_NOT_SET).defaultPrefix());
            return;
        }
        player.teleport(location);
    }
}
