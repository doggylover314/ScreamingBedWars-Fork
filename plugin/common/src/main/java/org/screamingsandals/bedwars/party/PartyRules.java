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

/**
 * Millisecond rules for the {@link PartyRegistry}. Negative grace / timeout = never; {@code maxSize <= 0} = unlimited.
 */
public record PartyRules(int maxSize, boolean membersCanInvite, long inviteExpireMillis,
                         long leaderGraceMillis, @NotNull LeaderOfflineAction leaderOfflineAction,
                         long memberOfflineMillis) {
}
