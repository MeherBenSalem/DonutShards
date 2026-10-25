package com.nightbeam.donutshards.command;

import com.nightbeam.donutshards.board.BoardService;
import com.nightbeam.donutshards.booster.BoosterService;
import com.nightbeam.donutshards.core.BoosterState;
import com.nightbeam.donutshards.core.DurationText;
import com.nightbeam.donutshards.core.HistoryPage;
import com.nightbeam.donutshards.gui.LeaderboardGui;
import com.nightbeam.donutshards.model.MutationContext;
import com.nightbeam.donutshards.model.ShardTransaction;
import com.nightbeam.donutshards.model.TransactionType;
import com.nightbeam.donutshards.reward.RewardService;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.service.ConversionService;
import com.nightbeam.donutshards.service.MessageService;
import com.nightbeam.donutshards.service.PlayerPrefsStore;
import com.nightbeam.donutshards.shop.ShopService;
import com.nightbeam.donutshards.transaction.TransactionService;
import com.nightbeam.donutshards.zone.ZoneService;
import com.nightbeam.donutshards.zone.ZoneWand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class CommandRouter implements CommandExecutor, TabCompleter {
    private static final int HISTORY_PAGE_SIZE = 8;

    /** Transfer limits from {@code transfer.*} in config.yml. */
    public record TransferRules(AtomicLong minimum, AtomicLong maximum, AtomicLong confirmationThreshold) {
        public TransferRules() {
            this(new AtomicLong(1), new AtomicLong(1_000_000), new AtomicLong(100_000));
        }
    }

    private final TransactionService tx;
    private final SchedulerService scheduler;
    private final MessageService messages;
    private final ZoneService zones;
    private final ShopService shop;
    private final ConversionService conversion;
    private final PlayerPrefsStore prefs;
    private final LeaderboardGui leaderboard;
    private final AtomicInteger tax;
    private final AtomicBoolean homeModeEnabled;
    private final Runnable reload;
    private final ZoneWand wand;
    private final BoosterService boosters;
    private final BoardService boards;
    private final RewardService rewards;
    private final TransferRules transfers;

    public CommandRouter(TransactionService tx, SchedulerService scheduler, MessageService messages, ZoneService zones,
                         ShopService shop, ConversionService conversion, PlayerPrefsStore prefs, LeaderboardGui leaderboard,
                         AtomicInteger tax, AtomicBoolean homeModeEnabled, Runnable reload, ZoneWand wand,
                         BoosterService boosters, BoardService boards, RewardService rewards, TransferRules transfers) {
        this.tx = tx;
        this.scheduler = scheduler;
        this.messages = messages;
        this.zones = zones;
        this.shop = shop;
        this.conversion = conversion;
        this.prefs = prefs;
        this.leaderboard = leaderboard;
        this.tax = tax;
        this.homeModeEnabled = homeModeEnabled;
        this.reload = reload;
        this.wand = wand;
        this.boosters = boosters;
        this.boards = boards;
        this.rewards = rewards;
        this.transfers = transfers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName()) {
            case "shards" -> shards(sender, args);
            case "shardshop" -> shop(sender);
            case "afk" -> afk(sender, args);
            case "shardmanager" -> admin(sender, args);
            default -> false;
        };
    }

    // ---------------------------------------------------------------------------------------------------- /shards

    private boolean shards(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.sendKey(sender, "players-only", Map.of());
            return true;
        }
        var sub = args.length == 0 ? "balance" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "balance" -> balance(player, args);
            case "top" -> {
                if (!player.hasPermission("shards.top")) {
                    messages.sendKey(player, "no-permission", Map.of());
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("chat")) {
                    leaderboard.sendChat(player);
                } else {
                    leaderboard.open(player);
                }
            }
            case "history" -> {
                if (!player.hasPermission("shards.history")) {
                    messages.sendKey(player, "no-permission", Map.of());
                } else {
                    history(player, player.getUniqueId(), HistoryPage.parse(args.length >= 2 ? args[1] : null, HISTORY_PAGE_SIZE));
                }
            }
            case "booster" -> boosters.active().ifPresentOrElse(
                    b -> messages.sendKey(player, "booster-info", boosters.values(b, System.currentTimeMillis())),
                    () -> messages.sendKey(player, "booster-none", Map.of()));
            case "confirmation" -> confirmation(player, args);
            case "pay" -> pay(player, args);
            case "convert" -> convert(player, args);
            default -> messages.sendKey(player, "shards-usage", Map.of());
        }
        return true;
    }

    private void balance(Player player, String[] args) {
        var target = player;
        if (args.length > 1 && player.hasPermission("shards.balance.others")) {
            var found = Bukkit.getPlayerExact(args[1]);
            if (found != null) {
                target = found;
            }
        }
        var finalTarget = target;
        tx.balance(target.getUniqueId()).whenComplete((value, error) -> reply(player, () -> {
            if (error == null) {
                messages.sendKey(player, "balance", Map.of("player", finalTarget.getName(), "balance", Long.toString(value)));
            } else {
                messages.sendKey(player, "balance-error", Map.of());
            }
        }));
    }

    private void confirmation(Player player, String[] args) {
        if (args.length == 2 && args[1].equalsIgnoreCase("on")) {
            prefs.setShopConfirmation(player.getUniqueId(), true);
            messages.sendKey(player, "confirmation-enabled", Map.of());
        } else if (args.length == 2 && args[1].equalsIgnoreCase("off")) {
            prefs.setShopConfirmation(player.getUniqueId(), false);
            messages.sendKey(player, "confirmation-disabled", Map.of());
        } else {
            messages.sendKey(player, "confirmation-usage", Map.of());
        }
    }

    private void pay(Player player, String[] args) {
        if (!player.hasPermission("shards.pay")) {
            messages.sendKey(player, "no-permission", Map.of());
            return;
        }
        if (args.length < 3) {
            messages.sendKey(player, "pay-usage", Map.of());
            return;
        }
        var target = Bukkit.getPlayerExact(args[1]);
        var amount = parse(args[2]);
        if (target == null || amount < 1) {
            messages.sendKey(player, "pay-usage", Map.of());
            return;
        }
        var min = transfers.minimum().get();
        var max = transfers.maximum().get();
        if (amount < min || amount > max) {
            messages.sendKey(player, "pay-limits", Map.of("min", Long.toString(min), "max", Long.toString(max)));
            return;
        }
        var confirmed = args.length >= 4 && args[3].equalsIgnoreCase("confirm");
        var threshold = transfers.confirmationThreshold().get();
        if (threshold > 0 && amount >= threshold && !confirmed) {
            messages.sendKey(player, "pay-confirm", Map.of("amount", Long.toString(amount), "player", target.getName()));
            return;
        }
        var event = new com.nightbeam.donutshards.event.ShardTransferEvent(player.getUniqueId(), target.getUniqueId(), amount, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            messages.sendKey(player, "pay-failed", Map.of("reason", "cancelled"));
            return;
        }
        var ctx = new MutationContext(TransactionType.PLAYER_TRANSFER, "player-transfer", player.getUniqueId(),
                "pay:" + UUID.randomUUID(), Map.of());
        var senderName = player.getName();
        tx.transfer(player.getUniqueId(), target.getUniqueId(), amount, tax.get(), ctx).whenComplete((result, error) -> {
            reply(player, () -> {
                if (error == null && result.success()) {
                    messages.sendKey(player, "paid", Map.of("amount", Long.toString(amount), "player", target.getName(),
                            "tax", Long.toString(result.tax())));
                } else {
                    messages.sendKey(player, "pay-failed", Map.of("reason", error == null ? result.reason() : "storage_error"));
                }
            });
            if (error == null && result.success() && target.isOnline()) {
                reply(target, () -> messages.sendKey(target, "pay-received", Map.of("amount", Long.toString(amount), "player", senderName)));
            }
        });
    }

    private void convert(Player player, String[] args) {
        if (!player.hasPermission("shards.convert")) {
            messages.sendKey(player, "no-permission", Map.of());
            return;
        }
        if (!conversion.enabled()) {
            messages.sendKey(player, "convert-unavailable", Map.of());
            return;
        }
        if (args.length != 3) {
            messages.sendKey(player, "shards-usage", Map.of());
            return;
        }
        conversion.convert(player, args[1], args[2]).whenComplete((result, error) -> reply(player, () -> {
            if (error != null || !result.success()) {
                messages.sendKey(player, "convert-failed", Map.of("reason", error == null ? result.reason() : "storage_error"));
            } else if (result.direction().equals("shards-to-money")) {
                messages.sendKey(player, "convert-shards-to-money", Map.of("shards", Long.toString(result.shards()),
                        "dollars", String.format(Locale.US, "%.2f", result.dollars())));
            } else {
                messages.sendKey(player, "convert-money-to-shards", Map.of("shards", Long.toString(result.shards()),
                        "dollars", String.format(Locale.US, "%.2f", result.dollars())));
            }
        }));
    }

    private void history(CommandSender viewer, UUID target, HistoryPage page) {
        await(viewer, tx.history(target, page.size(), page.offset()), (rows, error) -> {
            if (error != null) {
                messages.sendKey(viewer, "balance-error", Map.of());
                return;
            }
            messages.sendKey(viewer, "history-header", Map.of("page", Integer.toString(page.page())));
            if (rows.isEmpty()) {
                messages.sendKey(viewer, "history-empty", Map.of());
            }
            var now = System.currentTimeMillis();
            for (var row : rows) {
                messages.send(viewer, messages.raw(row.amount() >= 0 ? "history-entry-in" : "history-entry-out"), historyValues(row, now));
            }
        });
    }

    static Map<String, String> historyValues(ShardTransaction row, long now) {
        var amount = row.amount() >= 0 ? "+" + row.amount() : Long.toString(row.amount());
        return Map.of(
                "ago", HistoryPage.ago(now - row.timestamp().toEpochMilli()),
                "amount", amount,
                "source", row.source(),
                "type", row.type().name().toLowerCase(Locale.ROOT),
                "balance", Long.toString(row.newBalance()));
    }

    private boolean shop(CommandSender sender) {
        if (sender instanceof Player player) {
            shop.open(player);
        } else {
            messages.sendKey(sender, "players-only", Map.of());
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------- /afk

    private boolean afk(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.sendKey(sender, "players-only", Map.of());
            return true;
        }
        if (args.length == 0) {
            return enableHome(player);
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "home" -> enableHome(player);
            case "zone" -> teleportToZone(player);
            case "list" -> {
                if (!zones.hasZones()) {
                    messages.sendKey(player, "afk-none-configured", Map.of());
                    yield true;
                }
                var names = zones.zones().stream().map(z -> z.name()).collect(Collectors.joining(", "));
                messages.sendKey(player, "afk-zones-list", Map.of("zones", names));
                yield true;
            }
            case "join" -> {
                if (!zones.hasZones()) {
                    messages.sendKey(player, "afk-none-configured", Map.of());
                    yield true;
                }
                var name = args.length >= 2 ? args[1] : "";
                if (zones.join(player, name)) {
                    messages.sendKey(player, "afk-joined", Map.of("zone", zones.label(zones.current(player.getUniqueId()).orElse(name))));
                } else if (name.isEmpty()) {
                    messages.sendKey(player, "afk-stand-to-join", Map.of());
                } else {
                    messages.sendKey(player, "afk-join-failed", Map.of("zone", name.replaceAll("[^A-Za-z0-9_-]", "")));
                }
                yield true;
            }
            case "leave" -> {
                var wasHome = zones.isHomeMode(player.getUniqueId());
                zones.disableHome(player.getUniqueId());
                zones.leavePlayer(player).ifPresentOrElse(
                        zone -> messages.sendKey(player, "afk-left", Map.of("zone", zones.label(zone))),
                        () -> messages.sendKey(player, wasHome ? "afk-home-off" : "afk-not-in-zone", Map.of()));
                yield true;
            }
            case "info", "status" -> {
                if (zones.isHomeMode(player.getUniqueId())) {
                    messages.sendKey(player, "afk-mode-home", Map.of());
                } else {
                    zones.current(player.getUniqueId()).ifPresentOrElse(
                            zone -> messages.sendKey(player, "afk-current-zone", Map.of("zone", zones.label(zone))),
                            () -> messages.sendKey(player, "afk-not-in-zone", Map.of()));
                }
                yield true;
            }
            default -> {
                messages.sendKey(player, "afk-usage", Map.of());
                yield true;
            }
        };
    }

    private boolean enableHome(Player player) {
        if (!homeModeEnabled.get()) {
            messages.sendKey(player, "afk-home-disabled", Map.of());
            return true;
        }
        zones.enableHome(player);
        messages.sendKey(player, "afk-home-enabled", Map.of());
        return true;
    }

    private boolean teleportToZone(Player player) {
        var match = zones.resolveTeleportZone(player.getLocation());
        var dest = match.flatMap(zones::teleportLocation);
        if (dest.isEmpty()) {
            messages.sendKey(player, "afk-zone-teleport-failed", Map.of());
            return true;
        }
        var zoneLabel = match.get().label();
        // Folia-safe: teleportAsync, then run sync/messages on the destination entity thread.
        player.teleportAsync(dest.get()).whenComplete((success, error) -> {
            if (error != null || !Boolean.TRUE.equals(success)) {
                reply(player, () -> messages.sendKey(player, "afk-zone-teleport-failed", Map.of()));
                return;
            }
            reply(player, () -> {
                zones.disableHome(player.getUniqueId());
                zones.syncPlayer(player);
                messages.sendKey(player, "afk-zone-teleport", Map.of("zone", zoneLabel));
            });
        });
        return true;
    }

    // ------------------------------------------------------------------------------------------------ /shardmanager

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("shards.admin")) {
            messages.sendKey(sender, "no-permission", Map.of());
            return true;
        }
        var sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (allowed(sender, "shards.admin.reload")) {
                    reload.run();
                    messages.sendKey(sender, "admin-reloaded", Map.of());
                }
            }
            case "zone" -> {
                if (allowed(sender, "shards.admin.zone")) {
                    zoneAdmin(sender, args);
                }
            }
            case "give", "take", "set" -> {
                if (allowed(sender, "shards.admin." + sub)) {
                    ledgerAdmin(sender, sub, args);
                }
            }
            case "info" -> {
                if (allowed(sender, "shards.admin.info")) {
                    info(sender, args);
                }
            }
            case "history" -> {
                if (allowed(sender, "shards.admin.info")) {
                    if (args.length < 2) {
                        sender.sendMessage("Usage: /shardmanager history <player> [page]");
                    } else {
                        history(sender, Bukkit.getOfflinePlayer(args[1]).getUniqueId(),
                                HistoryPage.parse(args.length >= 3 ? args[2] : null, HISTORY_PAGE_SIZE));
                    }
                }
            }
            case "booster" -> {
                if (allowed(sender, "shards.admin.booster")) {
                    boosterAdmin(sender, args);
                }
            }
            case "board" -> {
                if (allowed(sender, "shards.admin.board")) {
                    boardAdmin(sender, args);
                }
            }
            default -> sender.sendMessage("/shardmanager give|take|set <player> <amount> | info|history <player> | "
                    + "zone wand|pos1|pos2|create|set|delete|list|info | booster start|stop|info | board create|delete|list|refresh | reload");
        }
        return true;
    }

    private boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        messages.sendKey(sender, "no-permission", Map.of());
        return false;
    }

    private void ledgerAdmin(CommandSender sender, String sub, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /shardmanager " + sub + " <player> <amount>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        var amount = parse(args[2]);
        if (amount < 0) {
            messages.sendKey(sender, "invalid-number", Map.of());
            return;
        }
        if (args[1].matches("[A-Za-z0-9_]{1,16}")) {
            // Leaderboards show this name until the player joins (offline players have no cached name).
            tx.touch(target.getUniqueId(), target.getName() != null ? target.getName() : args[1]);
        }
        var type = sub.equals("give") ? TransactionType.ADMIN_GIVE : sub.equals("take") ? TransactionType.ADMIN_TAKE : TransactionType.ADMIN_SET;
        var ctx = new MutationContext(type, "admin:" + sub, sender instanceof Player player ? player.getUniqueId() : null,
                "admin:" + UUID.randomUUID(), Map.of());
        var result = sub.equals("give") ? tx.add(target.getUniqueId(), amount, ctx)
                : sub.equals("take") ? tx.remove(target.getUniqueId(), amount, ctx)
                : tx.set(target.getUniqueId(), amount, ctx);
        await(sender, result, (value, error) -> sender.sendMessage(error == null && value.success()
                ? "Balance updated: " + args[1] + " now has " + value.transaction().newBalance() + " shards."
                : "Update failed: " + (error == null ? value.reason() : "storage error")));
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /shardmanager info <player>");
            return;
        }
        var target = Bukkit.getOfflinePlayer(args[1]);
        var id = target.getUniqueId();
        var online = Bukkit.getPlayer(id);
        await(sender, tx.statistics(id), (stats, error) -> {
            if (error != null) {
                sender.sendMessage("Could not read " + args[1] + ": storage error");
                return;
            }
            sender.sendMessage(args[1] + ": balance=" + stats.balance() + " earned=" + stats.totalEarned() + " spent=" + stats.totalSpent());
            var mode = zones.mode(id).name().toLowerCase(Locale.ROOT);
            var zone = zones.current(id).orElse("-");
            var session = rewards.session(id);
            sender.sendMessage("afk: mode=" + mode + " zone=" + zone
                    + " next=" + (session.isPresent() ? rewards.secondsToNext(id) + "s" : "-")
                    + " session=" + session.map(s -> Long.toString(s.earned())).orElse("-")
                    + (online != null ? " multiplier=" + BoosterState.format(RewardService.rankMultiplier(online)) : "")
                    + " booster=" + BoosterState.format(boosters.multiplier()));
        });
    }

    private void boosterAdmin(CommandSender sender, String[] args) {
        var sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "info";
        switch (sub) {
            case "start" -> {
                if (args.length < 4) {
                    sender.sendMessage("Usage: /shardmanager booster start <multiplier> <duration e.g. 30m, 2h> [reason]");
                    return;
                }
                double multiplier;
                try {
                    multiplier = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    multiplier = -1;
                }
                if (!BoosterState.validMultiplier(multiplier, boosters.maxMultiplier())) {
                    sender.sendMessage("The multiplier must be above 1 and at most " + BoosterState.format(boosters.maxMultiplier()) + ".");
                    return;
                }
                var seconds = DurationText.parseSeconds(args[3]);
                if (seconds.isEmpty()) {
                    sender.sendMessage("Invalid duration: " + args[3] + " (use 90s, 30m, 2h, 1h30m or 1d).");
                    return;
                }
                var reason = args.length > 4 ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)) : "";
                var started = boosters.start(multiplier, seconds.getAsLong(), reason, sender.getName());
                sender.sendMessage("Booster x" + BoosterState.format(started.multiplier()) + " started for "
                        + DurationText.compact(seconds.getAsLong()) + ".");
            }
            case "stop" -> sender.sendMessage(boosters.stop() ? "Booster stopped." : "No booster was running.");
            default -> sender.sendMessage(boosters.active()
                    .map(b -> "Booster x" + BoosterState.format(b.multiplier()) + ", "
                            + DurationText.compact(b.secondsLeft(System.currentTimeMillis())) + " left"
                            + (b.reason().isBlank() ? "" : " (" + b.reason() + ")") + ", started by " + b.startedBy() + ".")
                    .orElse("No booster is running."));
        }
    }

    private void boardAdmin(CommandSender sender, String[] args) {
        var sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "list";
        try {
            switch (sub) {
                case "create" -> {
                    if (args.length < 3) {
                        sender.sendMessage("Usage: /shardmanager board create <name> [world x y z]");
                        return;
                    }
                    Location at;
                    if (args.length >= 7) {
                        var world = Bukkit.getWorld(args[3]);
                        if (world == null) {
                            sender.sendMessage("Unknown world: " + args[3]);
                            return;
                        }
                        at = new Location(world, Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]));
                    } else if (sender instanceof Player player) {
                        at = player.getLocation().add(0, 2, 0);
                    } else {
                        sender.sendMessage("From the console: /shardmanager board create <name> <world> <x> <y> <z>");
                        return;
                    }
                    sender.sendMessage(boards.create(args[2], at) ? "Created leaderboard board " + args[2] + "."
                            : "A board with that name exists or the name is invalid (letters, digits, - and _).");
                }
                case "delete" -> {
                    if (args.length < 3) {
                        sender.sendMessage("Usage: /shardmanager board delete <name>");
                        return;
                    }
                    sender.sendMessage(boards.delete(args[2]) ? "Deleted board " + args[2] + "." : "Board not found.");
                }
                case "refresh" -> {
                    boards.refresh();
                    sender.sendMessage("Boards refreshed.");
                }
                default -> {
                    if (boards.boards().isEmpty()) {
                        sender.sendMessage("No leaderboard boards.");
                    }
                    boards.boards().forEach(b -> {
                        var count = boards.displayCount(b.name());
                        sender.sendMessage(b.name() + " @ " + b.world() + " (" + Math.round(b.x()) + ", " + Math.round(b.y()) + ", "
                                + Math.round(b.z()) + ") " + (count < 0 ? "not loaded" : "displays=" + count));
                    });
                }
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("Coordinates must be numbers.");
        } catch (Exception e) {
            sender.sendMessage("Could not save boards: " + e.getMessage());
        }
    }

    private void zoneAdmin(CommandSender sender, String[] args) {
        var sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        try {
            switch (sub) {
                case "list" -> {
                    if (zones.zones().isEmpty()) {
                        sender.sendMessage("No AFK zones configured.");
                    }
                    zones.zones().forEach(z -> sender.sendMessage(z.name() + " @ " + z.world() + " " + z.describe()
                            + " shards=" + z.settings().shardsOr(-1) + " interval=" + z.settings().intervalOr(-1)
                            + (z.settings().permission().isBlank() ? "" : " permission=" + z.settings().permission())));
                }
                case "info" -> {
                    if (args.length < 3) {
                        sender.sendMessage("Usage: /shardmanager zone info <name>");
                        return;
                    }
                    zones.zone(args[2]).ifPresentOrElse(z -> {
                        sender.sendMessage(z.name() + " @ " + z.world() + " " + z.describe());
                        sender.sendMessage("shards=" + (z.settings().shards() == null ? "default" : z.settings().shards())
                                + " interval=" + (z.settings().intervalSeconds() == null ? "default" : z.settings().intervalSeconds() + "s")
                                + " permission=" + (z.settings().permission().isBlank() ? "none" : z.settings().permission())
                                + " display-name=" + (z.settings().displayName().isBlank() ? "none" : z.settings().displayName()));
                    }, () -> sender.sendMessage("Zone not found."));
                }
                case "wand" -> {
                    if (!(sender instanceof Player player)) {
                        messages.sendKey(sender, "players-only", Map.of());
                        return;
                    }
                    var leftover = player.getInventory().addItem(wand.create());
                    leftover.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
                    messages.sendKey(player, "zone-wand-given", Map.of());
                }
                case "pos1", "pos2" -> {
                    if (!(sender instanceof Player player)) {
                        messages.sendKey(sender, "players-only", Map.of());
                        return;
                    }
                    // The block the admin stands on, so "pos1 here, walk, pos2 here" selects the floor.
                    wand.setCorner(player, sub.equals("pos1"), player.getLocation().subtract(0, 0.5, 0));
                }
                case "create" -> createZone(sender, args);
                case "set" -> setZone(sender, args);
                case "delete" -> {
                    if (args.length < 3) {
                        sender.sendMessage("Usage: /shardmanager zone delete <name>");
                        return;
                    }
                    sender.sendMessage(zones.deleteZone(args[2]) ? "Deleted AFK zone " + args[2] + "." : "Zone not found.");
                }
                default -> sender.sendMessage("/shardmanager zone wand | pos1 | pos2 | create <name> | set <name> <shards|interval|permission|display-name> <value|reset> | info <name> | delete <name> | list");
            }
        } catch (Exception e) {
            sender.sendMessage("Could not save zones: " + e.getMessage());
        }
    }

    private void createZone(CommandSender sender, String[] args) throws Exception {
        if (args.length < 3) {
            sender.sendMessage("Usage: /shardmanager zone create <name>");
            return;
        }
        var name = args[2];
        if (!ZoneService.validName(name)) {
            sender.sendMessage("Zone names use letters, digits, - and _ (up to 32).");
            return;
        }
        if (sender instanceof Player player) {
            var selection = wand.selection(player.getUniqueId());
            var box = selection.box();
            if (box.isPresent()) {
                if (zones.createBox(name, selection.world1(), box.get())) {
                    wand.clear(player.getUniqueId());
                    var b = box.get();
                    sender.sendMessage("Created AFK zone " + name + ": box " + b.describe() + " ("
                            + (b.maxX() - b.minX() + 1) + "×" + (b.maxZ() - b.minZ() + 1) + " floor, "
                            + "players count up to 2 blocks above the top).");
                } else {
                    sender.sendMessage("Zone already exists.");
                }
                return;
            }
            if (zones.createZone(name, player.getLocation(), zones.defaultRadius())) {
                sender.sendMessage("Created AFK zone " + name + ": sphere, radius " + zones.defaultRadius()
                        + " (select two corners with /shardmanager zone wand for a box).");
            } else {
                sender.sendMessage("Zone already exists.");
            }
            return;
        }
        if (args.length >= 10) {
            var world = Bukkit.getWorld(args[3]);
            if (world == null) {
                sender.sendMessage("Unknown world: " + args[3]);
                return;
            }
            var box = com.nightbeam.donutshards.core.ZoneBox.of(Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]),
                    Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]));
            sender.sendMessage(zones.createBox(name, world.getName(), box) ? "Created AFK zone " + name + ": box " + box.describe() + "."
                    : "Zone already exists.");
            return;
        }
        sender.sendMessage("From the console: /shardmanager zone create <name> <world> <x1> <y1> <z1> <x2> <y2> <z2>");
    }

    private void setZone(CommandSender sender, String[] args) throws Exception {
        if (args.length < 5) {
            sender.sendMessage("Usage: /shardmanager zone set <name> <shards|interval|permission|display-name> <value|reset>");
            return;
        }
        var zone = zones.zone(args[2]);
        if (zone.isEmpty()) {
            sender.sendMessage("Zone not found.");
            return;
        }
        var key = args[3].toLowerCase(Locale.ROOT);
        var raw = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
        var reset = raw.equalsIgnoreCase("reset");
        var settings = zone.get().settings();
        switch (key) {
            case "shards" -> {
                var value = parse(raw);
                if (!reset && value < 0) {
                    messages.sendKey(sender, "invalid-number", Map.of());
                    return;
                }
                settings = settings.withShards(reset ? null : value);
            }
            case "interval", "interval-seconds" -> {
                var value = DurationText.parseSeconds(raw);
                if (!reset && value.isEmpty()) {
                    sender.sendMessage("Invalid interval: " + raw + " (seconds, or 30s, 2m...).");
                    return;
                }
                settings = settings.withInterval(reset ? null : value.getAsLong());
            }
            case "permission" -> settings = settings.withPermission(reset ? "" : raw);
            case "display-name", "name" -> settings = settings.withDisplayName(reset ? "" : raw);
            default -> {
                sender.sendMessage("Unknown setting: " + args[3] + " (shards, interval, permission, display-name).");
                return;
            }
        }
        zones.updateSettings(zone.get().name(), settings);
        sender.sendMessage("Updated " + zone.get().name() + ": " + key + " = " + (reset ? "default" : raw) + ".");
    }

    // ------------------------------------------------------------------------------------------------------ helpers

    private void reply(Player player, Runnable task) {
        scheduler.entity(player, task, () -> {
        });
    }

    /**
     * Runs {@code then} with the result: on the player's thread for players; inline for the console and RCON, which
     * only capture output written while the command runs (admin queries wait up to 5 seconds for the database).
     */
    private <T> void await(CommandSender sender, java.util.concurrent.CompletionStage<T> stage, java.util.function.BiConsumer<T, Throwable> then) {
        if (sender instanceof Player player) {
            stage.whenComplete((value, error) -> reply(player, () -> then.accept(value, error)));
            return;
        }
        try {
            then.accept(stage.toCompletableFuture().get(5, java.util.concurrent.TimeUnit.SECONDS), null);
        } catch (java.util.concurrent.ExecutionException e) {
            then.accept(null, e.getCause());
        } catch (Exception e) {
            then.accept(null, e);
        }
    }

    private long parse(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        var name = command.getName();
        var completions = completions(sender, name, args);
        var last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return completions.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(last)).toList();
    }

    private List<String> completions(CommandSender sender, String name, String[] args) {
        var zoneNames = zones.zones().stream().map(z -> z.name()).toList();
        if (args.length == 1) {
            return switch (name) {
                case "shardmanager" -> List.of("give", "take", "set", "info", "history", "zone", "booster", "board", "reload");
                case "afk" -> List.of("list", "join", "leave", "home", "zone", "info", "status");
                case "shards" -> List.of("balance", "pay", "top", "history", "booster", "convert", "confirmation");
                default -> List.of();
            };
        }
        var first = args[0].toLowerCase(Locale.ROOT);
        if (name.equals("shards") && args.length == 2) {
            return switch (first) {
                case "convert" -> List.of("shards", "money");
                case "top" -> List.of("chat");
                case "confirmation" -> List.of("on", "off");
                case "pay", "balance" -> null == sender ? List.of() : Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                default -> List.of();
            };
        }
        if (name.equals("afk") && args.length == 2 && first.equals("join")) {
            return zoneNames;
        }
        if (!name.equals("shardmanager")) {
            return List.of();
        }
        if (args.length == 2) {
            return switch (first) {
                case "zone" -> List.of("wand", "pos1", "pos2", "create", "set", "info", "delete", "list");
                case "booster" -> List.of("start", "stop", "info");
                case "board" -> List.of("create", "delete", "list", "refresh");
                case "give", "take", "set", "info", "history" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                default -> List.of();
            };
        }
        if (first.equals("zone") && args.length == 3 && List.of("set", "info", "delete").contains(args[1].toLowerCase(Locale.ROOT))) {
            return zoneNames;
        }
        if (first.equals("zone") && args.length == 4 && args[1].equalsIgnoreCase("set")) {
            return List.of("shards", "interval", "permission", "display-name");
        }
        if (first.equals("board") && args.length == 3 && args[1].equalsIgnoreCase("delete")) {
            return boards.boards().stream().map(BoardService.Board::name).toList();
        }
        if (first.equals("booster") && args[1].equalsIgnoreCase("start")) {
            return args.length == 3 ? List.of("1.5", "2", "3") : args.length == 4 ? List.of("30m", "1h", "2h") : List.of();
        }
        return List.of();
    }
}
