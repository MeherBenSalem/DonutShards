package com.nightbeam.donutshards.zone;

import com.nightbeam.donutshards.core.ZoneBox;

/**
 * An AFK zone: a sphere around {@code x/y/z} (1.x zones) or, when {@code box} is set, a block box. For boxes
 * {@code x/y/z/radius} hold the centre and an enclosing radius (what 1.x would read back).
 */
public record AfkZone(String name, String world, double x, double y, double z, double radius, ZoneBox box,
                      ZoneSettings settings) {
    public AfkZone {
        settings = settings == null ? ZoneSettings.DEFAULT : settings;
    }

    public AfkZone(String name, String world, double x, double y, double z, double radius) {
        this(name, world, x, y, z, radius, null, ZoneSettings.DEFAULT);
    }

    public static AfkZone box(String name, String world, ZoneBox box, ZoneSettings settings) {
        return new AfkZone(name, world, box.centerX(), box.centerY(), box.centerZ(), box.enclosingRadius(), box, settings);
    }

    public boolean isBox() {
        return box != null;
    }

    public boolean contains(String worldName, double px, double py, double pz) {
        if (world == null || !world.equalsIgnoreCase(worldName)) {
            return false;
        }
        if (box != null) {
            return box.contains(px, py, pz);
        }
        var dx = px - x;
        var dy = py - y;
        var dz = pz - z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    public AfkZone withSettings(ZoneSettings value) {
        return new AfkZone(name, world, x, y, z, radius, box, value);
    }

    /** The name players see: the display name when set, the zone id otherwise. */
    public String label() {
        return settings.displayName().isBlank() ? name : settings.displayName();
    }

    public String describe() {
        return box != null ? "box " + box.describe() : "sphere (" + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z) + ") r=" + radius;
    }
}
