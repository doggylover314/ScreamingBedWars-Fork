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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForkLanguageOverlayTest {
    private static final String OVERLAY = "bedwars-fork/languages/language_en-US.json";
    private static final String OVERRIDES = "bedwars-fork/languages/overrides_en-US.json";
    private static final String UPSTREAM = "languages/language_en-US.json";
    private static final Pattern SEGMENT = Pattern.compile("[a-z0-9_]+");
    private static final Pattern LEGACY_CODE = Pattern.compile("&[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAPI_PLACEHOLDER = Pattern.compile("%[a-z_]+%");

    private static ConfigurationNode overlay;
    private static ConfigurationNode overrides;
    private static ConfigurationNode upstream;

    @BeforeAll
    static void load() throws Exception {
        overlay = ForkLanguageLayering.loadJsonResource(ForkLanguageOverlayTest.class, OVERLAY);
        overrides = ForkLanguageLayering.loadJsonResource(ForkLanguageOverlayTest.class, OVERRIDES);
        upstream = ForkLanguageLayering.loadJsonResource(ForkLanguageOverlayTest.class, UPSTREAM);
    }

    private static boolean isScalarString(ConfigurationNode node) {
        return !node.isMap() && !node.isList() && node.rawScalar() instanceof String;
    }

    private static boolean isTranslationLeaf(ConfigurationNode node) {
        if (isScalarString(node)) {
            return true;
        }
        return node.isList() && node.childrenList().stream().allMatch(ForkLanguageOverlayTest::isScalarString);
    }

    private static void collectLeaves(ConfigurationNode node, String path, Set<String> out) {
        if (node.isMap()) {
            node.childrenMap().forEach((k, v) -> collectLeaves(v, path.isEmpty() ? String.valueOf(k) : path + "." + k, out));
        } else {
            out.add(path);
        }
    }

    private static void collectStrings(ConfigurationNode node, List<String> out) {
        if (node.isMap()) {
            node.childrenMap().values().forEach(v -> collectStrings(v, out));
        } else if (node.isList()) {
            node.childrenList().forEach(v -> collectStrings(v, out));
        } else if (node.rawScalar() instanceof String) {
            out.add((String) node.rawScalar());
        }
    }

    private static List<Field> keyFields() {
        List<Field> fields = new ArrayList<>();
        for (Field f : ForkLangKeys.class.getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())
                    && Modifier.isFinal(f.getModifiers()) && f.getType() == String[].class) {
                fields.add(f);
            }
        }
        return fields;
    }

    private static String[] keyOf(Field f) throws IllegalAccessException {
        return (String[]) f.get(null);
    }

    @Test
    void overlayLoadsAndHasOnlyTheForkRoot() {
        assertFalse(overlay.empty(), "overlay resource " + OVERLAY + " is missing or empty");
        assertEquals(Set.of("fork"), new HashSet<>(overlay.childrenMap().keySet().stream().map(String::valueOf).toList()));
    }

    @Test
    void everyConstantIsWellFormedAndPresentInTheOverlay() throws Exception {
        var fields = keyFields();
        assertFalse(fields.isEmpty(), "ForkLangKeys has no constants");
        for (Field f : fields) {
            String[] key = keyOf(f);
            assertEquals("fork", key[0], f.getName() + " must start with the fork root");
            for (String segment : key) {
                assertTrue(SEGMENT.matcher(segment).matches(), f.getName() + ": bad segment '" + segment + "'");
            }
            String expectedName = String.join("_", Arrays.copyOfRange(key, 1, key.length)).toUpperCase(Locale.ROOT);
            assertEquals(expectedName, f.getName(), "constant name does not match its path");

            ConfigurationNode node = overlay.node((Object[]) key);
            assertFalse(node.virtual(), f.getName() + " is missing in the overlay: " + String.join(".", key));
            assertTrue(isTranslationLeaf(node), f.getName() + " is not a string or a list of strings");
        }
    }

    @Test
    void everyOverlayLeafHasExactlyOneConstant() throws Exception {
        Set<String> leaves = new TreeSet<>();
        collectLeaves(overlay, "", leaves);

        Set<String> constants = new TreeSet<>();
        for (Field f : keyFields()) {
            String path = String.join(".", keyOf(f));
            assertTrue(constants.add(path), "two constants share the path " + path);
        }
        assertEquals(leaves, constants);
    }

    @Test
    void overlayTextsUseMiniMessageOnly() {
        List<String> texts = new ArrayList<>();
        collectStrings(overlay, texts);
        assertFalse(texts.isEmpty());
        for (String text : texts) {
            assertFalse(text.contains("§"), "section sign in: " + text);
            assertFalse(LEGACY_CODE.matcher(text).find(), "legacy color code in: " + text);
            assertFalse(PAPI_PLACEHOLDER.matcher(text).find(), "%placeholder% in: " + text);
        }
    }

    @Test
    void overridesOnlyCorrectExistingUpstreamTexts() {
        assertFalse(upstream.empty(), "upstream en-US language file is missing on the test classpath");
        assertFalse(overrides.empty(), "overrides resource " + OVERRIDES + " is missing or empty");
        assertTrue(overrides.node("fork").virtual(), "overrides must not touch the fork namespace");

        Set<String> leaves = new TreeSet<>();
        collectLeaves(overrides, "", leaves);
        assertFalse(leaves.isEmpty());
        for (String path : leaves) {
            assertFalse(path.startsWith("fork"), "override under the fork namespace: " + path);
            assertFalse(upstream.node((Object[]) path.split("\\.")).virtual(), "override of a key that does not exist upstream: " + path);
        }
    }

    @Test
    void layeredEnglishChainResolvesForkKeysAndOverrides() throws Exception {
        var forkUs = ForkLanguageLayering.loadForkNode(ForkLanguageOverlayTest.class, ForkLanguageLayering.DEFAULT_US_RESOURCE);
        var us = ForkLanguageLayering.buildUsContainer(upstream, forkUs, BasicConfigurationNode.root());

        // a fork key resolves from the overlay
        assertFalse(us.translate(ForkLangKeys.COMMON_FEATURE_DISABLED).isEmpty());
        // the corrected trap text wins over the upstream one that used %team%
        var blind = us.translate("in_game", "trap", "blind_trap_triggered", "message");
        assertEquals(1, blind.size());
        assertTrue(blind.get(0).contains("<team>"), blind.get(0));
        assertFalse(blind.get(0).contains("%team%"), blind.get(0));
        // untouched upstream texts still come from the upstream file
        assertFalse(us.translate("in_game", "trap", "miner_trap_triggered", "message").isEmpty());
    }

    @Test
    void upstreamHasNoForkRoot() {
        assertFalse(upstream.empty(), "upstream en-US language file is missing on the test classpath");
        assertTrue(upstream.node("fork").virtual(), "upstream now defines a 'fork' root, the overlay namespace must change");
    }
}
