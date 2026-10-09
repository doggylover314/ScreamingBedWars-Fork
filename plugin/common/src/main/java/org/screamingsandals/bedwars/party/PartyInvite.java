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

import java.util.UUID;

/**
 * A pending party invite (pure data).
 *
 * @param partyId     party the target is invited to
 * @param inviter     player who sent the invite (a member of the party at that time)
 * @param target      invited player
 * @param createdAt   creation time in millis
 * @param expiresAt   expiry time in millis (exclusive: the invite is expired at {@code now >= expiresAt})
 * @param maxSize     size limit captured from the inviter's permissions at invite time; {@code <= 0} = unlimited
 */
public record PartyInvite(@NotNull UUID partyId,
                          @NotNull UUID inviter, @NotNull String inviterName,
                          @NotNull UUID target, @NotNull String targetName,
                          long createdAt, long expiresAt,
                          int maxSize) {
    public boolean isExpired(long now) {
        return now >= expiresAt;
    }
}
