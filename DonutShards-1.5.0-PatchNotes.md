# DonutShards 1.5.0 — Patch Notes

**Release date:** August 2026

## Summary

DonutShards 1.5.0 fixes stay-in-place Home AFK not cancelling when you walk, and ships the AFK/leaderboard QOL from ticket-0020. The release jar targets Paper, Folia, Purpur, Spigot, and Bukkit on Minecraft 1.20.1 through 26.2.

## Bug fixes

- **Home AFK cancels on move** — block-coordinate movement now disables home mode, sends `afk-home-cancelled-move`, then syncs zone presence so walking into a zone can auto-join.
- Folia-safe messaging for the cancel notice via the entity scheduler.

## AFK

| Change | Detail |
|---|---|
| Zone enter/leave chat | Auto presence sends `afk-zone-enter` / `afk-zone-leave`; command join/leave still use `afk-joined` / `afk-left` |
| Bare `/afk` | Same as `/afk home` (status: `/afk info`) |
| `/afk zone` | Teleports to closest zone center (or `afk.zone-teleport.preferred-zone`) |
| Reward titles | Title `reward-title` + subtitle `next-reward` with `<amount>`, `<time>`, `<next_reward>` |

## Economy / commands

| Change | Detail |
|---|---|
| `/shards top` | Opens leaderboard GUI (`leaderboard.page-size`); `/shards top chat` for chat list |
| `/shards convert` | Now `/shards convert <shards\|money> <amount>` (currency then amount) |
| Tab-complete | Admin suggests only `give\|take\|set\|reload\|zone` |
| Aliases | `/dshards` and `/donutshards` move to `/shardmanager`; players keep `/shards` + `/shard` |

## Upgrade notes

1. Replace the jar and restart (or `/shardmanager reload` after swapping on a test server).
2. YamlKeyMerger adds new message/config keys without overwriting your values.
3. Update any scripts that used `/shards convert <amount> <currency>` to the new currency-first order.
4. Players who used `/dshards` as a balance command should switch to `/shards` or `/shard`.

## Build

- Artifact: `DonutShards-1.5.0-paper-folia-mc1.20.1-26.2.jar` (shadow jar with relocated bStats).
- Requires Java 21.

## Compatibility

- **Platforms:** Paper, Folia, Purpur, Spigot, Bukkit
- **Minecraft:** 1.20.1 – 26.2 (see `release/supported-minecraft.json`)
- **folia-supported:** true
