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

package org.screamingsandals.bedwars.game.upgrade.builtin;

/**
 * Arithmetic of the forge upgrade. Pure helper.
 * <p>
 * The forge changes the AMOUNT per spawn (not the interval): {@code ItemSpawnerImpl} already supports fractional
 * amounts (weighted random extra item), so +50 % means 1.5 items per cycle on average without interfering with the
 * timeline's interval changes.
 */
public final class ForgeMath {
    private ForgeMath() {
    }

    /**
     * Extra amount per spawn for a spawner with this base amount.
     */
    public static double delta(double baseAmountPerSpawn, double resourceBonus) {
        return baseAmountPerSpawn * Math.max(0, resourceBonus);
    }

    /**
     * New amountPerSpawn: removes what the forge added before and adds the new delta, keeping other (legacy or plugin) changes.
     */
    public static double rebase(double currentAmount, double previousDelta, double newDelta) {
        return currentAmount - previousDelta + newDelta;
    }

    /**
     * @param secondsActive seconds the emerald tier has been active (1, 2, 3 ...)
     */
    public static boolean shouldSpawnEmerald(long secondsActive, int intervalSeconds) {
        return intervalSeconds > 0 && secondsActive > 0 && secondsActive % intervalSeconds == 0;
    }
}
