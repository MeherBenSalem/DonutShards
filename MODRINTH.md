# DonutShards

> A configurable shard economy and reward foundation for modern Paper and Folia servers.

DonutShards is an original donut-and-cosmic virtual currency plugin built around safe asynchronous persistence and region-aware server operations.

## Highlights

- Integer-based shard balances with configurable maximums
- Atomic, taxed transfers with insufficient-funds protection
- SQLite, MariaDB, and compatible MySQL storage
- HikariCP connection pooling and prepared SQL statements
- Idempotent transaction records
- Recurring rewards without per-player tick tasks
- In-game AFK zone creation with `/shardmanager zone` and player `/afk` commands
- MiniMessage presentation
- Original inventory shop interface with purchase confirmations
- PlaceholderAPI placeholders (`%donutshard_balance%`, top names and balances)
- Async-first Java API
- Paper and Folia scheduling implementations

## Supported platforms

| Platform | Minecraft versions | Runtime |
| --- | --- | --- |
| Paper, Folia, Purpur, Spigot, Bukkit | 1.20.1–26.2 | Java 21 (Java 25 for 26.1.2+) |

The plugin targets Java 21 bytecode. See `release/supported-minecraft.json` for the full version list.

## Commands

```text
/shards balance [player]
/shards pay <player> <amount>
/shards top
/shardshop
/afk list|join [name]|leave|info
/shardmanager zone create|delete|list <name>
/shardmanager reload
/shardmanager take <player> <amount>
/shardmanager set <player> <amount>
```

`/donutshards` and `/dshards` are aliases for `/shards`.

## Setup

1. Put the JAR in `plugins/`.
2. Start the server once to generate configuration and the SQLite database.
3. Stop the server before changing storage settings.
4. Configure `database.yml` if MariaDB/MySQL is required.
5. Restart and confirm that DonutShards reports a successful connection.

Vault and PlaceholderAPI are optional soft dependencies.

## Version 1.4.0

- YAML key merge on upgrade and reload
- Shop purchase confirmations and per-item post-purchase commands
- PlaceholderAPI expansion with balance and leaderboard placeholders
- `/shards top` command
- bStats metrics and Modrinth update checker (configurable)

## Reporting issues

Provide the DonutShards, server, Minecraft, and Java versions; sanitized logs; and repeatable steps. Remove credentials and private server information before posting.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
