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
}
