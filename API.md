# Public API

Retrieve `DonutShardsApi` from Bukkit's `ServicesManager`. Reads and all mutations return `CompletionStage`; never block a region thread waiting for completion. `getCachedBalance` is the only synchronous balance accessor and may be empty.

Mutation calls require a `MutationContext` containing a transaction type, source, optional actor, unique idempotency key, and string metadata. Zone-state reads are in-memory and synchronous. Custom events may be asynchronous for offline API operations; listeners must inspect `Event#isAsynchronous()` before accessing Bukkit state.
