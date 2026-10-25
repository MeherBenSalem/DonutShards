# DonutShards 1.4.0 — Patch Notes

**Release date:** August 2026

## Summary

DonutShards 1.4.0 merges missing YAML keys on upgrade, adds shop purchase confirmations and post-purchase commands, registers PlaceholderAPI placeholders, and ships bStats metrics plus a Modrinth update checker. The release jar targets Paper, Folia, Purpur, Spigot, and Bukkit on Minecraft 1.20.1 through 26.2.

## Configuration merge

On enable and `/shardmanager reload`, the plugin deep-merges **missing keys only** from jar defaults into existing `config.yml`, `messages.yml`, `shop.yml`, `gui.yml`, and other managed files. Operator values are never overwritten. Upgraded servers automatically gain keys such as `afk-joined` without losing a custom `prefix`.

## Shop

| Feature | Detail |
|---|---|
| `confirmation:` per item | Opens confirm/cancel GUI from `gui.yml` when enabled |
| `/shards confirmation on\|off` | Player toggle stored in `player-prefs.yml` (default on) |
| `commands:` per item | Console commands after purchase with `%player%`, `%uuid%`, `%item%`, `%price%` |

## PlaceholderAPI

Registered when PlaceholderAPI is present:

| Placeholder | Meaning |
|---|---|
| `%donutshard_balance%` | Viewer balance |
| `%donutshard_top_<1-10>%` | Player name at rank |
| `%donutshard_top_bal_<1-10>%` | Balance at rank |

## Metrics and updates

- **bStats** plugin id **33616** — set `bstats: false` in `config.yml` to opt out.
- **Modrinth update check** — async check against project `4krPhA6H`; ops are notified in-game when a newer release exists. Set `update-check: false` to disable.

## Commands

- `/shards top` — shard balance leaderboard (top 10).
- `/shard` alias for `/shards`.

## Build

- Artifact: `DonutShards-1.4.0-paper-folia-mc1.20.1-26.2.jar` (shadow jar with relocated bStats).
- Requires Java 21.

## Compatibility

- **Platforms:** Paper, Folia, Purpur, Spigot, Bukkit
- **Minecraft:** 1.20.1 – 26.2 (see `release/supported-minecraft.json`)

## Upgrade notes

1. Replace the jar and restart (or reload after swapping on a test server).
2. Run `/shardmanager reload` if you only need message/shop merges without restart.
3. Existing `messages.yml` and `shop.yml` keep your values; new keys appear automatically.
