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
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Shifts every location string of a serialized arena file (pure, Configurate only).
 * <p>
 * Everything else in the node (team colours, max players, target types and countdowns, spawner and store settings,
 * {@code constant}, {@code variant}, {@code game-display-name}, {@code fee}, timers, weather, data other areas add)
 * is kept verbatim, which is how the clone copies it.
 */
public final class ArenaNodeRelocator {
    private ArenaNodeRelocator() {
    }

    /**
     * Paths of every location string an arena file stores ({@code LocalGameLoaderImpl#writeGameNode} plus the legacy
     * keys {@code loadGame} still reads). {@code "*"} = every child of a map node, {@code "[]"} = every element of a
     * list node. Terminal scalar strings are shifted; map/list terminals are ignored.
     * <p>
     * AREAS THAT ADD LOCATION FIELDS TO THE ARENA FILE MUST ADD THEIR PATH HERE (shared touchpoint).
     */
    public static final List<List<String>> ARENA_LOCATION_PATHS = List.of(
            List.of("pos1"),
            List.of("pos2"),
            List.of("specSpawn"),
            List.of("teams", "*", "spawns", "[]"),
            List.of("teams", "*", "spawn"),                // legacy single spawn
            List.of("teams", "*", "bed"),                  // legacy target
            List.of("teams", "*", "target", "loc"),        // "block" and "block-countdown" targets
            List.of("spawners", "[]", "location"),
            List.of("stores", "[]", "loc"),
            List.of("stores", "[]")                        // legacy store = plain location string
    );

    /**
     * The waiting lobby (spawn and region). Shifted only for {@link ClonePlanner.LobbyPolicy#RELOCATE}.
     */
    public static final List<List<String>> LOBBY_LOCATION_PATHS = List.of(
            List.of("lobbySpawn"),
            List.of("lobbyPos1"),
            List.of("lobbyPos2")
    );

    public record Options(
            @NotNull BlockOffset offset,
            @NotNull String targetWorld,
            @NotNull ClonePlanner.LobbyPolicy lobbyPolicy,
            @NotNull String newUuid,
            @NotNull String newName
    ) {
    }

    /**
     * @param relocated number of location strings that were shifted
     * @param warnings  one line per location string that could not be parsed (left unchanged)
     */
    public record Result(int relocated, @NotNull List<String> warnings) {
    }

    public static @NotNull Result relocate(@NotNull ConfigurationNode root, @NotNull Options o) throws SerializationException {
        root.node("uuid").set(o.newUuid());
        root.node("name").set(o.newName());
        root.node("world").set(o.targetWorld());

        int n = 0;
        var warnings = new ArrayList<String>();
        for (var p : ARENA_LOCATION_PATHS) {
            n += shift(root, p, 0, o.offset(), warnings, "");
        }
        switch (o.lobbyPolicy()) {
            case RELOCATE:
                for (var p : LOBBY_LOCATION_PATHS) {
                    n += shift(root, p, 0, o.offset(), warnings, "");
                }
                root.node("lobbySpawnWorld").set(o.targetWorld());
                break;
            case KEEP_SHARED_DROP_REGION:
                root.node("lobbyPos1").set(null);
                root.node("lobbyPos2").set(null);
                break;
            case KEEP_SHARED:
            default:
                break;
        }
        return new Result(n, warnings);
    }

    private static int shift(@NotNull ConfigurationNode node, @NotNull List<String> path, int index, @NotNull BlockOffset offset,
                             @NotNull List<String> warnings, @NotNull String pathString) throws SerializationException {
        if (index == path.size()) {
            if (node.virtual() || node.isMap() || node.isList()) {
                return 0;
            }
            var s = node.getString();
            if (s == null) {
                return 0;
            }
            var shifted = LocationStrings.shift(s, offset.dx(), offset.dy(), offset.dz());
            if (shifted == null) {
                warnings.add(pathString + ": unparsable location '" + s + "'");
                return 0;
            }
            node.set(shifted);
            return 1;
        }

        var segment = path.get(index);
        int n = 0;
        switch (segment) {
            case "*":
                for (var entry : List.copyOf(node.childrenMap().entrySet())) {
                    n += shift(entry.getValue(), path, index + 1, offset, warnings, join(pathString, String.valueOf(entry.getKey())));
                }
                break;
            case "[]":
                var list = node.childrenList();
                for (int i = 0; i < list.size(); i++) {
                    n += shift(list.get(i), path, index + 1, offset, warnings, pathString + "[" + i + "]");
                }
                break;
            default:
                if (node.isMap()) { // never materialise children of scalars/lists by accident
                    n += shift(node.node(segment), path, index + 1, offset, warnings, join(pathString, segment));
                }
                break;
        }
        return n;
    }

    private static @NotNull String join(@NotNull String base, @NotNull String segment) {
        return base.isEmpty() ? segment : base + "." + segment;
    }
}
