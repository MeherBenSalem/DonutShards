# Contributing to DonutShards

Thank you for helping. Open an issue before large changes, keep pull requests focused, match the surrounding code and
add tests when behaviour changes.

## Layout

- `src/main/java/com/nightbeam/donutshards/`
  - `core/`: Minecraft-free rules (payout maths, durations, deal rotation, AFK timers, boxes, paging), unit-tested.
  - `zone/`, `reward/`, `booster/`, `shop/`, `board/`, `gui/`, `command/`, `transaction/`, `database/`: the plugin.
  - `scheduler/`: Paper/Folia scheduling. Never use `BukkitScheduler`/`BukkitRunnable` (the build refuses them).
- `src/main/resources/`: default YAML (merged into servers' files on start; never rename a key without a migration).
- `tools/qa/`: in-game QA. `marketing/`: listing assets. `docs/`: design and QA records. `patchnotes/`: per release.

## Building and unit tests

Java 25 toolchain; the jar is Java 17 bytecode compiled against the Paper 1.20.1 API.

```bash
./gradlew build           # unit tests + jar
./gradlew compileMatrix   # also compile against 1.21.1, 26.1.2, 26.2, 26.3
```

## Testing in game

`tools/qa` runs real servers and a real client; needs Java 21 and 25 (`/usr/lib/jvm/java-{21,25}-openjdk-amd64`),
Xvfb, xdotool, ImageMagick and Python 3. Downloads (server jars, the vanilla client, its libraries and assets) go to
`$QA_HOME` (default `/tmp/donutshards-qa`).

```bash
./gradlew release                                  # the jar under test, in releases/
tools/qa/run_matrix.sh                             # every Paper and Folia version
tools/qa/run_matrix.sh paper:26.3 folia:1.20.1     # or some of them
```

For each server it starts from a 1.5.2 data folder (to prove upgrades), runs `shard_checks.py` over RCON and with real
keyboard and mouse input in the client (zones, wand, boss bars, shop clicks, kills, chat commands), restarts the
server to check persistence, and scans the log. Results, logs and screenshots land in `$QA_HOME/results/`. Record a
release's results in `docs/QA-<version>.md`.

Driving things by hand:

```bash
tools/qa/devrun.sh prepare-server paper 26.3 --legacy
tools/qa/devrun.sh start-server paper 26.3
python3 tools/qa/rcon.py "op Builder" "shardmanager info Builder"
DISPLAY=:1 tools/qa/devrun.sh start-client 26.3
tools/qa/devrun.sh stop-all
```

Stop servers and clients with `devrun.sh stop-*` (by PID), never with pattern-based kills.

## Releasing

1. Bump `version` in `build.gradle.kts`; write `patchnotes/<version>.md` and copy it to `PATCH_NOTES.md`.
2. `./gradlew release`, run the QA matrix, write `docs/QA-<version>.md`.
3. Commit `releases/` (jar + `SHA256SUMS.txt`). The owner publishes with the Publish workflow (tag or manual run).

## Code of conduct and licence

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). Contributions are licensed under the
[Apache License 2.0](LICENSE).
