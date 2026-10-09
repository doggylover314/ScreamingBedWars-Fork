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

/**
 * Why a {@link PartyRegistry} operation was refused.
 */
public enum PartyError {
    NOT_IN_PARTY,
    NOT_LEADER,
    CANNOT_INVITE_SELF,
    ALREADY_MEMBER,
    TARGET_IN_PARTY,
    ALREADY_INVITED,
    PARTY_FULL,
    ALREADY_IN_PARTY,
    NO_INVITE,
    NO_INVITE_FROM,
    NOT_A_MEMBER,
    CANNOT_KICK_SELF,
    CANNOT_TRANSFER_SELF,
    TARGET_OFFLINE
}
