package com.nightbeam.donutshards.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class LeaderboardHolder implements InventoryHolder {
    private final UUID session;
    private final int page;
    private Inventory inventory;

    public LeaderboardHolder(UUID session, int page) {
        this.session = session;
        this.page = page;
    }

    public UUID session() {
        return session;
    }

    public int page() {
        return page;
    }

    public void inventory(Inventory value) {
        inventory = value;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
