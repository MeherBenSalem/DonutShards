package com.nightbeam.donutshards.model;
import java.time.Instant;
import java.util.UUID;
public record ShardTransaction(UUID id, UUID correlationId, UUID playerId, UUID relatedPlayerId, TransactionType type, long amount, long previousBalance, long newBalance, String source, Instant timestamp, String serverId, UUID administratorId, boolean rolledBack) {}
