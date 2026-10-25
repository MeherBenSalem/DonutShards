package com.nightbeam.donutshards.core;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player AFK timers. Each player earns on their own clock: the interval starts when they enter a zone (or home
 * mode) and a session ends as soon as they leave, so leaving or logging off never pays.
 */
public final class AfkSessions {
    /** A player's stay in one place ({@code zone:<name>} or {@code home}). */
    public static final class Session {
        private final String key;
        private final long startedAt;
        private long elapsed;
        private long earned;
        private long payouts;

        Session(String key, long startedAt) {
            this.key = key;
            this.startedAt = startedAt;
        }

        public String key() {
            return key;
        }

        public long startedAt() {
            return startedAt;
        }

        /** Seconds counted towards the next payout. */
        public long elapsed() {
            return elapsed;
        }

        public long earned() {
            return earned;
        }

        public long payouts() {
            return payouts;
        }

        public long secondsLeft(long interval) {
            return Math.max(0, interval - elapsed);
        }

        public float progress(long interval) {
            return interval <= 0 ? 0f : Math.min(1f, Math.max(0f, elapsed / (float) interval));
        }
    }

    /** What one second did: a session that ended (to summarise), the current one, and whether a payout is due. */
    public record Step(Session ended, Session current, boolean due) {
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Advances the player's clock by one second in {@code key} (null = not AFK). When the key changes the old session
     * ends and a new one starts at zero.
     */
    public Step advance(UUID player, String key, long interval, long nowMillis) {
        var old = sessions.get(player);
        Session ended = null;
        if (old != null && (key == null || !old.key.equals(key))) {
            sessions.remove(player);
            ended = old;
            old = null;
        }
        if (key == null) {
            return new Step(ended, null, false);
        }
        var current = old != null ? old : new Session(key, nowMillis);
        sessions.put(player, current);
        current.elapsed++;
        var due = current.elapsed >= Math.max(1, interval);
        if (due) {
            current.elapsed = 0;
        }
        return new Step(ended, current, due);
    }

    /** The current session without moving its clock (used while a player is paused: dead, spectating, riding). */
    public Optional<Session> peek(UUID player) {
        return Optional.ofNullable(sessions.get(player));
    }

    public void credit(UUID player, String key, long amount) {
        var session = sessions.get(player);
        if (session != null && session.key.equals(key)) {
            session.earned += amount;
            session.payouts++;
        }
    }

    public Optional<Session> end(UUID player) {
        return Optional.ofNullable(sessions.remove(player));
    }

    public void clear() {
        sessions.clear();
    }
}
