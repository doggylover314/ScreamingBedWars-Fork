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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.commands.AdminCommand;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.methods.OnDisable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers which arena every admin is currently setting up (used by {@code /bw setup} and {@code /bw set}).
 * <p>
 * Selections survive quit/rejoin (UUID key) and are lost on {@code /bw reload}, as are unsaved arenas in edit mode.
 */
@Service
public class SetupSessionService {
    private final Map<UUID, String> selections = new HashMap<>(); // player UUID -> arena name (key of AdminCommand.gc)

    public static @NotNull SetupSessionService getInstance() {
        return ServiceManager.get(SetupSessionService.class);
    }

    public void select(@NotNull Player player, @NotNull String arenaName) {
        selections.put(player.getUniqueId(), arenaName);
    }

    public @Nullable String getSelectedName(@NotNull Player player) {
        return selections.get(player.getUniqueId());
    }

    /**
     * The selected arena if it is (still) in edit mode, else null.
     */
    public @Nullable GameImpl getSelectedGame(@NotNull Player player) {
        var name = getSelectedName(player);
        return name == null || AdminCommand.gc == null ? null : AdminCommand.gc.get(name);
    }

    public void deselect(@NotNull Player player) {
        selections.remove(player.getUniqueId());
    }

    public void deselectArena(@NotNull String arenaName) {
        selections.values().removeIf(arenaName::equals);
    }

    @OnDisable
    public void onDisable() {
        selections.clear(); // AdminCommand.onDisable clears gc on /bw reload too
    }
}
