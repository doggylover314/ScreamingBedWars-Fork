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

package org.screamingsandals.bedwars.party;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * What happens to a party whose leader stayed offline longer than the grace period.
 */
public enum LeaderOfflineAction {
    /** The first online member (join order) becomes leader; the party is disbanded when nobody is online. */
    TRANSFER,
    /** The party is disbanded (Hypixel-like). */
    DISBAND;

    /**
     * Case-insensitive, trimmed; null / unknown -> {@code def}.
     */
    public static @NotNull LeaderOfflineAction parse(@Nullable String s, @NotNull LeaderOfflineAction def) {
        if (s == null) {
            return def;
        }
        switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "transfer":
                return TRANSFER;
            case "disband":
                return DISBAND;
            default:
                return def;
        }
    }
}
