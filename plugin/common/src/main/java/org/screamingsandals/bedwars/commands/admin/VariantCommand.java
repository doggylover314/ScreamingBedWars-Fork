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
import cloud.commandframework.arguments.standard.StringArgument;
import org.screamingsandals.bedwars.setup.SetupOperations;
import org.screamingsandals.bedwars.variants.VariantManagerImpl;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.utils.annotations.Service;

/**
 * {@code /bw admin <arena> variant <variant>}: changes the variant of an arena in edit mode.
 */
@Service
public class VariantCommand extends BaseAdminSubCommand {
    public VariantCommand() {
        super("variant");
    }

    @Override
    public void construct(CommandManager<CommandSender> manager, Command.Builder<CommandSender> commandSenderWrapperBuilder) {
        manager.command(
                commandSenderWrapperBuilder
                        .argument(StringArgument.<CommandSender>newBuilder("variant")
                                .withSuggestionsProvider((c, s) -> VariantManagerImpl.getInstance().getVariantNames()))
                        .handler(ctx -> editMode(ctx, (sender, game) ->
                                SetupOperations.changeVariant(sender, game, ctx.get("variant"))))
        );
    }
}
