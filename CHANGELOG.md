# Changelog

## 1.5.1

- Fix: `/afk zone` uses `teleportAsync` and runs sync/messages on the destination entity thread (Folia-safe).
- Fix: bare `/afk join` no longer double-sends zone-enter + joined messages.
- Fix: leaderboard GUI page-size capped at 45; Next paging no longer phantom-bounces at the SQL top-100 cap.
- Docs: COMMANDS / CONFIGURATION / patch notes aligned with 1.5.0 command changes.

## 1.5.0

- Fix: Home AFK cancels on block-coordinate move, then zone presence syncs.
- Feature: AFK zone enter/leave chat; bare `/afk` = home; reward titles; `/afk zone` teleport; GUI `/shards top`; convert currency-first; admin tab cleanup; `/dshards`/`donutshards` → `/shardmanager`.

## 1.4.0

- Feature: `YamlKeyMerger` deep-merges missing keys from jar defaults on enable and reload without overwriting operator values.
- Feature: Shop items support `confirmation:` and post-purchase `commands:` with `%player%`, `%uuid%`, `%item%`, `%price%` placeholders.
- Feature: `/shards confirmation on|off` toggles shop confirm GUI (stored in `player-prefs.yml`, default on); `/shard` command alias.
- Feature: PlaceholderAPI expansion `donutshard_*` (balance, top names, top balances).
- Feature: `/shards top` leaderboard command.
- Feature: bStats metrics (plugin id 33616, shaded/relocated); `bstats: true` in config.
- Feature: Modrinth update checker for project `4krPhA6H`; `update-check: true` in config.
- Compatibility: Paper/Folia/Purpur/Spigot/Bukkit · Minecraft 1.20.1–26.2; jar name `…-mc1.20.1-26.2.jar`.

## 1.3.0

- Fix: `messages.yml` is loaded and reloaded; user-facing strings can be customized via message keys.
- Fix: `shop.yml` drives the shard shop (items, prices, layout); reload applies without restart.
- Feature: Shop items support optional Bukkit enchantments in `shop.yml`.
- Feature: Conversion `rate-mode: dollar-batch` ($100 = 1 shard), dollar-multiple validation, per-direction fees. Default servers keep 100 shards per dollar.
- Feature: AFK zone boundary particles (configurable type, interval, view radius).
- Tweak: AFK auto-rejoin when inside a zone after `/afk leave` (`afk.auto-rejoin-in-zone`, default true).
- Docs: CONFIGURATION.md updated for conversion, shop, messages, and zone-particle keys.

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
