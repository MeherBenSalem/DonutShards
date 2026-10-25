package com.nightbeam.donutshards.config;

import com.nightbeam.donutshards.service.MessageService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class YamlKeyMergerTest {
    @Test
    void mergesMissingMessageKeysAndPreservesPrefix(@TempDir Path dir) throws Exception {
        var messagesFile = dir.resolve("messages.yml");
        Files.writeString(messagesFile, """
                schema-version: 1
                prefix: "CUSTOM "
                balance: "<gray>balance</gray>"
                """);
        var defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getResourceAsStream("/messages.yml"), StandardCharsets.UTF_8));
        var existing = YamlConfiguration.loadConfiguration(messagesFile.toFile());
        YamlKeyMerger.mergeMissing(existing, defaults);
        existing.save(messagesFile.toFile());

        assertThat(existing.getString("prefix")).isEqualTo("CUSTOM ");
        assertThat(existing.getString("afk-joined")).contains("zone");

        var messages = new MessageService();
        messages.load(messagesFile.toFile());
        var component = messages.renderKey("afk-joined", Map.of("zone", "Nebula"));
        assertThat(component.toString()).contains("Nebula");
    }

    @Test
    void mergeMissingRecursesIntoSections() {
        var target = new YamlConfiguration();
        target.set("transfer.tax-basis-points", 500);
        var defaults = new YamlConfiguration();
        defaults.set("transfer.minimum", 1);
        defaults.set("transfer.tax-basis-points", 0);
        defaults.set("afk.home-mode.enabled", true);

        YamlKeyMerger.mergeMissing(target, defaults);

        assertThat(target.getInt("transfer.tax-basis-points")).isEqualTo(500);
        assertThat(target.getInt("transfer.minimum")).isEqualTo(1);
        assertThat(target.getBoolean("afk.home-mode.enabled")).isTrue();
    }

    @Test
    void existingShopKeepsItsLayoutAndGetsDealsSwitchedOff() throws Exception {
        var existing = new YamlConfiguration();
        existing.loadFromString("""
                title: "Mine"
                size: 27
                categories:
                  tools:
                    slot: 0
                """);
        var defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getResourceAsStream("/shop.yml"), StandardCharsets.UTF_8));
        YamlKeyMerger.mergeInto("shop.yml", existing, defaults);
        assertThat(existing.getConfigurationSection("categories").getKeys(false)).containsExactly("tools");
        assertThat(existing.getString("title")).isEqualTo("Mine");
        assertThat(existing.getBoolean("deals.enabled")).isFalse();
        assertThat(existing.isConfigurationSection("deals.pool")).isTrue();
    }

    @Test
    void newConfigKeysAreMergedIntoA15Config() throws Exception {
        var existing = new YamlConfiguration();
        existing.loadFromString("""
                rewards:
                  interval-seconds: 30
                  zone-shards: 4
                """);
        var defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
        YamlKeyMerger.mergeInto("config.yml", existing, defaults);
        assertThat(existing.getInt("rewards.interval-seconds")).isEqualTo(30);
        assertThat(existing.getBoolean("afk.boss-bar.enabled")).isTrue();
        assertThat(existing.getDouble("boosters.max-multiplier")).isEqualTo(10.0);
    }
}
