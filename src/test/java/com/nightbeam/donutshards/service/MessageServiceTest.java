package com.nightbeam.donutshards.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageServiceTest {
    @Test
    void loadReadsKeysAndPrefix(@TempDir Path dir) throws Exception {
        var file = dir.resolve("messages.yml");
        Files.writeString(file, """
                schema-version: 1
                prefix: "P "
                balance: "<gray><player> <balance></gray>"
                """);
        var messages = new MessageService();
        messages.load(file.toFile());
        assertThat(messages.raw("balance")).contains("<player>");
        var component = messages.renderKey("balance", Map.of("player", "Ada", "balance", "12"));
        assertThat(component.toString()).contains("Ada");
    }

    @Test
    void missingKeyHasFallback() {
        var messages = new MessageService();
        messages.load(null);
        assertThat(messages.raw("nope")).contains("Missing message");
    }
}
