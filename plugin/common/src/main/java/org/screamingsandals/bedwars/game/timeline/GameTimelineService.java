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

package org.screamingsandals.bedwars.game.timeline;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.api.config.GameConfigurationContainer;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.events.PlayerKilledEventImpl;
import org.screamingsandals.bedwars.events.PostTargetInvalidatedEventImpl;
import org.screamingsandals.bedwars.events.PreRebuildingEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lib.debug.Debug;
import org.screamingsandals.bedwars.utils.TimeFormat;
import org.screamingsandals.lib.event.OnEvent;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.sender.CommandSender;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.methods.OnDisable;
import org.screamingsandals.lib.utils.logger.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns one {@link GameTimeline} per running game: generator tiers, timed bed destruction / sudden death / game end,
 * the sidebar {@code <tier>} text and the per-game kill / final kill / destroyed target counters.
 */
@Service
@RequiredArgsConstructor
public class GameTimelineService {
    private static final GameSessionStats NO_STATS = new GameSessionStats();

    private final @NotNull Logger logger;
    private final @NotNull Map<UUID, GameTimeline> runtimes = new ConcurrentHashMap<>();

    public static @NotNull GameTimelineService getInstance() {
        return ServiceManager.get(GameTimelineService.class);
    }

    /**
     * GameCycleImpl.prepareGame (successful start only). Creates a fresh runtime for every game run.
     */
    public void startGame(@NotNull GameImpl game) {
        var variant = game.getGameVariant();
        var definition = variant != null ? variant.getTimeline() : TimelineDefinition.EMPTY;
        if (!game.getConfigurationContainer().getOrDefault(GameConfigurationContainer.TIMELINE_ENABLED, true)) {
            definition = TimelineDefinition.EMPTY;
        }
        var runtime = new GameTimeline(game, definition); // the counters exist even with an EMPTY timeline (default variant)
        var old = runtimes.put(game.getUuid(), runtime);
        if (old != null) {
            old.stop();
        }
        try {
            runtime.start();
        } catch (Throwable t) {
            logger.error("Could not apply the tier I generator intervals in game " + game.getName(), t);
        }
        Debug.info(game.getName() + ": timeline started with " + runtime.getState().getEvents().size() + " events");
    }

    /**
     * GameCycleImpl.runCycle (Phase 6.5), once per second while the game stays RUNNING.
     */
    public void tickRunning(@NotNull GameImpl game) {
        var runtime = runtimes.get(game.getUuid());
        if (runtime != null) {
            runtime.tick();
        }
    }

    public @Nullable GameTimeline getTimeline(@NotNull GameImpl game) {
        return runtimes.get(game.getUuid());
    }

    /**
     * @return seconds of RUNNING time, or -1 when the game has no timeline runtime
     */
    public long getElapsedSeconds(@NotNull GameImpl game) {
        var runtime = runtimes.get(game.getUuid());
        return runtime == null ? -1 : runtime.getState().getElapsedSeconds();
    }

    // ---- sidebar (called from GameSidebar's async update task; everything read here is thread-safe) ----

    /**
     * Text of the sidebar {@code <tier>} placeholder: next timeline event with a countdown, or the arena time limit.
     */
    public @NotNull Component renderSidebarTier(@NotNull GameImpl game, @Nullable CommandSender viewer) {
        var runtime = runtimes.get(game.getUuid());
        var view = SidebarTierView.resolve(
                runtime != null ? runtime.getState() : null,
                game.getStatus() == GameStatus.RUNNING,
                game.getCountdown()
        );
        Message msg;
        switch (view.kind()) {
            case NEXT_EVENT -> {
                if (runtime == null || view.event() == null) {
                    msg = Message.of(ForkLangKeys.IN_GAME_TIMELINE_SIDEBAR_NONE);
                } else {
                    msg = Message.of(ForkLangKeys.IN_GAME_TIMELINE_SIDEBAR_NEXT_EVENT)
                            .placeholder("event", runtime.displayName(view.event()))
                            .placeholder("time", TimeFormat.formatClock(view.seconds()));
                }
            }
            case TIME_LIMIT -> msg = Message.of(ForkLangKeys.IN_GAME_TIMELINE_SIDEBAR_TIME_LIMIT)
                    .placeholder("time", TimeFormat.formatClock(view.seconds()));
            case GAME_OVER -> msg = Message.of(ForkLangKeys.IN_GAME_TIMELINE_SIDEBAR_GAME_OVER);
            default -> msg = Message.of(ForkLangKeys.IN_GAME_TIMELINE_SIDEBAR_NONE);
        }
        return msg.asComponent(viewer);
    }

    public int getKills(@NotNull GameImpl game, @Nullable CommandSender viewer) {
        return stats(game).getKills(uuidOf(viewer));
    }

    public int getFinalKills(@NotNull GameImpl game, @Nullable CommandSender viewer) {
        return stats(game).getFinalKills(uuidOf(viewer));
    }

    public int getTargetBlocksDestroyed(@NotNull GameImpl game, @Nullable CommandSender viewer) {
        return stats(game).getTargetBlocksDestroyed(uuidOf(viewer));
    }

    private @NotNull GameSessionStats stats(@NotNull GameImpl game) {
        var runtime = runtimes.get(game.getUuid());
        return runtime != null ? runtime.getStats() : NO_STATS;
    }

    private static @Nullable UUID uuidOf(@Nullable CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    // ---- listeners ----

    @OnEvent
    public void onPlayerKilled(@NotNull PlayerKilledEventImpl event) {
        var game = event.getGame();
        var killer = event.getKiller();
        var victim = event.getPlayer();
        if (killer == null || killer.equals(victim) || killer.getGame() != game) {
            return;
        }
        var runtime = runtimes.get(game.getUuid());
        if (runtime == null) {
            return;
        }
        // PlayerListener removes a victim without a bed from the team BEFORE it fires PlayerKilledEventImpl,
        // so a victim without a team is a final kill
        boolean finalKill = game.getPlayerTeam(victim) == null;
        runtime.getStats().recordKill(killer.getUniqueId(), finalKill);
    }

    @OnEvent
    public void onTargetInvalidated(@NotNull PostTargetInvalidatedEventImpl event) {
        var initiator = event.getInitiator(); // null for TIMEOUT / bed destruction by the timeline / explosions without an owner
        if (initiator == null) {
            return;
        }
        var runtime = runtimes.get(event.getGame().getUuid());
        if (runtime == null) {
            return;
        }
        runtime.getStats().recordTargetDestroyed(initiator.getUniqueId());
    }

    @OnEvent
    public void onPreRebuild(@NotNull PreRebuildingEventImpl event) {
        var runtime = runtimes.remove(event.getGame().getUuid());
        if (runtime != null) {
            runtime.stop();
        }
    }

    @OnDisable
    public void onDisable() {
        runtimes.values().forEach(GameTimeline::stop);
        runtimes.clear();
    }
}
