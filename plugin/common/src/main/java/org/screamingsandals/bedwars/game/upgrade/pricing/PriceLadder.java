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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Per-level prices of an upgrade shop item: the {@code prices:} list and the optional {@code prices-by-team-size:} map
 * (key = minimal team size, the greatest key not above the real team size wins). Pure: reads only the configuration node.
 */
public final class PriceLadder {
    public static final PriceLadder EMPTY = new PriceLadder(List.of(), new TreeMap<>());

    private final @NotNull List<PriceSpec> base;
    private final @NotNull NavigableMap<Integer, List<PriceSpec>> byTeamSize;

    private PriceLadder(@NotNull List<PriceSpec> base, @NotNull NavigableMap<Integer, List<PriceSpec>> byTeamSize) {
        this.base = base;
        this.byTeamSize = byTeamSize;
    }

    /**
     * Reads {@code prices} and {@code prices-by-team-size} from the upgrade property node. Invalid entries are reported
     * to {@code warnings} and skipped.
     *
     * @param property        the whole {@code properties:} map of the shop item
     * @param defaultCurrency currency of entries written without one (usually the currency of the static price)
     */
    public static @NotNull PriceLadder parse(@NotNull ConfigurationNode property, @Nullable String defaultCurrency,
                                             @NotNull List<String> warnings) {
        var base = parseList(property.node("prices"), defaultCurrency, warnings, "prices");

        var bySize = new TreeMap<Integer, List<PriceSpec>>();
        var sizes = property.node("prices-by-team-size");
        if (!sizes.virtual()) {
            if (!sizes.isMap()) {
                warnings.add("prices-by-team-size must be a map");
            } else {
                for (var entry : sizes.childrenMap().entrySet()) {
                    var key = String.valueOf(entry.getKey()).trim();
                    int size;
                    try {
                        size = Integer.parseInt(key);
                    } catch (NumberFormatException e) {
                        warnings.add("invalid team size '" + key + "' in prices-by-team-size");
                        continue;
                    }
                    if (size < 1) {
                        warnings.add("team size must be >= 1 in prices-by-team-size, got '" + key + "'");
                        continue;
                    }
                    var list = parseList(entry.getValue(), defaultCurrency, warnings, "prices-by-team-size." + size);
                    if (!list.isEmpty()) {
                        bySize.put(size, List.copyOf(list));
                    }
                }
            }
        }
        return new PriceLadder(List.copyOf(base), Collections.unmodifiableNavigableMap(bySize));
    }

    private static @NotNull List<PriceSpec> parseList(@NotNull ConfigurationNode node, @Nullable String defaultCurrency,
                                                      @NotNull List<String> warnings, @NotNull String context) {
        var list = new ArrayList<PriceSpec>();
        if (node.virtual()) {
            return list;
        }
        for (var child : node.isList() ? node.childrenList() : List.of(node)) {
            var parsed = PriceSpec.parse(child, defaultCurrency);
            if (parsed.isPresent()) {
                list.add(parsed.get());
            } else {
                warnings.add("invalid price '" + child.raw() + "' in " + context);
            }
        }
        return list;
    }

    /**
     * @return the ladder for a team of this size: the entry with the greatest key not above {@code teamSize}, else the base ladder
     */
    public @NotNull List<PriceSpec> forTeamSize(int teamSize) {
        var entry = byTeamSize.floorEntry(teamSize);
        return entry != null ? entry.getValue() : base;
    }

    public @NotNull List<PriceSpec> base() {
        return base;
    }

    public boolean isEmpty() {
        return base.isEmpty() && byTeamSize.isEmpty();
    }
}
