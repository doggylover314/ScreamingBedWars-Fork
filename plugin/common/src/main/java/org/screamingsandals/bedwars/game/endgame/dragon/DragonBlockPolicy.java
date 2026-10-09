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

package org.screamingsandals.bedwars.game.endgame.dragon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * Decides which blocks a sudden death dragon may destroy.
 */
public final class DragonBlockPolicy {

    private DragonBlockPolicy() {
    }

    public enum Mode {
        /**
         * Any block (originals are recorded and restored on rebuild).
         */
        ALL,
        /**
         * Only blocks placed during the game.
         */
        PLACED,
        /**
         * Nothing.
         */
        NONE;

        /**
         * "all"; "placed" / "placed-only" / "player-placed"; "none" / "off" / "false"; case-insensitive, trimmed,
         * {@code '_'} equals {@code '-'}; anything else (or null) gives an empty result.
         */
        public static @NotNull Optional<Mode> fromConfig(@Nullable String raw) {
            if (raw == null) {
                return Optional.empty();
            }
            var normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
            return switch (normalized) {
                case "all" -> Optional.of(ALL);
                case "placed", "placed-only", "player-placed" -> Optional.of(PLACED);
                case "none", "off", "false" -> Optional.of(NONE);
                default -> Optional.empty();
            };
        }
    }

    public enum Decision {
        KEEP,
        /**
         * Break the block, nothing to record (it was placed during the game, so the region already tracks it).
         */
        DESTROY,
        /**
         * Break the block and record the original so that the rebuild restores it.
         */
        DESTROY_AND_RECORD
    }

    public static @NotNull Decision decide(@NotNull Mode mode, boolean insideArena, boolean immune, boolean protectedBlock,
                                           boolean placedDuringGame, boolean snapshotAvailable) {
        if (!insideArena || immune || protectedBlock) {
            return Decision.KEEP;
        }
        return switch (mode) {
            case NONE -> Decision.KEEP;
            case PLACED -> placedDuringGame ? Decision.DESTROY : Decision.KEEP;
            case ALL -> placedDuringGame
                    ? Decision.DESTROY
                    : (snapshotAvailable ? Decision.DESTROY_AND_RECORD : Decision.KEEP);
        };
    }
}
