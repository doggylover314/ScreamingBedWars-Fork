/*
 * Copyright (C) 2025 ScreamingSandals
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

package org.screamingsandals.bedwars.utils;

import org.screamingsandals.lib.block.Block;
import org.screamingsandals.lib.utils.BlockFace;
import org.screamingsandals.lib.block.BlockPlacement;

public class BedUtils {
    public static BlockPlacement getBedNeighbor(BlockPlacement head) {
        if (!isBedBlock(head)) {
            return null;
        }

        var data = head.block();
        var offset = BedGeometry.otherHalf(data.get("part"), data.get("facing")); // exact, from the block states
        if (offset != null) {
            // The exact other half is known. Never scan for a neighbour here: if this half's partner is gone (half of
            // the bed was destroyed), scanning could return the half of an adjacent bed that belongs to another team.
            // Callers tolerate a non-bed block being returned (they check isBedBlock or it is simply replaced by air).
            return head.location().add(offset[0], 0, offset[1]).getBlock();
        }

        // legacy fallback (unchanged old scan) only for versions/blocks without part/facing states
        if (isBedBlock(head.location().add(BlockFace.EAST).getBlock())) {
            return head.location().add(BlockFace.EAST).getBlock();
        } else if (isBedBlock(head.location().add(BlockFace.WEST).getBlock())) {
            return head.location().add(BlockFace.WEST).getBlock();
        } else if (isBedBlock(head.location().add(BlockFace.SOUTH).getBlock())) {
            return head.location().add(BlockFace.SOUTH).getBlock();
        } else {
            return head.location().add(BlockFace.NORTH).getBlock();
        }
    }

    public static boolean isBedBlock(BlockPlacement block) {
        if (block == null) {
            return false;
        }

        return block.block().is("#beds", "straw_bed[*]");
    }

    public static boolean isBedBlock(Block data) {
        return data != null && data.is("#beds", "straw_bed[*]");
    }
}
