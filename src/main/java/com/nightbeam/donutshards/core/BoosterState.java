package com.nightbeam.donutshards.core;

/** A server-wide reward booster. */
public record BoosterState(double multiplier, long startedAtMillis, long endsAtMillis, String reason, String startedBy) {
    public static final double MAX_MULTIPLIER = 100;

    public BoosterState {
        reason = reason == null ? "" : reason;
        startedBy = startedBy == null ? "" : startedBy;
    }

    public boolean active(long nowMillis) {
        return multiplier > 0 && nowMillis < endsAtMillis;
    }

    /** Remaining share of the booster's duration, 1 when it starts and 0 when it ends. */
    public float remaining(long nowMillis) {
        var total = endsAtMillis - startedAtMillis;
        if (total <= 0) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, (endsAtMillis - nowMillis) / (float) total));
    }

    public long secondsLeft(long nowMillis) {
        return Math.max(0, (endsAtMillis - nowMillis + 999) / 1000);
    }

    /** Validates a requested multiplier: finite, above 1 (a booster that does not boost is refused) and at most max. */
    public static boolean validMultiplier(double value, double max) {
        return Double.isFinite(value) && value > 1.0 && value <= Math.min(max, MAX_MULTIPLIER);
    }

    /** {@code 2}, {@code 1.5}: no trailing zeros. */
    public static String format(double multiplier) {
        if (multiplier == Math.rint(multiplier)) {
            return Long.toString((long) multiplier);
        }
        var text = String.format(java.util.Locale.ROOT, "%.2f", multiplier);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }
}
