package com.nightbeam.donutshards.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryPageTest {
    @Test
    void parsesAndClampsPages() {
        assertThat(HistoryPage.parse(null, 8).offset()).isZero();
        assertThat(HistoryPage.parse("3", 8).offset()).isEqualTo(16);
        assertThat(HistoryPage.parse("-2", 8).page()).isEqualTo(1);
        assertThat(HistoryPage.parse("x", 8).page()).isEqualTo(1);
        assertThat(HistoryPage.parse("9999", 8).page()).isEqualTo(HistoryPage.MAX_PAGES);
    }

    @Test
    void agesAreShort() {
        assertThat(HistoryPage.ago(5_000)).isEqualTo("5s");
        assertThat(HistoryPage.ago(300_000)).isEqualTo("5m");
        assertThat(HistoryPage.ago(7_200_000)).isEqualTo("2h");
        assertThat(HistoryPage.ago(172_800_000)).isEqualTo("2d");
    }
}
