package com.nightbeam.donutshards.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModrinthUpdateCheckerTest {
    @Test
    void picksNewestVersionByPublishedDate() {
        var json = """
                [
                  {"version_number":"1.3.0","date_published":"2026-01-01T00:00:00Z"},
                  {"version_number":"1.4.0","date_published":"2026-08-01T00:00:00Z"}
                ]
                """;
        assertThat(ModrinthUpdateChecker.pickNewestVersionNumber(json)).isEqualTo("1.4.0");
    }

    @Test
    void compareVersionsOrdersDottedReleases() {
        assertThat(ModrinthUpdateChecker.compareVersions("1.4.0", "1.3.0")).isPositive();
        assertThat(ModrinthUpdateChecker.compareVersions("1.3.0", "1.4.0")).isNegative();
    }
}
