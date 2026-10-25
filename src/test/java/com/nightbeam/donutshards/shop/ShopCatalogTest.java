package com.nightbeam.donutshards.shop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ShopCatalogTest {
    @Test
    void loadsItemsAndEnchantments(@TempDir Path dir) throws Exception {
        var file = dir.resolve("shop.yml");
        Files.writeString(file, """
                schema-version: 1
                title: "Shop"
                size: 27
                categories:
                  gear:
                    slot: 4
                    icon:
                      material: CHEST
                      name: "Gear"
                    items:
                      blade:
                        slot: 13
                        price: 250
                        material: DIAMOND_SWORD
                        amount: 1
                        name: "Blade"
                        lore:
                          - "sharp"
                        enchantments:
                          sharpness: 5
                          unbreaking: 3
                """);
        var catalog = new ShopCatalog();
        catalog.load(file.toFile());
        assertThat(catalog.size()).isEqualTo(27);
        assertThat(catalog.byId("blade")).isPresent();
        var blade = catalog.byId("blade").orElseThrow();
        assertThat(blade.price()).isEqualTo(250);
        assertThat(blade.enchantments()).containsEntry("sharpness", 5).containsEntry("unbreaking", 3);
        assertThat(blade.purchasable()).isTrue();
        assertThat(catalog.bySlot(4)).isEmpty();
    }

    @Test
    void loadsConfirmationAndCommands(@TempDir Path dir) throws Exception {
        var file = dir.resolve("shop.yml");
        Files.writeString(file, """
                categories:
                  treats:
                    items:
                      cookie:
                        slot: 10
                        price: 25
                        material: COOKIE
                        confirmation: true
                        commands:
                          - "say %player% bought %item% for %price%"
                """);
        var catalog = new ShopCatalog();
        catalog.load(file.toFile());
        var cookie = catalog.byId("cookie").orElseThrow();
        assertThat(cookie.confirmation()).isTrue();
        assertThat(cookie.commands()).containsExactly("say %player% bought %item% for %price%");
    }

    @Test
    void skipsItemsWithoutMaterial(@TempDir Path dir) throws Exception {
        var file = dir.resolve("shop.yml");
        Files.writeString(file, """
                categories:
                  a:
                    items:
                      bad:
                        slot: 0
                        price: 1
                """);
        var catalog = new ShopCatalog();
        catalog.load(file.toFile());
        assertThat(catalog.items()).isEmpty();
    }

    @Test
    void loadsDailyDeals(@TempDir Path dir) throws Exception {
        var file = dir.resolve("shop.yml");
        Files.writeString(file, """
                size: 54
                deals:
                  rotation-hours: 12
                  count: 5
                  slots: [38, 40, 40, 99]
                  limit-per-player: 2
                  pool:
                    apples: {price: 10, material: APPLE}
                    free: {price: 0, material: STONE}
                    rockets: {price: 5, material: FIREWORK_ROCKET, amount: 8}
                """);
        var catalog = new ShopCatalog();
        catalog.load(file.toFile());
        var deals = catalog.deals();
        assertThat(deals.active()).isTrue();
        assertThat(deals.slots()).containsExactly(38, 40);
        assertThat(deals.count()).isEqualTo(2);
        assertThat(deals.rotationHours()).isEqualTo(12);
        assertThat(deals.limitPerPlayer()).isEqualTo(2);
        assertThat(deals.pool()).extracting(ShopItem::id).containsExactly("apples", "rockets");
        assertThat(catalog.byId("apples")).isEmpty();
    }

    @Test
    void noDealsSectionMeansNoDeals(@TempDir Path dir) throws Exception {
        var file = dir.resolve("shop.yml");
        Files.writeString(file, "size: 27\n");
        var catalog = new ShopCatalog();
        catalog.load(file.toFile());
        assertThat(catalog.deals().active()).isFalse();
    }
}
