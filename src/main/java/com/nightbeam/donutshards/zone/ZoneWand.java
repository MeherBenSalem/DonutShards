package com.nightbeam.donutshards.zone;

import com.nightbeam.donutshards.core.ZoneBox;
import com.nightbeam.donutshards.service.MessageService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The zone selection wand: left-click a block for corner 1, right-click for corner 2. Corners can also be set where
 * the admin stands with {@code /shardmanager zone pos1|pos2}.
 */
public final class ZoneWand implements Listener {
    public static final String PERMISSION = "shards.admin.zone";

    /** One admin's two corners. Both must be in the same world to make a box. */
    public record Selection(String world1, int x1, int y1, int z1, String world2, int x2, int y2, int z2) {
        static final Selection EMPTY = new Selection(null, 0, 0, 0, null, 0, 0, 0);

        Selection withFirst(String world, int x, int y, int z) {
            return new Selection(world, x, y, z, world2, x2, y2, z2);
        }

        Selection withSecond(String world, int x, int y, int z) {
            return new Selection(world1, x1, y1, z1, world, x, y, z);
        }

        public boolean complete() {
            return world1 != null && world1.equals(world2);
        }

        public Optional<ZoneBox> box() {
            return complete() ? Optional.of(ZoneBox.of(x1, y1, z1, x2, y2, z2)) : Optional.empty();
        }
    }

    private final NamespacedKey key;
    private final MessageService messages;
    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    public ZoneWand(Plugin plugin, MessageService messages) {
        this.key = new NamespacedKey(plugin, "zone-wand");
        this.messages = messages;
    }

    public ItemStack create() {
        var stack = new ItemStack(Material.BLAZE_ROD);
        var meta = stack.getItemMeta();
        meta.displayName(messages.renderKey("zone-wand-name", Map.of()));
        meta.lore(List.of(messages.renderKey("zone-wand-lore", Map.of())));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isWand(ItemStack stack) {
        if (stack == null || stack.getType() != Material.BLAZE_ROD || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public Selection selection(UUID player) {
        return selections.getOrDefault(player, Selection.EMPTY);
    }

    public void clear(UUID player) {
        selections.remove(player);
    }

    public void setCorner(Player player, boolean first, Location at) {
        var world = at.getWorld() == null ? null : at.getWorld().getName();
        var x = at.getBlockX();
        var y = at.getBlockY();
        var z = at.getBlockZ();
        var updated = selections.compute(player.getUniqueId(), (id, old) -> {
            var base = old == null ? Selection.EMPTY : old;
            return first ? base.withFirst(world, x, y, z) : base.withSecond(world, x, y, z);
        });
        var values = new java.util.HashMap<String, String>();
        values.put("corner", first ? "1" : "2");
        values.put("x", Integer.toString(x));
        values.put("y", Integer.toString(y));
        values.put("z", Integer.toString(z));
        values.put("size", updated.box().map(b -> (b.maxX() - b.minX() + 1) + "×" + (b.maxY() - b.minY() + 1) + "×" + (b.maxZ() - b.minZ() + 1)).orElse("-"));
        messages.sendKey(player, "zone-corner-set", values);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void interact(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isWand(event.getItem())) {
            return;
        }
        var player = event.getPlayer();
        if (!player.hasPermission(PERMISSION)) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            setCorner(player, true, block.getLocation());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            setCorner(player, false, block.getLocation());
        }
    }

    /** In creative a left click breaks the block before the interaction is cancelled; keep the block. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void breakWithWand(BlockBreakEvent event) {
        if (isWand(event.getPlayer().getInventory().getItemInMainHand()) && event.getPlayer().hasPermission(PERMISSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
    }
}
