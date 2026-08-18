package com.nightbeam.donutshards.shop;

import com.nightbeam.donutshards.gui.ShopHolder;
import com.nightbeam.donutshards.model.MutationContext;
import com.nightbeam.donutshards.model.TransactionType;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.service.MessageService;
import com.nightbeam.donutshards.transaction.TransactionService;
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
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopService implements Listener {
    private final Plugin plugin;
    private final TransactionService tx;
    private final SchedulerService scheduler;
    private final MessageService messages;
    private final File shopFile;
    private final ShopCatalog catalog = new ShopCatalog();
    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    private final NamespacedKey tokenKey;
    private final NamespacedKey itemKey;

    public ShopService(Plugin plugin, TransactionService tx, SchedulerService scheduler, MessageService messages, File shopFile) {
        this.plugin = plugin;
        this.tx = tx;
        this.scheduler = scheduler;
        this.messages = messages;
        this.shopFile = shopFile;
        this.tokenKey = new NamespacedKey(plugin, "purchase-token");
        this.itemKey = new NamespacedKey(plugin, "shop-item");
        reload();
    }

    public void reload() {
        catalog.load(shopFile);
    }

    public ShopCatalog catalog() {
        return catalog;
    }

    public void open(Player player) {
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
        player.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ShopHolder) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        var clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !processing.add(player.getUniqueId())) {
            return;
        }
        var meta = clicked.getItemMeta();
        var id = meta == null ? null : meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        var item = catalog.byId(id).or(() -> catalog.bySlot(event.getRawSlot()));
        if (item.isEmpty()) {
            processing.remove(player.getUniqueId());
            return;
        }
        purchase(player, item.get());
    }

    @EventHandler
    public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ShopHolder) {
            event.setCancelled(true);
        }
    }

    private void purchase(Player player, ShopItem item) {
        var purchaseId = UUID.randomUUID();
        var ctx = new MutationContext(TransactionType.SHOP_PURCHASE, "shop:" + item.id(), player.getUniqueId(),
                "purchase:" + purchaseId, Map.of("item", item.id()));
        tx.remove(player.getUniqueId(), item.price(), ctx).whenComplete((result, error) -> scheduler.entity(player, () -> {
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
                messages.sendKey(player, "shop-purchase-complete", Map.of("balance", Long.toString(result.transaction().newBalance())));
            } finally {
                processing.remove(player.getUniqueId());
            }
        }, () -> processing.remove(player.getUniqueId())));
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
            lore.add(messages.render("<gray>Price: <light_purple><price> shards</light_purple>", Map.of("price", Long.toString(item.price()))));
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
        var key = name.toLowerCase(Locale.ROOT).replace(' ', '_');
        var byKey = Enchantment.getByKey(NamespacedKey.minecraft(key));
        if (byKey != null) {
            return byKey;
        }
        return Enchantment.getByName(key.toUpperCase(Locale.ROOT));
    }

    public void clear() {
        processing.clear();
    }
}
