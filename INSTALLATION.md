# Installation

1. Run Paper, Folia or Purpur on Minecraft 1.20.1 to 26.3 with the Java version that server needs (17+ for 1.20.x,
   21+ for 1.20.5–1.21.x, 25+ for 26.x).
2. Put `donutshards-paper-1.20.1-26.3-<version>.jar` in `plugins/` and start the server. The console reports the
   platform and "DonutShards is ready".
3. Make a zone: stand in game, `/shardmanager zone wand`, click two corners, `/shardmanager zone create spawn`.
4. Optional: Vault + an economy plugin for `/shards convert`, PlaceholderAPI for placeholders.
5. To use MariaDB/MySQL, stop the server, fill in `database.yml` (a dedicated least-privilege user, never root) and
   start it again.

Upgrading from 1.x: replace the jar. Back up `plugins/DonutShards/` first, as before any upgrade.
