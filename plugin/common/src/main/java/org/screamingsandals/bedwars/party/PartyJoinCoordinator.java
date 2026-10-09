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

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.commands.BedWarsPermission;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.mode.ModeManager;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.party.PartyJoinPlanner.Member;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.bedwars.utils.EconomyUtils;
import org.screamingsandals.lib.economy.EconomyManager;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.player.Players;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.ServiceDependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.screamingsandals.bedwars.party.PartyJoinPlanner.MemberState.FREE;
import static org.screamingsandals.bedwars.party.PartyJoinPlanner.MemberState.IN_TARGET;
import static org.screamingsandals.bedwars.party.PartyJoinPlanner.MemberState.OFFLINE;
import static org.screamingsandals.bedwars.party.PartyJoinPlanner.MemberState.PLAYING_ELSEWHERE;

/**
 * Makes every join path party-aware (contract C3): a leader's join becomes a party join, a non-leader's own join is
 * refused when the arena requires the leader, and {@code /party warp} brings the party to the leader.
 * <p>
 * Main thread only. The service is stateless per call; the bypass sets are transient.
 */
@Service
@ServiceDependencies(dependsOn = {
        PartyManagerImpl.class,
        PlayerManagerImpl.class
})
@RequiredArgsConstructor
public class PartyJoinCoordinator {
    private final PartyManagerImpl partyManager;
    /** players whose joinToGame is driven by this class */
    private final Set<UUID> bypassing = new HashSet<>();
    /** runBypassing(Runnable) */
    private int globalBypassDepth;

    public static @NotNull PartyJoinCoordinator getInstance() {
        return ServiceManager.get(PartyJoinCoordinator.class);
    }

    /**
     * Called at the start of {@code GameImpl#joinToGame}.
     *
     * @return {@code true} = continue the normal join, {@code false} = this class handled (or refused) the join
     */
    public boolean handleJoinRequest(@NotNull BedWarsPlayer player, @NotNull GameImpl game) {
        if (!partyManager.isEnabled() || GameImpl.isBungeeEnabled() || isBypassing(player.getUuid())) {
            return true;
        }
        if (game.getStatus() != GameStatus.WAITING) {
            return true; // spectator joins / "already running" etc.: normal flow
        }
        if (player.getGame() == game) {
            return true; // legacy behaviour unchanged
        }
        var party = partyManager.getParty(player.getUuid()).orElse(null);
        if (party == null) {
            return true;
        }
        if (!party.isLeader(player.getUuid())) {
            return mayJoinOnOwn(player, game, true);
        }
        boolean pull = game.getConfigurationContainer().getOrDefault(PartyConfigKeys.AUTOJOIN_MEMBERS, partyManager.getSettings().autojoinMembers());
        if (!pull || partyManager.getOnlineSize(party) <= 1) {
            return true;
        }
        joinWithParty(player, game);
        return false; // the party join did (or refused) everything
    }

    private boolean isBypassing(@NotNull UUID uuid) {
        return globalBypassDepth > 0 || bypassing.contains(uuid);
    }

    /**
     * Whether the player may join a game without his party leader ({@code target} may be null when no arena is chosen yet).
     *
     * @param notify tell the player why he cannot
     */
    public boolean mayJoinOnOwn(@NotNull BedWarsPlayer player, @Nullable GameImpl target, boolean notify) {
        if (!partyManager.isEnabled() || GameImpl.isBungeeEnabled() || isBypassing(player.getUuid())) {
            return true;
        }
        var party = partyManager.getParty(player.getUuid()).orElse(null);
        if (party == null || party.isLeader(player.getUuid())) {
            return true;
        }
        boolean require = target != null
                ? target.getConfigurationContainer().getOrDefault(PartyConfigKeys.REQUIRE_LEADER_TO_JOIN, partyManager.getSettings().requireLeaderToJoin())
                : partyManager.getSettings().requireLeaderToJoin();
        if (!require) {
            return true;
        }
        if (player.hasPermission(BedWarsPermission.PARTY_JOIN_BYPASS_PERMISSION.asPermission())) {
            return true;
        }
        if (!partyManager.isOnline(party.getLeader())) {
            return true; // do not lock members out during the grace period
        }
        if (target != null && PlayerManagerImpl.getInstance().getGameOfPlayer(party.getLeader()).orElse(null) == target) {
            return true; // follow the leader
        }
        if (notify) {
            Message.of(ForkLangKeys.PARTY_JOIN_ONLY_LEADER)
                    .placeholder("leader", PartyMessages.name(party.getLeader(), party.getLeaderName()))
                    .placeholderRaw("cmd", partyManager.commandPrefix())
                    .defaultPrefix()
                    .send(player);
        }
        return false;
    }

    /**
     * Joins the leader and pulls his party members into the game (call ONCE for the leader, never loop over members).
     *
     * @return whether the player ended up in the game
     */
    public boolean joinWithParty(@NotNull BedWarsPlayer player, @NotNull GameImpl game) {
        var party = partyManager.isEnabled() ? partyManager.getParty(player.getUuid()).orElse(null) : null;
        if (party == null || !party.isLeader(player.getUuid()) || GameImpl.isBungeeEnabled() || game.getStatus() != GameStatus.WAITING
                || !game.getConfigurationContainer().getOrDefault(PartyConfigKeys.AUTOJOIN_MEMBERS, partyManager.getSettings().autojoinMembers())) {
            game.joinToGame(player); // the hook applies the non-leader rule / passes the leader through
            return player.getGame() == game;
        }
        var plan = planFor(player, party, game);
        if (plan.outcome() != PartyJoinPlanner.Outcome.OK) {
            sendPlanError(player, game, plan);
            return false;
        }
        if (player.getGame() != game) {
            boolean joined = withBypass(List.of(player.getUuid()), () -> moveToGame(player, game));
            if (!joined) {
                return false; // fee / event cancelled: joinToGame already told him; nobody is pulled
            }
        }
        pullMembers(player, game, plan);
        return true;
    }

    private void pullMembers(@NotNull BedWarsPlayer leader, @NotNull GameImpl game, @NotNull PartyJoinPlanner.Plan plan) {
        var failed = new ArrayList<String>();
        int moved = 0;
        for (var mover : plan.movers()) {
            var online = Players.getPlayer(mover.uuid());
            if (online == null) {
                continue;
            }
            var member = PlayerManagerImpl.getInstance().getPlayerOrCreate(online);
            if (member.getGame() == game) {
                continue;
            }
            boolean ok = withBypass(List.of(mover.uuid()), () -> moveToGame(member, game));
            if (ok) {
                moved++;
                Message.of(LangKeys.PARTY_INFORM_GAME_JOIN).defaultPrefix().send(member); // "Joining game of party leader"
            } else {
                failed.add(mover.name());
            }
        }
        for (var busy : plan.busy()) {
            var online = Players.getPlayer(busy.uuid());
            if (online != null) {
                Message.of(ForkLangKeys.PARTY_JOIN_MEMBER_BUSY).placeholderRaw("arena", game.getName()).defaultPrefix().send(online);
            }
            Message.of(ForkLangKeys.PARTY_JOIN_MEMBER_BUSY_LEADER).placeholderRaw("player", busy.name()).defaultPrefix().send(leader);
        }
        for (var name : failed) {
            Message.of(ForkLangKeys.PARTY_JOIN_MEMBER_FAILED).placeholderRaw("player", name).defaultPrefix().send(leader);
        }
        if (moved > 0 && partyManager.getSettings().notifyWhenWarped()) {
            Message.of(LangKeys.PARTY_COMMAND_WARPED).defaultPrefix().send(leader); // once (the old listener sent it per member)
        }
    }

    private boolean withBypass(@NotNull Collection<UUID> uuids, @NotNull BooleanSupplier action) {
        var added = new ArrayList<UUID>();
        for (var uuid : uuids) {
            if (bypassing.add(uuid)) {
                added.add(uuid);
            }
        }
        try {
            return action.getAsBoolean();
        } finally {
            bypassing.removeAll(added);
        }
    }

    /**
     * Runs the action without any party rules (admin / forced joins, for example {@code /bw alljoin}).
     */
    public void runBypassing(@NotNull Runnable runnable) {
        globalBypassDepth++;
        try {
            runnable.run();
        } finally {
            globalBypassDepth--;
        }
    }

    /**
     * Member states for the planner. {@code target == null}: nobody is ever IN_TARGET (no arena chosen yet).
     */
    private @NotNull List<Member> memberStates(@NotNull BedWarsPlayer leader, @NotNull PartyImpl party, @Nullable GameImpl target) {
        var others = new ArrayList<Member>();
        for (var uuid : party.getMembersExcept(leader.getUuid())) {
            var name = Objects.requireNonNullElse(party.getName(uuid), "?");
            if (!partyManager.isOnline(uuid)) {
                others.add(new Member(uuid, name, OFFLINE));
                continue;
            }
            var bw = PlayerManagerImpl.getInstance().getPlayer(uuid).orElse(null);
            var game = bw == null ? null : bw.getGame();
            if (target != null && game == target) {
                others.add(new Member(uuid, name, IN_TARGET)); // target null: never IN_TARGET
            } else if (game != null && game.getStatus() == GameStatus.RUNNING && game.getPlayerTeam(bw) != null) {
                // still in a team = playing (final-dead players are removed from team.getPlayers())
                others.add(new Member(uuid, name, PLAYING_ELSEWHERE));
            } else {
                others.add(new Member(uuid, name, FREE)); // no game, other lobby, spectator, end celebration
            }
        }
        return others;
    }

    /**
     * Adapter from live objects to the pure planner.
     */
    @NotNull PartyJoinPlanner.Plan planFor(@NotNull BedWarsPlayer leader, @NotNull PartyImpl party, @NotNull GameImpl game) {
        var settings = partyManager.getSettings();
        var others = memberStates(leader, party, game);
        int free = game.getMaxPlayers() - game.countConnectedPlayers();
        int maxTeam = settings.mustFitOneTeam() ? maxTeamSize(game) : 0;
        int modeLimit = ModeManager.largestJoinableGroup(game, leader.getUuid()); // -1 = no mode limit; 0 = full (NO_ROOM reports it)
        if (modeLimit > 0) {
            maxTeam = maxTeam > 0 ? Math.min(maxTeam, modeLimit) : modeLimit;
        }
        return PartyJoinPlanner.plan(leader.getGame() == game, others, free, maxTeam, settings.pullFromRunningGames());
    }

    /**
     * Largest team size of the arena (runtime size: {@link TeamImpl#getMaxPlayers()} reflects an active mode).
     */
    int maxTeamSize(@NotNull GameImpl game) {
        return game.getAvailableTeams().stream().mapToInt(TeamImpl::getMaxPlayers).max().orElse(0);
    }

    /**
     * Seats a mode join needs before the arena is known: 1 + the members {@link #joinWithParty} would really move.
     */
    public int seatsFor(@NotNull BedWarsPlayer player) {
        var party = partyManager.isEnabled() ? partyManager.getParty(player.getUuid()).orElse(null) : null;
        if (party == null || !party.isLeader(player.getUuid()) || GameImpl.isBungeeEnabled()
                || !partyManager.getSettings().autojoinMembers()) {
            return 1;
        }
        return PartyJoinPlanner.plan(false, memberStates(player, party, null), Integer.MAX_VALUE, 0,
                partyManager.getSettings().pullFromRunningGames()).needed();
    }

    /**
     * Free slots this join needs in the game (party-aware; 1 for a solo player, 0 if he is already in the game).
     */
    public int requiredSlots(@NotNull BedWarsPlayer player, @NotNull GameImpl game) {
        int solo = player.getGame() == game ? 0 : 1;
        var party = partyManager.isEnabled() ? partyManager.getParty(player.getUuid()).orElse(null) : null;
        if (party == null || !party.isLeader(player.getUuid()) || GameImpl.isBungeeEnabled()) {
            return solo;
        }
        if (!game.getConfigurationContainer().getOrDefault(PartyConfigKeys.AUTOJOIN_MEMBERS, partyManager.getSettings().autojoinMembers())) {
            return solo;
        }
        return planFor(player, party, game).needed();
    }

    private void sendPlanError(@NotNull BedWarsPlayer leader, @NotNull GameImpl game, @NotNull PartyJoinPlanner.Plan plan) {
        switch (plan.outcome()) {
            case NO_ROOM:
                Message.of(ForkLangKeys.PARTY_JOIN_NO_ROOM)
                        .placeholderRaw("arena", game.getName())
                        .placeholder("needed", plan.needed())
                        .placeholder("free", Math.max(0, plan.freeSlots()))
                        .defaultPrefix()
                        .send(leader);
                break;
            case TOO_BIG_FOR_TEAM:
                Message.of(ForkLangKeys.PARTY_JOIN_TOO_BIG_FOR_TEAM)
                        .placeholderRaw("arena", game.getName())
                        .placeholder("size", plan.groupSize())
                        .placeholder("max", plan.maxTeamSize())
                        .defaultPrefix()
                        .send(leader);
                break;
            default:
                break;
        }
    }

    /**
     * Joins the target; if the player was in another game he switches (BedWarsPlayer.changeGame: leave old + join new,
     * the stored inventory of the FIRST join is kept). On success the old game's fee is refunded exactly like
     * {@code GameImpl#leaveFromGame} does. Also used by {@code /bw alljoin}.
     *
     * @return true when the player ends in the target game
     */
    public static boolean moveToGame(@NotNull BedWarsPlayer player, @NotNull GameImpl target) {
        var old = player.getGame();
        target.joinToGame(player);
        boolean ok = player.getGame() == target;
        if (ok && old != null && old != target
                && MainConfig.getInstance().node("economy", "enabled").getBoolean(true) && EconomyManager.isAvailable()
                && old.getFee() > 0 && MainConfig.getInstance().node("economy", "return-fee").getBoolean(true)) {
            EconomyUtils.deposit(player, old.getFee());
        }
        return ok;
    }

    // ---------------------------------------------------------------------------------------------------------
    // /party warp
    // ---------------------------------------------------------------------------------------------------------

    public void warp(@NotNull Player sender) {
        var settings = partyManager.getSettings();
        if (!settings.enabled() || !settings.warpEnabled()) {
            Message.of(ForkLangKeys.COMMON_FEATURE_DISABLED).defaultPrefix().send(sender);
            return;
        }
        var party = partyManager.getParty(sender.getUuid()).orElse(null);
        if (party == null) {
            Message.of(LangKeys.PARTY_COMMAND_NOT_IN_PARTY).defaultPrefix().send(sender);
            return;
        }
        if (!party.isLeader(sender.getUuid())) {
            Message.of(LangKeys.PARTY_COMMAND_NOT_PARTY_LEADER).defaultPrefix().send(sender);
            return;
        }
        var others = new ArrayList<Player>();
        for (var online : partyManager.getOnlineMembers(party)) {
            if (!online.getUuid().equals(sender.getUuid())) {
                others.add(online);
            }
        }
        if (others.isEmpty()) {
            Message.of(LangKeys.PARTY_COMMAND_IS_EMPTY).defaultPrefix().send(sender);
            return;
        }
        var leader = PlayerManagerImpl.getInstance().getPlayerOrCreate(sender);
        var game = leader.getGame();
        if (game != null) {
            if (game.getStatus() != GameStatus.WAITING) {
                Message.of(ForkLangKeys.PARTY_WARP_GAME_STARTED).defaultPrefix().send(sender);
                return;
            }
            var plan = planFor(leader, party, game); // leaderInTarget = true
            if (plan.outcome() != PartyJoinPlanner.Outcome.OK) {
                sendPlanError(leader, game, plan);
                return;
            }
            pullMembers(leader, game, plan);
            return;
        }
        if (!settings.warpTeleportOutsideGames()) {
            Message.of(ForkLangKeys.COMMON_FEATURE_DISABLED).defaultPrefix().send(sender);
            return;
        }
        int moved = 0;
        for (var member : others) {
            var bw = PlayerManagerImpl.getInstance().getPlayer(member.getUuid()).orElse(null);
            var memberGame = bw == null ? null : bw.getGame();
            if (memberGame != null && memberGame.getStatus() == GameStatus.RUNNING && memberGame.getPlayerTeam(bw) != null
                    && !settings.pullFromRunningGames()) {
                Message.of(ForkLangKeys.PARTY_JOIN_MEMBER_BUSY_LEADER).placeholderRaw("player", member.getName()).defaultPrefix().send(sender);
                continue;
            }
            if (memberGame != null) {
                memberGame.leaveFromGame(bw); // restoreInv + (main lobby) teleports happen first
                Tasker.runDelayed(member, () -> {
                    if (!member.isOnline() || !sender.isOnline() || PlayerManagerImpl.getInstance().isPlayerInGame(member)) {
                        return;
                    }
                    member.teleport(sender.getLocation());
                    Message.of(LangKeys.PARTY_WARPED).defaultPrefix().send(member);
                }, settings.warpTeleportDelayTicks(), TaskerTime.TICKS);
            } else {
                member.teleport(sender.getLocation());
                Message.of(LangKeys.PARTY_WARPED).defaultPrefix().send(member);
            }
            moved++;
        }
        if (moved > 0 && settings.notifyWhenWarped()) {
            Message.of(LangKeys.PARTY_COMMAND_WARPED).defaultPrefix().send(sender);
        }
    }
}
