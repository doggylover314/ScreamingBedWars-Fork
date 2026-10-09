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
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.api.config.ConfigurationKey;
import org.screamingsandals.bedwars.commands.admin.LobbyCommand;
import org.screamingsandals.bedwars.commands.admin.LobbyPos1Command;
import org.screamingsandals.bedwars.commands.admin.LobbyPos2Command;
import org.screamingsandals.bedwars.commands.admin.Pos1Command;
import org.screamingsandals.bedwars.commands.admin.Pos2Command;
import org.screamingsandals.bedwars.commands.admin.SpawnerCommand;
import org.screamingsandals.bedwars.commands.admin.SpecCommand;
import org.screamingsandals.bedwars.commands.admin.StoreCommand;
import org.screamingsandals.bedwars.commands.admin.TeamCommand;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.ItemSpawnerImpl;
import org.screamingsandals.bedwars.game.TeamColorImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.setup.SetupConfigKeys;
import org.screamingsandals.bedwars.setup.SetupOperations;
import org.screamingsandals.bedwars.setup.SetupSessionService;
import org.screamingsandals.bedwars.setup.TeamNaming;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * {@code /bw set <thing>}: sets one part of the arena selected with {@code /bw setup} at the admin's position.
 * Every handler delegates to the static operations of the {@code /bw admin} sub-commands, so validation and
 * messages are the same as there; afterwards the next missing step is shown.
 */
@Service
public class SetCommand extends BaseCommand {
    public SetCommand() {
        super("set", BedWarsPermission.ADMIN_PERMISSION, false);
    }

    @Override
    protected void construct(Command.Builder<CommandSender> b, CommandManager<CommandSender> manager) {
        // position-only things
        manager.command(b.literal("pos1").handler(ctx -> run(ctx, (p, g) ->
                Pos1Command.setPos1(p, g, p.getLocation(), false, "/bw set pos1 force"))));
        manager.command(b.literal("pos1").literal("force").handler(ctx -> run(ctx, (p, g) ->
                Pos1Command.setPos1(p, g, p.getLocation(), true, "/bw set pos1 force"))));
        manager.command(b.literal("pos2").handler(ctx -> run(ctx, (p, g) ->
                Pos2Command.setPos2(p, g, p.getLocation(), false, "/bw set pos2 force"))));
        manager.command(b.literal("pos2").literal("force").handler(ctx -> run(ctx, (p, g) ->
                Pos2Command.setPos2(p, g, p.getLocation(), true, "/bw set pos2 force"))));
        manager.command(b.literal("lobby").handler(ctx -> run(ctx, (p, g) ->
                LobbyCommand.setLobbySpawn(p, g, SetupOperations.snap(p.getLocation())))));
        manager.command(b.literal("lobbypos1").handler(ctx -> run(ctx, (p, g) ->
                LobbyPos1Command.setLobbyPos1(p, g, p.getLocation()))));
        manager.command(b.literal("lobbypos2").handler(ctx -> run(ctx, (p, g) ->
                LobbyPos2Command.setLobbyPos2(p, g, p.getLocation()))));
        manager.command(b.literal("spectator", "spec").handler(ctx -> run(ctx, (p, g) ->
                SpecCommand.setSpecSpawn(p, g, SetupOperations.snap(p.getLocation())))));

        // teams
        manager.command(b.literal("team")
                .argument(StringArgument.<CommandSender>newBuilder("color").withSuggestionsProvider(this::suggestFreeColors))
                .argument(IntegerArgument.<CommandSender>newBuilder("size").withMin(1).asOptional())
                .handler(ctx -> run(ctx, (p, g) -> addTeam(ctx, p, g))));
        manager.command(b.literal("spawn").argument(teamArg()).handler(ctx -> run(ctx, (p, g) ->
                TeamCommand.setTeamSpawn(p, g, ctx.get("team"), SetupOperations.snap(p.getLocation())))));
        manager.command(b.literal("bed").argument(teamArg()).handler(ctx -> run(ctx, (p, g) -> setTarget(ctx, p, g, true))));
        manager.command(b.literal("target").argument(teamArg()).handler(ctx -> run(ctx, (p, g) -> setTarget(ctx, p, g, false))));

        // spawners
        manager.command(b.literal("generator").argument(teamArg()).handler(ctx -> run(ctx, (p, g) -> setGenerator(ctx, p, g, true))));
        manager.command(b.literal("generator").argument(teamArg()).literal("add").handler(ctx -> run(ctx, (p, g) -> setGenerator(ctx, p, g, false))));
        manager.command(b.literal("diamond").handler(ctx -> run(ctx, (p, g) ->
                addResourceSpawner(p, g, SetupConfigKeys.DIAMOND_SPAWNER_TYPE, "diamond"))));
        manager.command(b.literal("emerald").handler(ctx -> run(ctx, (p, g) ->
                addResourceSpawner(p, g, SetupConfigKeys.EMERALD_SPAWNER_TYPE, "emerald"))));

        // stores
        manager.command(b.literal("shop").argument(teamArg().asOptional()).handler(ctx -> run(ctx, (p, g) -> addShop(ctx, p, g, false))));
        manager.command(b.literal("upgrades").argument(teamArg().asOptional()).handler(ctx -> run(ctx, (p, g) -> addShop(ctx, p, g, true))));
    }

    /**
     * Team argument with the names of the selected arena's teams.
     */
    private StringArgument.Builder<CommandSender> teamArg() {
        return StringArgument.<CommandSender>newBuilder("team").withSuggestionsProvider((c, s) -> {
            var game = SetupSessionService.getInstance().getSelectedGame(c.getSender().as(Player.class));
            return game == null ? List.of() : game.getTeams().stream().map(TeamImpl::getName).collect(Collectors.toList());
        });
    }

    private List<String> suggestFreeColors(CommandContext<CommandSender> c, String input) {
        var game = SetupSessionService.getInstance().getSelectedGame(c.getSender().as(Player.class));
        var all = new ArrayList<String>();
        for (var color : TeamColorImpl.values()) {
            all.add(color.name());
        }
        var used = new ArrayList<TeamColorImpl>();
        if (game != null) {
            game.getTeams().forEach(t -> used.add(t.getColor()));
        }
        return TeamNaming.orderByPreference(all).stream()
                .filter(name -> used.stream().noneMatch(u -> u.name().equals(name)))
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    /**
     * Resolves the selected arena (it must still be in edit mode), runs the action and, on success, shows the next step.
     */
    private void run(CommandContext<CommandSender> ctx, BiPredicate<Player, GameImpl> action) {
        var player = ctx.getSender().as(Player.class);
        var game = SetupOperations.requireSelectedInEdit(player); // sends SETUP_NO_SELECTION / SETUP_NOT_IN_EDIT itself
        if (game == null) {
            return;
        }
        if (action.test(player, game) && SetupConfigKeys.showNextStepHint()) {
            SetupOperations.sendNextStep(player, game);
        }
    }

    private boolean addTeam(CommandContext<CommandSender> ctx, Player p, GameImpl g) {
        String raw = ctx.get("color");
        TeamColorImpl color;
        try {
            color = TeamColorImpl.valueOf(raw.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            p.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_INVALID_COLOR).defaultPrefix());
            return false;
        }
        var used = g.getTeams().stream().filter(t -> t.getColor() == color).findFirst();
        if (used.isPresent()) {
            p.sendMessage(Message.of(ForkLangKeys.SETUP_SET_TEAM_COLOR_USED)
                    .defaultPrefix()
                    .placeholderRaw("team", used.get().getName())
                    .placeholder("teamcolor", Component.text(color.name(), color.getTextColor())));
            return false;
        }
        int size = ctx.<Integer>getOptional("size").orElse(SetupConfigKeys.defaultTeamSize());
        return TeamCommand.addTeam(p, g, TeamNaming.defaultName(color.name()), color, size) != null;
    }

    private boolean setTarget(CommandContext<CommandSender> ctx, Player p, GameImpl g, boolean bedOnly) {
        var block = SetupOperations.findTargetBlock(p, bedOnly);
        if (block == null) {
            p.sendMessage(Message.of(bedOnly ? ForkLangKeys.SETUP_SET_NO_BED_FOUND : ForkLangKeys.SETUP_SET_NO_TARGET_BLOCK).defaultPrefix());
            return false;
        }
        return TeamCommand.setTargetBlock(p, g, ctx.get("team"), block, null); // stores the bed head / door lower half
    }

    private boolean setGenerator(CommandContext<CommandSender> ctx, Player p, GameImpl g, boolean replace) {
        var team = g.getTeamFromName(ctx.get("team"));
        if (team == null) {
            p.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_TEAM_DOES_NOT_EXIST).defaultPrefix());
            return false;
        }
        var cc = g.getConfigurationContainer();
        List<String> types = cc.getOrDefault(SetupConfigKeys.TEAM_GENERATOR_TYPES, SetupConfigKeys.DEFAULT_TEAM_GENERATOR_TYPES)
                .stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
        boolean holo = cc.getOrDefault(SetupConfigKeys.TEAM_GENERATOR_HOLOGRAM, true);
        var loc = SetupOperations.snap(p.getLocation());

        // add first, so that a failed add removes nothing
        var created = new ArrayList<ItemSpawnerImpl>();
        for (var type : types) { // each failing type sends its own error (invalid spawner type / bounds)
            var spawner = SpawnerCommand.addSpawner(p, g, type, loc, team);
            if (spawner == null) {
                if (created.isEmpty()) {
                    return false; // a bounds error would repeat for every type - stop at the first failure
                }
                continue; // later types fail typically because the variant does not define them
            }
            spawner.setHologramEnabled(holo);
            created.add(spawner);
        }
        if (created.isEmpty()) {
            return false;
        }
        if (replace) { // "set" semantics: remove the team's previous generators of the configured types
            var old = g.getSpawners().stream()
                    .filter(s -> s.getTeam() == team && !created.contains(s)
                            && types.contains(s.getItemSpawnerType().configKey().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
            g.getSpawners().removeAll(old);
            if (!old.isEmpty()) {
                p.sendMessage(Message.of(ForkLangKeys.SETUP_SET_GENERATORS_REPLACED)
                        .defaultPrefix()
                        .placeholder("count", old.size())
                        .placeholderRaw("team", team.getName()));
            }
        }
        return true;
    }

    private boolean addResourceSpawner(Player p, GameImpl g, @NotNull ConfigurationKey<String> key, String def) {
        String type = g.getConfigurationContainer().getOrDefault(key, def).toLowerCase(Locale.ROOT);
        var loc = SetupOperations.snap(p.getLocation());
        boolean exists = g.getSpawners().stream().anyMatch(s -> s.getItemSpawnerType().configKey().equalsIgnoreCase(type)
                && s.getLocation().getWorld().equals(loc.getWorld())
                && s.getLocation().getBlockX() == loc.getBlockX()
                && s.getLocation().getBlockY() == loc.getBlockY()
                && s.getLocation().getBlockZ() == loc.getBlockZ());
        if (exists) {
            p.sendMessage(Message.of(ForkLangKeys.SETUP_SET_SPAWNER_ALREADY_HERE).defaultPrefix().placeholderRaw("type", type));
            return false;
        }
        return SpawnerCommand.addSpawner(p, g, type, loc, null) != null;
    }

    private boolean addShop(CommandContext<CommandSender> ctx, Player p, GameImpl g, boolean upgrades) {
        TeamImpl team = null;
        var teamName = ctx.<String>getOptional("team");
        if (teamName.isPresent()) {
            team = g.getTeamFromName(teamName.get());
            if (team == null) {
                p.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_TEAM_DOES_NOT_EXIST).defaultPrefix());
                return false;
            }
        }
        String file;
        if (upgrades) {
            file = SetupOperations.resolveUpgradeShopFile(g);
            if (file == null) {
                p.sendMessage(Message.of(ForkLangKeys.SETUP_SET_UPGRADE_SHOP_FILE_MISSING)
                        .defaultPrefix()
                        .placeholderRaw("variant", g.getGameVariant() == null ? "-" : g.getGameVariant().getName())
                        .placeholderRaw("arena", g.getName()));
                return false;
            }
        } else {
            file = SetupOperations.resolveShopFile(g); // null = no own file: the default shop file applies at runtime
        }
        var loc = SetupOperations.snap(p.getLocation());
        var store = StoreCommand.addStore(p, g, loc); // bounds/world/duplicate checks + the "store added" message
        if (store == null) {
            return false;
        }
        if (file != null) {
            store.setShopFile(file);
            p.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_SUCCESS_STORE_FILE_SET)
                    .defaultPrefix()
                    .placeholder("x", loc.getX(), 2)
                    .placeholder("y", loc.getY(), 2)
                    .placeholder("z", loc.getZ(), 2)
                    .placeholderRaw("file", file));
        }
        if (team != null) {
            store.setTeam(team);
            p.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_SUCCESS_STORE_TEAM_SET)
                    .defaultPrefix()
                    .placeholder("x", loc.getX(), 2)
                    .placeholder("y", loc.getY(), 2)
                    .placeholder("z", loc.getZ(), 2)
                    .placeholderRaw("team", team.getName()));
        }
        return true;
    }
}
