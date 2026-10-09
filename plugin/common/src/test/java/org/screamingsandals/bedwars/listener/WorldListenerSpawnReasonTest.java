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

package org.screamingsandals.bedwars.listener;

import org.junit.jupiter.api.Test;
import org.screamingsandals.lib.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Proxy;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldListenerSpawnReasonTest {
    private static CreatureSpawnEvent eventWithReason(Supplier<CreatureSpawnEvent.SpawnReason> reason) {
        return (CreatureSpawnEvent) Proxy.newProxyInstance(CreatureSpawnEvent.class.getClassLoader(), new Class<?>[] {CreatureSpawnEvent.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("spawnReason")) {
                        return reason.get();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void customSpawnIsDetected() {
        assertTrue(WorldListener.isCustomSpawn(eventWithReason(() -> CreatureSpawnEvent.SpawnReason.CUSTOM)));
    }

    @Test
    void otherKnownReasonsAreNotCustom() {
        assertFalse(WorldListener.isCustomSpawn(eventWithReason(() -> CreatureSpawnEvent.SpawnReason.NATURAL)));
        assertFalse(WorldListener.isCustomSpawn(eventWithReason(() -> CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)));
    }

    @Test
    void reasonUnknownToTheLibraryIsNotCustomAndDoesNotThrow() {
        // the platform event does SpawnReason.valueOf(name), which throws for COMMAND, SPELL, BUCKET, ...
        assertFalse(WorldListener.isCustomSpawn(eventWithReason(() -> CreatureSpawnEvent.SpawnReason.valueOf("COMMAND_NOT_IN_ENUM"))));
    }

    @Test
    void customSpawnIsIgnoredByTheMobPrevention() {
        assertTrue(WorldListener.isIgnoredSpawn(eventWithReason(() -> CreatureSpawnEvent.SpawnReason.CUSTOM)));
    }

    @Test
    void knownNonCustomReasonsAreNotIgnoredByTheMobPrevention() {
        assertFalse(WorldListener.isIgnoredSpawn(eventWithReason(() -> CreatureSpawnEvent.SpawnReason.NATURAL)));
        assertFalse(WorldListener.isIgnoredSpawn(eventWithReason(() -> CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)));
    }

    @Test
    void reasonUnknownToTheLibraryIsIgnoredByTheMobPreventionAndDoesNotThrow() {
        // e.g. an admin's /summon (COMMAND) must not be removed from the arena
        assertTrue(WorldListener.isIgnoredSpawn(eventWithReason(() -> CreatureSpawnEvent.SpawnReason.valueOf("COMMAND_NOT_IN_ENUM"))));
    }
}
