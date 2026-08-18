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
}
