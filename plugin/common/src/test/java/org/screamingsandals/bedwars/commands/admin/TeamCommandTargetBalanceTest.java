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

package org.screamingsandals.bedwars.commands.admin;

import org.junit.jupiter.api.Test;
import org.screamingsandals.bedwars.api.game.target.Target;
import org.screamingsandals.bedwars.game.target.ExpirableTargetImpl;
import org.screamingsandals.bedwars.game.target.NoTargetImpl;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamCommandTargetBalanceTest {
    private static final Target COUNTDOWN = new ExpirableTargetImpl(60);

    @Test
    void teamsWithoutATargetYetAreNotADifferentType() {
        // guided setup: only the first team has its target, the others are simply not configured yet
        assertFalse(TeamCommand.hasMixedTargets(Arrays.asList(NoTargetImpl.INSTANCE, null, null), NoTargetImpl.class));
        assertFalse(TeamCommand.hasMixedTargets(Arrays.asList(null, null), ExpirableTargetImpl.class));
    }

    @Test
    void sameTypeEverywhereIsBalanced() {
        assertFalse(TeamCommand.hasMixedTargets(List.of(COUNTDOWN, new ExpirableTargetImpl(30)), ExpirableTargetImpl.class));
        assertFalse(TeamCommand.hasMixedTargets(List.of(), NoTargetImpl.class));
    }

    @Test
    void anotherConfiguredTypeIsReported() {
        assertTrue(TeamCommand.hasMixedTargets(Arrays.asList(COUNTDOWN, NoTargetImpl.INSTANCE, null), ExpirableTargetImpl.class));
        assertTrue(TeamCommand.hasMixedTargets(List.of(NoTargetImpl.INSTANCE, COUNTDOWN), NoTargetImpl.class));
    }
}
