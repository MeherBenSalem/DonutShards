package com.nightbeam.donutshards.zone;

import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.service.MessageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public final class ZoneListener implements Listener {
    private final ZoneService zones;
    private final MessageService messages;
    private final SchedulerService scheduler;

    public ZoneListener(ZoneService zones, MessageService messages, SchedulerService scheduler) {
        this.zones = zones;
        this.messages = messages;
        this.scheduler = scheduler;
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        zones.syncPlayer(e.getPlayer());
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        zones.clearPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void move(PlayerMoveEvent e) {
        var from = e.getFrom();
        var to = e.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        var player = e.getPlayer();
        var id = player.getUniqueId();
        if (zones.isHomeMode(id)) {
            zones.disableHome(id);
            scheduler.entity(player, () -> messages.sendKey(player, "afk-home-cancelled-move", Map.of()), () -> {
            });
        }
        zones.syncPlayer(player);
    }
}
