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
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.mode.ModeManager;
import org.screamingsandals.bedwars.game.mode.ModeStats;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.spectator.Component;

import java.util.ArrayList;
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
     * <p>
     * The numbers are read ONCE here, on the calling (global) thread, and the placeholders only capture them: slib
     * resolves the placeholders per viewer, possibly on another thread, where the game state must not be touched
     * (and where every viewer would otherwise walk all arenas again). Because the lambdas capture the numbers they are
     * new instances per call, so the Message still differs. The lambdas capture only immutable values (numbers, the mode
     * id and its already resolved display name), they never read the game state or the mode registry themselves.
     */
    public static @NotNull Message line(@NotNull BedWarsNPC npc, @NotNull String raw) {
        return line(npc, raw, Counts.read(npc));
    }

    /**
     * All the given lines of one NPC built from a single read of the numbers (spawn): the arenas are walked once, not
     * once per line.
     */
    static @NotNull List<Message> lines(@NotNull BedWarsNPC npc, @NotNull List<String> raws) {
        if (raws.isEmpty()) {
            return List.of();
        }
        var counts = Counts.read(npc);
        var result = new ArrayList<Message>(raws.size());
        for (var raw : raws) {
            result.add(line(npc, raw, counts));
        }
        return result;
    }

    private static @NotNull Message line(@NotNull BedWarsNPC npc, @NotNull String raw, @NotNull Counts counts) {
        final int allPlayers = counts.allPlayers();
        var message = Message.ofRichText(raw)
                .placeholder("all-players", sender -> Component.text(allPlayers));
        var modeId = counts.modeId();
        if (modeId != null) {
            final var stats = counts.stats();
            final var modeName = counts.modeName();
            message.placeholder("mode", sender -> modeName)
                    .placeholder("mode-id", sender -> Component.text(modeId))
                    .placeholder("mode-players", sender -> Component.text(stats.players()))
                    .placeholder("mode-waiting", sender -> Component.text(stats.waitingPlayers()))
                    .placeholder("mode-playing", sender -> Component.text(stats.playingPlayers()))
                    .placeholder("mode-arenas", sender -> Component.text(stats.joinableArenas()));
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
        var counts = Counts.read(npc); // once per refresh: shared by the signature and every line
        var signature = counts.signature(npc);
        if (!papi && signature.equals(npc.getLastHologramSignature())) {
            return; // counts unchanged -> no packets
        }
        npc.setLastHologramSignature(signature);
        var hologram = visual.hologram();
        for (int i = 0; i < lines.size(); i++) {
            if (NpcHologramText.isDynamic(lines.get(i))) {
                hologram.replaceLine(i, line(npc, lines.get(i), counts));
            }
        }
    }

    static @NotNull String signature(@NotNull String action, @Nullable String value, int allPlayers, @Nullable ModeStats stats) {
        var sb = new StringBuilder().append(action).append('|').append(value).append('|').append(allPlayers);
        if (stats != null) {
            sb.append('|').append(stats.waitingPlayers()).append('|').append(stats.playingPlayers()).append('|').append(stats.joinableArenas());
        }
        return sb.toString();
    }

    /**
     * Numbers one refresh of an NPC needs, read once (global thread).
     *
     * @param modeId   normalised mode id of a {@code JOIN_MODE} NPC, otherwise {@code null}
     * @param modeName display name of that mode (the id itself if the mode is unknown), resolved here so that the
     *                 placeholders do not touch the mode registry
     */
    private record Counts(int allPlayers, @Nullable String modeId, @NotNull Component modeName, @NotNull ModeStats stats) {
        static @NotNull Counts read(@NotNull BedWarsNPC npc) {
            var manager = ModeManager.getInstance();
            var value = npc.getValue();
            var modeId = npc.getAction() == BedWarsNPC.Action.JOIN_MODE && value != null && !value.isBlank()
                    ? value.trim().toLowerCase(Locale.ROOT)
                    : null;
            if (modeId == null) {
                return new Counts(manager.countAllPlayers(), null, Component.empty(), ModeStats.EMPTY);
            }
            var mode = manager.getMode(modeId);
            return new Counts(
                    manager.countAllPlayers(),
                    modeId,
                    mode.map(ModeManager::displayNameComponent).orElseGet(() -> Component.text(modeId)),
                    mode.map(manager::getStats).orElse(ModeStats.EMPTY)
            );
        }

        @NotNull String signature(@NotNull BedWarsNPC npc) {
            return NpcHologramPlaceholders.signature(String.valueOf(npc.getAction()), npc.getValue(), allPlayers, modeId == null ? null : stats);
        }
    }
}
