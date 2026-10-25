package com.nightbeam.donutshards.api;
import com.nightbeam.donutshards.model.*;
import java.util.*;
import java.util.concurrent.CompletionStage;
public interface DonutShardsApi {
    CompletionStage<Long> getBalance(UUID player);
    OptionalLong getCachedBalance(UUID player);
    CompletionStage<TransactionResult> setBalance(UUID player,long amount,MutationContext context);
    CompletionStage<TransactionResult> addShards(UUID player,long amount,MutationContext context);
    CompletionStage<TransactionResult> removeShards(UUID player,long amount,MutationContext context);
    CompletionStage<TransferResult> transfer(UUID from,UUID to,long amount,MutationContext context);
    CompletionStage<Optional<ShardTransaction>> getTransaction(UUID id);
    CompletionStage<PlayerStatistics> getPlayerStatistics(UUID player);
    boolean isInAfkZone(UUID player);
    Optional<String> getCurrentAfkZone(UUID player);
    void registerAntiCheatProvider(AntiCheatFlagProvider provider);

    /** The running server booster's multiplier, 1 when none is active. Since 2.0. */
    default double getActiveBoosterMultiplier() {
        return 1.0;
    }

    /** The player's ledger entries, newest first. Since 2.0. */
    default CompletionStage<List<ShardTransaction>> getHistory(UUID player, int limit, int offset) {
        return java.util.concurrent.CompletableFuture.completedFuture(List.of());
    }
}
