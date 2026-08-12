package com.nightbeam.donutshards.integration.vault;
import net.milkbowl.vault.economy.Economy;import org.bukkit.Bukkit;import org.bukkit.plugin.RegisteredServiceProvider;
public final class VaultHook {private Economy economy;
 public boolean hook(){if(Bukkit.getPluginManager().getPlugin("Vault")==null)return false;var rsp=Bukkit.getServicesManager().getRegistration(Economy.class);if(rsp==null)return false;economy=rsp.getProvider();return economy!=null;}
 public boolean available(){return economy!=null;}
 public Economy economy(){return economy;}
}
