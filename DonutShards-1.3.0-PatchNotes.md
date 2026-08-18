# DonutShards v1.3.0

### New Features
* Shop GUI is driven by `shop.yml`, including optional `enchantments` per item.
* AFK zone boundary particles (`afk.zone-particles`).
* Opt-in conversion `rate-mode: dollar-batch` ($100 = 1 shard, dollar multiples, per-direction fees).

### Improvements
* `/afk leave` can auto-rejoin when you remain inside the zone (`afk.auto-rejoin-in-zone: true`).

### Bug Fixes
* `messages.yml` is loaded on startup and `/shardmanager reload`. Edited keys now appear in-game.
* Shop no longer ignores `shop.yml` (hardcoded cookie GUI removed).

### Configuration
* New conversion keys: `rate-mode`, `dollar-batch-size`, `require-dollar-multiples`, `fee-basis-points-money-to-shards`, `fee-basis-points-shards-to-money`.
* New AFK keys: `auto-rejoin-in-zone`, `zone-particles.*`.
* Existing servers that omit the new keys keep 1.2.0 conversion behaviour.

### Compatibility
* Drop-in update from 1.2.0
* Paper/Folia 1.20.1–26.1.2; 26.2 is not claimed
* Vault still required for `/shards convert`

### Upgrade Notes
1. Replace the old jar with `DonutShards-1.3.0-paper-folia-mc1.20.1-26.1.2.jar`
2. Restart the server (or `/shardmanager reload` after replacing the jar on a live test)
3. Copy new keys from the jar defaults into existing YAML if you want shop enchants, dollar-batch conversion, or particles
4. For $100 = 1 shard with 10%/80% fees, set the `dollar-batch` block documented in `PATCH_NOTES.md`
