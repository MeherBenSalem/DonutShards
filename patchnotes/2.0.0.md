# DonutShards 2.0.0 "Sweet Spots"

The biggest DonutShards update yet: AFK zones you draw in game with their own payouts, a boss bar that counts down
to every payout, server-wide boosters, a shop with Daily Deals and leaderboards that stand in your world. One jar for
Paper, Folia and Purpur on Minecraft 1.20.1 to 26.3. Your balances, zones and settings carry over: no migration.

## New

- **Sweet Spot Wand** (`/shardmanager zone wand`): left-click and right-click two blocks, then
  `/shardmanager zone create <name>` makes a box zone. `zone pos1|pos2` sets a corner where you stand. Spheres from
  1.x still work.
- **Per-zone settings**: `/shardmanager zone set <zone> shards|interval|permission|display-name <value>`. A VIP zone
  that pays 3 shards every 30 seconds is two commands.
- **AFK boss bar**: zone name, the next payout, a live countdown and the session total.
- **Per-player timers**: the interval starts when you walk in; walk out (or log off) and it stops. No more payouts
  for arriving one second before a server-wide tick.
- **Session summary** when you leave a zone: time spent and shards earned.
- **Boosters**: `/shardmanager booster start 2 1h Weekend` doubles AFK and kill rewards for everyone, with a
  booster boss bar; it survives restarts. `/shards booster` shows it.
- **Rank multipliers**: `shards.multiplier.<x>` (the highest wins).
- **Daily Deals**: a rotating row of offers in `/shardshop`, the same for every player, with a countdown and a
  per-player limit that is checked together with the payment.
- **Leaderboard boards**: `/shardmanager board create <name>` puts a live top 10 in the world (no hologram plugin).
- **History**: `/shards history` for players, `/shardmanager history <player>` and `/shardmanager info <player>` for
  staff (also from the console and RCON).
- **Placeholders**: `%donutshard_zone%`, `%donutshard_next_reward%`, `%donutshard_session%`,
  `%donutshard_booster%`, `%donutshard_booster_time%`.
- **API**: `getActiveBoosterMultiplier()`, `getHistory(...)`; `ShardRewardEvent`, `ShardPurchaseEvent` and
  `ShardTransferEvent` are now fired (they never were in 1.x).

## Fixed

- A second kill within the same second paid nothing (but still showed "+N Shard").
- `%donutshard_balance%` queried the database on the server thread; placeholders never block now.
- `/shards pay` never told the recipient; `transfer.minimum`, `maximum` and `confirmation-threshold` were ignored.
- Escaped placeholder values showed a stray `\` before `>` in chat.
- `shards.admin.give/take/set` and `shards.bypass.cooldown` were declared but not checked.
- The API kept the transfer tax from startup after `/shardmanager reload`.
- `gui.yml` `previous`, `next` and `filler` were ignored by the leaderboard menu.

## Changed

- Requires Paper, Folia or a Paper fork. 1.x listings claimed Spigot and Bukkit, which could never run it.
- Java 17 is enough on 1.20.1 servers (1.x needed Java 21).
- `rewards.yml` and `anti-abuse.yml` are no longer created (no version ever read them); old copies are ignored.
- Six permission nodes that did nothing are no longer declared (`shards.admin.reset`, `rollback`, `migrate`, `gui`,
  `debug`, `shop`).

## Upgrading

Drop in the new jar. New config keys are added to your files without touching your values. An existing `shop.yml`
keeps its layout and gets Daily Deals **switched off** (set `deals.enabled: true` and pick free `slots`). See
[CONFIGURATION.md](https://github.com/MeherBenSalem/DonutShards/blob/master/CONFIGURATION.md).
