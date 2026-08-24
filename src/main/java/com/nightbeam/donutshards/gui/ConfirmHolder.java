package com.nightbeam.donutshards.gui;

import com.nightbeam.donutshards.shop.ShopItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class ConfirmHolder implements InventoryHolder {
    private final UUID session;
    private final ShopItem item;
    private Inventory inventory;

    public ConfirmHolder(UUID session, ShopItem item) {
        this.session = session;
        this.item = item;
    }

    public UUID session() {
        return session;
    }

    public ShopItem item() {
        return item;
    }

    public void inventory(Inventory value) {
        inventory = value;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
