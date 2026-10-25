# Folia Compatibility

DonutShards declares `folia-supported: true` and routes work through global, region, entity, and async schedulers. Player messages, boss bars, inventory delivery, sounds and the per-second AFK check use the player entity scheduler. Leaderboard boards and zone particles use the region scheduler of their location. SQL and file work use the async scheduler. Console/global coordination uses the global scheduler.

The project intentionally contains no `BukkitScheduler` or `BukkitRunnable` use. Integrations must follow the same ownership rules and must not access players or worlds in completion callbacks without scheduling them first.
