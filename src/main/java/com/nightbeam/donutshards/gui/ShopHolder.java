package com.nightbeam.donutshards.gui;
import org.bukkit.inventory.*;import java.util.UUID;
public final class ShopHolder implements InventoryHolder {private final UUID session;private Inventory inventory;public ShopHolder(UUID session){this.session=session;}public UUID session(){return session;}public void inventory(Inventory value){inventory=value;}public Inventory getInventory(){return inventory;}}
