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

package org.screamingsandals.bedwars.game.timeline;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.variants.VariantImpl;
import org.screamingsandals.lib.utils.logger.Logger;
import org.spongepowered.configurate.ConfigurationNode;

/**
 * Loads the {@code timeline:} section of a variant file (called by {@code VariantLoaderImpl} after the custom
 * spawner types were parsed).
 */
public final class TimelineVariantLoader {
    private TimelineVariantLoader() {
    }

    /**
     * Parses the section, logs every problem as {@code Variant <name>: timeline: <problem>} and warns about spawner
     * types the variant does not know.
     *
     * @return the parsed definition (never null; {@link TimelineDefinition#EMPTY} for an empty section)
     */
    public static @NotNull TimelineDefinition load(@NotNull VariantImpl variant, @NotNull ConfigurationNode timelineNode, @NotNull Logger logger) {
        var result = TimelineParser.parse(timelineNode);
        for (var warning : result.warnings()) {
            logger.warn("Variant {}: timeline: {}", variant.getName(), warning);
        }

        var definition = result.definition();
        for (var type : definition.spawnerTiers().keySet()) {
            if (variant.getItemSpawnerType(type) == null) {
                logger.warn("Variant {}: timeline: spawner-tiers uses unknown spawner type {}", variant.getName(), type);
            }
        }
        for (var event : definition.events()) {
            if (event.type() == TimelineEventType.SPAWNER_TIER && event.spawnerType() != null
                    && variant.getItemSpawnerType(event.spawnerType()) == null) {
                logger.warn("Variant {}: timeline event {} uses unknown spawner type {}", variant.getName(), event.id(), event.spawnerType());
            }
        }
        return definition;
    }
}
