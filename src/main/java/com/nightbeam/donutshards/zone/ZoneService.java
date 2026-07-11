package com.nightbeam.donutshards.zone;
import java.util.*;import java.util.concurrent.ConcurrentHashMap;
public final class ZoneService {private final Map<UUID,String> current=new ConcurrentHashMap<>();public boolean isInZone(UUID id){return current.containsKey(id);}public Optional<String> current(UUID id){return Optional.ofNullable(current.get(id));}public void enter(UUID id,String zone){current.put(id,zone);}public Optional<String> leave(UUID id){return Optional.ofNullable(current.remove(id));}public void clear(){current.clear();}}
