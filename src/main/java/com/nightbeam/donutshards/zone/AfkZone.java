package com.nightbeam.donutshards.zone;

public record AfkZone(String name,String world,double x,double y,double z,double radius){
 public boolean contains(String worldName,double px,double py,double pz){if(world==null||!world.equalsIgnoreCase(worldName))return false;var dx=px-x;var dy=py-y;var dz=pz-z;return dx*dx+dy*dy+dz*dz<=radius*radius;}
}
