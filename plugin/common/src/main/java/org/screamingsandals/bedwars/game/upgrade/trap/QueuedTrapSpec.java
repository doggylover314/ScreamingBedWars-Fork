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

package org.screamingsandals.bedwars.game.upgrade.trap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.upgrade.EffectSpec;

import java.util.List;

/**
 * One trap of the {@code trap-queue.traps} map. Pure value class.
 *
 * @param id        lower-cased trap id
 * @param sourceKey the YAML key as written (used to find {@code trigger-sound} in the original node)
 * @param name      rich text or {@code "@key"} link
 * @param icon      item type of the queue display item, optional
 */
public record QueuedTrapSpec(@NotNull String id, @NotNull String sourceKey, @NotNull String name, @Nullable String icon,
                             double detectionRange, boolean affectAllIntruders,
                             @NotNull List<EffectSpec> enemyEffects, @NotNull List<EffectSpec> teamEffects,
                             double teamEffectRange, boolean revealInvisible, boolean messageIntruder) {
}
