package com.nightbeam.donutshards.service;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;
import java.util.Optional;

/**
 * Pure conversion math. Default mode keeps 1.2.0 behaviour: shards-per-dollar.
 */
public final class ConversionRates {
    public enum RateMode {
        SHARDS_PER_DOLLAR,
        DOLLAR_BATCH
    }

    public record Quote(boolean success, String reason, long shards, double dollars) {
        public static Quote ok(long shards, double dollars) {
            return new Quote(true, "ok", shards, dollars);
        }

        public static Quote fail(String reason) {
            return new Quote(false, reason, 0, 0);
        }
    }

    private final RateMode mode;
    private final int shardsPerDollar;
    private final double dollarBatchSize;
    private final boolean requireDollarMultiples;
    private final int feeMoneyToShards;
    private final int feeShardsToMoney;
    private final long minimumShards;
    private final double minimumDollars;

    public ConversionRates(RateMode mode, int shardsPerDollar, double dollarBatchSize, boolean requireDollarMultiples,
                           int feeMoneyToShards, int feeShardsToMoney, long minimumShards, double minimumDollars) {
        this.mode = mode;
        this.shardsPerDollar = Math.max(1, shardsPerDollar);
        this.dollarBatchSize = Math.max(0.01, dollarBatchSize);
        this.requireDollarMultiples = requireDollarMultiples;
        this.feeMoneyToShards = clampFee(feeMoneyToShards);
        this.feeShardsToMoney = clampFee(feeShardsToMoney);
        this.minimumShards = Math.max(1, minimumShards);
        this.minimumDollars = Math.max(0.01, minimumDollars);
    }

    public static ConversionRates fromConfig(YamlConfiguration config) {
        var modeName = Optional.ofNullable(config.getString("conversion.rate-mode", "shards-per-dollar"))
                .orElse("shards-per-dollar").toLowerCase(Locale.ROOT);
        var mode = modeName.equals("dollar-batch") ? RateMode.DOLLAR_BATCH : RateMode.SHARDS_PER_DOLLAR;
        var legacyFee = clampFee(config.getInt("conversion.fee-basis-points", 0));
        var moneyToShards = config.contains("conversion.fee-basis-points-money-to-shards")
                ? clampFee(config.getInt("conversion.fee-basis-points-money-to-shards"))
                : legacyFee;
        var shardsToMoney = config.contains("conversion.fee-basis-points-shards-to-money")
                ? clampFee(config.getInt("conversion.fee-basis-points-shards-to-money"))
                : legacyFee;
        return new ConversionRates(
                mode,
                config.getInt("conversion.shards-per-dollar", 100),
                config.getDouble("conversion.dollar-batch-size", 100),
                config.getBoolean("conversion.require-dollar-multiples", true),
                moneyToShards,
                shardsToMoney,
                config.getLong("conversion.minimum-shards", 1),
                config.getDouble("conversion.minimum-dollars", 0.01)
        );
    }

    public Quote moneyToShards(double dollars) {
        if (dollars <= 0) {
            return Quote.fail("invalid_amount");
        }
        if (dollars < minimumDollars) {
            return Quote.fail("below_minimum_dollars");
        }
        long shards;
        if (mode == RateMode.DOLLAR_BATCH) {
            if (requireDollarMultiples && !isMultiple(dollars, dollarBatchSize)) {
                return Quote.fail("invalid_dollar_multiple");
            }
            shards = (long) Math.floor(dollars / dollarBatchSize);
            shards = applyFloorFee(shards, feeMoneyToShards);
        } else {
            var raw = (long) Math.floor(dollars * shardsPerDollar);
            shards = applyFloorFee(raw, feeMoneyToShards);
        }
        if (shards < minimumShards) {
            return Quote.fail("below_minimum_shards");
        }
        return Quote.ok(shards, dollars);
    }

    public Quote shardsToMoney(long shards) {
        if (shards < 1) {
            return Quote.fail("invalid_amount");
        }
        if (shards < minimumShards) {
            return Quote.fail("below_minimum_shards");
        }
        double dollars;
        if (mode == RateMode.DOLLAR_BATCH) {
            dollars = shards * dollarBatchSize;
            dollars = applyFee(dollars, feeShardsToMoney);
        } else {
            dollars = shards / (double) shardsPerDollar;
            dollars = applyFee(dollars, feeShardsToMoney);
        }
        if (dollars < minimumDollars) {
            return Quote.fail("below_minimum_dollars");
        }
        return Quote.ok(shards, dollars);
    }

    public RateMode mode() {
        return mode;
    }

    private static int clampFee(int fee) {
        return Math.max(0, Math.min(10000, fee));
    }

    private static double applyFee(double amount, int feeBps) {
        return amount * (10_000 - feeBps) / 10_000.0;
    }

    private static long applyFloorFee(long shards, int feeBps) {
        if (feeBps <= 0) {
            return shards;
        }
        var kept = shards * (10_000L - feeBps) / 10_000L;
        if (kept < 1 && shards >= 1 && feeBps < 10_000) {
            return shards;
        }
        return Math.max(0, kept);
    }

    static boolean isMultiple(double dollars, double batch) {
        var dollarCents = Math.round(dollars * 100.0);
        var batchCents = Math.round(batch * 100.0);
        if (batchCents <= 0) {
            return false;
        }
        return dollarCents % batchCents == 0;
    }
}
