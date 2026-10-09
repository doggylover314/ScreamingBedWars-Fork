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

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.events.ArenaModeChangedEventImpl;
import org.screamingsandals.bedwars.events.PlayerJoinedEventImpl;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.lib.event.OnEvent;
import org.screamingsandals.lib.event.player.PlayerLeaveEvent;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.player.Players;
import org.screamingsandals.lib.tasker.DefaultThreads;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.ServiceDependencies;
import org.screamingsandals.lib.utils.annotations.methods.OnPreDisable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Joins players into a game mode (NPC JOIN_MODE, /bw mode join, games inventory join_mode).
 */
@Service
@ServiceDependencies(dependsOn = {
        ModeManager.class
})
@RequiredArgsConstructor
public class ModeJoinService {
    private static final long CLICK_COOLDOWN_MILLIS = 500;
    private final @NotNull ModeManager modeManager;
    private final ModeQueue queue = new ModeQueue();
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private final RandomGenerator random = new Random();

    public enum JoinOutcome {
        JOINED, NO_ARENA, REFUSED
    }

    public static @NotNull ModeJoinService getInstance() {
        return ServiceManager.get(ModeJoinService.class);
    }

    /**
     * NPC JOIN_MODE, /bw mode join, games inventory join_mode. MUST run on DefaultThreads.GLOBAL_THREAD.
     */
    public void joinMode(@NotNull Player player, @Nullable String modeId) {
        long now = System.currentTimeMillis();
        var last = lastClick.put(player.getUuid(), now);
        if (last != null && now - last < CLICK_COOLDOWN_MILLIS) {
            return; // double click / NPC spam
        }
        if (!modeManager.isEnabled() || GameImpl.isBungeeEnabled()) { // D48: in bungee mode the hub picks the arena
            player.sendMessage(Message.of(ForkLangKeys.COMMON_FEATURE_DISABLED).defaultPrefix());
            return;
        }
        var mode = modeManager.getMode(modeId);
        if (mode.isEmpty()) {
            player.sendMessage(Message.of(ForkLangKeys.MODES_UNKNOWN_MODE).defaultPrefix().placeholderRaw("mode", String.valueOf(modeId)));
            return;
        }
        if (PlayerManagerImpl.getInstance().isPlayerInGame(player)) {
            player.sendMessage(Message.of(LangKeys.IN_GAME_ERRORS_ALREADY_IN_GAME).defaultPrefix());
            return;
        }
        var outcome = tryJoin(player, mode.get());
        if (outcome != JoinOutcome.NO_ARENA) {
            queue.remove(player.getUuid());
            return;
        }
        if (!modeManager.isQueueEnabled()) {
            player.sendMessage(Message.of(ForkLangKeys.MODES_NO_ARENA_AVAILABLE).defaultPrefix()
                    .placeholder("mode", ModeManager.displayNameComponent(mode.get())));
            return;
        }
        boolean already = queue.modeOf(player.getUuid()).map(mode.get().id()::equals).orElse(false);
        int position = queue.enqueue(player.getUuid(), mode.get().id(), now);
        player.sendMessage(Message.of(already ? ForkLangKeys.MODES_QUEUE_ALREADY : ForkLangKeys.MODES_QUEUE_JOINED).defaultPrefix()
                .placeholder("mode", ModeManager.displayNameComponent(mode.get()))
                .placeholder("position", position));
    }

    @NotNull JoinOutcome tryJoin(@NotNull Player player, @NotNull ModeDefinition mode) {
        var uuid = player.getUuid();
        var bwPlayer = PlayerManagerImpl.getInstance().getPlayerOrCreate(player);
        int seats = 1;
        boolean partyJoin = false;
        var party = ModePartyBridge.partyOf(uuid);
        if (party.isPresent() && party.get().onlineSize() > 1) {
            if (party.get().leader().equals(uuid)) {
                seats = Math.max(1, ModePartyBridge.seatsFor(bwPlayer)); // D50
                partyJoin = true;
            } else if (!ModePartyBridge.mayJoinOnOwn(bwPlayer)) { // D32
                player.sendMessage(Message.of(ForkLangKeys.MODES_PARTY_LEADER_ONLY).defaultPrefix());
                return JoinOutcome.REFUSED;
            }
        }
        var result = ModeArenaSelector.select(modeManager.snapshotArenas(true), mode, seats, random);
        if (result.outcome() == SelectionResult.Outcome.PARTY_TOO_LARGE) {
            player.sendMessage(Message.of(ForkLangKeys.MODES_PARTY_TOO_LARGE).defaultPrefix()
                    .placeholder("mode", ModeManager.displayNameComponent(mode))
                    .placeholder("size", seats)
                    .placeholder("team-size", mode.teamSize()));
            return JoinOutcome.REFUSED;
        }
        if (result.outcome() == SelectionResult.Outcome.NONE_AVAILABLE || result.arenaId() == null) {
            return JoinOutcome.NO_ARENA;
        }
        var game = GameManagerImpl.getInstance().getLocalGame(UUID.fromString(result.arenaId())).orElse(null);
        if (game == null) {
            return JoinOutcome.NO_ARENA;
        }
        boolean claimed = false;
        if (result.outcome() == SelectionResult.Outcome.CLAIM_IDLE) {
            if (!game.applyMode(mode)) {
                return JoinOutcome.NO_ARENA; // state changed meanwhile; next click/tick retries
            }
            claimed = true;
        }
        player.sendMessage(Message.of(ForkLangKeys.MODES_JOINING).defaultPrefix()
                .placeholder("mode", ModeManager.displayNameComponent(mode))
                .placeholder("arena", game.getDisplayNameComponent()));
        if (partyJoin) {
            ModePartyBridge.joinWithParty(bwPlayer, game);
        } else {
            game.joinToGame(bwPlayer);
        }
        if (claimed && game.countConnectedPlayers() == 0 && !game.isPreparing()) {
            game.clearActiveMode(); // join refused (fee, cancelled event, party rule)
        }
        return bwPlayer.isInGame() ? JoinOutcome.JOINED : JoinOutcome.REFUSED;
    }

    /**
     * OPTIONAL queue: called every second by ModeManager and 1 tick after an arena lost its mode.
     */
    public void processQueue() {
        if (queue.isEmpty()) {
            return;
        }
        if (!modeManager.isQueueEnabled()) {
            queue.clear();
            return;
        }
        long now = System.currentTimeMillis();
        for (var expired : queue.removeExpired(now, modeManager.getQueueMaxWaitMillis())) {
            var p = Players.getPlayer(expired.player());
            if (p != null) {
                p.sendMessage(Message.of(ForkLangKeys.MODES_QUEUE_EXPIRED).defaultPrefix()
                        .placeholder("mode", ModeManager.displayNameComponent(modeManager.getMode(expired.modeId()).orElse(null))));
            }
        }
        for (var modeId : queue.modesWithEntries()) {
            var mode = modeManager.getMode(modeId);
            if (mode.isEmpty()) {
                queue.removeMode(modeId);
                continue;
            }
            for (var entry : queue.entries(modeId)) { // FIFO; no head-of-line blocking
                var p = Players.getPlayer(entry.player());
                if (p == null || PlayerManagerImpl.getInstance().isPlayerInGame(entry.player())) {
                    queue.remove(entry.player());
                    continue;
                }
                if (tryJoin(p, mode.get()) != JoinOutcome.NO_ARENA) {
                    queue.remove(entry.player());
                }
            }
        }
    }

    public void leaveQueue(@NotNull Player player) {
        var mode = queue.remove(player.getUuid());
        player.sendMessage(mode.isEmpty()
                ? Message.of(ForkLangKeys.MODES_QUEUE_NOT_QUEUED).defaultPrefix()
                : Message.of(ForkLangKeys.MODES_QUEUE_LEFT).defaultPrefix()
                        .placeholder("mode", ModeManager.displayNameComponent(modeManager.getMode(mode.get()).orElse(null))));
    }

    @OnEvent
    public void onServerLeave(PlayerLeaveEvent event) { // org.screamingsandals.lib.event.player.PlayerLeaveEvent
        queue.remove(event.player().getUuid());
        lastClick.remove(event.player().getUuid());
    }

    @OnEvent
    public void onJoinedGame(PlayerJoinedEventImpl event) {
        queue.remove(event.getPlayer().getUuid());
    }

    @OnEvent
    public void onArenaModeChanged(ArenaModeChangedEventImpl event) {
        if (event.getNewMode() == null && !queue.isEmpty()) {
            Tasker.runDelayed(DefaultThreads.GLOBAL_THREAD, this::processQueue, 1, TaskerTime.TICKS);
        }
    }

    @OnPreDisable
    public void onPreDisable() {
        queue.clear();
        lastClick.clear();
    }
}
