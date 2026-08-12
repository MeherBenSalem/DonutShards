# Changelog

## 1.2.0

- Vault-backed shard ↔ money conversion via `/shards convert <amount> <shards|money>` (`shards.convert` permission).
- Stay-in-place AFK home mode (`/afk home`, `/afk zone`) with configurable reduced shard multiplier; rewards require zone or home when enabled.
- Kill rewards for player and mob kills loaded from `kills.yml`, with per-player PvP cooldown and spawner-mob exclusion.
- Reward loop now respects AFK zone/home eligibility and configurable interval/shard amounts in `config.yml`.
- Release build copies jar to `releases/`; GitHub publish workflow added for Modrinth/CurseForge.

## 1.1.0

- AFK zones load and save from `zones.yml` with spherical regions (world, center, radius).
- `/afk list`, `/afk join [name]`, `/afk leave`, and `/afk info` work with configured zones.
- `/shardmanager zone create|delete|list` persists zones in-game; `/shardmanager reload` reloads configuration.
- Movement and join listeners auto-detect when players enter or leave AFK regions.
- Expanded `.gitignore` for Gradle, IDE, database, and local env artifacts.

## 1.0.0

- Initial original DonutShards economy, scheduler abstraction, SQLite/MariaDB schema, asynchronous API, commands, grouped rewards, zone state, and inventory shop foundation.
