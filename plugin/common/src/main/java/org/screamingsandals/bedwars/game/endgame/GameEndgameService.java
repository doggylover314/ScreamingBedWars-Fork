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

package org.screamingsandals.bedwars.game.endgame;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.BedWarsPlugin;
import org.screamingsandals.bedwars.api.config.GameConfigurationContainer;
import org.screamingsandals.bedwars.api.events.TargetInvalidationReason;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.boss.BossBarImpl;
import org.screamingsandals.bedwars.entities.EntitiesManagerImpl;
import org.screamingsandals.bedwars.events.GameChangedStatusEventImpl;
import org.screamingsandals.bedwars.events.GameEndingEventImpl;
import org.screamingsandals.bedwars.events.GameStartedEventImpl;
import org.screamingsandals.bedwars.events.GameTickEventImpl;
import org.screamingsandals.bedwars.events.PreRebuildingEventImpl;
import org.screamingsandals.bedwars.game.GameCycleImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.endgame.dragon.DragonAllocation;
import org.screamingsandals.bedwars.game.endgame.dragon.DragonBlockPolicy;
import org.screamingsandals.bedwars.game.endgame.dragon.DragonSpawnPlanner;
import org.screamingsandals.bedwars.game.target.AExpirableTarget;
import org.screamingsandals.bedwars.game.target.TargetBlockImpl;
import org.screamingsandals.bedwars.game.timeline.GameTimelineService;
import org.screamingsandals.bedwars.game.upgrade.builtin.DragonBuffUpgradeDefinition;
import org.screamingsandals.bedwars.holograms.StatisticsHolograms;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.lib.debug.Debug;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.bedwars.statistics.PlayerStatisticManager;
import org.screamingsandals.bedwars.utils.BedUtils;
import org.screamingsandals.bedwars.utils.Broadcasts;
import org.screamingsandals.bedwars.utils.TitleUtils;
import org.screamingsandals.lib.block.Block;
import org.screamingsandals.lib.block.BlockPlacement;
import org.screamingsandals.lib.entity.Entity;
import org.screamingsandals.lib.entity.projectile.ProjectileEntity;
import org.screamingsandals.lib.event.EventManager;
import org.screamingsandals.lib.event.OnEvent;
import org.screamingsandals.lib.event.entity.EnderDragonChangePhaseEvent;
import org.screamingsandals.lib.event.entity.EntityDamageByEntityEvent;
import org.screamingsandals.lib.event.entity.EntityDamageEvent;
import org.screamingsandals.lib.event.entity.EntityExplodeEvent;
import org.screamingsandals.lib.event.world.WorldLoadEvent;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.spectator.bossbar.BossBarColor;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.methods.OnDisable;
import org.screamingsandals.lib.utils.annotations.methods.OnPostEnable;
import org.screamingsandals.lib.utils.annotations.methods.OnPreDisable;
import org.screamingsandals.lib.world.Worlds;
import org.screamingsandals.lib.world.gamerule.GameRuleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bed destruction, sudden death dragons and the end of the game by time (R2).
 * <p>
 * Entry points called by the timeline area ({@code GameCycleImpl} Phase 6.5, global/main thread):
 * {@link #destroyAllTargets(GameImpl)}, {@link #startSuddenDeath(GameImpl)} and {@link #endGameByTime(GameImpl)}.
 * The arena time limit goes through {@link #endGameByTime(GameImpl, GameTickEventImpl)} (Phase 5.1.2).
 */
@Service
public class GameEndgameService {
    /**
     * Game uuid to runtime.
     */
    private final Map<UUID, EndgameRuntime> runtimes = new ConcurrentHashMap<>();
    /**
     * Dragon entity uuid to dragon.
     */
    private final Map<UUID, ManagedDragon> dragons = new ConcurrentHashMap<>();
    /**
     * World name to override (main thread only).
     */
    private final Map<String, MobGriefingOverride> mobGriefing = new HashMap<>();
    /**
     * Created on first use (needs the plugin data folder).
     */
    private @Nullable GameRuleRestoreStore gameRuleStore;

    private static final class EndgameRuntime {
        volatile boolean endedByTime;
        /**
         * Sudden death already read the Dragon Buff levels of the teams.
         */
        volatile boolean dragonBuffLocked;
        volatile @Nullable SuddenDeathSession suddenDeath;
    }

    private static final class MobGriefingOverride {
        @Nullable Object previous;
        int refs;
    }

    public static @NotNull GameEndgameService getInstance() {
        return ServiceManager.get(GameEndgameService.class);
    }

    private @NotNull EndgameRuntime runtime(@NotNull GameImpl game) {
        return runtimes.computeIfAbsent(game.getUuid(), uuid -> new EndgameRuntime());
    }

    // ================================================ bed destruction ================================================

    /**
     * Timeline BED_DESTRUCTION event: invalidates every target of every active team and announces it once.
     */
    public void destroyAllTargets(@NotNull GameImpl game) {
        destroyAllTargets(game, true, true);
    }

    /**
     * Invalidates every still valid target of every active team as if it was destroyed.
     *
     * @param announceAlways announce even when nothing was destroyed right now (as long as the arena uses destroyable targets)
     * @param showTitle      false when sudden death shows its own title right after
     * @return number of targets invalidated now
     */
    public int destroyAllTargets(@NotNull GameImpl game, boolean announceAlways, boolean showTitle) {
        if (game.getStatus() != GameStatus.RUNNING) {
            return 0;
        }
        var kinds = new ArrayList<TargetKind>();
        boolean destroyable = false;
        int destroyed = 0;
        for (var team : game.getActiveTeams()) { // a copy; teams in game only
            var target = team.getTarget();
            if (target instanceof TargetBlockImpl || target instanceof AExpirableTarget) {
                destroyable = true;
            }
            if (target == null || !target.isValid()) {
                continue; // NoTargetImpl is never valid
            }
            var kind = target instanceof TargetBlockImpl targetBlock
                    ? classify(targetBlock.getTargetBlock().getBlock().block()) // read BEFORE the block is removed
                    : TargetKind.NON_BLOCK;
            if (game.internalProcessInvalidation(team, target, null, TargetInvalidationReason.GAME_EVENT, true, false)) {
                destroyed++;
                kinds.add(kind);
            }
        }
        Debug.info(game.getName() + ": bed destruction invalidated " + destroyed + " target(s)");
        boolean announce = destroyed > 0 || (announceAlways && destroyable);
        if (announce && game.getConfigurationContainer().getOrDefault(GameConfigurationContainer.BED_DESTRUCTION_ANNOUNCE, true)) {
            announceTargetDestruction(game, TargetKind.aggregate(kinds), showTitle);
        }
        return destroyed;
    }

    static @NotNull TargetKind classify(@NotNull Block block) {
        if (BedUtils.isBedBlock(block)) {
            return TargetKind.BED;
        }
        if (block.isSameType("respawn_anchor")) {
            return TargetKind.ANCHOR;
        }
        if (block.isSameType("cake")) {
            return TargetKind.CAKE;
        }
        if (block.is("#doors")) {
            return TargetKind.DOOR;
        }
        return TargetKind.OTHER;
    }

    private void announceTargetDestruction(@NotNull GameImpl game, @NotNull TargetKind kind, boolean showTitle) {
        String[] titleKey = switch (kind) {
            case BED -> ForkLangKeys.IN_GAME_ENDGAME_BED_DESTRUCTION_TITLE_BED;
            case ANCHOR -> ForkLangKeys.IN_GAME_ENDGAME_BED_DESTRUCTION_TITLE_ANCHOR;
            case CAKE -> ForkLangKeys.IN_GAME_ENDGAME_BED_DESTRUCTION_TITLE_CAKE;
            default -> ForkLangKeys.IN_GAME_ENDGAME_BED_DESTRUCTION_TITLE_ANY;
        };
        String[] descriptionKey = switch (kind) { // existing keys
            case BED -> LangKeys.IN_GAME_TARGET_BLOCK_DESTROYED_ALL_BEDS;
            case ANCHOR -> LangKeys.IN_GAME_TARGET_BLOCK_DESTROYED_ALL_ANCHORS;
            case CAKE -> LangKeys.IN_GAME_TARGET_BLOCK_DESTROYED_ALL_CAKES;
            default -> LangKeys.IN_GAME_TARGET_BLOCK_DESTROYED_ALL_TARGET_BLOCKS;
        };
        var receivers = game.getConnectedPlayers(); // players and spectators of this game only
        if (showTitle) {
            Message.of(titleKey).join(descriptionKey).times(TitleUtils.defaultTimes()).title(receivers);
        }
        Message.of(ForkLangKeys.IN_GAME_ENDGAME_BED_DESTRUCTION_CHAT)
                .placeholder("title", Message.of(titleKey))
                .placeholder("description", Message.of(descriptionKey))
                .prefixOrDefault(game.getCustomPrefixComponent())
                .send(receivers);
        Broadcasts.sound(receivers, Broadcasts.configuredSound("bed_destruction", "entity.ender_dragon.growl"));
    }

    // ================================================== sudden death =================================================

    /**
     * Timeline SUDDEN_DEATH event. Idempotent per game run.
     */
    public void startSuddenDeath(@NotNull GameImpl game) {
        if (game.getStatus() != GameStatus.RUNNING) {
            return;
        }
        var rt = runtime(game);
        if (rt.suddenDeath != null) {
            return;
        }
        var settings = EndgameSettings.read(game);
        if (!settings.suddenDeathEnabled()) {
            Debug.info(game.getName() + ": sudden death disabled by configuration");
            return;
        }

        if (settings.suddenDeathDestroyTargets()) {
            destroyAllTargets(game, false, false); // chat only, and only if something was destroyed
        }

        var alive = game.getTeamsAlive();
        if (alive.size() < 2) {
            Debug.info(game.getName() + ": sudden death skipped, less than two teams alive");
            return;
        }

        int[] requested = new int[alive.size()];
        for (int i = 0; i < alive.size(); i++) {
            requested[i] = settings.dragonsPerTeam() + Math.max(0, DragonBuffUpgradeDefinition.getExtraDragons(alive.get(i))); // C2
        }
        rt.dragonBuffLocked = true; // the levels were read: a Dragon Buff bought from now on would change nothing
        int[] allocated = DragonAllocation.allocate(requested, settings.dragonMaxTotal());
        int total = Arrays.stream(allocated).sum();
        if (total <= 0) { // dragons-per-team 0 and no Dragon Buff: no session, watchdog, game rule change or announcement
            Debug.info(game.getName() + ": sudden death skipped, no dragons to spawn");
            return;
        }

        var session = new SuddenDeathSession(this, game, settings, dragons);
        rt.suddenDeath = session;
        if (settings.forceMobGriefing()) {
            acquireMobGriefing(session);
        }

        var points = DragonSpawnPlanner.plan(session.spawnCentre, total, session.ringRadius, session.flightBox);
        int point = 0;
        for (int i = 0; i < alive.size(); i++) {
            for (int k = 0; k < allocated[i]; k++) {
                session.spawnDragon(alive.get(i), points.get(point++), 0);
            }
        }
        if (session.size() == 0) { // per-team 0 without Dragon Buff, or every spawn failed: nothing to announce or watch
            stopSuddenDeath(game);
            Debug.warn(game.getName() + ": sudden death started without dragons", true);
            return;
        }
        session.startWatchdog();
        Debug.info(game.getName() + ": sudden death started with " + session.size() + " dragon(s)");
        announceSuddenDeath(game, settings, alive, requested, allocated, session.size());
    }

    private void announceSuddenDeath(@NotNull GameImpl game, @NotNull EndgameSettings settings, @NotNull List<TeamImpl> alive,
                                     int[] requested, int[] allocated, int spawned) {
        var receivers = game.getConnectedPlayers();
        Message.of(ForkLangKeys.IN_GAME_ENDGAME_SUDDEN_DEATH_TITLE).join(ForkLangKeys.IN_GAME_ENDGAME_SUDDEN_DEATH_SUBTITLE)
                .times(TitleUtils.defaultTimes())
                .title(receivers);
        Message.of(ForkLangKeys.IN_GAME_ENDGAME_SUDDEN_DEATH_CHAT)
                .placeholder("count", spawned)
                .prefixOrDefault(game.getCustomPrefixComponent())
                .send(receivers);
        for (int i = 0; i < alive.size(); i++) {
            if (requested[i] > settings.dragonsPerTeam()) { // the team has Dragon Buff
                Message.of(ForkLangKeys.IN_GAME_ENDGAME_SUDDEN_DEATH_DRAGON_BUFF)
                        .placeholder("count", allocated[i])
                        .prefixOrDefault(game.getCustomPrefixComponent())
                        .send(alive.get(i).getPlayers());
            }
        }
        Broadcasts.sound(receivers, Broadcasts.configuredSound("sudden_death", "entity.ender_dragon.growl"));
        if (settings.suddenDeathBossbar() && game.getStatusBar() instanceof BossBarImpl bossBar) {
            bossBar.setMessage(Message.of(ForkLangKeys.IN_GAME_ENDGAME_SUDDEN_DEATH_BOSSBAR).asComponent());
            bossBar.setColor(BossBarColor.RED);
        }
    }

    /**
     * Stops the sudden death of a game (dragons are removed synchronously when on the server thread). Idempotent.
     */
    public void stopSuddenDeath(@NotNull GameImpl game) {
        var rt = runtimes.get(game.getUuid());
        if (rt == null) {
            return;
        }
        var session = rt.suddenDeath;
        rt.suddenDeath = null;
        if (session != null) {
            session.stop();
        }
    }

    public boolean isSuddenDeathActive(@NotNull GameImpl game) {
        var rt = runtimes.get(game.getUuid());
        return rt != null && rt.suddenDeath != null;
    }

    /**
     * @return true once sudden death of this run has read the Dragon Buff levels (the dragon counts are fixed)
     */
    public boolean isDragonBuffLocked(@NotNull GameImpl game) {
        var rt = runtimes.get(game.getUuid());
        return rt != null && rt.dragonBuffLocked;
    }

    public boolean isManagedDragon(@Nullable Entity entity) {
        return entity != null && dragons.containsKey(entity.getUniqueId());
    }

    @NotNull Component ownerName(@NotNull TeamImpl team) {
        return Message.of(ForkLangKeys.IN_GAME_ENDGAME_DRAGON_NAME)
                .placeholder("team", Component.text(team.getName(), team.getColor().getTextColor()))
                .asComponent();
    }

    @NotNull Component neutralName() {
        return Message.of(ForkLangKeys.IN_GAME_ENDGAME_DRAGON_NAME_NEUTRAL).asComponent();
    }

    // The game boss bar is reset by existing code: runCycle Phase 7 clears the message when GAME_END_CELEBRATING starts and
    // prepareGameLobby/prepareGame set message and colour again for the next game.

    /**
     * Sets the world game rule {@code mob_griefing} to true for the duration of a sudden death (reference counted per
     * world, main thread only). The original value is also written to a file before the change, see {@link GameRuleRestoreStore}.
     */
    void acquireMobGriefing(@NotNull SuddenDeathSession session) {
        try {
            var world = session.game.getWorld();
            var worldName = world.getName();
            var override = mobGriefing.get(worldName);
            if (override == null) {
                var rule = GameRuleType.of("mob_griefing");
                Object previous = world.getGameRuleValue(rule);
                if (!Boolean.TRUE.equals(previous)) {
                    rememberGameRule(worldName, previous);
                    boolean changed = false;
                    try {
                        world.setGameRuleValue(rule, true);
                        changed = true;
                    } finally {
                        if (!changed) {
                            forgetGameRule(worldName); // nothing was overridden
                        }
                    }
                }
                override = new MobGriefingOverride();
                override.previous = previous;
                mobGriefing.put(worldName, override);
            }
            override.refs++; // only after the world is in the wanted state: a failed acquire leaves nothing to release
            session.mobGriefingAcquired = true;
        } catch (Throwable t) {
            Debug.warn("Could not change mob_griefing: " + t, true); // game rule API differences on very old versions
        }
    }

    void releaseMobGriefing(@NotNull SuddenDeathSession session) {
        try {
            if (!session.mobGriefingAcquired) {
                return;
            }
            session.mobGriefingAcquired = false;
            var world = session.game.getWorld();
            var override = mobGriefing.get(world.getName());
            if (override == null || --override.refs > 0) {
                return;
            }
            mobGriefing.remove(world.getName());
            if (override.previous != null && !Boolean.TRUE.equals(override.previous)) {
                world.setGameRuleValue(GameRuleType.of("mob_griefing"), override.previous);
            }
            forgetGameRule(world.getName()); // restored; if the call above threw, the entry stays for the next start
        } catch (Throwable t) {
            Debug.warn("Could not change mob_griefing: " + t, true);
        }
    }

    private @NotNull GameRuleRestoreStore gameRuleStore() {
        var store = gameRuleStore;
        if (store == null) {
            store = new GameRuleRestoreStore(BedWarsPlugin.getInstance().getPluginDescription().dataFolder().resolve("endgame-gamerules.properties"));
            gameRuleStore = store;
        }
        return store;
    }

    /**
     * Writes the value to restore BEFORE the rule is changed, so the file exists whenever the rule is overridden.
     */
    private void rememberGameRule(@NotNull String worldName, @Nullable Object previous) {
        if (!(previous instanceof Boolean value)) {
            return; // unknown or not a boolean: the in-memory restore still works
        }
        try {
            gameRuleStore().put(worldName, value);
        } catch (Throwable t) {
            Debug.warn("Could not save the original mob_griefing of " + worldName + ": " + t, true); // never breaks the sudden death
        }
    }

    private void forgetGameRule(@NotNull String worldName) {
        try {
            gameRuleStore().remove(worldName);
        } catch (Throwable t) {
            Debug.warn("Could not update the saved mob_griefing of " + worldName + ": " + t, true);
        }
    }

    /**
     * A crash, kill or power loss during a sudden death left {@code mob_griefing} overridden in the world data: puts the saved
     * original value back. Worlds that are not loaded yet are handled by {@link #onWorldLoad(WorldLoadEvent)}.
     */
    @OnPostEnable
    public void restoreLeftoverGameRules() {
        try {
            for (var worldName : gameRuleStore().load().keySet()) {
                restoreLeftoverGameRule(worldName);
            }
        } catch (Throwable t) {
            Debug.warn("Could not restore the saved mob_griefing values: " + t, true);
        }
    }

    @OnEvent
    public void onWorldLoad(@NotNull WorldLoadEvent event) {
        restoreLeftoverGameRule(event.world().getName());
    }

    private void restoreLeftoverGameRule(@NotNull String worldName) {
        try {
            if (mobGriefing.containsKey(worldName)) {
                return; // a sudden death of this run is overriding it right now
            }
            var saved = gameRuleStore().get(worldName);
            var world = saved != null ? Worlds.getWorld(worldName) : null;
            if (world == null) {
                return; // nothing saved, or the world is not loaded (yet)
            }
            world.setGameRuleValue(GameRuleType.of("mob_griefing"), saved);
            gameRuleStore().remove(worldName);
            Debug.info("Restored mob_griefing=" + saved + " of world " + worldName + " (left over from an interrupted sudden death)");
        } catch (Throwable t) {
            Debug.warn("Could not restore mob_griefing of " + worldName + ": " + t, true);
        }
    }

    // ================================================ game end by time ===============================================

    /**
     * Timeline GAME_END event (game-cycle thread, Phase 6.5: after the next status/countdown were committed).
     */
    public void endGameByTime(@NotNull GameImpl game) {
        endGameByTime(game, null);
    }

    /**
     * Also called from {@code GameCycleImpl.runCycle} Phase 5.1.2 when the arena time limit runs out ({@code tick != null}).
     * With a tick the next status/countdown are written into the tick (the cycle applies them in Phase 6); without a
     * tick they are written directly into the game.
     */
    public void endGameByTime(@NotNull GameImpl game, @Nullable GameTickEventImpl tick) {
        if (game.getStatus() != GameStatus.RUNNING) {
            return;
        }
        var rt = runtime(game);
        if (rt.endedByTime) {
            return;
        }
        rt.endedByTime = true;

        stopSuddenDeath(game);
        var settings = EndgameSettings.read(game);
        var alive = game.getTeamsAlive();
        int elapsed = elapsedSeconds(game);

        TeamImpl winner = null;
        TimeEndResolver.Criterion decidedBy = null;
        if (alive.size() == 1) {
            winner = alive.get(0); // last team standing at the same second
        } else if (alive.size() > 1 && settings.timeEndMode() == TimeEndMode.TIE_BREAK) {
            var result = TimeEndResolver.resolve(alive.stream().map(team -> standing(game, team)).toList(), settings.tieBreak());
            if (!result.isDraw()) {
                winner = alive.stream().filter(team -> team.getName().equals(result.winnerId())).findFirst().orElse(null);
                decidedBy = result.decidedBy();
            }
        }

        if (winner != null && game.getGameCycle() instanceof GameCycleImpl cycle) {
            if (decidedBy != null) {
                Message.of(ForkLangKeys.IN_GAME_ENDGAME_TIE_BREAK_CHAT)
                        .placeholder("team", Component.text(winner.getName(), winner.getColor().getTextColor()))
                        .placeholder("reason", Message.of(reasonKey(decidedBy)))
                        .prefixOrDefault(game.getCustomPrefixComponent())
                        .send(game.getConnectedPlayers());
                countLosses(game, alive, winner); // alive players of the other teams lose (like a normal defeat)
            }
            cycle.announceWinner(winner, elapsed, false); // titles, stats, economy, rewards, GameEndingEventImpl; no record
        } else {
            winner = null;
            announceDraw(game, settings);
        }
        EventManager.fire(new GameChangedStatusEventImpl(game));
        Debug.info(game.getName() + ": game ended by time (" + (winner != null ? "winner " + winner.getName() : "draw") + ")");

        if (tick != null) {
            tick.setNextCountdown(game.getPostGameWaiting());
            tick.setNextStatus(GameStatus.GAME_END_CELEBRATING);
        } else {
            game.setCountdown(game.getPostGameWaiting());
            game.setStatus(GameStatus.GAME_END_CELEBRATING);
        }
    }

    private static String @NotNull [] reasonKey(@NotNull TimeEndResolver.Criterion criterion) {
        return switch (criterion) {
            case TARGET -> ForkLangKeys.IN_GAME_ENDGAME_TIE_BREAK_REASON_TARGET;
            case PLAYERS -> ForkLangKeys.IN_GAME_ENDGAME_TIE_BREAK_REASON_PLAYERS;
            case KILLS -> ForkLangKeys.IN_GAME_ENDGAME_TIE_BREAK_REASON_KILLS;
            case FINAL_KILLS -> ForkLangKeys.IN_GAME_ENDGAME_TIE_BREAK_REASON_FINAL_KILLS;
        };
    }

    private static int elapsedSeconds(@NotNull GameImpl game) {
        long fromTimeline = GameTimelineService.getInstance().getElapsedSeconds(game); // -1 if the game has no timeline runtime
        return fromTimeline >= 0 ? (int) fromTimeline : Math.max(0, game.getGameTime() - game.getCountdown());
    }

    private static TimeEndResolver.@NotNull Standing standing(@NotNull GameImpl game, @NotNull TeamImpl team) {
        var timeline = GameTimelineService.getInstance().getTimeline(game);
        var stats = timeline != null ? timeline.getStats() : null;
        int kills = 0;
        int finals = 0;
        if (stats != null) {
            for (var member : team.getTeamMembers()) { // everybody who started in the team
                kills += stats.getKills(member.getUuid());
                finals += stats.getFinalKills(member.getUuid());
            }
        }
        var target = team.getTarget();
        return new TimeEndResolver.Standing(team.getName(), target != null && target.isValid(), team.countConnectedPlayers(), kills, finals);
    }

    private static void countLosses(@NotNull GameImpl game, @NotNull List<TeamImpl> alive, @NotNull TeamImpl winner) {
        if (!PlayerStatisticManager.isEnabled()) {
            return;
        }
        int loseScore = game.getConfigurationContainer().getOrDefault(GameConfigurationContainer.STATISTICS_SCORES_LOSE, 0);
        for (var team : alive) {
            if (team == winner) {
                continue;
            }
            for (var player : List.copyOf(team.getPlayers())) {
                var statistic = PlayerStatisticManager.getInstance().getStatistic(player);
                statistic.addLoses(1);
                statistic.addScore(loseScore);
            }
        }
    }

    private void announceDraw(@NotNull GameImpl game, @NotNull EndgameSettings settings) {
        var receivers = game.getConnectedPlayers();
        Message.of(ForkLangKeys.IN_GAME_ENDGAME_DRAW_TITLE).join(ForkLangKeys.IN_GAME_ENDGAME_DRAW_SUBTITLE)
                .times(TitleUtils.defaultTimes())
                .title(receivers);
        Message.of(ForkLangKeys.IN_GAME_ENDGAME_DRAW_CHAT)
                .prefixOrDefault(game.getCustomPrefixComponent())
                .send(receivers);
        Broadcasts.sound(receivers, Broadcasts.configuredSound("game_draw", "block.beacon.deactivate"));
        int loseScore = game.getConfigurationContainer().getOrDefault(GameConfigurationContainer.STATISTICS_SCORES_LOSE, 0);
        for (var team : game.getActiveTeams()) {
            for (var player : List.copyOf(team.getPlayers())) {
                if (settings.drawCountsAsLoss() && PlayerStatisticManager.isEnabled()) {
                    var statistic = PlayerStatisticManager.getInstance().getStatistic(player);
                    statistic.addLoses(1);
                    statistic.addScore(loseScore);
                }
                game.dispatchRewardCommands("player-game-draw", player, 0, team, null, null);
                if (StatisticsHolograms.isEnabled()) {
                    StatisticsHolograms.getInstance().updateHolograms(player);
                }
            }
        }
        EventManager.fire(new GameEndingEventImpl(game, null)); // API GameEndingEvent#getWinningTeam is @Nullable
    }

    // ============================================= events and cleanup ================================================

    @OnEvent
    public void onDragonPhaseChange(@NotNull EnderDragonChangePhaseEvent event) {
        var dragon = dragons.get(event.entity().getUniqueId());
        if (dragon == null) {
            return;
        }
        var phase = event.newPhase();
        if (phase == EnderDragonChangePhaseEvent.Phase.HOVER) {
            return;
        }
        event.cancelled(true); // the plugin flies the dragon; never let the vanilla AI take over
        if (phase == EnderDragonChangePhaseEvent.Phase.DYING && !dragon.slain) {
            dragon.slain = true; // lethal damage (only possible with invulnerable: false)
            var game = dragon.session.game;
            if (dragon.owner != null) {
                Message.of(ForkLangKeys.IN_GAME_ENDGAME_DRAGON_SLAIN)
                        .placeholder("team", Component.text(dragon.owner.getName(), dragon.owner.getColor().getTextColor()))
                        .prefixOrDefault(game.getCustomPrefixComponent())
                        .send(game.getConnectedPlayers());
            }
            var entity = dragon.entity;
            Tasker.run(entity, entity::remove); // next tick: do not remove the entity inside its own damage call
        }
    }

    @OnEvent
    public void onEntityDamage(@NotNull EntityDamageEvent event) {
        if (dragons.isEmpty() || event.cancelled()) { // fast path: no sudden death is running anywhere
            return;
        }
        var victimDragon = dragons.get(event.entity().getUniqueId());
        if (victimDragon != null) {
            handleDamageToDragon(victimDragon, event);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            var attacker = dragons.get(byEntity.damager().getUniqueId());
            if (attacker == null) {
                return;
            }
            if (!(event.entity() instanceof Player player)
                    || !PlayerManagerImpl.getInstance().isPlayerInGame(player)
                    || player.as(BedWarsPlayer.class).getGame() != attacker.session.game) {
                event.cancelled(true); // shop villagers, golems, armor stands, other dragons, players outside this arena
            }
            // players of this arena: decided by PlayerListener#onDamage (order LAST) -> handleDragonDamageToPlayer
        }
    }

    private void handleDamageToDragon(@NotNull ManagedDragon dragon, @NotNull EntityDamageEvent event) {
        if (dragon.session.settings.dragonInvulnerable()) {
            event.cancelled(true);
            return;
        }
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            return; // void, explosions ... are allowed
        }
        var damager = byEntity.damager();
        Player player = null;
        if (damager instanceof Player p) {
            player = p;
        } else if (damager instanceof ProjectileEntity projectile && projectile.getShooter() instanceof Player shooter) {
            player = shooter;
        }
        if (player == null) {
            return;
        }
        if (!PlayerManagerImpl.getInstance().isPlayerInGame(player)) {
            event.cancelled(true);
            return;
        }
        var bedWarsPlayer = player.as(BedWarsPlayer.class);
        if (bedWarsPlayer.isSpectator()
                || bedWarsPlayer.getGame() != dragon.session.game
                || (!dragon.isNeutral() && dragon.session.game.getPlayerTeam(bedWarsPlayer) == dragon.owner)) {
            event.cancelled(true); // nobody hurts their own team's dragon
        }
    }

    /**
     * Called by {@code PlayerListener#onDamage} (order LAST) for in-game players of a running game hit by a managed dragon.
     */
    public void handleDragonDamageToPlayer(@NotNull GameImpl game, @NotNull BedWarsPlayer victim, @NotNull EntityDamageByEntityEvent event) {
        var dragon = dragons.get(event.damager().getUniqueId());
        if (dragon == null) {
            return;
        }
        if (dragon.session.game != game || game.getStatus() != GameStatus.RUNNING) {
            event.cancelled(true);
            return;
        }
        var victimTeam = game.getPlayerTeam(victim);
        if (victimTeam == null) {
            event.cancelled(true);
            return;
        }
        var settings = dragon.session.settings;
        if (!settings.dragonDamageOwnTeam() && !dragon.isNeutral() && dragon.owner == victimTeam) {
            event.cancelled(true);
            return;
        }
        if (settings.dragonDamageMultiplier() != 1.0) {
            event.damage(event.damage() * settings.dragonDamageMultiplier());
        }
    }

    @OnEvent
    public void onDragonExplode(@NotNull EntityExplodeEvent event) {
        if (event.cancelled()) {
            return;
        }
        var dragon = dragons.get(event.entity().getUniqueId());
        if (dragon == null) {
            return; // WorldListener handles every other explosion
        }
        var session = dragon.session;
        var game = session.game;
        var mode = session.settings.blockDestruction();
        if (session.stopped || game.getStatus() != GameStatus.RUNNING || mode == DragonBlockPolicy.Mode.NONE) {
            event.cancelled(true);
            return;
        }
        var immune = session.settings.immuneBlocks().toArray();
        var region = game.getRegion();
        event.blocks().removeIf(block -> { // true = keep the block (remove it from the destroy list)
            var location = block.location();
            boolean inside = game.isLocationInArena(location);
            var type = block.block();
            boolean isImmune = immune.length > 0 && type.is(immune);
            boolean protectedBlock = inside && isProtectedBlock(game, block);
            boolean placed = region.isLocationModifiedDuringGame(location);
            var snapshot = (inside && !isImmune && !protectedBlock && !placed && mode == DragonBlockPolicy.Mode.ALL)
                    ? block.blockSnapshot()
                    : null;
            return switch (DragonBlockPolicy.decide(mode, inside, isImmune, protectedBlock, placed, snapshot != null)) {
                case KEEP -> true;
                case DESTROY_AND_RECORD -> {
                    region.putOriginalBlockIfAbsent(location, snapshot);
                    yield false;
                }
                case DESTROY -> false; // placed during the game: already tracked, the regen sets air
            };
        });
        event.yield(0f); // no drops
    }

    /**
     * Target blocks (both halves of beds and doors) and team chests are never broken by dragons.
     */
    private static boolean isProtectedBlock(@NotNull GameImpl game, @NotNull BlockPlacement block) {
        var location = block.location();
        if (game.getTeamOfTargetBlock(location) != null || game.getTeamOfChest(location) != null) {
            return true;
        }
        var type = block.block();
        if (BedUtils.isBedBlock(type)) { // the target is registered with one half only
            var neighbor = BedUtils.getBedNeighbor(block);
            return neighbor != null && game.getTeamOfTargetBlock(neighbor.location()) != null;
        }
        if (type.is("#doors")) {
            return game.getTeamOfTargetBlock(location.add(0, 1, 0)) != null
                    || game.getTeamOfTargetBlock(location.add(0, -1, 0)) != null;
        }
        return false;
    }

    @OnEvent
    public void onGameEnding(@NotNull GameEndingEventImpl event) { // winner found (or our draw)
        stopSuddenDeath(event.getGame());
    }

    @OnEvent
    public void onGameStarted(@NotNull GameStartedEventImpl event) { // fresh state per run
        cleanup(event.getGame());
    }

    @OnEvent
    public void onPreRebuild(@NotNull PreRebuildingEventImpl event) { // before RegionImpl#regen
        cleanup(event.getGame());
    }

    /**
     * Stops the sudden death and forgets all per-game state; also sweeps dragons of this game that lost their session.
     */
    public void cleanup(@NotNull GameImpl game) {
        stopSuddenDeath(game);
        runtimes.remove(game.getUuid());
        for (var dragon : List.copyOf(dragons.values())) {
            if (dragon.session.game == game) {
                dragon.session.stop();
            }
        }
    }

    @OnPreDisable
    public void onPreDisable() {
        shutdown();
    }

    @OnDisable
    public void onDisable() {
        shutdown();
    }

    private void shutdown() {
        for (var rt : runtimes.values()) {
            var session = rt.suddenDeath;
            rt.suddenDeath = null;
            if (session != null) {
                session.stop();
            }
        }
        for (var dragon : List.copyOf(dragons.values())) {
            dragon.session.stop();
        }
        runtimes.clear();
        dragons.clear();
    }
}
