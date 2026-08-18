package com.nightbeam.donutshards;

import com.nightbeam.donutshards.api.DefaultDonutShardsApi;
import com.nightbeam.donutshards.api.DonutShardsApi;
import com.nightbeam.donutshards.command.CommandRouter;
import com.nightbeam.donutshards.database.DatabaseConfig;
import com.nightbeam.donutshards.database.DatabaseManager;
import com.nightbeam.donutshards.integration.vault.VaultHook;
import com.nightbeam.donutshards.reward.KillRewardListener;
import com.nightbeam.donutshards.reward.KillRewardService;
import com.nightbeam.donutshards.reward.RewardService;
import com.nightbeam.donutshards.scheduler.SchedulerFactory;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.service.ConversionService;
import com.nightbeam.donutshards.service.MessageService;
import com.nightbeam.donutshards.shop.ShopService;
import com.nightbeam.donutshards.transaction.TransactionRepository;
import com.nightbeam.donutshards.transaction.TransactionService;
import com.nightbeam.donutshards.zone.ZoneListener;
import com.nightbeam.donutshards.zone.ZoneParticleService;
import com.nightbeam.donutshards.zone.ZoneService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class DonutShardsPlugin extends JavaPlugin {
    private final AtomicBoolean stopping = new AtomicBoolean();
    private SchedulerService scheduler;
    private volatile DatabaseManager database;
    private volatile RewardService rewards;
    private volatile ShopService shop;
    private volatile ZoneService zones;
    private volatile ConversionService conversion;
    private volatile KillRewardService killRewards;
    private volatile MessageService messages;
    private volatile ZoneParticleService particles;
    private final AtomicInteger tax = new AtomicInteger();
    private final AtomicBoolean homeModeEnabled = new AtomicBoolean(true);

    @Override
    public void onEnable() {
        scheduler = SchedulerFactory.create(this);
        getLogger().info("Starting DonutShards " + getDescription().getVersion() + " on " + scheduler.platform() + " with region-safe scheduling.");
        scheduler.async(this::bootstrap);
    }

    private void bootstrap() {
        try {
            getDataFolder().mkdirs();
            for (var file : List.of("config.yml", "database.yml", "messages.yml", "rewards.yml", "zones.yml", "shop.yml", "gui.yml", "anti-abuse.yml", "kills.yml")) {
                if (!new File(getDataFolder(), file).exists()) {
                    saveResource(file, false);
                }
            }
            var config = loadConfig();
            applyConfig(config);
            var db = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "database.yml"));
            var type = DatabaseConfig.Type.valueOf(db.getString("type", "SQLITE").toUpperCase());
            var databaseConfig = new DatabaseConfig(type, db.getString("sqlite-file", "donutshards.db"), db.getString("host", "127.0.0.1"),
                    db.getInt("port", 3306), db.getString("database", "donutshards"), db.getString("username", "donutshards"),
                    db.getString("password", ""), Math.max(1, db.getInt("pool-size", 10)), db.getLong("connection-timeout-ms", 10000));
            database = new DatabaseManager(databaseConfig, getDataFolder().toPath());
            database.migrate();
            if (stopping.get()) {
                database.close();
                return;
            }
            var repository = new TransactionRepository(database, config.getString("server-id", "server-1"), config.getLong("maximum-balance", 9_000_000_000_000_000L));
            var tx = new TransactionService(repository, scheduler);
            messages = new MessageService();
            messages.load(new File(getDataFolder(), "messages.yml"));
            zones = new ZoneService(this, new File(getDataFolder(), "zones.yml"));
            zones.setAutoRejoinInZone(config.getBoolean("afk.auto-rejoin-in-zone", true));
            var vault = new VaultHook();
            if (vault.hook()) {
                getLogger().info("Vault economy hook enabled.");
            } else {
                getLogger().info("Vault not found; shard conversion disabled.");
            }
            conversion = new ConversionService(tx, vault);
            conversion.reload(config);
            killRewards = new KillRewardService(tx, messages, new File(getDataFolder(), "kills.yml"));
            var api = new DefaultDonutShardsApi(tx, zones, tax.get());
            shop = new ShopService(this, tx, scheduler, messages, new File(getDataFolder(), "shop.yml"));
            rewards = new RewardService(tx, scheduler, messages, zones);
            rewards.configure(config.getLong("rewards.interval-seconds", 60), config.getLong("rewards.zone-shards", 1),
                    config.getDouble("afk.home-mode.shard-multiplier", 0.5), config.getBoolean("rewards.require-zone-or-home", true));
            particles = new ZoneParticleService(this, scheduler, zones);
            particles.configure(config);
            var commands = new CommandRouter(tx, scheduler, messages, zones, shop, conversion, tax, homeModeEnabled, this::reloadConfigs);
            scheduler.global(() -> {
                if (stopping.get()) {
                    return;
                }
                for (var name : List.of("shards", "shardshop", "afk", "shardmanager")) {
                    var command = getCommand(name);
                    if (command != null) {
                        command.setExecutor(commands);
                        command.setTabCompleter(commands);
                    }
                }
                getServer().getPluginManager().registerEvents(shop, this);
                getServer().getPluginManager().registerEvents(rewards, this);
                getServer().getPluginManager().registerEvents(new ZoneListener(zones), this);
                getServer().getPluginManager().registerEvents(new KillRewardListener(killRewards), this);
                getServer().getServicesManager().register(DonutShardsApi.class, api, this, ServicePriority.Normal);
                rewards.start();
                particles.start();
                getLogger().info("Database connected and DonutShards is ready.");
            });
        } catch (Throwable error) {
            getLogger().log(Level.SEVERE, "DonutShards could not initialize; commands remain unavailable.", error);
        }
    }

    public void reloadConfigs() {
        var config = loadConfig();
        applyConfig(config);
        if (messages != null) {
            messages.load(new File(getDataFolder(), "messages.yml"));
        }
        if (conversion != null) {
            conversion.reload(config);
        }
        if (killRewards != null) {
            killRewards.reload();
        }
        if (zones != null) {
            zones.reload();
            zones.setAutoRejoinInZone(config.getBoolean("afk.auto-rejoin-in-zone", true));
        }
        if (shop != null) {
            shop.reload();
        }
        if (rewards != null) {
            rewards.configure(config.getLong("rewards.interval-seconds", 60), config.getLong("rewards.zone-shards", 1),
                    config.getDouble("afk.home-mode.shard-multiplier", 0.5), config.getBoolean("rewards.require-zone-or-home", true));
            rewards.start();
        }
        if (particles != null) {
            particles.configure(config);
            particles.start();
        }
        getLogger().info("DonutShards configuration reloaded.");
    }

    private void applyConfig(YamlConfiguration config) {
        tax.set(Math.max(0, Math.min(10000, config.getInt("transfer.tax-basis-points", 0))));
        homeModeEnabled.set(config.getBoolean("afk.home-mode.enabled", true));
    }

    private YamlConfiguration loadConfig() {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
    }

    @Override
    public void onDisable() {
        stopping.set(true);
        if (particles != null) {
            particles.stop();
        }
        if (rewards != null) {
            rewards.stop();
        }
        if (shop != null) {
            shop.clear();
        }
        if (zones != null) {
            zones.clear();
        }
        if (scheduler != null) {
            scheduler.cancelAll();
        }
        var db = database;
        if (db != null) {
            db.close();
        }
        getServer().getServicesManager().unregisterAll(this);
        getLogger().info("DonutShards shut down cleanly.");
    }
}
