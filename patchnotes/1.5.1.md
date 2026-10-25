# DonutShards 1.5.1 — Patch Notes

**Release date:** August 2026

## Summary

Hotfix for Folia-safe `/afk zone` teleport after 1.5.0, plus small AFK join messaging and leaderboard paging fixes. Full 1.5.0 feature set is unchanged.

## Fixes

- `/afk zone` uses `teleportAsync`; sync + success message run on the destination entity thread
- Bare `/afk join` no longer doubles `afk-zone-enter` with `afk-joined`
- `/afk leave` while in home mode shows `afk-home-off`
- Leaderboard GUI page-size capped at 45; Next button respects the top-100 SQL cap
- COMMANDS.md / CONFIGURATION.md / bundled patch notes updated for 1.5.x behavior

## Upgrade notes

1. Replace the 1.5.0 jar with 1.5.1 and restart (or reload after swap on a test server).
2. No new config keys required beyond 1.5.0.

## Compatibility

- **Platforms:** Paper, Folia, Purpur, Spigot, Bukkit
- **Minecraft:** 1.20.1 – 26.2
- **folia-supported:** true
