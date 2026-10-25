package com.nightbeam.donutshards.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;

public final class GuiConfig {
    private volatile int confirmSlot = 11;
    private volatile int cancelSlot = 15;
    private volatile Material confirmMaterial = Material.LIME_CONCRETE;
    private volatile Material cancelMaterial = Material.RED_CONCRETE;
    private volatile String confirmName = "<green>Confirm purchase";
    private volatile String cancelName = "<red>Cancel";
    private volatile Button previous = new Button(45, Material.ARROW, "<yellow>Previous page");
    private volatile Button next = new Button(53, Material.ARROW, "<yellow>Next page");
    private volatile Button filler = new Button(-1, Material.GRAY_STAINED_GLASS_PANE, " ");

    /** A leaderboard navigation button or the filler pane (slot unused). */
    public record Button(int slot, Material material, String name) {
    }

    public void load(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        applyButton(yaml.getConfigurationSection("confirm"), true);
        applyButton(yaml.getConfigurationSection("cancel"), false);
        previous = button(yaml.getConfigurationSection("previous"), previous);
        next = button(yaml.getConfigurationSection("next"), next);
        filler = button(yaml.getConfigurationSection("filler"), filler);
    }

    /** Leaderboard buttons live in the bottom row (45-53); other slots keep the previous value. */
    static Button button(ConfigurationSection section, Button fallback) {
        if (section == null) {
            return fallback;
        }
        var slot = section.getInt("slot", fallback.slot());
        if (fallback.slot() >= 0 && (slot < 45 || slot > 53)) {
            slot = fallback.slot();
        }
        var materialName = section.getString("material");
        var material = materialName == null ? null : Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
        if (material == null || material.isAir()) {
            material = fallback.material();
        }
        return new Button(slot, material, section.getString("name", fallback.name()));
    }

    public Button previous() {
        return previous;
    }

    public Button next() {
        return next;
    }

    public Button filler() {
        return filler;
    }

    private void applyButton(ConfigurationSection section, boolean confirm) {
        if (section == null) {
            return;
        }
        if (section.contains("slot")) {
            var slot = Math.max(0, section.getInt("slot"));
            if (confirm) {
                confirmSlot = slot;
            } else {
                cancelSlot = slot;
            }
        }
        var materialName = section.getString("material");
        if (materialName != null) {
            var material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
            if (material != null && !material.isAir()) {
                if (confirm) {
                    confirmMaterial = material;
                } else {
                    cancelMaterial = material;
                }
            }
        }
        var name = section.getString("name");
        if (name != null) {
            if (confirm) {
                confirmName = name;
            } else {
                cancelName = name;
            }
        }
    }

    public int confirmSlot() {
        return confirmSlot;
    }

    public int cancelSlot() {
        return cancelSlot;
    }

    public Material confirmMaterial() {
        return confirmMaterial;
    }

    public Material cancelMaterial() {
        return cancelMaterial;
    }

    public String confirmName() {
        return confirmName;
    }

    public String cancelName() {
        return cancelName;
    }
}
