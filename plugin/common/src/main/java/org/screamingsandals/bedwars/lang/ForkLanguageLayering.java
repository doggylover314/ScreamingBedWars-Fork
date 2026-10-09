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

package org.screamingsandals.bedwars.lang;

import org.jetbrains.annotations.NotNull;
import org.screamingsandals.lib.lang.container.TranslationContainer;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure helpers (no server access) for layering the fork-local language overlay on top of the upstream BedWarsLanguage files.
 * <p>
 * Precedence (first hit wins):
 * <pre>
 * selected-locale container (only if the selected locale is not en-US):
 *     admin custom locale  &gt;  fork language_tag + overrides_tag (if shipped)  &gt;  BedWarsLanguage tag  &gt;  fallback
 * en-US container:
 *     admin custom en-US  &gt;  fork language_en-US + overrides_en-US  &gt;  BedWarsLanguage en-US
 * </pre>
 */
public final class ForkLanguageLayering {
    public static final String FORK_RESOURCE_PREFIX = "bedwars-fork/";
    public static final String DEFAULT_US_RESOURCE = "languages/language_en-US.json";
    private static final String UPSTREAM_FILE_PREFIX = "language_";
    private static final String OVERRIDES_FILE_PREFIX = "overrides_";

    private ForkLanguageLayering() {
    }

    /**
     * "languages/language_en-US.json" -> "bedwars-fork/languages/language_en-US.json"
     */
    public static @NotNull String forkResourcePath(@NotNull String upstreamPath) {
        return FORK_RESOURCE_PREFIX + upstreamPath;
    }

    /**
     * "languages/language_en-US.json" -> "bedwars-fork/languages/overrides_en-US.json"
     */
    public static @NotNull String forkOverridesResourcePath(@NotNull String upstreamPath) {
        int slash = upstreamPath.lastIndexOf('/');
        String dir = slash >= 0 ? upstreamPath.substring(0, slash + 1) : "";
        String file = slash >= 0 ? upstreamPath.substring(slash + 1) : upstreamPath;
        if (file.startsWith(UPSTREAM_FILE_PREFIX)) {
            file = file.substring(UPSTREAM_FILE_PREFIX.length());
        }
        return FORK_RESOURCE_PREFIX + dir + OVERRIDES_FILE_PREFIX + file;
    }

    /**
     * Loads a UTF-8 JSON classpath resource relative to the classpath root. Missing resource -> empty root.
     * Malformed JSON -> throws.
     */
    public static @NotNull ConfigurationNode loadJsonResource(@NotNull Class<?> anchor, @NotNull String path) throws ConfigurateException {
        try (InputStream in = anchor.getResourceAsStream("/" + path)) {
            if (in == null) {
                return BasicConfigurationNode.root();
            }
            // the loader is built with a reader supplier, so the stream is consumed inside load()
            return GsonConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                    .build()
                    .load();
        } catch (IOException ex) {
            throw new ConfigurateException(ex);
        }
    }

    /**
     * Fork language file merged with the fork overrides file for the same upstream file.
     */
    public static @NotNull ConfigurationNode loadForkNode(@NotNull Class<?> anchor, @NotNull String upstreamPath) throws ConfigurateException {
        ConfigurationNode node = loadJsonResource(anchor, forkResourcePath(upstreamPath));
        node.mergeFrom(loadJsonResource(anchor, forkOverridesResourcePath(upstreamPath))); // paths are disjoint by convention
        return node;
    }

    /**
     * en-US chain: custom &gt; fork &gt; shaded.
     */
    public static @NotNull LayeredTranslationContainer buildUsContainer(@NotNull ConfigurationNode shadedUs,
                                                                         @NotNull ConfigurationNode forkUs,
                                                                         @NotNull ConfigurationNode customUs) {
        LayeredTranslationContainer c = LayeredTranslationContainer.of(shadedUs);
        c.setNode(forkUs);
        c.setCustomNode(customUs);
        return c;
    }

    /**
     * Selected-locale chain: custom &gt; fork &gt; shaded &gt; us.
     */
    public static @NotNull LayeredTranslationContainer buildLocaleContainer(@NotNull TranslationContainer us,
                                                                             @NotNull ConfigurationNode shadedLocale,
                                                                             @NotNull ConfigurationNode forkLocale,
                                                                             @NotNull ConfigurationNode customLocale) {
        return LayeredTranslationContainer.of(us, shadedLocale, forkLocale, customLocale);
    }

    /**
     * Exact match, else first entry with the same language (in list order), else {@link Locale#US}.
     */
    public static @NotNull Locale resolveLocale(@NotNull Locale requested, @NotNull List<@NotNull Locale> available) {
        if (available.contains(requested)) {
            return requested;
        }
        for (Locale l : available) {
            if (l.getLanguage().equals(requested.getLanguage())) {
                return l;
            }
        }
        return Locale.US;
    }

    /**
     * Locales whose admin files feed the selected container, highest priority first.
     */
    public static @NotNull List<@NotNull Locale> customLocalesFor(@NotNull Locale requested, @NotNull Locale resolved) {
        List<Locale> result = new ArrayList<>();
        if (requested.getLanguage().equals(resolved.getLanguage())) {
            result.add(requested);
        }
        if (!result.contains(resolved)) {
            result.add(resolved);
        }
        return result;
    }

    /**
     * New root containing all nodes merged; earlier nodes win on conflicts. Empty list -> empty root.
     */
    public static @NotNull ConfigurationNode mergeFirstWins(@NotNull List<? extends ConfigurationNode> nodes) {
        BasicConfigurationNode result = BasicConfigurationNode.root();
        for (ConfigurationNode n : nodes) {
            result.mergeFrom(n);
        }
        return result;
    }
}
