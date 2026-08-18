package com.nightbeam.donutshards.service;

import com.nightbeam.donutshards.integration.vault.VaultHook;
import com.nightbeam.donutshards.model.MutationContext;
import com.nightbeam.donutshards.model.TransactionType;
import com.nightbeam.donutshards.transaction.TransactionService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ConversionService {
    private final TransactionService tx;
    private final VaultHook vault;
    private volatile boolean enabled;
    private volatile ConversionRates rates;

    public ConversionService(TransactionService tx, VaultHook vault) {
        this.tx = tx;
        this.vault = vault;
        this.rates = ConversionRates.fromConfig(new YamlConfiguration());
    }

    public void reload(YamlConfiguration config) {
        enabled = config.getBoolean("conversion.enabled", true);
        rates = ConversionRates.fromConfig(config);
    }

    public boolean enabled() {
        return enabled && vault.available();
    }

    public ConversionRates rates() {
        return rates;
    }

    public CompletionStage<ConversionResult> convert(Player player, String amountText, String direction) {
        if (!enabled()) {
            return CompletableFuture.completedFuture(ConversionResult.failure("conversion_disabled"));
        }
        if (!player.hasPermission("shards.convert")) {
            return CompletableFuture.completedFuture(ConversionResult.failure("no_permission"));
        }
        var dir = direction == null ? "" : direction.toLowerCase(Locale.ROOT);
        if (dir.equals("shards") || dir.equals("money")) {
            return doConvert(player, amountText, dir);
        }
        return CompletableFuture.completedFuture(ConversionResult.failure("invalid_direction"));
    }

    private CompletionStage<ConversionResult> doConvert(Player player, String amountText, String direction) {
        if (direction.equals("money")) {
            return shardsToMoney(player, parseLong(amountText));
        }
        return moneyToShards(player, parseDouble(amountText));
    }

    private CompletionStage<ConversionResult> shardsToMoney(Player player, long shards) {
        var quote = rates.shardsToMoney(shards);
        if (!quote.success()) {
            return CompletableFuture.completedFuture(ConversionResult.failure(quote.reason()));
        }
        var ctx = new MutationContext(TransactionType.CONVERSION, "vault:shards-to-money", player.getUniqueId(),
                "convert:" + UUID.randomUUID(), Map.of("direction", "shards-to-money", "shards", Long.toString(shards)));
        var dollars = quote.dollars();
        return tx.remove(player.getUniqueId(), shards, ctx).thenCompose(result -> {
            if (!result.success()) {
                return CompletableFuture.completedFuture(ConversionResult.failure(result.reason()));
            }
            if (!vault.economy().depositPlayer(player, dollars).transactionSuccess()) {
                var refund = new MutationContext(TransactionType.CONVERSION, "vault:refund", player.getUniqueId(),
                        "convert-refund:" + UUID.randomUUID(), Map.of());
                return tx.add(player.getUniqueId(), shards, refund).thenApply(ignored -> ConversionResult.failure("vault_deposit_failed"));
            }
            return CompletableFuture.completedFuture(ConversionResult.success(shards, dollars, "shards-to-money"));
        });
    }

    private CompletionStage<ConversionResult> moneyToShards(Player player, double dollars) {
        var quote = rates.moneyToShards(dollars);
        if (!quote.success()) {
            return CompletableFuture.completedFuture(ConversionResult.failure(quote.reason()));
        }
        if (!vault.economy().has(player, dollars) || !vault.economy().withdrawPlayer(player, dollars).transactionSuccess()) {
            return CompletableFuture.completedFuture(ConversionResult.failure("insufficient_funds"));
        }
        var ctx = new MutationContext(TransactionType.CONVERSION, "vault:money-to-shards", player.getUniqueId(),
                "convert:" + UUID.randomUUID(), Map.of("direction", "money-to-shards", "dollars", Double.toString(dollars)));
        var shards = quote.shards();
        return tx.add(player.getUniqueId(), shards, ctx).thenApply(result -> {
            if (result.success()) {
                return ConversionResult.success(shards, dollars, "money-to-shards");
            }
            vault.economy().depositPlayer(player, dollars);
            return ConversionResult.failure(result.reason());
        });
    }

    private long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public record ConversionResult(boolean success, String reason, long shards, double dollars, String direction) {
        public static ConversionResult success(long shards, double dollars, String direction) {
            return new ConversionResult(true, "ok", shards, dollars, direction);
        }

        public static ConversionResult failure(String reason) {
            return new ConversionResult(false, reason, 0, 0, "");
        }
    }
}
