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
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed {@code party} section of config.yml (PURE: Configurate only). Defaults MUST equal the MainConfig defaults.
 */
public record PartySettings(
        boolean enabled, boolean autojoinMembers, boolean notifyWhenWarped, boolean requireLeaderToJoin,
        int maxSize, boolean membersCanInvite, int inviteExpireSeconds,
        int leaderGraceSeconds, @NotNull LeaderOfflineAction leaderOfflineAction, int memberOfflineTimeoutSeconds,
        boolean mustFitOneTeam, boolean pullFromRunningGames,
        boolean warpEnabled, boolean warpTeleportOutsideGames, int warpTeleportDelayTicks,
        boolean chatEnabled, @NotNull String chatBypassPrefix, boolean chatLogToConsole,
        boolean rootCommand, @NotNull String label, @NotNull List<String> aliases, boolean allowInGame, boolean inviteShortcut) {

    public static @NotNull PartySettings fromNode(@NotNull ConfigurationNode n) {
        int expire = n.node("invite-expire-seconds").getInt(60);
        expire = expire <= 0 ? 60 : Math.min(Math.max(expire, 5), 3600);
        int max = n.node("max-size").getInt(4);
        List<String> rawAliases = readAliases(n.node("commands", "aliases"));
        var label = PartyCommandLabels.labelOrDefault(n.node("commands", "label").getString("party"));
        var bypass = n.node("chat", "bypass-prefix").getString("!");
        return new PartySettings(
                n.node("enabled").getBoolean(true),
                n.node("autojoin-members").getBoolean(true),
                n.node("notify-when-warped").getBoolean(true),
                n.node("require-leader-to-join").getBoolean(true),
                Math.max(max, 0),
                n.node("members-can-invite").getBoolean(false),
                expire,
                n.node("leader-offline", "grace-seconds").getInt(300),
                LeaderOfflineAction.parse(n.node("leader-offline", "action").getString("transfer"), LeaderOfflineAction.TRANSFER),
                n.node("member-offline-timeout-seconds").getInt(300),
                n.node("join", "must-fit-one-team").getBoolean(true),
                n.node("join", "pull-from-running-games").getBoolean(false),
                n.node("warp", "enabled").getBoolean(true),
                n.node("warp", "teleport-outside-games").getBoolean(true),
                Math.min(Math.max(n.node("warp", "teleport-delay-ticks").getInt(5), 1), 100),
                n.node("chat", "enabled").getBoolean(true),
                bypass == null ? "" : bypass,
                n.node("chat", "log-to-console").getBoolean(true),
                n.node("commands", "root-command").getBoolean(true),
                label,
                PartyCommandLabels.sanitizeAliases(label, rawAliases),
                n.node("commands", "allow-in-game").getBoolean(true),
                n.node("commands", "invite-shortcut").getBoolean(true)
        );
    }

    /**
     * A missing node means the default alias ({@code p}); an explicit empty list means "no aliases" (an admin disables
     * {@code /p} that way when another plugin owns it). Configurate's {@code getList(type, def)} would return the default
     * for an empty list, so lists are read element by element.
     */
    private static @NotNull List<String> readAliases(@NotNull ConfigurationNode node) {
        if (node.isList()) {
            var result = new ArrayList<String>();
            for (var child : node.childrenList()) {
                var alias = child.getString();
                if (alias != null) {
                    result.add(alias);
                }
            }
            return result;
        }
        if (node.virtual() || node.isNull()) {
            return List.of("p");
        }
        try {
            return node.getList(String.class, List.of("p")); // a single scalar becomes a one-element list
        } catch (SerializationException e) {
            return List.of("p");
        }
    }

    public @NotNull PartyRules toRules() {
        return new PartyRules(maxSize, membersCanInvite, inviteExpireSeconds * 1000L,
                leaderGraceSeconds < 0 ? -1 : leaderGraceSeconds * 1000L, leaderOfflineAction,
                memberOfflineTimeoutSeconds < 0 ? -1 : memberOfflineTimeoutSeconds * 1000L);
    }
}
