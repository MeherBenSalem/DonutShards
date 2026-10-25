package com.nightbeam.donutshards.api;

import com.nightbeam.donutshards.model.MutationContext;
import com.nightbeam.donutshards.model.PlayerStatistics;
import com.nightbeam.donutshards.model.ShardTransaction;
import com.nightbeam.donutshards.model.TransactionResult;
import com.nightbeam.donutshards.model.TransferResult;
import com.nightbeam.donutshards.transaction.TransactionService;
import com.nightbeam.donutshards.zone.ZoneService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleSupplier;

public final class DefaultDonutShardsApi implements DonutShardsApi {
    private final TransactionService tx;
    private final ZoneService zones;
    private final AtomicInteger tax;
    private final DoubleSupplier booster;
    private final List<AntiCheatFlagProvider> providers = new CopyOnWriteArrayList<>();

    public DefaultDonutShardsApi(TransactionService tx, ZoneService zones, AtomicInteger tax, DoubleSupplier booster) {
        this.tx = tx;
        this.zones = zones;
        this.tax = tax;
        this.booster = booster;
    }

    public CompletionStage<Long> getBalance(UUID p) {
        return tx.balance(p);
    }

    public OptionalLong getCachedBalance(UUID p) {
        return tx.cached(p);
    }

    public CompletionStage<TransactionResult> setBalance(UUID p, long a, MutationContext c) {
        return tx.set(p, a, c);
    }

    public CompletionStage<TransactionResult> addShards(UUID p, long a, MutationContext c) {
        return tx.add(p, a, c);
    }

    public CompletionStage<TransactionResult> removeShards(UUID p, long a, MutationContext c) {
        return tx.remove(p, a, c);
    }

    /** Uses the transfer tax currently configured (it follows {@code /shardmanager reload}). */
    public CompletionStage<TransferResult> transfer(UUID f, UUID t, long a, MutationContext c) {
        return tx.transfer(f, t, a, tax.get(), c);
    }

    public CompletionStage<Optional<ShardTransaction>> getTransaction(UUID id) {
        return tx.transaction(id);
    }

    public CompletionStage<PlayerStatistics> getPlayerStatistics(UUID p) {
        return tx.statistics(p);
    }

    public boolean isInAfkZone(UUID p) {
        return zones.isInZone(p);
    }

    public Optional<String> getCurrentAfkZone(UUID p) {
        return zones.current(p);
    }

    public double getActiveBoosterMultiplier() {
        return booster.getAsDouble();
    }

    public CompletionStage<List<ShardTransaction>> getHistory(UUID p, int limit, int offset) {
        return tx.history(p, limit, offset);
    }

    public void registerAntiCheatProvider(AntiCheatFlagProvider p) {
        Objects.requireNonNull(p);
        providers.removeIf(x -> x.id().equals(p.id()));
        providers.add(p);
    }
}
