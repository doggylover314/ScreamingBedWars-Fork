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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source world "w", arena (0,60,0)-(99,100,99), world height -64..320 (exclusive), maxBlocks 20,000,000 unless stated.
 */
class ClonePlannerTest {
    private static final BlockBox ARENA = new BlockBox(0, 60, 0, 99, 100, 99);

    private static ClonePlanner.CloneRequest request(String lobbyWorld, double lx, double ly, double lz, BlockBox region,
                                                     String targetWorld, int tx, int ty, int tz, long maxBlocks,
                                                     ClonePlanner.Obstacle... obstacles) {
        return new ClonePlanner.CloneRequest("w", ARENA, lobbyWorld, lx, ly, lz, region, targetWorld, tx, ty, tz,
                -64, 320, maxBlocks, List.of(obstacles));
    }

    private static ClonePlanner.CloneRequest simple(String targetWorld, int tx, int ty, int tz) {
        return request("w", 50.5, 95, 50.5, null, targetWorld, tx, ty, tz, 20_000_000L);
    }

    private static void assertSingleError(ClonePlanner.PlanResult result, ClonePlanner.ErrorCode code, String detail) {
        assertFalse(result.ok());
        assertNull(result.plan());
        assertEquals(1, result.errors().size(), result.errors().toString());
        assertEquals(code, result.errors().get(0).code());
        assertEquals(detail, result.errors().get(0).detail());
    }

    @Test
    void lobbyInsideTheArenaMovesWithIt() {
        var result = ClonePlanner.plan(simple("w", 1000, 60, 0));
        assertTrue(result.ok());
        var plan = result.plan();
        assertNotNull(plan);
        assertEquals(new BlockOffset(1000, 0, 0), plan.offset());
        assertEquals(ARENA, plan.sourceArenaBox());
        assertEquals(new BlockBox(1000, 60, 0, 1099, 100, 99), plan.targetArenaBox());
        assertEquals(ClonePlanner.LobbyPolicy.RELOCATE, plan.lobbyPolicy());
        assertNull(plan.sourceLobbyBox());
        assertNull(plan.targetLobbyBox());
        assertEquals(410_000L, plan.totalBlocks());
        assertTrue(plan.warnings().isEmpty());
    }

    @Test
    void lobbyOutsideTheArenaIsShared() {
        var plan = ClonePlanner.plan(request("w", 200, 95, 50, null, "w", 1000, 60, 0, 20_000_000L)).plan();
        assertNotNull(plan);
        assertEquals(ClonePlanner.LobbyPolicy.KEEP_SHARED, plan.lobbyPolicy());
        assertEquals(List.of(ClonePlanner.Warning.LOBBY_SHARED), plan.warnings());
        assertNull(plan.sourceLobbyBox());
        assertEquals(410_000L, plan.totalBlocks());
    }

    @Test
    void lobbyRegionOutsideTheArenaIsCopiedWhenTheSpawnIsInsideIt() {
        var region = new BlockBox(200, 90, 40, 220, 100, 60);
        var plan = ClonePlanner.plan(request("w", 210.5, 95, 50.5, region, "w", 1000, 60, 0, 20_000_000L)).plan();
        assertNotNull(plan);
        assertEquals(ClonePlanner.LobbyPolicy.RELOCATE, plan.lobbyPolicy());
        assertEquals(region, plan.sourceLobbyBox());
        assertEquals(new BlockBox(1200, 90, 40, 1220, 100, 60), plan.targetLobbyBox());
        assertEquals(414_851L, plan.totalBlocks());
        assertEquals(List.of(ClonePlanner.Warning.LOBBY_REGION_COPIED), plan.warnings());
    }

    @Test
    void lobbySpawnInsideTheArenaWithARegionStickingOut() {
        var region = new BlockBox(90, 90, 90, 110, 100, 110);
        var plan = ClonePlanner.plan(request("w", 95, 95, 95, region, "w", 1000, 60, 0, 20_000_000L)).plan();
        assertNotNull(plan);
        assertEquals(ClonePlanner.LobbyPolicy.RELOCATE, plan.lobbyPolicy());
        assertEquals(region, plan.sourceLobbyBox());
        assertEquals(414_851L, plan.totalBlocks());
    }

    @Test
    void lobbyRegionInsideTheArenaNeedsNoExtraBox() {
        var region = new BlockBox(40, 90, 40, 60, 99, 60);
        var plan = ClonePlanner.plan(request("w", 50.5, 95, 50.5, region, "w", 1000, 60, 0, 20_000_000L)).plan();
        assertNotNull(plan);
        assertEquals(ClonePlanner.LobbyPolicy.RELOCATE, plan.lobbyPolicy());
        assertNull(plan.sourceLobbyBox());
        assertEquals(410_000L, plan.totalBlocks());
        assertTrue(plan.warnings().isEmpty());
    }

    @Test
    void lobbyInAnotherWorldDropsTheRegion() {
        var region = new BlockBox(200, 90, 40, 220, 100, 60);
        var plan = ClonePlanner.plan(request("lobby", 210.5, 95, 50.5, region, "w", 1000, 60, 0, 20_000_000L)).plan();
        assertNotNull(plan);
        assertEquals(ClonePlanner.LobbyPolicy.KEEP_SHARED_DROP_REGION, plan.lobbyPolicy());
        assertEquals(List.of(ClonePlanner.Warning.LOBBY_SHARED, ClonePlanner.Warning.LOBBY_REGION_DROPPED), plan.warnings());
        assertNull(plan.sourceLobbyBox());
    }

    @Test
    void lobbyOutsideArenaAndRegionDropsTheRegion() {
        var region = new BlockBox(200, 90, 40, 220, 100, 60);
        var plan = ClonePlanner.plan(request("w", 500, 95, 500, region, "w", 1000, 60, 0, 20_000_000L)).plan();
        assertNotNull(plan);
        assertEquals(ClonePlanner.LobbyPolicy.KEEP_SHARED_DROP_REGION, plan.lobbyPolicy());
    }

    @Test
    void overlapWithAnotherArenaIsRefusedInTheSameWorldOnly() {
        var obstacle = new ClonePlanner.Obstacle("other", "w", new BlockBox(1050, 0, 50, 1060, 255, 60), ClonePlanner.ObstacleKind.ARENA);
        assertSingleError(ClonePlanner.plan(request("w", 50.5, 95, 50.5, null, "w", 1000, 60, 0, 20_000_000L, obstacle)),
                ClonePlanner.ErrorCode.OVERLAPS_ARENA, "other");

        var elsewhere = new ClonePlanner.Obstacle("other", "w2", new BlockBox(1050, 0, 50, 1060, 255, 60), ClonePlanner.ObstacleKind.ARENA);
        assertTrue(ClonePlanner.plan(request("w", 50.5, 95, 50.5, null, "w", 1000, 60, 0, 20_000_000L, elsewhere)).ok());
    }

    @Test
    void touchingAnotherArenaCountsAsOverlap() {
        var obstacle = new ClonePlanner.Obstacle("other", "w", new BlockBox(1099, 0, 0, 1200, 255, 99), ClonePlanner.ObstacleKind.ARENA);
        assertSingleError(ClonePlanner.plan(request("w", 50.5, 95, 50.5, null, "w", 1000, 60, 0, 20_000_000L, obstacle)),
                ClonePlanner.ErrorCode.OVERLAPS_ARENA, "other");
    }

    @Test
    void foreignLobbyRegionOverlappingTheTargetLobbyBoxIsRefused() {
        var region = new BlockBox(200, 90, 40, 220, 100, 60);
        // only the target lobby box (1200..1220) is hit, the target arena box (1000..1099) is not
        var obstacle = new ClonePlanner.Obstacle("x", "w", new BlockBox(1210, 90, 50, 1215, 95, 55), ClonePlanner.ObstacleKind.LOBBY_REGION);
        assertSingleError(ClonePlanner.plan(request("w", 210.5, 95, 50.5, region, "w", 1000, 60, 0, 20_000_000L, obstacle)),
                ClonePlanner.ErrorCode.OVERLAPS_ARENA, "x");
    }

    @Test
    void pendingCloneTargetsAreReportedSeparately() {
        var obstacle = new ClonePlanner.Obstacle("pending", "w", new BlockBox(1050, 0, 50, 1060, 255, 60), ClonePlanner.ObstacleKind.CLONE_TARGET);
        assertSingleError(ClonePlanner.plan(request("w", 50.5, 95, 50.5, null, "w", 1000, 60, 0, 20_000_000L, obstacle)),
                ClonePlanner.ErrorCode.OVERLAPS_CLONE, "pending");
    }

    @Test
    void oneErrorPerObstacle() {
        var a = new ClonePlanner.Obstacle("a", "w", new BlockBox(1050, 0, 50, 1060, 255, 60), ClonePlanner.ObstacleKind.ARENA);
        var b = new ClonePlanner.Obstacle("b", "w", new BlockBox(1010, 0, 10, 1020, 255, 20), ClonePlanner.ObstacleKind.ARENA);
        var result = ClonePlanner.plan(request("w", 50.5, 95, 50.5, null, "w", 1000, 60, 0, 20_000_000L, a, b));
        assertFalse(result.ok());
        assertEquals(2, result.errors().size());
        assertEquals("a", result.errors().get(0).detail());
        assertEquals("b", result.errors().get(1).detail());
    }

    @Test
    void overlapWithTheSourceItself() {
        assertSingleError(ClonePlanner.plan(simple("w", 50, 60, 0)), ClonePlanner.ErrorCode.OVERLAPS_SOURCE, null);
        assertSingleError(ClonePlanner.plan(simple("w", 99, 100, 99)), ClonePlanner.ErrorCode.OVERLAPS_SOURCE, null); // corner touches
    }

    @Test
    void samePlace() {
        assertSingleError(ClonePlanner.plan(simple("w", 0, 60, 0)), ClonePlanner.ErrorCode.SAME_PLACE, null);
    }

    @Test
    void samePlaceInAnotherWorldIsFine() {
        var result = ClonePlanner.plan(simple("w2", 0, 60, 0));
        assertTrue(result.ok());
        assertTrue(result.plan().offset().isZero());
    }

    @Test
    void worldHeight() {
        assertSingleError(ClonePlanner.plan(simple("w", 1000, -100, 0)), ClonePlanner.ErrorCode.OUT_OF_WORLD, "-100..-60");
        assertSingleError(ClonePlanner.plan(simple("w", 1000, 280, 0)), ClonePlanner.ErrorCode.OUT_OF_WORLD, "280..320");
        assertTrue(ClonePlanner.plan(simple("w", 1000, 279, 0)).ok()); // maxY 319
        assertTrue(ClonePlanner.plan(simple("w", 1000, -64, 0)).ok()); // minY exactly the world minimum
        assertSingleError(ClonePlanner.plan(simple("w", 1000, -65, 0)), ClonePlanner.ErrorCode.OUT_OF_WORLD, "-65..-25");
    }

    @Test
    void tooLarge() {
        assertSingleError(ClonePlanner.plan(request("w", 50.5, 95, 50.5, null, "w", 1000, 60, 0, 400_000L)),
                ClonePlanner.ErrorCode.TOO_LARGE, "410000");
        assertTrue(ClonePlanner.plan(request("w", 50.5, 95, 50.5, null, "w", 1000, 60, 0, 410_000L)).ok());
    }

    @Test
    void severalIndependentErrorsAreAllReported() {
        var obstacle = new ClonePlanner.Obstacle("other", "w", new BlockBox(0, 250, 0, 99, 320, 99), ClonePlanner.ObstacleKind.ARENA);
        var result = ClonePlanner.plan(request("w", 50.5, 95, 50.5, null, "w", 0, 280, 0, 1000L, obstacle));
        assertFalse(result.ok());
        var codes = result.errors().stream().map(ClonePlanner.PlanError::code).toList();
        assertEquals(List.of(ClonePlanner.ErrorCode.OUT_OF_WORLD, ClonePlanner.ErrorCode.TOO_LARGE,
                ClonePlanner.ErrorCode.OVERLAPS_ARENA), codes);
    }

    @Test
    void extremeTargetsDoNotOverflow() {
        var result = ClonePlanner.plan(simple("w", 1000, Integer.MAX_VALUE, 0));
        assertFalse(result.ok());
        assertEquals(ClonePlanner.ErrorCode.OUT_OF_WORLD, result.errors().get(0).code());
    }
}
