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

package org.screamingsandals.bedwars.setup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Default team names and colour suggestion order for {@code /bw set team} (pure).
 */
public final class TeamNaming {
    /**
     * Hypixel-like order for suggestions; colours not listed follow in enum order.
     */
    public static final List<String> PREFERRED = List.of("RED", "BLUE", "GREEN", "YELLOW", "CYAN", "WHITE", "PINK", "GRAY");

    /**
     * "RED" -&gt; "Red", "LIGHT_BLUE" -&gt; "LightBlue", "light_gray" -&gt; "LightGray" (single word: usable as a command argument).
     */
    public static @NotNull String defaultName(@NotNull String colorEnumName) {
        var sb = new StringBuilder();
        for (var part : colorEnumName.split("[_\\-\\s]+")) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    /**
     * PREFERRED (if present in allColors) first, then the rest in the given order.
     */
    public static @NotNull List<String> orderByPreference(@NotNull List<String> allColors) {
        var result = new ArrayList<String>(allColors.size());
        var added = new HashSet<String>();
        for (var preferred : PREFERRED) {
            for (var color : allColors) {
                if (color.equalsIgnoreCase(preferred) && added.add(color.toUpperCase(Locale.ROOT))) {
                    result.add(color);
                }
            }
        }
        for (var color : allColors) {
            if (added.add(color.toUpperCase(Locale.ROOT))) {
                result.add(color);
            }
        }
        return result;
    }

    /**
     * First colour of {@link #orderByPreference(List)} not contained in used (case-insensitive); null if all are used.
     */
    public static @Nullable String firstUnused(@NotNull List<String> allColors, @NotNull Collection<String> used) {
        Set<String> usedUpper = new HashSet<>();
        for (var u : used) {
            if (u != null) {
                usedUpper.add(u.toUpperCase(Locale.ROOT));
            }
        }
        for (var color : orderByPreference(allColors)) {
            if (!usedUpper.contains(color.toUpperCase(Locale.ROOT))) {
                return color;
            }
        }
        return null;
    }

    private TeamNaming() {
    }
}
