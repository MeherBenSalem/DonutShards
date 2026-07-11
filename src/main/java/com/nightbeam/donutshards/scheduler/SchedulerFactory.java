package com.nightbeam.donutshards.scheduler;
import org.bukkit.plugin.Plugin;
public final class SchedulerFactory {
    private SchedulerFactory() {}
    public static SchedulerService create(Plugin plugin) {
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return new FoliaSchedulerService(plugin); }
        catch (ClassNotFoundException ignored) { return new PaperSchedulerService(plugin); }
    }
}
