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
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Pure arena selection for a mode join.
 */
public final class ModeArenaSelector {
    private ModeArenaSelector() {
    }

    public static @NotNull SelectionResult select(@NotNull List<ArenaSnapshot> arenas, @NotNull ModeDefinition mode,
                                                  int seats, @NotNull RandomGenerator random) {
        int needed = Math.max(1, seats);
        if (!ModeRules.partyFits(mode, needed)) {
            return new SelectionResult(SelectionResult.Outcome.PARTY_TOO_LARGE, null);
        }
        // 1) lobbies already running this mode with room for everybody - fullest first
        var running = arenas.stream().filter(a -> isRunningLobbyWithRoom(a, mode, needed)).collect(Collectors.toList());
        if (!running.isEmpty()) {
            int max = running.stream().mapToInt(ArenaSnapshot::players).max().orElse(0);
            return new SelectionResult(SelectionResult.Outcome.JOIN_EXISTING,
                    pick(running.stream().filter(a -> a.players() == max).collect(Collectors.toList()), random).id());
        }
        // 2) idle arenas - preferred sizes first
        var idle = arenas.stream().filter(a -> isIdleCandidate(a, mode)).collect(Collectors.toList());
        if (!idle.isEmpty() && mode.maxPlayers() >= needed) {
            var preferred = idle.stream().filter(a -> a.preferredTeamSizes().contains(mode.teamSize())).collect(Collectors.toList());
            return new SelectionResult(SelectionResult.Outcome.CLAIM_IDLE, pick(preferred.isEmpty() ? idle : preferred, random).id());
        }
        return new SelectionResult(SelectionResult.Outcome.NONE_AVAILABLE, null);
    }

    static boolean isRunningLobbyWithRoom(ArenaSnapshot a, ModeDefinition mode, int needed) {
        if (a.state() != ArenaState.WAITING || !mode.id().equals(a.activeModeId()) || !mode.allowsArena(a.name())
                || mode.maxPlayers() - a.players() < needed) {
            return false;
        }
        if (needed <= 1) {
            return true; // one more player always fits while a seat is free
        }
        var sizes = new ArrayList<>(a.groupSizes());
        sizes.add(needed);
        return ModeTeamDistributor.fits(sizes, mode.teamSize(), a.teamCount());
    }

    static boolean isIdleCandidate(ArenaSnapshot a, ModeDefinition mode) {
        return a.state() == ArenaState.WAITING && a.players() == 0 && a.activeModeId() == null
                && a.teamCount() == mode.teamCount() && a.allowedTeamSizes().contains(mode.teamSize())
                && mode.allowsArena(a.name());
    }

    /**
     * For holograms / PAPI: arenas a single player could enter right now.
     */
    public static int countJoinable(@NotNull List<ArenaSnapshot> arenas, @NotNull ModeDefinition mode) {
        return (int) arenas.stream().filter(a -> isRunningLobbyWithRoom(a, mode, 1) || isIdleCandidate(a, mode)).count();
    }

    private static ArenaSnapshot pick(List<ArenaSnapshot> list, RandomGenerator random) {
        return list.size() == 1 ? list.get(0) : list.get(random.nextInt(list.size()));
    }
}
