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

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonBuffSpecTest {
    @Test
    void defaults() throws Exception {
        var warnings = new ArrayList<String>();
        var spec = DragonBuffSpec.parse(YamlConfigurationLoader.builder().buildAndLoadString("{}"), warnings);
        assertEquals(new DragonBuffSpec(1, 1), spec);
        assertTrue(warnings.isEmpty());
        assertEquals(0, spec.extraDragonsFor(0));
        assertEquals(1, spec.extraDragonsFor(1));
        assertEquals(1, spec.extraDragonsFor(2)); // clamped to the max level
        assertEquals(0, spec.extraDragonsFor(-3));
    }

    @Test
    void multipleLevelsAndDragons() {
        var spec = new DragonBuffSpec(2, 2);
        assertEquals(2, spec.extraDragonsFor(1));
        assertEquals(4, spec.extraDragonsFor(2));
        assertEquals(4, spec.extraDragonsFor(5));
    }

    @Test
    void invalidValuesAreCorrected() throws Exception {
        var warnings = new ArrayList<String>();
        var spec = DragonBuffSpec.parse(YamlConfigurationLoader.builder().buildAndLoadString("max-level: 0\nextra-dragons-per-level: -1\n"), warnings);
        assertEquals(new DragonBuffSpec(1, 0), spec);
        assertEquals(2, warnings.size());
    }
}
