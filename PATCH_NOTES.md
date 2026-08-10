# Patch Notes — 1.1.0

AFK zones are now fully functional. Admins create spherical regions with `/shardmanager zone create <name>` at their location; zones persist in `zones.yml`. Players use `/afk join` (auto-detects when standing inside a zone), `/afk leave`, `/afk list`, and `/afk info`. Entering or leaving a region is tracked automatically for reward eligibility. `/shardmanager reload` refreshes zone and config data without a restart.

## 1.0.0

This first release establishes the Java 21 Paper/Folia-compatible architecture, persistent integer ledger, idempotent transaction keys, original Cosmic Confection Exchange, grouped reward loop, and public API. The validated build range is 1.20.1–26.1.2; 26.2 is intentionally not claimed.
