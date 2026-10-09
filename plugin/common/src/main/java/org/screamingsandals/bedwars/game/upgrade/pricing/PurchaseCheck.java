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

package org.screamingsandals.bedwars.game.upgrade.pricing;

/**
 * Result of validating an upgrade purchase before anything is charged. Pure enum.
 */
public enum PurchaseCheck {
    /** The purchase may proceed. */
    OK,
    /** The team already owns the highest level. */
    MAXED,
    /** A legacy spawner upgrade would exceed its maximum level. */
    LEGACY_MAX_LEVEL,
    /** The trap queue has no free slot. */
    QUEUE_FULL,
    /** The trap is already queued and duplicates are not allowed. */
    ALREADY_QUEUED,
    /** A legacy spawner upgrade matched no spawner. */
    NOTHING_TO_UPGRADE,
    /** The item is misconfigured or not available in this arena. */
    UNAVAILABLE
}
