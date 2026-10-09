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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Party-aware version of the random team choice of classic games (pure).
 */
public final class ClassicTeamFiller {
    /**
     * @param activationIndex position in GameImpl.teamsInGame, -1 = team not in game yet
     */
    public record TeamSlot(@NotNull String name, int players, int maxPlayers, int activationIndex) {
        public boolean active() {
            return activationIndex >= 0;
        }

        public int free() {
            return Math.max(0, maxPlayers - players);
        }
    }

    public record Placement(@NotNull UUID member, @NotNull String team) {
    }

    public record Result(@NotNull List<Placement> placements, @NotNull List<UUID> unplaced, @NotNull Set<String> splitGroups) {
    }

    private ClassicTeamFiller() {
    }

    /**
     * Same rule as GameImpl#chooseRandomTeamForPlayerToJoin(false, false), generalised to a group of {@code size}.
     */
    public static @Nullable String chooseTeam(@NotNull List<TeamSlot> slots, int size) {
        long active = slots.stream().filter(TeamSlot::active).count();
        if (active < 2) {
            return firstInactiveWithRoom(slots, size);
        }
        TeamSlot lowest = null;
        for (var slot : slots.stream().filter(TeamSlot::active)
                .sorted(Comparator.comparingInt(TeamSlot::activationIndex)).collect(Collectors.toList())) {
            if (slot.free() < size) {
                continue;
            }
            if (lowest == null || slot.players() < lowest.players()) {
                lowest = slot;
            }
        }
        return lowest != null ? lowest.name() : firstInactiveWithRoom(slots, size);
    }

    private static @Nullable String firstInactiveWithRoom(List<TeamSlot> slots, int size) {
        return slots.stream().filter(s -> !s.active() && s.maxPlayers() >= size).map(TeamSlot::name).findFirst().orElse(null);
    }

    /**
     * Placement order: groups that already have a team to follow first (their room is reserved in that team), then the
     * groups that fit a team, the biggest first so that parties are not split by solos that arrived earlier, and last
     * the groups too big for any team (they are split anyway and fill the remaining seats). Stable, so equal groups
     * (all solos) keep their join order.
     */
    private static @NotNull List<PlayerGroup> placementOrder(@NotNull List<TeamSlot> slots, @NotNull List<PlayerGroup> groups) {
        int largestTeam = slots.stream().mapToInt(TeamSlot::maxPlayers).max().orElse(0);
        var ordered = new ArrayList<>(groups);
        ordered.sort(Comparator
                .comparingInt((PlayerGroup g) -> g.preferredTeam() != null ? 0 : 1)
                .thenComparingInt(g -> g.size() > largestTeam ? 1 : 0)
                .thenComparing(Comparator.comparingInt(PlayerGroup::size).reversed()));
        return ordered;
    }

    public static @NotNull Result fill(@NotNull List<TeamSlot> slots, @NotNull List<PlayerGroup> groups) {
        var work = new ArrayList<>(slots); // replaced by updated copies while placing
        int nextActivation = slots.stream().mapToInt(TeamSlot::activationIndex).max().orElse(-1) + 1;
        var placements = new ArrayList<Placement>();
        var unplaced = new ArrayList<UUID>();
        var split = new LinkedHashSet<String>();
        for (var group : placementOrder(slots, groups)) {
            String target = null;
            if (group.preferredTeam() != null) {
                var pref = find(work, group.preferredTeam());
                if (pref != null && pref.free() >= group.size()) {
                    target = pref.name();
                }
            }
            if (target == null) {
                target = chooseTeam(work, group.size());
            }
            if (target != null) {
                nextActivation = place(work, target, group.members(), placements, nextActivation);
            } else if (group.size() > 1) {
                split.add(group.id());
                for (var member : group.members()) {
                    var single = chooseTeam(work, 1);
                    if (single == null) {
                        unplaced.add(member);
                    } else {
                        nextActivation = place(work, single, List.of(member), placements, nextActivation);
                    }
                }
            } else {
                unplaced.add(group.members().get(0));
            }
        }
        return new Result(placements, unplaced, split);
    }

    private static int place(List<TeamSlot> work, String teamName, List<UUID> members, List<Placement> placements, int nextActivation) {
        for (int i = 0; i < work.size(); i++) {
            var slot = work.get(i);
            if (!slot.name().equals(teamName)) {
                continue;
            }
            int activation = slot.activationIndex();
            if (!slot.active()) {
                activation = nextActivation++;
            }
            work.set(i, new TeamSlot(slot.name(), slot.players() + members.size(), slot.maxPlayers(), activation));
            break;
        }
        for (var member : members) {
            placements.add(new Placement(member, teamName));
        }
        return nextActivation;
    }

    private static @Nullable TeamSlot find(List<TeamSlot> slots, String name) {
        for (var slot : slots) {
            if (slot.name().equals(name)) {
                return slot;
            }
        }
        return null;
    }
}
