# Commands

## Players

| Command | What it does | Permission |
|---------|--------------|------------|
| `/shards` or `/shards balance [player]` | Your balance (another player's with `shards.balance.others`) | `shards.use` |
| `/shards pay <player> <amount> [confirm]` | Send shards to an online player. Amounts must be within `transfer.minimum`–`maximum`; at or above `transfer.confirmation-threshold` you add `confirm` | `shards.pay` |
| `/shards top [chat]` | Leaderboard GUI (or chat list) | `shards.top` |
| `/shards history [page]` | Your last transactions, newest first, 8 per page | `shards.history` |
| `/shards booster` | The running server booster and its time left | `shards.use` |
| `/shards convert <shards\|money> <amount>` | Convert through Vault (`money`: spend shards; `shards`: spend money) | `shards.convert` |
| `/shards confirmation <on\|off>` | Turn shop confirmation screens on or off for you | `shards.use` |
| `/shardshop` | The shop, with the Daily Deals | `shards.shop` |
| `/afk` or `/afk home` | Home AFK: reduced rewards where you stand, until you move | `shards.afk` |
| `/afk zone` | Teleport to the preferred or closest AFK zone | `shards.afk` |
| `/afk list`, `/afk join [zone]`, `/afk leave`, `/afk info` | List, join, leave, show your AFK state | `shards.afk` |

`/shard` is an alias of `/shards`.

## Staff (`/shardmanager`, aliases `/donutshards`, `/dshards`)

| Command | What it does | Permission |
|---------|--------------|------------|
| `give\|take\|set <player> <amount>` | Change a balance (recorded in the ledger) | `shards.admin.give/take/set` |
| `info <player>` | Balance, totals, AFK mode and zone, next payout, session total, rank multiplier, booster | `shards.admin.info` |
| `history <player> [page]` | A player's ledger | `shards.admin.info` |
| `zone wand` | The Sweet Spot Wand: left-click a block for corner 1, right-click for corner 2 | `shards.admin.zone` |
| `zone pos1\|pos2` | Set a corner at the block you stand on | `shards.admin.zone` |
| `zone create <name>` | A box from your two corners, or a sphere (radius `default-radius`) around you when no corners are set. From the console: `zone create <name> <world> <x1> <y1> <z1> <x2> <y2> <z2>` | `shards.admin.zone` |
| `zone set <zone> <shards\|interval\|permission\|display-name> <value\|reset>` | Per-zone payout, interval (`30`, `45s`, `2m`), permission and MiniMessage display name; `reset` goes back to the default | `shards.admin.zone` |
| `zone info <zone>`, `zone list`, `zone delete <zone>` | Inspect and remove zones | `shards.admin.zone` |
| `booster start <multiplier> <duration> [reason]` | Server-wide booster, e.g. `booster start 2 1h30m Weekend` (one at a time; a new one replaces the old) | `shards.admin.booster` |
| `booster stop`, `booster` | End it, show it | `shards.admin.booster` |
| `board create <name> [world x y z]` | An in-world leaderboard two blocks above you (or at the coordinates) | `shards.admin.board` |
| `board delete <name>`, `board list`, `board refresh` | Manage boards | `shards.admin.board` |
| `reload` | Reload every file (missing keys are merged in first) | `shards.admin.reload` |

Console and RCON can run every staff command; `info` and `history` answer inline so scripts can read them.
