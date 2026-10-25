#!/usr/bin/env bash
# Runs shard_checks.py (server, client and restart checks) on a fresh server for each build:
#   tools/qa/run_matrix.sh [platform:minecraft ...]   (default: Paper and Folia on every supported version)
# Uses the release jar in releases/ (or QA_JAR). Results go to $QA_HOME/results/<platform>-<minecraft>.txt with the
# server log, the client log and screenshots; a summary line per build is printed.
set -u
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
QA="$ROOT/tools/qa"
QA_HOME=${QA_HOME:-/tmp/donutshards-qa}
export QA_HOME DISPLAY=${DISPLAY:-:1}
BUILDS=${*:-"paper:1.20.1 folia:1.20.1 paper:1.21.1 folia:1.21.4 paper:26.1.2 folia:26.1.2 paper:26.2 folia:26.2 paper:26.3 folia:26.3"}
mkdir -p "$QA_HOME/results"

wait_for() {  # file, pattern, seconds
  local deadline=$((SECONDS + $3))
  while [ $SECONDS -lt $deadline ]; do
    grep -qE "$2" "$1" 2>/dev/null && return 0
    sleep 3
  done
  return 1
}

for build in $BUILDS; do
  platform=${build%%:*}; mc=${build##*:}
  name="$platform-$mc"; out="$QA_HOME/results/$name.txt"
  "$QA/devrun.sh" stop-client; "$QA/devrun.sh" stop-server
  jar_args=(); [ -n "${QA_JAR:-}" ] && jar_args=(--jar "$QA_JAR")
  "$QA/devrun.sh" prepare-server "$platform" "$mc" --legacy "${jar_args[@]}" || { echo "$name: PREPARE FAILED" | tee "$out"; continue; }
  rm -f "$QA_HOME/server.log" "$QA_HOME/client-$mc.log"
  "$QA/devrun.sh" start-server "$platform" "$mc"
  if ! wait_for "$QA_HOME/server.log" 'Done \(|Failed to start|Exception in thread' 900 || ! grep -q 'Done (' "$QA_HOME/server.log"; then
    echo "$name: SERVER DID NOT START" | tee "$out"; cp "$QA_HOME/server.log" "$QA_HOME/results/$name-server.log"; continue
  fi
  wait_for "$QA_HOME/server.log" 'DonutShards is ready|DonutShards could not' 120
  python3 "$QA/rcon.py" "op Builder" > /dev/null
  "$QA/devrun.sh" start-client "$mc"
  client=(--client --chat-log "$QA_HOME/client-$mc.log" --shots "$QA_HOME/results/shots/$name")
  if ! wait_for "$QA_HOME/server.log" 'Builder joined the game' 600; then
    echo "client did not join" > "$out"; client=()
  else
    sleep 8
  fi
  python3 "$QA/shard_checks.py" --server-dir "$QA_HOME/servers/$name" --log "$QA_HOME/server.log" "${client[@]}" \
    --platform "$platform" --minecraft "$mc" --restart-log "$QA_HOME/results/$name-server-before-restart.log" >> "$out" 2>&1
  "$QA/devrun.sh" stop-client
  cp "$QA_HOME/server.log" "$QA_HOME/results/$name-server.log"
  cp "$QA_HOME/client-$mc.log" "$QA_HOME/results/$name-client.log" 2>/dev/null
  echo "$name ($(cat "$QA_HOME/servers/$name/BUILD")): $(tail -n 1 "$out")"
done
"$QA/devrun.sh" stop-all
