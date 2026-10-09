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

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForkLanguageLayeringTest {
    private static final Locale EN_US = Locale.forLanguageTag("en-US");
    private static final Locale DE_DE = Locale.forLanguageTag("de-DE");

    private static LayeredTranslationContainer usContainer() throws Exception {
        ConfigurationNode shadedUs = BasicConfigurationNode.root(n -> {
            n.node("a").set("shaded-a");
            n.node("b").set("shaded-b");
            n.node("list").setList(String.class, List.of("s1", "s2"));
        });
        ConfigurationNode forkUs = BasicConfigurationNode.root(n -> {
            n.node("fork", "x").set("fork-x");
            n.node("b").set("fork-b");
        });
        ConfigurationNode customUs = BasicConfigurationNode.root(n -> {
            n.node("fork", "x").set("custom-x");
            n.node("a").set("custom-a");
        });
        return ForkLanguageLayering.buildUsContainer(shadedUs, forkUs, customUs);
    }

    @Test
    void resourcePaths() {
        assertEquals("bedwars-fork/languages/language_en-US.json",
                ForkLanguageLayering.forkResourcePath("languages/language_en-US.json"));
        assertEquals("bedwars-fork/languages/overrides_en-US.json",
                ForkLanguageLayering.forkOverridesResourcePath("languages/language_en-US.json"));
        assertEquals("bedwars-fork/overrides_de-DE.json",
                ForkLanguageLayering.forkOverridesResourcePath("language_de-DE.json"));
    }

    @Test
    void usContainerPrecedenceIsCustomThenForkThenShaded() throws Exception {
        var us = usContainer();
        assertEquals(List.of("custom-a"), us.translate("a"));
        assertEquals(List.of("fork-b"), us.translate("b"));
        assertEquals(List.of("custom-x"), us.translate("fork", "x"));
        assertEquals(List.of("s1", "s2"), us.translate("list"));
        assertEquals(List.of(), us.translate("missing"));
    }

    @Test
    void localeContainerPrecedence() throws Exception {
        var us = usContainer();
        ConfigurationNode shadedDe = BasicConfigurationNode.root(n -> n.node("a").set("de-a"));
        ConfigurationNode forkDe = BasicConfigurationNode.root(n -> n.node("fork", "y").set("fork-de-y"));
        ConfigurationNode customDe = BasicConfigurationNode.root(n -> n.node("b").set("custom-de-b"));
        var de = ForkLanguageLayering.buildLocaleContainer(us, shadedDe, forkDe, customDe);

        // a real translation beats the English admin override
        assertEquals(List.of("de-a"), de.translate("a"));
        assertEquals(List.of("custom-de-b"), de.translate("b"));
        assertEquals(List.of("custom-x"), de.translate("fork", "x"));
        assertEquals(List.of("fork-de-y"), de.translate("fork", "y"));
        assertEquals(List.of("s1", "s2"), de.translate("list"));
    }

    @Test
    void resolveLocale() {
        var available = List.of(DE_DE, EN_US);
        assertEquals(EN_US, ForkLanguageLayering.resolveLocale(Locale.forLanguageTag("en"), available));
        assertEquals(EN_US, ForkLanguageLayering.resolveLocale(EN_US, available));
        assertEquals(DE_DE, ForkLanguageLayering.resolveLocale(Locale.forLanguageTag("de-AT"), available));
        assertEquals(EN_US, ForkLanguageLayering.resolveLocale(Locale.forLanguageTag("xx-YY"), available));

        var zhHans = Locale.forLanguageTag("zh-Hans");
        var zhHant = Locale.forLanguageTag("zh-Hant");
        assertEquals(zhHans, ForkLanguageLayering.resolveLocale(Locale.forLanguageTag("zh-CN"), List.of(zhHans, zhHant)));
    }

    @Test
    void customLocalesFor() {
        var en = Locale.forLanguageTag("en");
        var de = Locale.forLanguageTag("de");
        assertEquals(List.of(en, EN_US), ForkLanguageLayering.customLocalesFor(en, EN_US));
        assertEquals(List.of(EN_US), ForkLanguageLayering.customLocalesFor(EN_US, EN_US));
        assertEquals(List.of(de, DE_DE), ForkLanguageLayering.customLocalesFor(de, DE_DE));
        assertEquals(List.of(DE_DE), ForkLanguageLayering.customLocalesFor(DE_DE, DE_DE));
        // German is not available -> fallback to en-US
        assertEquals(List.of(EN_US), ForkLanguageLayering.customLocalesFor(de, EN_US));
    }

    @Test
    void mergeFirstWinsKeepsEarlierValuesAndMergesMaps() throws Exception {
        ConfigurationNode first = BasicConfigurationNode.root(n -> {
            n.node("a").set("1");
            n.node("n", "x").set("1");
        });
        ConfigurationNode second = BasicConfigurationNode.root(n -> {
            n.node("a").set("2");
            n.node("b").set("2");
            n.node("n", "y").set("2");
        });
        var merged = ForkLanguageLayering.mergeFirstWins(List.of(first, second));
        assertEquals("1", merged.node("a").getString());
        assertEquals("2", merged.node("b").getString());
        assertEquals("1", merged.node("n", "x").getString());
        assertEquals("2", merged.node("n", "y").getString());
    }

    @Test
    void mergeFirstWinsOfNothingIsEmpty() {
        assertTrue(ForkLanguageLayering.mergeFirstWins(List.of()).empty());
    }

    @Test
    void mergeFirstWinsDoesNotMutateItsInputs() throws Exception {
        ConfigurationNode first = BasicConfigurationNode.root(n -> n.node("a").set("1"));
        ConfigurationNode second = BasicConfigurationNode.root(n -> n.node("b").set("2"));
        ForkLanguageLayering.mergeFirstWins(List.of(first, second));
        assertTrue(first.node("b").virtual());
        assertTrue(second.node("a").virtual());
    }

    @Test
    void loadJsonResourceOfMissingFileIsEmpty() throws Exception {
        var node = ForkLanguageLayering.loadJsonResource(getClass(), "does/not/exist.json");
        assertTrue(node.empty());
    }
}
