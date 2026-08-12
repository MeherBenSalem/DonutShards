package com.nightbeam.donutshards.reward;
import org.bukkit.entity.*;import org.bukkit.event.*;import org.bukkit.event.entity.CreatureSpawnEvent;import org.bukkit.event.entity.EntityDeathEvent;
public final class KillRewardListener implements Listener {private final KillRewardService kills;public KillRewardListener(KillRewardService kills){this.kills=kills;}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.MONITOR)public void onDeath(EntityDeathEvent event){if(!kills.enabled())return;var killer=event.getEntity().getKiller();if(killer==null||!killer.isOnline())return;if(event.getEntity() instanceof Player victim){if(victim.getUniqueId().equals(killer.getUniqueId()))return;kills.handlePlayerKill(killer,victim);return;}if(event.getEntity() instanceof LivingEntity living&&living.getEntitySpawnReason()==CreatureSpawnEvent.SpawnReason.SPAWNER)return;if(event.getEntity() instanceof Monster||event.getEntity() instanceof Animals)kills.handleMobKill(killer,event.getEntityType());}
}
