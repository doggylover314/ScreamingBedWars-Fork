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

package org.screamingsandals.bedwars.game.mode;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default mode list written to config.yml.
 */
public final class ModeDefaults {
    private ModeDefaults() {
    }

    /**
     * 12 entries: team counts 2,3,4 x sizes 1..4 (in that order), as raw config maps (LinkedHashMap keeps key order).
     */
    public static @NotNull List<Map<String, Object>> defaultModeList() {
        var list = new ArrayList<Map<String, Object>>();
        for (int count = 2; count <= 4; count++) {
            for (int size = 1; size <= 4; size++) {
                var id = ModeRules.defaultId(count, size);
                var map = new LinkedHashMap<String, Object>();
                map.put("id", id);
                map.put("display-name", "<yellow>" + id);
                map.put("team-count", count);
                map.put("team-size", size);
                map.put("min-players", ModeRules.defaultMinPlayers(size));
                list.add(map);
            }
        }
        return list;
    }
}
