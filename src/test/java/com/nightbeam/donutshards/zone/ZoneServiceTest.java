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
}
