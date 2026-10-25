package com.nightbeam.donutshards.shop;

import com.nightbeam.donutshards.gui.ConfirmHolder;
import com.nightbeam.donutshards.gui.GuiConfig;
import com.nightbeam.donutshards.gui.ShopHolder;
import com.nightbeam.donutshards.model.MutationContext;
import com.nightbeam.donutshards.model.TransactionType;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.service.MessageService;
import com.nightbeam.donutshards.service.PlayerPrefsStore;
import com.nightbeam.donutshards.transaction.TransactionService;
import com.nightbeam.donutshards.util.RegistryLookups;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import com.nightbeam.donutshards.core.DealRotation;
import com.nightbeam.donutshards.core.DurationText;
import com.nightbeam.donutshards.model.TransactionResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopService implements Listener {
    private final Plugin plugin;
    private final TransactionService tx;
    private final SchedulerService scheduler;
    private final MessageService messages;
    private final PlayerPrefsStore prefs;
    private final File shopFile;
    private final File guiFile;
    private final ShopCatalog catalog = new ShopCatalog();
    private final GuiConfig gui = new GuiConfig();
    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    private final NamespacedKey tokenKey;
    private final NamespacedKey itemKey;
    private static final String DEAL_PREFIX = "deal:";
    private volatile String serverId = "server-1";

    public ShopService(Plugin plugin, TransactionService tx, SchedulerService scheduler, MessageService messages,
                         PlayerPrefsStore prefs, File shopFile, File guiFile) {
        this.plugin = plugin;
        this.tx = tx;
        this.scheduler = scheduler;
        this.messages = messages;
        this.prefs = prefs;
        this.shopFile = shopFile;
        this.guiFile = guiFile;
        this.tokenKey = new NamespacedKey(plugin, "purchase-token");
        this.itemKey = new NamespacedKey(plugin, "shop-item");
        reload();
    }

    public void serverId(String value) {
        serverId = value == null ? "server-1" : value;
    }

    public void reload() {
        catalog.load(shopFile);
        gui.load(guiFile);
    }

    public GuiConfig gui() {
        return gui;
    }

    public ShopCatalog catalog() {
        return catalog;
    }

    /** The deals of the current rotation, in slot order. */
    public List<ShopItem> currentDeals(long now) {
        var deals = catalog.deals();
        if (!deals.active()) {
            return List.of();
        }
        var rotation = DealRotation.rotationIndex(now, deals.rotationHours());
        return DealRotation.pick(deals.pool(), deals.count(), rotation, serverId);
    }

    private String dealScope(long rotation, ShopItem item, UUID player) {
        return "deal:" + rotation + ':' + item.id() + ':' + player;
    }

    public void open(Player player) {
        var now = System.currentTimeMillis();
        var deals = catalog.deals();
        var current = currentDeals(now);
        if (current.isEmpty()) {
            show(player, List.of(), Map.of(), now);
            return;
        }
        var rotation = DealRotation.rotationIndex(now, deals.rotationHours());
        var scopes = current.stream().map(item -> dealScope(rotation, item, player.getUniqueId())).toList();
        tx.limitCounts(scopes).whenComplete((counts, error) -> scheduler.entity(player, () -> {
            var used = new HashMap<String, Long>();
            for (int i = 0; i < current.size(); i++) {
                used.put(current.get(i).id(), counts == null ? 0L : counts.getOrDefault(scopes.get(i), 0L));
            }
            show(player, current, used, now);
        }, () -> {
        }));
    }

    private void show(Player player, List<ShopItem> deals, Map<String, Long> used, long now) {
        var holder = new ShopHolder(UUID.randomUUID());
        var inv = Bukkit.createInventory(holder, catalog.size(), messages.render(catalog.title(), Map.of()));
        holder.inventory(inv);
        for (var item : catalog.items()) {
            if (item.slot() < 0 || item.slot() >= catalog.size()) {
                continue;
            }
            var stack = displayStack(item);
            if (stack != null) {
                inv.setItem(item.slot(), stack);
            }
        }
        var config = catalog.deals();
        var endsIn = (DealRotation.rotationEndsAt(now, config.rotationHours()) - now) / 1000;
        var bypass = player.hasPermission("shards.bypass.limit");
        for (int i = 0; i < deals.size() && i < config.slots().size(); i++) {
            var item = deals.get(i);
            var left = bypass ? -1 : Math.max(0, config.limitPerPlayer() - used.getOrDefault(item.id(), 0L));
            var stack = dealStack(item, endsIn, left, config.limitPerPlayer());
            if (stack != null) {
                inv.setItem(config.slots().get(i), stack);
            }
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        var topHolder = event.getView().getTopInventory().getHolder();
        if (topHolder instanceof ConfirmHolder confirmHolder) {
            handleConfirmClick(event, player, confirmHolder);
            return;
        }
        if (!(topHolder instanceof ShopHolder)) {
            return;
        }
        event.setCancelled(true);
        var clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !processing.add(player.getUniqueId())) {
            return;
        }
        var meta = clicked.getItemMeta();
        var id = meta == null ? null : meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if (id != null && id.startsWith(DEAL_PREFIX)) {
            var dealId = id.substring(DEAL_PREFIX.length());
            var deal = currentDeals(System.currentTimeMillis()).stream().filter(d -> d.id().equals(dealId)).findFirst();
            if (deal.isEmpty()) {
                processing.remove(player.getUniqueId());
                messages.sendKey(player, "deal-expired", Map.of());
                player.closeInventory();
                return;
            }
            beginPurchase(player, deal.get(), true);
            return;
        }
        var item = catalog.byId(id).or(() -> catalog.bySlot(event.getRawSlot()));
        if (item.isEmpty()) {
            processing.remove(player.getUniqueId());
            return;
        }
        beginPurchase(player, item.get(), false);
    }

    @EventHandler
    public void drag(InventoryDragEvent event) {
        var holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof ShopHolder || holder instanceof ConfirmHolder) {
            event.setCancelled(true);
        }
    }

    private void handleConfirmClick(InventoryClickEvent event, Player player, ConfirmHolder holder) {
        event.setCancelled(true);
        if (!processing.add(player.getUniqueId())) {
            return;
        }
        var slot = event.getRawSlot();
        if (slot == gui.confirmSlot()) {
            purchase(player, holder.item(), holder.deal());
            player.closeInventory();
            return;
        }
        if (slot == gui.cancelSlot()) {
            processing.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }
        processing.remove(player.getUniqueId());
    }

    private void beginPurchase(Player player, ShopItem item, boolean deal) {
        if (item.confirmation() && prefs.shopConfirmationEnabled(player.getUniqueId())) {
            processing.remove(player.getUniqueId());
            openConfirmation(player, item, deal);
            return;
        }
        purchase(player, item, deal);
    }

    private void openConfirmation(Player player, ShopItem item, boolean deal) {
        var holder = new ConfirmHolder(UUID.randomUUID(), item, deal);
        var inv = Bukkit.createInventory(holder, 27, messages.renderKey("shop-confirm-title", Map.of()));
        holder.inventory(inv);
        inv.setItem(gui.confirmSlot(), buttonStack(gui.confirmMaterial(), gui.confirmName()));
        inv.setItem(gui.cancelSlot(), buttonStack(gui.cancelMaterial(), gui.cancelName()));
        player.openInventory(inv);
    }

    private ItemStack buttonStack(Material material, String name) {
        var stack = new ItemStack(material);
        var meta = stack.getItemMeta();
        meta.displayName(messages.render(name, Map.of()));
        stack.setItemMeta(meta);
        return stack;
    }

    private void purchase(Player player, ShopItem item, boolean isDeal) {
        var event = new com.nightbeam.donutshards.event.ShardPurchaseEvent(player.getUniqueId(), item.id(), item.price());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            processing.remove(player.getUniqueId());
            return;
        }
        var purchaseId = UUID.randomUUID();
        var ctx = new MutationContext(TransactionType.SHOP_PURCHASE, "shop:" + item.id(), player.getUniqueId(),
                "purchase:" + purchaseId, Map.of("item", item.id()));
        var deals = catalog.deals();
        CompletionStage<TransactionResult> charge;
        if (isDeal) {
            var now = System.currentTimeMillis();
            var rotation = DealRotation.rotationIndex(now, deals.rotationHours());
            var limit = player.hasPermission("shards.bypass.limit") ? Long.MAX_VALUE : deals.limitPerPlayer();
            charge = tx.limitedRemove(player.getUniqueId(), item.price(), ctx, dealScope(rotation, item, player.getUniqueId()),
                    limit, DealRotation.rotationEndsAt(now, deals.rotationHours()));
        } else {
            charge = tx.remove(player.getUniqueId(), item.price(), ctx);
        }
        charge.whenComplete((result, error) -> scheduler.entity(player, () -> {
            try {
                if (error != null || !result.success()) {
                    messages.sendKey(player, "shop-purchase-failed", Map.of("reason", error == null ? result.reason() : "storage_error"));
                    return;
                }
                var stack = giveStack(item, purchaseId);
                if (stack == null) {
                    messages.sendKey(player, "shop-purchase-failed", Map.of("reason", "invalid_material"));
                    return;
                }
                var leftovers = player.getInventory().addItem(stack);
                if (!leftovers.isEmpty()) {
                    leftovers.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
                }
                runCommands(player, item);
                messages.sendKey(player, "shop-purchase-complete", Map.of("balance", Long.toString(result.transaction().newBalance())));
            } finally {
                processing.remove(player.getUniqueId());
            }
        }, () -> processing.remove(player.getUniqueId())));
    }

    private void runCommands(Player player, ShopItem item) {
        if (item.commands().isEmpty()) {
            return;
        }
        var safeName = sanitizePlayerName(player.getName());
        var replacements = Map.of(
                "player", safeName,
                "uuid", player.getUniqueId().toString(),
                "item", item.id().replaceAll("[^a-zA-Z0-9_-]", ""),
                "price", Long.toString(item.price())
        );
        for (var command : item.commands()) {
            if (command == null || command.isBlank()) {
                continue;
            }
            var parsed = command;
            for (var entry : replacements.entrySet()) {
                parsed = parsed.replace("%" + entry.getKey() + "%", entry.getValue());
            }
            if (parsed.indexOf('\n') >= 0 || parsed.indexOf('\r') >= 0) {
                plugin.getLogger().warning("Skipped shop command with control characters for item " + item.id());
                continue;
            }
            final var toRun = parsed;
            scheduler.global(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun));
        }
    }

    static String sanitizePlayerName(String name) {
        if (name == null) {
            return "unknown";
        }
        var cleaned = name.replaceAll("[^A-Za-z0-9_]", "");
        if (cleaned.isEmpty()) {
            return "unknown";
        }
        return cleaned.length() > 16 ? cleaned.substring(0, 16) : cleaned;
    }

    private ItemStack displayStack(ShopItem item) {
        var stack = baseStack(item);
        if (stack == null) {
            return null;
        }
        var meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, item.id());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack dealStack(ShopItem item, long endsInSeconds, long left, long limit) {
        var stack = baseStack(item);
        if (stack == null) {
            return null;
        }
        var meta = stack.getItemMeta();
        var lore = meta.lore() == null ? new ArrayList<Component>() : new ArrayList<>(meta.lore());
        lore.add(messages.renderKey("deal-lore-rotation", Map.of("time", DurationText.compact(endsInSeconds))));
        if (left < 0) {
            lore.add(messages.renderKey("deal-lore-unlimited", Map.of()));
        } else if (left == 0) {
            lore.add(messages.renderKey("deal-lore-sold-out", Map.of("limit", Long.toString(limit))));
        } else {
            lore.add(messages.renderKey("deal-lore-left", Map.of("left", Long.toString(left), "limit", Long.toString(limit))));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, DEAL_PREFIX + item.id());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack giveStack(ShopItem item, UUID purchaseId) {
        var stack = baseStack(item);
        if (stack == null) {
            return null;
        }
        var meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(tokenKey, PersistentDataType.STRING, purchaseId.toString());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack baseStack(ShopItem item) {
        var material = Material.matchMaterial(item.material());
        if (material == null || material.isAir()) {
            plugin.getLogger().warning("Invalid shop material for " + item.id() + ": " + item.material());
            return null;
        }
        var stack = new ItemStack(material, item.amount());
        var meta = stack.getItemMeta();
        meta.displayName(messages.render(item.name(), Map.of()));
        var lore = new ArrayList<Component>();
        for (var line : item.lore()) {
            lore.add(messages.render(line, Map.of()));
        }
        if (item.purchasable()) {
            lore.add(messages.renderKey("shop-price-lore", Map.of("price", Long.toString(item.price()))));
        }
        meta.lore(lore);
        for (var entry : item.enchantments().entrySet()) {
            var enchantment = findEnchantment(entry.getKey());
            if (enchantment != null) {
                meta.addEnchant(enchantment, entry.getValue(), true);
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    static Enchantment findEnchantment(String name) {
        return RegistryLookups.enchantment(name);
    }

    public void clear() {
        processing.clear();
    }
}
