package com.nightbeam.donutshards.core;

import java.util.Collection;
import java.util.Locale;

/** Minecraft-free payout rules shared by AFK and kill rewards. */
public final class RewardMath {
    public static final String MULTIPLIER_PREFIX = "shards.multiplier.";

    private RewardMath() {
    }

    /**
     * {@code floor(base × mode × rank × booster)}, at least 1 while every factor is positive, 0 otherwise.
     */
    public static long payout(long base, double mode, double rank, double booster) {
        if (base <= 0 || !(mode > 0) || !(rank > 0) || !(booster > 0)) {
            return 0;
        }
        var raw = Math.floor(base * mode * rank * booster);
        if (raw >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1, (long) raw);
    }

    /** The highest {@code shards.multiplier.<x>} among granted permission nodes (x > 0), or 1. */
    public static double rankMultiplier(Collection<String> grantedPermissions) {
        var best = 1.0;
        var found = false;
        for (var node : grantedPermissions) {
            if (node == null) {
                continue;
            }
            var lower = node.toLowerCase(Locale.ROOT);
            if (!lower.startsWith(MULTIPLIER_PREFIX)) {
                continue;
            }
            try {
                var value = Double.parseDouble(lower.substring(MULTIPLIER_PREFIX.length()));
                if (value > 0 && Double.isFinite(value) && (!found || value > best)) {
                    best = value;
                    found = true;
                }
            } catch (NumberFormatException ignored) {
                // Not a numeric multiplier node.
            }
        }
        return best;
    }
}
