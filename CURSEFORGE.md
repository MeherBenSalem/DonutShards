# DonutShards

**A configurable shard economy and reward foundation for modern Paper and Folia servers.**

DonutShards brings an original donut-and-cosmic currency system to Minecraft servers. Balances are stored in a durable SQL ledger, economy operations run asynchronously, and player-facing Bukkit interactions are routed through region-safe schedulers.

## Current features

- Virtual shard balances stored as 64-bit integers
- Add, remove, set, and transfer operations
- Atomic taxed player transfers
- Negative-balance and overflow protection
- Idempotent transaction keys and transaction history storage
- SQLite storage by default
- Optional MariaDB and compatible MySQL storage
- HikariCP connection pooling
- Basic recurring shard rewards
- Original Cosmic Confection Exchange inventory GUI
- MiniMessage-formatted messages
- Asynchronous public Java API
- Paper and genuine Folia scheduler abstraction
- Configurable balance maximum and transfer tax

## Commands

- `/shards balance [player]`
- `/shards pay <player> <amount>`
- `/shardshop`
- `/afk leave`
- `/shardmanager give <player> <amount>`
- `/shardmanager take <player> <amount>`
- `/shardmanager set <player> <amount>`

Aliases for `/shards` are `/donutshards` and `/dshards`.

## Compatibility

- **Server software:** Paper and Folia
- **Minecraft:** 1.20.1 through 26.1.2
- **Plugin bytecode:** Java 21
- **Minecraft 26.1.2 runtime:** Java 25, as required by Paper/Folia

Minecraft 26.2 is not currently claimed because a stable dual-platform Paper/Folia release was not available when this build was produced.

## Installation

1. Download the DonutShards JAR.
2. Place it in the server's `plugins` directory.
3. Start the server and wait for the database-ready message.
4. Stop the server before editing generated configuration files.
5. Restart and verify the selected database connection.

SQLite works without additional setup. MariaDB/MySQL users should configure a dedicated, least-privilege database account in `database.yml`.

## Important release status

Version 1.0.0 is an early functional foundation. Advanced AFK-zone management, persistent shop delivery reconciliation, transaction rollback commands, leaderboards, Vault/PlaceholderAPI hooks, migration tools, and administration editors are planned but are not complete in this build.

## Support and bug reports

Include the following when reporting a problem:

- DonutShards version
- Paper or Folia version
- Minecraft and Java versions
- Sanitized relevant console logs
- Exact reproduction steps

Never publish database passwords, private network addresses, or the contents of a production database.

## License

Copyright © 2026 Nightbeam. All rights reserved. Redistribution, modification, or resale requires prior written authorization.
