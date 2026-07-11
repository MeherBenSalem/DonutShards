package com.nightbeam.donutshards.scheduler;
import org.bukkit.plugin.Plugin;
public final class FoliaSchedulerService extends AbstractModernScheduler {
    public FoliaSchedulerService(Plugin plugin) { super(plugin); }
    public String platform() { return "Folia"; }
}
