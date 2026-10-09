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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.BedWarsPlugin;
import org.screamingsandals.bedwars.commands.BedWarsPermission;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.lib.Server;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.player.Players;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.tasker.DefaultThreads;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.tasker.task.Task;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.ServiceDependencies;
import org.screamingsandals.lib.utils.annotations.methods.OnEnable;
import org.screamingsandals.lib.utils.annotations.methods.OnPostEnable;
import org.screamingsandals.lib.utils.annotations.methods.OnPreDisable;
import org.spongepowered.configurate.BasicConfigurationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The built-in party system (contract C3): owns the {@link PartyRegistry} and adapts it to players, messages and tasks.
 * <p>
 * Threading: all registry access happens on the server main thread. The only cross-thread state is {@link #chatToggled}
 * (a concurrent set) and the volatile cached settings read by the asynchronous chat listener.
 */
@Service
@ServiceDependencies(dependsOn = {
        MainConfig.class,
        PlayerManagerImpl.class
})
public class PartyManagerImpl {
    private final PartyRegistry registry = new PartyRegistry();
    private final Set<UUID> chatToggled = ConcurrentHashMap.newKeySet();
    private volatile @NotNull PartySettings settings = PartySettings.fromNode(BasicConfigurationNode.root()); // defaults until onEnable
    private volatile @NotNull Set<String> inGameLabels = Set.of();
    private volatile boolean rootRegistered; // set by PartyCommand after registering the root command
    private @Nullable Task sweepTask;

    public static @NotNull PartyManagerImpl getInstance() {
        return ServiceManager.get(PartyManagerImpl.class);
    }

    // ---------------------------------------------------------------------------------------------------------
    // lifecycle
    // ---------------------------------------------------------------------------------------------------------

    @OnEnable
    public void onEnable() { // MainConfig.load() ran before (dependsOn)
        settings = PartySettings.fromNode(MainConfig.getInstance().node("party"));
        var plugin = BedWarsPlugin.getInstance().getPluginDescription();
        inGameLabels = settings.enabled() && settings.rootCommand() && settings.allowInGame()
                ? PartyCommandLabels.inGameLabels(settings.label(), settings.aliases(), List.of(plugin.name(), plugin.pluginKey()))
                : Set.of();
    }

    @OnPostEnable
    public void onPostEnable() {
        if (!settings.enabled()) {
            if (!registry.getParties().isEmpty()) {
                BedWarsPlugin.getInstance().getLogger().info("Party system disabled - clearing {} parties", registry.getParties().size());
            }
            registry.clear();
            chatToggled.clear();
            return;
        }
        sweepTask = Tasker.runRepeatedly(DefaultThreads.GLOBAL_THREAD, () -> sweep(), 1, TaskerTime.SECONDS);
    }

    @OnPreDisable
    public void onPreDisable() {
        if (sweepTask != null && sweepTask.isScheduledOrRunning()) {
            sweepTask.cancel();
        }
        sweepTask = null;
        // registry intentionally kept: parties survive /bw reload (services are not re-constructed); lost on restart
    }

    private void sweep() {
        try {
            dispatch(registry.tick(System.currentTimeMillis(), this::isOnline, settings.toRules()));
        } catch (RuntimeException ex) {
            BedWarsPlugin.getInstance().getLogger().error("Party sweep failed", ex);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // C3 API
    // ---------------------------------------------------------------------------------------------------------

    public boolean isEnabled() {
        return settings.enabled();
    }

    public @NotNull PartySettings getSettings() {
        return settings;
    }

    public @NotNull Optional<PartyImpl> getParty(@NotNull UUID player) {
        return registry.getParty(player);
    }

    /**
     * Members (including the leader) that are online.
     */
    public int getOnlineSize(@NotNull PartyImpl party) {
        return (int) party.getMembers().stream().filter(this::isOnline).count();
    }

    /**
     * Online members in join order.
     */
    public @NotNull List<Player> getOnlineMembers(@NotNull PartyImpl party) {
        var result = new ArrayList<Player>();
        for (var member : party.getMembers()) {
            var online = Players.getPlayer(member);
            if (online != null) {
                result.add(online);
            }
        }
        return result;
    }

    /**
     * Helper for team assignment: parties together (first appearance order), solo players as singletons.
     */
    public @NotNull List<List<UUID>> groupByParty(@NotNull List<UUID> players) {
        return PartyGrouping.group(players, uuid -> registry.getParty(uuid).map(PartyImpl::getId));
    }

    public boolean isOnline(@NotNull UUID uuid) {
        return Players.getPlayer(uuid) != null;
    }

    /**
     * {@code "/party"} or {@code "/bw party"}, whichever works on this server.
     */
    public @NotNull String commandPrefix() {
        return PartyCommandLabels.commandPrefix(rootRegistered, settings.label());
    }

    @ApiStatus.Internal
    public void setRootRegistered(boolean registered) { // called by PartyCommand (other package)
        this.rootRegistered = registered;
    }

    public boolean isRootRegistered() {
        return rootRegistered;
    }

    /**
     * BedWarsPlugin.isCommandAllowedInGame hook. Thread: main (command preprocess).
     */
    public boolean isPartyCommandAllowedInGame(@Nullable String commandPref) {
        return PartyCommandLabels.matches(commandPref, inGameLabels);
    }

    // ---------------------------------------------------------------------------------------------------------
    // command entry points (main thread; sender = online player)
    // ---------------------------------------------------------------------------------------------------------

    public void invite(@NotNull Player sender, @NotNull String targetName) {
        if (!checkEnabled(sender)) {
            return;
        }
        var name = targetName.trim();
        var target = Players.getPlayerExact(name);
        if (target == null) {
            Message.of(ForkLangKeys.PARTY_ERROR_PLAYER_NOT_FOUND).placeholderRaw("player", name).defaultPrefix().send(sender);
            return;
        }
        int max = sender.hasPermission(BedWarsPermission.PARTY_SIZE_BYPASS_PERMISSION.asPermission()) ? 0 : settings.maxSize();
        var result = registry.invite(sender.getUuid(), sender.getName(), target.getUuid(), target.getName(), max,
                settings.membersCanInvite(), System.currentTimeMillis(), settings.inviteExpireSeconds() * 1000L);
        if (!result.isSuccess()) {
            PartyMessages.error(sender, result.error(), target.getName(), max > 0 ? max : settings.maxSize(), commandPrefix());
            return;
        }
        dispatch(result);
    }

    /**
     * @param from optional inviter or leader name; null = the newest invite
     */
    public void accept(@NotNull Player sender, @Nullable String from) {
        if (!checkEnabled(sender)) {
            return;
        }
        var result = registry.accept(sender.getUuid(), sender.getName(), from, System.currentTimeMillis());
        if (!result.isSuccess()) {
            PartyMessages.error(sender, result.error(), from, settings.maxSize(), commandPrefix());
            return;
        }
        dispatch(result);
    }

    public void deny(@NotNull Player sender, @Nullable String from) {
        if (!checkEnabled(sender)) {
            return;
        }
        var result = registry.deny(sender.getUuid(), from, System.currentTimeMillis());
        if (!result.isSuccess()) {
            PartyMessages.error(sender, result.error(), from, settings.maxSize(), commandPrefix());
            return;
        }
        dispatch(result);
    }

    public void leave(@NotNull Player sender) {
        if (!checkEnabled(sender)) {
            return;
        }
        var result = registry.leave(sender.getUuid(), this::isOnline);
        if (!result.isSuccess()) {
            PartyMessages.error(sender, result.error(), null, settings.maxSize(), commandPrefix());
            return;
        }
        dispatch(result);
    }

    public void kick(@NotNull Player sender, @NotNull String targetName) {
        if (!checkEnabled(sender)) {
            return;
        }
        var result = registry.kick(sender.getUuid(), targetName.trim());
        if (!result.isSuccess()) {
            PartyMessages.error(sender, result.error(), targetName.trim(), settings.maxSize(), commandPrefix());
            return;
        }
        dispatch(result);
    }

    public void disband(@NotNull Player sender) {
        if (!checkEnabled(sender)) {
            return;
        }
        var result = registry.disband(sender.getUuid());
        if (!result.isSuccess()) {
            PartyMessages.error(sender, result.error(), null, settings.maxSize(), commandPrefix());
            return;
        }
        dispatch(result);
    }

    public void transfer(@NotNull Player sender, @NotNull String targetName) {
        if (!checkEnabled(sender)) {
            return;
        }
        var result = registry.transfer(sender.getUuid(), targetName.trim(), this::isOnline);
        if (!result.isSuccess()) {
            PartyMessages.error(sender, result.error(), targetName.trim(), settings.maxSize(), commandPrefix());
            return;
        }
        dispatch(result);
    }

    public void list(@NotNull Player sender) {
        if (!checkEnabled(sender)) {
            return;
        }
        var party = registry.getParty(sender.getUuid()).orElse(null);
        if (party == null) {
            PartyMessages.error(sender, PartyError.NOT_IN_PARTY, null, settings.maxSize(), commandPrefix());
            return;
        }
        var header = Message.of(ForkLangKeys.PARTY_LIST_HEADER).noPrefix().placeholder("count", party.size());
        if (settings.maxSize() <= 0) {
            header.placeholder("max", Message.of(ForkLangKeys.PARTY_LIST_UNLIMITED));
        } else {
            header.placeholder("max", settings.maxSize());
        }
        header.send(sender);
        Message.of(ForkLangKeys.PARTY_LIST_LEADER).defaultPrefix()
                .placeholder("player", listEntry(party, party.getLeader(), sender))
                .send(sender);
        var others = party.getMembersExcept(party.getLeader());
        Component members = others.isEmpty()
                ? Message.of(ForkLangKeys.PARTY_LIST_NONE).asComponent(sender)
                : joinComponents(others.stream().map(uuid -> listEntry(party, uuid, sender)).collect(Collectors.toList()), sender);
        Message.of(ForkLangKeys.PARTY_LIST_MEMBERS).defaultPrefix().placeholder("players", members).send(sender);
        if (!party.getInvites().isEmpty()) {
            var invited = party.getInvites().stream()
                    .map(invite -> {
                        var online = Players.getPlayer(invite.target()) != null;
                        return Message.of(online ? ForkLangKeys.PARTY_LIST_ENTRY_ONLINE : ForkLangKeys.PARTY_LIST_ENTRY_OFFLINE)
                                .placeholderRaw("name", invite.targetName())
                                .asComponent(sender);
                    })
                    .collect(Collectors.toList());
            Message.of(ForkLangKeys.PARTY_LIST_INVITES).defaultPrefix().placeholder("players", joinComponents(invited, sender)).send(sender);
        }
    }

    private @NotNull Component listEntry(@NotNull PartyImpl party, @NotNull UUID uuid, @NotNull Player viewer) {
        var name = party.getName(uuid);
        var safeName = name == null ? "?" : name;
        if (Players.getPlayer(uuid) == null) {
            return Message.of(ForkLangKeys.PARTY_LIST_ENTRY_OFFLINE).placeholderRaw("name", safeName).asComponent(viewer);
        }
        var game = PlayerManagerImpl.getInstance().getGameOfPlayer(uuid).orElse(null);
        if (game != null) {
            return Message.of(ForkLangKeys.PARTY_LIST_ENTRY_IN_GAME)
                    .placeholderRaw("name", safeName)
                    .placeholderRaw("arena", game.getName())
                    .asComponent(viewer);
        }
        return Message.of(ForkLangKeys.PARTY_LIST_ENTRY_ONLINE).placeholderRaw("name", safeName).asComponent(viewer);
    }

    private @NotNull Component joinComponents(@NotNull List<Component> components, @NotNull Player viewer) {
        var builder = Component.text();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                builder.append(Message.of(ForkLangKeys.PARTY_LIST_SEPARATOR).asComponent(viewer));
            }
            builder.append(components.get(i));
        }
        return builder.build();
    }

    // ---------------------------------------------------------------------------------------------------------
    // party chat
    // ---------------------------------------------------------------------------------------------------------

    public void toggleChat(@NotNull Player sender) {
        if (!checkEnabled(sender)) {
            return;
        }
        if (!settings.chatEnabled()) {
            Message.of(ForkLangKeys.COMMON_FEATURE_DISABLED).defaultPrefix().send(sender);
            return;
        }
        if (registry.getParty(sender.getUuid()).isEmpty()) {
            PartyMessages.error(sender, PartyError.NOT_IN_PARTY, null, settings.maxSize(), commandPrefix());
            return;
        }
        if (chatToggled.remove(sender.getUuid())) {
            Message.of(ForkLangKeys.PARTY_CHAT_TOGGLED_OFF).defaultPrefix().send(sender);
        } else {
            chatToggled.add(sender.getUuid());
            Message.of(ForkLangKeys.PARTY_CHAT_TOGGLED_ON).placeholderRaw("bypass", settings.chatBypassPrefix()).defaultPrefix().send(sender);
        }
    }

    /**
     * Sends one message to the party of the sender. MAIN THREAD.
     */
    public void sendChat(@NotNull UUID senderUuid, @NotNull String message) {
        var sender = Players.getPlayer(senderUuid);
        if (sender == null || message.isBlank()) {
            return;
        }
        if (!settings.enabled() || !settings.chatEnabled()) {
            chatToggled.remove(senderUuid);
            Message.of(ForkLangKeys.COMMON_FEATURE_DISABLED).defaultPrefix().send(sender);
            return;
        }
        var party = registry.getParty(senderUuid).orElse(null);
        if (party == null) {
            if (chatToggled.remove(senderUuid)) {
                Message.of(ForkLangKeys.PARTY_CHAT_TOGGLED_OFF_NO_PARTY).defaultPrefix().send(sender);
            } else {
                PartyMessages.error(sender, PartyError.NOT_IN_PARTY, null, settings.maxSize(), commandPrefix());
            }
            return;
        }
        var line = Message.of(ForkLangKeys.PARTY_CHAT_FORMAT)
                .noPrefix()
                .placeholder("sender", sender.getDisplayName())
                .placeholderRaw("message", message.trim()); // raw: no MiniMessage injection
        for (var member : getOnlineMembers(party)) {
            line.send(member);
        }
        if (settings.chatLogToConsole()) {
            line.send(Server.getConsoleSender());
        }
    }

    /**
     * Any thread.
     */
    public boolean isChatToggled(@NotNull UUID uuid) {
        return chatToggled.contains(uuid);
    }

    /**
     * Any thread (volatile settings).
     */
    public @NotNull String getChatBypassPrefix() {
        return settings.chatBypassPrefix();
    }

    // ---------------------------------------------------------------------------------------------------------
    // suggestions (cloud may call them asynchronously: read-only, races are tolerated)
    // ---------------------------------------------------------------------------------------------------------

    public @NotNull List<String> suggestInviters(@NotNull UUID player) {
        try {
            return registry.getInvitesFor(player, System.currentTimeMillis()).stream()
                    .map(PartyInvite::inviterName)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    public @NotNull List<String> suggestMembers(@NotNull UUID player, boolean onlineOnly) {
        try {
            var party = registry.getParty(player).orElse(null);
            if (party == null) {
                return List.of();
            }
            return party.getMembersExcept(player).stream()
                    .filter(uuid -> !onlineOnly || isOnline(uuid))
                    .map(party::getName)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // player connection (from PartyEventListener)
    // ---------------------------------------------------------------------------------------------------------

    void handleQuit(@NotNull Player player) {
        if (isEnabled()) {
            dispatch(registry.markOffline(player.getUuid(), System.currentTimeMillis()));
        }
    }

    void handleJoin(@NotNull Player player) {
        if (isEnabled()) {
            dispatch(registry.markOnline(player.getUuid(), player.getName()));
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    private boolean checkEnabled(@NotNull Player sender) {
        if (settings.enabled()) {
            return true;
        }
        Message.of(ForkLangKeys.COMMON_FEATURE_DISABLED).defaultPrefix().send(sender);
        return false;
    }

    private void dispatch(@NotNull PartyResult result) {
        PartyMessages.dispatch(result, settings, rootRegistered);
        // leaving a party ends the chat mode
        for (var uuid : chatToggled) {
            if (registry.getParty(uuid).isEmpty() && chatToggled.remove(uuid)) {
                var online = Players.getPlayer(uuid);
                if (online != null) {
                    Message.of(ForkLangKeys.PARTY_CHAT_TOGGLED_OFF_NO_PARTY).defaultPrefix().send(online);
                }
            }
        }
    }
}
