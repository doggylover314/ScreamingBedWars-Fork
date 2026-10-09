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

package org.screamingsandals.bedwars.setup;

import org.junit.jupiter.api.Test;
import org.screamingsandals.bedwars.setup.ArenaSetupSnapshot.SpawnerInfo;
import org.screamingsandals.bedwars.setup.ArenaSetupSnapshot.StoreInfo;
import org.screamingsandals.bedwars.setup.ArenaSetupSnapshot.TargetKind;
import org.screamingsandals.bedwars.setup.ArenaSetupSnapshot.TeamInfo;
import org.screamingsandals.bedwars.setup.SetupChecklist.Entry;
import org.screamingsandals.bedwars.setup.SetupChecklist.Item;
import org.screamingsandals.bedwars.setup.SetupChecklist.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupChecklistTest {
    static final List<String> ALL_COLORS = List.of("BLACK", "BLUE", "GREEN", "RED", "MAGENTA", "ORANGE", "LIGHT_GRAY", "GRAY",
            "LIGHT_BLUE", "LIME", "CYAN", "PINK", "YELLOW", "WHITE", "BROWN");
    static final String UPGRADE_SHOP = "certain-popular-server/upgrade-shop.yml";

    /** Mutable builder so each test only states what differs from an empty default-variant arena. */
    static final class B {
        boolean pos1;
        boolean pos2;
        boolean lobby;
        boolean lobbyPos1;
        boolean lobbyPos2;
        boolean spec;
        List<TeamInfo> teams = new ArrayList<>();
        List<SpawnerInfo> spawners = new ArrayList<>();
        List<StoreInfo> stores = new ArrayList<>();
        boolean upgrades;
        Set<String> variantTypes = Set.of("bronze", "iron", "gold");
        String upgradeShopFile;

        ArenaSetupSnapshot build() {
            return new ArenaSetupSnapshot("Test", "variant", pos1, pos2, lobby, lobbyPos1, lobbyPos2, spec, teams, spawners, stores,
                    upgrades, variantTypes, "diamond", "emerald", upgradeShopFile, ALL_COLORS);
        }

        B team(String name, String color, int spawns, TargetKind target) {
            teams.add(new TeamInfo(name, color, 4, spawns, target));
            return this;
        }

        B minimalComplete() {
            pos1 = true;
            pos2 = true;
            lobby = true;
            spec = true;
            team("Red", "RED", 1, TargetKind.BLOCK);
            team("Blue", "BLUE", 1, TargetKind.BLOCK);
            return this;
        }
    }

    private static List<Item> items(List<Entry> entries) {
        return entries.stream().map(Entry::item).toList();
    }

    private static Entry find(List<Entry> entries, Item item) {
        return entries.stream().filter(e -> e.item() == item).findFirst().orElseThrow(() -> new AssertionError("missing " + item));
    }

    private static Entry find(List<Entry> entries, Item item, String team) {
        return entries.stream().filter(e -> e.item() == item && team.equals(e.team())).findFirst()
                .orElseThrow(() -> new AssertionError("missing " + item + " " + team));
    }

    @Test
    void emptyArenaOfDefaultVariant() {
        var entries = SetupChecklist.evaluate(new B().build());

        assertEquals(List.of(Item.BOUNDS, Item.LOBBY, Item.SPECTATOR, Item.TEAMS, Item.SPAWNERS, Item.SHOPS, Item.LOBBY_REGION),
                items(entries));
        assertEquals("/bw set pos1", find(entries, Item.BOUNDS).command());
        assertEquals("/bw set team red", find(entries, Item.TEAMS).command());
        assertEquals(0, find(entries, Item.TEAMS).count());
        assertEquals("/bw set generator", find(entries, Item.SPAWNERS).command());
        assertEquals(Severity.OPTIONAL, find(entries, Item.LOBBY_REGION).severity());
        assertEquals("/bw set lobbypos1", find(entries, Item.LOBBY_REGION).command());
        assertFalse(SetupChecklist.isSaveable(entries));
        assertEquals(Item.BOUNDS, SetupChecklist.nextStep(entries).orElseThrow().item());
        assertEquals(0, SetupChecklist.requiredDone(entries));
        assertEquals(4, SetupChecklist.requiredTotal(entries));
    }

    @Test
    void onlyPos1SetAsksForPos2() {
        var b = new B();
        b.pos1 = true;
        var entries = SetupChecklist.evaluate(b.build());
        var bounds = find(entries, Item.BOUNDS);
        assertFalse(bounds.done());
        assertEquals("/bw set pos2", bounds.command());
    }

    @Test
    void minimalCompleteArenaIsSaveable() {
        var entries = SetupChecklist.evaluate(new B().minimalComplete().build());

        assertTrue(SetupChecklist.isSaveable(entries));
        assertEquals(8, SetupChecklist.requiredTotal(entries));
        assertEquals(8, SetupChecklist.requiredDone(entries));
        assertEquals(Severity.OPTIONAL, find(entries, Item.TEAM_GENERATOR, "Red").severity());
        assertEquals(Severity.OPTIONAL, find(entries, Item.TEAM_GENERATOR, "Blue").severity());
        var next = SetupChecklist.nextStep(entries).orElseThrow();
        assertEquals(Item.SPAWNERS, next.item());
        assertEquals("/bw set generator Red", next.command());
    }

    @Test
    void everythingDoneHasNoNextStep() {
        var b = new B().minimalComplete();
        b.spawners.add(new SpawnerInfo("iron", "Red"));
        b.stores.add(new StoreInfo(null, null));
        b.stores.add(new StoreInfo(null, null));
        var entries = SetupChecklist.evaluate(b.build());
        assertEquals(Optional.empty(), SetupChecklist.nextStep(entries));
    }

    @Test
    void certainPopularServerLikeVariant() {
        var b = new B();
        b.minimalComplete();
        b.upgrades = true;
        b.variantTypes = Set.of("iron", "gold", "diamond", "emerald");
        b.upgradeShopFile = UPGRADE_SHOP;
        b.spawners.add(new SpawnerInfo("iron", "Red"));
        b.spawners.add(new SpawnerInfo("gold", "Red"));
        b.stores.add(new StoreInfo(null, "Red"));
        b.stores.add(new StoreInfo(UPGRADE_SHOP, "Red"));
        var entries = SetupChecklist.evaluate(b.build());

        assertTrue(find(entries, Item.TEAM_GENERATOR, "Red").done());
        assertTrue(find(entries, Item.TEAM_SHOP, "Red").done());
        assertTrue(find(entries, Item.TEAM_UPGRADES, "Red").done());

        var gen = find(entries, Item.TEAM_GENERATOR, "Blue");
        assertEquals(Severity.RECOMMENDED, gen.severity());
        assertFalse(gen.done());
        assertEquals("/bw set generator Blue", gen.command());
        var shop = find(entries, Item.TEAM_SHOP, "Blue");
        assertFalse(shop.done());
        assertEquals("/bw set shop Blue", shop.command());
        var up = find(entries, Item.TEAM_UPGRADES, "Blue");
        assertFalse(up.done());
        assertEquals(Severity.RECOMMENDED, up.severity());
        assertEquals("/bw set upgrades Blue", up.command());

        assertEquals(1, find(entries, Item.SHOPS).count());
        assertEquals(1, find(entries, Item.UPGRADE_SHOPS).count());
        assertTrue(find(entries, Item.UPGRADE_SHOPS).done());
        assertTrue(find(entries, Item.STORE_COUNT).done());
        assertEquals("/bw admin Test info stores", find(entries, Item.STORE_COUNT).command());
        assertFalse(find(entries, Item.DIAMOND).done());
        assertFalse(find(entries, Item.EMERALD).done());
        assertEquals("/bw set diamond", find(entries, Item.DIAMOND).command());

        var next = SetupChecklist.nextStep(entries).orElseThrow();
        assertEquals(Item.TEAM_GENERATOR, next.item());
        assertEquals("Blue", next.team());
    }

    @Test
    void baseGeneratorOfAnyTypeCountsAsSet() {
        var b = new B().minimalComplete();
        b.spawners.add(new SpawnerInfo("bronze", "Red"));
        b.spawners.add(new SpawnerInfo("iron", "Blue"));
        b.spawners.add(new SpawnerInfo("gold", "Blue"));
        var entries = SetupChecklist.evaluate(b.build());

        assertTrue(find(entries, Item.TEAM_GENERATOR, "Red").done());
        assertTrue(find(entries, Item.TEAM_GENERATOR, "Blue").done());
    }

    @Test
    void spawnerWithoutTeamIsNoBaseGenerator() {
        var b = new B().minimalComplete();
        b.spawners.add(new SpawnerInfo("iron", null));
        b.spawners.add(new SpawnerInfo("iron", "Red"));
        var entries = SetupChecklist.evaluate(b.build());

        assertTrue(find(entries, Item.TEAM_GENERATOR, "Red").done());
        assertFalse(find(entries, Item.TEAM_GENERATOR, "Blue").done());
    }

    @Test
    void unlinkedStoresAreCountedButHaveNoTeamRows() {
        var b = new B().minimalComplete();
        for (int i = 0; i < 3; i++) {
            b.stores.add(new StoreInfo(null, null));
        }
        var entries = SetupChecklist.evaluate(b.build());

        var count = find(entries, Item.STORE_COUNT);
        assertFalse(count.done());
        assertEquals(3, count.count());
        assertFalse(items(entries).contains(Item.TEAM_SHOP));
        assertFalse(items(entries).contains(Item.TEAM_UPGRADES));
    }

    @Test
    void targetKindsOtherThanNoneSetCountAsDone() {
        for (var kind : List.of(TargetKind.NO_TARGET, TargetKind.COUNTDOWN, TargetKind.BLOCK_COUNTDOWN, TargetKind.BLOCK)) {
            var b = new B();
            b.team("Red", "RED", 1, kind);
            assertTrue(find(SetupChecklist.evaluate(b.build()), Item.TEAM_TARGET, "Red").done(), kind.name());
        }
        var b = new B();
        b.team("Red", "RED", 1, TargetKind.NONE_SET);
        var target = find(SetupChecklist.evaluate(b.build()), Item.TEAM_TARGET, "Red");
        assertFalse(target.done());
        assertEquals("/bw set bed Red", target.command());
    }

    @Test
    void halfALobbyRegionIsRecommended() {
        var b = new B();
        b.lobbyPos1 = true;
        var region = find(SetupChecklist.evaluate(b.build()), Item.LOBBY_REGION);
        assertEquals(Severity.RECOMMENDED, region.severity());
        assertFalse(region.done());
        assertEquals("/bw set lobbypos2", region.command());

        b.lobbyPos2 = true;
        var both = find(SetupChecklist.evaluate(b.build()), Item.LOBBY_REGION);
        assertTrue(both.done());
        assertEquals(Severity.OPTIONAL, both.severity());
    }

    @Test
    void teamsRowSuggestsTheNextFreeColour() {
        var b = new B();
        b.team("Red", "RED", 0, TargetKind.NONE_SET);
        b.team("Blue", "BLUE", 0, TargetKind.NONE_SET);
        assertEquals("/bw set team green", find(SetupChecklist.evaluate(b.build()), Item.TEAMS).command());

        var all = new B();
        for (var c : ALL_COLORS) {
            all.team(c, c, 0, TargetKind.NONE_SET);
        }
        assertEquals("/bw set team", find(SetupChecklist.evaluate(all.build()), Item.TEAMS).command());
    }

    @Test
    void upgradeShopDetection() {
        var up = "certain-popular-server/upgrade-shop.yml";
        assertTrue(SetupChecklist.isUpgradeShop("certain-popular-server/upgrade-shop", up));
        assertTrue(SetupChecklist.isUpgradeShop("CERTAIN-POPULAR-SERVER\\upgrade-shop.yml", up));
        assertFalse(SetupChecklist.isUpgradeShop("shop.yml", up));
        assertFalse(SetupChecklist.isUpgradeShop(null, up));
        assertTrue(SetupChecklist.isUpgradeShop("my-upgrades.yml", null));
        assertFalse(SetupChecklist.isUpgradeShop("shop.yml", null));
    }
}
