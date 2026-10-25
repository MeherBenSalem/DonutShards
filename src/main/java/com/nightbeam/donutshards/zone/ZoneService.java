package com.nightbeam.donutshards.zone;

import com.nightbeam.donutshards.core.ZoneBox;
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
import java.util.Comparator;
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
    private final Map<UUID, String> deniedNotified = new ConcurrentHashMap<>();
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
            var zone = section.getConfigurationSection(key);
            if (zone == null) {
                continue;
            }
            var world = zone.getString("world");
            if (world == null || world.isBlank()) {
                continue;
            }
            var settings = new ZoneSettings(
                    zone.contains("shards") ? zone.getLong("shards") : null,
                    zone.contains("interval-seconds") ? zone.getLong("interval-seconds") : null,
                    zone.getString("permission", ""),
                    zone.getString("display-name", ""));
            if ("box".equalsIgnoreCase(zone.getString("shape", "sphere")) && zone.isConfigurationSection("min")
                    && zone.isConfigurationSection("max")) {
                var box = ZoneBox.of(zone.getInt("min.x"), zone.getInt("min.y"), zone.getInt("min.z"),
                        zone.getInt("max.x"), zone.getInt("max.y"), zone.getInt("max.z"));
                configured.put(key.toLowerCase(Locale.ROOT), AfkZone.box(key, world, box, settings));
                continue;
            }
            var radius = Math.max(1.0, zone.getDouble("radius", defaultRadius));
            configured.put(key.toLowerCase(Locale.ROOT), new AfkZone(key, world, zone.getDouble("x"), zone.getDouble("y"),
                    zone.getDouble("z"), radius, null, settings));
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
            if (zone.isBox()) {
                yaml.set(path + ".shape", "box");
            }
            yaml.set(path + ".world", zone.world());
            yaml.set(path + ".x", zone.x());
            yaml.set(path + ".y", zone.y());
            yaml.set(path + ".z", zone.z());
            yaml.set(path + ".radius", zone.radius());
            if (zone.isBox()) {
                var box = zone.box();
                yaml.set(path + ".min.x", box.minX());
                yaml.set(path + ".min.y", box.minY());
                yaml.set(path + ".min.z", box.minZ());
                yaml.set(path + ".max.x", box.maxX());
                yaml.set(path + ".max.y", box.maxY());
                yaml.set(path + ".max.z", box.maxZ());
            }
            var settings = zone.settings();
            if (settings.shards() != null) {
                yaml.set(path + ".shards", settings.shards());
            }
            if (settings.intervalSeconds() != null) {
                yaml.set(path + ".interval-seconds", settings.intervalSeconds());
            }
            if (!settings.permission().isBlank()) {
                yaml.set(path + ".permission", settings.permission());
            }
            if (!settings.displayName().isBlank()) {
                yaml.set(path + ".display-name", settings.displayName());
            }
        }
        yaml.save(file);
    }

    public Collection<AfkZone> zones() {
        return configured.values().stream().sorted(Comparator.comparing(z -> z.name().toLowerCase(Locale.ROOT))).toList();
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
        for (var zone : zones()) {
            if (zone.contains(w, loc.getX(), loc.getY(), loc.getZ())) {
                return Optional.of(zone.name());
            }
        }
        return Optional.empty();
    }

    /**
     * The first zone (by name) the player stands in and may earn in. A zone with a permission the player lacks is
     * skipped; the player is told once per visit.
     */
    public Optional<String> findFor(Player player) {
        var loc = player.getLocation();
        if (loc.getWorld() == null) {
            return Optional.empty();
        }
        var w = loc.getWorld().getName();
        AfkZone denied = null;
        for (var zone : zones()) {
            if (!zone.contains(w, loc.getX(), loc.getY(), loc.getZ())) {
                continue;
            }
            if (mayUse(player, zone)) {
                deniedNotified.remove(player.getUniqueId());
                return Optional.of(zone.name());
            }
            if (denied == null) {
                denied = zone;
            }
        }
        if (denied == null) {
            deniedNotified.remove(player.getUniqueId());
        } else if (!denied.name().equals(deniedNotified.put(player.getUniqueId(), denied.name()))) {
            announce(player, "afk-zone-no-permission", Map.of("zone", denied.label()));
        }
        return Optional.empty();
    }

    public static boolean mayUse(Player player, AfkZone zone) {
        var permission = zone.settings().permission();
        return permission.isBlank() || player.hasPermission(permission);
    }

    public String label(String zoneName) {
        return zone(zoneName).map(AfkZone::label).orElse(zoneName == null ? "" : zoneName);
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
        deniedNotified.clear();
        manualLeave.clear();
        homeMode.clear();
    }

    public void clearPlayer(UUID id) {
        leave(id);
        deniedNotified.remove(id);
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
        if (!validName(name) || loc == null || loc.getWorld() == null) {
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

    public boolean createBox(String name, String world, ZoneBox box) throws IOException {
        if (name == null || !validName(name) || world == null || box == null) {
            return false;
        }
        var key = name.toLowerCase(Locale.ROOT);
        if (configured.containsKey(key)) {
            return false;
        }
        configured.put(key, AfkZone.box(name, world, box, ZoneSettings.DEFAULT));
        save();
        return true;
    }

    public boolean updateSettings(String name, ZoneSettings settings) throws IOException {
        var zone = zone(name);
        if (zone.isEmpty()) {
            return false;
        }
        configured.put(zone.get().name().toLowerCase(Locale.ROOT), zone.get().withSettings(settings));
        save();
        return true;
    }

    /** Letters, digits, {@code -} and {@code _}: zone names end up in YAML paths and permission-free commands. */
    public static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,32}");
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
        applyPresence(player.getUniqueId(), plugin == null ? findAt(player.getLocation()) : findFor(player), player);
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
        var loc = player.getLocation();
        if (!match.get().contains(player.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()) || !mayUse(player, match.get())) {
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
                announce(player, "afk-zone-enter", Map.of("zone", label(zone)));
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
            announce(player, "afk-zone-leave", Map.of("zone", label(zone)));
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
