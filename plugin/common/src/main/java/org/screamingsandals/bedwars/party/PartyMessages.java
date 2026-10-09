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
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.utils.Broadcasts;
import org.screamingsandals.bedwars.utils.TimeFormat;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.player.Players;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.spectator.event.ClickEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Runtime renderer of {@link PartyEvent}s and {@link PartyError}s (needs the server, not unit-tested).
 */
final class PartyMessages {
    private PartyMessages() {
    }

    /**
     * Online -> {@link Player#getDisplayName()}; else the fallback name ("?" when unknown).
     */
    static @NotNull Component name(@Nullable UUID uuid, @Nullable String fallback) {
        if (uuid != null) {
            var online = Players.getPlayer(uuid);
            if (online != null) {
                return online.getDisplayName();
            }
        }
        return Component.text(fallback != null ? fallback : "?");
    }

    /**
     * Sends every event of the result to its ONLINE recipients.
     */
    static void dispatch(@NotNull PartyResult result, @NotNull PartySettings settings, boolean rootRegistered) {
        for (var event : result.events()) {
            for (var recipient : event.recipients()) {
                var player = Players.getPlayer(recipient);
                if (player == null) {
                    continue;
                }
                var message = render(event, player, settings, rootRegistered);
                if (message != null) {
                    message.defaultPrefix().send(player);
                }
                if (event.type() == PartyEventType.INVITE_RECEIVED) {
                    Broadcasts.sound(List.of(player), Broadcasts.configuredSound("party_invite", "entity.experience_orb.pickup"));
                }
            }
        }
    }

    static @Nullable Message render(@NotNull PartyEvent e, @NotNull Player viewer, @NotNull PartySettings s, boolean rootRegistered) {
        var subject = name(e.subject(), e.subjectName());
        var other = name(e.other(), e.otherName());
        switch (e.type()) {
            case INVITE_SENT:
                return Message.of(ForkLangKeys.PARTY_INVITE_SENT)
                        .placeholder("player", subject)
                        .placeholder("seconds", s.inviteExpireSeconds());
            case INVITE_SENT_OTHERS:
                return Message.of(ForkLangKeys.PARTY_INVITE_SENT_OTHERS)
                        .placeholder("player", subject)
                        .placeholder("inviter", other)
                        .placeholder("seconds", s.inviteExpireSeconds());
            case INVITE_RECEIVED: {
                boolean fromLeader = Objects.equals(e.other(), e.leader());
                var accept = PartyCommandLabels.clickCommand(rootRegistered, s.label(), "accept", e.otherName());
                var deny = PartyCommandLabels.clickCommand(rootRegistered, s.label(), "deny", e.otherName());
                return Message.of(fromLeader ? ForkLangKeys.PARTY_INVITE_RECEIVED : ForkLangKeys.PARTY_INVITE_RECEIVED_MEMBER)
                        .placeholder("inviter", other)
                        .placeholder("leader", name(e.leader(), e.leaderName()))
                        .placeholder("seconds", s.inviteExpireSeconds())
                        .placeholder("accept", button(ForkLangKeys.PARTY_INVITE_BUTTON_ACCEPT, ForkLangKeys.PARTY_INVITE_BUTTON_ACCEPT_HOVER, accept, viewer))
                        .placeholder("deny", button(ForkLangKeys.PARTY_INVITE_BUTTON_DENY, ForkLangKeys.PARTY_INVITE_BUTTON_DENY_HOVER, deny, viewer));
            }
            case INVITE_EXPIRED_INVITER:
                return Message.of(ForkLangKeys.PARTY_INVITE_EXPIRED_INVITER).placeholder("player", subject);
            case INVITE_EXPIRED_TARGET:
                return Message.of(ForkLangKeys.PARTY_INVITE_EXPIRED_TARGET).placeholder("inviter", other);
            case INVITE_DENIED_INVITER:
                return Message.of(ForkLangKeys.PARTY_INVITE_DENIED_INVITER).placeholder("player", subject);
            case INVITE_DENIED_TARGET:
                return Message.of(ForkLangKeys.PARTY_INVITE_DENIED_TARGET).placeholder("inviter", other);
            case MEMBER_JOINED:
                return Message.of(ForkLangKeys.PARTY_MEMBER_JOINED).placeholder("player", subject);
            case MEMBER_JOINED_SELF:
                return Message.of(ForkLangKeys.PARTY_MEMBER_JOINED_SELF).placeholder("leader", name(e.leader(), e.leaderName()));
            case MEMBER_LEFT:
                return Message.of(ForkLangKeys.PARTY_MEMBER_LEFT).placeholder("player", subject);
            case MEMBER_LEFT_SELF:
                return Message.of(ForkLangKeys.PARTY_MEMBER_LEFT_SELF);
            case MEMBER_KICKED:
                return Message.of(ForkLangKeys.PARTY_MEMBER_KICKED).placeholder("player", subject).placeholder("actor", other);
            case MEMBER_KICKED_SELF:
                return Message.of(ForkLangKeys.PARTY_MEMBER_KICKED_SELF).placeholder("actor", other);
            case MEMBER_DISCONNECTED:
                return s.memberOfflineTimeoutSeconds() < 0
                        ? Message.of(ForkLangKeys.PARTY_MEMBER_DISCONNECTED_NO_TIMEOUT).placeholder("player", subject)
                        : Message.of(ForkLangKeys.PARTY_MEMBER_DISCONNECTED).placeholder("player", subject)
                                .placeholderRaw("time", TimeFormat.formatMinutesSeconds(s.memberOfflineTimeoutSeconds()));
            case LEADER_DISCONNECTED:
                return s.leaderGraceSeconds() < 0
                        ? Message.of(ForkLangKeys.PARTY_LEADER_DISCONNECTED_NO_TIMEOUT).placeholder("player", subject)
                        : Message.of(s.leaderOfflineAction() == LeaderOfflineAction.TRANSFER
                                        ? ForkLangKeys.PARTY_LEADER_DISCONNECTED_TRANSFER : ForkLangKeys.PARTY_LEADER_DISCONNECTED_DISBAND)
                                .placeholder("player", subject)
                                .placeholderRaw("time", TimeFormat.formatMinutesSeconds(s.leaderGraceSeconds()));
            case MEMBER_RECONNECTED:
                return Message.of(ForkLangKeys.PARTY_MEMBER_RECONNECTED).placeholder("player", subject);
            case MEMBER_REMOVED_OFFLINE:
                return Message.of(ForkLangKeys.PARTY_MEMBER_REMOVED_OFFLINE).placeholder("player", subject);
            case LEADER_CHANGED:
                return Message.of(ForkLangKeys.PARTY_LEADER_CHANGED).placeholder("player", subject).placeholder("old", other);
            case LEADER_TRANSFERRED:
                return Message.of(ForkLangKeys.PARTY_LEADER_TRANSFERRED).placeholder("player", subject).placeholder("old", other);
            case LEADER_TRANSFERRED_OFFLINE:
                return Message.of(ForkLangKeys.PARTY_LEADER_TRANSFERRED_OFFLINE).placeholder("player", subject).placeholder("old", other);
            case DISBANDED_BY_LEADER:
                return Message.of(ForkLangKeys.PARTY_DISBAND_BY_LEADER).placeholder("player", subject);
            case DISBANDED_EMPTY:
                return Message.of(ForkLangKeys.PARTY_DISBAND_EMPTY);
            case DISBANDED_LEADER_OFFLINE:
                return Message.of(ForkLangKeys.PARTY_DISBAND_LEADER_OFFLINE).placeholder("player", subject);
            case DISBANDED_LEADER_LEFT:
                return Message.of(ForkLangKeys.PARTY_DISBAND_LEADER_LEFT).placeholder("player", subject);
            default:
                return null;
        }
    }

    static @NotNull Component button(@NotNull String[] textKey, @NotNull String[] hoverKey, @NotNull String command, @NotNull Player viewer) {
        return Message.of(textKey).asComponent(viewer)
                .withClickEvent(ClickEvent.runCommand(command))
                .withHoverEvent(Message.of(hoverKey).asComponent(viewer));
    }

    /**
     * Error replies of commands; {@code name} = typed / target name (raw), {@code max} = size limit used.
     */
    static void error(@NotNull Player player, @NotNull PartyError error, @Nullable String name, int max, @NotNull String cmdPrefix) {
        Message message;
        switch (error) {
            case NOT_IN_PARTY:
                message = Message.of(LangKeys.PARTY_COMMAND_NOT_IN_PARTY);
                break;
            case NOT_LEADER:
                message = Message.of(LangKeys.PARTY_COMMAND_NOT_PARTY_LEADER);
                break;
            case CANNOT_INVITE_SELF:
                message = Message.of(ForkLangKeys.PARTY_ERROR_CANNOT_INVITE_SELF);
                break;
            case ALREADY_MEMBER:
                message = Message.of(ForkLangKeys.PARTY_ERROR_ALREADY_MEMBER);
                break;
            case TARGET_IN_PARTY:
                message = Message.of(ForkLangKeys.PARTY_ERROR_TARGET_IN_PARTY);
                break;
            case ALREADY_INVITED:
                message = Message.of(ForkLangKeys.PARTY_ERROR_ALREADY_INVITED);
                break;
            case PARTY_FULL:
                message = Message.of(ForkLangKeys.PARTY_ERROR_PARTY_FULL).placeholder("max", max);
                break;
            case ALREADY_IN_PARTY:
                message = Message.of(ForkLangKeys.PARTY_ERROR_ALREADY_IN_PARTY);
                break;
            case NO_INVITE:
                message = Message.of(ForkLangKeys.PARTY_ERROR_NO_INVITE);
                break;
            case NO_INVITE_FROM:
                message = Message.of(ForkLangKeys.PARTY_ERROR_NO_INVITE_FROM);
                break;
            case NOT_A_MEMBER:
                message = Message.of(ForkLangKeys.PARTY_ERROR_NOT_A_MEMBER);
                break;
            case CANNOT_KICK_SELF:
                message = Message.of(ForkLangKeys.PARTY_ERROR_CANNOT_KICK_SELF);
                break;
            case CANNOT_TRANSFER_SELF:
                message = Message.of(ForkLangKeys.PARTY_ERROR_CANNOT_TRANSFER_SELF);
                break;
            case TARGET_OFFLINE:
            default:
                message = Message.of(ForkLangKeys.PARTY_ERROR_TARGET_OFFLINE);
                break;
        }
        message.placeholderRaw("player", name == null ? "" : name)
                .placeholderRaw("cmd", cmdPrefix)
                .defaultPrefix()
                .send(player);
    }
}
