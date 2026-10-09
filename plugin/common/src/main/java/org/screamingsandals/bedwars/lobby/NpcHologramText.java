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

package org.screamingsandals.bedwars.lobby;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Pure text inspection for NPC hologram lines: tells whether a line needs periodic refreshing.
 */
public final class NpcHologramText {
    /**
     * Placeholders resolved by {@link NpcHologramPlaceholders} (written as {@code <name>} in a hologram line).
     */
    public static final List<String> OWN_PLACEHOLDERS =
            List.of("mode", "mode-id", "mode-players", "mode-waiting", "mode-playing", "mode-arenas", "all-players");
    private static final Pattern OWN = Pattern.compile("<(mode|mode-id|mode-players|mode-waiting|mode-playing|mode-arenas|all-players)>");
    private static final Pattern PAPI = Pattern.compile("%[^%\\s]+%");

    private NpcHologramText() {
    }

    public static boolean hasOwnPlaceholders(@Nullable String raw) {
        return raw != null && OWN.matcher(raw).find();
    }

    public static boolean hasPapiPlaceholders(@Nullable String raw) {
        return raw != null && PAPI.matcher(raw).find();
    }

    public static boolean isDynamic(@Nullable String raw) {
        return hasOwnPlaceholders(raw) || hasPapiPlaceholders(raw);
    }
}
