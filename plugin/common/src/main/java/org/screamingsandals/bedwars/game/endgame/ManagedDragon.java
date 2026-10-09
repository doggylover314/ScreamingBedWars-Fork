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

package org.screamingsandals.bedwars.game.endgame;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.endgame.dragon.DragonSteering;
import org.screamingsandals.lib.entity.Entity;
import org.screamingsandals.lib.tasker.task.Task;

import java.util.UUID;

/**
 * One sudden death dragon flown by the plugin.
 */
final class ManagedDragon {
    final @NotNull SuddenDeathSession session;
    final @NotNull Entity entity;
    final @NotNull UUID uuid;
    /**
     * The team this dragon fights for; null = neutral (hunts everybody).
     */
    final @Nullable TeamImpl owner;
    final @NotNull DragonSteering.State steering;
    final int respawnCount;
    /**
     * Player currently hunted.
     */
    @Nullable UUID target;
    int ticks;
    boolean neutralNamed;
    /**
     * Killed by players (DYING phase intercepted): never respawned.
     */
    volatile boolean slain;
    /**
     * The entity vanished (isDead) without being slain.
     */
    volatile boolean lost;
    /**
     * Per-dragon steering task.
     */
    @Nullable Task task;

    ManagedDragon(@NotNull SuddenDeathSession session, @NotNull Entity entity, @Nullable TeamImpl owner,
                  @NotNull DragonSteering.State steering, int respawnCount) {
        this.session = session;
        this.entity = entity;
        this.uuid = entity.getUniqueId();
        this.owner = owner;
        this.steering = steering;
        this.respawnCount = respawnCount;
    }

    boolean isNeutral() {
        return owner == null || !owner.isAlive();
    }
}
