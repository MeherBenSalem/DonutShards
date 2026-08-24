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

    public void load(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        applyButton(yaml.getConfigurationSection("confirm"), true);
        applyButton(yaml.getConfigurationSection("cancel"), false);
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
