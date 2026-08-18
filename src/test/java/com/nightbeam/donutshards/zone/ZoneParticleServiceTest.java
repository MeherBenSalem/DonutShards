package com.nightbeam.donutshards.zone;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneParticleServiceTest {
    @Test
    void ringPointsSitOnRadius() {
        var points = ZoneParticleService.ringPoints(0, 64, 0, 10, 8, 1);
        assertThat(points).hasDimensions(8, 3);
        for (var point : points) {
            var dx = point[0];
            var dz = point[2];
            assertThat(Math.hypot(dx, dz)).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.0001));
            assertThat(point[1]).isEqualTo(64.0);
        }
    }
}
