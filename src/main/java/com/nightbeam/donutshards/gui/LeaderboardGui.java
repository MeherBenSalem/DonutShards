package com.nightbeam.donutshards.gui;

import com.nightbeam.donutshards.model.LeaderboardEntry;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.service.MessageService;
import com.nightbeam.donutshards.transaction.TransactionService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class LeaderboardGui implements Listener {
    private static final int SIZE = 54;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final TransactionService tx;
    private final SchedulerService scheduler;
    private final MessageService messages;
    private final AtomicInteger pageSize;

    public LeaderboardGui(TransactionService tx, SchedulerService scheduler, MessageService messages, AtomicInteger pageSize) {
        this.tx = tx;
        this.scheduler = scheduler;
        this.messages = messages;
        this.pageSize = pageSize;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        var safePage = Math.max(0, page);
        var size = Math.max(1, pageSize.get());
        var limit = size * (safePage + 1);
        tx.topBalances(limit).whenComplete((entries, error) -> scheduler.entity(player, () -> {
            if (error != null || entries == null || entries.isEmpty()) {
                messages.sendKey(player, "top-empty", Map.of());
                return;
            }
            var holder = new LeaderboardHolder(player.getUniqueId(), safePage);
            var title = messages.renderKey("top-gui-title", Map.of("page", Integer.toString(safePage + 1)));
            var inv = Bukkit.createInventory(holder, SIZE, title);
            holder.inventory(inv);
            var from = safePage * size;
            var to = Math.min(entries.size(), from + size);
            if (from >= entries.size()) {
                open(player, Math.max(0, safePage - 1));
                return;
            }
            var slice = entries.subList(from, to);
            for (int i = 0; i < slice.size(); i++) {
                inv.setItem(i, entryStack(slice.get(i), from + i + 1));
            }
            if (safePage > 0) {
                inv.setItem(PREV_SLOT, navStack(Material.ARROW, "<yellow>Previous page"));
            }
            if (to < entries.size() || entries.size() >= limit) {
                // Fetch one extra page worth to detect more; if we filled the request, allow next.
                if (entries.size() >= limit) {
                    inv.setItem(NEXT_SLOT, navStack(Material.ARROW, "<yellow>Next page"));
                }
            }
            player.openInventory(inv);
        }, () -> {
        }));
    }

    public void sendChat(Player player) {
        var size = Math.max(1, pageSize.get());
        tx.topBalances(size).whenComplete((entries, error) -> scheduler.entity(player, () -> {
            if (error != null || entries == null || entries.isEmpty()) {
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
        }, () -> {
        }));
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof LeaderboardHolder holder)) {
            return;
        }
        event.setCancelled(true);
        var slot = event.getRawSlot();
        if (slot == PREV_SLOT && holder.page() > 0) {
            open(player, holder.page() - 1);
        } else if (slot == NEXT_SLOT) {
            open(player, holder.page() + 1);
        }
    }

    @EventHandler
    public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof LeaderboardHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack entryStack(LeaderboardEntry entry, int rank) {
        var offline = Bukkit.getOfflinePlayer(entry.player());
        var name = offline.getName() == null ? entry.player().toString() : offline.getName();
        var stack = new ItemStack(Material.PLAYER_HEAD);
        var meta = stack.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(offline);
            skull.displayName(messages.renderKey("top-entry-name", Map.of(
                    "rank", Integer.toString(rank),
                    "player", name
            )));
            skull.lore(List.of(messages.renderKey("top-entry-lore", Map.of(
                    "balance", Long.toString(entry.balance()),
                    "rank", Integer.toString(rank),
                    "player", name
            ))));
            stack.setItemMeta(skull);
        } else if (meta != null) {
            meta.displayName(messages.renderKey("top-entry-name", Map.of(
                    "rank", Integer.toString(rank),
                    "player", name
            )));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack navStack(Material material, String name) {
        var stack = new ItemStack(material);
        var meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.render(name, Map.of()));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
