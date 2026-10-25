#!/usr/bin/env bash
# Real Paper/Folia servers and a real Minecraft client for QA, all under $QA_HOME (default /tmp/donutshards-qa).
#   tools/qa/devrun.sh fetch-server <paper|folia> <minecraft>      download the latest build of that server
#   tools/qa/devrun.sh prepare-server <paper|folia> <minecraft> [--legacy] [--jar <plugin.jar>] [--level-type <type>]
#       fresh server folder: eula, server.properties (RCON 25575 / password qa, offline mode, flat world), the plugin jar
#       (default: the release jar in releases/, else build/libs). --legacy seeds plugins/DonutShards with a 1.5.2 data
#       folder (1.5.2 config, a sphere zone, a SQLite ledger holding 1234 shards for Builder) to prove the upgrade.
#       permissions.yml gives ops shards.multiplier.1.5.
#   tools/qa/devrun.sh start-server <paper|folia> <minecraft>   (QA_SERVER_JAVA overrides the Java binary)
#   tools/qa/devrun.sh start-client <minecraft>                    (Xvfb on $DISPLAY, default :1, 1920x1080)
#   tools/qa/devrun.sh stop-client | stop-server | stop-all          stops by PID file, never by pattern
set -u
QA_HOME=${QA_HOME:-/tmp/donutshards-qa}
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
mkdir -p "$QA_HOME"

java_for() {  # minecraft version -> java binary
  case "$1" in
    1.20.*|1.21.*) echo /usr/lib/jvm/java-21-openjdk-amd64/bin/java ;;
    *) echo /usr/lib/jvm/java-25-openjdk-amd64/bin/java ;;
  esac
}

stop_pid() {  # pid file
  [ -f "$1" ] || return 0
  local pid; pid=$(cat "$1")
  if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null
    for _ in $(seq 1 60); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
    kill -9 "$pid" 2>/dev/null
  fi
  rm -f "$1"
}

case "${1:-}" in
  fetch-server)
    dir="$QA_HOME/servers/$2-$3"; mkdir -p "$dir"
    if [ ! -f "$dir/server.jar" ]; then
      url=$(curl -fsS "https://fill.papermc.io/v3/projects/$2/versions/$3/builds/latest" \
        | python3 -c 'import json,sys; print(json.load(sys.stdin)["downloads"]["server:default"]["url"])') || exit 1
      curl -fsS -o "$dir/server.jar.part" "$url" && mv "$dir/server.jar.part" "$dir/server.jar"
      basename "$url" > "$dir/BUILD"
    fi
    cat "$dir/BUILD"
    ;;
  prepare-server)
    platform=$2; mc=$3; shift 3
    legacy=0; jar=""; level_type="minecraft\\:flat"
    while [ $# -gt 0 ]; do
      case "$1" in
        --legacy) legacy=1 ;;
        --jar) jar=$2; shift ;;
        --level-type) level_type=$2; shift ;;
      esac
      shift
    done
    dir="$QA_HOME/servers/$platform-$mc"
    [ -f "$dir/server.jar" ] || "$0" fetch-server "$platform" "$mc" >/dev/null || exit 1
    rm -rf "${dir:?}/world" "$dir/world_nether" "$dir/world_the_end" "$dir/plugins" "$dir/logs"
    mkdir -p "$dir/plugins"
    if [ -z "$jar" ]; then
      jar=$(ls "$ROOT"/releases/donutshards-paper-*.jar 2>/dev/null | sort -V | tail -1)
      [ -n "$jar" ] || jar=$(ls "$ROOT"/build/libs/DonutShards-*.jar | grep -v sources | sort -V | tail -1)
    fi
    cp "$jar" "$dir/plugins/"
    echo "$jar" > "$dir/PLUGIN"
    echo 'eula=true' > "$dir/eula.txt"
    cat > "$dir/server.properties" <<PROPS
enable-rcon=true
rcon.port=25575
rcon.password=qa
broadcast-rcon-to-ops=false
online-mode=false
enforce-secure-profile=false
level-name=world
level-type=$level_type
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains","lakes":false,"features":false,"structure_overrides":[]}
level-seed=7214950366214
spawn-protection=0
view-distance=8
simulation-distance=6
difficulty=peaceful
spawn-monsters=true
spawn-animals=false
gamemode=creative
white-list=false
motd=DonutShards QA
PROPS
    # Ops get a rank multiplier, so every reward check also proves shards.multiplier.<x>.
    printf 'shards.multiplier.1.5:\n  default: op\n' > "$dir/permissions.yml"
    # Paper's bStats and update checks stay quiet in a sandbox.
    mkdir -p "$dir/plugins/bStats"; printf 'enabled: false\n' > "$dir/plugins/bStats/config.yml"
    if [ "$legacy" = 1 ]; then
      python3 "$ROOT/tools/qa/legacy_data.py" "$dir/plugins/DonutShards" || exit 1
    fi
    ;;
  start-server)
    dir="$QA_HOME/servers/$2-$3"
    java=${QA_SERVER_JAVA:-}; [ -n "$java" ] || java=$(java_for "$3")
    cd "$dir" || exit 1
    # The console reads a FIFO (commands for servers whose RCON cannot run commands, such as Folia 1.20.1); a sleeping
    # writer keeps it open. Commands: echo "list" > $QA_HOME/console.fifo
    rm -f "$QA_HOME/console.fifo"; mkfifo "$QA_HOME/console.fifo"
    nohup "$java" -Xmx3G -DPaper.IgnoreJavaVersion=true -jar server.jar --nogui < "$QA_HOME/console.fifo" > "$QA_HOME/server.log" 2>&1 &
    echo $! > "$QA_HOME/server.pid"
    nohup sleep 2147483647 > "$QA_HOME/console.fifo" 2>/dev/null &
    echo $! > "$QA_HOME/console.pid"
    ;;
  start-client)
    export DISPLAY=${DISPLAY:-:1}
    if ! xdpyinfo >/dev/null 2>&1; then
      nohup Xvfb "$DISPLAY" -screen 0 1920x1080x24 -nolisten tcp > "$QA_HOME/xvfb.log" 2>&1 &
      echo $! > "$QA_HOME/xvfb.pid"
      sleep 2
    fi
    python3 "$ROOT/tools/qa/mcclient.py" launch "$2" >/dev/null
    ;;
  stop-client) stop_pid "$QA_HOME/client.pid" ;;
  stop-server) stop_pid "$QA_HOME/server.pid"; stop_pid "$QA_HOME/console.pid" ;;
  stop-all) stop_pid "$QA_HOME/client.pid"; stop_pid "$QA_HOME/server.pid"; stop_pid "$QA_HOME/console.pid"; stop_pid "$QA_HOME/xvfb.pid" ;;
  *) sed -n '2,13p' "$0"; exit 1 ;;
esac
