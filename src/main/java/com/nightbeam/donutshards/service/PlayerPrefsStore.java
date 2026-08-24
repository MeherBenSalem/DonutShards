package com.nightbeam.donutshards.service;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerPrefsStore {
    private final File file;
    private final Logger logger;
    private final ConcurrentHashMap<UUID, Boolean> shopConfirmation = new ConcurrentHashMap<>();

    public PlayerPrefsStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "player-prefs.yml");
        this.logger = logger;
        load();
    }

    public boolean shopConfirmationEnabled(UUID player) {
        return shopConfirmation.getOrDefault(player, true);
    }

    public void setShopConfirmation(UUID player, boolean enabled) {
        shopConfirmation.put(player, enabled);
        save();
    }

    public void load() {
        shopConfirmation.clear();
        if (!file.exists()) {
            return;
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("shop-confirmation");
        if (section == null) {
            return;
        }
        for (var key : section.getKeys(false)) {
            try {
                shopConfirmation.put(UUID.fromString(key), section.getBoolean(key, true));
            } catch (IllegalArgumentException ignored) {
                // Skip invalid UUID keys.
            }
        }
    }

    private void save() {
        var yaml = new YamlConfiguration();
        var section = yaml.createSection("shop-confirmation");
        for (var entry : shopConfirmation.entrySet()) {
            section.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException error) {
            logger.log(Level.WARNING, "Could not save player-prefs.yml", error);
        }
    }
}
