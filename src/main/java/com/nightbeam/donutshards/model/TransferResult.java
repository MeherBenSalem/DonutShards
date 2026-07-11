package com.nightbeam.donutshards.model;
import java.util.UUID;
public record TransferResult(boolean success, String reason, UUID correlationId, long amount, long tax) {
    public static TransferResult failure(String reason) { return new TransferResult(false, reason, null, 0, 0); }
}
