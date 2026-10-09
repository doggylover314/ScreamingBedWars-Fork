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

package org.screamingsandals.bedwars.lobby;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.game.GameManagerImpl;
import org.screamingsandals.bedwars.game.GroupManagerImpl;
import org.screamingsandals.bedwars.game.mode.ModeJoinService;
import org.screamingsandals.bedwars.inventories.GamesInventory;
import org.screamingsandals.bedwars.player.PlayerManagerImpl;
import org.screamingsandals.bedwars.utils.MiscUtils;
import org.screamingsandals.bedwars.utils.SerializableLocation;
import org.screamingsandals.lib.Server;
import org.screamingsandals.lib.npc.NPC;
import org.screamingsandals.lib.npc.skin.NPCSkin;
import org.screamingsandals.lib.player.Player;
import org.screamingsandals.lib.tasker.DefaultThreads;
import org.screamingsandals.lib.tasker.Tasker;
import org.screamingsandals.lib.utils.InteractType;
import org.screamingsandals.lib.world.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@ConfigSerializable
public class BedWarsNPC {
    private SerializableLocation location;
    @Nullable
    private NPCSkin skin;
    private Action action = Action.DUMMY;
    private String value;
    private boolean shouldLookAtPlayer = true;
    private final List<String> hologramAbove = new ArrayList<>();
    /**
     * Last state the hologram was rendered for (NpcHologramPlaceholders); transient: not saved, not part of equals.
     */
    private transient @Nullable String lastHologramSignature;

    @Getter
    private transient NPC npc;

    public void spawn() {
        if (npc == null && location != null && location.isWorldLoaded()) {
            npc = NPC.of(location.as(Location.class))
                    .touchable(true)
                    .lookAtPlayer(shouldLookAtPlayer);

            var holo = npc.hologram();
            for (var line : NpcHologramPlaceholders.lines(this, hologramAbove)) { // numbers read once for all lines
                holo.bottomLine(line);
            }
            lastHologramSignature = null;

            if (skin != null && skin.getValue() != null) {
                npc.skin(skin);
            }

            npc.show();

            Server.getConnectedPlayers().forEach(npc::addViewer);
        }
    }

    public void destroy() {
        if (npc != null) {
            npc.destroy();
            npc = null;
        }
    }

    public void handleClick(Player player, InteractType type) {
        if (action != null && (!action.requireArguments() || (value != null && !value.isEmpty()))) {
            action.handler.accept(this, player, type);
        }
    }

    @RequiredArgsConstructor
    public enum Action {
        DUMMY((bedWarsNPC, player, type) -> {
        }),
        PLAYER_COMMAND((bedWarsNPC, player, type) -> {
            Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> player.tryToDispatchCommand(bedWarsNPC.value));
        }),
        CONSOLE_COMMAND((bedWarsNPC, player, type) -> {
            Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> Server.getConsoleSender().tryToDispatchCommand(bedWarsNPC.value));
        }),
        OPEN_GAMES_INVENTORY((bedWarsNPC, player, type) -> {
            Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> GamesInventory.getInstance().openForPlayer(player, bedWarsNPC.value));
        }),
        JOIN_GAME((bedWarsNPC, player, type) -> {
            GameManagerImpl.getInstance().getGame(bedWarsNPC.value).ifPresent(game1 ->
                    Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> game1.joinToGame(PlayerManagerImpl.getInstance().getPlayerOrCreate(player)))
            );
        }),
        // fork: JOIN_GROUP / JOIN_VARIANT / JOIN_RANDOM pick the arena on the global thread, with the joining player, so that
        // a party leader gets an arena with room for his party
        JOIN_GROUP((bedWarsNPC, player, type) -> {
            Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> {
                var bwPlayer = PlayerManagerImpl.getInstance().getPlayerOrCreate(player);
                MiscUtils.getGameWithHighestPlayers(GroupManagerImpl.getInstance().getGamesInGroup(bedWarsNPC.value), false, bwPlayer)
                        .ifPresent(game -> game.joinToGame(bwPlayer));
            });
        }),
        JOIN_VARIANT((bedWarsNPC, player, type) -> {
            Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> {
                var bwPlayer = PlayerManagerImpl.getInstance().getPlayerOrCreate(player);
                MiscUtils.getGameWithHighestPlayers(GameManagerImpl.getInstance().getLocalGames().stream().filter(game -> game.getGameVariant().getName().equals(bedWarsNPC.value)).collect(Collectors.toList()), false, bwPlayer)
                        .ifPresent(game -> game.joinToGame(bwPlayer));
            });
        }),
        JOIN_RANDOM((bedWarsNPC, player, type) -> {
            Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> {
                var bwPlayer = PlayerManagerImpl.getInstance().getPlayerOrCreate(player);
                MiscUtils.getGameWithHighestPlayers(GameManagerImpl.getInstance().getGames(), false, bwPlayer)
                        .ifPresent(game -> game.joinToGame(bwPlayer));
            });
        }),
        TELEPORT_TO_LOBBY((bedWarsNPC, player, type) ->
                Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> MainLobby.teleportFromNpc(player))
        ),
        JOIN_MODE((bedWarsNPC, player, type) ->
                Tasker.run(DefaultThreads.GLOBAL_THREAD, () -> ModeJoinService.getInstance().joinMode(player, bedWarsNPC.value))
        );

        private final Handler handler;

        public boolean requireArguments() {
            return this != DUMMY && this != JOIN_RANDOM && this != TELEPORT_TO_LOBBY;
        }

        public interface Handler {
            void accept(BedWarsNPC bedWarsNPC, Player player, InteractType type);
        }
    }

}
