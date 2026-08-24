package com.nightbeam.donutshards.command;

import com.nightbeam.donutshards.model.MutationContext;
import com.nightbeam.donutshards.model.TransactionType;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.service.ConversionService;
import com.nightbeam.donutshards.service.MessageService;
import com.nightbeam.donutshards.service.PlayerPrefsStore;
import com.nightbeam.donutshards.shop.ShopService;
import com.nightbeam.donutshards.transaction.TransactionService;
import com.nightbeam.donutshards.zone.ZoneService;
import org.bukkit.Bukkit;
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
import java.util.stream.Collectors;

public final class CommandRouter implements CommandExecutor, TabCompleter {
    private final TransactionService tx;
    private final SchedulerService scheduler;
    private final MessageService messages;
    private final ZoneService zones;
    private final ShopService shop;
    private final ConversionService conversion;
    private final PlayerPrefsStore prefs;
    private final AtomicInteger tax;
    private final AtomicBoolean homeModeEnabled;
    private final Runnable reload;

    public CommandRouter(TransactionService tx, SchedulerService scheduler, MessageService messages, ZoneService zones,
                         ShopService shop, ConversionService conversion, PlayerPrefsStore prefs, AtomicInteger tax,
                         AtomicBoolean homeModeEnabled, Runnable reload) {
        this.tx = tx;
        this.scheduler = scheduler;
        this.messages = messages;
        this.zones = zones;
        this.shop = shop;
        this.conversion = conversion;
        this.prefs = prefs;
        this.tax = tax;
        this.homeModeEnabled = homeModeEnabled;
        this.reload = reload;
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

    private boolean shards(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.sendKey(sender, "players-only", Map.of());
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("balance")) {
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
            return true;
        }
        if (args[0].equalsIgnoreCase("top")) {
            if (!player.hasPermission("shards.top")) {
                messages.sendKey(player, "no-permission", Map.of());
                return true;
            }
            tx.topBalances(10).whenComplete((entries, error) -> reply(player, () -> {
                if (error != null || entries.isEmpty()) {
                    messages.sendKey(player, "top-empty", Map.of());
                    return;
                }
                for (int i = 0; i < entries.size(); i++) {
                    var entry = entries.get(i);
                    var offline = Bukkit.getOfflinePlayer(entry.player());
                    var name = offline.getName() == null ? entry.player().toString() : offline.getName();
                    messages.sendKey(player, "top-entry", Map.of(
                            "rank", Integer.toString(i + 1),
                            "player", name,
                            "balance", Long.toString(entry.balance())
                    ));
                }
            }));
            return true;
        }
        if (args[0].equalsIgnoreCase("confirmation") && args.length == 2) {
            if (args[1].equalsIgnoreCase("on")) {
                prefs.setShopConfirmation(player.getUniqueId(), true);
                messages.sendKey(player, "confirmation-enabled", Map.of());
            } else if (args[1].equalsIgnoreCase("off")) {
                prefs.setShopConfirmation(player.getUniqueId(), false);
                messages.sendKey(player, "confirmation-disabled", Map.of());
            } else {
                messages.sendKey(player, "confirmation-usage", Map.of());
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("pay") && args.length == 3) {
            var target = Bukkit.getPlayerExact(args[1]);
            var amount = parse(args[2]);
            if (target == null || amount < 1) {
                messages.sendKey(player, "pay-usage", Map.of());
                return true;
            }
            var ctx = new MutationContext(TransactionType.PLAYER_TRANSFER, "player-transfer", player.getUniqueId(),
                    "pay:" + UUID.randomUUID(), Map.of());
            tx.transfer(player.getUniqueId(), target.getUniqueId(), amount, tax.get(), ctx).whenComplete((result, error) -> reply(player, () -> {
                if (error == null && result.success()) {
                    messages.sendKey(player, "paid", Map.of("amount", Long.toString(amount), "player", target.getName(),
                            "tax", Long.toString(result.tax())));
                } else {
                    messages.sendKey(player, "pay-failed", Map.of("reason", error == null ? result.reason() : "storage_error"));
                }
            }));
            return true;
        }
        if (args[0].equalsIgnoreCase("convert") && args.length == 3) {
            if (!player.hasPermission("shards.convert")) {
                messages.sendKey(player, "no-permission", Map.of());
                return true;
            }
            if (!conversion.enabled()) {
                messages.sendKey(player, "convert-unavailable", Map.of());
                return true;
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
            return true;
        }
        messages.sendKey(player, "shards-usage", Map.of());
        return true;
    }

    private boolean shop(CommandSender sender) {
        if (sender instanceof Player player) {
            shop.open(player);
        } else {
            messages.sendKey(sender, "players-only", Map.of());
        }
        return true;
    }

    private boolean afk(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.sendKey(sender, "players-only", Map.of());
            return true;
        }
        if (args.length == 0) {
            zones.syncPlayer(player);
            if (zones.isHomeMode(player.getUniqueId())) {
                messages.sendKey(player, "afk-home-active", Map.of());
            } else if (zones.current(player.getUniqueId()).isPresent()) {
                messages.sendKey(player, "afk-in-zone", Map.of("zone", zones.current(player.getUniqueId()).get()));
            } else {
                messages.sendKey(player, "afk-stand-hint", Map.of());
            }
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "home" -> {
                if (!homeModeEnabled.get()) {
                    messages.sendKey(player, "afk-home-disabled", Map.of());
                    yield true;
                }
                zones.enableHome(player);
                messages.sendKey(player, "afk-home-enabled", Map.of());
                yield true;
            }
            case "zone" -> {
                zones.disableHome(player.getUniqueId());
                zones.syncPlayer(player);
                if (zones.current(player.getUniqueId()).isPresent()) {
                    messages.sendKey(player, "afk-rejoined-zone", Map.of("zone", zones.current(player.getUniqueId()).get()));
                } else {
                    messages.sendKey(player, "afk-home-off", Map.of());
                }
                yield true;
            }
            case "list" -> {
                if (!zones.hasZones()) {
                    messages.sendKey(player, "afk-none-configured", Map.of());
                    yield true;
                }
                var names = zones.zones().stream().map(z -> z.name()).sorted().collect(Collectors.joining(", "));
                messages.sendKey(player, names.isBlank() ? "afk-none-configured" : "afk-zones-list", Map.of("zones", names));
                yield true;
            }
            case "join" -> {
                if (!zones.hasZones()) {
                    messages.sendKey(player, "afk-none-configured", Map.of());
                    yield true;
                }
                if (args.length >= 2) {
                    if (!zones.join(player, args[1])) {
                        messages.sendKey(player, "afk-join-failed", Map.of("zone", args[1]));
                    } else {
                        messages.sendKey(player, "afk-joined", Map.of("zone", zones.current(player.getUniqueId()).orElse(args[1])));
                    }
                } else {
                    zones.syncPlayer(player);
                    zones.current(player.getUniqueId()).ifPresentOrElse(
                            zone -> messages.sendKey(player, "afk-joined", Map.of("zone", zone)),
                            () -> messages.sendKey(player, "afk-stand-to-join", Map.of()));
                }
                yield true;
            }
            case "leave" -> {
                zones.disableHome(player.getUniqueId());
                zones.leavePlayer(player).ifPresentOrElse(
                        zone -> messages.sendKey(player, "afk-left", Map.of("zone", zone)),
                        () -> messages.sendKey(player, "afk-not-in-zone", Map.of()));
                yield true;
            }
            case "info" -> {
                if (zones.isHomeMode(player.getUniqueId())) {
                    messages.sendKey(player, "afk-mode-home", Map.of());
                } else {
                    zones.current(player.getUniqueId()).ifPresentOrElse(
                            zone -> messages.sendKey(player, "afk-current-zone", Map.of("zone", zone)),
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

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("shards.admin")) {
            messages.sendKey(sender, "no-permission", Map.of());
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("shards.admin.reload")) {
                messages.sendKey(sender, "no-permission", Map.of());
                return true;
            }
            reload.run();
            messages.sendKey(sender, "admin-reloaded", Map.of());
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("zone")) {
            if (!sender.hasPermission("shards.admin.zone")) {
                messages.sendKey(sender, "no-permission", Map.of());
                return true;
            }
            return zoneAdmin(sender, args);
        }
        if (args.length >= 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("set"))) {
            var target = Bukkit.getOfflinePlayer(args[1]);
            var amount = parse(args[2]);
            if (amount < 0) {
                messages.sendKey(sender, "invalid-number", Map.of());
                return true;
            }
            var type = args[0].equalsIgnoreCase("give") ? TransactionType.ADMIN_GIVE
                    : args[0].equalsIgnoreCase("take") ? TransactionType.ADMIN_TAKE : TransactionType.ADMIN_SET;
            var ctx = new MutationContext(type, "admin:" + args[0].toLowerCase(Locale.ROOT),
                    sender instanceof Player player ? player.getUniqueId() : null, "admin:" + UUID.randomUUID(), Map.of());
            var result = args[0].equalsIgnoreCase("give") ? tx.add(target.getUniqueId(), amount, ctx)
                    : args[0].equalsIgnoreCase("take") ? tx.remove(target.getUniqueId(), amount, ctx)
                    : tx.set(target.getUniqueId(), amount, ctx);
            result.whenComplete((value, error) -> scheduler.global(() ->
                    sender.sendMessage(error == null && value.success() ? "Balance updated." : "Update failed: " + (error == null ? value.reason() : "storage error"))));
            return true;
        }
        sender.sendMessage("/shardmanager give|take|set <player> <amount> | reload | zone create|delete|list <name>");
        return true;
    }

    private boolean zoneAdmin(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/shardmanager zone create|delete|list <name>");
            return true;
        }
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                if (zones.zones().isEmpty()) {
                    sender.sendMessage("No AFK zones configured.");
                } else {
                    zones.zones().stream()
                            .map(z -> z.name() + " @ " + z.world() + " (" + Math.round(z.x()) + ", " + Math.round(z.y()) + ", " + Math.round(z.z()) + ") r=" + z.radius())
                            .forEach(sender::sendMessage);
                }
                yield true;
            }
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    messages.sendKey(sender, "players-only", Map.of());
                    yield true;
                }
                if (args.length < 3) {
                    sender.sendMessage("Usage: /shardmanager zone create <name>");
                    yield true;
                }
                try {
                    if (zones.createZone(args[2], player.getLocation(), zones.defaultRadius())) {
                        sender.sendMessage("Created AFK zone " + args[2] + " (radius " + zones.defaultRadius() + ").");
                    } else {
                        sender.sendMessage("Zone already exists or name is invalid.");
                    }
                } catch (Exception e) {
                    sender.sendMessage("Could not save zone: " + e.getMessage());
                }
                yield true;
            }
            case "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /shardmanager zone delete <name>");
                    yield true;
                }
                try {
                    if (zones.deleteZone(args[2])) {
                        sender.sendMessage("Deleted AFK zone " + args[2] + ".");
                    } else {
                        sender.sendMessage("Zone not found.");
                    }
                } catch (Exception e) {
                    sender.sendMessage("Could not save zones: " + e.getMessage());
                }
                yield true;
            }
            default -> {
                sender.sendMessage("/shardmanager zone create|delete|list <name>");
                yield true;
            }
        };
    }

    private void reply(Player player, Runnable task) {
        scheduler.entity(player, task, () -> {
        });
    }

    private long parse(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return command.getName().equals("shardmanager")
                    ? List.of("give", "take", "set", "reset", "reload", "history", "rollback", "zone", "migrate", "debug", "version", "gui")
                    : command.getName().equals("afk")
                    ? List.of("list", "join", "leave", "home", "zone", "info")
                    : List.of("balance", "pay", "top", "history", "convert", "confirmation");
        }
        if (args.length == 2 && command.getName().equals("shards") && args[0].equalsIgnoreCase("convert")) {
            return List.of("shards", "money");
        }
        if (args.length == 2 && command.getName().equals("shards") && args[0].equalsIgnoreCase("confirmation")) {
            return List.of("on", "off");
        }
        if (args.length == 2 && command.getName().equals("shardmanager") && args[0].equalsIgnoreCase("zone")) {
            return List.of("create", "delete", "list");
        }
        if (args.length == 3 && command.getName().equals("shardmanager") && args[0].equalsIgnoreCase("zone")
                && (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("join"))) {
            return zones.zones().stream().map(z -> z.name()).toList();
        }
        if (args.length == 2 && command.getName().equals("afk") && args[0].equalsIgnoreCase("join")) {
            return zones.zones().stream().map(z -> z.name()).toList();
        }
        return List.of();
    }
}
