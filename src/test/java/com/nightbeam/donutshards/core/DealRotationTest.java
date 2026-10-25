package com.nightbeam.donutshards.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DealRotationTest {
    private static final List<String> POOL = List.of("a", "b", "c", "d", "e", "f");

    @Test
    void sameRotationAndSeedGiveTheSameDeals() {
        var first = DealRotation.pick(POOL, 3, 20_000, "server-1");
        assertThat(DealRotation.pick(POOL, 3, 20_000, "server-1")).isEqualTo(first);
        assertThat(first).hasSize(3).doesNotHaveDuplicates().allMatch(POOL::contains);
    }

    @Test
    void dealsChangeBetweenRotations() {
        var seen = new HashSet<List<String>>();
        for (long rotation = 0; rotation < 20; rotation++) {
            seen.add(DealRotation.pick(POOL, 3, rotation, "server-1"));
        }
        assertThat(seen.size()).isGreaterThan(5);
    }

    @Test
    void countIsCappedByThePool() {
        assertThat(DealRotation.pick(List.of("only"), 3, 1, "s")).containsExactly("only");
        assertThat(DealRotation.pick(POOL, 0, 1, "s")).isEmpty();
    }

    @Test
    void rotationBoundaries() {
        var day = 24 * 3_600_000L;
        assertThat(DealRotation.rotationIndex(day * 5 + 1, 24)).isEqualTo(5);
        assertThat(DealRotation.rotationEndsAt(day * 5 + 1, 24)).isEqualTo(day * 6);
        assertThat(DealRotation.rotationIndex(day * 6, 24)).isEqualTo(6);
        assertThat(DealRotation.periodMillis(0)).isEqualTo(3_600_000L);
    }
}
