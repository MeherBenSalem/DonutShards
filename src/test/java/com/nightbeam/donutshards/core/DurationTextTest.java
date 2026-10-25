package com.nightbeam.donutshards.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DurationTextTest {
    @Test
    void parsesUnitsAndCombinations() {
        assertThat(DurationText.parseSeconds("90")).hasValue(90);
        assertThat(DurationText.parseSeconds("90s")).hasValue(90);
        assertThat(DurationText.parseSeconds("30m")).hasValue(1800);
        assertThat(DurationText.parseSeconds("1h30m")).hasValue(5400);
        assertThat(DurationText.parseSeconds("2D")).hasValue(172800);
    }

    @Test
    void rejectsGarbage() {
        assertThat(DurationText.parseSeconds("")).isEmpty();
        assertThat(DurationText.parseSeconds("0")).isEmpty();
        assertThat(DurationText.parseSeconds("5x")).isEmpty();
        assertThat(DurationText.parseSeconds("m5")).isEmpty();
        assertThat(DurationText.parseSeconds("-5m")).isEmpty();
        assertThat(DurationText.parseSeconds("99999999999999999999d")).isEmpty();
    }

    @Test
    void printsCompactly() {
        assertThat(DurationText.compact(12)).isEqualTo("12s");
        assertThat(DurationText.compact(249)).isEqualTo("4m 09s");
        assertThat(DurationText.compact(3900)).isEqualTo("1h 05m");
        assertThat(DurationText.compact(183600)).isEqualTo("2d 03h");
        assertThat(DurationText.compact(-4)).isEqualTo("0s");
    }
}
