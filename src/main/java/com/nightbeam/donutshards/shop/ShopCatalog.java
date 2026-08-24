package com.nightbeam.donutshards.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ShopCatalog {
    private volatile String title = "Shop";
    private volatile int size = 27;
    private volatile List<ShopItem> items = List.of();

    public void load(File file) {
        if (file == null || !file.exists()) {
            title = "Shop";
            size = 27;
            items = List.of();
            return;
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        title = yaml.getString("title", "Shop");
        var rawSize = yaml.getInt("size", 27);
        size = Math.max(9, Math.min(54, (rawSize / 9) * 9));
        var parsed = new ArrayList<ShopItem>();
        var categories = yaml.getConfigurationSection("categories");
        if (categories != null) {
            for (var categoryKey : categories.getKeys(false)) {
                var category = categories.getConfigurationSection(categoryKey);
                if (category == null) {
                    continue;
                }
                var icon = category.getConfigurationSection("icon");
                if (icon != null) {
                    parseItem(categoryKey + "-icon", category.getInt("slot", 0), icon, false).ifPresent(parsed::add);
                }
                var itemsSection = category.getConfigurationSection("items");
                if (itemsSection != null) {
                    for (var itemKey : itemsSection.getKeys(false)) {
                        var item = itemsSection.getConfigurationSection(itemKey);
                        if (item != null) {
                            parseItem(itemKey, item.getInt("slot", 0), item, true).ifPresent(parsed::add);
                        }
                    }
                }
            }
        }
        items = List.copyOf(parsed);
    }

    public String title() {
        return title;
    }

    public int size() {
        return size;
    }

    public List<ShopItem> items() {
        return items;
    }

    public Optional<ShopItem> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return items.stream().filter(item -> item.id().equals(id) && item.purchasable()).findFirst();
    }

    public Optional<ShopItem> bySlot(int slot) {
        return items.stream().filter(item -> item.slot() == slot && item.purchasable()).findFirst();
    }

    static Optional<ShopItem> parseItem(String id, int slot, ConfigurationSection section, boolean purchasable) {
        var material = section.getString("material");
        if (material == null || material.isBlank()) {
            return Optional.empty();
        }
        var enchantments = new LinkedHashMap<String, Integer>();
        var enchantSection = section.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (var key : enchantSection.getKeys(false)) {
                enchantments.put(key.toLowerCase(Locale.ROOT), Math.max(1, enchantSection.getInt(key, 1)));
            }
        } else if (section.isList("enchantments")) {
            for (var name : section.getStringList("enchantments")) {
                enchantments.put(name.toLowerCase(Locale.ROOT), 1);
            }
        }
        var lore = section.getStringList("lore");
        return Optional.of(new ShopItem(
                id,
                Math.max(0, slot),
                Math.max(0, section.getLong("price", 0)),
                material.toUpperCase(Locale.ROOT),
                Math.max(1, section.getInt("amount", 1)),
                section.getString("name", id),
                List.copyOf(lore),
                Map.copyOf(enchantments),
                purchasable && section.getLong("price", 0) > 0,
                section.getBoolean("confirmation", false),
                List.copyOf(section.getStringList("commands"))
        ));
    }
}
