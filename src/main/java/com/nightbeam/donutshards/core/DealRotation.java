package com.nightbeam.donutshards.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Picks the Daily Deals of a rotation. The pick only depends on the pool order, the rotation number and the seed,
 * so every player and every restart sees the same deals until the rotation ends.
 */
public final class DealRotation {
    private DealRotation() {
    }

    public static long rotationIndex(long nowMillis, long rotationHours) {
        return Math.floorDiv(nowMillis, periodMillis(rotationHours));
    }

    public static long rotationEndsAt(long nowMillis, long rotationHours) {
        return (rotationIndex(nowMillis, rotationHours) + 1) * periodMillis(rotationHours);
    }

    public static long periodMillis(long rotationHours) {
        return Math.max(1, rotationHours) * 3_600_000L;
    }

    public static <T> List<T> pick(List<T> pool, int count, long rotation, String seed) {
        var shuffled = new ArrayList<>(pool);
        var random = new Random(31L * (seed == null ? 0 : seed.hashCode()) + rotation * 0x9E3779B97F4A7C15L);
        // Fisher-Yates with our own Random so the result does not depend on Collections.shuffle internals.
        for (int i = shuffled.size() - 1; i > 0; i--) {
            var j = random.nextInt(i + 1);
            var tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }
        return List.copyOf(shuffled.subList(0, Math.max(0, Math.min(count, shuffled.size()))));
    }
}
