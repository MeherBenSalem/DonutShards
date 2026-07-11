# Folia Compatibility

DonutShards declares `folia-supported: true` and routes work through global, region, entity, and async schedulers. Player messages, inventory delivery, sounds, and reward snapshots use the player entity scheduler. Location operations use the region scheduler. SQL and file work use the async scheduler. Console/global coordination uses the global scheduler.

The project intentionally contains no `BukkitScheduler` or `BukkitRunnable` use. Integrations must follow the same ownership rules and must not access players or worlds in completion callbacks without scheduling them first.
