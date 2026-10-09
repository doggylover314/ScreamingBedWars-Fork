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

package org.screamingsandals.bedwars.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.keys.SimpleCloudKey;
import cloud.commandframework.permission.PredicatePermission;
import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.BedWarsPlugin;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.party.PartyJoinCoordinator;
import org.screamingsandals.bedwars.party.PartyManagerImpl;
import org.screamingsandals.bedwars.party.PartySettings;
import org.screamingsandals.lib.Server;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The party commands: the root command {@code /party} (alias {@code /p}, both configurable) and the legacy path
 * {@code /bw party ...}. Both share one command tree, see {@link #registerTree}.
 */
@Service
public class PartyCommand extends BaseCommand {
    public PartyCommand() {
        super("party", BedWarsPermission.PARTY_PERMISSION, false);
    }

    @Override
    protected void construct(Command.Builder<CommandSender> bwParty, CommandManager<CommandSender> manager) {
        var settings = PartySettings.fromNode(MainConfig.getInstance().node("party")); // do not rely on the service order
        registerTree(bwParty, manager, settings); // /bw party ...
        boolean root = settings.enabled() && settings.rootCommand();
        if (root) {
            var rootBuilder = manager.commandBuilder(settings.label(), settings.aliases().toArray(String[]::new))
                    .permission(PredicatePermission.of(SimpleCloudKey.of("bw-party-root"), sender ->
                            sender.getType() == CommandSender.Type.CONSOLE
                                    || BedWarsPermission.PARTY_PERMISSION.asPermission().hasPermission(sender)))
                    .senderType(Player.class);
            registerTree(rootBuilder, manager, settings);
        }
        PartyManagerImpl.getInstance().setRootRegistered(root); // click buttons / hints use "/party" or "/bw party"
    }

    private void registerTree(Command.Builder<CommandSender> b, CommandManager<CommandSender> manager, PartySettings s) {
        manager.command(b.handler(ctx -> help(player(ctx))));
        manager.command(b.literal("help").handler(ctx -> help(player(ctx))));
        manager.command(b.literal("invite")
                .argument(onlinePlayerArg(manager, "player"))
                .handler(ctx -> pm().invite(player(ctx), ctx.<String>get("player"))));
        manager.command(b.literal("accept")
                .argument(inviterArg(manager, "player").asOptional())
                .handler(ctx -> pm().accept(player(ctx), ctx.<String>getOptional("player").orElse(null))));
        manager.command(b.literal("deny")
                .argument(inviterArg(manager, "player").asOptional())
                .handler(ctx -> pm().deny(player(ctx), ctx.<String>getOptional("player").orElse(null))));
        manager.command(b.literal("leave")
                .handler(ctx -> pm().leave(player(ctx))));
        manager.command(b.literal("kick", "remove")
                .argument(memberArg(manager, "player", false))
                .handler(ctx -> pm().kick(player(ctx), ctx.<String>get("player"))));
        manager.command(b.literal("disband")
                .handler(ctx -> pm().disband(player(ctx))));
        manager.command(b.literal("list", "members", "info")
                .handler(ctx -> pm().list(player(ctx))));
        manager.command(b.literal("chat")
                .argument(StringArgument.<CommandSender>optional("message", StringArgument.StringMode.GREEDY))
                .handler(ctx -> {
                    Optional<String> message = ctx.getOptional("message");
                    if (message.isPresent() && !message.get().isBlank()) {
                        pm().sendChat(player(ctx).getUuid(), message.get());
                    } else {
                        pm().toggleChat(player(ctx));
                    }
                }));
        manager.command(b.literal("transfer", "promote")
                .argument(memberArg(manager, "player", true))
                .handler(ctx -> pm().transfer(player(ctx), ctx.<String>get("player"))));
        manager.command(b.literal("warp")
                .handler(ctx -> PartyJoinCoordinator.getInstance().warp(player(ctx))));
        if (s.inviteShortcut()) {
            // registered LAST: a variable child next to the literals is the only construct cloud could reject (UNCERTAIN)
            try {
                manager.command(b.argument(onlinePlayerArg(manager, "player"))
                        .handler(ctx -> pm().invite(player(ctx), ctx.<String>get("player"))));
            } catch (RuntimeException ex) { // cloud AmbiguousNodeException (spec-party 13.2)
                BedWarsPlugin.getInstance().getLogger().warn("Party invite shortcut '/party <player>' disabled: {}", ex.toString());
            }
        }
    }

    private static @NotNull PartyManagerImpl pm() {
        return PartyManagerImpl.getInstance();
    }

    private static @NotNull Player player(@NotNull CommandContext<CommandSender> ctx) {
        return ctx.getSender().as(Player.class);
    }

    private void help(@NotNull Player player) {
        if (!pm().isEnabled()) {
            Message.of(ForkLangKeys.COMMON_FEATURE_DISABLED).defaultPrefix().send(player);
            return;
        }
        Message.of(ForkLangKeys.PARTY_HELP)
                .placeholderRaw("cmd", pm().commandPrefix())
                .placeholderRaw("arg_player", "<player>")
                .send(player);
    }

    private static @NotNull CommandArgument.Builder<CommandSender, String> onlinePlayerArg(@NotNull CommandManager<CommandSender> manager, @NotNull String name) {
        return manager.argumentBuilder(String.class, name).withSuggestionsProvider((context, input) -> {
            if (context.getSender().getType() != CommandSender.Type.PLAYER) {
                return List.of();
            }
            UUID self = context.getSender().as(Player.class).getUuid();
            return Server.getConnectedPlayers().stream()
                    .filter(p -> !p.getUuid().equals(self))
                    .map(Player::getName)
                    .collect(Collectors.toList());
        });
    }

    private static @NotNull CommandArgument.Builder<CommandSender, String> inviterArg(@NotNull CommandManager<CommandSender> manager, @NotNull String name) {
        return manager.argumentBuilder(String.class, name).withSuggestionsProvider((context, input) -> {
            if (context.getSender().getType() != CommandSender.Type.PLAYER) {
                return List.of();
            }
            return pm().suggestInviters(context.getSender().as(Player.class).getUuid());
        });
    }

    private static @NotNull CommandArgument.Builder<CommandSender, String> memberArg(@NotNull CommandManager<CommandSender> manager, @NotNull String name, boolean onlineOnly) {
        return manager.argumentBuilder(String.class, name).withSuggestionsProvider((context, input) -> {
            if (context.getSender().getType() != CommandSender.Type.PLAYER) {
                return List.of();
            }
            return pm().suggestMembers(context.getSender().as(Player.class).getUuid(), onlineOnly);
        });
    }
}
