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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.lib.item.ItemStack;
import org.screamingsandals.lib.item.builder.ItemStackFactory;
import org.screamingsandals.lib.item.meta.PotionEffect;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.spectator.sound.SoundStart;

import java.util.List;

/**
 * Runtime form of a queued trap: the parsed {@link QueuedTrapSpec} plus the resolved potion effects and sound.
 */
@Getter
@RequiredArgsConstructor
public final class QueuedTrapDefinition {
    private final @NotNull QueuedTrapSpec spec;
    private final @NotNull List<@NotNull PotionEffect> enemyEffects;
    private final @NotNull List<@NotNull PotionEffect> teamEffects;
    private final @Nullable SoundStart triggerSound;

    /**
     * A fresh (Message is mutable) rich-text message of the trap name; {@code "@key"} links are resolved by slib.
     */
    public @NotNull Message displayName() {
        return Message.ofRichText(spec.name());
    }

    /**
     * @return the queue display icon or {@code null} when none is configured or the item type is unknown
     */
    public @Nullable ItemStack icon() {
        return spec.icon() == null ? null : ItemStackFactory.build(spec.icon());
    }
}
