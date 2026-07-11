package com.nightbeam.donutshards.model;
import java.util.Map;
import java.util.UUID;
public record MutationContext(TransactionType type, String source, UUID actor, String idempotencyKey, Map<String,String> metadata) {
    public MutationContext { metadata = metadata == null ? Map.of() : Map.copyOf(metadata); }
    public static MutationContext api(String source) { return new MutationContext(TransactionType.API, source, null, UUID.randomUUID().toString(), Map.of()); }
}
