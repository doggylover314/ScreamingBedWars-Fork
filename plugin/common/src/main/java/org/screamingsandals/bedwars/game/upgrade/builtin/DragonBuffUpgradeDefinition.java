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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.api.game.upgrade.Upgradable;
import org.screamingsandals.bedwars.game.GameImpl;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.endgame.GameEndgameService;
import org.screamingsandals.bedwars.lib.debug.Debug;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;

/**
 * Team upgrade that gives the team extra ender dragons in sudden death.
 * The sudden-death code reads the bought level through {@link #getExtraDragons(TeamImpl)}.
 */
@Getter
@RequiredArgsConstructor
public class DragonBuffUpgradeDefinition implements BuiltInUpgradeDefinition {
    private final @NotNull DragonBuffSpec spec;

    @Override
    public double getInitialLevel() {
        return 0;
    }

    @Override
    public @Nullable Double getMaximalLevel() {
        return (double) spec.maxLevel();
    }

    @Override
    public boolean isApplicable(@NotNull Upgradable upgradable) {
        return upgradable instanceof TeamImpl;
    }

    /**
     * Extra sudden-death dragons the team gets from its Dragon Buff upgrade.
     * Sudden death spawns {@code 1 + getExtraDragons(team)} dragons per alive team.
     *
     * @return the number of extra dragons; 0 if none were bought or the variant has no dragon-buff upgrade
     */
    public static int getExtraDragons(@NotNull TeamImpl team) {
        var game = team.getGame();
        if (game == null) {
            return 0;
        }
        int extra = 0;
        for (var entry : game.getGameVariant().getUpgrades().entrySet()) {
            if (!(entry.getValue() instanceof DragonBuffUpgradeDefinition)) {
                continue;
            }
            var upgrade = team.getUpgrade(entry.getKey());
            if (upgrade == null) {
                continue;
            }
            extra += ((DragonBuffUpgradeDefinition) entry.getValue()).getSpec()
                    .extraDragonsFor((int) Math.floor(upgrade.getLevel() - upgrade.getInitialLevel() + 1e-9));
        }
        return Math.max(0, extra);
    }

    /**
     * Sudden death reads the Dragon Buff levels once, when it spawns the dragons. From then on buying the buff would
     * cost the team resources and change nothing.
     *
     * @return true when {@code upgradeName} is a Dragon Buff of the game variant and sudden death already fixed the dragon counts
     */
    public static boolean isLockedBySuddenDeath(@NotNull GameImpl game, @NotNull String upgradeName) {
        return game.getGameVariant().getUpgrade(upgradeName) instanceof DragonBuffUpgradeDefinition
                && GameEndgameService.getInstance().isDragonBuffLocked(game);
    }

    public static class Loader implements BuiltInUpgradeDefinition.Loader<DragonBuffUpgradeDefinition> {
        public static final @NotNull Loader INSTANCE = new Loader();

        @Override
        public @NotNull DragonBuffUpgradeDefinition load(@NotNull ConfigurationNode node) throws ConfigurateException {
            var warnings = new ArrayList<String>();
            var spec = DragonBuffSpec.parse(node, warnings);
            warnings.forEach(w -> Debug.warn("Dragon buff upgrade " + node.key() + ": " + w, true));
            return new DragonBuffUpgradeDefinition(spec);
        }
    }
}
