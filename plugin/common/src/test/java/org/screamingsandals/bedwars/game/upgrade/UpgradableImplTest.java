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

package org.screamingsandals.bedwars.game.upgrade;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.screamingsandals.bedwars.api.game.upgrade.Upgradable;
import org.screamingsandals.bedwars.game.upgrade.builtin.BuiltInUpgradeDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpgradableImplTest {
    private static BuiltInUpgradeDefinition definition(double max, boolean applicable) {
        return new BuiltInUpgradeDefinition() {
            @Override
            public double getInitialLevel() {
                return 0;
            }

            @Override
            public @Nullable Double getMaximalLevel() {
                return max;
            }

            @Override
            public boolean isApplicable(@NotNull Upgradable upgradable) {
                return applicable;
            }
        };
    }

    private static Map<String, BuiltInUpgradeDefinition> defs(Object... nameAndDefinition) {
        var map = new LinkedHashMap<String, BuiltInUpgradeDefinition>();
        for (int i = 0; i < nameAndDefinition.length; i += 2) {
            map.put((String) nameAndDefinition[i], (BuiltInUpgradeDefinition) nameAndDefinition[i + 1]);
        }
        return map;
    }

    /** Exposes the protected API. */
    private static final class TestUpgradable extends UpgradableImpl {
        void sync(Map<String, BuiltInUpgradeDefinition> upgrades) {
            syncBuiltInUpgrades(upgrades);
        }

        void reset() {
            resetUpgrades();
        }
    }

    @Test
    void syncAddsBuiltInUpgradesAndKeepsPluginUpgrades() {
        var up = new TestUpgradable();
        up.registerUpgrade("plugin:x", 0, null);

        up.sync(defs("a", definition(4, true), "b", definition(4, true)));
        assertEquals(Set.of("plugin:x", "a", "b"), up.getUpgrades().keySet());

        // "b" disappeared from the variant: the stale built-in is dropped, the plugin upgrade stays
        up.sync(defs("a", definition(4, true)));
        assertEquals(Set.of("plugin:x", "a"), up.getUpgrades().keySet());
    }

    @Test
    void changedDefinitionCreatesANewUpgrade() {
        var up = new TestUpgradable();
        up.sync(defs("a", definition(4, true)));
        var first = up.getUpgrade("a");
        assertNotNull(first);
        assertEquals(4.0, first.getMaximalLevel());

        up.sync(defs("a", definition(2, true)));
        var second = up.getUpgrade("a");
        assertNotNull(second);
        assertEquals(2.0, second.getMaximalLevel());
    }

    @Test
    void unchangedDefinitionKeepsTheSameUpgradeAndLevel() {
        var up = new TestUpgradable();
        up.sync(defs("a", definition(4, true)));
        var first = up.getUpgrade("a");
        assertNotNull(first);
        first.setLevel(3);

        up.sync(defs("a", definition(4, true)));
        assertSame(first, up.getUpgrade("a"));
        assertEquals(3.0, up.getUpgrade("a").getLevel());

        up.reset();
        assertEquals(0.0, up.getUpgrade("a").getLevel());
    }

    @Test
    void notApplicableDefinitionsAreNotRegisteredAndAreRemoved() {
        var up = new TestUpgradable();
        up.sync(defs("a", definition(4, false)));
        assertNull(up.getUpgrade("a"));

        up.sync(defs("a", definition(4, true)));
        assertNotNull(up.getUpgrade("a"));

        up.sync(defs("a", definition(4, false)));
        assertNull(up.getUpgrade("a"));
    }

    @Test
    void registeringTwiceStillFails() {
        var up = new TestUpgradable();
        up.registerUpgrade("plugin:x", 0, null);
        assertThrows(IllegalStateException.class, () -> up.registerUpgrade("plugin:x", 0, null));
    }
}
