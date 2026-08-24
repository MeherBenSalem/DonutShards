package com.nightbeam.donutshards.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

public final class YamlKeyMerger {
    public static final List<String> MANAGED_FILES = List.of(
            "config.yml",
            "database.yml",
            "messages.yml",
            "rewards.yml",
            "zones.yml",
            "shop.yml",
            "gui.yml",
            "anti-abuse.yml",
            "kills.yml"
    );

    private YamlKeyMerger() {
    }

    public static void mergeAll(Plugin plugin) {
        for (var file : MANAGED_FILES) {
            merge(plugin, file);
        }
    }

    public static void merge(Plugin plugin, String filename) {
        var dataFile = new File(plugin.getDataFolder(), filename);
        var defaults = loadDefault(plugin, filename);
        if (defaults == null || defaults.getKeys(false).isEmpty()) {
            return;
        }
        if (!dataFile.exists()) {
            plugin.saveResource(filename, false);
            return;
        }
        var existing = YamlConfiguration.loadConfiguration(dataFile);
        mergeMissing(existing, defaults);
        try {
            existing.save(dataFile);
        } catch (IOException error) {
            plugin.getLogger().log(Level.WARNING, "Could not save merged " + filename, error);
        }
    }

    public static YamlConfiguration loadDefault(Plugin plugin, String filename) {
        try (var stream = plugin.getResource(filename)) {
            if (stream == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException error) {
            plugin.getLogger().log(Level.WARNING, "Could not read default " + filename, error);
            return new YamlConfiguration();
        }
    }

    static void mergeMissing(ConfigurationSection target, ConfigurationSection defaults) {
        for (var key : defaults.getKeys(false)) {
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
            } else if (target.isConfigurationSection(key) && defaults.isConfigurationSection(key)) {
                mergeMissing(target.getConfigurationSection(key), defaults.getConfigurationSection(key));
            }
        }
    }
}
