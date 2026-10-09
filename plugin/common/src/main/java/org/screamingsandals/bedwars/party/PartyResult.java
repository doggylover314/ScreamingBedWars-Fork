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

import java.util.List;

/**
 * Result of a {@link PartyRegistry} operation: either an error or the (possibly dissolved, then null) party plus the
 * events that have to be delivered.
 */
public record PartyResult(@Nullable PartyError error, @Nullable PartyImpl party, @NotNull List<@NotNull PartyEvent> events) {

    public static @NotNull PartyResult ok(@Nullable PartyImpl party, @NotNull List<PartyEvent> events) {
        return new PartyResult(null, party, List.copyOf(events));
    }

    public static @NotNull PartyResult fail(@NotNull PartyError error) {
        return new PartyResult(error, null, List.of());
    }

    public boolean isSuccess() {
        return error == null;
    }
}
