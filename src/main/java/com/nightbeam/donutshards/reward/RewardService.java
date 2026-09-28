package com.nightbeam.donutshards.reward;

import com.nightbeam.donutshards.model.MutationContext;
import com.nightbeam.donutshards.model.TransactionType;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.scheduler.TaskHandle;
import com.nightbeam.donutshards.service.MessageService;
import com.nightbeam.donutshards.transaction.TransactionService;
import com.nightbeam.donutshards.util.RegistryLookups;
import com.nightbeam.donutshards.zone.ZoneService;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RewardService implements Listener {
    private final TransactionService tx;
    private final SchedulerService scheduler;
    private final MessageService messages;
    private final ZoneService zones;
    private final Map<UUID, Player> online = new ConcurrentHashMap<>();
    private final AtomicLong generation = new AtomicLong();
    private volatile TaskHandle task;
    private volatile long intervalSeconds = 60;
    private volatile long zoneShards = 1;
    private volatile double homeMultiplier = 0.5;
    private volatile boolean requireZoneOrHome = true;

    public RewardService(TransactionService tx, SchedulerService scheduler, MessageService messages, ZoneService zones) {
        this.tx = tx;
        this.scheduler = scheduler;
        this.messages = messages;
        this.zones = zones;
    }

    public void configure(long intervalSeconds, long zoneShards, double homeMultiplier, boolean requireZoneOrHome) {
        this.intervalSeconds = Math.max(1, intervalSeconds);
        this.zoneShards = Math.max(0, zoneShards);
        this.homeMultiplier = homeMultiplier;
        this.requireZoneOrHome = requireZoneOrHome;
    }

    public void start() {
        var current = generation.incrementAndGet();
        var old = task;
        if (old != null) {
            old.cancel();
        }
        var interval = Duration.ofSeconds(intervalSeconds);
        task = scheduler.asyncRepeating(() -> {
            if (generation.get() != current) {
                return;
            }
            for (var entry : online.entrySet()) {
                var p = entry.getValue();
                scheduler.entity(p, () -> check(p), () -> online.remove(entry.getKey()));
            }
        }, interval, interval);
    }

    private void check(Player p) {
        if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.isInsideVehicle()) {
            return;
        }
        var id = p.getUniqueId();
        if (!zones.isRewardEligible(id, requireZoneOrHome)) {
            return;
        }
        var multiplier = zones.rewardMultiplier(id, homeMultiplier);
        if (multiplier <= 0 || zoneShards <= 0) {
            return;
        }
        var computed = (long) Math.floor(zoneShards * multiplier);
        if (computed < 1 && multiplier > 0) {
            computed = 1;
        }
        final long amount = computed;
        if (amount < 1) {
            return;
        }
        var context = new MutationContext(TransactionType.REWARD, "reward:afk", null,
                "reward:" + id + ':' + (System.currentTimeMillis() / (intervalSeconds * 1000)),
                Map.of("reward", "afk", "mode", zones.mode(id).name()));
        tx.add(id, amount, context).whenComplete((r, e) -> scheduler.entity(p, () -> {
            if (e == null && r.success()) {
                var placeholders = Map.of(
                        "amount", Long.toString(amount),
                        "next_reward", Long.toString(intervalSeconds),
                        "time", Long.toString(intervalSeconds)
                );
                p.sendActionBar(messages.renderKey("reward", placeholders));
                p.showTitle(Title.title(
                        messages.renderKey("reward-title", placeholders),
                        messages.renderKey("next-reward", placeholders),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400))
                ));
                p.playSound(p.getLocation(), RegistryLookups.sound(
                        "entity.experience_orb.pickup", Sound.ENTITY_EXPERIENCE_ORB_PICKUP), 0.7f, 1.3f);
            }
        }, () -> {
        }));
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        online.put(e.getPlayer().getUniqueId(), e.getPlayer());
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        online.remove(e.getPlayer().getUniqueId());
        tx.forget(e.getPlayer().getUniqueId());
    }

    public void stop() {
        generation.incrementAndGet();
        if (task != null) {
            task.cancel();
        }
        online.clear();
    }
}
