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

package org.screamingsandals.bedwars.commands.admin;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.setup.clone.CloneCommandSupport;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.utils.annotations.Service;

/**
 * {@code /bw admin <arena> clone <new-name> <x> <y> <z> [world]} and {@code ... clone confirm|cancel|status}:
 * copies an arena (blocks, block entities, entities and every stored location) to another place, see
 * {@link org.screamingsandals.bedwars.setup.clone.ArenaCloneService}.
 */
@Service
public class CloneCommand extends BaseAdminSubCommand {
    public CloneCommand() {
        super("clone");
    }

    @Override
    public void construct(CommandManager<CommandSender> manager, Command.Builder<CommandSender> commandSenderWrapperBuilder) {
        CloneCommandSupport.register(manager, commandSenderWrapperBuilder,
                (ctx, player) -> {
                    var game = viewMode(ctx); // arena in edit mode or a registered local arena
                    if (game == null) {
                        Message.of(LangKeys.IN_GAME_ERRORS_GAME_NOT_FOUND).defaultPrefix().send(player);
                    }
                    return game;
                },
                ctx -> "/bw admin " + ctx.<String>get("game") + " clone confirm");
    }
}
