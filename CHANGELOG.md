# Changelog

## 1.1.0

- AFK zones load and save from `zones.yml` with spherical regions (world, center, radius).
- `/afk list`, `/afk join [name]`, `/afk leave`, and `/afk info` work with configured zones.
- `/shardmanager zone create|delete|list` persists zones in-game; `/shardmanager reload` reloads configuration.
- Movement and join listeners auto-detect when players enter or leave AFK regions.
- Expanded `.gitignore` for Gradle, IDE, database, and local env artifacts.

## 1.0.0

- Initial original DonutShards economy, scheduler abstraction, SQLite/MariaDB schema, asynchronous API, commands, grouped rewards, zone state, and inventory shop foundation.
