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
import org.screamingsandals.bedwars.commands.AdminCommand;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.game.LocalGameLoaderImpl;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.setup.SetupOperations;
import org.screamingsandals.lib.Server;
import org.screamingsandals.lib.block.Block;
import org.screamingsandals.lib.block.snapshot.BilateralSignBlockSnapshot;
import org.screamingsandals.lib.block.snapshot.SignBlockSnapshot;
import org.screamingsandals.lib.entity.LivingEntity;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.player.Players;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.tasker.DefaultThreads;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.tasker.task.Task;
import org.screamingsandals.lib.utils.ResourceLocation;
import org.screamingsandals.lib.world.Location;
import org.screamingsandals.lib.world.World;
import org.screamingsandals.lib.world.chunk.Chunk;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One running arena clone: copies the blocks of the source arena (and, when the planner says so, of its lobby region)
 * over many ticks, then the block entities, then optionally the entities, and finally writes, loads and registers the
 * new arena (package-private runtime class, see {@link ArenaCloneService}).
 * <p>
 * All methods run on the server thread (commands and the repeating task).
 */
final class ArenaCloneJob {
    enum Phase {
        BLOCKS,
        BLOCK_ENTITIES,
        ENTITIES,
        FINISHING,
        DONE
    }

    private final ArenaCloneService service;
    private final UUID initiator;
    private final UUID sourceUuid;
    private final String sourceName;
    private final String targetName;
    private final UUID targetUuid;
    private final ConfigurationNode node;
    private final ClonePlanner.ClonePlan plan;
    private final World sourceWorld;
    private final World targetWorld;
    private final CloneSettings settings;
    private final int dx;
    private final int dy;
    private final int dz;

    private Phase phase = Phase.BLOCKS;
    private boolean ended;

    private final Deque<BlockBox> boxes = new ArrayDeque<>();
    private final List<BlockBox> copiedBoxes = new ArrayList<>();
    private ChunkedBoxIterator iterator;
    private long processedBefore;
    private long changed;

    private final List<int[]> blockEntityPositions = new ArrayList<>();
    private int beIndex;
    private int platformCopied;
    private int containers;
    private int signs;
    private int entities;

    private List<int[]> entityColumns = List.of();
    private int entityIndex;
    private Object[] entityTypeFilter = new Object[0];

    private long startedAt;
    private long lastProgressAt;
    private @Nullable Task task;

    // chunk cache
    private long srcColumn = Long.MIN_VALUE;
    private @Nullable Chunk srcChunk;
    private final Map<Long, Chunk> dstChunks = new HashMap<>();
    private final List<Chunk> ownTickets = new ArrayList<>();
    private boolean ticketsSupported = true;
    private final List<ChunkRange> otherArenaChunks = new ArrayList<>(); // PLAN 6.5 / D52

    // block entity filter
    private final Map<ResourceLocation, Boolean> beCache = new HashMap<>();
    private Object[] beTags = new Object[0];
    private Object[] beTypes = new Object[0];

    ArenaCloneJob(@NotNull ArenaCloneService service, @NotNull UUID initiator, @NotNull UUID sourceUuid,
                  @NotNull String sourceName, @NotNull String targetName, @NotNull UUID targetUuid,
                  @NotNull ConfigurationNode node, @NotNull ClonePlanner.ClonePlan plan,
                  @NotNull World sourceWorld, @NotNull World targetWorld, @NotNull CloneSettings settings) {
        this.service = service;
        this.initiator = initiator;
        this.sourceUuid = sourceUuid;
        this.sourceName = sourceName;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
        this.node = node;
        this.plan = plan;
        this.sourceWorld = sourceWorld;
        this.targetWorld = targetWorld;
        this.settings = settings;
        this.dx = plan.offset().dx();
        this.dy = plan.offset().dy();
        this.dz = plan.offset().dz();

        boxes.add(plan.sourceArenaBox());
        copiedBoxes.add(plan.sourceArenaBox());
        if (plan.sourceLobbyBox() != null) {
            boxes.add(plan.sourceLobbyBox());
            copiedBoxes.add(plan.sourceLobbyBox());
        }
    }

    @NotNull String sourceName() {
        return sourceName;
    }

    @NotNull String targetName() {
        return targetName;
    }

    @NotNull UUID sourceUuid() {
        return sourceUuid;
    }

    @NotNull UUID initiator() {
        return initiator;
    }

    @NotNull Phase phase() {
        return phase;
    }

    /**
     * Percent of the blocks that were handled (100 once the block phase is over).
     */
    int percent() {
        if (phase != Phase.BLOCKS) {
            return 100;
        }
        if (iterator == null) {
            return 0;
        }
        return CloneMath.percent(processedBefore + iterator.processed(), plan.totalBlocks());
    }

    // ------------------------------------------------------------------------------------------------ lifecycle

    void start() {
        startedAt = System.currentTimeMillis();
        lastProgressAt = startedAt;

        // D52: the chunks other local arenas claim plugin chunk tickets on (the source is locked and cannot start)
        collectOtherArenaChunks();
        compileFilters();

        iterator = new ChunkedBoxIterator(boxes.poll());
        task = Tasker.runRepeatedly(DefaultThreads.GLOBAL_THREAD, this::tick, 1, TaskerTime.TICKS);
        BedWarsPlugin.getInstance().getLogger().info("Cloning {} -> {}: {} blocks", sourceName, targetName, plan.totalBlocks());
    }

    private void collectOtherArenaChunks() {
        var seen = new IdentityHashMap<GameImpl, Boolean>();
        var all = new ArrayList<GameImpl>(GameManagerImpl.getInstance().getLocalGames());
        if (AdminCommand.gc != null) {
            all.addAll(AdminCommand.gc.values());
        }
        for (var g : all) {
            if (g.getUuid().equals(sourceUuid) || seen.put(g, Boolean.TRUE) != null) {
                continue;
            }
            if (g.getWorld() != null && g.getPos1() != null && g.getPos2() != null) {
                otherArenaChunks.add(ChunkRange.ofBlocks(g.getWorld().getName(),
                        g.getPos1().getBlockX(), g.getPos1().getBlockZ(), g.getPos2().getBlockX(), g.getPos2().getBlockZ()));
            }
        }
    }

    private void compileFilters() {
        var tags = new ArrayList<Object>();
        var types = new ArrayList<Object>();
        for (var entry : settings.blockEntityTypes()) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            var e = entry.trim();
            if (e.startsWith("#")) {
                tags.add(e);
                continue;
            }
            try {
                var block = Block.ofNullable(e.endsWith("[*]") ? e.substring(0, e.length() - 3) : e);
                if (block != null) {
                    types.add(block);
                }
            } catch (RuntimeException ignored) {
                // unknown on this server version
            }
        }
        beTags = tags.toArray();
        beTypes = types.toArray();
        entityTypeFilter = settings.entityTypes().toArray();
    }

    private void tick() {
        if (phase == Phase.DONE) {
            return;
        }
        try {
            long deadline = System.nanoTime() + settings.maxMillisPerTick() * 1_000_000L;
            switch (phase) {
                case BLOCKS:
                    tickBlocks(deadline);
                    break;
                case BLOCK_ENTITIES:
                    tickBlockEntities(deadline);
                    break;
                case ENTITIES:
                    tickEntities(deadline);
                    break;
                case FINISHING:
                    finish();
                    break;
                default:
                    break;
            }
            long now = System.currentTimeMillis();
            if (phase == Phase.BLOCKS && now - lastProgressAt >= settings.progressIntervalSeconds() * 1000L) {
                lastProgressAt = now;
                sendProgress(now);
            }
        } catch (Throwable t) {
            BedWarsPlugin.getInstance().getLogger().error("Arena clone failed", t);
            fail(String.valueOf(t));
        }
    }

    private void sendProgress(long now) {
        long done = processedBefore + iterator.processed();
        send(Message.of(ForkLangKeys.CLONE_PROGRESS)
                .placeholderRaw("source", sourceName)
                .placeholderRaw("target", targetName)
                .placeholder("percent", percent())
                .placeholder("done", done)
                .placeholder("total", plan.totalBlocks())
                .placeholder("seconds", CloneMath.etaSeconds(done, plan.totalBlocks(), now - startedAt, settings.blocksPerTick())));
    }

    // ------------------------------------------------------------------------------------------------ phase 1: blocks

    private void tickBlocks(long deadline) {
        int budget = settings.blocksPerTick();
        while (budget > 0 && System.nanoTime() < deadline) {
            if (!iterator.hasNext()) {
                processedBefore += iterator.processed();
                var next = boxes.poll();
                if (next == null) {
                    releaseTickets();
                    srcChunk = null;
                    srcColumn = Long.MIN_VALUE;
                    afterBlocks();
                    return;
                }
                iterator = new ChunkedBoxIterator(next);
            }
            budget -= iterator.nextBatch(Math.min(budget, 256), this::copyBlock);
        }
    }

    private static long columnKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    private void copyBlock(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        long col = columnKey(cx, cz);
        if (col != srcColumn || srcChunk == null) {
            releaseTickets();
            srcColumn = col;
            srcChunk = chunk(sourceWorld, cx, cz);
        }
        int tx = x + dx;
        int ty = y + dy;
        int tz = z + dz;
        long tcol = columnKey(tx >> 4, tz >> 4);
        var dst = dstChunks.get(tcol);
        if (dst == null) {
            dst = chunk(targetWorld, tx >> 4, tz >> 4);
            dstChunks.put(tcol, dst);
        }
        var data = srcChunk.getBlock(x & 15, y, z & 15).block();
        var target = dst.getBlock(tx & 15, ty, tz & 15);
        if (!data.equals(target.block())) {
            target.alterBlockWithoutPhysics(data); // no physics: bed/door halves and double chests stay intact
            changed++;
        }
        if (settings.copyBlockEntities() && isBlockEntity(data)) {
            blockEntityPositions.add(new int[]{x, y, z});
        }
    }

    private @NotNull Chunk chunk(@NotNull World world, int cx, int cz) {
        var c = world.getChunkAt(cx, cz);
        if (c == null) {
            throw new IllegalStateException("Cannot load chunk " + cx + "," + cz + " in " + world.getName());
        }
        if (!c.isLoaded()) {
            c.load(true); // generate if needed
        }
        // Only chunks WE ticketed are released, and never a chunk another arena claims tickets on (D52):
        // GameImpl#configureChunkTickets records only chunks where addPluginChunkTicket() returned true, so a ticket
        // held by the clone would make a starting neighbour skip (and later our release would drop the only ticket).
        if (ticketsSupported && ChunkRange.anyContains(otherArenaChunks, world.getName(), cx, cz)) {
            BedWarsPlugin.getInstance().getLogger().debug("Clone {}: no ticket for chunk {},{} in {} (inside another arena)", targetName, cx, cz, world.getName());
        } else if (ticketsSupported) {
            try {
                if (!c.hasPluginChunkTicket() && c.addPluginChunkTicket()) {
                    ownTickets.add(c);
                    BedWarsPlugin.getInstance().getLogger().debug("Clone {}: ticketed chunk {},{} in {}", targetName, cx, cz, world.getName());
                }
            } catch (Throwable t) {
                ticketsSupported = false; // < 1.14.3
            }
        }
        return c;
    }

    private void releaseTickets() {
        for (var c : ownTickets) {
            try {
                c.removePluginChunkTicket();
            } catch (Throwable ignored) {
                // nothing sensible to do
            }
        }
        ownTickets.clear();
        dstChunks.clear();
    }

    private boolean isBlockEntity(@NotNull Block b) {
        return beCache.computeIfAbsent(b.location(), k -> {
            try {
                return (beTags.length > 0 && b.is(beTags)) || (beTypes.length > 0 && b.isSameType(beTypes));
            } catch (RuntimeException e) {
                return false;
            }
        }); // one tag/type evaluation per block TYPE, then a HashMap hit
    }

    private void afterBlocks() {
        if (settings.copyBlockEntities() && !blockEntityPositions.isEmpty()) {
            phase = Phase.BLOCK_ENTITIES;
            send(Message.of(ForkLangKeys.CLONE_PHASE_BLOCK_ENTITIES).placeholder("count", blockEntityPositions.size()));
        } else {
            afterBlockEntities();
        }
    }

    // ------------------------------------------------------------------------------------------------ phase 2: block entities

    private void afterBlockEntities() {
        if (settings.copyEntities() && PlatformService.getInstance().isEntityCopySupported()) {
            var columns = new LinkedHashSet<Long>();
            var list = new ArrayList<int[]>();
            for (var box : copiedBoxes) {
                for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                    for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                        if (columns.add(columnKey(cx, cz))) {
                            list.add(new int[]{cx, cz});
                        }
                    }
                }
            }
            entityColumns = list;
            phase = Phase.ENTITIES;
            send(Message.of(ForkLangKeys.CLONE_PHASE_ENTITIES));
        } else {
            phase = Phase.FINISHING;
        }
    }

    private void tickBlockEntities(long deadline) {
        int n = 0;
        while (beIndex < blockEntityPositions.size() && n++ < settings.blockEntitiesPerTick() && System.nanoTime() < deadline) {
            var p = blockEntityPositions.get(beIndex++);
            copyBlockEntity(p[0], p[1], p[2]);
        }
        if (beIndex >= blockEntityPositions.size()) {
            blockEntityPositions.clear();
            afterBlockEntities();
        }
    }

    private void copyBlockEntity(int x, int y, int z) {
        var src = new Location(x, y, z, 0F, 0F, sourceWorld).getBlock();
        var dst = new Location(x + dx, y + dy, z + dz, 0F, 0F, targetWorld).getBlock();
        if (!src.block().equals(dst.block())) {
            return; // target changed meanwhile -> skip
        }
        var platform = PlatformService.getInstance();
        if (platform.isBlockEntityCopySupported() && platform.copyBlockEntity(src, dst)) {
            platformCopied++;
            return;
        }

        // fallback through slib only: sign texts and container contents
        var s = src.blockSnapshot();
        if (s instanceof SignBlockSnapshot) {
            var ss = (SignBlockSnapshot) s;
            var d = dst.blockSnapshot();
            if (d instanceof SignBlockSnapshot) {
                var ds = (SignBlockSnapshot) d;
                var lines = ss.frontLines();
                for (int i = 0; i < Math.min(4, lines.length); i++) {
                    ds.frontLine(i, lines[i]);
                }
                if (ss instanceof BilateralSignBlockSnapshot && ds instanceof BilateralSignBlockSnapshot) {
                    var sb = (BilateralSignBlockSnapshot) ss;
                    var db = (BilateralSignBlockSnapshot) ds;
                    var back = sb.backLines();
                    for (int i = 0; i < Math.min(4, back.length); i++) {
                        db.backLine(i, back[i]);
                    }
                    try {
                        db.backSideGlowing(sb.backSideGlowing());
                    } catch (Throwable ignored) {
                        // < 1.20
                    }
                }
                try {
                    ds.frontSideGlowing(ss.frontSideGlowing());
                } catch (Throwable ignored) {
                    // < 1.17
                }
                try {
                    ds.waxed(ss.waxed());
                } catch (Throwable ignored) {
                    // < 1.20
                }
                ds.updateBlock(true, false); // REQUIRED: the setters only touch the snapshot
                signs++;
            }
        } else if (s != null && s.holdsInventory()) {
            var si = s.getInventory();
            var d = dst.blockSnapshot();
            if (si != null && d != null && d.holdsInventory()) {
                var di = d.getInventory();
                if (di != null) {
                    di.setContents(Arrays.copyOf(si.getContents(), di.getSize()));
                    containers++;
                }
                // NEVER call d.updateBlock(...) here: it would write the (empty) snapshot inventory back over the live one
            }
        }
        // Double chests: both halves exist now, so both inventories are the same 54-slot double inventory; copying twice is idempotent.
    }

    // ------------------------------------------------------------------------------------------------ phase 3: entities

    private void tickEntities(long deadline) {
        int n = 0;
        while (entityIndex < entityColumns.size() && n++ < 4 && System.nanoTime() < deadline) {
            var col = entityColumns.get(entityIndex++);
            var chunk = sourceWorld.getChunkAt(col[0], col[1]);
            if (chunk == null) {
                continue;
            }
            if (!chunk.isLoaded()) {
                chunk.load();
            }
            for (var e : chunk.getEntities()) {
                if (e instanceof Player) {
                    continue;
                }
                if (entityTypeFilter.length == 0 || !e.getEntityType().is(entityTypeFilter)) {
                    continue;
                }
                if (settings.skipHologramArmorStands() && e instanceof LivingEntity
                        && ((LivingEntity) e).isInvisible() && e.isCustomNameVisible()) {
                    continue;
                }
                var l = e.getLocation();
                if (!inAnyCopiedBox(BlockBox.floor(l.getX()), BlockBox.floor(l.getY()), BlockBox.floor(l.getZ()))) {
                    continue;
                }
                if (PlatformService.getInstance().copyEntity(e, l.add(dx, dy, dz).withWorld(targetWorld))) {
                    entities++;
                }
            }
        }
        if (entityIndex >= entityColumns.size()) {
            phase = Phase.FINISHING;
        }
    }

    private boolean inAnyCopiedBox(int x, int y, int z) {
        for (var box : copiedBoxes) {
            if (box.contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------ end of the job

    /**
     * One guard for every way a job ends (finish / fail / cancel).
     */
    private boolean stop() {
        if (ended) {
            return false;
        }
        ended = true;
        phase = Phase.DONE;
        if (task != null) {
            task.cancel();
        }
        releaseTickets();
        srcChunk = null;
        return true;
    }

    private void finish() {
        if (!stop()) {
            return;
        }
        try {
            BedWarsPlugin.getInstance().getLogger().info("Clone {} -> {}: {} of {} blocks differed and were written",
                    sourceName, targetName, changed, plan.totalBlocks());
            if (GameManagerImpl.getInstance().hasGame(targetName) || (AdminCommand.gc != null && AdminCommand.gc.containsKey(targetName))) {
                sendAndLog(failed("name taken meanwhile"));
                service.onJobEnded(this);
                return;
            }
            File file;
            try {
                file = LocalGameLoaderImpl.getInstance().writeNewArenaFile(targetUuid, node);
            } catch (ConfigurateException e) {
                BedWarsPlugin.getInstance().getLogger().error("Arena clone: cannot write the arena file", e);
                sendAndLog(failed(String.valueOf(e.getMessage())));
                service.onJobEnded(this);
                return;
            }
            LocalGameLoaderImpl.getInstance().loadGame(file, false).whenComplete((game, error) -> {
                try {
                    if (game == null || error != null) {
                        if (error != null) {
                            BedWarsPlugin.getInstance().getLogger().error("Arena clone: loading the new arena failed", error);
                        }
                        var disabled = new File(file.getPath() + ".disabled"); // GameManagerImpl skips *.disabled
                        //noinspection ResultOfMethodCallIgnored
                        file.renameTo(disabled);
                        sendAndLog(Message.of(ForkLangKeys.CLONE_FAILED_LOAD)
                                .placeholderRaw("target", targetName)
                                .placeholderRaw("file", disabled.getName()));
                    } else {
                        GameManagerImpl.getInstance().addGame(game); // loadGame already called start()
                        sendAndLog(Message.of(ForkLangKeys.CLONE_FINISHED)
                                .placeholderRaw("target", targetName)
                                .placeholder("blocks", plan.totalBlocks())
                                .placeholder("block_entities", platformCopied + containers + signs)
                                .placeholder("entities", entities)
                                .placeholder("seconds", (System.currentTimeMillis() - startedAt) / 1000L)
                                .placeholder("command", commandFor("/bw setup " + targetName)));
                    }
                } catch (Throwable t) {
                    BedWarsPlugin.getInstance().getLogger().error("Arena clone: finishing failed", t);
                } finally {
                    service.onJobEnded(this);
                }
            });
        } catch (Throwable t) {
            // stop() already ran: make sure the job is released and the initiator hears about it
            BedWarsPlugin.getInstance().getLogger().error("Arena clone failed while finishing", t);
            try {
                sendAndLog(failed(String.valueOf(t)));
            } finally {
                service.onJobEnded(this);
            }
        }
    }

    private @NotNull Message failed(@NotNull String reason) {
        return Message.of(ForkLangKeys.CLONE_FAILED)
                .placeholderRaw("source", sourceName)
                .placeholderRaw("target", targetName)
                .placeholderRaw("reason", reason);
    }

    private void fail(@NotNull String reason) {
        if (!stop()) {
            return;
        }
        try {
            sendAndLog(failed(reason));
        } finally {
            service.onJobEnded(this);
        }
    }

    /**
     * Cancels the job (clone cancel, reload, shutdown).
     *
     * @param requester who asked for it, informed too when it is not the initiator; null for reload/shutdown
     */
    void cancel(@Nullable CommandSender requester) {
        int p = percent();
        if (!stop()) {
            return;
        }
        try {
            var message = Message.of(ForkLangKeys.CLONE_CANCELLED)
                    .placeholderRaw("source", sourceName)
                    .placeholder("percent", p);
            send(message);
            // the initiator got the message (or the console did when they are offline): tell the requester too if it is somebody else
            boolean requesterIsInitiator = requester instanceof Player && ((Player) requester).getUniqueId().equals(initiator);
            boolean requesterIsConsoleThatAlreadyHeard = !(requester instanceof Player) && Players.getPlayer(initiator) == null;
            if (requester != null && !requesterIsInitiator && !requesterIsConsoleThatAlreadyHeard) {
                requester.sendMessage(Message.of(ForkLangKeys.CLONE_CANCELLED)
                        .defaultPrefix()
                        .placeholderRaw("source", sourceName)
                        .placeholder("percent", p));
            }
        } finally {
            service.onJobEnded(this);
        }
    }

    // ------------------------------------------------------------------------------------------------ messages

    private @NotNull Component commandFor(@NotNull String command) {
        var player = Players.getPlayer(initiator);
        if (player != null) {
            return SetupOperations.clickable(player, command, true);
        }
        return Component.text(command);
    }

    /**
     * To the initiator, or to the console when they are offline.
     */
    private void send(@NotNull Message message) {
        send(message, false);
    }

    /**
     * Like {@link #send(Message)}, and additionally to the console when the initiator is online (finish / failure).
     */
    private void sendAndLog(@NotNull Message message) {
        send(message, true);
    }

    private void send(@NotNull Message message, boolean alsoConsole) {
        message.defaultPrefix();
        var player = Players.getPlayer(initiator);
        if (player != null) {
            message.send(player);
            if (alsoConsole) {
                message.send(Server.getConsoleSender());
            }
        } else {
            message.send(Server.getConsoleSender());
        }
    }
}
