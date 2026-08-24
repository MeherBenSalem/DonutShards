# Patch Notes — 1.4.0

**YAML merge:** On upgrade, missing keys from jar defaults are added automatically — your custom `prefix` and other edits stay intact. `/shardmanager reload` runs the same merge.

**Shop:** Per-item `confirmation:` opens the confirm/cancel GUI from `gui.yml`. Per-item `commands:` run as console after purchase. `/shards confirmation on|off` toggles confirmations for your account (default on).

**Placeholders:** `%donutshard_balance%`, `%donutshard_top_1`–`10`, `%donutshard_top_bal_1`–`10` when PlaceholderAPI is installed.

**Ops:** `/shards top` shows the top 10 balances. bStats (id 33616) and Modrinth update checks are on by default — disable with `bstats: false` and `update-check: false` in `config.yml`.

## 1.3.0

**Messages and shop:** Edit `messages.yml` and `shop.yml` — `/shardmanager reload` now applies both. Add enchanted gear with an `enchantments` block per item (see the Starlight Blade example).

**Conversion:** Default rate is unchanged (100 shards per $1). To use $100 = 1 shard with separate buy/sell fees:

```yaml
conversion:
  rate-mode: dollar-batch
  dollar-batch-size: 100
  require-dollar-multiples: true
  fee-basis-points-money-to-shards: 1000
  fee-basis-points-shards-to-money: 8000
```

**AFK zones:** Optional particle rings show zone radius. Standing inside a zone auto-joins AFK again after `/afk leave` unless `afk.auto-rejoin-in-zone` is false.

## 1.2.0

Three community-requested features ship together. **Conversion:** exchange shards and server money through Vault with configurable rates and fees (`/shards convert`). **Home AFK:** earn reduced shards anywhere with `/afk home` without standing in a zone; `/afk zone` returns to full zone rewards. **Kill rewards:** PvP and mob kills grant shards from `kills.yml` (PvP cooldown, no spawner farms).

## 1.1.0

AFK zones are now fully functional. Admins create spherical regions with `/shardmanager zone create <name>` at their location; zones persist in `zones.yml`. Players use `/afk join` (auto-detects when standing inside a zone), `/afk leave`, `/afk list`, and `/afk info`. Entering or leaving a region is tracked automatically for reward eligibility. `/shardmanager reload` refreshes zone and config data without a restart.

## 1.0.0

This first release establishes the Java 21 Paper/Folia-compatible architecture, persistent integer ledger, idempotent transaction keys, original Cosmic Confection Exchange, grouped reward loop, and public API. The validated build range is 1.20.1–26.1.2; 26.2 is intentionally not claimed.
