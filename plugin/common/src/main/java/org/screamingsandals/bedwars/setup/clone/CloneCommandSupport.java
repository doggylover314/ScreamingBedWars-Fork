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

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.sender.CommandSender;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * FOUNDATION-SKELETON: replaced by package P9 (PLAN.md).
 */
public final class CloneCommandSupport {
    private CloneCommandSupport() {
    }

    /**
     * Registers the clone sub-commands below the given root builder (no-op in the skeleton).
     */
    public static void register(@NotNull CommandManager<CommandSender> manager,
                                @NotNull Command.Builder<CommandSender> root,
                                @NotNull BiFunction<CommandContext<CommandSender>, Player, @Nullable GameImpl> sourceResolver,
                                @NotNull Function<CommandContext<CommandSender>, String> confirmCommand) {
    }
}
