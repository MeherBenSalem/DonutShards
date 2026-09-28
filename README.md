# DonutShards

DonutShards is an original donut-and-cosmic shard economy for modern Paper and Folia servers. It provides an asynchronous SQL-backed balance ledger, transfers, recurring rewards, configurable AFK zones with in-game setup, an inventory shop, a public API, and configurable MiniMessage presentation.

## Compatibility

- Java 21 bytecode.
- Paper, Folia, Purpur, Spigot, and Bukkit **1.20.1 through 26.3** (see `release/supported-minecraft.json`).
- SQLite by default; MariaDB and compatible MySQL servers are optional.
- PlaceholderAPI and Vault are soft dependencies and never required for startup.

## Install

Copy the release JAR to `plugins/`, start the server once, edit files under `plugins/DonutShards/`, and restart. See [INSTALLATION.md](INSTALLATION.md), [COMMANDS.md](COMMANDS.md), and [PERMISSIONS.md](PERMISSIONS.md).

Report problems with the server version, Java version, DonutShards version, platform, relevant sanitized logs, and reproduction steps. Never publish database passwords or the generated server data directory.

## License

Licensed under the [Apache License, Version 2.0](LICENSE). See [NOTICE](NOTICE) for attribution.
