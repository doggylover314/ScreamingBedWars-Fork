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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The "fewest teams, evened, parties never split" algorithm for mode games (pure).
 */
public final class ModeTeamDistributor {
    /**
     * Above this number of multi-player groups the exhaustive search is replaced by a greedy placement.
     */
    public static final int MAX_EXHAUSTIVE_PARTIES = 10;

    private ModeTeamDistributor() {
    }

    public record Distribution(@NotNull List<List<PlayerGroup>> teams, @NotNull List<PlayerGroup> unassigned, boolean partiesSplit) {
        public Distribution {
            teams = teams.stream().map(List::copyOf).collect(Collectors.toUnmodifiableList());
            unassigned = List.copyOf(unassigned);
        }

        /**
         * At least two non-empty teams and everybody placed.
         */
        public boolean isStartable() {
            return teams.size() >= 2 && unassigned.isEmpty();
        }

        public @NotNull List<Integer> teamSizes() {
            return teams.stream().map(t -> t.stream().mapToInt(PlayerGroup::size).sum()).collect(Collectors.toList());
        }
    }

    /**
     * @param groups    groups in join order (a group = party members present in the game, or one solo player)
     * @param teamSize  capacity of one team (mode team size)
     * @param teamCount number of teams the arena has
     * @return teams sorted by size descending (stable); strict result if possible, otherwise best effort
     */
    public static @NotNull Distribution distribute(@NotNull List<PlayerGroup> groups, int teamSize, int teamCount) {
        if (teamSize < 1 || teamCount < 1) {
            throw new IllegalArgumentException("teamSize and teamCount must be >= 1");
        }
        var normalized = new ArrayList<PlayerGroup>();
        boolean split = false;
        for (var group : groups) {
            if (group.size() <= teamSize) {
                normalized.add(group);
            } else { // only reachable in classic games / direct joins
                normalized.addAll(group.splitInto(teamSize));
                split = true;
            }
        }
        int total = normalized.stream().mapToInt(PlayerGroup::size).sum();
        if (total == 0) {
            return new Distribution(List.of(), List.of(), split);
        }
        int minTeams = Math.max(2, ModeRules.ceilDiv(total, teamSize));
        for (int k = minTeams; k <= teamCount; k++) { // "if parties don't fit, use more teams"
            var bins = bestPartition(normalized, k, teamSize, total);
            if (bins != null) {
                return new Distribution(sortBySizeDesc(bins), List.of(), split);
            }
        }
        return bestEffort(normalized, teamSize, teamCount, split);
    }

    /**
     * True when groups of these sizes can all be placed, unsplit, into teamCount teams of teamSize (D49).
     */
    public static boolean fits(@NotNull List<Integer> groupSizes, int teamSize, int teamCount) {
        var groups = new ArrayList<PlayerGroup>();
        int index = 0;
        for (Integer size : groupSizes) {
            if (size == null || size < 1) {
                continue;
            }
            if (size > teamSize) {
                return false;
            }
            var members = new ArrayList<UUID>(size);
            for (int m = 0; m < size; m++) {
                members.add(new UUID(index, m)); // synthetic, unique per (group, member)
            }
            groups.add(new PlayerGroup("g" + index, members, null, false));
            index++;
        }
        return distribute(groups, teamSize, teamCount).unassigned().isEmpty();
    }

    private static List<List<PlayerGroup>> bestPartition(List<PlayerGroup> groups, int k, int cap, int total) {
        var parties = groups.stream()
                .filter(g -> g.size() >= 2)
                .sorted(Comparator.comparingInt(PlayerGroup::size).reversed())
                .collect(Collectors.toList());
        var singles = groups.stream().filter(g -> g.size() == 1).collect(Collectors.toList());
        int lowerBound = (total % k == 0) ? 0 : 1; // best spread that can exist
        var search = new Search(parties, singles, k, cap, lowerBound);
        if (parties.size() > MAX_EXHAUSTIVE_PARTIES) {
            search.greedy();
        } else {
            search.recurse(0, 0);
        }
        return search.best;
    }

    private static final class Search {
        private final List<PlayerGroup> parties;
        private final List<PlayerGroup> singles;
        private final int k;
        private final int cap;
        private final int lowerBound;
        private final int[] loads;
        private final int[] assignment;
        private List<List<PlayerGroup>> best = null;
        private int bestSpread = Integer.MAX_VALUE;

        private Search(List<PlayerGroup> parties, List<PlayerGroup> singles, int k, int cap, int lowerBound) {
            this.parties = parties;
            this.singles = singles;
            this.k = k;
            this.cap = cap;
            this.lowerBound = lowerBound;
            this.loads = new int[k];
            this.assignment = new int[parties.size()];
        }

        private void recurse(int index, int usedBins) {
            if (bestSpread == lowerBound) {
                return; // cannot be improved
            }
            if (index == parties.size()) {
                evaluate();
                return;
            }
            int size = parties.get(index).size();
            for (int b = 0; b <= Math.min(usedBins, k - 1); b++) { // canonical: a party may open at most one new bin
                if (loads[b] + size > cap) {
                    continue;
                }
                loads[b] += size;
                assignment[index] = b;
                recurse(index + 1, Math.max(usedBins, b + 1));
                loads[b] -= size;
            }
        }

        private void greedy() { // many parties: first-fit decreasing, least loaded bin
            for (int i = 0; i < parties.size(); i++) {
                int size = parties.get(i).size();
                int target = -1;
                for (int b = 0; b < k; b++) {
                    if (loads[b] + size <= cap && (target < 0 || loads[b] < loads[target])) {
                        target = b;
                    }
                }
                if (target < 0) {
                    return;
                }
                loads[target] += size;
                assignment[i] = target;
            }
            evaluate();
        }

        private void evaluate() { // water-fill singles into the least loaded bin
            int[] filled = loads.clone();
            int[] singleBin = new int[singles.size()];
            for (int s = 0; s < singles.size(); s++) {
                int target = -1;
                for (int b = 0; b < k; b++) {
                    if (filled[b] < cap && (target < 0 || filled[b] < filled[target])) {
                        target = b;
                    }
                }
                if (target < 0) {
                    return;
                }
                filled[target]++;
                singleBin[s] = target;
            }
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int f : filled) {
                min = Math.min(min, f);
                max = Math.max(max, f);
            }
            if (min == 0) {
                return; // every one of the k teams must get a player
            }
            if (max - min < bestSpread) {
                bestSpread = max - min;
                best = materialize(singleBin);
            }
        }

        private List<List<PlayerGroup>> materialize(int[] singleBin) {
            var bins = new ArrayList<List<PlayerGroup>>(k);
            for (int b = 0; b < k; b++) {
                bins.add(new ArrayList<>());
            }
            for (int i = 0; i < parties.size(); i++) {
                bins.get(assignment[i]).add(parties.get(i));
            }
            for (int s = 0; s < singles.size(); s++) {
                bins.get(singleBin[s]).add(singles.get(s));
            }
            return bins;
        }
    }

    /**
     * No valid strict split (a single group only, or more players than capacity): place what fits.
     */
    private static Distribution bestEffort(List<PlayerGroup> groups, int cap, int teamCount, boolean split) {
        var sorted = groups.stream().sorted(Comparator.comparingInt(PlayerGroup::size).reversed()).collect(Collectors.toList());
        var bins = new ArrayList<List<PlayerGroup>>();
        var loads = new ArrayList<Integer>();
        var unassigned = new ArrayList<PlayerGroup>();
        for (var group : sorted) {
            int target = -1;
            for (int b = 0; b < bins.size(); b++) {
                if (loads.get(b) + group.size() <= cap && (target < 0 || loads.get(b) < loads.get(target))) {
                    target = b;
                }
            }
            if (target < 0 && bins.size() < teamCount) {
                bins.add(new ArrayList<>());
                loads.add(0);
                target = bins.size() - 1;
            }
            if (target < 0) {
                unassigned.add(group);
                continue;
            }
            bins.get(target).add(group);
            loads.set(target, loads.get(target) + group.size());
        }
        return new Distribution(sortBySizeDesc(bins), unassigned, split);
    }

    private static List<List<PlayerGroup>> sortBySizeDesc(List<List<PlayerGroup>> bins) {
        var copy = new ArrayList<>(bins);
        copy.sort(Comparator.comparingInt((List<PlayerGroup> team) -> team.stream().mapToInt(PlayerGroup::size).sum()).reversed());
        return copy;
    }
}
