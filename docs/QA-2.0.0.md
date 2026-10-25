# QA record: 2.0.0

`tools/qa/run_matrix.sh` (see [CONTRIBUTING](../CONTRIBUTING.md#testing-in-game)) on ten real servers: Paper and Folia
for every supported Minecraft line, each with a real Minecraft client (the vanilla game, 1920×1080 under Xvfb) joined
and driven with real keyboard and mouse input (xdotool), and the server driven over RCON. `tools/qa/shard_checks.py`
does the checks. Every server ran the **release jar itself**,
`releases/donutshards-paper-1.20.1-26.3-2.0.0.jar` (SHA-256 `8c5b9577…0521a973`), which `./gradlew release`
rebuilds byte for byte from the sources of commit "DonutShards 2.0: Sweet Spots, boosters, Daily Deals, leaderboard
boards". The 64 unit tests and `./gradlew compileMatrix` (sources compiled against the 1.21.1, 26.1.2, 26.2 and 26.3
APIs) passed in the same build.

Every server started from a **1.5.2 data folder** (`tools/qa/legacy_data.py`: a 1.5.2 config, a custom message
prefix, a 1.x sphere zone, a custom shop and a SQLite ledger where Builder holds 1234 shards) and a `permissions.yml`
that gives ops `shards.multiplier.1.5`, so the upgrade and the rank multiplier are tested on every build.

| Server (latest build when tested) | Client | Java | Checks | Failures |
|-----------------------------------|--------|------|--------|----------|
| Paper 1.20.1 (196) | 1.20.1 | 21 | 55 | 0 |
| Folia 1.20.1 (17) | 1.20.1 | 21 | 54 + 1 by screenshot* | 0 |
| Paper 1.21.1 (133) | 1.21.1 | 21 | 55 | 0 |
| Folia 1.21.4 (6)** | 1.21.4 | 21 | 55 | 0 |
| Paper 26.1.2 (74) | 26.1.2 | 25 | 55 | 0 |
| Folia 26.1.2 (8) | 26.1.2 | 25 | 55 | 0 |
| Paper 26.2 (134) | 26.2 | 25 | 55 | 0 |
| Folia 26.2 (7) | 26.2 | 25 | 55 | 0 |
| Paper 26.3 (222) | 26.3 | 25 | 55 | 0 |
| Folia 26.3 (5) | 26.3 | 25 | 55 | 0 |

\* Folia 1.20.1 has no command that counts a player's items (no `/data`, no `execute if items`, and Folia's `/clear`
reports players, not items), so "the deal item is delivered" was checked on the inventory screenshot the script takes
there (32 cookies in the hotbar).
\*\* Folia has no 1.21.1 build; 1.21.4 is its oldest 1.21 release.

Extra server-only runs (`shard_checks.py` without the client: upgrade, zones, booster, board, restart, log):

| Server | Java | Checks | Failures |
|--------|------|--------|----------|
| Paper 1.20.1 (196) | **17** (the jar's minimum) | 26 | 0 |
| Paper 1.21.11 (latest 1.21) | 21 | 26 | 0 |

A plugin has no obfuscated "production" build of its own: Paper and Folia load the same jar these servers loaded, and
DonutShards uses only the Bukkit/Paper API (no server internals), so these runs are the production smoke test.

## The checks (55 per build)

| Group | Checks | What |
|-------|--------|------|
| upgrade | 6 | The 1.5.2 balance (1234) and sphere zone load; the custom prefix stays and new message keys are merged; new config keys are merged and old values kept; the 1.5.2 shop keeps its categories and gets Daily Deals switched off; the unused 1.x `rewards.yml` is left untouched |
| shop / zone | 4 | A fresh `shop.yml` has deals on; a box zone created from the console; per-zone shards, interval and display name saved; invalid zone names refused |
| booster | 4 | Started from the console; info shows multiplier, time and reason; a multiplier above `boosters.max-multiplier` is refused; `/shardmanager info` shows it |
| board | 3 | Created and listed; exactly one text display spawned |
| client zone | 6 | Walking in shows the zone's display name; `info` shows the zone and the 1.5 rank multiplier; payouts are 2 × 1.5 × 2 = 6 every 5 seconds (zone shards × rank × booster, zone interval); the session total counts them; walking out (holding W) prints the session summary; out of the zone nothing is paid |
| client home | 3 | `/afk` typed in chat enables home AFK; mode home; moving (W) cancels it |
| client teleport | 2 | `/afk zone` teleports into a zone |
| client wand | 5 | `/shardmanager zone wand` gives the wand; a left click on a block sets corner 1 and the block is not broken (creative); a right click sets corner 2 (5×1×4); `/shardmanager zone create` makes the box from the selection |
| client permission | 3 | A zone with a permission tells a player without it (deopped) and pays nothing; once allowed the zone works |
| client shop | 4 | `/shardshop`, a mouse click on a Daily Deal charges its price; the item arrives; a second click is refused (`limit_reached`) and does not charge |
| client kill | 1 | A husk killed with a click pays 1 × 1.5 × 2 = 3 (kills.yml default, rank, booster applies to kills) |
| client commands | 4 | `/shards history` lists the ledger (with the kill); `/shards booster`; a payment ≥ the confirmation threshold asks for `confirm`; amounts above `transfer.maximum` are refused |
| restart | 4 | After a server restart the booster is still running, the box zone and its settings are kept, the board is kept, and exactly one board display exists (no leftovers) |
| log | 6 | Before and after the restart: no errors from DonutShards, no errors at all, "DonutShards is ready" |

Every build also saves screenshots (boss bars in a zone, home AFK, the shop, the sold-out deal, the inventory after
the deal, the `/shards top` menu and the board); those of Paper 26.3, Paper 1.20.1, Folia 1.20.1 and Folia 26.3 were
reviewed by eye, and they are the source of several fixes below.

## Found and fixed by this QA

* **Zone display names showed their MiniMessage tags** (`<gold\>Sugar Orbit`): placeholder values were all escaped.
  Values written by staff (`<zone>`, `<reason>`) are now rendered; player-provided values stay escaped.
* **Escaped values showed a stray backslash** (`\>`) in chat, a 1.x bug: the escaping now uses MiniMessage's own.
* **RCON and the console printed nothing for `/shardmanager info` and `history`**: the answers arrived after RCON had
  stopped listening. Console and RCON now get them inline (waiting up to 5 s for the database).
* **`/shardmanager board list`** now reports how many displays a board has, so leftovers would be visible.
* **The deal lore read "You can buy 1/1 more"**; it now reads "Left for you this rotation: 1/1".
* The QA run also caught its own setup mistakes, fixed in the scripts, not the plugin: the stop command killed a
  wrapper shell instead of the server; a zombie used for the kill check burned in daylight; `spawn-monsters=false`
  removed summoned monsters on 1.20.1; the first click after a teleport is used by the client to grab the mouse;
  Folia's console cannot run world commands (those checks are now run by the player); Folia 1.20.1's RCON fails
  every command (the scripts fall back to the server console); Folia's `/clear` reports players, not items.

## Tooling checks

* `node .github/scripts/detect-jar-meta.test.js`: 9 cases pass; the release jar resolves to loaders paper, folia,
  purpur and the 23 game versions of `release/supported-minecraft.json`.
* `.github/workflows/publish.yml` and `build.yml` parse; the publish workflow was **not run**.
