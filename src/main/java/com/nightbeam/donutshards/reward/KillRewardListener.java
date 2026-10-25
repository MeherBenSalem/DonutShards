package com.nightbeam.donutshards.reward;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public final class KillRewardListener implements Listener {
    private final KillRewardService kills;

    public KillRewardListener(KillRewardService kills) {
        this.kills = kills;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (!kills.enabled()) {
            return;
        }
        var entity = event.getEntity();
        var killer = entity.getKiller();
        if (killer == null || !killer.isOnline()) {
            return;
        }
        if (entity instanceof Player victim) {
            if (!victim.getUniqueId().equals(killer.getUniqueId())) {
                kills.handlePlayerKill(killer, victim);
            }
            return;
        }
        if (entity.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        if (entity instanceof Monster || entity instanceof Animals) {
            kills.handleMobKill(killer, entity.getType());
        }
    }
}
