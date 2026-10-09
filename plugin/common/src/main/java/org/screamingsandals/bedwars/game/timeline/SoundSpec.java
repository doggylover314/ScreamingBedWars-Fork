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

/**
 * Sound played by a timeline announcement (pure data; converted to a platform sound by {@link GameTimeline}).
 *
 * @param name   sound key, e.g. {@code block.note_block.pling}
 * @param source sound source name in lower case, e.g. {@code master}
 */
public record SoundSpec(@NotNull String name, @NotNull String source, float volume, float pitch) {
    /**
     * Explicit "no sound".
     */
    public static final SoundSpec NONE = new SoundSpec("", "master", 0f, 0f);
    public static final SoundSpec DEFAULT_ANNOUNCEMENT = new SoundSpec("block.note_block.pling", "master", 1f, 1f);

    public boolean isNone() {
        return this == NONE || name.isEmpty();
    }
}
