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

package org.screamingsandals.bedwars.lobby;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.game.mode.ModeManager;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.spectator.Component;

import java.util.List;
import java.util.Locale;

/**
 * Builds NPC hologram lines with the mode placeholders and refreshes them periodically (the holograms of slib do not
 * refresh by themselves).
 * <p>
 * Supported placeholders: {@code <mode> <mode-id> <mode-players> <mode-waiting> <mode-playing> <mode-arenas>}
 * (only for {@code JOIN_MODE} NPCs) and {@code <all-players>} (every NPC); PlaceholderAPI {@code %...%} placeholders
 * are resolved by slib per viewer.
 */
public final class NpcHologramPlaceholders {
    private NpcHologramPlaceholders() {
    }

    /**
     * Always attaches at least one lambda placeholder, so every call returns a Message that is NOT equal to the
     * previous one (Message is a Lombok data class and lambdas compare by identity). That is what makes the slib
     * hologram re-send a line when it is replaced.
     */
    public static @NotNull Message line(@NotNull BedWarsNPC npc, @NotNull String raw) {
        var message = Message.ofRichText(raw)
                .placeholder("all-players", sender -> Component.text(ModeManager.getInstance().countAllPlayers()));
        var value = npc.getValue();
        if (npc.getAction() == BedWarsNPC.Action.JOIN_MODE && value != null && !value.isBlank()) {
            var modeId = value.trim().toLowerCase(Locale.ROOT);
            message.placeholder("mode", sender -> ModeManager.getInstance().getMode(modeId)
                            .map(ModeManager::displayNameComponent)
                            .orElseGet(() -> Component.text(modeId)))
                    .placeholder("mode-id", sender -> Component.text(modeId))
                    .placeholder("mode-players", sender -> Component.text(ModeManager.getInstance().getStats(modeId).players()))
                    .placeholder("mode-waiting", sender -> Component.text(ModeManager.getInstance().getStats(modeId).waitingPlayers()))
                    .placeholder("mode-playing", sender -> Component.text(ModeManager.getInstance().getStats(modeId).playingPlayers()))
                    .placeholder("mode-arenas", sender -> Component.text(ModeManager.getInstance().getStats(modeId).joinableArenas()));
        }
        return message;
    }

    /**
     * Re-sends the dynamic lines of one NPC. Called by {@link NPCManager} every
     * {@code modes.npc-hologram-refresh-seconds} on the global thread.
     */
    public static void refresh(@NotNull BedWarsNPC npc) {
        var visual = npc.getNpc();
        if (visual == null || npc.getHologramAbove().isEmpty()) {
            return;
        }
        var lines = List.copyOf(npc.getHologramAbove());
        boolean papi = lines.stream().anyMatch(NpcHologramText::hasPapiPlaceholders);
        boolean own = lines.stream().anyMatch(NpcHologramText::hasOwnPlaceholders);
        if (!papi && !own) {
            return;
        }
        var signature = signature(npc);
        if (!papi && signature.equals(npc.getLastHologramSignature())) {
            return; // counts unchanged -> no packets
        }
        npc.setLastHologramSignature(signature);
        var hologram = visual.hologram();
        for (int i = 0; i < lines.size(); i++) {
            if (NpcHologramText.isDynamic(lines.get(i))) {
                hologram.replaceLine(i, line(npc, lines.get(i)));
            }
        }
    }

    static @NotNull String signature(@NotNull BedWarsNPC npc) {
        var sb = new StringBuilder().append(npc.getAction()).append('|').append(npc.getValue())
                .append('|').append(ModeManager.getInstance().countAllPlayers());
        if (npc.getAction() == BedWarsNPC.Action.JOIN_MODE && npc.getValue() != null) {
            var s = ModeManager.getInstance().getStats(npc.getValue());
            sb.append('|').append(s.waitingPlayers()).append('|').append(s.playingPlayers()).append('|').append(s.joinableArenas());
        }
        return sb.toString();
    }
}
