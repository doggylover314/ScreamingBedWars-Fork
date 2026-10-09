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

package org.screamingsandals.bedwars.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.lib.debug.Debug;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.spectator.sound.SoundSource;
import org.screamingsandals.lib.spectator.sound.SoundStart;
import org.screamingsandals.lib.utils.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Small helpers for messages, titles and sounds sent to the players of a game.
 * Runtime helper (needs the server), not unit-testable.
 */
public final class Broadcasts {
    private Broadcasts() {
    }

    /**
     * Chat to every connected player of the game (including spectators) with the arena prefix.
     */
    public static void chat(@NotNull GameImpl game, @NotNull Message message) {
        chat(game, game.getConnectedPlayers(), message);
    }

    public static void chat(@NotNull GameImpl game, @NotNull Collection<? extends BedWarsPlayer> receivers, @NotNull Message message) {
        message.prefixOrDefault(game.getCustomPrefixComponent());
        receivers.forEach(receiver -> message.send(receiver));
    }

    /**
     * Title: translation element 0 = title, element 1 = subtitle; uses the configured title times.
     */
    public static void title(@NotNull Collection<? extends BedWarsPlayer> receivers, @NotNull Message titleAndSubtitle) {
        titleAndSubtitle.times(TitleUtils.defaultTimes());
        receivers.forEach(receiver -> titleAndSubtitle.title(receiver));
    }

    public static void sound(@NotNull Collection<? extends Player> receivers, @Nullable SoundStart sound) {
        if (sound != null) {
            receivers.forEach(player -> player.playSound(sound));
        }
    }

    /**
     * Reads config.yml {@code sounds.<name>.{sound,volume,pitch}}; "" / "none" / invalid -> null (= disabled).
     */
    public static @Nullable SoundStart configuredSound(@NotNull String name, @NotNull String defaultSound) {
        var node = MainConfig.getInstance().node("sounds", name);
        var key = node.node("sound").getString(defaultSound);
        if (key == null || key.isBlank() || "none".equalsIgnoreCase(key)) {
            return null;
        }
        try {
            return SoundStart.sound(
                    ResourceLocation.of(key),
                    SoundSource.AMBIENT,
                    (float) node.node("volume").getDouble(1),
                    (float) node.node("pitch").getDouble(1)
            );
        } catch (RuntimeException ex) {
            Debug.warn("Invalid sound '" + key + "' in sounds." + name, true);
            return null;
        }
    }

    /**
     * Members of the team that are currently alive (not final-dead, not waiting for respawn).
     */
    public static @NotNull List<BedWarsPlayer> aliveMembers(@NotNull TeamImpl team) {
        return team.getPlayers().stream().filter(p -> !p.isSpectator()).collect(Collectors.toList());
    }
}
