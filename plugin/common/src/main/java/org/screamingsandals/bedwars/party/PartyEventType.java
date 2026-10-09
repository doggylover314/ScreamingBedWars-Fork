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
 * Kinds of {@link PartyEvent}. The comment of each constant is {@code subject / other}.
 */
public enum PartyEventType {
    /** target / inviter */
    INVITE_SENT,
    /** target / inviter */
    INVITE_SENT_OTHERS,
    /** target / inviter */
    INVITE_RECEIVED,
    /** target / inviter */
    INVITE_EXPIRED_INVITER,
    /** target / inviter */
    INVITE_EXPIRED_TARGET,
    /** target / inviter */
    INVITE_DENIED_INVITER,
    /** target / inviter */
    INVITE_DENIED_TARGET,
    /** new member / - */
    MEMBER_JOINED,
    /** new member / - */
    MEMBER_JOINED_SELF,
    /** leaver / - */
    MEMBER_LEFT,
    /** leaver / - */
    MEMBER_LEFT_SELF,
    /** kicked / actor */
    MEMBER_KICKED,
    /** kicked / actor */
    MEMBER_KICKED_SELF,
    /** member / - */
    MEMBER_DISCONNECTED,
    /** leader / - */
    LEADER_DISCONNECTED,
    /** member / - */
    MEMBER_RECONNECTED,
    /** member / - */
    MEMBER_REMOVED_OFFLINE,
    /** new leader / old leader (the old leader left the party) */
    LEADER_CHANGED,
    /** new leader / old leader */
    LEADER_TRANSFERRED,
    /** new leader / old leader */
    LEADER_TRANSFERRED_OFFLINE,
    /** leader / - */
    DISBANDED_BY_LEADER,
    /** leader / - */
    DISBANDED_EMPTY,
    /** old leader / - */
    DISBANDED_LEADER_OFFLINE,
    /** old leader / - */
    DISBANDED_LEADER_LEFT
}
