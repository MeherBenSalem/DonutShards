# Permissions

## Players (default: everyone)

`shards.use`, `shards.balance`, `shards.pay`, `shards.top`, `shards.history`, `shards.shop`, `shards.afk`,
`shards.convert`. `shards.balance.others` (default op) shows other players' balances.

## Staff (default: op)

`shards.admin` grants `shards.admin.give`, `.take`, `.set`, `.reload`, `.zone`, `.booster`, `.board` and `.info`.
Each can also be granted alone. `shards.admin` itself is needed to run `/shardmanager` at all.

## Rewards

* **Rank multipliers**: `shards.multiplier.<x>`, for example `shards.multiplier.1.5` or `shards.multiplier.2`. The
  highest one a player has multiplies their AFK and kill rewards. Grant them with your permissions plugin (or, without
  one, in the server's `permissions.yml`).
* **Zone permissions**: a zone with a `permission` (`/shardmanager zone set <zone> permission <node>`) only pays
  players who have that node. Others are told once when they walk in.

## Bypasses

* `shards.bypass.cooldown` (default op): no cooldown between player-kill rewards.
* `shards.bypass.limit` (default false): no Daily Deals limit.

Removed in 2.0 because they never did anything: `shards.admin.reset`, `.rollback`, `.migrate`, `.gui`, `.debug`,
`.shop`.
