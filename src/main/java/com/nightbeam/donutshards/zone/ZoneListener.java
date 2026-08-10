package com.nightbeam.donutshards.zone;

import org.bukkit.event.*;import org.bukkit.event.player.*;
public final class ZoneListener implements Listener {private final ZoneService zones;public ZoneListener(ZoneService zones){this.zones=zones;}
 @EventHandler public void join(PlayerJoinEvent e){zones.syncPlayer(e.getPlayer());}
 @EventHandler public void quit(PlayerQuitEvent e){zones.leave(e.getPlayer().getUniqueId());}
 @EventHandler(ignoreCancelled=true)public void move(PlayerMoveEvent e){var f=e.getFrom();var t=e.getTo();if(f.getBlockX()==t.getBlockX()&&f.getBlockY()==t.getBlockY()&&f.getBlockZ()==t.getBlockZ())return;zones.syncPlayer(e.getPlayer());}
}
