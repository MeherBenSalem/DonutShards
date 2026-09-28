# DonutShards 1.5.2 — Patch Notes

**Release date:** September 2026

## Summary

Adds Minecraft 26.3 support while keeping the 1.20.1–26.2 matrix, Java 21 bytecode, and Folia declarations.

## Changes

- Compile against Paper API `26.3.build.49-alpha` using JDK 25; emit Java 21 bytecode
- Boot-tested on Paper 26.3 ALPHA build 133
- Registry lookups for sounds, AFK zone particles, shop enchantments, and kill-reward entity types, with `valueOf`/constant fallbacks for older servers
- Publish workflow uses Java 25 and uploads with the full supported game-version list (including 26.3) and Paper-family loaders

## Upgrade notes

1. Replace the 1.5.1 jar with 1.5.2 and restart.
2. Java 21 is still enough to *run* on 1.20.1–26.1 servers. Paper/Folia 26.1.2+ require Java 25 at runtime.
3. No new config keys.

## Compatibility

- **Platforms:** Paper, Folia, Purpur, Spigot, Bukkit
- **Minecraft:** 1.20.1 – 26.3
- **folia-supported:** true
- **Paper 26.3 test build:** 133 (ALPHA)
