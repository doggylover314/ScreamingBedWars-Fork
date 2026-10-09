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

package org.screamingsandals.bedwars.setup.clone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides what an arena clone copies and whether the target place is acceptable (pure).
 * <p>
 * The horizontal limits of the world (about +-30,000,000 blocks) are NOT checked here; the caller rejects such
 * coordinates before planning (the box arithmetic saturates instead of overflowing).
 */
public final class ClonePlanner {
    private ClonePlanner() {
    }

    /**
     * What happens to the waiting lobby of the source.
     * <ul>
     *     <li>RELOCATE: the lobby spawn (and region) move with the copied blocks</li>
     *     <li>KEEP_SHARED: the clone uses the same lobby spawn as the source</li>
     *     <li>KEEP_SHARED_DROP_REGION: same, and the clone gets no lobby region</li>
     * </ul>
     */
    public enum LobbyPolicy {
        RELOCATE,
        KEEP_SHARED,
        KEEP_SHARED_DROP_REGION
    }

    public enum Warning {
        LOBBY_REGION_COPIED,
        LOBBY_SHARED,
        LOBBY_REGION_DROPPED
    }

    public enum ErrorCode {
        SAME_PLACE,
        OUT_OF_WORLD,
        TOO_LARGE,
        OVERLAPS_SOURCE,
        OVERLAPS_ARENA,
        OVERLAPS_CLONE
    }

    public enum ObstacleKind {
        ARENA,
        LOBBY_REGION,
        CLONE_TARGET
    }

    public record Obstacle(@NotNull String name, @NotNull String world, @NotNull BlockBox box, @NotNull ObstacleKind kind) {
    }

    /**
     * @param arenaBox    arena box already clamped to the source world height
     * @param lobbyRegion lobbyPos1/2 (world = lobbyWorld), null unless both are set
     * @param obstacles   excludes the source ARENA box (checked separately)
     */
    public record CloneRequest(
            @NotNull String sourceWorld,
            @NotNull BlockBox arenaBox,
            @NotNull String lobbyWorld,
            double lobbyX,
            double lobbyY,
            double lobbyZ,
            @Nullable BlockBox lobbyRegion,
            @NotNull String targetWorld,
            int targetMinX,
            int targetMinY,
            int targetMinZ,
            int targetWorldMinY,
            int targetWorldMaxYExclusive,
            long maxBlocks,
            @NotNull List<Obstacle> obstacles
    ) {
    }

    /**
     * @param sourceLobbyBox extra box to copy (null = none)
     * @param targetLobbyBox the extra box moved by the offset (null = none)
     */
    public record ClonePlan(
            @NotNull BlockOffset offset,
            @NotNull BlockBox sourceArenaBox,
            @NotNull BlockBox targetArenaBox,
            @Nullable BlockBox sourceLobbyBox,
            @Nullable BlockBox targetLobbyBox,
            @NotNull LobbyPolicy lobbyPolicy,
            long totalBlocks,
            @NotNull List<Warning> warnings
    ) {
    }

    public record PlanError(@NotNull ErrorCode code, @Nullable String detail) {
    }

    public record PlanResult(@Nullable ClonePlan plan, @NotNull List<PlanError> errors) {
        public boolean ok() {
            return plan != null;
        }
    }

    public static @NotNull PlanResult plan(@NotNull CloneRequest r) {
        var offset = BlockOffset.toMinCorner(r.arenaBox(), r.targetMinX(), r.targetMinY(), r.targetMinZ());
        if (offset.isZero() && r.targetWorld().equals(r.sourceWorld())) {
            return new PlanResult(null, List.of(new PlanError(ErrorCode.SAME_PLACE, null)));
        }

        // lobby decision
        boolean sameWorldLobby = r.lobbyWorld().equals(r.sourceWorld());
        boolean spawnInArena = sameWorldLobby && r.arenaBox().containsPoint(r.lobbyX(), r.lobbyY(), r.lobbyZ());
        boolean spawnInRegion = r.lobbyRegion() != null && r.lobbyRegion().containsPoint(r.lobbyX(), r.lobbyY(), r.lobbyZ());
        var warnings = new ArrayList<Warning>();
        LobbyPolicy policy;
        BlockBox sourceLobbyBox = null;
        if (sameWorldLobby && (spawnInArena || spawnInRegion)) {
            policy = LobbyPolicy.RELOCATE;
            if (r.lobbyRegion() != null && !r.arenaBox().contains(r.lobbyRegion())) {
                sourceLobbyBox = r.lobbyRegion();
                warnings.add(Warning.LOBBY_REGION_COPIED);
            }
        } else if (r.lobbyRegion() != null) {
            policy = LobbyPolicy.KEEP_SHARED_DROP_REGION;
            warnings.add(Warning.LOBBY_SHARED);
            warnings.add(Warning.LOBBY_REGION_DROPPED);
        } else {
            policy = LobbyPolicy.KEEP_SHARED;
            warnings.add(Warning.LOBBY_SHARED);
        }

        var targetArena = r.arenaBox().shift(offset);
        var targetLobby = sourceLobbyBox == null ? null : sourceLobbyBox.shift(offset);
        long total = r.arenaBox().volume() + (sourceLobbyBox == null ? 0 : sourceLobbyBox.volume());

        var sources = new ArrayList<BlockBox>(2);
        sources.add(r.arenaBox());
        var targets = new ArrayList<BlockBox>(2);
        targets.add(targetArena);
        if (sourceLobbyBox != null) {
            sources.add(sourceLobbyBox);
            targets.add(targetLobby);
        }

        var errors = new ArrayList<PlanError>();

        for (var t : targets) {
            if (t.minY() < r.targetWorldMinY() || t.maxY() >= r.targetWorldMaxYExclusive()) {
                errors.add(new PlanError(ErrorCode.OUT_OF_WORLD, t.minY() + ".." + t.maxY()));
                break; // once
            }
        }

        if (total > r.maxBlocks()) {
            errors.add(new PlanError(ErrorCode.TOO_LARGE, String.valueOf(total)));
        }

        if (r.targetWorld().equals(r.sourceWorld())) {
            overlap:
            for (var t : targets) {
                for (var s : sources) {
                    if (t.intersects(s)) {
                        errors.add(new PlanError(ErrorCode.OVERLAPS_SOURCE, null));
                        break overlap; // once
                    }
                }
            }
        }

        for (var o : r.obstacles()) {
            if (!o.world().equals(r.targetWorld())) {
                continue;
            }
            for (var t : targets) {
                if (t.intersects(o.box())) {
                    errors.add(new PlanError(
                            o.kind() == ObstacleKind.CLONE_TARGET ? ErrorCode.OVERLAPS_CLONE : ErrorCode.OVERLAPS_ARENA,
                            o.name()
                    ));
                    break; // one error per obstacle
                }
            }
        }

        if (!errors.isEmpty()) {
            return new PlanResult(null, List.copyOf(errors));
        }
        return new PlanResult(
                new ClonePlan(offset, r.arenaBox(), targetArena, sourceLobbyBox, targetLobby, policy, total, List.copyOf(warnings)),
                List.of()
        );
    }
}
