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
import cloud.commandframework.arguments.standard.StringArgument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.commands.AdminCommand;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.setup.clone.ArenaCloneService;
import org.screamingsandals.bedwars.variants.VariantImpl;
import org.screamingsandals.bedwars.variants.VariantManagerImpl;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.UUID;

@Service
public class AddCommand extends BaseAdminSubCommand {
    public AddCommand() {
        super("add");
    }

    @Override
    public void construct(CommandManager<CommandSender> manager, Command.Builder<CommandSender> commandSenderWrapperBuilder) {
        manager.command(
                commandSenderWrapperBuilder
                        .argument(StringArgument
                                .<CommandSender>newBuilder("variant")
                                .withSuggestionsProvider((c, s) -> VariantManagerImpl.getInstance().getVariantNames())
                                .asOptional()
                        )
                        .handler(commandContext -> createArena(commandContext.getSender(),
                                commandContext.get("game"), commandContext.<String>getOptional("variant").orElse(null)))
        );
    }

    /**
     * Creates a new arena in edit mode. Returns null (after sending the error) when it cannot be created.
     */
    public static @Nullable GameImpl createArena(@NotNull CommandSender sender, @NotNull String gameName, @Nullable String variantName) {
        if (isUuid(gameName)) { // an arena named like a UUID could never be found again (GameManagerImpl.getGame(String) parses UUIDs first)
            sender.sendMessage(Message.of(ForkLangKeys.SETUP_INVALID_NAME).placeholderRaw("arena", gameName).defaultPrefix());
            return null;
        }
        if (GameManagerImpl.getInstance().hasGame(gameName)) {
            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_ERROR_ALREADY_EXISTS).defaultPrefix());
            return null;
        } else if (AdminCommand.gc.containsKey(gameName) || ArenaCloneService.getInstance().isNameReserved(gameName)) {
            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_ERROR_ALREADY_WORKING_ON_IT).defaultPrefix());
            return null;
        }

        VariantImpl variantObj = null;
        if (variantName != null) {
            variantObj = VariantManagerImpl.getInstance().getVariant(variantName);
            if (variantObj == null) {
                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_INVALID_VARIANT)
                        .placeholder("variant", variantName).defaultPrefix());
                return null;
            }
        }

        var creator = GameImpl.createGame(gameName);
        AdminCommand.gc.put(gameName, creator);

        if (variantObj != null) {
            creator.setGameVariant(variantObj);
            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_SUCCESS_ADDED_WITH_VARIANT)
                    .placeholder("arena", gameName)
                    .placeholder("variant", variantObj.getName())
                    .defaultPrefix());
        } else {
            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_SUCCESS_ADDED)
                    .placeholder("arena", gameName)
                    .defaultPrefix());
        }
        return creator;
    }

    private static boolean isUuid(@NotNull String s) {
        try {
            UUID.fromString(s); // same test as GameManagerImpl.getGame(String)
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
