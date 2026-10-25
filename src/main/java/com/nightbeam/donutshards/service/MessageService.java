package com.nightbeam.donutshards.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MessageService {
    private final MiniMessage mini = MiniMessage.miniMessage();
    private volatile Map<String, String> templates = Map.of();
    private volatile String prefix = "";

    public void load(File file) {
        var loaded = new LinkedHashMap<String, String>();
        var nextPrefix = "";
        if (file != null && file.exists()) {
            var yaml = YamlConfiguration.loadConfiguration(file);
            nextPrefix = yaml.getString("prefix", "");
            for (var key : yaml.getKeys(false)) {
                if ("schema-version".equals(key) || "prefix".equals(key)) {
                    continue;
                }
                var value = yaml.getString(key);
                if (value != null) {
                    loaded.put(key, value);
                }
            }
        }
        this.prefix = nextPrefix == null ? "" : nextPrefix;
        this.templates = Map.copyOf(loaded);
    }

    public String raw(String key) {
        return templates.getOrDefault(key, "<red>Missing message: " + key + "</red>");
    }

    /**
     * Placeholders whose values are MiniMessage written by the server's staff (zone display names, booster reasons)
     * and are rendered; every other value (player names, typed text) is escaped.
     */
    public static final java.util.Set<String> STAFF_FORMATTED = java.util.Set.of("zone", "reason");

    public Component render(String input, Map<String, String> values) {
        var text = input;
        for (var e : values.entrySet()) {
            var value = e.getValue() == null ? "" : e.getValue();
            text = text.replace('<' + e.getKey() + '>', STAFF_FORMATTED.contains(e.getKey()) ? value : escape(value));
        }
        return mini.deserialize(text);
    }

    public Component renderKey(String key, Map<String, String> values) {
        return render(raw(key), values);
    }

    public void send(CommandSender target, String input, Map<String, String> values) {
        target.sendMessage(render(input, values));
    }

    public void sendKey(CommandSender target, String key, Map<String, String> values) {
        target.sendMessage(render(prefix + raw(key), values));
    }

    private String escape(String value) {
        return mini.escapeTags(value);
    }
}
