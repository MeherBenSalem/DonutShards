package com.nightbeam.donutshards.reward;

import com.nightbeam.donutshards.booster.BoosterService;
import com.nightbeam.donutshards.core.AfkSessions;
import com.nightbeam.donutshards.core.DurationText;
import com.nightbeam.donutshards.core.RewardMath;
import com.nightbeam.donutshards.model.MutationContext;
import com.nightbeam.donutshards.model.TransactionType;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.scheduler.TaskHandle;
import com.nightbeam.donutshards.service.MessageService;
import com.nightbeam.donutshards.transaction.TransactionService;
import com.nightbeam.donutshards.util.RegistryLookups;
import com.nightbeam.donutshards.zone.ZoneService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import com.nightbeam.donutshards.event.ShardRewardEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AFK rewards on a per-player clock. Once a second every online player's place is checked on their own entity
 * thread: inside a zone (or home AFK) the clock advances and pays at the zone's interval; anywhere else the session
 * ends, with a summary if it earned something. A boss bar shows the countdown.
 */
public final class RewardService implements Listener {
    private static final String HOME = "home";

    private final TransactionService tx;
    private final SchedulerService scheduler;
    private final MessageService messages;
    private final ZoneService zones;
    private final BoosterService boosters;
    private final AfkSessions sessions = new AfkSessions();
    private final Map<UUID, Player> online = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final AtomicLong generation = new AtomicLong();
    private volatile TaskHandle task;
    private volatile long intervalSeconds = 60;
    private volatile long zoneShards = 1;
    private volatile double homeMultiplier = 0.5;
    private volatile boolean showTitle = true;
    private volatile boolean playSound = true;
    private volatile boolean showActionBar = true;
    private volatile boolean showBar = true;
    private volatile boolean summary = true;
    private volatile BossBar.Color barColor = BossBar.Color.PINK;

    public RewardService(TransactionService tx, SchedulerService scheduler, MessageService messages, ZoneService zones,
                         BoosterService boosters) {
        this.tx = tx;
        this.scheduler = scheduler;
        this.messages = messages;
        this.zones = zones;
        this.boosters = boosters;
    }

    public void configure(YamlConfiguration config) {
        intervalSeconds = Math.max(1, config.getLong("rewards.interval-seconds", 60));
        zoneShards = Math.max(0, config.getLong("rewards.zone-shards", 1));
        homeMultiplier = config.getDouble("afk.home-mode.shard-multiplier", 0.5);
        showTitle = config.getBoolean("rewards.title", true);
        playSound = config.getBoolean("rewards.sound", true);
        showActionBar = config.getBoolean("rewards.action-bar", true);
        showBar = config.getBoolean("afk.boss-bar.enabled", true);
        summary = config.getBoolean("afk.session-summary", true);
        try {
            barColor = BossBar.Color.valueOf(config.getString("afk.boss-bar.color", "PINK").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            barColor = BossBar.Color.PINK;
        }
        for (var bar : bars.values()) {
            bar.color(barColor);
        }
    }

    public void start() {
        var current = generation.incrementAndGet();
        var old = task;
        if (old != null) {
            old.cancel();
        }
        var second = Duration.ofSeconds(1);
        task = scheduler.asyncRepeating(() -> {
            if (generation.get() != current) {
                return;
            }
            boosters.tick();
            for (var entry : online.entrySet()) {
                var p = entry.getValue();
                scheduler.entity(p, () -> tick(p), () -> online.remove(entry.getKey()));
            }
        }, second, second);
    }

    /** What the player is doing right now: {@code zone:<name>}, {@code home} or null. */
    private String placeKey(UUID id) {
        if (zones.isHomeMode(id)) {
            return HOME;
        }
        return zones.current(id).map(z -> "zone:" + z.toLowerCase(Locale.ROOT)).orElse(null);
    }

    private long interval(String key) {
        if (key == null || HOME.equals(key)) {
            return intervalSeconds;
        }
        return zones.zone(key.substring(5)).map(z -> z.settings().intervalOr(intervalSeconds)).orElse(intervalSeconds);
    }

    private long baseShards(String key) {
        if (key == null || HOME.equals(key)) {
            return zoneShards;
        }
        return zones.zone(key.substring(5)).map(z -> z.settings().shardsOr(zoneShards)).orElse(zoneShards);
    }

    private String label(String key) {
        if (key == null) {
            return "";
        }
        if (HOME.equals(key)) {
            return messages.raw("afk-home-label");
        }
        return zones.label(key.substring(5));
    }

    private void tick(Player p) {
        var id = p.getUniqueId();
        // Catches teleports, logins and zones created around a standing player (none of them fire a move event).
        zones.syncPlayer(p);
        var key = placeKey(id);
        if (key != null && (p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.isInsideVehicle())) {
            // Paused: the clock does not move but the session (and its bar) stays.
            sessions.peek(id).ifPresent(s -> updateBar(p, s, key));
            return;
        }
        var interval = interval(key);
        var step = sessions.advance(id, key, interval, System.currentTimeMillis());
        if (step.ended() != null) {
            endSession(p, step.ended());
        }
        var current = step.current();
        if (current == null) {
            hideBar(p);
            return;
        }
        if (step.due()) {
            pay(p, key);
        }
        updateBar(p, current, key);
    }

    /** The payout for the player's next reward in {@code key}. */
    public long nextAmount(Player p, String key) {
        var mode = HOME.equals(key) ? homeMultiplier : 1.0;
        return RewardMath.payout(baseShards(key), mode, rankMultiplier(p), boosters.multiplier());
    }

    public static double rankMultiplier(Player p) {
        var nodes = new java.util.ArrayList<String>();
        for (PermissionAttachmentInfo info : p.getEffectivePermissions()) {
            if (info.getValue()) {
                nodes.add(info.getPermission());
            }
        }
        return RewardMath.rankMultiplier(nodes);
    }

    private void pay(Player p, String key) {
        var id = p.getUniqueId();
        var computed = nextAmount(p, key);
        if (computed < 1) {
            return;
        }
        var source0 = HOME.equals(key) ? "reward:afk-home" : "reward:afk:" + key.substring(5);
        var event = new ShardRewardEvent(id, source0, computed, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.amount() < 1) {
            return;
        }
        var amount = event.amount();
        var metadata = new HashMap<String, String>();
        metadata.put("reward", "afk");
        metadata.put("mode", HOME.equals(key) ? "HOME" : "ZONE");
        if (!HOME.equals(key)) {
            metadata.put("zone", key.substring(5));
        }
        var booster = boosters.multiplier();
        if (booster > 1) {
            metadata.put("booster", Double.toString(booster));
        }
        var context = new MutationContext(TransactionType.REWARD, source0, null, "reward:" + id + ':' + UUID.randomUUID(), metadata);
        var interval = interval(key);
        tx.add(id, amount, context).whenComplete((r, e) -> scheduler.entity(p, () -> {
            if (e != null || !r.success()) {
                return;
            }
            sessions.credit(id, key, amount);
            var placeholders = Map.of(
                    "amount", Long.toString(amount),
                    "next_reward", Long.toString(interval),
                    "time", Long.toString(interval),
                    "zone", label(key),
                    "balance", Long.toString(r.transaction().newBalance()));
            if (showActionBar) {
                p.sendActionBar(messages.renderKey("reward", placeholders));
            }
            if (showTitle) {
                p.showTitle(Title.title(
                        messages.renderKey("reward-title", placeholders),
                        messages.renderKey("next-reward", placeholders),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400))));
            }
            if (playSound) {
                p.playSound(p.getLocation(), RegistryLookups.sound("entity.experience_orb.pickup", Sound.ENTITY_EXPERIENCE_ORB_PICKUP), 0.7f, 1.3f);
            }
        }, () -> {
        }));
    }

    private void updateBar(Player p, AfkSessions.Session session, String key) {
        if (!showBar) {
            hideBar(p);
            return;
        }
        var interval = interval(key);
        var values = new HashMap<String, String>();
        values.put("zone", label(key));
        values.put("amount", Long.toString(nextAmount(p, key)));
        values.put("time", Long.toString(session.secondsLeft(interval)));
        values.put("session", Long.toString(session.earned()));
        values.put("duration", DurationText.compact((System.currentTimeMillis() - session.startedAt()) / 1000));
        Component name = messages.renderKey(HOME.equals(key) ? "afk-bossbar-home" : "afk-bossbar", values);
        var created = new boolean[1];
        var bar = bars.computeIfAbsent(p.getUniqueId(), id -> {
            created[0] = true;
            return BossBar.bossBar(name, 0f, barColor, BossBar.Overlay.PROGRESS);
        });
        bar.name(name);
        bar.progress(session.progress(interval));
        if (created[0]) {
            p.showBossBar(bar);
        }
    }

    private void hideBar(Player p) {
        var bar = bars.remove(p.getUniqueId());
        if (bar != null) {
            p.hideBossBar(bar);
        }
    }

    private void endSession(Player p, AfkSessions.Session ended) {
        if (!summary || ended.earned() <= 0) {
            return;
        }
        var seconds = (System.currentTimeMillis() - ended.startedAt()) / 1000;
        messages.sendKey(p, "afk-session-summary", Map.of(
                "zone", label(ended.key()),
                "duration", DurationText.compact(seconds),
                "earned", Long.toString(ended.earned()),
                "payouts", Long.toString(ended.payouts())));
    }

    /** The player's current AFK session, for placeholders and {@code /shardmanager info}. */
    public Optional<AfkSessions.Session> session(UUID player) {
        return sessions.peek(player);
    }

    public long secondsToNext(UUID player) {
        return sessions.peek(player).map(s -> s.secondsLeft(interval(s.key()))).orElse(-1L);
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        online.put(e.getPlayer().getUniqueId(), e.getPlayer());
        tx.touch(e.getPlayer().getUniqueId(), e.getPlayer().getName());
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        var id = e.getPlayer().getUniqueId();
        online.remove(id);
        sessions.end(id);
        bars.remove(id);
        boosters.forget(e.getPlayer());
        tx.forget(id);
    }

    public void stop() {
        generation.incrementAndGet();
        if (task != null) {
            task.cancel();
        }
        for (var entry : online.entrySet()) {
            var bar = bars.remove(entry.getKey());
            if (bar != null) {
                try {
                    entry.getValue().hideBossBar(bar);
                } catch (Throwable ignored) {
                    // Shutting down.
                }
            }
        }
        online.clear();
        sessions.clear();
    }

    /** Keeps the online map in step after {@code /reload}-style restarts of the service. */
    public void track(Player player) {
        online.put(player.getUniqueId(), player);
    }
}
