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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.BedWarsPlugin;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.events.GameTimelineEventImpl;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.ItemSpawnerImpl;
import org.screamingsandals.bedwars.game.endgame.GameEndgameService;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lib.debug.Debug;
import org.screamingsandals.bedwars.utils.RomanNumerals;
import org.screamingsandals.bedwars.utils.TimeFormat;
import org.screamingsandals.bedwars.utils.TitleUtils;
import org.screamingsandals.lib.event.EventManager;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.spectator.sound.SoundSource;
import org.screamingsandals.lib.spectator.sound.SoundStart;

import java.util.List;

/**
 * Runtime of the variant timeline for ONE game run: owns the pure {@link TimelineState} (clock + next event) and the
 * {@link GameSessionStats}, fires due events and announces them.
 * <p>
 * Created by {@link GameTimelineService#startGame(GameImpl)}, dropped when the game is rebuilt.
 */
public final class GameTimeline {
    private final @NotNull GameImpl game;
    private final @NotNull TimelineDefinition definition;
    @Getter
    private final @NotNull TimelineState state;
    @Getter
    private final @NotNull GameSessionStats stats = new GameSessionStats();
    private volatile boolean stopped;

    GameTimeline(@NotNull GameImpl game, @NotNull TimelineDefinition definition) {
        this.game = game;
        this.definition = definition;
        // tier events of spawner types the arena does not have are dropped (no "Diamond II" on maps without diamond generators)
        this.state = new TimelineState(definition.isEmpty() ? List.of()
                : definition.events().stream()
                        .filter(e -> e.type() != TimelineEventType.SPAWNER_TIER || !matchingSpawners(e).isEmpty())
                        .toList());
    }

    /**
     * @return the spawners of the event's type; spawners linked to a team only if the event includes them
     */
    @NotNull List<ItemSpawnerImpl> matchingSpawners(@NotNull TimelineEventDefinition e) {
        var type = e.spawnerType();
        if (type == null) {
            return List.of();
        }
        return game.getItemSpawners().stream()
                .filter(s -> s.getItemSpawnerType().configKey().equalsIgnoreCase(type))
                .filter(s -> e.includeTeamSpawners() || s.getTeam() == null)
                .toList();
    }

    /**
     * Called once right after the spawners were started (GameCycleImpl.prepareGame): applies tier I intervals.
     */
    void start() {
        if (definition.isEmpty()) {
            return;
        }
        for (var spawner : game.getItemSpawners()) {
            if (spawner.getTeam() != null || spawner.getInitialInterval() != null) {
                continue; // team generators are not on the tier ladder; an admin interval overrides tier I
            }
            var t1 = definition.intervalTicks(spawner.getItemSpawnerType().configKey(), 1);
            if (t1 != null) {
                spawner.getLocation().tasker().run(() -> spawner.restartWithInterval(t1)); // full tier I cycle, not min(old cycle left, tier I)
            }
        }
    }

    /**
     * Once per game-cycle second while the game stays RUNNING.
     */
    void tick() {
        if (stopped) {
            return;
        }
        for (var event : state.advance()) {
            if (stopped || game.getStatus() != GameStatus.RUNNING) {
                // still consumed, so every event happens at most once
                Debug.info(game.getName() + ": timeline event " + event.id() + " consumed without effect (game no longer running)");
                continue;
            }
            fire(event);
        }
    }

    void stop() {
        stopped = true;
        stats.clear();
    }

    private void fire(@NotNull TimelineEventDefinition event) {
        var apiEvent = new GameTimelineEventImpl(game, event.id(), event.type().configName(), (int) event.timeSeconds(), event.spawnerType(), event.tier());
        EventManager.fire(apiEvent);
        if (apiEvent.isCancelled()) {
            Debug.info(game.getName() + ": timeline event " + event.id() + " was cancelled");
            return;
        }
        Debug.info(game.getName() + ": timeline event " + event.id() + " (" + event.type().configName() + ") fired");
        try {
            switch (event.type()) {
                case SPAWNER_TIER -> applySpawnerTier(event);
                case BED_DESTRUCTION -> GameEndgameService.getInstance().destroyAllTargets(game);
                case SUDDEN_DEATH -> GameEndgameService.getInstance().startSuddenDeath(game);
                case GAME_END -> GameEndgameService.getInstance().endGameByTime(game);
                case ANNOUNCEMENT -> {
                }
            }
        } catch (Throwable t) {
            BedWarsPlugin.getInstance().getLogger().error("Timeline event " + event.id() + " failed in game " + game.getName(), t);
        }
        // announce after the action (tier already applied; the endgame area announces its own events inside the call)
        if (!stopped) { // the action may have triggered a rebuild
            try {
                announce(event);
            } catch (Throwable t) {
                BedWarsPlugin.getInstance().getLogger().error("Could not announce timeline event " + event.id() + " in game " + game.getName(), t);
            }
        }
    }

    private void applySpawnerTier(@NotNull TimelineEventDefinition event) {
        var interval = definition.effectiveIntervalTicks(event);
        for (var spawner : matchingSpawners(event)) {
            // the spawner's own thread (Folia-safe; same thread as the spawner task)
            spawner.getLocation().tasker().run(() -> {
                if (interval != null) {
                    spawner.setIntervalTicks(interval); // no-op if not started / disabled
                }
                spawner.setTier(event.tier());
            });
        }
    }

    private void announce(@NotNull TimelineEventDefinition event) {
        var plan = definition.planFor(event);
        if (plan.isEmpty()) {
            return;
        }
        var receivers = game.getConnectedPlayers(); // players and spectators of this game only
        if (plan.chat()) {
            Message msg;
            if (event.message() != null) {
                msg = Message.ofRichText(event.message());
            } else if (event.type() == TimelineEventType.SPAWNER_TIER) {
                msg = Message.of(ForkLangKeys.IN_GAME_TIMELINE_SPAWNER_TIER_CHAT);
            } else {
                msg = Message.of(ForkLangKeys.IN_GAME_TIMELINE_ANNOUNCEMENT_CHAT);
            }
            withPlaceholders(msg, event).prefixOrDefault(game.getCustomPrefixComponent()).send(receivers);
        }
        if (plan.title()) {
            Message title;
            if (event.title() != null || event.subtitle() != null) {
                // "@dotted.lang.key" links are resolved by slib for rich text, in the title and in the subtitle
                title = Message.ofRichText(event.title() != null ? event.title() : "")
                        .joinRichText(event.subtitle() != null ? event.subtitle() : "");
            } else if (event.type() == TimelineEventType.SPAWNER_TIER) {
                title = Message.of(ForkLangKeys.IN_GAME_TIMELINE_SPAWNER_TIER_TITLE).join(ForkLangKeys.IN_GAME_TIMELINE_SPAWNER_TIER_SUBTITLE);
            } else {
                title = Message.of(ForkLangKeys.IN_GAME_TIMELINE_ANNOUNCEMENT_TITLE).joinRichText("");
            }
            withPlaceholders(title, event).times(TitleUtils.defaultTimes()).title(receivers);
        }
        if (plan.sound() != null) {
            var sound = toSoundStart(plan.sound());
            if (sound != null) {
                receivers.forEach(p -> p.playSound(sound));
            }
        }
    }

    /**
     * Placeholders available in every timeline text: {@code <spawner>}, {@code <tier>}, {@code <tier-number>},
     * {@code <event>}, {@code <time>}. Only for chat and title messages; never call this on the result of
     * {@link #displayName(TimelineEventDefinition)}.
     */
    private @NotNull Message withPlaceholders(@NotNull Message msg, @NotNull TimelineEventDefinition e) {
        return msg.placeholder("spawner", spawnerName(e, true))
                .placeholder("tier", RomanNumerals.toRoman(e.tier()))
                .placeholder("tier-number", e.tier())
                .placeholder("time", TimeFormat.formatClock(e.timeSeconds()))
                .placeholder("event", displayName(e));
    }

    private @NotNull Component spawnerName(@NotNull TimelineEventDefinition e, boolean colored) {
        if (e.spawnerType() == null) {
            return Component.empty();
        }
        var variant = game.getGameVariant();
        var type = variant == null ? null : variant.getItemSpawnerType(e.spawnerType());
        if (type == null) {
            return Component.text(e.spawnerType());
        }
        return colored ? type.getItemName() : type.getTranslatableKey();
    }

    /**
     * Display name used by the sidebar ({@code <event>}) and by announcements. A fresh {@link Message} per call
     * ({@code Message} is mutable).
     */
    public @NotNull Message displayName(@NotNull TimelineEventDefinition e) {
        if (e.name() != null) {
            return Message.ofRichText(e.name())
                    .placeholder("spawner", spawnerName(e, false))
                    .placeholder("tier", RomanNumerals.toRoman(e.tier()))
                    .placeholder("tier-number", e.tier());
        }
        return switch (e.type()) {
            case SPAWNER_TIER -> Message.of(ForkLangKeys.IN_GAME_TIMELINE_EVENT_SPAWNER_TIER)
                    .placeholder("spawner", spawnerName(e, false))
                    .placeholder("tier", RomanNumerals.toRoman(e.tier()))
                    .placeholder("tier-number", e.tier());
            case BED_DESTRUCTION -> Message.of(ForkLangKeys.IN_GAME_TIMELINE_EVENT_BED_DESTRUCTION);
            case SUDDEN_DEATH -> Message.of(ForkLangKeys.IN_GAME_TIMELINE_EVENT_SUDDEN_DEATH);
            case GAME_END -> Message.of(ForkLangKeys.IN_GAME_TIMELINE_EVENT_GAME_END);
            case ANNOUNCEMENT -> Message.of(ForkLangKeys.IN_GAME_TIMELINE_EVENT_ANNOUNCEMENT);
        };
    }

    private static @Nullable SoundStart toSoundStart(@NotNull SoundSpec spec) {
        try {
            return SoundStart.sound(spec.name(), SoundSource.soundSource(spec.source()), spec.volume(), spec.pitch());
        } catch (Throwable t) {
            Debug.warn("Invalid timeline sound " + spec.name(), true);
            return null;
        }
    }
}
