package com.nightbeam.donutshards.model;
public record TransactionResult(boolean success, String reason, ShardTransaction transaction) {
    public static TransactionResult failure(String reason) { return new TransactionResult(false, reason, null); }
    public static TransactionResult success(ShardTransaction tx) { return new TransactionResult(true, "ok", tx); }
}
