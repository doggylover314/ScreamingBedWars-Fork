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

package org.screamingsandals.bedwars.lang;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.bedwars.config.MainConfig;
import org.screamingsandals.lib.lang.Lang;
import org.screamingsandals.lib.lang.LangService;
import org.screamingsandals.lib.spectator.Component;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.ServiceDependencies;
import org.screamingsandals.lib.utils.annotations.methods.OnEnable;
import org.screamingsandals.lib.utils.annotations.parameters.DataFolder;
import org.screamingsandals.lib.utils.logger.Logger;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@ServiceDependencies(dependsOn = {
        MainConfig.class
})
@RequiredArgsConstructor
public class BedWarsLangService extends LangService {
    public static final Pattern LANGUAGE_PATTERN = Pattern.compile("[a-z]{2}-[A-Z]{2}");
    public static final String MESSAGE_PLACEHOLDER_NAME = "bw-lang";

    private final MainConfig mainConfig;
    private final Logger logger;
    @DataFolder("languages")
    private final Path languagesFolder;
    @Getter
    private LanguageDefinition internalLanguageDefinition;

    {
        Lang.initDefault(this);
    }

    @SneakyThrows
    @OnEnable
    public void onEnable() {
        Locale locale;
        try {
            locale = Locale.forLanguageTag(mainConfig.node("locale").getString("en_US").replace("_", "-"));
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid locale specified in config, falling back to en_US!", ex);
            locale = Locale.US;
        }
        final var requestedLocale = locale;
        var prefix = mainConfig.node("prefix").getString("[BW]");

        Lang.setDefaultPrefix(Component.fromLegacy(prefix));

        internalLanguageDefinition = null;
        try {
            var definitionNode = ForkLanguageLayering.loadJsonResource(BedWarsLangService.class, "language_definition.json");
            if (!definitionNode.empty()) {
                internalLanguageDefinition = definitionNode.get(LanguageDefinition.class);
            }
        } catch (ConfigurateException ex) {
            logger.error("Can't load default language definition!", ex);
        }

        if (internalLanguageDefinition == null) {
            logger.error("Can't load default language definition!");
            // keep the fork keys working even without the upstream artifact
            fallbackContainer = ForkLanguageLayering.buildUsContainer(
                    BasicConfigurationNode.root(),
                    loadForkNodeSafe(ForkLanguageLayering.DEFAULT_US_RESOURCE),
                    BasicConfigurationNode.root()
            );
            return;
        }

        var languages = internalLanguageDefinition
                .getLanguages()
                .entrySet()
                .stream()
                .map(entry -> {
                    try {
                        return Map.entry(Locale.forLanguageTag(entry.getKey()), entry.getValue());
                    } catch (IllegalArgumentException ex) {
                        logger.error("Invalid language definition: {}", ex.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                // deterministic pick of "the first entry with the same language"
                .sorted(Comparator.comparing((Map.Entry<Locale, String> e) -> e.getKey().toLanguageTag()))
                .collect(Collectors.toList());

        var usPath = languages
                .stream()
                .filter(entry -> entry.getKey().equals(Locale.US))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        var shadedUs = usPath != null ? loadShadedSafe(usPath) : BasicConfigurationNode.root();
        if (shadedUs.empty()) {
            logger.warn("Language definitions don't contain en_US file!");
        }
        var us = ForkLanguageLayering.buildUsContainer(
                shadedUs,
                loadForkNodeSafe(usPath != null ? usPath : ForkLanguageLayering.DEFAULT_US_RESOURCE),
                BasicConfigurationNode.root()
        );

        var resolvedLocale = ForkLanguageLayering.resolveLocale(
                requestedLocale,
                languages.stream().map(Map.Entry::getKey).collect(Collectors.toList())
        );
        LayeredTranslationContainer main = us;
        if (!resolvedLocale.equals(Locale.US)) {
            var path = languages
                    .stream()
                    .filter(entry -> entry.getKey().equals(resolvedLocale))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow();
            var shadedLocale = loadShadedSafe(path);
            if (shadedLocale.empty()) {
                logger.error("Can't load language file {}, falling back to en_US", path);
            } else {
                main = ForkLanguageLayering.buildLocaleContainer(us, shadedLocale, loadForkNodeSafe(path), BasicConfigurationNode.root());
            }
        }
        fallbackContainer = main;

        if (!Files.exists(languagesFolder)) {
            Files.createDirectory(languagesFolder);
            return;
        }

        var customByLocale = new HashMap<Locale, List<ConfigurationNode>>();
        try (var stream = Files.walk(languagesFolder.toAbsolutePath())) {
            stream.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing((Path file) -> file.getFileName().toString()))
                    .forEach(file -> {
                        var name = file.getFileName().toString();
                        var matcher = LANGUAGE_PATTERN.matcher(name);
                        if (!matcher.find()) {
                            return;
                        }
                        try {
                            var fileLocale = Locale.forLanguageTag(matcher.group());
                            var node = GsonConfigurationLoader.builder().path(file).build().load();
                            customByLocale.computeIfAbsent(fileLocale, k -> new ArrayList<>()).add(node);
                        } catch (IllegalArgumentException | ConfigurateException ex) {
                            logger.warn("Invalid language file in languages directory: " + name, ex);
                        }
                    });
        } catch (IOException e) {
            logger.error("Could not read the languages folder", e);
        }

        var effectiveResolved = main == us ? Locale.US : resolvedLocale;
        main.setCustomNode(mergeCustom(ForkLanguageLayering.customLocalesFor(requestedLocale, effectiveResolved), customByLocale));
        if (main != us) {
            us.setCustomNode(mergeCustom(List.of(Locale.US), customByLocale));
        }
    }

    private @NotNull ConfigurationNode mergeCustom(@NotNull List<Locale> order, @NotNull Map<Locale, List<ConfigurationNode>> byLocale) {
        var nodes = new ArrayList<ConfigurationNode>();
        for (var l : order) {
            nodes.addAll(byLocale.getOrDefault(l, List.of()));
        }
        return ForkLanguageLayering.mergeFirstWins(nodes);
    }

    private @NotNull ConfigurationNode loadShadedSafe(@NotNull String path) {
        try {
            return ForkLanguageLayering.loadJsonResource(BedWarsLangService.class, path);
        } catch (ConfigurateException ex) {
            logger.error("Can't load language file {}", path, ex);
            return BasicConfigurationNode.root();
        }
    }

    private @NotNull ConfigurationNode loadForkNodeSafe(@NotNull String upstreamPath) {
        try {
            return ForkLanguageLayering.loadForkNode(BedWarsLangService.class, upstreamPath);
        } catch (ConfigurateException ex) {
            logger.error("Fork language overlay for {} is broken!", upstreamPath, ex);
            return BasicConfigurationNode.root();
        }
    }

    @Override
    @Nullable
    public String getMessagePlaceholderName() {
        return MESSAGE_PLACEHOLDER_NAME;
    }
}
