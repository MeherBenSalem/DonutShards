package com.nightbeam.donutshards.zone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneServiceTest {
    @Test
    void tracksMembership() {
        var service = new ZoneService(null, new File("missing-zones.yml"));
        var id = UUID.randomUUID();
        assertThat(service.isInZone(id)).isFalse();
        service.enter(id, "glaze-ring");
        assertThat(service.current(id)).contains("glaze-ring");
        service.leave(id);
        assertThat(service.current(id)).isEmpty();
    }

    @Test
    void sphereContainsPoint() {
        var zone = new AfkZone("hub", "world", 0, 64, 0, 10);
        assertThat(zone.contains("world", 5, 64, 0)).isTrue();
        assertThat(zone.contains("world", 11, 64, 0)).isFalse();
        assertThat(zone.contains("other", 0, 64, 0)).isFalse();
    }

    @Test
    void loadsZonesFromYaml(@TempDir Path dir) throws IOException {
        var file = dir.resolve("zones.yml").toFile();
        Files.writeString(file.toPath(), """
                schema-version: 1
                default-radius: 10.0
                zones:
                  glaze-ring:
                    world: world
                    x: 100.0
                    y: 64.0
                    z: -50.0
                    radius: 12.0
                """);
        var service = new ZoneService(null, file);
        assertThat(service.zone("glaze-ring")).isPresent();
        assertThat(service.zone("glaze-ring").get().radius()).isEqualTo(12.0);
    }

    @Test
    void deletePersists(@TempDir Path dir) throws IOException {
        var file = dir.resolve("zones.yml").toFile();
        Files.writeString(file.toPath(), """
                schema-version: 1
                default-radius: 10.0
                zones:
                  temp:
                    world: world
                    x: 0.0
                    y: 64.0
                    z: 0.0
                    radius: 5.0
                """);
        var service = new ZoneService(null, file);
        assertThat(service.deleteZone("temp")).isTrue();
        service = new ZoneService(null, file);
        assertThat(service.zone("temp")).isEmpty();
    }

    @Test
    void rewardEligibilityAndClear() {
        var service = new ZoneService(null, new File("missing-zones.yml"));
        var id = UUID.randomUUID();
        assertThat(service.isRewardEligible(id, true)).isFalse();
        service.enter(id, "hub");
        assertThat(service.isRewardEligible(id, true)).isTrue();
        assertThat(service.rewardMultiplier(id, 0.5)).isEqualTo(1.0);
        assertThat(service.mode(id)).isEqualTo(AfkMode.ZONE);
        service.clearPlayer(id);
        assertThat(service.isInZone(id)).isFalse();
        assertThat(service.mode(id)).isEqualTo(AfkMode.NONE);
    }

    @Test
    void autoRejoinWhileStillInsideZone() {
        var service = new ZoneService(null, new File("missing-zones.yml"));
        service.setAutoRejoinInZone(true);
        var id = UUID.randomUUID();
        service.enter(id, "hub");
        service.leavePlayerId(id);
        assertThat(service.isInZone(id)).isFalse();
        assertThat(service.isManualLeave(id)).isTrue();
        service.applyPresence(id, Optional.of("hub"), null);
        assertThat(service.current(id)).contains("hub");
        assertThat(service.isManualLeave(id)).isFalse();
    }

    @Test
    void manualLeaveBlocksWithoutAutoRejoin() {
        var service = new ZoneService(null, new File("missing-zones.yml"));
        service.setAutoRejoinInZone(false);
        var id = UUID.randomUUID();
        service.enter(id, "hub");
        service.leavePlayerId(id);
        service.applyPresence(id, Optional.of("hub"), null);
        assertThat(service.isInZone(id)).isFalse();
    }

    @Test
    void homeModeCancelClearsEligibilityUntilZonePresence() {
        var service = new ZoneService(null, new File("missing-zones.yml"));
        var id = UUID.randomUUID();
        service.putHomeMode(id);
        assertThat(service.isHomeMode(id)).isTrue();
        assertThat(service.isRewardEligible(id, true)).isTrue();
        assertThat(service.rewardMultiplier(id, 0.5)).isEqualTo(0.5);

        service.disableHome(id);
        assertThat(service.isHomeMode(id)).isFalse();
        assertThat(service.isRewardEligible(id, true)).isFalse();

        service.applyPresence(id, Optional.of("hub"), null);
        assertThat(service.current(id)).contains("hub");
        assertThat(service.isRewardEligible(id, true)).isTrue();
        assertThat(service.rewardMultiplier(id, 0.5)).isEqualTo(1.0);
    }

    @Test
    void preferredTeleportZoneOverridesClosest(@TempDir Path dir) throws IOException {
        var file = dir.resolve("zones.yml").toFile();
        Files.writeString(file.toPath(), """
                schema-version: 1
                default-radius: 10.0
                zones:
                  near:
                    world: world
                    x: 0.0
                    y: 64.0
                    z: 0.0
                    radius: 5.0
                  far:
                    world: world
                    x: 100.0
                    y: 64.0
                    z: 0.0
                    radius: 5.0
                """);
        var service = new ZoneService(null, file);
        service.setPreferredTeleportZone("far");
        assertThat(service.zone("far")).isPresent();
        assertThat(service.resolveTeleportZone(null).map(AfkZone::name)).contains("far");
    }

    @Test
    void boxZonesAndSettingsSurviveASaveAndLoad(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var file = dir.resolve("zones.yml").toFile();
        var service = new ZoneService(null, file);
        assertThat(service.createBox("sweet-spot", "world", com.nightbeam.donutshards.core.ZoneBox.of(0, 64, 0, 4, 64, 4))).isTrue();
        assertThat(service.createBox("sweet-spot", "world", com.nightbeam.donutshards.core.ZoneBox.of(0, 64, 0, 1, 64, 1))).isFalse();
        assertThat(service.createBox("bad name", "world", com.nightbeam.donutshards.core.ZoneBox.of(0, 64, 0, 1, 64, 1))).isFalse();
        service.updateSettings("SWEET-SPOT", ZoneSettings.DEFAULT.withShards(3L).withInterval(20L).withPermission("shards.zone.vip").withDisplayName("<gold>Sweet"));
        var reloaded = new ZoneService(null, file);
        var zone = reloaded.zone("sweet-spot").orElseThrow();
        assertThat(zone.isBox()).isTrue();
        assertThat(zone.box().maxX()).isEqualTo(4);
        assertThat(zone.settings().shardsOr(1)).isEqualTo(3);
        assertThat(zone.settings().intervalOr(60)).isEqualTo(20);
        assertThat(zone.settings().permission()).isEqualTo("shards.zone.vip");
        assertThat(zone.label()).isEqualTo("<gold>Sweet");
        assertThat(zone.contains("world", 2.5, 65, 2.5)).isTrue();
        assertThat(zone.contains("world", 5.5, 65, 2.5)).isFalse();
        assertThat(reloaded.findAt(null)).isEmpty();
        // 1.x readers see a sphere around the box centre.
        var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        assertThat(yaml.getDouble("zones.sweet-spot.x")).isEqualTo(2.5);
        assertThat(yaml.getDouble("zones.sweet-spot.radius")).isGreaterThan(3);
    }

    @Test
    void sphereZonesWithoutSettingsUseDefaults(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        var file = dir.resolve("zones.yml");
        Files.writeString(file, """
                schema-version: 1
                default-radius: 10.0
                zones:
                  lobby:
                    world: world
                    x: 0.0
                    y: 64.0
                    z: 0.0
                    radius: 6.0
                """);
        var zone = new ZoneService(null, file.toFile()).zone("lobby").orElseThrow();
        assertThat(zone.isBox()).isFalse();
        assertThat(zone.settings()).isEqualTo(ZoneSettings.DEFAULT);
        assertThat(zone.settings().shardsOr(4)).isEqualTo(4);
        assertThat(zone.label()).isEqualTo("lobby");
    }
}
