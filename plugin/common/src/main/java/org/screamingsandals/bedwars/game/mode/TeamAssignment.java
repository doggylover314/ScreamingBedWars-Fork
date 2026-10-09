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

package org.screamingsandals.bedwars.game.mode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.lang.ForkLangKeys;
import org.screamingsandals.bedwars.lib.debug.Debug;
import org.screamingsandals.bedwars.player.BedWarsPlayer;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.spectator.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter between live games and the pure team assignment classes; all team assignment of the modes feature
 * (mode games and party-aware classic games) goes through here (contract C4).
 */
public final class TeamAssignment {
    private TeamAssignment() {
    }

    // ---------- mode games ----------

    /**
     * Called from GameImpl#makePlayersJoinRandomTeams() for mode games (prepareGame, start item, force start).
     */
    public static void assignModeTeams(@NotNull GameImpl game) {
        var mode = game.getActiveMode();
        if (mode == null) {
            return;
        }
        var players = game.getConnectedPlayers().stream().filter(p -> !p.isSpectator()).collect(Collectors.toList());
        if (players.isEmpty()) {
            return;
        }
        if (players.stream().allMatch(p -> game.getPlayerTeam(p) != null)) {
            return; // already assigned (force start / start item) or picked by the players themselves
        }
        game.resetLobbyTeams();
        var dist = distributeForMode(buildGroups(game, players, false), mode.teamSize(), game.getTeams().size());
        var order = TeamOrder.order(game.getTeams().stream().map(TeamImpl::getName).collect(Collectors.toList()),
                game.getConfigurationContainer().getOrDefault(ModeConfigKeys.TEAM_PRIORITY, List.of()));
        var byUuid = players.stream().collect(Collectors.toMap(BedWarsPlayer::getUuid, p -> p, (a, b) -> a));
        for (int i = 0; i < dist.teams().size() && i < order.size(); i++) {
            var team = game.getTeamFromName(order.get(i));
            for (var group : dist.teams().get(i)) {
                for (var uuid : group.members()) {
                    var player = byUuid.get(uuid);
                    if (player != null && team != null) {
                        game.internalTeamJoin(player, team, true);
                    }
                    if (player != null && group.fragment()) {
                        notifySplit(game, player);
                    }
                }
            }
        }
        // dist.unassigned() (only if more players than capacity) stay teamless -> spectators at start, as today
        Debug.info(game.getName() + ": mode " + mode.id() + " teams " + dist.teamSizes());
    }

    /**
     * Countdown condition for mode games (GameImpl#isAllowedToStart).
     */
    public static boolean canFormModeTeams(@NotNull GameImpl game) {
        var mode = game.getActiveMode();
        if (mode == null) {
            return false;
        }
        var players = game.getConnectedPlayers().stream().filter(p -> !p.isSpectator()).collect(Collectors.toList());
        if (!players.isEmpty() && players.stream().allMatch(p -> game.getPlayerTeam(p) != null)) {
            return game.getTeamsInGame().size() > 1; // team selection allowed and everybody picked: keep picks
        }
        return distributeForMode(buildGroups(game, players, false), mode.teamSize(), game.getTeams().size()).isStartable();
    }

    /**
     * D49 last resort: if the strict distribution leaves groups unassigned although everybody fits by count (party
     * formed inside the lobby, admin joins), split only the unassigned groups into singles (fragments -> notifySplit).
     */
    static ModeTeamDistributor.Distribution distributeForMode(List<PlayerGroup> groups, int teamSize, int teamCount) {
        var dist = ModeTeamDistributor.distribute(groups, teamSize, teamCount);
        int total = groups.stream().mapToInt(PlayerGroup::size).sum();
        if (dist.unassigned().isEmpty() || total > teamSize * teamCount) {
            return dist;
        }
        var unplaced = dist.unassigned().stream().map(PlayerGroup::id).collect(Collectors.toSet());
        var retry = new ArrayList<PlayerGroup>();
        for (var g : groups) {
            boolean hit = unplaced.contains(g.id()) || unplaced.stream().anyMatch(id -> id.startsWith(g.id() + "#"));
            if (hit) {
                retry.addAll(g.splitInto(1));
            } else {
                retry.add(g);
            }
        }
        return ModeTeamDistributor.distribute(retry, teamSize, teamCount);
    }

    /**
     * Largest group that can still join this WAITING mode lobby; the group {@code groupKey} (its members already in the
     * game) counts as part of the new group. -1 = no limit (no active mode, not waiting, parties not kept together).
     */
    static int largestJoinableGroup(@NotNull GameImpl game, @NotNull String groupKey) {
        var mode = game.getActiveMode();
        if (mode == null || game.getStatus() != GameStatus.WAITING || !keepParties(game)) {
            return -1;
        }
        var players = game.getConnectedPlayers().stream().filter(p -> !p.isSpectator()).collect(Collectors.toList());
        var others = buildGroups(game, players, false).stream()
                .filter(g -> !g.id().equals(groupKey))
                .map(PlayerGroup::size)
                .collect(Collectors.toCollection(ArrayList::new));
        int free = mode.maxPlayers() - others.stream().mapToInt(Integer::intValue).sum();
        for (int size = Math.min(mode.teamSize(), free); size >= 1; size--) {
            others.add(size);
            boolean ok = ModeTeamDistributor.fits(others, mode.teamSize(), game.getTeams().size());
            others.remove(others.size() - 1);
            if (ok) {
                return size;
            }
        }
        return 0;
    }

    /**
     * GameImpl#joinToGame guard (D49): refuses a party member whose group (members already in this mode lobby + him)
     * could no longer be placed, e.g. a member following his leader.
     *
     * @return true if refused (message sent)
     */
    public static boolean refuseUnplaceableJoin(@NotNull GameImpl game, @NotNull BedWarsPlayer player) {
        if (game.getActiveMode() == null || game.getStatus() != GameStatus.WAITING || !keepParties(game)) {
            return false;
        }
        var party = ModePartyBridge.partyOf(player.getUuid());
        if (party.isEmpty()) {
            return false;
        }
        int present = (int) game.getConnectedPlayers().stream()
                .filter(p -> p != player && !p.isSpectator() && party.get().members().contains(p.getUuid()))
                .count();
        if (present == 0) {
            return false; // a new group of one always fits while a seat is free
        }
        int limit = largestJoinableGroup(game, "party:" + party.get().leader());
        if (limit < 0 || present + 1 <= limit) {
            return false;
        }
        Message.of(ForkLangKeys.PARTY_JOIN_TOO_BIG_FOR_TEAM)
                .placeholderRaw("arena", game.getName())
                .placeholder("size", present + 1)
                .placeholder("max", Math.max(1, limit))
                .defaultPrefix()
                .send(player);
        return true;
    }

    // ---------- classic games ----------

    /**
     * Party-aware replacement of the old makePlayersJoinRandomTeams loop.
     */
    public static void fillTeamsClassic(@NotNull GameImpl game) {
        var teamless = game.getConnectedPlayers().stream()
                .filter(p -> !p.isSpectator() && game.getPlayerTeam(p) == null)
                .collect(Collectors.toList());
        if (teamless.isEmpty()) {
            return;
        }
        var groups = buildGroups(game, teamless, true);
        if (groups.stream().allMatch(g -> g.size() == 1 && g.preferredTeam() == null)) {
            teamless.forEach(game::joinRandomTeam); // nobody in a party: exactly today's behaviour
            return;
        }
        var result = ClassicTeamFiller.fill(slotsOf(game), groups);
        var byUuid = teamless.stream().collect(Collectors.toMap(BedWarsPlayer::getUuid, p -> p, (a, b) -> a));
        for (var placement : result.placements()) {
            var player = byUuid.get(placement.member());
            var team = game.getTeamFromName(placement.team());
            if (player != null && team != null) {
                game.internalTeamJoin(player, team, false);
            }
        }
        groups.stream()
                .filter(g -> result.splitGroups().contains(g.id()))
                .flatMap(g -> g.members().stream())
                .map(byUuid::get)
                .filter(Objects::nonNull)
                .forEach(p -> notifySplit(game, p));
    }

    /**
     * JOIN_RANDOM_TEAM_ON_JOIN / API selectPlayerRandomTeam for classic games.
     */
    public static @Nullable TeamImpl chooseTeamForJoiningPlayer(@NotNull GameImpl game, @NotNull BedWarsPlayer player) {
        if (keepParties(game)) {
            var party = ModePartyBridge.partyOf(player.getUuid());
            if (party.isPresent() && party.get().onlineSize() > 1) {
                int alreadyTeamed = 0;
                for (var memberUuid : party.get().members()) {
                    if (memberUuid.equals(player.getUuid())) {
                        continue;
                    }
                    var member = PlayerManagerImpl.getInstance().getPlayer(memberUuid).orElse(null);
                    if (member == null || member.getGame() != game) {
                        continue;
                    }
                    var team = game.getPlayerTeam(member);
                    if (team == null) {
                        continue;
                    }
                    alreadyTeamed++;
                    if (team.countConnectedPlayers() < team.getMaxPlayers()) {
                        return team; // join the party's team
                    }
                }
                var name = ClassicTeamFiller.chooseTeam(slotsOf(game), Math.max(1, party.get().onlineSize() - alreadyTeamed));
                if (name != null) {
                    return game.getTeamFromName(name);
                }
            }
        }
        return game.chooseRandomTeamForPlayerToJoin(false, false);
    }

    /**
     * Manual selection (GUI / armor stand / API) in classic games.
     *
     * @return true if handled (party moved or refused with a message), false = caller does the normal single-player join
     */
    public static boolean selectTeamForParty(@NotNull GameImpl game, @NotNull BedWarsPlayer player, @NotNull TeamImpl team) {
        if (!keepParties(game)) {
            return false;
        }
        var party = ModePartyBridge.partyOf(player.getUuid());
        if (party.isEmpty()) {
            return false;
        }
        var inGame = party.get().members().stream()
                .map(uuid -> PlayerManagerImpl.getInstance().getPlayer(uuid).orElse(null))
                .filter(Objects::nonNull)
                .filter(m -> m.getGame() == game)
                .collect(Collectors.toList());
        if (inGame.size() <= 1) {
            return false;
        }
        if (inGame.stream().noneMatch(m -> m.getUuid().equals(party.get().leader()))) {
            return false;
        }
        if (!party.get().leader().equals(player.getUuid())) {
            Message.of(ForkLangKeys.MODES_PARTY_TEAM_CHOICE_LEADER_ONLY)
                    .prefixOrDefault(game.getCustomPrefixComponent())
                    .send(player);
            return true;
        }
        int moving = (int) inGame.stream().filter(m -> game.getPlayerTeam(m) != team).count();
        if (team.countConnectedPlayers() + moving > team.getMaxPlayers()) {
            Message.of(ForkLangKeys.MODES_PARTY_TEAM_FULL)
                    .prefixOrDefault(game.getCustomPrefixComponent())
                    .placeholder("team", Component.text(team.getName(), team.getColor().getTextColor()))
                    .placeholder("needed", inGame.size())
                    .send(player);
            return true;
        }
        if (game.getPlayerTeam(player) != team) {
            game.internalTeamJoin(player, team, true); // leader first
        }
        for (var member : inGame) {
            if (member != player && game.getPlayerTeam(member) != team) {
                game.internalTeamJoin(member, team, true);
            }
        }
        return true;
    }

    // ---------- helpers ----------

    /**
     * Groups in join order; key = party leader (if parties are kept together) or the player himself.
     */
    static @NotNull List<PlayerGroup> buildGroups(@NotNull GameImpl game, @NotNull List<BedWarsPlayer> players, boolean withPreferredTeams) {
        boolean keep = keepParties(game);
        var members = new LinkedHashMap<String, List<UUID>>();
        for (var p : players) {
            members.computeIfAbsent(groupKey(p, keep), k -> new ArrayList<>()).add(p.getUuid());
        }
        var preferred = new HashMap<String, String>();
        if (withPreferredTeams) {
            for (var p : game.getConnectedPlayers()) {
                var team = game.getPlayerTeam(p);
                if (team != null) {
                    preferred.putIfAbsent(groupKey(p, keep), team.getName());
                }
            }
        }
        return members.entrySet().stream()
                .map(e -> new PlayerGroup(e.getKey(), e.getValue(), preferred.get(e.getKey()), false))
                .collect(Collectors.toList());
    }

    /**
     * Must stay in sync with the key built in {@link #refuseUnplaceableJoin} and {@link ModeManager#largestJoinableGroup}.
     */
    private static String groupKey(BedWarsPlayer p, boolean keep) {
        if (!keep) {
            return "solo:" + p.getUuid();
        }
        return ModePartyBridge.partyOf(p.getUuid()).map(info -> "party:" + info.leader()).orElse("solo:" + p.getUuid());
    }

    static @NotNull List<ClassicTeamFiller.TeamSlot> slotsOf(@NotNull GameImpl game) {
        var inGame = game.getTeamsInGame();
        return game.getTeams().stream()
                .map(t -> new ClassicTeamFiller.TeamSlot(t.getName(), t.countConnectedPlayers(), t.getMaxPlayers(), inGame.indexOf(t)))
                .collect(Collectors.toList());
    }

    private static boolean keepParties(GameImpl game) {
        return game.getConfigurationContainer().getOrDefault(ModeConfigKeys.KEEP_PARTIES_TOGETHER, true);
    }

    private static void notifySplit(GameImpl game, BedWarsPlayer p) {
        Message.of(ForkLangKeys.MODES_PARTY_SPLIT).prefixOrDefault(game.getCustomPrefixComponent()).send(p);
    }
}
