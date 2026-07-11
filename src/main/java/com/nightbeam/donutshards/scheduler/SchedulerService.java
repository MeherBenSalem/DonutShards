package com.nightbeam.donutshards.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import java.time.Duration;

public interface SchedulerService {
    TaskHandle global(Runnable task);
    TaskHandle globalRepeating(Runnable task, long delayTicks, long periodTicks);
    TaskHandle region(Location location, Runnable task);
    boolean entity(Entity entity, Runnable task, Runnable retired);
    TaskHandle async(Runnable task);
    TaskHandle asyncRepeating(Runnable task, Duration delay, Duration period);
    void cancelAll();
    String platform();
}
