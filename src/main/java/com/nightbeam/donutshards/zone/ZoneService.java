package com.nightbeam.donutshards.zone;

import com.nightbeam.donutshards.event.AfkZoneEnterEvent;
import com.nightbeam.donutshards.event.AfkZoneLeaveEvent;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.service.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ZoneService {
    private final Plugin plugin;
    private final File file;
    private final Map<UUID, String> current = new ConcurrentHashMap<>();
    private final Set<UUID> manualLeave = ConcurrentHashMap.newKeySet();
    private final Set<UUID> homeMode = ConcurrentHashMap.newKeySet();
    private final Map<String, AfkZone> configured = new ConcurrentHashMap<>();
    private double defaultRadius = 10.0;
    private volatile boolean autoRejoinInZone = true;
    private volatile String preferredTeleportZone = "";
    private volatile MessageService messages;
    private volatile SchedulerService scheduler;

    public ZoneService(Plugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
        load();
    }

    public void setAnnouncer(MessageService messages, SchedulerService scheduler) {
        this.messages = messages;
        this.scheduler = scheduler;
    }

    public void load() {
        configured.clear();
        if (!file.exists()) {
            return;
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        defaultRadius = Math.max(1.0, yaml.getDouble("default-radius", 10.0));
        var section = yaml.getConfigurationSection("zones");
        if (section == null) {
            return;
        }
        for (var key : section.getKeys(false)) {
            var path = "zones." + key;
            var world = yaml.getString(path + ".world");
            if (world == null || world.isBlank()) {
                continue;
            }
            var x = yaml.getDouble(path + ".x");
            var y = yaml.getDouble(path + ".y");
            var z = yaml.getDouble(path + ".z");
            var radius = Math.max(1.0, yaml.getDouble(path + ".radius", defaultRadius));
            configured.put(key.toLowerCase(Locale.ROOT), new AfkZone(key, world, x, y, z, radius));
        }
    }

    public void reload() {
        load();
    }

    public void setAutoRejoinInZone(boolean autoRejoinInZone) {
        this.autoRejoinInZone = autoRejoinInZone;
    }

    public boolean autoRejoinInZone() {
        return autoRejoinInZone;
    }

    public void setPreferredTeleportZone(String preferredTeleportZone) {
        this.preferredTeleportZone = preferredTeleportZone == null ? "" : preferredTeleportZone.trim();
    }

    public String preferredTeleportZone() {
        return preferredTeleportZone;
    }

    public boolean isManualLeave(UUID id) {
        return manualLeave.contains(id);
    }

    public void save() throws IOException {
        var yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        yaml.set("schema-version", 1);
        yaml.set("default-radius", defaultRadius);
        yaml.set("zones", null);
        for (var zone : configured.values()) {
            var path = "zones." + zone.name();
            yaml.set(path + ".world", zone.world());
            yaml.set(path + ".x", zone.x());
            yaml.set(path + ".y", zone.y());
            yaml.set(path + ".z", zone.z());
            yaml.set(path + ".radius", zone.radius());
        }
        yaml.save(file);
    }

    public Collection<AfkZone> zones() {
        return List.copyOf(configured.values());
    }

    public boolean hasZones() {
        return !configured.isEmpty();
    }

    public Optional<AfkZone> zone(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(configured.get(name.toLowerCase(Locale.ROOT)));
    }

    public Optional<String> findAt(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return Optional.empty();
        }
        var w = loc.getWorld().getName();
        for (var zone : configured.values()) {
            if (zone.contains(w, loc.getX(), loc.getY(), loc.getZ())) {
                return Optional.of(zone.name());
            }
        }
        return Optional.empty();
    }

    public Optional<AfkZone> closestZone(Location loc) {
        if (loc == null || loc.getWorld() == null || configured.isEmpty()) {
            return Optional.empty();
        }
        AfkZone best = null;
        double bestDist = Double.MAX_VALUE;
        var worldName = loc.getWorld().getName();
        for (var zone : configured.values()) {
            if (!zone.world().equalsIgnoreCase(worldName)) {
                continue;
            }
            var dist = distanceSquared(loc, zone);
            if (dist < bestDist) {
                bestDist = dist;
                best = zone;
            }
        }
        if (best != null) {
            return Optional.of(best);
        }
        for (var zone : configured.values()) {
            var dist = distanceSquared(loc, zone);
            if (dist < bestDist) {
                bestDist = dist;
                best = zone;
            }
        }
        return Optional.ofNullable(best);
    }

    public Optional<AfkZone> resolveTeleportZone(Location loc) {
        if (preferredTeleportZone != null && !preferredTeleportZone.isBlank()) {
            var named = zone(preferredTeleportZone);
            if (named.isPresent()) {
                return named;
            }
        }
        return closestZone(loc);
    }

    public Optional<Location> teleportLocation(AfkZone zone) {
        if (zone == null) {
            return Optional.empty();
        }
        var world = Bukkit.getWorld(zone.world());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, zone.x(), zone.y(), zone.z()));
    }

    public boolean isInZone(UUID id) {
        return current.containsKey(id);
    }

    public boolean isHomeMode(UUID id) {
        return homeMode.contains(id);
    }

    public AfkMode mode(UUID id) {
        if (homeMode.contains(id)) {
            return AfkMode.HOME;
        }
        if (current.containsKey(id)) {
            return AfkMode.ZONE;
        }
        return AfkMode.NONE;
    }

    public Optional<String> current(UUID id) {
        return Optional.ofNullable(current.get(id));
    }

    public void enter(UUID id, String zone) {
        current.put(id, zone);
    }

    public Optional<String> leave(UUID id) {
        return Optional.ofNullable(current.remove(id));
    }

    public void clear() {
        current.clear();
        manualLeave.clear();
        homeMode.clear();
    }

    public void clearPlayer(UUID id) {
        leave(id);
        homeMode.remove(id);
        manualLeave.remove(id);
    }

    public boolean isRewardEligible(UUID id, boolean requireZoneOrHome) {
        return !requireZoneOrHome || isInZone(id) || isHomeMode(id);
    }

    public double rewardMultiplier(UUID id, double homeMultiplier) {
        if (isInZone(id)) {
            return 1.0;
        }
        if (isHomeMode(id)) {
            return homeMultiplier;
        }
        return 0.0;
    }

    public void enableHome(Player player) {
        var id = player.getUniqueId();
        manualLeave.remove(id);
        current(id).ifPresent(z -> {
            leave(id);
            fireLeave(player, z, false);
        });
        homeMode.add(id);
    }

    public void disableHome(UUID id) {
        homeMode.remove(id);
    }

    /** Package-visible for unit tests without a live Player. */
    void putHomeMode(UUID id) {
        homeMode.add(id);
    }

    public boolean createZone(String name, Location loc, double radius) throws IOException {
        if (name == null || name.isBlank() || loc == null || loc.getWorld() == null) {
            return false;
        }
        var key = name.toLowerCase(Locale.ROOT);
        if (configured.containsKey(key)) {
            return false;
        }
        configured.put(key, new AfkZone(name, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), Math.max(1.0, radius)));
        save();
        return true;
    }

    public boolean deleteZone(String name) throws IOException {
        if (name == null) {
            return false;
        }
        var removed = configured.remove(name.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        save();
        return true;
    }

    public double defaultRadius() {
        return defaultRadius;
    }

    public void syncPlayer(Player player) {
        if (homeMode.contains(player.getUniqueId())) {
            return;
        }
        applyPresence(player.getUniqueId(), findAt(player.getLocation()), player);
    }

    public void applyPresence(UUID id, Optional<String> at, Player player) {
        if (at.isEmpty()) {
            manualLeave.remove(id);
            current(id).ifPresent(z -> {
                if (player != null) {
                    fireLeave(player, z, true);
                } else {
                    leave(id);
                }
            });
            return;
        }
        if (manualLeave.contains(id)) {
            if (!autoRejoinInZone) {
                return;
            }
            manualLeave.remove(id);
        }
        var zone = at.get();
        if (zone.equals(current.get(id))) {
            return;
        }
        current(id).ifPresent(z -> {
            if (player != null) {
                fireLeave(player, z, true);
            } else {
                leave(id);
            }
        });
        if (player != null) {
            fireEnter(player, zone, true);
        } else {
            enter(id, zone);
        }
    }

    public boolean join(Player player, String name) {
        Optional<AfkZone> match = name == null || name.isBlank()
                ? findAt(player.getLocation()).flatMap(this::zone)
                : zone(name);
        if (match.isEmpty()) {
            return false;
        }
        if (!match.get().contains(player.getWorld().getName(), player.getX(), player.getY(), player.getZ())) {
            return false;
        }
        homeMode.remove(player.getUniqueId());
        manualLeave.remove(player.getUniqueId());
        current(player.getUniqueId()).ifPresent(z -> fireLeave(player, z, false));
        fireEnter(player, match.get().name(), false);
        return true;
    }

    public Optional<String> leavePlayer(Player player) {
        return leavePlayerId(player.getUniqueId()).map(z -> {
            fireLeave(player, z, false);
            return z;
        });
    }

    public Optional<String> leavePlayerId(UUID id) {
        manualLeave.add(id);
        homeMode.remove(id);
        return leave(id);
    }

    private void fireEnter(Player player, String zone, boolean announce) {
        if (plugin == null) {
            enter(player.getUniqueId(), zone);
            return;
        }
        var event = new AfkZoneEnterEvent(player, zone);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            enter(player.getUniqueId(), zone);
            if (announce) {
                announce(player, "afk-zone-enter", Map.of("zone", zone));
            }
        }
    }

    private void fireLeave(Player player, String zone, boolean announce) {
        if (plugin == null) {
            leave(player.getUniqueId());
            return;
        }
        Bukkit.getPluginManager().callEvent(new AfkZoneLeaveEvent(player, zone));
        leave(player.getUniqueId());
        if (announce) {
            announce(player, "afk-zone-leave", Map.of("zone", zone));
        }
    }

    private void announce(Player player, String key, Map<String, String> values) {
        var msg = messages;
        var sched = scheduler;
        if (msg == null || sched == null) {
            return;
        }
        sched.entity(player, () -> msg.sendKey(player, key, values), () -> {
        });
    }

    private static double distanceSquared(Location loc, AfkZone zone) {
        var dx = loc.getX() - zone.x();
        var dy = loc.getY() - zone.y();
        var dz = loc.getZ() - zone.z();
        return dx * dx + dy * dy + dz * dz;
    }
}
