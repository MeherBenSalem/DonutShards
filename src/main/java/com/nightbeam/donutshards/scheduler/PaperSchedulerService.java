package com.nightbeam.donutshards.scheduler;
import org.bukkit.plugin.Plugin;
public final class PaperSchedulerService extends AbstractModernScheduler {
    public PaperSchedulerService(Plugin plugin) { super(plugin); }
    public String platform() { return "Paper"; }
}
