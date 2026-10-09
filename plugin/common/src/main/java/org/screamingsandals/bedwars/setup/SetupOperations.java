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

package org.screamingsandals.bedwars.setup;

import cloud.commandframework.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.commands.AdminCommand;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.game.TeamColorImpl;
import org.screamingsandals.bedwars.game.target.ExpirableTargetBlockImpl;
import org.screamingsandals.bedwars.game.target.ExpirableTargetImpl;
import org.screamingsandals.bedwars.game.target.NoTargetImpl;
import org.screamingsandals.bedwars.game.target.TargetBlockImpl;
import org.screamingsandals.bedwars.inventories.ShopInventory;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.setup.ArenaSetupSnapshot.SpawnerInfo;
import org.screamingsandals.bedwars.setup.ArenaSetupSnapshot.StoreInfo;
import org.screamingsandals.bedwars.setup.ArenaSetupSnapshot.TargetKind;
import org.screamingsandals.bedwars.setup.ArenaSetupSnapshot.TeamInfo;
import org.screamingsandals.bedwars.setup.SetupChecklist.Entry;
import org.screamingsandals.bedwars.utils.BedUtils;
import org.screamingsandals.bedwars.variants.VariantManagerImpl;
import org.screamingsandals.lib.block.BlockPlacement;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.spectator.event.ClickEvent;
import org.screamingsandals.lib.spectator.event.HoverEvent;
import org.screamingsandals.lib.world.Location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Runtime helpers of {@code /bw setup} and {@code /bw set} (platform-bound, not unit-tested; the logic that can be
 * tested lives in {@link SetupChecklist}, {@link TeamNaming} and {@link ArenaNames}).
 */
public final class SetupOperations {
    private SetupOperations() {
    }

    // ---------------------------------------------------------------- selection

    /**
     * The arena selected by the player if it is still in edit mode. Otherwise sends the matching error
     * ({@code SETUP_NO_SELECTION} / {@code SETUP_NOT_IN_EDIT}) and returns null.
     */
    public static @Nullable GameImpl requireSelectedInEdit(@NotNull Player p) {
        var name = SetupSessionService.getInstance().getSelectedName(p);
        if (name == null) {
            p.sendMessage(Message.of(ForkLangKeys.SETUP_NO_SELECTION).defaultPrefix());
            return null;
        }
        var game = AdminCommand.gc.get(name);
        if (game == null) {
            p.sendMessage(Message.of(ForkLangKeys.SETUP_NOT_IN_EDIT)
                    .defaultPrefix()
                    .placeholderRaw("arena", name)
                    .placeholder("command", clickable(p, "/bw setup " + name, true)));
            return null;
        }
        return game;
    }

    public static void withSelected(@NotNull CommandContext<CommandSender> ctx, @NotNull BiConsumer<Player, GameImpl> handler) {
        var player = ctx.getSender().as(Player.class);
        var game = requireSelectedInEdit(player);
        if (game != null) {
            handler.accept(player, game);
        }
    }

    /**
     * The selected arena for {@code /bw setup clone}: the arena in edit mode, else the registered arena of that name.
     * Sends {@code SETUP_NO_SELECTION} and returns null when nothing usable is selected.
     */
    public static @Nullable GameImpl selectedSourceForClone(@NotNull Player p) {
        var name = SetupSessionService.getInstance().getSelectedName(p);
        GameImpl game = null;
        if (name != null) {
            game = AdminCommand.gc.get(name);
            if (game == null) {
                game = GameManagerImpl.getInstance().getLocalGame(name).orElse(null);
            }
        }
        if (game == null) {
            p.sendMessage(Message.of(ForkLangKeys.SETUP_NO_SELECTION).defaultPrefix());
        }
        return game;
    }

    // ---------------------------------------------------------------- locations and files

    /**
     * Centres X/Z on the block when {@code setup.snap-to-block-center} is on (Y and rotation are kept).
     * NOT used for pos1/pos2/lobbypos (block bounds keep the upstream semantics).
     */
    public static @NotNull Location snap(@NotNull Location l) {
        if (SetupConfigKeys.snapToBlockCenter()) {
            return l.withX(l.getBlockX() + 0.5).withZ(l.getBlockZ() + 0.5);
        }
        return l;
    }

    /**
     * The shop file for new item shops ({@code setup.shop-file}), null = none (the default shop file applies at runtime).
     */
    public static @Nullable String resolveShopFile(@NotNull GameImpl g) {
        var value = g.getConfigurationContainer().getOrDefault(SetupConfigKeys.SHOP_FILE, "");
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * The shop file for new upgrade shops: {@code setup.upgrade-shop-file} if set, else the convention
     * {@code <variant>/upgrade-shop.yml} when that file exists, else null.
     */
    public static @Nullable String resolveUpgradeShopFile(@NotNull GameImpl g) {
        var configured = g.getConfigurationContainer().getOrDefault(SetupConfigKeys.UPGRADE_SHOP_FILE, "");
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        var variant = g.getGameVariant();
        if (variant != null) {
            var conventional = variant.getName() + "/upgrade-shop.yml";
            if (ShopInventory.getInstance().normalizeShopFile(conventional).exists()) {
                return conventional;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- checklist adapter and output

    /**
     * Adapter from the live arena to the pure checklist input.
     */
    public static @NotNull ArenaSetupSnapshot snapshot(@NotNull GameImpl g) {
        var cc = g.getConfigurationContainer();
        var variant = g.getGameVariant();

        var teams = new ArrayList<TeamInfo>();
        for (var t : g.getTeams()) {
            teams.add(new TeamInfo(t.getName(), t.getColor().name(), t.getConfiguredMaxPlayers(), t.getTeamSpawns().size(), targetKind(t.getTarget())));
        }
        var spawners = new ArrayList<SpawnerInfo>();
        for (var s : g.getSpawners()) {
            spawners.add(new SpawnerInfo(s.getItemSpawnerType().configKey().toLowerCase(Locale.ROOT),
                    s.getTeam() == null ? null : s.getTeam().getName()));
        }
        var stores = new ArrayList<StoreInfo>();
        for (var st : g.getGameStoreList()) {
            stores.add(new StoreInfo(st.getShopFile(), st.getTeam() == null ? null : st.getTeam().getName()));
        }
        var variantTypes = new HashSet<String>();
        if (variant != null) {
            for (var n : variant.getItemSpawnerTypeNames()) {
                variantTypes.add(n.toLowerCase(Locale.ROOT));
            }
        }
        var colors = new ArrayList<String>();
        for (var c : TeamColorImpl.values()) {
            colors.add(c.name());
        }

        return new ArenaSetupSnapshot(
                g.getName(),
                variant == null ? "-" : variant.getName(),
                g.getPos1() != null,
                g.getPos2() != null,
                g.getLobbySpawn() != null,
                g.getLobbyPos1() != null,
                g.getLobbyPos2() != null,
                g.getSpecSpawn() != null,
                teams,
                spawners,
                stores,
                variant != null && !variant.getUpgrades().isEmpty(),
                variantTypes,
                cc.getOrDefault(SetupConfigKeys.DIAMOND_SPAWNER_TYPE, "diamond").toLowerCase(Locale.ROOT),
                cc.getOrDefault(SetupConfigKeys.EMERALD_SPAWNER_TYPE, "emerald").toLowerCase(Locale.ROOT),
                resolveUpgradeShopFile(g),
                colors
        );
    }

    private static @NotNull TargetKind targetKind(@Nullable Object target) {
        if (target == null) {
            return TargetKind.NONE_SET;
        }
        if (target instanceof ExpirableTargetBlockImpl) { // before TargetBlockImpl: it is a subclass
            return TargetKind.BLOCK_COUNTDOWN;
        }
        if (target instanceof TargetBlockImpl) {
            return TargetKind.BLOCK;
        }
        if (target instanceof ExpirableTargetImpl) {
            return TargetKind.COUNTDOWN;
        }
        if (target instanceof NoTargetImpl) {
            return TargetKind.NO_TARGET;
        }
        return TargetKind.BLOCK;
    }

    /**
     * Sends the status: header, every row that is not done, and the closing hint.
     */
    public static void sendStatus(@NotNull Player p, @NotNull GameImpl g) {
        var entries = SetupChecklist.evaluate(snapshot(g));
        p.sendMessage(Message.of(ForkLangKeys.SETUP_STATUS_HEADER)
                .defaultPrefix()
                .placeholderRaw("arena", g.getName())
                .placeholderRaw("variant", g.getGameVariant() == null ? "-" : g.getGameVariant().getName())
                .placeholder("done", SetupChecklist.requiredDone(entries))
                .placeholder("total", SetupChecklist.requiredTotal(entries)));
        for (var e : entries) {
            if (!e.done()) {
                sendEntry(p, e);
            }
        }
        // optional rows never keep the arena from being "set up"
        boolean everythingDone = entries.stream().allMatch(e -> e.done() || e.severity() == SetupChecklist.Severity.OPTIONAL);
        if (everythingDone) {
            p.sendMessage(Message.of(ForkLangKeys.SETUP_STATUS_EVERYTHING_DONE));
        }
        if (SetupChecklist.isSaveable(entries)) {
            p.sendMessage(Message.of(ForkLangKeys.SETUP_ALL_REQUIRED_DONE)
                    .placeholder("command", clickable(p, "/bw setup save", true)));
        }
    }

    public static void sendEntry(@NotNull Player p, @NotNull Entry e) {
        String[] key;
        switch (e.severity()) {
            case REQUIRED:
                key = ForkLangKeys.SETUP_STATUS_MISSING;
                break;
            case RECOMMENDED:
                key = ForkLangKeys.SETUP_STATUS_RECOMMENDED;
                break;
            default:
                key = ForkLangKeys.SETUP_STATUS_OPTIONAL;
                break;
        }
        p.sendMessage(Message.of(key)
                .placeholder("item", itemLabel(e))
                .placeholder("command", clickable(p, e.command(), false)));
    }

    /**
     * Prints the next missing step (or the save hint when nothing important is missing).
     */
    public static void sendNextStep(@NotNull Player p, @NotNull GameImpl g) {
        var entries = SetupChecklist.evaluate(snapshot(g));
        var next = SetupChecklist.nextStep(entries);
        if (next.isPresent()) {
            p.sendMessage(Message.of(ForkLangKeys.SETUP_NEXT_STEP)
                    .placeholder("item", itemLabel(next.get()))
                    .placeholder("command", clickable(p, next.get().command(), false)));
        } else {
            p.sendMessage(Message.of(ForkLangKeys.SETUP_ALL_REQUIRED_DONE)
                    .placeholder("command", clickable(p, "/bw setup save", true)));
        }
    }

    /**
     * Human readable label of a checklist row ({@code SETUP_ITEM_*}), with the team and count placeholders filled.
     */
    public static @NotNull Message itemLabel(@NotNull Entry e) {
        String[] key;
        switch (e.item()) {
            case BOUNDS:
                key = ForkLangKeys.SETUP_ITEM_BOUNDS;
                break;
            case LOBBY:
                key = ForkLangKeys.SETUP_ITEM_LOBBY;
                break;
            case SPECTATOR:
                key = ForkLangKeys.SETUP_ITEM_SPECTATOR;
                break;
            case TEAMS:
                key = ForkLangKeys.SETUP_ITEM_TEAMS;
                break;
            case TEAM_SPAWN:
                key = ForkLangKeys.SETUP_ITEM_TEAM_SPAWN;
                break;
            case TEAM_TARGET:
                key = ForkLangKeys.SETUP_ITEM_TEAM_TARGET;
                break;
            case TEAM_GENERATOR:
                key = ForkLangKeys.SETUP_ITEM_TEAM_GENERATOR;
                break;
            case TEAM_SHOP:
                key = ForkLangKeys.SETUP_ITEM_TEAM_SHOP;
                break;
            case TEAM_UPGRADES:
                key = ForkLangKeys.SETUP_ITEM_TEAM_UPGRADES;
                break;
            case SPAWNERS:
                key = ForkLangKeys.SETUP_ITEM_SPAWNERS;
                break;
            case SHOPS:
                key = ForkLangKeys.SETUP_ITEM_SHOPS;
                break;
            case STORE_COUNT:
                key = ForkLangKeys.SETUP_ITEM_STORE_COUNT;
                break;
            case UPGRADE_SHOPS:
                key = ForkLangKeys.SETUP_ITEM_UPGRADE_SHOPS;
                break;
            case DIAMOND:
                key = ForkLangKeys.SETUP_ITEM_DIAMOND;
                break;
            case EMERALD:
                key = ForkLangKeys.SETUP_ITEM_EMERALD;
                break;
            case LOBBY_REGION:
            default:
                key = ForkLangKeys.SETUP_ITEM_LOBBY_REGION;
                break;
        }
        var message = Message.of(key).placeholder("count", e.count());
        if (e.team() != null) {
            message = message.placeholderRaw("team", e.team());
        }
        return message;
    }

    /**
     * A clickable command text: runs the command ({@code run}) or puts it into the chat box (location-based steps:
     * the admin walks there first).
     */
    public static @NotNull Component clickable(@NotNull CommandSender s, @NotNull String cmd, boolean run) {
        var hover = (run ? Message.of(LangKeys.ADMIN_INFO_SELECT_CLICK) : Message.of(ForkLangKeys.SETUP_CLICK_TO_SUGGEST))
                .placeholderRaw("command", cmd)
                .asComponent(s);
        return Component.text()
                .content(cmd)
                .hoverEvent(HoverEvent.showText(hover))
                .clickEvent(run ? ClickEvent.runCommand(cmd) : ClickEvent.suggestCommand(cmd))
                .build();
    }

    // ---------------------------------------------------------------- variant

    /**
     * Changes the variant of an arena in edit mode and warns about spawners whose type the new variant lacks.
     */
    public static boolean changeVariant(@NotNull CommandSender s, @NotNull GameImpl g, @NotNull String variantName) {
        var variant = VariantManagerImpl.getInstance().getVariant(variantName);
        if (variant == null) {
            s.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_INVALID_VARIANT).placeholder("variant", variantName).defaultPrefix());
            return false;
        }
        var old = g.getGameVariant();
        if (old != null && old.getName().equals(variant.getName())) {
            s.sendMessage(Message.of(ForkLangKeys.SETUP_VARIANT_UNCHANGED)
                    .defaultPrefix()
                    .placeholderRaw("arena", g.getName())
                    .placeholderRaw("variant", variant.getName()));
            return true;
        }
        g.setGameVariant(variant); // re-parents the arena config container
        s.sendMessage(Message.of(ForkLangKeys.SETUP_VARIANT_CHANGED)
                .defaultPrefix()
                .placeholderRaw("arena", g.getName())
                .placeholderRaw("old", old == null ? "-" : old.getName())
                .placeholderRaw("new", variant.getName()));

        Map<String, Long> unknown = new LinkedHashMap<>();
        for (var sp : g.getSpawners()) {
            var type = sp.getItemSpawnerType().configKey();
            if (variant.getItemSpawnerType(type) == null) {
                unknown.merge(type, 1L, Long::sum);
            }
        }
        unknown.forEach((type, count) -> s.sendMessage(Message.of(ForkLangKeys.SETUP_VARIANT_UNKNOWN_SPAWNER_TYPE)
                .defaultPrefix()
                .placeholder("count", count)
                .placeholderRaw("type", type)
                .placeholderRaw("variant", variant.getName())
                .placeholderRaw("arena", g.getName())));
        return true;
    }

    // ---------------------------------------------------------------- target block

    /**
     * Finds the block a {@code /bw set bed} / {@code /bw set target} refers to.
     * Beds: the bed at the feet / below / looked at; then respawn anchors and cakes when {@code bedOnly}.
     * Other targets: the looked-at block, else the block stood on.
     */
    public static @Nullable BlockPlacement findTargetBlock(@NotNull Player p, boolean bedOnly) {
        var feet = p.getLocation().getBlock();                       // a bed is 9/16 high: standing on it puts the feet in the bed block
        var below = p.getLocation().subtract(0, 0.5, 0).getBlock();  // = TeamCommand STANDING_ON
        var looked = p.getTargetBlock(5);
        if (looked != null && looked.isEmpty()) {
            looked = null;
        }

        var candidates = new ArrayList<BlockPlacement>(); // not List.of: nulls
        candidates.add(feet);
        candidates.add(below);
        if (looked != null) {
            candidates.add(looked);
        }
        for (var c : candidates) {
            if (BedUtils.isBedBlock(c)) {
                return c;
            }
        }
        if (bedOnly) {
            if (looked != null && looked.block().isSameType("respawn_anchor", "cake")) {
                return looked;
            }
            if (below.block().isSameType("respawn_anchor", "cake")) {
                return below;
            }
            return null;
        }
        if (looked != null && !looked.block().isAir()) {
            return looked;
        }
        if (!below.block().isAir()) {
            return below;
        }
        return null;
    }
}
