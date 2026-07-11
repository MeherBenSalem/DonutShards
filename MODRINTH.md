# DonutShards

> A configurable shard economy and reward foundation for modern Paper and Folia servers.

DonutShards is an original donut-and-cosmic virtual currency plugin built around safe asynchronous persistence and region-aware server operations.

## Highlights

- Integer-based shard balances with configurable maximums
- Atomic, taxed transfers with insufficient-funds protection
- SQLite, MariaDB, and compatible MySQL storage
- HikariCP connection pooling and prepared SQL statements
- Idempotent transaction records
- Grouped recurring rewards without per-player tick tasks
- MiniMessage presentation
- Original inventory shop interface
- Async-first Java API
- Paper and Folia scheduling implementations

## Supported platforms

| Platform | Minecraft versions | Runtime |
| --- | --- | --- |
| Paper | 1.20.1–26.1.2 | Java 21 on older releases; Java 25 for 26.1.2 |
| Folia | 1.20.1–26.1.2 | Java 21 on older releases; Java 25 for 26.1.2 |

The plugin itself targets Java 21 bytecode. Support for Minecraft 26.2 will only be claimed after stable Paper and Folia builds are available and tested.

## Commands

```text
/shards balance [player]
/shards pay <player> <amount>
/shardshop
/afk leave
/shardmanager give <player> <amount>
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

There are no mandatory plugin dependencies.

## Release status

The current 1.0.0 build is an early functional foundation. The following planned systems are not yet complete: advanced AFK zones, durable purchase reconciliation, rollback and migration commands, leaderboards, Vault and PlaceholderAPI hooks, and the full administration editor.

## Reporting issues

Provide the DonutShards, server, Minecraft, and Java versions; sanitized logs; and repeatable steps. Remove credentials and private server information before posting.

## License

Copyright © 2026 Nightbeam. All rights reserved.
