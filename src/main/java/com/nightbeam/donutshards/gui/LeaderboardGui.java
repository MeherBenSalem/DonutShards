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
    /** Slots 0–44 for entries; 45/53 reserved for prev/next. */
    private static final int MAX_PAGE_SIZE = 45;
    private static final int SQL_CAP = 100;

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

    private int pageSize() {
        return Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize.get()));
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        var safePage = Math.max(0, page);
        var size = pageSize();
        var from = safePage * size;
        // Fetch one extra row past this page to detect whether Next should show (within SQL_CAP).
        var request = Math.min(SQL_CAP, from + size + 1);
        tx.topBalances(request).whenComplete((entries, error) -> scheduler.entity(player, () -> {
            if (error != null || entries == null || entries.isEmpty()) {
                messages.sendKey(player, "top-empty", Map.of());
                return;
            }
            if (from >= entries.size()) {
                open(player, Math.max(0, safePage - 1));
                return;
            }
            var holder = new LeaderboardHolder(player.getUniqueId(), safePage);
            var title = messages.renderKey("top-gui-title", Map.of("page", Integer.toString(safePage + 1)));
            var inv = Bukkit.createInventory(holder, SIZE, title);
            holder.inventory(inv);
            var to = Math.min(entries.size(), from + size);
            var slice = entries.subList(from, to);
            for (int i = 0; i < slice.size(); i++) {
                inv.setItem(i, entryStack(slice.get(i), from + i + 1));
            }
            if (safePage > 0) {
                inv.setItem(PREV_SLOT, navStack(Material.ARROW, "<yellow>Previous page"));
            }
            if (to < entries.size() && to < SQL_CAP) {
                inv.setItem(NEXT_SLOT, navStack(Material.ARROW, "<yellow>Next page"));
            }
            player.openInventory(inv);
        }, () -> {
        }));
    }

    public void sendChat(Player player) {
        var size = pageSize();
        tx.topBalances(Math.min(SQL_CAP, size)).whenComplete((entries, error) -> scheduler.entity(player, () -> {
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
