# Configuration

DonutShards writes its files to `plugins/DonutShards/` on first start: `config.yml`, `messages.yml`, `database.yml`,
`zones.yml`, `shop.yml`, `gui.yml` and `kills.yml` (plus `boards.yml`, `state.yml` and `player-prefs.yml` once used).
On every start and on `/shardmanager reload`, keys missing from your files are added from the defaults; values you
changed are never touched. Balances are whole shards stored as 64-bit integers, capped by `maximum-balance`.

Upgrading from 1.x: nothing to do. Your files keep their values and gain the new keys. Two exceptions protect your
layout: an existing `shop.yml` gets the `deals` section **switched off** and no example categories, and the 1.x
`rewards.yml` / `anti-abuse.yml` (never read by any version) are left on disk and ignored.

## config.yml

| Key | Default | What it does |
|-----|---------|--------------|
| `server-id` | `server-1` | Written to every ledger row; also seeds the Daily Deals rotation |
| `maximum-balance` | `9000000000000000` | No balance can go above this |
| `transfer.minimum` / `maximum` | `1` / `1000000` | Allowed `/shards pay` amounts |
| `transfer.tax-basis-points` | `0` | Tax on payments (100 = 1 %), paid by the sender |
| `transfer.confirmation-threshold` | `100000` | Payments at or above this need `confirm` (0 = never) |
| `leaderboard.page-size` | `10` | Rows per `/shards top` page (max 45) |
| `leaderboard.cache-seconds` | `60` | How long `%donutshard_top_*%` reuse the top list |
| `leaderboard.board-refresh-seconds` / `board-rows` | `30` / `10` | In-world boards |
| `conversion.*` | see file | Vault conversion (needs Vault and an economy plugin) |
| `rewards.interval-seconds` / `zone-shards` | `60` / `1` | Default payout for zones without their own, and for home AFK |
| `rewards.title` / `action-bar` / `sound` | `true` | What a player sees and hears when paid |
| `afk.home-mode.enabled` / `shard-multiplier` | `true` / `0.5` | Home AFK and its share of the zone payout |
| `afk.auto-rejoin-in-zone` | `true` | After `/afk leave`, walking inside the zone starts AFK again |
| `afk.zone-teleport.preferred-zone` | `""` | Where `/afk zone` goes; empty = the closest zone |
| `afk.boss-bar.enabled` / `color` | `true` / `PINK` | The AFK boss bar (zone, countdown, session total) |
| `afk.session-summary` | `true` | Time and shards when leaving a zone after earning |
| `afk.zone-particles.*` | see file | Particle outline of every zone (ring for spheres, floor edge for boxes) |
| `boosters.max-multiplier` | `10` | Highest booster `/shardmanager booster start` accepts |
| `boosters.applies-to-kills` | `true` | Boosters also multiply kill rewards |
| `boosters.boss-bar.enabled` / `color` | `true` / `YELLOW` | The booster boss bar every player sees |
| `bstats`, `update-check` | `true` | Anonymous metrics, Modrinth update check |

The payout of one reward is `floor(base × mode × rank × booster)`, at least 1: base is the zone's `shards` (or
`rewards.zone-shards`), mode is 1 in a zone and `afk.home-mode.shard-multiplier` at home, rank is the player's
highest `shards.multiplier.<x>`, booster the running booster.

## zones.yml

Created in game; you rarely edit it by hand.

```yaml
default-radius: 10.0
zones:
  lobby:                 # 1.x sphere
    world: world
    x: 0.5
    y: 64.0
    z: 0.5
    radius: 6.0
  sweet-spot:            # 2.0 box: the selected blocks plus 2 blocks of headroom
    shape: box
    world: world
    min: {x: 10, y: 63, z: 10}
    max: {x: 20, y: 63, z: 20}
    x: 15.5              # centre and radius are written too, so a 1.x install reads a sphere
    y: 64.0
    z: 15.5
    radius: 7.8
    shards: 3            # optional: per payout
    interval-seconds: 30 # optional
    permission: shards.zone.vip   # optional
    display-name: "<gradient:#ffd166:#ff78b8>Sweet Spot</gradient>"   # optional, MiniMessage
```

A player is in a zone when their feet are inside it. Where zones overlap, the first by name the player may use wins.

## shop.yml

Categories hold an optional non-purchasable `icon` and purchasable `items` (`slot`, `price`, `material`, `amount`,
`name`, `lore`, `enchantments`, `confirmation`, `commands` with `%player%`, `%uuid%`, `%item%`, `%price%`).

### Daily Deals

```yaml
deals:
  enabled: true
  rotation-hours: 24       # all deals change together, at multiples of this many hours (UTC)
  count: 3                 # how many offers are shown (at most one per slot)
  slots: [38, 40, 42]
  limit-per-player: 1      # purchases per offer, per player, per rotation
  pool:                    # offers to pick from; same item format as above
    golden-glaze: {price: 40, material: GOLDEN_APPLE, amount: 2, name: "<gold>Golden Glaze</gold>"}
```

The offers are picked from the pool with a seed made of the rotation number and `server-id`: every player sees the
same deals, restarts do not reshuffle them, and the next rotation brings new ones. The lore shows when they rotate
and how many the player may still buy. The limit is checked in the same database transaction as the payment.

## gui.yml

`confirm` / `cancel` style the purchase confirmation screen; `previous`, `next` (bottom row, slots 45–53) and
`filler` style the `/shards top` screen.

## messages.yml

MiniMessage. `prefix` goes before chat messages. Placeholders such as `<player>` are escaped (players cannot inject
formatting); `<zone>` and `<reason>` come from staff and are rendered. A missing key shows a red "Missing message"
line. 2.0 adds the boss bar, session summary, wand, booster, history, deals and board texts.

## kills.yml

`player-kill` (shards, cooldown) and `mob-kills` per entity type (`default` for the rest). Mobs from spawners pay
nothing. Rank multipliers and (with `boosters.applies-to-kills`) boosters apply.

## database.yml

`SQLITE` (default, `donutshards.db`) or `MARIADB` (also MySQL) with host, port, database, user, password and pool
size. Several servers pointing at the same MariaDB database share balances.
