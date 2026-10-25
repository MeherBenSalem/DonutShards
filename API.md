# Public API

Get `DonutShardsApi` from Bukkit's `ServicesManager`. Reads and mutations return `CompletionStage`; never block a
region thread waiting for one. `getCachedBalance` is the only synchronous balance accessor and may be empty.

Mutations take a `MutationContext`: transaction type, source, optional actor, a unique idempotency key and string
metadata. Repeating a key returns the first result instead of applying the change twice.

| Method | Since |
|--------|-------|
| `getBalance`, `getCachedBalance`, `setBalance`, `addShards`, `removeShards`, `transfer` (uses the live tax) | 1.0 |
| `getTransaction`, `getPlayerStatistics`, `isInAfkZone`, `getCurrentAfkZone`, `registerAntiCheatProvider` | 1.0 |
| `getActiveBoosterMultiplier()`: the running booster, 1 when none | 2.0 |
| `getHistory(player, limit, offset)`: ledger rows, newest first | 2.0 |

Events (all fired on the thread that owns the player, never asynchronously):

| Event | When | Cancel / change |
|-------|------|-----------------|
| `AfkZoneEnterEvent`, `AfkZoneLeaveEvent` | A player enters or leaves a zone | Cancelling enter keeps them out |
| `ShardRewardEvent` | Before an AFK payout (since 2.0) | Cancel, or change `amount` |
| `ShardPurchaseEvent` | Before a shop or deal purchase is charged (since 2.0) | Cancel |
| `ShardTransferEvent` | Before a `/shards pay` transfer (since 2.0) | Cancel |

`ShardBalanceChangeEvent` exists for source compatibility and is not fired; listen to the events above or read the
ledger with `getHistory`. (In 1.x none of the shard events were fired.)
