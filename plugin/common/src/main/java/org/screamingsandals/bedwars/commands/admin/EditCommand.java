/*
 * Copyright (C) 2025 ScreamingSandals
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

package org.screamingsandals.bedwars.commands.admin;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.commands.AdminCommand;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.utils.annotations.Service;

@Service
public class EditCommand extends BaseAdminSubCommand {
    public EditCommand() {
        super("edit");
    }

    @Override
    public void construct(CommandManager<CommandSender> manager, Command.Builder<CommandSender> commandSenderWrapperBuilder) {
        manager.command(
                commandSenderWrapperBuilder
                        .handler(commandContext -> enterEditMode(commandContext.getSender(), commandContext.get("game")))
        );
    }

    /**
     * Stops the arena and puts it into edit mode (keyed by its real name). Null + message if not found.
     */
    public static @Nullable GameImpl enterEditMode(@NotNull CommandSender sender, @NotNull String gameName) {
        var opt = GameManagerImpl.getInstance().getLocalGame(gameName);
        if (opt.isEmpty()) {
            Message.of(LangKeys.IN_GAME_ERRORS_GAME_NOT_FOUND).defaultPrefix().send(sender);
            return null;
        }

        var game = opt.get();
        game.stop();
        AdminCommand.gc.put(game.getName(), game); // keyed by the real name: the typed string can be the arena's UUID
        Message.of(LangKeys.ADMIN_ARENA_SUCCESS_EDIT_MODE)
                .defaultPrefix()
                .send(sender);
        return game;
    }
}
