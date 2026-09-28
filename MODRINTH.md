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
| Paper, Folia, Purpur, Spigot, Bukkit | 1.20.1–26.3 | Java 21 (Java 25 for 26.1.2+) |

The plugin targets Java 21 bytecode. See `release/supported-minecraft.json` for the full version list.

## Commands

```text
/shards balance [player]
/shards pay <player> <amount>
/shards top [chat]
/shards convert <shards|money> <amount>
/shardshop
/afk | home | list | join [name] | leave | zone | info
/shardmanager give|take|set <player> <amount>
/shardmanager zone create|delete|list <name>
/shardmanager reload
```

`/shard` aliases `/shards`. `/donutshards` and `/dshards` alias `/shardmanager`.

## Setup

1. Put the JAR in `plugins/`.
2. Start the server once to generate configuration and the SQLite database.
3. Stop the server before changing storage settings.
4. Configure `database.yml` if MariaDB/MySQL is required.
5. Restart and confirm that DonutShards reports a successful connection.

Vault and PlaceholderAPI are optional soft dependencies.

## Version 1.5.2

- Minecraft 26.3 support (compiled against Paper API `26.3.build.49-alpha`, boot-tested on Paper 26.3 build 133)
- Registry lookups for sounds, particles, enchantments, and entity types

## Version 1.5.1

- Folia-safe `/afk zone` teleport (`teleportAsync`)
- Home AFK cancel-on-move, AFK enter/leave messages, GUI leaderboard, convert currency-first (see 1.5.0)

## Reporting issues

Provide the DonutShards, server, Minecraft, and Java versions; sanitized logs; and repeatable steps. Remove credentials and private server information before posting.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
