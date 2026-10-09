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

import java.util.List;
import java.util.Set;

/**
 * Pure, platform-free view of an arena in edit mode; input of {@link SetupChecklist}.
 * Built from a {@code GameImpl} by {@code SetupOperations#snapshot}.
 */
public record ArenaSetupSnapshot(
        @NotNull String arenaName,
        @NotNull String variantName,
        boolean pos1Set,
        boolean pos2Set,
        boolean lobbySpawnSet,
        boolean lobbyPos1Set,
        boolean lobbyPos2Set,
        boolean specSpawnSet,
        @NotNull List<TeamInfo> teams,
        @NotNull List<SpawnerInfo> spawners,
        @NotNull List<StoreInfo> stores,
        boolean variantHasUpgrades,
        @NotNull Set<String> variantSpawnerTypes,   // lower-case config keys
        @NotNull List<String> teamGeneratorTypes,   // lower-case
        @NotNull String diamondType,                // lower-case
        @NotNull String emeraldType,                // lower-case
        @Nullable String upgradeShopFile,           // resolved, or null if the variant has none
        @NotNull List<String> allColors             // TeamColorImpl enum names in declaration order
) {
    public enum TargetKind {
        NONE_SET,
        BLOCK,
        BLOCK_COUNTDOWN,
        COUNTDOWN,
        NO_TARGET
    }

    public record TeamInfo(@NotNull String name, @NotNull String color, int maxPlayers, int spawnCount, @NotNull TargetKind target) {
    }

    public record SpawnerInfo(@NotNull String type, @Nullable String team) {
    }

    public record StoreInfo(@Nullable String shopFile, @Nullable String team) {
    }
}
