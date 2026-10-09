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
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.world.World;
import org.screamingsandals.lib.world.Worlds;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Registers the clone sub-commands below a root command, shared by {@code /bw admin <arena> clone ...} and
 * {@code /bw setup clone ...}:
 * <pre>
 * &lt;root&gt; &lt;new-name&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt; [world]
 * &lt;root&gt; confirm | cancel | status
 * </pre>
 * {@code <x> <y> <z>} is the new position of the minimum corner of the source arena box; each accepts an integer, a
 * decimal (floored), {@code ~} or {@code ~<n>} relative to the sender's block coordinate.
 */
public final class CloneCommandSupport {
    private CloneCommandSupport() {
    }

    /**
     * @param sourceResolver resolves the source arena of the command (sends the error and returns null when there is none)
     * @param confirmCommand the command the clickable confirm button runs
     */
    public static void register(@NotNull CommandManager<CommandSender> manager,
                                @NotNull Command.Builder<CommandSender> root,
                                @NotNull BiFunction<CommandContext<CommandSender>, Player, @Nullable GameImpl> sourceResolver,
                                @NotNull Function<CommandContext<CommandSender>, String> confirmCommand) {
        Supplier<ArenaCloneService> service = ArenaCloneService::getInstance;

        manager.command(root.literal("confirm").handler(ctx -> {
            var player = ctx.getSender().as(Player.class);
            var source = sourceResolver.apply(ctx, player);
            if (source != null) {
                service.get().confirm(player, source);
            }
        }));
        manager.command(root.literal("cancel").handler(ctx -> service.get().cancel(ctx.getSender())));
        manager.command(root.literal("status").handler(ctx -> service.get().status(ctx.getSender())));
        manager.command(root
                .argument(StringArgument.<CommandSender>newBuilder("new-name"))
                .argument(coordinate("x", 0))
                .argument(coordinate("y", 1))
                .argument(coordinate("z", 2))
                .argument(StringArgument.<CommandSender>newBuilder("world")
                        .withSuggestionsProvider((c, s) -> Worlds.getWorlds().stream().map(World::getName).collect(Collectors.toList()))
                        .asOptional())
                .handler(ctx -> {
                    var player = ctx.getSender().as(Player.class);
                    var source = sourceResolver.apply(ctx, player);
                    if (source == null) {
                        return;
                    }
                    service.get().request(player, source, ctx.get("new-name"), ctx.get("x"), ctx.get("y"), ctx.get("z"),
                            ctx.<String>getOptional("world").orElse(null), confirmCommand.apply(ctx));
                }));
    }

    /**
     * Suggests "~" and the sender's block coordinate on that axis (0 = X, 1 = Y, 2 = Z).
     * A negative number such as {@code -120} is a normal single token for the string argument.
     */
    private static @NotNull CommandArgument.Builder<CommandSender, String> coordinate(@NotNull String name, int axis) {
        return StringArgument.<CommandSender>newBuilder(name).withSuggestionsProvider((c, s) -> {
            try {
                var l = c.getSender().as(Player.class).getLocation();
                int v = axis == 0 ? l.getBlockX() : axis == 1 ? l.getBlockY() : l.getBlockZ();
                return List.of("~", String.valueOf(v));
            } catch (RuntimeException e) {
                return List.of("~");
            }
        });
    }
}
