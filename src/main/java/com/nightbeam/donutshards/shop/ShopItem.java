package com.nightbeam.donutshards.shop;

import java.util.List;
import java.util.Map;

public record ShopItem(
        String id,
        int slot,
        long price,
        String material,
        int amount,
        String name,
        List<String> lore,
        Map<String, Integer> enchantments,
        boolean purchasable
) {
}
