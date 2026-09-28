package com.nightbeam.donutshards.zone;

import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.scheduler.TaskHandle;
import com.nightbeam.donutshards.util.RegistryLookups;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.time.Duration;

public final class ZoneParticleService {
    private final Plugin plugin;
    private final SchedulerService scheduler;
    private final ZoneService zones;
    private volatile boolean enabled = true;
    private volatile Particle particle = Particle.END_ROD;
    private volatile int countPerPoint = 1;
    private volatile int ringPoints = 32;
    private volatile int verticalSamples = 3;
    private volatile long intervalSeconds = 2;
    private volatile double viewRadius = 64;
    private volatile TaskHandle task;

    public ZoneParticleService(Plugin plugin, SchedulerService scheduler, ZoneService zones) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.zones = zones;
    }

    public void configure(YamlConfiguration config) {
        enabled = config.getBoolean("afk.zone-particles.enabled", true);
        countPerPoint = Math.max(1, config.getInt("afk.zone-particles.count-per-point", 1));
        ringPoints = Math.max(8, config.getInt("afk.zone-particles.ring-points", 32));
        verticalSamples = Math.max(1, config.getInt("afk.zone-particles.vertical-samples", 3));
        intervalSeconds = Math.max(1, config.getLong("afk.zone-particles.interval-seconds", 2));
        viewRadius = Math.max(8.0, config.getDouble("afk.zone-particles.view-radius", 64));
        var name = config.getString("afk.zone-particles.particle", "END_ROD");
        particle = RegistryLookups.particle(name, Particle.END_ROD);
    }

    public void start() {
        stop();
        if (!enabled) {
            return;
        }
        var interval = Duration.ofSeconds(intervalSeconds);
        task = scheduler.asyncRepeating(this::tick, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (var zone : zones.zones()) {
            var world = plugin.getServer().getWorld(zone.world());
            if (world == null) {
                continue;
            }
            var center = new Location(world, zone.x(), zone.y(), zone.z());
            scheduler.region(center, () -> spawnRing(zone, center));
        }
    }

    private void spawnRing(AfkZone zone, Location center) {
        var world = center.getWorld();
        if (world == null) {
            return;
        }
        var nearby = false;
        for (var entity : world.getNearbyEntities(center, viewRadius, viewRadius, viewRadius)) {
            if (entity instanceof org.bukkit.entity.Player) {
                nearby = true;
                break;
            }
        }
        if (!nearby) {
            return;
        }
        var points = ringPoints(zone.x(), zone.y(), zone.z(), zone.radius(), ringPoints, verticalSamples);
        for (var point : points) {
            world.spawnParticle(particle, point[0], point[1], point[2], countPerPoint, 0, 0, 0, 0);
        }
    }

    static double[][] ringPoints(double x, double y, double z, double radius, int ringPoints, int verticalSamples) {
        var samples = Math.max(1, verticalSamples);
        var points = new double[ringPoints * samples][3];
        var index = 0;
        for (var layer = 0; layer < samples; layer++) {
            var yOffset = samples == 1 ? 0 : (layer - (samples - 1) / 2.0);
            for (var i = 0; i < ringPoints; i++) {
                var angle = (Math.PI * 2 * i) / ringPoints;
                points[index][0] = x + Math.cos(angle) * radius;
                points[index][1] = y + yOffset;
                points[index][2] = z + Math.sin(angle) * radius;
                index++;
            }
        }
        return points;
    }
}
