package com.nightbeam.donutshards.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoosterStateTest {
    @Test
    void activeUntilItEnds() {
        var booster = new BoosterState(2, 1_000, 61_000, "weekend", "admin");
        assertThat(booster.active(30_000)).isTrue();
        assertThat(booster.active(61_000)).isFalse();
        assertThat(booster.secondsLeft(30_500)).isEqualTo(31);
        assertThat(booster.remaining(31_000)).isEqualTo(0.5f);
    }

    @Test
    void validatesMultipliers() {
        assertThat(BoosterState.validMultiplier(2, 10)).isTrue();
        assertThat(BoosterState.validMultiplier(1, 10)).isFalse();
        assertThat(BoosterState.validMultiplier(11, 10)).isFalse();
        assertThat(BoosterState.validMultiplier(Double.POSITIVE_INFINITY, 1000)).isFalse();
    }

    @Test
    void formatsWithoutTrailingZeros() {
        assertThat(BoosterState.format(2)).isEqualTo("2");
        assertThat(BoosterState.format(1.5)).isEqualTo("1.5");
        assertThat(BoosterState.format(1.25)).isEqualTo("1.25");
    }
}
