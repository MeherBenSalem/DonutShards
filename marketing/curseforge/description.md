![DonutShards](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/banner.png)

**The shard economy your players will AFK for.** Draw AFK zones with a wand and give each one its own payout. Players
see every payout coming on a boss bar, spend their shards in a shop whose **Daily Deals** change every day, and fight
for a spot on **leaderboards that stand in your world**. Run a **booster** for the weekend and everyone feels it.

Paper, Folia and Purpur · Minecraft 1.20.1 to 26.3 · one jar · server-side only, nothing to install for players ·
SQLite or MariaDB/MySQL

![What's new in 2.0](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/header-whats-new.png)

Version 2.0 "Sweet Spots" is the biggest DonutShards update yet:

- **Sweet Spot Wand**: draw box AFK zones with two clicks (1.x spheres still work)
- **Per-zone payouts**: shards, interval, permission and a display name for every zone
- **AFK boss bar** with a live countdown and the session total, and a **summary** when you leave
- **Per-player timers**: the interval starts when you walk in; walking out or logging off never pays
- **Server boosters** with their own boss bar, surviving restarts, and **rank multipliers** (`shards.multiplier.<x>`)
- **Daily Deals** in the shop: rotating offers with a countdown and per-player limits
- **Leaderboard boards** in the world, no hologram plugin needed
- **`/shards history`**, `/shardmanager info` and `history`, new placeholders, API events
- **Fixes** from 1.x: same-second kills paid nothing, a blocking balance placeholder, ignored transfer limits and more
- Your balances, zones and settings carry over: no migration

![Sweet Spots](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/header-sweetspots.png)

![Sweet Spots](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/card-sweetspots.png)

- `/shardmanager zone wand`, left-click one corner, right-click the other, `/shardmanager zone create spawn`. The zone
  covers the blocks you picked plus two blocks above them, so picking the floor is enough. Or use `zone pos1` and
  `pos2` where you stand.
- `/shardmanager zone set <zone> shards 3`, `interval 30s`, `permission shards.zone.vip`, `display-name <MiniMessage>`.
  Players without a zone's permission are told once and earn nothing there.
- Standing in a zone shows a **boss bar**: the zone's name, the next payout, the seconds left and the shards earned
  this session. Each payout plays a sound and shows a title and an action bar (each can be turned off).
- Every player earns on **their own clock**, from the moment they walk in. Leave and the clock stops; leave after
  earning and chat tells you how long you stayed and what you made.
- `/afk` pays a reduced rate wherever you stand until you move; `/afk zone` teleports you to a zone.
- Zones are outlined with particles.

![Boosters](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/header-boosters.png)

![Shard Boosters](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/card-boosters.png)

- `/shardmanager booster start 2 1h Weekend Sugar Rush`: AFK (and, by default, kill) rewards are doubled for
  everyone. The whole server is told, everyone sees a booster bar with the time left, and a restart keeps it running.
- Ranks: give `shards.multiplier.1.5` (or any number) with your permissions plugin; the highest one counts.
- A payout is `base × (1 in a zone, 0.5 at home) × rank × booster`, rounded down, at least 1.

![Daily Deals](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/header-deals.png)

![Daily Deals](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/card-deals.png)

- `/shardshop` is a configurable inventory shop (items, enchantments, lore, confirmation screens, console commands).
- **Daily Deals**: a row of offers drawn from a pool you define. Every player sees the same deals, a restart does not
  reshuffle them, and every day (or every `rotation-hours`) they change.
- The tooltip shows when the deals rotate and how many the player may still buy. The limit is checked in the same
  database transaction as the payment, so fast double clicks cannot get around it.

![Leaderboards](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/header-boards.png)

![Leaderboards in the world](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/card-boards.png)

- `/shardmanager board create top` places a live top 10 where you stand, refreshed every 30 seconds. It is a text
  display, so no hologram plugin is needed, and it never duplicates after a crash.
- `/shards top` opens a menu with player heads; `/shards top chat` prints it.
- PlaceholderAPI: `%donutshard_balance%`, `%donutshard_top_1%`…`10`, `%donutshard_top_bal_1%`…`10`,
  `%donutshard_zone%`, `%donutshard_next_reward%`, `%donutshard_session%`, `%donutshard_booster%`,
  `%donutshard_booster_time%`.

![The economy](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/header-economy.png)

![Fair by design](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/card-economy.png)

- Every shard is a row in a SQL ledger (SQLite by default, MariaDB/MySQL to share balances between servers).
  `/shards history` shows a player where their shards came from; staff use `/shardmanager history <player>`.
- `/shards pay` is atomic, with an optional tax, minimum and maximum, and a `confirm` step for large amounts.
- Kill rewards per mob type and for players (with a cooldown; spawner mobs pay nothing).
- `/shards convert` to and from money with Vault.
- An async Java API with reward, purchase, transfer and zone events.

![Commands](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/header-commands.png)

| Players | Staff (`/shardmanager`, `/dshards`) |
|---------|-------------------------------------|
| `/shards [balance]`, `pay`, `top`, `history`, `booster`, `convert` | `give`, `take`, `set`, `info`, `history` |
| `/shardshop` | `zone wand`, `pos1`, `pos2`, `create`, `set`, `info`, `list`, `delete` |
| `/afk`, `/afk zone`, `list`, `join`, `leave`, `info` | `booster start`, `stop`; `board create`, `delete`, `list`; `reload` |

![One jar, every server](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/card-platforms.png)

![Download](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/header-downloads.png)

**One jar for every supported server**: `donutshards-paper-1.20.1-26.3-2.0.0.jar`. Put it in `plugins/`.

| Server | Minecraft | Java |
|--------|-----------|------|
| Paper, Purpur or another Paper fork | 1.20.1 – 1.20.4 | 17+ |
| Paper, Purpur or another Paper fork | 1.20.5 – 1.21.11 | 21+ |
| Paper, Purpur or another Paper fork | 26.1 – 26.3 | 25+ |
| Folia | 1.20.1 – 26.3 | as above |

Spigot and CraftBukkit are not supported: DonutShards uses Paper's API. Optional: Vault (conversion), PlaceholderAPI.

![FAQ](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/header-faq.png)

**I run 1.x. Do I lose anything?** No. Balances, zones, messages and settings carry over and gain the new options.
Your shop keeps its layout; Daily Deals start switched off until you enable them and pick free slots.

**Does it need WorldGuard or a hologram plugin?** No. Zones and leaderboards are built in.

**Can players earn while logged off or walking around the lobby?** No. Only time spent inside a zone (or in home AFK,
standing still) counts, on the player's own clock, and only while they are online.

**Does it work on Folia?** Yes. Everything runs on the thread that owns the player or the location.

**Do players need a mod?** No, it is a server plugin.

![Gallery](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/header-gallery.png)

![A Sweet Spot](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/screenshots/overview.jpg)

![Earning in a zone](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/screenshots/sweetspot.jpg)

![A booster starts](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/screenshots/booster.jpg)

![The shop and its Daily Deals](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/screenshots/shop.jpg)

![A leaderboard board](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/screenshots/board.jpg)

*Screenshots are single unedited frames from Minecraft 26.3 on Paper. The donut platform was built with commands;
the other names on the leaderboard are demo balances set with `/shardmanager set`.*

![](https://raw.githubusercontent.com/MeherBenSalem/DonutShards/master/marketing/images/divider.png)

[Discord](https://discord.gg/SAmd9fhNhr) · [Source and issues](https://github.com/MeherBenSalem/DonutShards) · [Support us](https://nightbeam.dev/support) · Made by NightBeam Studio
