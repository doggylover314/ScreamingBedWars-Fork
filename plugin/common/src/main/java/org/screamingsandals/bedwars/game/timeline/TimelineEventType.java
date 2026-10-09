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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * Kinds of entries a variant {@code timeline:} section can contain (pure).
 */
public enum TimelineEventType {
    SPAWNER_TIER("spawner-tier"),
    BED_DESTRUCTION("bed-destruction"),
    SUDDEN_DEATH("sudden-death"),
    GAME_END("game-end"),
    ANNOUNCEMENT("announcement");

    private final @NotNull String configName;

    TimelineEventType(@NotNull String configName) {
        this.configName = configName;
    }

    public @NotNull String configName() {
        return configName;
    }

    /**
     * Case-insensitive; {@code '_'} and {@code ' '} are treated as {@code '-'}.
     *
     * @return the type, or empty for {@code null} and unknown names
     */
    public static @NotNull Optional<TimelineEventType> fromConfig(@Nullable String value) {
        if (value == null) {
            return Optional.empty();
        }
        var normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
        for (var type : values()) {
            if (type.configName.equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * @return true for BED_DESTRUCTION, SUDDEN_DEATH and GAME_END (announced by the endgame area, not by the timeline)
     */
    public boolean isEndgameType() {
        return this == BED_DESTRUCTION || this == SUDDEN_DEATH || this == GAME_END;
    }
}
