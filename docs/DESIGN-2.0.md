# DonutShards 2.0 "Sweet Spots": audit, market gaps and plan

This document records what 1.5.2 actually did (read from the code and the shipped YAML, not the README), what
server owners and players expect from a shard economy with AFK zones today, where 1.5.2 fell short, and what 2.0
changes. Every 2.0 claim below is implemented in the one release jar and checked in game on Paper and Folia for
Minecraft 1.20.1, 1.21.x, 26.1.2, 26.2 and 26.3 by `tools/qa/shard_checks.py` (see
[CONTRIBUTING](../CONTRIBUTING.md#testing-in-game) and [QA-2.0.0](QA-2.0.0.md)).

## 1. What DonutShards is

A **server plugin** (not a mod): one jar for Paper and Folia (and Paper forks such as Purpur), no client install.
It gives a server a second currency, *shards*, earned by standing in AFK zones (or, at a reduced rate, AFK "at
home"), by kills, and through Vault conversion, and spent in an inventory shop. Plugin name `DonutShards`, main class
`com.nightbeam.donutshards.DonutShardsPlugin`, commands `/shards` (`/shard`), `/shardshop`, `/afk`,
`/shardmanager` (`/donutshards`, `/dshards`), permissions `shards.*`, PlaceholderAPI identifier `donutshard`,
bStats id 33616, Modrinth project `4krPhA6H`.

## 2. Audit of 1.5.2

| Area | What the code did |
|------|-------------------|
| Platforms | Built against the Paper 26.3 API with Java 21 bytecode, `api-version: 1.20`, `folia-supported: true`. The README and the publish workflow also claimed **Spigot and Bukkit**, but the code calls Paper-only API on every path (Adventure components, `getGlobalRegionScheduler`, `teleportAsync`, `sendActionBar(Component)`), so on Spigot it cannot enable. Java 21 bytecode also locked out 1.20.1 servers that still run Java 17. Only 1.20.1 and 26.x had ever been booted. |
| Storage | SQLite (default) or MariaDB through HikariCP. Tables `players`, `transactions` (ledger with idempotency keys), plus `purchases`, `reward_history`, `afk_sessions`, `shop_limits`, `audit_logs`, `permission_grants`, `network_fingerprints`, **all created and never used**. `players.last_name` and `reward_streak` never written. |
| AFK zones | Spheres only, created where the admin stands (`/shardmanager zone create <name>`), radius from `default-radius`. No per-zone settings: one global `rewards.zone-shards` every `rewards.interval-seconds`. A particle ring marks the radius. |
| Rewards | One async timer for the whole server; a player who walked in one second before the tick was paid a full interval, one who walked in just after waited almost two. No feedback until the reward arrived, then a **title every interval** plus an action bar and a sound. `rewards.yml` (a full reward-definition format) and `anti-abuse.yml` were written to disk on every start and **never read**. |
| Home AFK | `/afk` pays `afk.home-mode.shard-multiplier` (0.5) anywhere until the player moves a block. |
| Permissions | `PERMISSIONS.md` promised `shards.afk.zone.<zone>` and `shards.multiplier.<value>`; neither existed. `plugin.yml` declared `shards.history`, `shards.admin.give/take/set/reset/rollback/migrate/gui/debug/shop` and `shards.bypass.cooldown/limit`; only `shards.admin` and `shards.admin.reload/zone` were ever checked. |
| Transfers | Atomic and taxed in SQL, but `transfer.minimum`, `maximum` and `confirmation-threshold` were never read, and the `received` message existed but the recipient was never told. |
| Kill rewards | The idempotency key was `kill:<player>:<source>:<epoch second>`, so a second kill in the same second was treated as a duplicate: **no shards, but the "+N Shard" action bar still showed**. |
| Shop | Static slots from `shop.yml`, optional confirmation screen and console commands. `gui.yml` `filler`, `previous`, `next` and `back` were never used; the leaderboard arrows were hard-coded. |
| Placeholders | `%donutshard_balance%` ran a **blocking SQL query on the calling thread** (up to 2 s, the main/region thread for scoreboards); top-10 refreshed with another blocking query. |
| API | `DefaultDonutShardsApi` copied the transfer tax at startup, so `/shardmanager reload` never changed the API's tax. |
| Console | No way to read a balance from the console (`/shards balance` is players-only), so no automation could check one. |
| Tooling | 31 unit tests. No in-game QA, no release jars in the repo, a publish workflow that guessed the loader list. |

## 3. What the market offers and what players ask for

Looked at: the shard/AFK-zone economy plugins on Modrinth, CurseForge (Bukkit plugins), Hangar and the paid
marketplaces, their feature lists, update logs and buyer reviews, and the standalone AFK-zone and daily-reward
plugins people pair with them. (Competitor names are deliberately not recorded in this repository.)

Recurring expectations and complaints:

1. **AFK zones drawn like a region** (two corners, a selection wand), several zones with **their own payout and
   permission** (VIP zone, event zone). Several plugins require WorldGuard for this; reviewers dislike the extra
   dependency.
2. **"How long until my next shard?"**: an on-screen countdown (action bar or boss bar). Reviewers call plugins
   without one "feels broken".
3. **Abuse**: "players keep earning after logging off in the zone", "people earn walking around the lobby", alt
   accounts. Rewards must stop the moment a player leaves, and only count real time spent in the zone.
4. **Boosters / multipliers**: weekend events ("2x shards for 2 hours") and rank multipliers. The paid plugins sell
   this as a premium feature.
5. **A shop that changes**: rotating offers with limited stock keep players logging in. Only one paid plugin
   advertises it; most shops are static grids.
6. **Leaderboards in the world**, not only in chat: usually done through a separate hologram plugin and
   placeholders.
7. Basics that are often missing: admin take/set, placeholders for balance and top, a transaction history, Folia
   support, SQL storage, current Minecraft versions.

## 4. Gap table

| Expectation | Typical plugin | DonutShards 1.5.2 | DonutShards 2.0 |
|-------------|----------------|-------------------|-----------------|
| Zone shape | WorldGuard region or two corners | sphere around the admin | **box drawn with a selection wand** (or pos1/pos2), spheres still load |
| Per-zone payout | rare (premium) | one global rate | **per-zone shards, interval and permission**, editable in game |
| Countdown | action bar on a few | none until paid | **boss bar** with zone name, countdown and session total |
| Fair timing | fixed server timer | fixed server timer | **per-player timer**: the interval starts when you enter |
| Leave feedback | none | "Left zone" | **session summary**: time spent and shards earned |
| Boosters | premium | none | **timed server boosters** (persist through restarts) with a boss bar, **rank multipliers** `shards.multiplier.<x>` |
| Rotating shop | one premium plugin | static | **Daily Deals**: deterministic rotation, countdown in the lore, per-player stock |
| In-world leaderboard | needs a hologram plugin | none | **built-in leaderboard boards** (text displays), no extra plugin |
| History | rare | ledger stored, no command | **`/shards history`** for players, `/shardmanager history <player>` for staff |
| Placeholders | balance, top | blocking balance, top | non-blocking, plus zone, countdown, session, booster |
| Platforms | Paper/Folia, often one version | claimed Spigot (false), Java 21 | **Paper + Folia 1.20.1 to 26.3, Java 17 bytecode**, verified on ten servers |

## 5. What 2.0 builds, and why

The identity stays: a sweet, cosmic, DonutSMP-style shard economy that is fair, Folia-safe and needs no other
plugin. The name stays **DonutShards** (it is what server owners search for and what existing servers have
installed); 2.0 gets the release name **"Sweet Spots"** after its headline.

### Headline: Sweet Spots (AFK zones 2.0)

* **Box zones from a wand.** `/shardmanager zone wand` gives a tagged blaze rod: left-click a block for corner 1,
  right-click for corner 2 (the click is cancelled, nothing breaks). `/shardmanager zone pos1|pos2` sets a corner
  where you stand. `/shardmanager zone create <name>` makes a box when both corners are set in the same world,
  otherwise the 1.5 sphere around you. Existing sphere zones in `zones.yml` load unchanged; boxes are saved as
  `shape: box` with `min`/`max`.
* **Per-zone payout.** Each zone can set `shards`, `interval-seconds`, `permission` and `display-name`
  (`/shardmanager zone set <zone> <key> <value>`); unset keys fall back to `rewards.*`. A zone with a permission
  ignores players without it (they get one message when they walk in).
* **Per-player timer and a boss bar.** Every player in a zone (or home AFK) has their own progress; the boss bar
  shows the zone's display name, the shards of the next payout, the seconds left and what they earned this session,
  and fills up as the interval passes. Leaving resets the timer, so logging off or walking out never pays.
* **Session summary.** Leaving a zone after earning prints the time spent and the shards earned there.

### Boosters and multipliers

* `/shardmanager booster start <multiplier> <duration> [reason]` starts a server-wide booster (one at a time; a new
  one replaces the old), `booster stop` ends it, `/shards booster` shows it. It is stored in `state.yml`, so a
  restart keeps the remaining time. While it runs every player sees a booster boss bar with the time left.
* Rank multipliers: the highest `shards.multiplier.<x>` permission a player has (for example
  `shards.multiplier.1.5`) multiplies their AFK and kill rewards.
* The payout is `floor(base × mode × rank × booster)`, at least 1 (mode is 1 in a zone, the home multiplier at
  home). The rule lives in a Minecraft-free class with unit tests.

### Daily Deals

A `deals:` section in `shop.yml`: a pool of offers, how many are shown, which slots, how often they rotate and a
per-player limit. The offers for each rotation are picked from the pool with a seed made of the rotation number
and `server-id`, so every player (and every restart) sees the same deals until the rotation ends, then they all
change. The lore shows the time left and how many the player may still buy. The limit is enforced in the same SQL
transaction as the payment (the 1.x `shop_limits` table finally gets used), so two quick clicks can never exceed
it. `shards.bypass.limit` skips it.

### Leaderboard boards

`/shardmanager board create <name> [world x y z]` puts a leaderboard in the world: a text display entity with the
top 10, refreshed every `leaderboard.board-refresh-seconds`. The entities are not saved with the chunk (no
duplicates after a crash); the plugin spawns and updates them on the region thread of their location (Folia-safe)
and removes them on shutdown. Boards are stored in `boards.yml`.

### Fixes and smaller additions

* `/shards history [page]` and `/shardmanager history <player> [page]` read the ledger; `/shardmanager info
  <player>` prints balance, totals, AFK state and the active booster (works from the console).
* The recipient of `/shards pay` is told; `transfer.minimum`/`maximum` are enforced; payments at or above
  `transfer.confirmation-threshold` need `confirm` as a fourth argument.
* Kill rewards use a unique key per kill (second kill in a second now pays) and honour `shards.bypass.cooldown`.
* `shards.admin.give/take/set/zone/reload` are checked; permissions that never did anything (`reset`, `rollback`,
  `migrate`, `gui`, `debug`, `shop`) are no longer declared.
* Placeholders never block: balances come from a cache refreshed in the background. New:
  `%donutshard_zone%`, `%donutshard_next_reward%`, `%donutshard_session%`, `%donutshard_booster%`,
  `%donutshard_booster_time%`.
* The API reads the live tax. `players.last_name` is filled on join and used by the leaderboard and history.
* The leaderboard GUI uses `gui.yml` `previous`, `next` and `filler`.
* `rewards.yml` and `anti-abuse.yml` are no longer shipped (they were never read); copies on disk are ignored.
* Java 17 bytecode, compiled against the Paper 1.20.1 API, and the build compiles the same sources against the
  1.21.1, 26.1.2, 26.2 and 26.3 APIs to catch removed methods. The docs stop claiming Spigot/Bukkit.

### Compatibility promise

Every command, permission (except the six no-op nodes above), config key, message key, table and the PlaceholderAPI
identifier from 1.5.2 keep working. New keys are merged into existing files by `YamlKeyMerger` without touching
edited values; `zones.yml` spheres load as before; the database schema only gains an index. No migration step is
needed; downgrading to 1.5.2 also loads (it ignores the new keys and reads a box zone as a sphere around its centre).

## 6. The plan

1. Code (one source tree, `com.nightbeam.donutshards`): `zone/` (shapes, wand, per-zone settings, per-player
   timers, boss bar), `reward/` (payout rule, boosters), `shop/` (deals rotation, limits), `gui/` (boards), commands,
   placeholders. Minecraft-free logic (`RewardMath`, `DealRotation`, `ZoneShape`, `DurationParser`, `BoosterState`,
   history paging) gets JUnit tests; the SQL parts are tested against SQLite.
2. Build: Java 17 release jar; `./gradlew compileMatrix` compiles against all five Paper APIs.
3. QA (`tools/qa/`): real Paper and Folia servers for 1.20.1, 1.21.x, 26.1.2, 26.2 and 26.3 with the **release jar**,
   a legacy 1.5.2 data folder to prove upgrades, RCON-driven server checks and a real Minecraft client under Xvfb
   driven with xdotool (wand clicks, shop clicks, commands typed in chat, screenshots of the boss bars, the shop and
   the boards).
4. Marketing from screenshots captured by a script, rendered with `marketing/source/render.cjs`.
5. Docs, release jar, hardened publish workflow (not run).

### Out of scope for 2.0

* Cross-server balances over Redis: MariaDB already shares balances between servers that point at the same database.
* WorldGuard region zones: box zones cover the need without a dependency.
* IP/alt-account limits: they need hashed IP storage and policy decisions per server; left for a later release.
