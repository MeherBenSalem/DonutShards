package com.nightbeam.donutshards.gui;

import com.nightbeam.donutshards.shop.ShopItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class ConfirmHolder implements InventoryHolder {
    private final UUID session;
    private final ShopItem item;
    private final boolean deal;
    private Inventory inventory;

    public ConfirmHolder(UUID session, ShopItem item, boolean deal) {
        this.session = session;
        this.item = item;
        this.deal = deal;
    }

    public boolean deal() {
        return deal;
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
