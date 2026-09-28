# Patch Notes — 1.5.2

**Minecraft 26.3:** Compiles against Paper API `26.3.build.49-alpha`. Boot-tested on Paper 26.3 ALPHA build 133. `api-version` stays `1.20` so 1.20.1–26.2 still load. Folia-supported remains true (no Folia 26.3; Folia 26.2 is the current Folia target).

**API:** Sounds, zone particles, shop enchantments, and kill-reward mob keys use Registry lookups with enum/constant fallbacks instead of deprecated-for-removal `Sound.valueOf` / `Enchantment.getByName`. Particle lookup does not assume `Keyed` (1.20.1 Particle is not Keyed).

## 1.5.1

**Folia:** `/afk zone` now uses Paper `teleportAsync` and finishes sync/chat on the destination region thread.

**AFK:** Bare `/afk join` only sends the command joined message (no duplicate zone-enter). Leaving home mode with `/afk leave` shows the home-off message.

**Leaderboard:** GUI page size is capped at 45 slots; Next respects the top-100 query cap.

## 1.5.0

**Home AFK:** Walking cancels stay-in-place AFK, then zone auto-join can apply.

**AFK QOL:** Enter/leave chat, bare `/afk` = home, reward titles with `<amount>`/`<time>`/`<next_reward>`, `/afk zone` teleport (closest or `preferred-zone`).

**Commands:** `/shards top` GUI (`/shards top chat` for chat), `/shards convert <shards|money> <amount>`, `/dshards` + `/donutshards` are admin aliases.

## 1.4.0

**YAML merge:** On upgrade, missing keys from jar defaults are added automatically — your custom `prefix` and other edits stay intact. `/shardmanager reload` runs the same merge.

**Shop:** Per-item `confirmation:` opens the confirm/cancel GUI from `gui.yml`. Per-item `commands:` run as console after purchase. `/shards confirmation on|off` toggles confirmations for your account (default on).

**Placeholders:** `%donutshard_balance%`, `%donutshard_top_1`–`10`, `%donutshard_top_bal_1`–`10` when PlaceholderAPI is installed.

**Ops:** `/shards top` shows the top 10 balances. bStats (id 33616) and Modrinth update checks are on by default — disable with `bstats: false` and `update-check: false` in `config.yml`.
