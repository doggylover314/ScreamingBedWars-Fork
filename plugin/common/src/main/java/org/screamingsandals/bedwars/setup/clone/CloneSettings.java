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

package org.screamingsandals.bedwars.setup.clone;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.config.MainConfig;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code clone.*} section of config.yml, read once per job.
 */
public record CloneSettings(
        int blocksPerTick,
        int maxMillisPerTick,
        long maxBlocks,
        int progressIntervalSeconds,
        boolean requireConfirmation,
        int confirmationTimeoutSeconds,
        boolean copyBlockEntities,
        int blockEntitiesPerTick,
        @NotNull List<String> blockEntityTypes,
        boolean copyEntities,
        @NotNull List<String> entityTypes,
        boolean skipHologramArmorStands
) {
    public static final List<String> DEFAULT_BLOCK_ENTITY_TYPES = List.of("#all_signs", "#signs", "#banners", "#shulker_boxes",
            "chest", "trapped_chest", "barrel", "furnace", "blast_furnace", "smoker", "hopper", "dropper",
            "dispenser", "brewing_stand", "lectern", "jukebox", "chiseled_bookshelf", "decorated_pot",
            "crafter", "player_head", "player_wall_head", "spawner", "beacon");
    public static final List<String> DEFAULT_ENTITY_TYPES = List.of("item_frame", "glow_item_frame", "painting", "armor_stand");

    /**
     * Reads the settings with the defaults of the config and clamps them to sane values:
     * blocks per tick 64..1,000,000, millis per tick 1..45, block entities per tick 1..10,000,
     * progress interval >= 1 s, confirmation timeout >= 5 s, max blocks >= 1.
     */
    public static @NotNull CloneSettings load() {
        var config = MainConfig.getInstance();
        return new CloneSettings(
                clamp(config.node("clone", "blocks-per-tick").getInt(4096), 64, 1_000_000),
                clamp(config.node("clone", "max-millis-per-tick").getInt(15), 1, 45),
                Math.max(1L, config.node("clone", "max-blocks").getLong(20_000_000L)),
                Math.max(1, config.node("clone", "progress-interval-seconds").getInt(5)),
                config.node("clone", "require-confirmation").getBoolean(true),
                Math.max(5, config.node("clone", "confirmation-timeout-seconds").getInt(60)),
                config.node("clone", "copy-block-entities").getBoolean(true),
                clamp(config.node("clone", "block-entities-per-tick").getInt(64), 1, 10_000),
                list(config.node("clone", "block-entity-types"), DEFAULT_BLOCK_ENTITY_TYPES),
                config.node("clone", "copy-entities").getBoolean(true),
                list(config.node("clone", "entity-types"), DEFAULT_ENTITY_TYPES),
                config.node("clone", "skip-hologram-armor-stands").getBoolean(true)
        );
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static @NotNull List<String> list(@NotNull ConfigurationNode node, @NotNull List<String> def) {
        if (node.virtual()) {
            return def;
        }
        try {
            var values = node.getList(String.class, def);
            return values == null ? def : new ArrayList<>(values);
        } catch (SerializationException e) {
            return def;
        }
    }
}
