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
import cloud.commandframework.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.commands.AdminCommand;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.game.LocalGameLoaderImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.spectator.event.ClickEvent;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.ArrayList;

@Service
public class SaveCommand extends BaseAdminSubCommand {
    public SaveCommand() {
        super("save");
    }

    @Override
    public void construct(CommandManager<CommandSender> manager, Command.Builder<CommandSender> commandSenderWrapperBuilder) {
        manager.command(
                commandSenderWrapperBuilder
                        .handler(ctx -> save(ctx, false))
        );

        manager.command(
                commandSenderWrapperBuilder
                        .literal("force")
                        .handler(ctx -> save(ctx, true))
        );
    }

    private void save(@NotNull CommandContext<CommandSender> commandContext, boolean force) {
        editMode(commandContext, (sender, game) ->
                saveArena(sender, game, force, "/" + commandContext.getRawInputJoined() + " force"));
    }

    /**
     * Validates, saves, registers and starts an arena that is in edit mode. Returns true when saved.
     *
     * @param force        skip the warnings (the SEVERE checks always apply)
     * @param forceCommand command shown (clickable) when only warnings prevent saving
     */
    public static boolean saveArena(@NotNull CommandSender sender, @NotNull GameImpl game, boolean force, @NotNull String forceCommand) {
        // SEVERE (currently)
        for (var team : game.getTeams()) {
            if (team.getTarget() == null) {
                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_SET_TARGET_BLOCK_FOR_TEAM_BEFORE_SAVE).defaultPrefix().placeholder("team", team.getName()));
                return false;
            } else if (team.getTeamSpawns().isEmpty()) {
                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_SET_SPAWN_FOR_TEAM_BEFORE_SAVE).defaultPrefix().placeholder("team", team.getName()));
                return false;
            }
        }
        if (game.getTeams().size() < 2) {
            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_NEED_2_TEAMS).defaultPrefix());
            return false;
        } else if (game.getPos1() == null || game.getPos2() == null) {
            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_SET_BOUNDS_BEFORE_SAVE).defaultPrefix());
            return false;
        } else if (game.getLobbySpawn() == null) {
            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_SET_LOBBY_BEFORE_SAVE).defaultPrefix());
            return false;
        } else if (game.getSpecSpawn() == null) {
            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_SET_SPEC_BEFORE_SAVE).defaultPrefix());
            return false;
        }

        // WARNINGS
        var warnings = new ArrayList<Message>();
        if (!force) {
            if (game.getGameStoreList().isEmpty()) {
                warnings.add(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_MISSING_STORES).defaultPrefix());
            } else if ((game.getGameStoreList().size() % game.getTeams().size()) != 0) {
                warnings.add(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_WEIRD_STORE_COUNT).placeholder("count", game.getGameStoreList().size()).defaultPrefix());
            }
            if (game.getSpawners().isEmpty()) {
                warnings.add(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_MISSING_SPAWNERS).defaultPrefix());
            }
        }

        if (warnings.isEmpty()) {
            // same result a reload gives; also keeps the saved file clean
            relinkTeamReferences(game, null);
            LocalGameLoaderImpl.getInstance().saveGame(game);
            GameManagerImpl.getInstance().addGame(game);
            game.start();
            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_SUCCESS_SAVED_AND_STARTED).placeholderRaw("game", game.getName()).defaultPrefix());
            AdminCommand.gc.remove(game.getName());
            return true;
        }

        warnings.forEach(sender::sendMessage);
        sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_SKIP_WARNINGS)
                .placeholderRaw("game", game.getName())
                .placeholder("command", Component.text()
                        .content(forceCommand)
                        .hoverEvent(Message.of(LangKeys.ADMIN_INFO_SELECT_CLICK)
                                .placeholderRaw("command", forceCommand)
                                .asComponent(sender))
                        .clickEvent(ClickEvent.runCommand(forceCommand)).build())
                .defaultPrefix());
        return false;
    }

    /**
     * Re-links generators and shops whose team object is no longer part of the arena (team removed, or removed and added
     * again, which creates a new object). Links are compared by identity everywhere at runtime, so a stale object would
     * disable the generator or lock the shop. The team is looked up by name; the link is dropped when it does not exist.
     *
     * @param onlyTeamName only repair links to a team with this name (null: all stale links)
     */
    public static void relinkTeamReferences(@NotNull GameImpl game, @Nullable String onlyTeamName) {
        for (var spawner : game.getSpawners()) {
            var linked = spawner.getTeam();
            if (isStaleLink(game, linked, onlyTeamName)) {
                spawner.setTeam(game.getTeamFromName(linked.getName())); // TeamImpl overload, null if the team is gone
            }
        }
        for (var store : game.getGameStoreList()) {
            var linked = store.getTeam();
            if (isStaleLink(game, linked, onlyTeamName)) {
                store.setTeam(game.getTeamFromName(linked.getName()));
            }
        }
    }

    private static boolean isStaleLink(@NotNull GameImpl game, @Nullable TeamImpl linked, @Nullable String onlyTeamName) {
        // contains() is identity based (TeamImpl has no equals), which is exactly what is needed here
        return linked != null
                && !game.getTeams().contains(linked)
                && (onlyTeamName == null || linked.getName().equalsIgnoreCase(onlyTeamName));
    }
}
