package com.nightbeam.donutshards.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RewardMathTest {
    @Test
    void multipliesAndFloorsWithAMinimumOfOne() {
        assertThat(RewardMath.payout(1, 1, 1, 1)).isEqualTo(1);
        assertThat(RewardMath.payout(3, 1, 1.5, 2)).isEqualTo(9);
        assertThat(RewardMath.payout(5, 0.5, 1, 1)).isEqualTo(2);
        assertThat(RewardMath.payout(1, 0.5, 1, 1)).isEqualTo(1);
    }

    @Test
    void zeroWhenAnyFactorIsOff() {
        assertThat(RewardMath.payout(0, 1, 1, 1)).isZero();
        assertThat(RewardMath.payout(5, 0, 1, 1)).isZero();
        assertThat(RewardMath.payout(5, 1, 1, Double.NaN)).isZero();
    }

    @Test
    void highestNumericMultiplierPermissionWins() {
        assertThat(RewardMath.rankMultiplier(List.of("shards.use", "shards.multiplier.1.5", "shards.multiplier.2"))).isEqualTo(2.0);
        assertThat(RewardMath.rankMultiplier(List.of("SHARDS.MULTIPLIER.3"))).isEqualTo(3.0);
        assertThat(RewardMath.rankMultiplier(List.of("shards.multiplier.vip", "shards.multiplier.-2", "shards.multiplier.0"))).isEqualTo(1.0);
        assertThat(RewardMath.rankMultiplier(List.of("shards.multiplier.0.5"))).isEqualTo(0.5);
        assertThat(RewardMath.rankMultiplier(List.of())).isEqualTo(1.0);
    }
}
