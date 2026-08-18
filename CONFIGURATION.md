# Configuration

DonutShards creates `config.yml`, `messages.yml`, `database.yml`, `rewards.yml`, `zones.yml`, `shop.yml`, `gui.yml`, and `anti-abuse.yml`. Packaged defaults contain comments and no credentials. Monetary values are whole shards stored as signed 64-bit integers and are validated against `maximum-balance`.

MiniMessage is accepted for presentation fields. Materials, sounds, worlds, modes, durations, slots, ranges, and permissions must be valid for the running server. Keep `schema-version` intact.

`/shardmanager reload` reloads `config.yml`, `messages.yml`, `shop.yml`, `zones.yml`, `kills.yml`, conversion, AFK particles, and reward timing.

## messages.yml

Chat strings use MiniMessage. `prefix` is prepended to `sendKey` chat messages. Missing keys fall back to a red "Missing message" line.

## shop.yml

Each category can define a non-purchasable `icon` and purchasable `items` with `slot`, `price`, `material`, `amount`, `name`, `lore`, and optional `enchantments` (`sharpness: 5`). Invalid materials are skipped and logged.

## conversion

Default `rate-mode: shards-per-dollar` is unchanged from 1.2.0 (`shards-per-dollar: 100` means 100 shards per $1).

To charge $100 per shard and only accept multiples of $100:

```yaml
conversion:
  rate-mode: dollar-batch
  dollar-batch-size: 100
  require-dollar-multiples: true
  fee-basis-points-money-to-shards: 1000   # 10%
  fee-basis-points-shards-to-money: 8000   # 80%
```

If the per-direction fee keys are omitted, `fee-basis-points` applies to both directions.

## AFK

* `afk.auto-rejoin-in-zone` (default true): after `/afk leave`, walking or syncing while still inside the zone starts AFK again.
* `afk.zone-particles`: Folia-safe ring at the zone radius. Disable with `enabled: false`.

