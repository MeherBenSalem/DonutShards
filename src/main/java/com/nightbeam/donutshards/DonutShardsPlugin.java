package com.nightbeam.donutshards;

import com.nightbeam.donutshards.api.DefaultDonutShardsApi;
import com.nightbeam.donutshards.api.DonutShardsApi;
import com.nightbeam.donutshards.board.BoardService;
import com.nightbeam.donutshards.booster.BoosterService;
import com.nightbeam.donutshards.command.CommandRouter;
import com.nightbeam.donutshards.config.YamlKeyMerger;
import com.nightbeam.donutshards.database.DatabaseConfig;
import com.nightbeam.donutshards.database.DatabaseManager;
import com.nightbeam.donutshards.gui.LeaderboardGui;
import com.nightbeam.donutshards.integration.placeholder.DonutShardsExpansion;
import com.nightbeam.donutshards.integration.vault.VaultHook;
import com.nightbeam.donutshards.reward.KillRewardListener;
import com.nightbeam.donutshards.reward.KillRewardService;
import com.nightbeam.donutshards.reward.RewardService;
import com.nightbeam.donutshards.scheduler.SchedulerFactory;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.service.ConversionService;
import com.nightbeam.donutshards.service.MessageService;
import com.nightbeam.donutshards.service.PlayerPrefsStore;
import com.nightbeam.donutshards.shop.ShopService;
import com.nightbeam.donutshards.transaction.TransactionRepository;
import com.nightbeam.donutshards.transaction.TransactionService;
import com.nightbeam.donutshards.util.ModrinthUpdateChecker;
import com.nightbeam.donutshards.zone.ZoneListener;
import com.nightbeam.donutshards.zone.ZoneParticleService;
import com.nightbeam.donutshards.zone.ZoneService;
import com.nightbeam.donutshards.zone.ZoneWand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
    private volatile PlayerPrefsStore playerPrefs;
    private volatile BoosterService boosters;
    private volatile BoardService boards;
    private final CommandRouter.TransferRules transfers = new CommandRouter.TransferRules();
    private final AtomicLong topCacheMillis = new AtomicLong(60_000);
    private final AtomicInteger tax = new AtomicInteger();
    private final AtomicInteger leaderboardPageSize = new AtomicInteger(10);
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
            YamlKeyMerger.mergeAll(this);
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
            playerPrefs = new PlayerPrefsStore(getDataFolder(), getLogger());
            zones = new ZoneService(this, new File(getDataFolder(), "zones.yml"));
            zones.setAutoRejoinInZone(config.getBoolean("afk.auto-rejoin-in-zone", true));
            zones.setPreferredTeleportZone(config.getString("afk.zone-teleport.preferred-zone", ""));
            zones.setAnnouncer(messages, scheduler);
            var vault = new VaultHook();
            if (vault.hook()) {
                getLogger().info("Vault economy hook enabled.");
            } else {
                getLogger().info("Vault not found; shard conversion disabled.");
            }
            conversion = new ConversionService(tx, vault);
            conversion.reload(config);
            boosters = new BoosterService(getDataFolder(), getLogger(), messages, scheduler);
            boosters.configure(config);
            killRewards = new KillRewardService(tx, messages, new File(getDataFolder(), "kills.yml"), boosters, scheduler);
            var api = new DefaultDonutShardsApi(tx, zones, tax, boosters::multiplier);
            shop = new ShopService(this, tx, scheduler, messages, playerPrefs,
                    new File(getDataFolder(), "shop.yml"), new File(getDataFolder(), "gui.yml"));
            shop.serverId(config.getString("server-id", "server-1"));
            rewards = new RewardService(tx, scheduler, messages, zones, boosters);
            rewards.configure(config);
            particles = new ZoneParticleService(this, scheduler, zones);
            particles.configure(config);
            boards = new BoardService(this, new File(getDataFolder(), "boards.yml"), tx, scheduler, messages);
            boards.configure(config);
            var wand = new ZoneWand(this, messages);
            var leaderboard = new LeaderboardGui(tx, scheduler, messages, leaderboardPageSize, shop::gui);
            var commands = new CommandRouter(tx, scheduler, messages, zones, shop, conversion, playerPrefs, leaderboard,
                    tax, homeModeEnabled, this::reloadConfigs, wand, boosters, boards, rewards, transfers);
            var rewardService = rewards;
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
                var plugins = getServer().getPluginManager();
                plugins.registerEvents(shop, this);
                plugins.registerEvents(rewardService, this);
                plugins.registerEvents(leaderboard, this);
                plugins.registerEvents(wand, this);
                plugins.registerEvents(new ZoneListener(zones, messages, scheduler), this);
                plugins.registerEvents(new KillRewardListener(killRewards), this);
                getServer().getServicesManager().register(DonutShardsApi.class, api, this, ServicePriority.Normal);
                registerPlaceholderApi(tx);
                if (config.getBoolean("bstats", true)) {
                    enableBstats();
                }
                ModrinthUpdateChecker.checkAsync(this, scheduler, config.getBoolean("update-check", true));
                // Players already online (plugin loaded late) are tracked like fresh joins.
                for (var player : getServer().getOnlinePlayers()) {
                    rewardService.track(player);
                    tx.touch(player.getUniqueId(), player.getName());
                }
                rewardService.start();
                particles.start();
                boards.start();
                getLogger().info("Database connected and DonutShards is ready.");
            });
        } catch (Throwable error) {
            getLogger().log(Level.SEVERE, "DonutShards could not initialize; commands remain unavailable.", error);
        }
    }

    private void registerPlaceholderApi(TransactionService tx) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        new DonutShardsExpansion(tx, zones, rewards, boosters, getDescription().getVersion(), topCacheMillis).register();
        getLogger().info("PlaceholderAPI expansion registered (donutshard_*).");
    }

    private void enableBstats() {
        try {
            new org.bstats.bukkit.Metrics(this, 33616);
        } catch (Throwable error) {
            getLogger().warning("bStats metrics could not be enabled: " + error.getMessage());
        }
    }

    public void reloadConfigs() {
        YamlKeyMerger.mergeAll(this);
        var config = loadConfig();
        applyConfig(config);
        if (messages != null) {
            messages.load(new File(getDataFolder(), "messages.yml"));
        }
        if (playerPrefs != null) {
            playerPrefs.load();
        }
        if (conversion != null) {
            conversion.reload(config);
        }
        if (killRewards != null) {
            killRewards.reload();
        }
        if (boosters != null) {
            boosters.configure(config);
        }
        if (boards != null) {
            boards.load();
            boards.configure(config);
            boards.start();
        }
        if (zones != null) {
            zones.reload();
            zones.setAutoRejoinInZone(config.getBoolean("afk.auto-rejoin-in-zone", true));
            zones.setPreferredTeleportZone(config.getString("afk.zone-teleport.preferred-zone", ""));
            if (messages != null && scheduler != null) {
                zones.setAnnouncer(messages, scheduler);
            }
        }
        if (shop != null) {
            shop.serverId(config.getString("server-id", "server-1"));
            shop.reload();
        }
        if (rewards != null) {
            rewards.configure(config);
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
        leaderboardPageSize.set(Math.min(45, Math.max(1, config.getInt("leaderboard.page-size", 10))));
        homeModeEnabled.set(config.getBoolean("afk.home-mode.enabled", true));
        transfers.minimum().set(Math.max(1, config.getLong("transfer.minimum", 1)));
        transfers.maximum().set(Math.max(transfers.minimum().get(), config.getLong("transfer.maximum", 1_000_000)));
        transfers.confirmationThreshold().set(Math.max(0, config.getLong("transfer.confirmation-threshold", 100_000)));
        topCacheMillis.set(Math.max(5, config.getLong("leaderboard.cache-seconds", 60)) * 1000L);
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
        if (boosters != null) {
            boosters.shutdown();
        }
        if (boards != null) {
            boards.shutdown();
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
