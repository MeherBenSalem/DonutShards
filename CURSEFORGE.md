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
- Recurring shard rewards and kill rewards
- Original Cosmic Confection Exchange inventory GUI with purchase confirmations
- PlaceholderAPI placeholders for balance and top-10 leaderboard
- MiniMessage-formatted messages
- Asynchronous public Java API
- Paper and genuine Folia scheduler abstraction
- Configurable balance maximum and transfer tax
- Vault integration (soft dependency)

## Commands

- `/shards balance [player]`
- `/shards pay <player> <amount>`
- `/shards top [chat]`
- `/shards convert <shards|money> <amount>`
- `/shardshop`
- `/afk` / `/afk home` / `/afk zone` / `/afk leave`
- `/shardmanager give <player> <amount>`
- `/shardmanager take <player> <amount>`
- `/shardmanager set <player> <amount>`

`/shard` aliases `/shards`. `/donutshards` and `/dshards` alias `/shardmanager`.

## Compatibility

- **Server software:** Paper, Folia, Purpur, Spigot, and Bukkit
- **Minecraft:** 1.20.1 through 26.2
- **Plugin bytecode:** Java 21
- **Minecraft 26.1.2+ runtime:** Java 25, as required by Paper/Folia

## Installation

1. Download the DonutShards JAR.
2. Place it in the server's `plugins` directory.
3. Start the server and wait for the database-ready message.
4. Stop the server before editing generated configuration files.
5. Restart and verify the selected database connection.

SQLite works without additional setup. MariaDB/MySQL users should configure a dedicated, least-privilege database account in `database.yml`.

## Version 1.5.1

- Folia-safe `/afk zone` teleport (`teleportAsync`)
- Home AFK cancel-on-move, AFK enter/leave messages, GUI leaderboard, convert currency-first (see 1.5.0)

## Support and bug reports

Include the following when reporting a problem:

- DonutShards version
- Paper or Folia version
- Minecraft and Java versions
- Sanitized relevant console logs
- Exact reproduction steps

Never publish database passwords, private network addresses, or the contents of a production database.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
