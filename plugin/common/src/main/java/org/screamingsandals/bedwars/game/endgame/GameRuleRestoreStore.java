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

package org.screamingsandals.bedwars.game.endgame;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Small file ({@code world=true|false}) with the original value of a world game rule that sudden death overrides.
 * <p>
 * The game rule is saved in the world data, so after a crash during a sudden death nothing in memory knows the old value.
 * The entry is written before the rule is changed and removed after it was restored; leftovers are restored on the next start.
 * The file is deleted when the last entry is removed.
 */
final class GameRuleRestoreStore {
    private final @NotNull Path file;

    GameRuleRestoreStore(@NotNull Path file) {
        this.file = file;
    }

    /**
     * @return world name to the value to restore; empty when the file does not exist
     */
    synchronized @NotNull Map<String, Boolean> load() throws IOException {
        var result = new HashMap<String, Boolean>();
        if (!Files.isRegularFile(file)) {
            return result;
        }
        var properties = new Properties();
        properties.load(new StringReader(Files.readString(file, StandardCharsets.UTF_8)));
        for (var name : properties.stringPropertyNames()) {
            var value = properties.getProperty(name).trim();
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                result.put(name, Boolean.parseBoolean(value));
            } // anything else is a damaged entry and is dropped on the next write
        }
        return result;
    }

    synchronized @Nullable Boolean get(@NotNull String world) throws IOException {
        return load().get(world);
    }

    synchronized void put(@NotNull String world, boolean value) throws IOException {
        var entries = load();
        entries.put(world, value);
        write(entries);
    }

    /**
     * Removes the entry of the world; deletes the file when it was the last one. No-op for an unknown world.
     */
    synchronized void remove(@NotNull String world) throws IOException {
        var entries = load();
        if (entries.remove(world) == null) {
            return;
        }
        write(entries);
    }

    private void write(@NotNull Map<String, Boolean> entries) throws IOException {
        if (entries.isEmpty()) {
            Files.deleteIfExists(file);
            return;
        }
        var properties = new Properties();
        entries.forEach((world, value) -> properties.setProperty(world, Boolean.toString(value)));
        var writer = new StringWriter();
        properties.store(writer, "Original values of the game rules changed by sudden death, restored on the next start after a crash");

        var parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        var temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (var channel = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            var buffer = ByteBuffer.wrap(writer.toString().getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true); // the whole point is to survive a crash or power loss
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
