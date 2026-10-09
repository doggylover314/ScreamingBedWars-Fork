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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure checklist of what an arena in edit mode still needs (REQUIRED rows mirror the SEVERE checks of
 * {@code SaveCommand}, RECOMMENDED rows mirror its warnings plus Hypixel-style extras).
 */
public final class SetupChecklist {
    public enum Severity {
        REQUIRED,
        RECOMMENDED,
        OPTIONAL
    }

    public enum Item {
        BOUNDS,
        LOBBY,
        SPECTATOR,
        TEAMS,
        TEAM_SPAWN,
        TEAM_TARGET,
        TEAM_GENERATOR,
        TEAM_SHOP,
        TEAM_UPGRADES,
        SPAWNERS,
        SHOPS,
        STORE_COUNT,
        UPGRADE_SHOPS,
        DIAMOND,
        EMERALD,
        LOBBY_REGION
    }

    /**
     * One row of the checklist.
     *
     * @param team    set for the per-team rows, else null
     * @param count   meaningful count of the row (teams, spawners, stores ...), 0 where not meaningful
     * @param command command that fixes the row (or the best starting point)
     */
    public record Entry(@NotNull Item item, @NotNull Severity severity, boolean done, @Nullable String team, int count, @NotNull String command) {
    }

    public static @NotNull List<Entry> evaluate(@NotNull ArenaSetupSnapshot s) {
        var entries = new ArrayList<Entry>();

        // 1-3 bounds, lobby, spectator
        entries.add(new Entry(Item.BOUNDS, Severity.REQUIRED, s.pos1Set() && s.pos2Set(), null, 0,
                !s.pos1Set() ? "/bw set pos1" : "/bw set pos2"));
        entries.add(new Entry(Item.LOBBY, Severity.REQUIRED, s.lobbySpawnSet(), null, 0, "/bw set lobby"));
        entries.add(new Entry(Item.SPECTATOR, Severity.REQUIRED, s.specSpawnSet(), null, 0, "/bw set spectator"));

        // 4 teams
        var usedColors = new ArrayList<String>();
        for (var t : s.teams()) {
            usedColors.add(t.color());
        }
        var freeColor = TeamNaming.firstUnused(s.allColors(), usedColors);
        entries.add(new Entry(Item.TEAMS, Severity.REQUIRED, s.teams().size() >= 2, null, s.teams().size(),
                freeColor == null ? "/bw set team" : "/bw set team " + freeColor.toLowerCase(Locale.ROOT)));

        // 5 per team
        var upgradesSeverity = s.variantHasUpgrades() ? Severity.RECOMMENDED : Severity.OPTIONAL;
        boolean anyStoreLinked = s.stores().stream().anyMatch(st -> st.team() != null);
        for (var t : s.teams()) {
            entries.add(new Entry(Item.TEAM_SPAWN, Severity.REQUIRED, t.spawnCount() > 0, t.name(), t.spawnCount(),
                    "/bw set spawn " + t.name()));
            entries.add(new Entry(Item.TEAM_TARGET, Severity.REQUIRED, t.target() != ArenaSetupSnapshot.TargetKind.NONE_SET, t.name(), 0,
                    "/bw set bed " + t.name()));
            boolean hasGenerator = s.spawners().stream().anyMatch(sp -> sp.team() != null
                    && sp.team().equalsIgnoreCase(t.name())
                    && s.teamGeneratorTypes().contains(sp.type()));
            entries.add(new Entry(Item.TEAM_GENERATOR, upgradesSeverity, hasGenerator, t.name(), 0,
                    "/bw set generator " + t.name()));
            if (anyStoreLinked) {
                boolean hasShop = s.stores().stream().anyMatch(st -> st.team() != null
                        && st.team().equalsIgnoreCase(t.name())
                        && !isUpgradeShop(st.shopFile(), s.upgradeShopFile()));
                entries.add(new Entry(Item.TEAM_SHOP, Severity.RECOMMENDED, hasShop, t.name(), 0,
                        "/bw set shop " + t.name()));
                if (s.upgradeShopFile() != null) {
                    boolean hasUpgrades = s.stores().stream().anyMatch(st -> st.team() != null
                            && st.team().equalsIgnoreCase(t.name())
                            && isUpgradeShop(st.shopFile(), s.upgradeShopFile()));
                    entries.add(new Entry(Item.TEAM_UPGRADES, upgradesSeverity, hasUpgrades, t.name(), 0,
                            "/bw set upgrades " + t.name()));
                }
            }
        }

        // 6 spawners
        entries.add(new Entry(Item.SPAWNERS, Severity.RECOMMENDED, !s.spawners().isEmpty(), null, s.spawners().size(),
                s.teams().isEmpty() ? "/bw set generator" : "/bw set generator " + s.teams().get(0).name()));

        // 7 shops
        int upgradeStores = 0;
        int itemStores = 0;
        for (var st : s.stores()) {
            if (isUpgradeShop(st.shopFile(), s.upgradeShopFile())) {
                upgradeStores++;
            } else {
                itemStores++;
            }
        }
        entries.add(new Entry(Item.SHOPS, Severity.RECOMMENDED, itemStores > 0, null, itemStores, "/bw set shop"));

        // 8 store count
        if (!s.stores().isEmpty() && !s.teams().isEmpty()) {
            entries.add(new Entry(Item.STORE_COUNT, Severity.RECOMMENDED, s.stores().size() % s.teams().size() == 0, null,
                    s.stores().size(), "/bw admin " + s.arenaName() + " info stores"));
        }

        // 9 upgrade shops
        if (s.upgradeShopFile() != null) {
            entries.add(new Entry(Item.UPGRADE_SHOPS, upgradesSeverity, upgradeStores > 0, null, upgradeStores, "/bw set upgrades"));
        }

        // 10-11 diamond / emerald
        if (s.variantSpawnerTypes().contains(s.diamondType())) {
            int count = countSpawners(s, s.diamondType());
            entries.add(new Entry(Item.DIAMOND, Severity.RECOMMENDED, count > 0, null, count, "/bw set diamond"));
        }
        if (s.variantSpawnerTypes().contains(s.emeraldType())) {
            int count = countSpawners(s, s.emeraldType());
            entries.add(new Entry(Item.EMERALD, Severity.RECOMMENDED, count > 0, null, count, "/bw set emerald"));
        }

        // 12 lobby region (half a region is useless: the loader needs both)
        entries.add(new Entry(Item.LOBBY_REGION, (s.lobbyPos1Set() ^ s.lobbyPos2Set()) ? Severity.RECOMMENDED : Severity.OPTIONAL,
                s.lobbyPos1Set() && s.lobbyPos2Set(), null, 0,
                !s.lobbyPos1Set() ? "/bw set lobbypos1" : "/bw set lobbypos2"));

        return entries;
    }

    private static int countSpawners(@NotNull ArenaSetupSnapshot s, @NotNull String type) {
        int count = 0;
        for (var sp : s.spawners()) {
            if (sp.type().equals(type)) {
                count++;
            }
        }
        return count;
    }

    /**
     * True when every REQUIRED row is done.
     */
    public static boolean isSaveable(@NotNull List<Entry> entries) {
        return entries.stream().noneMatch(e -> e.severity() == Severity.REQUIRED && !e.done());
    }

    /**
     * The first not-done REQUIRED row, else the first not-done RECOMMENDED row.
     */
    public static @NotNull Optional<Entry> nextStep(@NotNull List<Entry> entries) {
        var required = entries.stream().filter(e -> e.severity() == Severity.REQUIRED && !e.done()).findFirst();
        if (required.isPresent()) {
            return required;
        }
        return entries.stream().filter(e -> e.severity() == Severity.RECOMMENDED && !e.done()).findFirst();
    }

    public static int requiredDone(@NotNull List<Entry> entries) {
        return (int) entries.stream().filter(e -> e.severity() == Severity.REQUIRED && e.done()).count();
    }

    public static int requiredTotal(@NotNull List<Entry> entries) {
        return (int) entries.stream().filter(e -> e.severity() == Severity.REQUIRED).count();
    }

    /**
     * Whether a store's shop file is the upgrade shop: equal to the variant's upgrade shop file when known (ignoring case,
     * path separators and the file extension), else any file name containing "upgrade".
     */
    public static boolean isUpgradeShop(@Nullable String shopFile, @Nullable String upgradeShopFile) {
        if (shopFile == null) {
            return false;
        }
        var file = normalize(shopFile);
        if (upgradeShopFile != null) {
            return file.equals(normalize(upgradeShopFile));
        }
        return file.contains("upgrade");
    }

    private static @NotNull String normalize(@NotNull String x) {
        var n = x.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (n.endsWith(".yml")) {
            n = n.substring(0, n.length() - 4);
        } else if (n.endsWith(".groovy")) {
            n = n.substring(0, n.length() - 7);
        }
        return n;
    }

    private SetupChecklist() {
    }
}
