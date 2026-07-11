package com.nightbeam.donutshards.scheduler;

import org.bukkit.Server;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

abstract class AbstractModernScheduler implements SchedulerService {
    protected final Plugin plugin;
    protected final Server server;
    AbstractModernScheduler(Plugin plugin) { this.plugin = plugin; this.server = plugin.getServer(); }
    public TaskHandle global(Runnable task) {
        var scheduled = server.getGlobalRegionScheduler().run(plugin, ignored -> task.run());
        return () -> scheduled.cancel();
    }
    public TaskHandle globalRepeating(Runnable task, long delay, long period) {
        var scheduled = server.getGlobalRegionScheduler().runAtFixedRate(plugin, ignored -> task.run(), Math.max(1, delay), Math.max(1, period));
        return () -> scheduled.cancel();
    }
    public TaskHandle region(Location location, Runnable task) {
        var scheduled = server.getRegionScheduler().run(plugin, location, ignored -> task.run());
        return () -> scheduled.cancel();
    }
    public boolean entity(Entity entity, Runnable task, Runnable retired) {
        return entity.getScheduler().execute(plugin, task, retired, 0L);
    }
    public TaskHandle async(Runnable task) {
        var scheduled = server.getAsyncScheduler().runNow(plugin, ignored -> task.run());
        return () -> scheduled.cancel();
    }
    public TaskHandle asyncRepeating(Runnable task, Duration delay, Duration period) {
        var scheduled = server.getAsyncScheduler().runAtFixedRate(plugin, ignored -> task.run(), delay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
        return () -> scheduled.cancel();
    }
    public void cancelAll() {
        server.getGlobalRegionScheduler().cancelTasks(plugin);
        server.getAsyncScheduler().cancelTasks(plugin);
    }
}
