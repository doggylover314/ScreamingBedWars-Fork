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
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import org.screamingsandals.bedwars.commands.admin.AddCommand;
import org.screamingsandals.bedwars.commands.admin.EditCommand;
import org.screamingsandals.bedwars.commands.admin.SaveCommand;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.game.LocalGameLoaderImpl;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.setup.ArenaNames;
import org.screamingsandals.bedwars.setup.SetupChecklist;
import org.screamingsandals.bedwars.setup.SetupOperations;
import org.screamingsandals.bedwars.setup.SetupSessionService;
import org.screamingsandals.bedwars.setup.clone.CloneCommandSupport;
import org.screamingsandals.bedwars.variants.VariantManagerImpl;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code /bw setup}: creates or opens an arena, selects it for the admin and shows what is still missing.
 * The things themselves are set with {@code /bw set} ({@link SetCommand}).
 */
@Service
public class SetupCommand extends BaseCommand {
    public SetupCommand() {
        super("setup", BedWarsPermission.ADMIN_PERMISSION, false);
    }

    @Override
    protected void construct(Command.Builder<CommandSender> b, CommandManager<CommandSender> manager) {
        manager.command(b.handler(ctx -> { // /bw setup
            var p = ctx.getSender().as(Player.class);
            var game = SetupSessionService.getInstance().getSelectedGame(p);
            if (game == null) {
                Message.of(ForkLangKeys.SETUP_USAGE).defaultPrefix().send(p);
            } else {
                SetupOperations.sendStatus(p, game);
            }
        }));
        manager.command(b.literal("status").handler(ctx -> SetupOperations.withSelected(ctx, SetupOperations::sendStatus)));
        manager.command(b.literal("save").handler(ctx -> save(ctx, false)));
        manager.command(b.literal("save").literal("force").handler(ctx -> save(ctx, true)));
        manager.command(b.literal("variant")
                .argument(StringArgument.<CommandSender>newBuilder("variant")
                        .withSuggestionsProvider((c, s) -> VariantManagerImpl.getInstance().getVariantNames()))
                .handler(ctx -> SetupOperations.withSelected(ctx, (p, game) ->
                        SetupOperations.changeVariant(p, game, ctx.get("variant")))));
        manager.command(b.literal("cancel").handler(this::cancel));
        CloneCommandSupport.register(manager, b.literal("clone"),
                (ctx, player) -> SetupOperations.selectedSourceForClone(player),
                ctx -> "/bw setup clone confirm");
        manager.command(b
                .argument(StringArgument.<CommandSender>newBuilder("arena")
                        .withSuggestionsProvider((c, s) -> Stream.concat(GameManagerImpl.getInstance().getLocalGameNames().stream(),
                                AdminCommand.gc.keySet().stream()).distinct().collect(Collectors.toList())))
                .argument(StringArgument.<CommandSender>newBuilder("variant")
                        .withSuggestionsProvider((c, s) -> VariantManagerImpl.getInstance().getVariantNames())
                        .asOptional())
                .handler(this::open));
    }

    private void open(CommandContext<CommandSender> ctx) {
        var player = ctx.getSender().as(Player.class);
        String name = ctx.get("arena");
        String variant = ctx.<String>getOptional("variant").orElse(null);

        GameImpl game = AdminCommand.gc.get(name);
        if (game == null) {
            var local = GameManagerImpl.getInstance().getLocalGame(name);
            if (local.isPresent()) {
                var existing = local.get();
                if (existing.countConnectedPlayers() > 0) {
                    player.sendMessage(Message.of(ForkLangKeys.SETUP_ARENA_HAS_PLAYERS)
                            .defaultPrefix()
                            .placeholderRaw("arena", existing.getName())
                            .placeholder("count", existing.countConnectedPlayers())
                            .placeholder("command", SetupOperations.clickable(player, "/bw admin " + existing.getName() + " edit", true)));
                    return;
                }
                game = EditCommand.enterEditMode(player, existing.getName());
                if (game == null) {
                    return;
                }
                if (variant != null) {
                    SetupOperations.changeVariant(player, game, variant);
                }
            } else if (GameManagerImpl.getInstance().hasGame(name)) { // remote game with that name
                player.sendMessage(Message.of(LangKeys.ADMIN_ARENA_ERROR_ALREADY_EXISTS).defaultPrefix());
                return;
            } else {
                // names that are already taken are handled by createArena
                if (ArenaNames.validate(name, List.of(), ArenaNames.RESERVED).isPresent()) {
                    player.sendMessage(Message.of(ForkLangKeys.SETUP_INVALID_NAME).defaultPrefix().placeholderRaw("arena", name));
                    return;
                }
                game = AddCommand.createArena(player, name, variant);
                if (game == null) {
                    return;
                }
            }
        } else if (variant != null) {
            SetupOperations.changeVariant(player, game, variant);
        }

        SetupSessionService.getInstance().select(player, game.getName());
        player.sendMessage(Message.of(ForkLangKeys.SETUP_SELECTED)
                .defaultPrefix()
                .placeholderRaw("arena", game.getName())
                .placeholderRaw("variant", game.getGameVariant() == null ? "-" : game.getGameVariant().getName()));
        SetupOperations.sendStatus(player, game);
    }

    private void save(CommandContext<CommandSender> ctx, boolean force) {
        SetupOperations.withSelected(ctx, (p, game) -> {
            var entries = SetupChecklist.evaluate(SetupOperations.snapshot(game));
            if (!SetupChecklist.isSaveable(entries)) {
                p.sendMessage(Message.of(ForkLangKeys.SETUP_SAVE_BLOCKED).defaultPrefix().placeholderRaw("arena", game.getName()));
                entries.stream()
                        .filter(e -> e.severity() == SetupChecklist.Severity.REQUIRED && !e.done())
                        .forEach(e -> SetupOperations.sendEntry(p, e));
                return;
            }
            if (SaveCommand.saveArena(p, game, force, "/bw setup save force")) {
                SetupSessionService.getInstance().deselectArena(game.getName());
            }
        });
    }

    private void cancel(CommandContext<CommandSender> ctx) {
        var player = ctx.getSender().as(Player.class);
        var sessions = SetupSessionService.getInstance();
        var name = sessions.getSelectedName(player);
        if (name == null) {
            player.sendMessage(Message.of(ForkLangKeys.SETUP_NO_SELECTION).defaultPrefix());
            return;
        }
        var game = AdminCommand.gc.remove(name);
        sessions.deselectArena(name);
        if (game == null) {
            player.sendMessage(Message.of(ForkLangKeys.SETUP_NOT_IN_EDIT)
                    .defaultPrefix()
                    .placeholderRaw("arena", name)
                    .placeholder("command", SetupOperations.clickable(player, "/bw setup " + name, true)));
            return;
        }

        boolean registered = GameManagerImpl.getInstance().getLocalGame(game.getUuid()).filter(g -> g == game).isPresent();
        if (!registered) {
            player.sendMessage(Message.of(ForkLangKeys.SETUP_CANCELLED_NEW).defaultPrefix().placeholderRaw("arena", game.getName()));
            return;
        }

        var file = game.getFile();
        if (file == null || !file.exists()) {
            game.start(); // keeps the in-memory state
            player.sendMessage(Message.of(ForkLangKeys.SETUP_CANCEL_RELOAD_FAILED).defaultPrefix().placeholderRaw("arena", game.getName()));
            return;
        }
        GameManagerImpl.getInstance().removeGame(game); // loadGame refuses a duplicate UUID
        LocalGameLoaderImpl.getInstance().loadGame(file, false).thenAccept(loaded -> {
            if (loaded != null) {
                GameManagerImpl.getInstance().addGame(loaded);
                player.sendMessage(Message.of(ForkLangKeys.SETUP_CANCELLED_RELOADED).defaultPrefix().placeholderRaw("arena", loaded.getName()));
            } else {
                // the file could not be read: keep the arena as it is in memory
                GameManagerImpl.getInstance().addGame(game);
                game.start();
                player.sendMessage(Message.of(ForkLangKeys.SETUP_CANCEL_RELOAD_FAILED).defaultPrefix().placeholderRaw("arena", game.getName()));
            }
        });
    }
}
