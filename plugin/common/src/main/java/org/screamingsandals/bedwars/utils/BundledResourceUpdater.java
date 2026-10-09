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

package org.screamingsandals.bedwars.utils;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.BedWarsPlugin;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.lib.utils.logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Replaces outdated copies of bundled files (lower {@code # fork-revision} than the file in the jar).
 * Runtime class (needs the server and the main config), not unit-testable.
 */
public final class BundledResourceUpdater {
    /**
     * Bundled files that carry a {@code # fork-revision: N} marker. Add a marker to the bundled file when appending here.
     */
    public static final List<String> TRACKED = List.of(
            "variants/certain-popular-server.yml",
            "shop/certain-popular-server/upgrade-shop.yml"
    );

    private BundledResourceUpdater() {
    }

    public static void updateOutdated(@NotNull Path dataFolder, @NotNull Logger logger) {
        boolean autoUpdate = MainConfig.getInstance().node("bundled-files", "auto-update").getBoolean(true);
        boolean backup = MainConfig.getInstance().node("bundled-files", "backup").getBoolean(true);
        for (String resource : TRACKED) {
            try {
                updateOne(dataFolder, resource, autoUpdate, backup, logger);
            } catch (IOException | UncheckedIOException ex) {
                logger.error("Could not check/update bundled file {}", resource, ex);
            }
        }
    }

    private static void updateOne(@NotNull Path dataFolder, @NotNull String resource, boolean autoUpdate, boolean backup, @NotNull Logger logger) throws IOException {
        Path target = dataFolder.resolve(resource);
        if (!Files.isRegularFile(target)) {
            return; // the normal copy-if-missing code handles it
        }
        int bundled;
        try (InputStream in = BundledResourceUpdater.class.getResourceAsStream("/" + resource)) {
            if (in == null) {
                return;
            }
            var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            bundled = BundledRevision.parse(reader.lines().limit(BundledRevision.MAX_SCAN_LINES).collect(Collectors.toList()));
        }
        int onDisk;
        try (var lines = Files.lines(target, StandardCharsets.UTF_8)) {
            onDisk = BundledRevision.parse(lines.limit(BundledRevision.MAX_SCAN_LINES).collect(Collectors.toList()));
        }
        if (!BundledRevision.isOutdated(onDisk, bundled)) {
            return;
        }
        if (!autoUpdate) {
            logger.warn("{} is outdated (fork-revision {} < {}). Delete it or set bundled-files.auto-update: true to get the new defaults.", resource, onDisk, bundled);
            return;
        }
        if (backup) {
            // files in variants/ are all loaded unless they end with ".disabled" -> keep variant backups disabled
            String suffix = resource.startsWith("variants/") ? ".bak.disabled" : ".bak";
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String base = target.getFileName() + ".rev" + Math.max(onDisk, 0) + "." + stamp;
            Path backupPath = target.resolveSibling(base + suffix);
            for (int n = 2; Files.exists(backupPath); n++) {
                backupPath = target.resolveSibling(base + "-" + n + suffix); // two updates within one second
            }
            Files.move(target, backupPath); // no REPLACE_EXISTING: old backups survive
            logger.info("Backed up {} to {}", resource, backupPath.getFileName());
        } else {
            Files.delete(target);
        }
        BedWarsPlugin.getInstance().saveResource(resource, true);
        logger.info("Updated {} to fork-revision {} (backup: {})", resource, bundled, backup);
    }
}
