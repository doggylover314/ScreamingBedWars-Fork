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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Decides whether a party leader can bring his party into a game and who has to be moved (PURE).
 */
public final class PartyJoinPlanner {
    private PartyJoinPlanner() {
    }

    public enum MemberState {
        /** not online */
        OFFLINE,
        /** already in the target game */
        IN_TARGET,
        /** online and not busy (no game, other lobby, spectator, end celebration) */
        FREE,
        /** playing in a running game (still in a team) */
        PLAYING_ELSEWHERE
    }

    public record Member(@NotNull UUID uuid, @NotNull String name, @NotNull MemberState state) {
    }

    public enum Outcome {
        OK,
        NO_ROOM,
        TOO_BIG_FOR_TEAM
    }

    /**
     * @param movers   members that have to be moved into the game
     * @param busy     members that are playing elsewhere and stay there
     * @param needed   free slots the join needs (movers + the leader unless he is already in the game)
     * @param groupSize leader + members already in the target + movers
     */
    public record Plan(@NotNull Outcome outcome, @NotNull List<Member> movers, @NotNull List<Member> busy,
                       int needed, int freeSlots, int groupSize, int maxTeamSize) {
    }

    /**
     * @param leaderInTarget leader already in the target game
     * @param others         every member except the leader
     * @param freeSlots      target maxPlayers - connected players (may be negative)
     * @param maxTeamSize    largest team size of the target; {@code <= 0} = do not check
     * @param pullPlaying    move PLAYING_ELSEWHERE members as well
     */
    public static @NotNull Plan plan(boolean leaderInTarget, @NotNull List<Member> others, int freeSlots, int maxTeamSize, boolean pullPlaying) {
        var movers = new ArrayList<Member>();
        var busy = new ArrayList<Member>();
        int present = 0;
        for (var member : others) {
            switch (member.state()) {
                case FREE:
                    movers.add(member);
                    break;
                case PLAYING_ELSEWHERE:
                    if (pullPlaying) {
                        movers.add(member);
                    } else {
                        busy.add(member);
                    }
                    break;
                case IN_TARGET:
                    present++;
                    break;
                case OFFLINE:
                default:
                    break;
            }
        }
        int needed = movers.size() + (leaderInTarget ? 0 : 1);
        int groupSize = 1 + present + movers.size();
        var outcome = Outcome.OK;
        if (maxTeamSize > 0 && groupSize > maxTeamSize) {
            outcome = Outcome.TOO_BIG_FOR_TEAM; // permanent problem first
        } else if (needed > 0 && needed > freeSlots) {
            outcome = Outcome.NO_ROOM; // nobody to move -> never "no room"
        }
        return new Plan(outcome, List.copyOf(movers), List.copyOf(busy), needed, freeSlots, groupSize, maxTeamSize);
    }
}
