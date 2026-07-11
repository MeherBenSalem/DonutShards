package com.nightbeam.donutshards.event;
import org.bukkit.entity.Player;import org.bukkit.event.*;
public final class AfkZoneLeaveEvent extends Event {private static final HandlerList HANDLERS=new HandlerList();private final Player player;private final String zone;public AfkZoneLeaveEvent(Player player,String zone){this.player=player;this.zone=zone;}public Player player(){return player;}public String zone(){return zone;}public HandlerList getHandlers(){return HANDLERS;}public static HandlerList getHandlerList(){return HANDLERS;}}
