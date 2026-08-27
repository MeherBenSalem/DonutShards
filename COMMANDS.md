# Commands

- `/shards [balance [player]]` — view a balance.
- `/shards pay <online-player> <amount>` — atomically transfer shards.
- `/shards convert <shards|money> <amount>` — convert to money (spend shards) or to shards (spend money); requires Vault.
- `/shards top` — open the GUI leaderboard (`leaderboard.page-size` in config).
- `/shards top chat` — print the leaderboard in chat.
- `/shards confirmation <on|off>` — toggle shop purchase confirmation menus (overrides per-item confirmation).
- `/shardshop` — open the Cosmic Confection Exchange.
- `/afk` or `/afk home` — enable stay-in-place AFK (reduced rewards, no zone required).
- `/afk list` — list configured AFK zones.
- `/afk join [name]` — join a zone (auto-detects when standing inside).
- `/afk leave` — leave the current AFK zone or cancel home mode.
- `/afk zone` — teleport to the closest AFK zone center (or `afk.zone-teleport.preferred-zone`).
- `/afk info` — show your current AFK zone or home mode.
- `/shardmanager give|take|set <player> <amount>` — ledger administration.
- `/shardmanager zone create|delete|list <name>` — manage AFK zones in-game.
- `/shardmanager reload` — reload configuration including zones.

Aliases for `/shards`: `/shard`.
Aliases for `/shardmanager`: `/donutshards`, `/dshards`.
