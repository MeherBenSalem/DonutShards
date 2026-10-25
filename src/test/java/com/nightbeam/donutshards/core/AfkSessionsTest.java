package com.nightbeam.donutshards.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AfkSessionsTest {
    private final AfkSessions sessions = new AfkSessions();
    private final UUID player = UUID.randomUUID();

    @Test
    void paysOnTheIntervalCountedFromEntry() {
        for (int second = 1; second < 5; second++) {
            assertThat(sessions.advance(player, "zone:a", 5, 0).due()).isFalse();
        }
        var fifth = sessions.advance(player, "zone:a", 5, 0);
        assertThat(fifth.due()).isTrue();
        assertThat(fifth.current().elapsed()).isZero();
        assertThat(sessions.advance(player, "zone:a", 5, 0).due()).isFalse();
    }

    @Test
    void leavingEndsTheSessionAndResetsTheClock() {
        sessions.advance(player, "zone:a", 5, 0);
        sessions.advance(player, "zone:a", 5, 0);
        sessions.credit(player, "zone:a", 7);
        var left = sessions.advance(player, null, 5, 0);
        assertThat(left.ended()).isNotNull();
        assertThat(left.ended().earned()).isEqualTo(7);
        assertThat(left.current()).isNull();
        assertThat(sessions.peek(player)).isEmpty();
        var back = sessions.advance(player, "zone:a", 5, 0);
        assertThat(back.current().elapsed()).isEqualTo(1);
        assertThat(back.current().earned()).isZero();
    }

    @Test
    void switchingZonesStartsANewSession() {
        sessions.advance(player, "zone:a", 5, 0);
        var moved = sessions.advance(player, "zone:b", 5, 0);
        assertThat(moved.ended().key()).isEqualTo("zone:a");
        assertThat(moved.current().key()).isEqualTo("zone:b");
    }

    @Test
    void creditOnlyCountsForTheCurrentPlace() {
        sessions.advance(player, "zone:a", 5, 0);
        sessions.credit(player, "home", 3);
        assertThat(sessions.peek(player).orElseThrow().earned()).isZero();
    }

    @Test
    void progressAndSecondsLeft() {
        sessions.advance(player, "home", 4, 0);
        var session = sessions.peek(player).orElseThrow();
        assertThat(session.progress(4)).isEqualTo(0.25f);
        assertThat(session.secondsLeft(4)).isEqualTo(3);
    }
}
