package com.nightbeam.donutshards.util;

import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import com.nightbeam.donutshards.reward.KillRewardService;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryLookupsTest {
    @Test
    void candidateKeysCoverEnumAndNamespacedForms() {
        assertThat(RegistryLookups.candidateKeys("ENTITY_EXPERIENCE_ORB_PICKUP"))
                .contains("entity_experience_orb_pickup", "minecraft:entity_experience_orb_pickup", "entity.experience.orb.pickup");
        assertThat(RegistryLookups.candidateKeys("entity.experience_orb.pickup"))
                .contains("entity.experience_orb.pickup", "minecraft:entity.experience_orb.pickup");
        assertThat(RegistryLookups.candidateKeys("minecraft:end_rod"))
                .contains("end_rod", "minecraft:end_rod");
        assertThat(RegistryLookups.candidateKeys(" sharpness ")).contains("sharpness", "minecraft:sharpness");
        assertThat(RegistryLookups.candidateKeys("")).isEmpty();
    }

    @Test
    void constantNameNormalizesDotsAndNamespace() {
        assertThat(RegistryLookups.constantName("minecraft:end_rod")).isEqualTo("END_ROD");
        assertThat(RegistryLookups.constantName("entity.experience_orb.pickup")).isEqualTo("ENTITY_EXPERIENCE_ORB_PICKUP");
    }

    @Test
    void particleFallsBackToEnumConstant() {
        assertThat(RegistryLookups.particle("END_ROD", Particle.CLOUD)).isEqualTo(Particle.END_ROD);
        assertThat(RegistryLookups.particle("end_rod", Particle.CLOUD)).isEqualTo(Particle.END_ROD);
        assertThat(RegistryLookups.particle("not-a-particle", Particle.END_ROD)).isEqualTo(Particle.END_ROD);
    }

    @Test
    void entityTypeFallsBackToEnumConstant() {
        assertThat(RegistryLookups.entityType("ZOMBIE")).isEqualTo(EntityType.ZOMBIE);
        assertThat(RegistryLookups.entityType("minecraft:zombie")).isEqualTo(EntityType.ZOMBIE);
        assertThat(RegistryLookups.entityType("not-an-entity")).isNull();
    }

    @Test
    void killRewardsSkipUnknownMobKeys(@TempDir Path dir) throws Exception {
        var file = dir.resolve("kills.yml");
        Files.writeString(file, """
                enabled: true
                player-kill:
                  enabled: true
                  shards: 5
                mob-kills:
                  default: 1
                  ZOMBIE: 4
                  not-a-mob: 99
                """);
        var service = new KillRewardService(null, null, file.toFile());
        assertThat(service.enabled()).isTrue();
        assertThat(service.mobRewards()).containsEntry(EntityType.ZOMBIE, 4L);
        assertThat(service.mobRewards()).doesNotContainKey(null);
        assertThat(service.mobRewards()).hasSize(1);
    }
}
