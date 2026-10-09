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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation and formatting of the party root command labels (PURE).
 */
public final class PartyCommandLabels {
    private static final Pattern LABEL = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final Set<String> RESERVED = Set.of("bw", "bedwars");
    public static final String DEFAULT_LABEL = "party";

    private PartyCommandLabels() {
    }

    /**
     * Trimmed + lower-cased ({@link Locale#ROOT}); returns null when invalid or reserved.
     */
    public static @Nullable String normalize(@Nullable String label) {
        if (label == null) {
            return null;
        }
        var normalized = label.trim().toLowerCase(Locale.ROOT);
        if (!LABEL.matcher(normalized).matches() || RESERVED.contains(normalized)) {
            return null;
        }
        return normalized;
    }

    /**
     * {@link #normalize(String)} or {@link #DEFAULT_LABEL}.
     */
    public static @NotNull String labelOrDefault(@Nullable String label) {
        var normalized = normalize(label);
        return normalized == null ? DEFAULT_LABEL : normalized;
    }

    /**
     * Normalizes each alias; drops null / invalid / reserved ones and the ones equal to the label; distinct, order kept.
     */
    public static @NotNull List<String> sanitizeAliases(@NotNull String label, @Nullable List<String> aliases) {
        if (aliases == null) {
            return List.of();
        }
        var result = new LinkedHashSet<String>();
        for (var alias : aliases) {
            var normalized = normalize(alias);
            if (normalized != null && !normalized.equals(label)) {
                result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    /**
     * {@code "/" + label, "/" + alias..., "/" + ns + ":" + label, "/" + ns + ":" + alias...} for each non-blank namespace
     * (lower-cased).
     */
    public static @NotNull Set<String> inGameLabels(@NotNull String label, @NotNull List<String> aliases, @NotNull Collection<String> namespaces) {
        var all = new LinkedHashSet<String>();
        all.add(label);
        all.addAll(aliases);
        var result = new LinkedHashSet<String>();
        for (var one : all) {
            result.add("/" + one);
        }
        for (var namespace : namespaces) {
            if (namespace == null || namespace.isBlank()) {
                continue;
            }
            var ns = namespace.trim().toLowerCase(Locale.ROOT);
            for (var one : all) {
                result.add("/" + ns + ":" + one);
            }
        }
        return result;
    }

    /**
     * {@code commandPref} = first token of the typed command including the slash; case-insensitive membership.
     */
    public static boolean matches(@Nullable String commandPref, @NotNull Set<String> labels) {
        if (commandPref == null || labels.isEmpty()) {
            return false;
        }
        return labels.contains(commandPref.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * {@code "/party accept Bob"} (root registered) or {@code "/bw party accept Bob"}; {@code arg} may be null.
     */
    public static @NotNull String clickCommand(boolean rootRegistered, @NotNull String label, @NotNull String sub, @Nullable String arg) {
        var command = commandPrefix(rootRegistered, label) + " " + sub;
        return arg == null || arg.isEmpty() ? command : command + " " + arg;
    }

    /**
     * {@code "/party"} or {@code "/bw party"} - used for {@code <cmd>} placeholders in help / hints.
     */
    public static @NotNull String commandPrefix(boolean rootRegistered, @NotNull String label) {
        return rootRegistered ? "/" + label : "/bw party";
    }
}
