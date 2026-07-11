# Permissions

Player nodes are `shards.use`, `shards.balance`, `shards.pay`, `shards.top`, `shards.history`, `shards.shop`, and `shards.afk`. Other-player balance access uses `shards.balance.others`; zone access uses `shards.afk.zone.<zone>`.

Administrative nodes are children of `shards.admin`: `give`, `take`, `set`, `reset`, `reload`, `rollback`, `zone`, `shop`, `migrate`, `gui`, and `debug`. Bypasses use `shards.bypass.cooldown` and `shards.bypass.limit`. Reward multipliers use `shards.multiplier.<value>`.
