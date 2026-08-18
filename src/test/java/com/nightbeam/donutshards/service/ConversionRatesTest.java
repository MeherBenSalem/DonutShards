package com.nightbeam.donutshards.service;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConversionRatesTest {
    @Test
    void legacyShardsPerDollarUnchanged() {
        var config = new YamlConfiguration();
        config.set("conversion.shards-per-dollar", 100);
        config.set("conversion.fee-basis-points", 0);
        var rates = ConversionRates.fromConfig(config);
        var quote = rates.moneyToShards(1.0);
        assertThat(quote.success()).isTrue();
        assertThat(quote.shards()).isEqualTo(100);
        var back = rates.shardsToMoney(100);
        assertThat(back.success()).isTrue();
        assertThat(back.dollars()).isEqualTo(1.0);
    }

    @Test
    void dollarBatchConvertsHundredDollarsToOneShard() {
        var config = new YamlConfiguration();
        config.set("conversion.rate-mode", "dollar-batch");
        config.set("conversion.dollar-batch-size", 100);
        config.set("conversion.require-dollar-multiples", true);
        config.set("conversion.fee-basis-points-money-to-shards", 0);
        config.set("conversion.fee-basis-points-shards-to-money", 0);
        var rates = ConversionRates.fromConfig(config);
        assertThat(rates.moneyToShards(50).success()).isFalse();
        assertThat(rates.moneyToShards(50).reason()).isEqualTo("invalid_dollar_multiple");
        var quote = rates.moneyToShards(100);
        assertThat(quote.success()).isTrue();
        assertThat(quote.shards()).isEqualTo(1);
        var back = rates.shardsToMoney(1);
        assertThat(back.dollars()).isEqualTo(100.0);
    }

    @Test
    void perDirectionFees() {
        var config = new YamlConfiguration();
        config.set("conversion.rate-mode", "dollar-batch");
        config.set("conversion.dollar-batch-size", 100);
        config.set("conversion.require-dollar-multiples", true);
        config.set("conversion.fee-basis-points-money-to-shards", 1000);
        config.set("conversion.fee-basis-points-shards-to-money", 8000);
        var rates = ConversionRates.fromConfig(config);
        var buy = rates.moneyToShards(1000);
        assertThat(buy.shards()).isEqualTo(9);
        var smallBuy = rates.moneyToShards(100);
        assertThat(smallBuy.shards()).isEqualTo(1);
        var sell = rates.shardsToMoney(1);
        assertThat(sell.dollars()).isEqualTo(20.0);
    }
}
