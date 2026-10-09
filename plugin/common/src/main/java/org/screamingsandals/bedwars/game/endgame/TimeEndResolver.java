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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Decides who wins when the game time runs out in {@link TimeEndMode#TIE_BREAK} mode.
 */
public final class TimeEndResolver {
    public static final List<Criterion> DEFAULT_CRITERIA = List.of(Criterion.TARGET, Criterion.PLAYERS);

    private TimeEndResolver() {
    }

    public enum Criterion {
        TARGET("target"),
        PLAYERS("players"),
        KILLS("kills"),
        FINAL_KILLS("final-kills");

        private final String configName;

        Criterion(String configName) {
            this.configName = configName;
        }

        public @NotNull String configName() {
            return configName;
        }

        /**
         * Case-insensitive; {@code '_'} and {@code ' '} are treated as {@code '-'}; unknown or null gives an empty result.
         */
        public static @NotNull Optional<Criterion> fromConfig(@Nullable String raw) {
            if (raw == null) {
                return Optional.empty();
            }
            var normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
            for (var criterion : values()) {
                if (criterion.configName.equals(normalized)) {
                    return Optional.of(criterion);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * State of one team that is still alive when the time runs out. {@code teamId} is the team name (unique per arena).
     */
    public record Standing(@NotNull String teamId, boolean targetValid, int players, int kills, int finalKills) {
    }

    public record Result(@Nullable String winnerId, @Nullable Criterion decidedBy, @NotNull List<String> tied) {
        public boolean isDraw() {
            return winnerId == null;
        }
    }

    /**
     * null gives {@link #DEFAULT_CRITERIA}; unknown names add "unknown tie-break criterion '&lt;x&gt;'" to {@code warnings}
     * and are skipped; duplicates are dropped silently; the order is kept. An explicitly empty list stays empty
     * (= always a draw).
     */
    public static @NotNull List<Criterion> parseCriteria(@Nullable List<String> raw, @NotNull List<String> warnings) {
        if (raw == null) {
            return DEFAULT_CRITERIA;
        }
        var result = new LinkedHashSet<Criterion>();
        for (var name : raw) {
            var criterion = Criterion.fromConfig(name);
            if (criterion.isPresent()) {
                result.add(criterion.get());
            } else {
                warnings.add("unknown tie-break criterion '" + name + "'");
            }
        }
        return List.copyOf(result);
    }

    public static @NotNull Result resolve(@NotNull List<Standing> standings, @NotNull List<Criterion> criteria) {
        if (standings.isEmpty()) {
            return new Result(null, null, List.of());
        }
        if (standings.size() == 1) {
            return new Result(standings.get(0).teamId(), null, List.of());
        }
        List<Standing> remaining = new ArrayList<>(standings);
        for (var criterion : criteria) {
            int best = remaining.stream().mapToInt(s -> value(s, criterion)).max().orElse(0);
            var filtered = remaining.stream().filter(s -> value(s, criterion) == best).toList();
            if (filtered.size() < remaining.size()) {
                remaining = new ArrayList<>(filtered);
                if (remaining.size() == 1) {
                    return new Result(remaining.get(0).teamId(), criterion, List.of());
                }
            }
        }
        return new Result(null, null, remaining.stream().map(Standing::teamId).toList());
    }

    static int value(Standing standing, Criterion criterion) {
        return switch (criterion) {
            case TARGET -> standing.targetValid() ? 1 : 0;
            case PLAYERS -> standing.players();
            case KILLS -> standing.kills();
            case FINAL_KILLS -> standing.finalKills();
        };
    }
}
