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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.BedWarsPlugin;
import org.screamingsandals.bedwars.PlatformService;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.commands.AdminCommand;
import org.screamingsandals.bedwars.events.PlayerJoinEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.game.LocalGameLoaderImpl;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lobby.MainLobby;
import org.screamingsandals.bedwars.setup.ArenaNames;
import org.screamingsandals.bedwars.setup.SetupChecklist;
import org.screamingsandals.bedwars.setup.SetupOperations;
import org.screamingsandals.lib.Server;
import org.screamingsandals.lib.event.OnEvent;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.methods.OnPreDisable;
import org.screamingsandals.lib.world.Location;
import org.screamingsandals.lib.world.World;
import org.screamingsandals.lib.world.Worlds;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clones arenas: validates the request (pure {@link ClonePlanner}), shows a preview, asks for confirmation and runs
 * one {@link ArenaCloneJob} at a time. While a job runs, joins into its source arena are refused.
 */
@Service
public class ArenaCloneService {
    /**
     * Largest block coordinate (X/Z) accepted from a command; Minecraft worlds end at +-30,000,000.
     */
    static final int MAX_HORIZONTAL = 30_000_000;
    private static final int MAX_ERROR_LINES = 5;

    private final Map<UUID, PendingClone> pending = new HashMap<>();
    private final Set<UUID> lockedArenas = ConcurrentHashMap.newKeySet(); // read by other packages (D51 snapshot, P8 AddCommand)
    private volatile @Nullable ArenaCloneJob job; // one job at a time

    record PendingClone(UUID sourceUuid, String sourceName, String newName, int x, int y, int z, String world, long createdAt) {
    }

    record Prepared(ClonePlanner.ClonePlan plan, World targetWorld, int playersInTarget) {
    }

    public static @NotNull ArenaCloneService getInstance() {
        return ServiceManager.get(ArenaCloneService.class);
    }

    /**
     * Whether an arena name is reserved by a clone job in progress.
     */
    public boolean isNameReserved(@NotNull String name) {
        var j = job;
        return j != null && j.targetName().equalsIgnoreCase(name);
    }

    /**
     * Whether the arena is locked (it is the source of a clone job in progress).
     */
    public boolean isLocked(@NotNull UUID arena) {
        return lockedArenas.contains(arena);
    }

    /**
     * Whether a box spanned by {@code a} and {@code b} ({@code b == null}: the single point {@code a}) touches the area
     * the running clone job is writing to. That area belongs to no registered arena until the job is over, so the
     * overlap checks of {@code pos1}/{@code pos2} ask here.
     */
    public boolean overlapsRunningTarget(@NotNull Location a, @Nullable Location b) {
        var j = job;
        if (j == null || a.getWorld() == null || !a.getWorld().getName().equals(j.targetWorldName())) {
            return false;
        }
        var other = b == null ? a : b;
        var box = BlockBox.ofDoubles(a.getX(), a.getY(), a.getZ(), other.getX(), other.getY(), other.getZ());
        var plan = j.plan();
        return box.intersects(plan.targetArenaBox())
                || (plan.targetLobbyBox() != null && box.intersects(plan.targetLobbyBox()));
    }

    // ------------------------------------------------------------------------------------------------ request

    /**
     * {@code /bw admin <arena> clone <new-name> <x> <y> <z> [world]}: validates, shows the preview and either waits for
     * the confirmation or starts at once.
     */
    public void request(@NotNull Player p, @NotNull GameImpl source, @NotNull String newName, @NotNull String xs,
                        @NotNull String ys, @NotNull String zs, @Nullable String worldName, @NotNull String confirmCommand) {
        var settings = CloneSettings.load();
        pending.remove(p.getUniqueId()); // a new request replaces the old pending one, even when it is refused

        World targetWorld = worldName == null ? p.getLocation().getWorld() : Worlds.getWorld(worldName);
        if (targetWorld == null) {
            p.sendMessage(Message.of(ForkLangKeys.CLONE_ERROR_UNKNOWN_WORLD).defaultPrefix().placeholderRaw("world", String.valueOf(worldName)));
            return;
        }

        var l = p.getLocation();
        OptionalInt x = RelativeCoordinate.resolve(xs, l.getBlockX());
        OptionalInt y = RelativeCoordinate.resolve(ys, l.getBlockY());
        OptionalInt z = RelativeCoordinate.resolve(zs, l.getBlockZ());
        if (x.isEmpty() || Math.abs((long) x.getAsInt()) > MAX_HORIZONTAL) {
            sendInvalidCoordinate(p, xs);
            return;
        }
        if (y.isEmpty()) {
            sendInvalidCoordinate(p, ys);
            return;
        }
        if (z.isEmpty() || Math.abs((long) z.getAsInt()) > MAX_HORIZONTAL) {
            sendInvalidCoordinate(p, zs);
            return;
        }

        var prepared = prepare(p, source, newName, x.getAsInt(), y.getAsInt(), z.getAsInt(), targetWorld, settings);
        if (prepared == null) {
            return;
        }
        sendPreview(p, source, newName, targetWorld, prepared, settings);

        if (!settings.requireConfirmation()) {
            start(p, source, newName, prepared, settings);
            return;
        }
        pending.put(p.getUniqueId(), new PendingClone(source.getUuid(), source.getName(), newName,
                x.getAsInt(), y.getAsInt(), z.getAsInt(), targetWorld.getName(), System.currentTimeMillis()));
        p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_CONFIRM)
                .defaultPrefix()
                .placeholder("command", SetupOperations.clickable(p, confirmCommand, true))
                .placeholder("seconds", settings.confirmationTimeoutSeconds()));
    }

    private void sendInvalidCoordinate(@NotNull Player p, @NotNull String value) {
        p.sendMessage(Message.of(ForkLangKeys.CLONE_ERROR_INVALID_COORDINATE).defaultPrefix().placeholderRaw("value", value));
    }

    /**
     * Validates everything about the request. Returns null after sending the first errors (at most 5 lines).
     */
    private @Nullable Prepared prepare(@NotNull Player p, @NotNull GameImpl source, @NotNull String newName, int x, int y, int z,
                                       @NotNull World targetWorld, @NotNull CloneSettings settings) {
        var running = job;
        if (running != null) {
            p.sendMessage(Message.of(ForkLangKeys.CLONE_ERROR_BUSY)
                    .defaultPrefix()
                    .placeholderRaw("source", running.sourceName())
                    .placeholderRaw("target", running.targetName())
                    .placeholder("percent", running.percent())
                    .placeholder("command", SetupOperations.clickable(p, "/bw setup clone cancel", true)));
            return null;
        }

        var taken = new ArrayList<>(GameManagerImpl.getInstance().getGameNames());
        if (AdminCommand.gc != null) {
            taken.addAll(AdminCommand.gc.keySet());
        }
        var problem = ArenaNames.validate(newName, taken, ArenaNames.RESERVED);
        if (problem.isPresent()) {
            if (problem.get() == ArenaNames.Problem.TAKEN) {
                p.sendMessage(Message.of(ForkLangKeys.CLONE_ERROR_NAME_TAKEN).defaultPrefix().placeholderRaw("name", newName));
            } else {
                p.sendMessage(Message.of(ForkLangKeys.SETUP_INVALID_NAME).defaultPrefix().placeholderRaw("arena", newName));
            }
            return null;
        }

        // source state: edit mode, disabled, or waiting with nobody in it
        boolean inEdit = AdminCommand.gc != null && AdminCommand.gc.get(source.getName()) == source;
        boolean idle = source.getStatus() == GameStatus.DISABLED
                || (source.getStatus() == GameStatus.WAITING && source.countConnectedPlayers() == 0);
        if (!inEdit && !idle) {
            p.sendMessage(Message.of(ForkLangKeys.CLONE_ERROR_SOURCE_IN_USE)
                    .defaultPrefix()
                    .placeholderRaw("arena", source.getName())
                    .placeholderRaw("status", source.getStatus().name())
                    .placeholder("command", SetupOperations.clickable(p, "/bw admin " + source.getName() + " edit", true)));
            return null;
        }

        // completeness = the required checks of /bw admin save (guarantees that the clone file loads)
        var entries = SetupChecklist.evaluate(SetupOperations.snapshot(source));
        if (!SetupChecklist.isSaveable(entries)) {
            var missing = entries.stream().filter(e -> e.severity() == SetupChecklist.Severity.REQUIRED && !e.done()).findFirst();
            var item = missing.isPresent() ? SetupOperations.itemLabel(missing.get()) : Message.of(ForkLangKeys.SETUP_ITEM_BOUNDS);
            p.sendMessage(Message.of(ForkLangKeys.CLONE_ERROR_SOURCE_INCOMPLETE)
                    .defaultPrefix()
                    .placeholderRaw("arena", source.getName())
                    .placeholder("item", item));
            return null;
        }

        var sourceWorld = source.getWorld();
        var arenaBox = BlockBox.ofDoubles(
                source.getPos1().getX(), source.getPos1().getY(), source.getPos1().getZ(),
                source.getPos2().getX(), source.getPos2().getY(), source.getPos2().getZ());
        arenaBox = arenaBox.withY(Math.max(arenaBox.minY(), sourceWorld.getMinY()), Math.min(arenaBox.maxY(), sourceWorld.getMaxY() - 1));

        var lobby = source.getLobbySpawn();
        BlockBox lobbyRegion = null;
        if (source.getLobbyPos1() != null && source.getLobbyPos2() != null) {
            lobbyRegion = BlockBox.ofDoubles(
                    source.getLobbyPos1().getX(), source.getLobbyPos1().getY(), source.getLobbyPos1().getZ(),
                    source.getLobbyPos2().getX(), source.getLobbyPos2().getY(), source.getLobbyPos2().getZ());
        }

        // the source's own lobby spawn moves with the copy (and is then covered by the source boxes) in the same cases as ClonePlanner
        boolean sourceSpawnRelocates = lobby.getWorld().getName().equals(sourceWorld.getName())
                && (arenaBox.containsPoint(lobby.getX(), lobby.getY(), lobby.getZ())
                || (lobbyRegion != null && lobbyRegion.containsPoint(lobby.getX(), lobby.getY(), lobby.getZ())));

        var obstacles = new ArrayList<ClonePlanner.Obstacle>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<GameImpl, Boolean>());
        var others = new ArrayList<GameImpl>(GameManagerImpl.getInstance().getLocalGames());
        if (AdminCommand.gc != null) {
            others.addAll(AdminCommand.gc.values());
        }
        for (var g : others) {
            if (!seen.add(g)) {
                continue;
            }
            BlockBox gArena = null;
            BlockBox gLobbyRegion = null;
            if (g != source && g.getWorld() != null && g.getPos1() != null && g.getPos2() != null) {
                gArena = BlockBox.ofDoubles(g.getPos1().getX(), g.getPos1().getY(), g.getPos1().getZ(),
                        g.getPos2().getX(), g.getPos2().getY(), g.getPos2().getZ());
                obstacles.add(new ClonePlanner.Obstacle(g.getName(), g.getWorld().getName(), gArena, ClonePlanner.ObstacleKind.ARENA));
            }
            if (g.getLobbyPos1() != null && g.getLobbyPos2() != null) { // includes the source's own region
                gLobbyRegion = BlockBox.ofDoubles(g.getLobbyPos1().getX(), g.getLobbyPos1().getY(), g.getLobbyPos1().getZ(),
                        g.getLobbyPos2().getX(), g.getLobbyPos2().getY(), g.getLobbyPos2().getZ());
                obstacles.add(new ClonePlanner.Obstacle(g.getName(), g.getLobbyPos1().getWorld().getName(), gLobbyRegion,
                        ClonePlanner.ObstacleKind.LOBBY_REGION));
            }
            // A waiting lobby is often just a spawn point without a region: protect that block too, unless a box above
            // already covers it (or it moves with the clone)
            var spawn = g.getLobbySpawn();
            if (spawn != null && spawn.getWorld() != null) {
                var spawnWorld = spawn.getWorld().getName();
                boolean inArena = gArena != null && g.getWorld().getName().equals(spawnWorld)
                        && gArena.containsPoint(spawn.getX(), spawn.getY(), spawn.getZ());
                boolean inRegion = gLobbyRegion != null && g.getLobbyPos1().getWorld().getName().equals(spawnWorld)
                        && gLobbyRegion.containsPoint(spawn.getX(), spawn.getY(), spawn.getZ());
                boolean covered = g == source ? sourceSpawnRelocates : (inArena || inRegion);
                if (!covered) {
                    obstacles.add(ClonePlanner.spawnObstacle(g.getName() + " (lobby)", spawnWorld, spawn.getX(), spawn.getY(), spawn.getZ()));
                }
            }
        }
        var mainLobby = MainLobby.getLocation();
        if (mainLobby != null && mainLobby.getWorld() != null) {
            obstacles.add(ClonePlanner.spawnObstacle("main lobby", mainLobby.getWorld().getName(),
                    mainLobby.getX(), mainLobby.getY(), mainLobby.getZ()));
        }

        var result = ClonePlanner.plan(new ClonePlanner.CloneRequest(
                sourceWorld.getName(), arenaBox,
                lobby.getWorld().getName(), lobby.getX(), lobby.getY(), lobby.getZ(), lobbyRegion,
                targetWorld.getName(), x, y, z,
                targetWorld.getMinY(), targetWorld.getMaxY(), settings.maxBlocks(), obstacles));
        if (!result.ok()) {
            int lines = 0;
            for (var error : result.errors()) {
                if (lines++ >= MAX_ERROR_LINES) {
                    break;
                }
                p.sendMessage(errorMessage(error, settings));
            }
            return null;
        }

        var plan = result.plan();
        int playersInTarget = 0;
        for (var other : Server.getConnectedPlayersFromWorld(targetWorld)) {
            var pl = other.getLocation();
            if (plan.targetArenaBox().containsPoint(pl.getX(), pl.getY(), pl.getZ())
                    || (plan.targetLobbyBox() != null && plan.targetLobbyBox().containsPoint(pl.getX(), pl.getY(), pl.getZ()))) {
                playersInTarget++;
            }
        }
        return new Prepared(plan, targetWorld, playersInTarget);
    }

    private static @NotNull Message errorMessage(@NotNull ClonePlanner.PlanError error, @NotNull CloneSettings settings) {
        var detail = error.detail() == null ? "" : error.detail();
        switch (error.code()) {
            case SAME_PLACE:
                return Message.of(ForkLangKeys.CLONE_ERROR_SAME_PLACE).defaultPrefix();
            case OUT_OF_WORLD:
                return Message.of(ForkLangKeys.CLONE_ERROR_OUT_OF_WORLD).defaultPrefix().placeholderRaw("range", detail);
            case TOO_LARGE:
                return Message.of(ForkLangKeys.CLONE_ERROR_TOO_LARGE).defaultPrefix()
                        .placeholderRaw("blocks", detail)
                        .placeholder("limit", settings.maxBlocks());
            case OVERLAPS_SOURCE:
                return Message.of(ForkLangKeys.CLONE_ERROR_OVERLAPS_SOURCE).defaultPrefix();
            case OVERLAPS_CLONE:
                return Message.of(ForkLangKeys.CLONE_ERROR_OVERLAPS_CLONE).defaultPrefix().placeholderRaw("arena", detail);
            case OVERLAPS_ARENA:
            default:
                return Message.of(ForkLangKeys.CLONE_ERROR_OVERLAPS_ARENA).defaultPrefix().placeholderRaw("arena", detail);
        }
    }

    private void sendPreview(@NotNull Player p, @NotNull GameImpl source, @NotNull String newName, @NotNull World targetWorld,
                             @NotNull Prepared prepared, @NotNull CloneSettings settings) {
        var plan = prepared.plan();
        p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_HEADER)
                .defaultPrefix()
                .placeholderRaw("source", source.getName())
                .placeholderRaw("target", newName));
        p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_SOURCE)
                .placeholderRaw("world", source.getWorld().getName())
                .placeholderRaw("min", plan.sourceArenaBox().minString())
                .placeholderRaw("max", plan.sourceArenaBox().maxString())
                .placeholderRaw("size", plan.sourceArenaBox().sizeString()));
        p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_TARGET)
                .placeholderRaw("world", targetWorld.getName())
                .placeholderRaw("min", plan.targetArenaBox().minString())
                .placeholderRaw("max", plan.targetArenaBox().maxString()));
        p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_BLOCKS)
                .placeholder("blocks", plan.totalBlocks())
                .placeholder("seconds", CloneMath.minimumSeconds(plan.totalBlocks(), settings.blocksPerTick())));

        if (plan.lobbyPolicy() == ClonePlanner.LobbyPolicy.RELOCATE) {
            p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_LOBBY_MOVED));
        }
        for (var warning : plan.warnings()) {
            switch (warning) {
                case LOBBY_REGION_COPIED:
                    p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_LOBBY_REGION_COPIED));
                    break;
                case LOBBY_SHARED:
                    p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_LOBBY_SHARED).placeholderRaw("target", newName));
                    break;
                case LOBBY_REGION_DROPPED:
                    p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_LOBBY_REGION_DROPPED));
                    break;
                default:
                    break;
            }
        }
        if (prepared.playersInTarget() > 0) {
            p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_PLAYERS_IN_TARGET).placeholder("count", prepared.playersInTarget()));
        }
        var platform = PlatformService.getInstance();
        if (!platform.isBlockEntityCopySupported()) {
            p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_BLOCK_ENTITIES_LIMITED));
        }
        var entities = Message.of(platform.isEntityCopySupported() && settings.copyEntities()
                ? ForkLangKeys.CLONE_PREVIEW_ENTITIES_COPIED
                : ForkLangKeys.CLONE_PREVIEW_ENTITIES_NOT_COPIED);
        p.sendMessage(Message.of(ForkLangKeys.CLONE_PREVIEW_LIMITATIONS).placeholder("entities", entities));
    }

    // ------------------------------------------------------------------------------------------------ confirm / start

    /**
     * {@code clone confirm}: re-validates with the stored absolute coordinates (the state may have changed) and starts.
     */
    public void confirm(@NotNull Player p, @NotNull GameImpl expectedSource) {
        var settings = CloneSettings.load();
        var pc = pending.remove(p.getUniqueId());
        if (pc == null
                || System.currentTimeMillis() - pc.createdAt() > settings.confirmationTimeoutSeconds() * 1000L
                || !pc.sourceUuid().equals(expectedSource.getUuid())) {
            p.sendMessage(Message.of(ForkLangKeys.CLONE_NO_PENDING).defaultPrefix());
            return;
        }
        var targetWorld = Worlds.getWorld(pc.world());
        if (targetWorld == null) {
            p.sendMessage(Message.of(ForkLangKeys.CLONE_ERROR_UNKNOWN_WORLD).defaultPrefix().placeholderRaw("world", pc.world()));
            return;
        }
        var prepared = prepare(p, expectedSource, pc.newName(), pc.x(), pc.y(), pc.z(), targetWorld, settings);
        if (prepared != null) {
            start(p, expectedSource, pc.newName(), prepared, settings);
        }
    }

    private void start(@NotNull Player p, @NotNull GameImpl source, @NotNull String newName, @NotNull Prepared prepared,
                       @NotNull CloneSettings settings) {
        var plan = prepared.plan();
        var arenasDir = BedWarsPlugin.getInstance().getPluginDescription().dataFolder().resolve("arenas");
        UUID newUuid;
        do {
            newUuid = UUID.randomUUID();
        } while (GameManagerImpl.getInstance().getGame(newUuid).isPresent() || arenasDir.resolve(newUuid + ".json").toFile().exists());

        ConfigurationNode node;
        try {
            node = LocalGameLoaderImpl.getInstance().serializeGame(source);
            var relocation = ArenaNodeRelocator.relocate(node, new ArenaNodeRelocator.Options(
                    plan.offset(), prepared.targetWorld().getName(), plan.lobbyPolicy(), newUuid.toString(), newName));
            relocation.warnings().forEach(w ->
                    BedWarsPlugin.getInstance().getLogger().warn("Clone {} -> {}: {}", source.getName(), newName, w));
        } catch (ConfigurateException e) { // includes SerializationException
            BedWarsPlugin.getInstance().getLogger().error("Arena clone: cannot serialize the source arena", e);
            p.sendMessage(Message.of(ForkLangKeys.CLONE_FAILED)
                    .defaultPrefix()
                    .placeholderRaw("source", source.getName())
                    .placeholderRaw("target", newName)
                    .placeholderRaw("reason", String.valueOf(e.getMessage())));
            return;
        }

        lockedArenas.add(source.getUuid());
        var newJob = new ArenaCloneJob(this, p.getUniqueId(), source.getUuid(), source.getName(), newName, newUuid, node, plan,
                source.getWorld(), prepared.targetWorld(), settings);
        job = newJob;
        try {
            newJob.start();
        } catch (RuntimeException e) {
            BedWarsPlugin.getInstance().getLogger().error("Arena clone: cannot start", e);
            onJobEnded(newJob);
            p.sendMessage(Message.of(ForkLangKeys.CLONE_FAILED)
                    .defaultPrefix()
                    .placeholderRaw("source", source.getName())
                    .placeholderRaw("target", newName)
                    .placeholderRaw("reason", String.valueOf(e)));
            return;
        }
        p.sendMessage(Message.of(ForkLangKeys.CLONE_STARTED)
                .defaultPrefix()
                .placeholderRaw("source", source.getName())
                .placeholderRaw("target", newName)
                .placeholder("blocks", plan.totalBlocks()));
    }

    // ------------------------------------------------------------------------------------------------ cancel / status

    public void cancel(@NotNull CommandSender sender) {
        var running = job;
        if (running == null) {
            sender.sendMessage(Message.of(ForkLangKeys.CLONE_NONE_RUNNING).defaultPrefix());
            return;
        }
        running.cancel(sender);
    }

    public void status(@NotNull CommandSender sender) {
        var running = job;
        if (running == null) {
            sender.sendMessage(Message.of(ForkLangKeys.CLONE_NONE_RUNNING).defaultPrefix());
            return;
        }
        sender.sendMessage(Message.of(ForkLangKeys.CLONE_STATUS)
                .defaultPrefix()
                .placeholderRaw("source", running.sourceName())
                .placeholderRaw("target", running.targetName())
                .placeholder("phase", Message.of(phaseKey(running.phase())))
                .placeholder("percent", running.percent()));
    }

    private static @NotNull String[] phaseKey(@NotNull ArenaCloneJob.Phase phase) {
        switch (phase) {
            case BLOCKS:
                return ForkLangKeys.CLONE_PHASE_NAME_BLOCKS;
            case BLOCK_ENTITIES:
                return ForkLangKeys.CLONE_PHASE_NAME_BLOCK_ENTITIES;
            case ENTITIES:
                return ForkLangKeys.CLONE_PHASE_NAME_ENTITIES;
            case FINISHING:
            case DONE:
            default:
                return ForkLangKeys.CLONE_PHASE_NAME_FINISHING;
        }
    }

    /**
     * Called by the job when it is over (finished, failed or cancelled).
     */
    void onJobEnded(@NotNull ArenaCloneJob ended) {
        if (job == ended) {
            job = null;
        }
        lockedArenas.remove(ended.sourceUuid());
    }

    // ------------------------------------------------------------------------------------------------ lifecycle

    /**
     * Joins into the source arena are refused while it is being copied (an idle WAITING source would otherwise be able
     * to start a game in the middle of the copy).
     */
    @OnEvent
    public void onPlayerJoin(PlayerJoinEventImpl event) {
        if (lockedArenas.contains(event.getGame().getUuid())) {
            event.cancelled(true); // GameImpl#internalJoinPlayer then calls changeGame(null)
            Message.of(ForkLangKeys.CLONE_ERROR_JOIN_LOCKED).defaultPrefix().send(event.getPlayer());
        }
    }

    @OnPreDisable
    public void onPreDisable() {
        var running = job;
        if (running != null) {
            try {
                running.cancel(null); // releases the tickets and informs the initiator
            } catch (RuntimeException e) {
                BedWarsPlugin.getInstance().getLogger().warn("Cannot cancel the running arena clone cleanly", e);
            }
        }
        job = null;
        pending.clear();
        lockedArenas.clear();
    }
}
