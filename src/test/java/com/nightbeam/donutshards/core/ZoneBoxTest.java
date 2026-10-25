package com.nightbeam.donutshards.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneBoxTest {
    @Test
    void cornersInAnyOrderAndFloorSelectionHasHeadroom() {
        var box = ZoneBox.of(10, 64, -5, 0, 64, 5);
        assertThat(box.minX()).isZero();
        assertThat(box.maxZ()).isEqualTo(5);
        // A player standing on the selected floor has their feet at y = 65.
        assertThat(box.contains(0.5, 65, 0.5)).isTrue();
        assertThat(box.contains(10.99, 66.9, 5.99)).isTrue();
        assertThat(box.contains(11.0, 65, 0)).isFalse();
        assertThat(box.contains(5, 67, 0)).isFalse();
        assertThat(box.contains(5, 63.9, 0)).isFalse();
    }

    @Test
    void centreAndEnclosingRadius() {
        var box = ZoneBox.of(0, 64, 0, 9, 66, 9);
        assertThat(box.centerX()).isEqualTo(5.0);
        assertThat(box.centerY()).isEqualTo(65.0);
        assertThat(box.enclosingRadius()).isCloseTo(Math.sqrt(50), org.assertj.core.data.Offset.offset(1e-9));
        assertThat(box.blocks()).isEqualTo(300);
    }
}
