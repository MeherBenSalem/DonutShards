package com.nightbeam.donutshards.zone;

/**
 * Optional per-zone overrides. {@code null} means "use the global {@code rewards.*} value"; an empty permission or
 * display name means "none".
 */
public record ZoneSettings(Long shards, Long intervalSeconds, String permission, String displayName) {
    public static final ZoneSettings DEFAULT = new ZoneSettings(null, null, "", "");

    public ZoneSettings {
        permission = permission == null ? "" : permission.trim();
        displayName = displayName == null ? "" : displayName;
        shards = shards == null || shards < 0 ? null : shards;
        intervalSeconds = intervalSeconds == null || intervalSeconds < 1 ? null : intervalSeconds;
    }

    public long shardsOr(long fallback) {
        return shards == null ? fallback : shards;
    }

    public long intervalOr(long fallback) {
        return intervalSeconds == null ? fallback : intervalSeconds;
    }

    public ZoneSettings withShards(Long value) {
        return new ZoneSettings(value, intervalSeconds, permission, displayName);
    }

    public ZoneSettings withInterval(Long value) {
        return new ZoneSettings(shards, value, permission, displayName);
    }

    public ZoneSettings withPermission(String value) {
        return new ZoneSettings(shards, intervalSeconds, value, displayName);
    }

    public ZoneSettings withDisplayName(String value) {
        return new ZoneSettings(shards, intervalSeconds, permission, value);
    }
}
