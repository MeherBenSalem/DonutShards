package com.nightbeam.donutshards.booster;

import com.nightbeam.donutshards.core.BoosterState;
import com.nightbeam.donutshards.core.DurationText;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.service.MessageService;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server-wide shard booster. One at a time; it is saved in {@code state.yml} so a restart keeps the time left.
 * While it runs every online player sees a booster boss bar.
 */
public final class BoosterService {
    private final File file;
    private final Logger logger;
    private final MessageService messages;
    private final SchedulerService scheduler;
    private final BossBar bar = BossBar.bossBar(net.kyori.adventure.text.Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private volatile BoosterState state;
    private volatile boolean showBar = true;
    private volatile boolean appliesToKills = true;
    private volatile double maxMultiplier = 10;

    public BoosterService(File dataFolder, Logger logger, MessageService messages, SchedulerService scheduler) {
        this.file = new File(dataFolder, "state.yml");
        this.logger = logger;
        this.messages = messages;
        this.scheduler = scheduler;
        load();
    }

    public void configure(YamlConfiguration config) {
        showBar = config.getBoolean("boosters.boss-bar.enabled", true);
        appliesToKills = config.getBoolean("boosters.applies-to-kills", true);
        maxMultiplier = Math.max(1.01, config.getDouble("boosters.max-multiplier", 10));
        bar.color(color(config.getString("boosters.boss-bar.color", "YELLOW"), BossBar.Color.YELLOW));
    }

    static BossBar.Color color(String name, BossBar.Color fallback) {
        try {
            return BossBar.Color.valueOf(name == null ? "" : name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public double maxMultiplier() {
        return maxMultiplier;
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("booster")) {
            return;
        }
        var loaded = new BoosterState(yaml.getDouble("booster.multiplier"), yaml.getLong("booster.started-at"),
                yaml.getLong("booster.ends-at"), yaml.getString("booster.reason", ""), yaml.getString("booster.started-by", ""));
        state = loaded.active(System.currentTimeMillis()) ? loaded : null;
    }

    private void save() {
        var yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        var current = state;
        if (current == null) {
            yaml.set("booster", null);
        } else {
            yaml.set("booster.multiplier", current.multiplier());
            yaml.set("booster.started-at", current.startedAtMillis());
            yaml.set("booster.ends-at", current.endsAtMillis());
            yaml.set("booster.reason", current.reason());
            yaml.set("booster.started-by", current.startedBy());
        }
        try {
            yaml.save(file);
        } catch (IOException error) {
            logger.log(Level.WARNING, "Could not save state.yml", error);
        }
    }

    public Optional<BoosterState> active() {
        var current = state;
        return current != null && current.active(System.currentTimeMillis()) ? Optional.of(current) : Optional.empty();
    }

    public double multiplier() {
        return active().map(BoosterState::multiplier).orElse(1.0);
    }

    public double killMultiplier() {
        return appliesToKills ? multiplier() : 1.0;
    }

    public BoosterState start(double multiplier, long seconds, String reason, String startedBy) {
        var now = System.currentTimeMillis();
        var started = new BoosterState(multiplier, now, now + seconds * 1000L, reason, startedBy);
        state = started;
        save();
        broadcast("booster-started", values(started, now));
        return started;
    }

    public boolean stop() {
        if (active().isEmpty()) {
            state = null;
            save();
            return false;
        }
        state = null;
        save();
        broadcast("booster-ended", Map.of());
        hideAll();
        return true;
    }

    public Map<String, String> values(BoosterState booster, long now) {
        return Map.of(
                "multiplier", BoosterState.format(booster.multiplier()),
                "time", DurationText.compact(booster.secondsLeft(now)),
                "reason", booster.reason(),
                "player", booster.startedBy());
    }

    /** Called once a second: ends an expired booster and refreshes the boss bar. */
    public void tick() {
        var current = state;
        var now = System.currentTimeMillis();
        if (current == null) {
            if (!viewers.isEmpty()) {
                hideAll();
            }
            return;
        }
        if (!current.active(now)) {
            state = null;
            save();
            broadcast("booster-ended", Map.of());
            hideAll();
            return;
        }
        if (!showBar) {
            hideAll();
            return;
        }
        var key = current.reason().isBlank() ? "booster-bossbar" : "booster-bossbar-reason";
        bar.name(messages.renderKey(key, values(current, now)));
        bar.progress(current.remaining(now));
        for (var player : Bukkit.getOnlinePlayers()) {
            if (viewers.add(player.getUniqueId())) {
                scheduler.entity(player, () -> player.showBossBar(bar), () -> viewers.remove(player.getUniqueId()));
            }
        }
    }

    public void forget(Player player) {
        viewers.remove(player.getUniqueId());
    }

    private void hideAll() {
        for (var player : Bukkit.getOnlinePlayers()) {
            if (viewers.remove(player.getUniqueId())) {
                scheduler.entity(player, () -> player.hideBossBar(bar), () -> {
                });
            }
        }
        viewers.clear();
    }

    private void broadcast(String key, Map<String, String> values) {
        for (var player : Bukkit.getOnlinePlayers()) {
            scheduler.entity(player, () -> messages.sendKey(player, key, values), () -> {
            });
        }
        scheduler.global(() -> messages.sendKey(Bukkit.getConsoleSender(), key, values));
    }

    public void shutdown() {
        for (var player : Bukkit.getOnlinePlayers()) {
            if (viewers.contains(player.getUniqueId())) {
                try {
                    player.hideBossBar(bar);
                } catch (Throwable ignored) {
                    // Shutting down; the client drops boss bars on disconnect anyway.
                }
            }
        }
        viewers.clear();
    }
}
