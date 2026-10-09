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

package org.screamingsandals.bedwars.inventories.upgrade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.screamingsandals.bedwars.game.upgrade.builtin.DragonBuffSpec;
import org.screamingsandals.bedwars.game.upgrade.builtin.EffectUpgradeSpec;
import org.screamingsandals.bedwars.game.upgrade.builtin.ForgeSpec;
import org.screamingsandals.bedwars.game.upgrade.pricing.PriceLadder;
import org.screamingsandals.bedwars.game.upgrade.pricing.PriceSpec;
import org.screamingsandals.bedwars.game.upgrade.trap.TrapQueueConfigParser;
import org.screamingsandals.bedwars.utils.BundledRevision;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the bundled {@code certain-popular-server} variant and its upgrade shop against each other: every shop item
 * must reference an upgrade or trap that the variant defines, with price ladders as long as the upgrade's levels.
 */
class ShippedUpgradeConfigTest {
    private static final String VARIANT = "/variants/certain-popular-server.yml";
    private static final String SHOP = "/shop/certain-popular-server/upgrade-shop.yml";
    /** {@code shop.rows} default of config.yml; the upgrade shop must fit one page. */
    private static final int SHOP_ROWS = 4;
    private static final int SHOP_COLUMNS = 9;

    private static ConfigurationNode variant;
    private static ConfigurationNode shop;
    private static List<String> variantLines;
    private static List<String> shopLines;

    private static String read(String resource) throws IOException {
        try (InputStream stream = ShippedUpgradeConfigTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "missing resource " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @BeforeAll
    static void load() throws Exception {
        var variantText = read(VARIANT);
        var shopText = read(SHOP);
        variantLines = variantText.lines().collect(Collectors.toList());
        shopLines = shopText.lines().collect(Collectors.toList());
        variant = YamlConfigurationLoader.builder().buildAndLoadString(variantText);
        shop = YamlConfigurationLoader.builder().buildAndLoadString(shopText);
    }

    private static List<ConfigurationNode> upgradeItems() {
        return shop.node("data").childrenList().stream()
                .filter(item -> "upgrade".equals(item.node("properties", "name").getString()))
                .collect(Collectors.toList());
    }

    private static List<ConfigurationNode> slotItems() {
        return shop.node("data").childrenList().stream()
                .filter(item -> "trap-queue-slot".equals(item.node("properties", "name").getString()))
                .collect(Collectors.toList());
    }

    /** Maximum level of a variant upgrade, the way the runtime definitions compute it. */
    private static int maxLevelOf(String upgradeName) throws Exception {
        var node = variant.node("upgrades", upgradeName);
        assertFalse(node.virtual(), "the shop references an upgrade the variant does not define: " + upgradeName);
        var warnings = new ArrayList<String>();
        var type = node.node("type").getString("");
        switch (type) {
            case "enchantment":
                return node.node("max-level").getInt(1);
            case "effect":
                return EffectUpgradeSpec.parse(node, warnings).maxLevel();
            case "forge":
                return ForgeSpec.parse(node, warnings).maxLevel();
            case "dragon-buff":
                return DragonBuffSpec.parse(node, warnings).maxLevel();
            case "trap":
            case "area-effect":
                return 1;
            default:
                throw new AssertionError("unexpected upgrade type '" + type + "' of " + upgradeName);
        }
    }

    @Test
    void bothFilesCarryAForkRevision() {
        assertTrue(BundledRevision.parse(variantLines) >= 1, "variant has no '# fork-revision' marker");
        assertTrue(BundledRevision.parse(shopLines) >= 1, "upgrade shop has no '# fork-revision' marker");
    }

    @Test
    void trapQueueSectionParsesWithoutWarnings() {
        var result = TrapQueueConfigParser.parse(variant.node("trap-queue"));

        assertTrue(result.warnings().isEmpty(), result.warnings()::toString);
        assertTrue(result.config().settings().enabled());
        assertEquals(3, result.config().settings().maxSize());
        assertEquals("diamond", result.config().settings().currency());
        assertEquals(List.of(1, 2, 4), result.config().settings().costs());
        assertEquals(Set.of("its-a-trap", "counter-offensive", "alarm", "miner-fatigue"), result.config().traps().keySet());
    }

    @Test
    void builtInUpgradeSectionsParseWithoutWarnings() throws Exception {
        var warnings = new ArrayList<String>();

        var forge = ForgeSpec.parse(variant.node("upgrades", "forge"), warnings);
        var haste = EffectUpgradeSpec.parse(variant.node("upgrades", "haste"), warnings);
        var dragon = DragonBuffSpec.parse(variant.node("upgrades", "dragon-buff"), warnings);

        assertTrue(warnings.isEmpty(), warnings::toString);
        assertEquals(4, forge.maxLevel());
        assertEquals(2, haste.maxLevel());
        assertEquals(1, dragon.maxLevel());
    }

    @Test
    void upgradeShopItemsHaveAFallbackPrice() {
        var items = upgradeItems();
        assertFalse(items.isEmpty());
        for (var item : items) {
            var name = item.node("stack", "display-name").getString("?");
            assertTrue(PriceSpec.parse(item.node("price"), null).isPresent(), "item '" + name + "' needs a parsable price");
        }
    }

    @Test
    void upgradeShopEntitiesReferenceDefinedUpgradesAndTraps() {
        var traps = variant.node("trap-queue", "traps");
        for (var item : upgradeItems()) {
            var entities = item.node("properties", "entities").childrenList();
            assertFalse(entities.isEmpty(), "an upgrade item without entities");
            for (var entity : entities) {
                var type = entity.node("type").getString("");
                if ("team".equals(type)) {
                    var upgrade = entity.node("upgrade-name").getString();
                    assertNotNull(upgrade, "team entity without upgrade-name");
                    assertFalse(variant.node("upgrades", upgrade).virtual(), "unknown upgrade " + upgrade);
                } else if ("trap-queue".equals(type)) {
                    var trap = entity.node("trap").getString();
                    assertNotNull(trap, "trap-queue entity without trap");
                    assertFalse(traps.node(trap).virtual(), "unknown trap " + trap);
                } else {
                    throw new AssertionError("unexpected entity type '" + type + "'");
                }
            }
        }
    }

    @Test
    void priceLaddersParseAndMatchTheUpgradeLevels() throws Exception {
        for (var item : upgradeItems()) {
            var properties = item.node("properties");
            var warnings = new ArrayList<String>();
            var ladder = PriceLadder.parse(properties, "diamond", warnings);
            assertTrue(warnings.isEmpty(), warnings::toString);

            for (var entity : properties.node("entities").childrenList()) {
                if (!"team".equals(entity.node("type").getString())) {
                    continue;
                }
                if (ladder.isEmpty()) {
                    continue;
                }
                var upgrade = entity.node("upgrade-name").getString();
                int maxLevel = maxLevelOf(upgrade);
                assertEquals(maxLevel, ladder.base().size(), "price ladder of " + upgrade);
                for (var sizeEntry : properties.node("prices-by-team-size").childrenMap().entrySet()) {
                    int size = Integer.parseInt(String.valueOf(sizeEntry.getKey()));
                    assertEquals(maxLevel, ladder.forTeamSize(size).size(), "price ladder of " + upgrade + " for teams of " + size);
                }
            }
        }
    }

    @Test
    void trapQueueSlotItemsAreNotBuyableAndCoverTheQueue() {
        var slots = new HashSet<Integer>();
        for (var item : slotItems()) {
            assertTrue(item.node("price").virtual(), "a queue slot must not be priced (it would become clickable)");
            slots.add(item.node("properties", "slot").getInt(-1));
        }
        int maxSize = variant.node("trap-queue", "max-size").getInt(3);
        var expected = new HashSet<Integer>();
        for (int slot = 1; slot <= maxSize; slot++) {
            expected.add(slot);
        }
        assertEquals(expected, slots);
    }

    @Test
    void itemsFitTheShopPageWithoutOverlapping() {
        var positions = new HashSet<String>();
        for (var item : shop.node("data").childrenList()) {
            int row = item.node("row").getInt(-1);
            int column = item.node("column").getInt(-1);
            assertTrue(row >= 1 && row <= SHOP_ROWS, "row " + row + " is outside the shop");
            assertTrue(column >= 0 && column < SHOP_COLUMNS, "column " + column + " is outside the shop");
            assertTrue(positions.add(row + ":" + column), "two items share row " + row + ", column " + column);
        }
    }

    @Test
    void everyBuyableTrapItemHasAPriceAsFallback() {
        for (var item : upgradeItems()) {
            for (var entity : item.node("properties", "entities").childrenList()) {
                if ("trap-queue".equals(entity.node("type").getString())) {
                    assertTrue(PriceSpec.parse(item.node("price"), null).isPresent(),
                            "trap item " + entity.node("trap").getString() + " needs a price (SimpleInventories only fires trades for priced items)");
                }
            }
        }
    }
}
