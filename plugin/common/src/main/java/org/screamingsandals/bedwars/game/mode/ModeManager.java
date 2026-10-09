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

package org.screamingsandals.bedwars.game.mode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.setup.clone.ArenaCloneService;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.tasker.DefaultThreads;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.tasker.task.Task;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.ServiceDependencies;
import org.screamingsandals.lib.utils.annotations.methods.OnEnable;
import org.screamingsandals.lib.utils.annotations.methods.OnPostEnable;
import org.screamingsandals.lib.utils.annotations.methods.OnPreDisable;
import org.screamingsandals.lib.utils.logger.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Loads the configured game modes and gives read access to them and to their arena statistics.
 */
@Service
@ServiceDependencies(dependsOn = {
        MainConfig.class
})
@RequiredArgsConstructor
public class ModeManager {
    private final @NotNull Logger logger;
    /**
     * Insertion-ordered, unmodifiable map; replaced as a whole on (re)load, never mutated (D53).
     */
    private volatile Map<String, ModeDefinition> modes = Map.of();
    @Getter
    private volatile boolean enabled;
    @Getter
    private volatile boolean queueEnabled;
    @Getter
    private volatile long queueMaxWaitMillis;
    private volatile long claimTimeoutMillis = 10_000L;
    private @Nullable Task task;

    public static @NotNull ModeManager getInstance() {
        return ServiceManager.get(ModeManager.class);
    }

    /**
     * Safe in any context (false if the service is not loaded).
     */
    public static boolean isModesEnabled() {
        return ServiceManager.getOptional(ModeManager.class).map(ModeManager::isEnabled).orElse(false);
    }

    public static @NotNull Component displayNameComponent(@Nullable ModeDefinition mode) {
        return mode == null ? Component.empty() : Component.fromMiniMessage(mode.displayName());
    }

    /**
     * Largest party (or other group) led by {@code partyLeader} that can still join the WAITING mode lobby of the game;
     * -1 = no mode limit; 0 = the lobby cannot take anybody else (D49).
     */
    public static int largestJoinableGroup(@NotNull GameImpl game, @NotNull UUID partyLeader) {
        return TeamAssignment.largestJoinableGroup(game, "party:" + partyLeader);
    }

    @OnEnable
    public void onEnable() {
        var config = MainConfig.getInstance();
        var result = ModeConfigParser.parse(config.node("modes", "list"));
        result.warnings().forEach(w -> logger.warn("[modes] {}", w));
        var loaded = new LinkedHashMap<String, ModeDefinition>();
        result.modes().forEach(m -> loaded.put(m.id(), m));
        modes = Collections.unmodifiableMap(loaded);
        enabled = config.node("modes", "enabled").getBoolean(true);
        claimTimeoutMillis = Math.max(1, config.node("modes", "claim-timeout-seconds").getInt(10)) * 1000L;
        queueEnabled = config.node("modes", "queue", "enabled").getBoolean(false);
        queueMaxWaitMillis = Math.max(0, config.node("modes", "queue", "max-wait-seconds").getInt(300)) * 1000L;
        logger.info("Loaded {} BedWars mode(s)", loaded.size());
    }

    @OnPostEnable
    public void onPostEnable() {
        cancelTask();
        task = Tasker.runRepeatedly(DefaultThreads.GLOBAL_THREAD, this::tick, 1, TaskerTime.SECONDS);
    }

    @OnPreDisable
    public void onPreDisable() {
        cancelTask();
    }

    private void cancelTask() {
        if (task != null) {
            if (task.isScheduledOrRunning()) {
                task.cancel();
            }
            task = null;
        }
    }

    public @NotNull List<ModeDefinition> getModes() {
        return List.copyOf(modes.values());
    }

    public @NotNull List<String> getModeIds() {
        return List.copyOf(modes.keySet());
    }

    public @NotNull Optional<ModeDefinition> getMode(@Nullable String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(modes.get(id.trim().toLowerCase(Locale.ROOT)));
    }

    /**
     * D37: whether any configured mode could claim this arena (team count, allowed team sizes and arena whitelist).
     */
    public boolean hasModeFor(@NotNull GameImpl game) {
        var current = modes;
        if (current.isEmpty()) {
            return false;
        }
        var allowed = game.getConfigurationContainer().getOrDefault(ModeConfigKeys.ALLOWED_TEAM_SIZES, ModeConfigKeys.DEFAULT_ALLOWED_TEAM_SIZES);
        int teamCount = game.getTeams().size();
        return current.values().stream()
                .anyMatch(m -> m.teamCount() == teamCount && allowed.contains(m.teamSize()) && m.allowsArena(game.getName()));
    }

    public @NotNull List<ArenaSnapshot> snapshotArenas() {
        return snapshotArenas(false);
    }

    public @NotNull List<ArenaSnapshot> snapshotArenas(boolean withGroups) {
        return GameManagerImpl.getInstance().getLocalGames().stream()
                .map(game -> snapshot(game, withGroups))
                .collect(Collectors.toList());
    }

    static @NotNull ArenaSnapshot snapshot(@NotNull GameImpl game, boolean withGroups) {
        ArenaState state;
        var status = game.getStatus();
        if (ServiceManager.getOptional(ArenaCloneService.class).map(s -> s.isLocked(game.getUuid())).orElse(false)) {
            state = ArenaState.UNAVAILABLE;
        } else if (status == GameStatus.WAITING && !game.isPreparing() && !game.isInEditMode()) {
            state = ArenaState.WAITING;
        } else if (status == GameStatus.RUNNING || status == GameStatus.GAME_END_CELEBRATING || status == GameStatus.REBUILDING) {
            state = ArenaState.BUSY;
        } else {
            state = ArenaState.UNAVAILABLE;
        }
        var cc = game.getConfigurationContainer();
        var mode = game.getActiveMode();
        List<Integer> groupSizes = List.of();
        if (withGroups && mode != null && state == ArenaState.WAITING) {
            var connected = game.getConnectedPlayers().stream().filter(p -> !p.isSpectator()).collect(Collectors.toList());
            groupSizes = TeamAssignment.buildGroups(game, connected, false).stream().map(PlayerGroup::size).collect(Collectors.toList());
        }
        return new ArenaSnapshot(game.getUuid().toString(), game.getName(), state, game.countConnectedPlayers(),
                mode == null ? null : mode.id(), game.getTeams().size(),
                cc.getOrDefault(ModeConfigKeys.ALLOWED_TEAM_SIZES, ModeConfigKeys.DEFAULT_ALLOWED_TEAM_SIZES).stream().filter(Objects::nonNull).collect(Collectors.toSet()),
                cc.getOrDefault(ModeConfigKeys.PREFERRED_TEAM_SIZES, List.of()).stream().filter(Objects::nonNull).collect(Collectors.toSet()),
                groupSizes);
    }

    public @NotNull ModeStats getStats(@NotNull ModeDefinition mode) {
        int waiting = 0;
        int playing = 0;
        for (var game : GameManagerImpl.getInstance().getLocalGames()) {
            var active = game.getActiveMode();
            if (active == null || !active.id().equals(mode.id())) {
                continue;
            }
            switch (game.getStatus()) {
                case WAITING:
                    waiting += game.countConnectedPlayers();
                    break;
                case RUNNING:
                case GAME_END_CELEBRATING:
                    playing += game.countConnectedPlayers();
                    break;
                default:
                    break;
            }
        }
        return new ModeStats(waiting, playing, ModeArenaSelector.countJoinable(snapshotArenas(false), mode));
    }

    public @NotNull ModeStats getStats(@NotNull String modeId) {
        return getMode(modeId).map(this::getStats).orElse(ModeStats.EMPTY);
    }

    public int countAllPlayers() {
        return GameManagerImpl.getInstance().getLocalGames().stream().mapToInt(GameImpl::countConnectedPlayers).sum();
    }

    /**
     * Every second: sweeps stale claims and processes the optional queue. (D34: NPC holograms are refreshed by NPCManager.)
     */
    private void tick() {
        try {
            sweepStaleClaims();
            ServiceManager.getOptional(ModeJoinService.class).ifPresent(ModeJoinService::processQueue);
        } catch (Throwable t) {
            logger.warn("Mode tick failed", t);
        }
    }

    /**
     * Releases arenas that were claimed but nobody entered (join refused, fee, cancelled event, deferred join that failed).
     */
    private void sweepStaleClaims() {
        long now = System.currentTimeMillis();
        for (var game : GameManagerImpl.getInstance().getLocalGames()) {
            if (game.getActiveMode() != null && game.getStatus() == GameStatus.WAITING && game.countConnectedPlayers() == 0
                    && !game.isPreparing() && now - game.getActiveModeClaimedAt() > claimTimeoutMillis) {
                game.clearActiveMode();
            }
        }
    }
}
