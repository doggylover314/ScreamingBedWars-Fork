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

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Validation of names for NEW arenas created through {@code /bw setup} and {@code clone} (pure).
 */
public final class ArenaNames {
    /**
     * Command words that would be shadowed by a literal below {@code /bw setup} or {@code ... clone}.
     */
    public static final Set<String> RESERVED = Set.of("status", "save", "variant", "cancel", "clone", "confirm", "help", "info");
    public static final int MAX_LENGTH = 48;

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]+");

    public enum Problem {
        EMPTY,
        TOO_LONG,
        INVALID_CHARS,
        LOOKS_LIKE_UUID,
        RESERVED,
        TAKEN
    }

    /**
     * Checks in this order: EMPTY (null/blank), TOO_LONG, INVALID_CHARS (not [A-Za-z0-9_-]+), LOOKS_LIKE_UUID
     * (UUID.fromString succeeds), RESERVED (lower-case in reserved), TAKEN (case-insensitive in taken).
     */
    public static @NotNull Optional<Problem> validate(@Nullable String name, @NotNull Collection<String> taken, @NotNull Set<String> reserved) {
        if (name == null || name.isBlank()) {
            return Optional.of(Problem.EMPTY);
        }
        if (name.length() > MAX_LENGTH) {
            return Optional.of(Problem.TOO_LONG);
        }
        if (!VALID.matcher(name).matches()) {
            return Optional.of(Problem.INVALID_CHARS);
        }
        if (looksLikeUuid(name)) {
            return Optional.of(Problem.LOOKS_LIKE_UUID);
        }
        if (reserved.contains(name.toLowerCase(Locale.ROOT))) {
            return Optional.of(Problem.RESERVED);
        }
        for (var t : taken) {
            if (t != null && t.equalsIgnoreCase(name)) {
                return Optional.of(Problem.TAKEN);
            }
        }
        return Optional.empty();
    }

    private static boolean looksLikeUuid(@NotNull String s) {
        try {
            UUID.fromString(s); // same test as GameManagerImpl.getGame(String)
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private ArenaNames() {
    }
}
