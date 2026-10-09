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

package org.screamingsandals.bedwars.game.upgrade.pricing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A price in the shape {@code amount of currency}. Pure value class (no platform access).
 *
 * @param amount   amount, never negative
 * @param currency lower-cased spawner type key (e.g. {@code diamond})
 */
public record PriceSpec(int amount, @NotNull String currency) {
    // "4 of diamond", "4 diamond", "4"; amount up to 9 digits (no overflow)
    private static final Pattern PATTERN =
            Pattern.compile("^\\s*(\\d{1,9})(?:\\s+(?:of\\s+)?([A-Za-z0-9_\\-]+))?\\s*$");

    public PriceSpec {
        if (amount < 0) {
            throw new IllegalArgumentException("amount < 0");
        }
        Objects.requireNonNull(currency, "currency");
        currency = currency.trim().toLowerCase(Locale.ROOT);
        if (currency.isEmpty()) {
            throw new IllegalArgumentException("empty currency");
        }
    }

    public static @NotNull PriceSpec of(int amount, @NotNull String currency) {
        return new PriceSpec(amount, currency);
    }

    /**
     * Parses {@code "4 of diamond"}, {@code "4 diamond"} or {@code "4"}.
     * A missing currency is replaced by {@code defaultCurrency}; still missing or invalid input gives an empty result.
     */
    public static @NotNull Optional<PriceSpec> parse(@Nullable String text, @Nullable String defaultCurrency) {
        if (text == null) {
            return Optional.empty();
        }
        var m = PATTERN.matcher(text);
        if (!m.matches()) {
            return Optional.empty();
        }
        var currency = m.group(2) != null ? m.group(2) : defaultCurrency;
        if (currency == null || currency.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new PriceSpec(Integer.parseInt(m.group(1)), currency));
    }

    /**
     * Scalar string, or map {@code {amount: int, currency: string}}.
     */
    public static @NotNull Optional<PriceSpec> parse(@NotNull ConfigurationNode node, @Nullable String defaultCurrency) {
        if (node.isMap()) {
            int amount = node.node("amount").getInt(-1);
            var configured = node.node("currency").getString(); // getString(def) rejects a null default
            var currency = configured != null && !configured.isBlank() ? configured : defaultCurrency;
            if (amount < 0 || currency == null || currency.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new PriceSpec(amount, currency));
        }
        return parse(node.getString(), defaultCurrency);
    }
}
