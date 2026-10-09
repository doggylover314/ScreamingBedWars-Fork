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

package org.screamingsandals.bedwars.game.mode;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import org.screamingsandals.bedwars.commands.BaseCommand;
import org.screamingsandals.bedwars.commands.BedWarsPermission;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.tasker.DefaultThreads;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.utils.annotations.Service;

/**
 * {@code /bw mode [list]|join|leavequeue}.
 */
@Service
public class ModeCommand extends BaseCommand {
    public ModeCommand() {
        super("mode", BedWarsPermission.MODE_PERMISSION, false);
    }

    @Override
    protected void construct(Command.Builder<CommandSender> builder, CommandManager<CommandSender> manager) {
        manager.command(builder.handler(ctx -> sendModeList(ctx.getSender().as(Player.class)))); // bare /bw mode
        manager.command(builder.literal("list").handler(ctx -> sendModeList(ctx.getSender().as(Player.class))));
        manager.command(builder.literal("join").handler(ctx -> sendModeList(ctx.getSender().as(Player.class)))); // no mode given
        manager.command(builder.literal("join")
                .argument(manager.argumentBuilder(String.class, "mode")
                        .withSuggestionsProvider((c, s) -> ModeManager.getInstance().getModeIds()))
                .handler(ctx -> {
                    var player = ctx.getSender().as(Player.class);
                    String modeId = ctx.get("mode");
                    Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> ModeJoinService.getInstance().joinMode(player, modeId));
                }));
        manager.command(builder.literal("leavequeue")
                .handler(ctx -> {
                    var player = ctx.getSender().as(Player.class);
                    Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> ModeJoinService.getInstance().leaveQueue(player));
                }));
    }

    private static void sendModeList(Player player) {
        player.sendMessage(Message.of(ForkLangKeys.MODES_LIST_HEADER).defaultPrefix());
        for (var mode : ModeManager.getInstance().getModes()) {
            var stats = ModeManager.getInstance().getStats(mode);
            player.sendMessage(Message.of(ForkLangKeys.MODES_LIST_ENTRY)
                    .placeholder("mode", ModeManager.displayNameComponent(mode))
                    .placeholderRaw("id", mode.id())
                    .placeholder("players", stats.players())
                    .placeholder("arenas", stats.joinableArenas()));
        }
    }
}
