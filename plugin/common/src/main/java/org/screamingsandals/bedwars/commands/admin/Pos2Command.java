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
import cloud.commandframework.execution.CommandExecutionHandler;
import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.commands.AdminCommand;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.setup.clone.ArenaCloneService;
import org.screamingsandals.bedwars.utils.ArenaUtils;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.world.Location;

import java.util.LinkedHashSet;
import java.util.function.Function;

@Service
public class Pos2Command extends BaseAdminSubCommand {
    public Pos2Command() {
        super("pos2");
    }

    @Override
    public void construct(CommandManager<CommandSender> manager, Command.Builder<CommandSender> commandSenderWrapperBuilder) {
        Function<Boolean, CommandExecutionHandler<CommandSender>> handler = force -> commandContext -> editMode(commandContext,
                (sender, game) -> setPos2(sender, game, sender.as(Player.class).getLocation(), force,
                        "/" + commandContext.getRawInputJoined() + " force"));

        manager.command(
                commandSenderWrapperBuilder
                        .handler(handler.apply(false))
        );

        manager.command(
                commandSenderWrapperBuilder
                        .literal("force")
                        .handler(handler.apply(true))
        );
    }

    /**
     * Sets position 2 of the arena (in edit mode). Returns true when it was set.
     *
     * @param forceCommand command shown to the admin when the overlap check refuses the position
     */
    public static boolean setPos2(@NotNull CommandSender sender, @NotNull GameImpl game, @NotNull Location loc, boolean force, @NotNull String forceCommand) {
        if (game.getWorld() == null) {
            game.setWorld(loc.getWorld());
        }
        if (!game.getWorld().equals(loc.getWorld())) {
            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_MUST_BE_IN_SAME_WORLD).defaultPrefix());
            return false;
        }
        if (game.getPos1() != null) {
            if (Math.abs(game.getPos1().getBlockY() - loc.getBlockY()) <= 5) {
                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_INVALID_BOUNDS).defaultPrefix());
                return false;
            }
        }

        if (!force) {
            // registered arenas and arenas that are still being created
            var others = new LinkedHashSet<GameImpl>(GameManagerImpl.getInstance().getLocalGames());
            others.addAll(AdminCommand.gc.values());
            for (var otherGame : others) {
                if (otherGame == game || otherGame.getPos1() == null || otherGame.getPos2() == null) {
                    continue;
                }

                if (
                        game.getPos1() != null && ArenaUtils.arenaOverlaps(otherGame.getPos1(), otherGame.getPos2(), game.getPos1(), loc)
                        || game.getPos1() == null && ArenaUtils.isInArea(loc, otherGame.getPos1(), otherGame.getPos2())
                ) {
                    sender.sendMessage(
                            Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_OVERLAPPING_ARENAS)
                                    .defaultPrefix()
                                    .placeholder("command", forceCommand)
                    );
                    return false;
                }
            }
            if (ArenaCloneService.getInstance().overlapsRunningTarget(loc, game.getPos1())) { // target area of a running clone
                sender.sendMessage(
                        Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_OVERLAPPING_ARENAS)
                                .defaultPrefix()
                                .placeholder("command", forceCommand)
                );
                return false;
            }
        }

        game.setPos2(loc);
        sender.sendMessage(
                Message
                        .of(LangKeys.ADMIN_ARENA_EDIT_SUCCESS_POS2_SET)
                        .defaultPrefix()
                        .placeholder("arena", game.getName())
                        .placeholder("x", loc.getBlockX())
                        .placeholder("y", loc.getBlockY())
                        .placeholder("z", loc.getBlockZ())
        );
        return true;
    }
}
