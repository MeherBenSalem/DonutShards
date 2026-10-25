package com.nightbeam.donutshards.core;

/**
 * An axis-aligned AFK zone made of whole blocks. The selected corner blocks are both inside; the box also reaches
 * {@link #HEADROOM} blocks above the highest selected block, so selecting just the floor makes a zone players can
 * stand in.
 */
public record ZoneBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static final int HEADROOM = 2;

    public static ZoneBox of(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new ZoneBox(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    /** Feet position test: x and z within the selected blocks, y from the bottom block to the headroom top. */
    public boolean contains(double x, double y, double z) {
        return x >= minX && x < maxX + 1 && z >= minZ && z < maxZ + 1 && y >= minY && y < maxY + 1 + HEADROOM;
    }

    public double centerX() {
        return (minX + maxX + 1) / 2.0;
    }

    public double centerY() {
        return minY + 1;
    }

    public double centerZ() {
        return (minZ + maxZ + 1) / 2.0;
    }

    /** Radius of the circle around the centre that encloses the floor (used for the 1.x fields in zones.yml). */
    public double enclosingRadius() {
        var dx = (maxX + 1 - minX) / 2.0;
        var dz = (maxZ + 1 - minZ) / 2.0;
        return Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
    }

    public long blocks() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public String describe() {
        return "(" + minX + ", " + minY + ", " + minZ + ") to (" + maxX + ", " + maxY + ", " + maxZ + ")";
    }
}
