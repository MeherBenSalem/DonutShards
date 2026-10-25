#!/usr/bin/env python3
"""In-game checks for DonutShards against a running QA server (see devrun.sh), optionally with the QA client.

The server is prepared with `devrun.sh prepare-server ... --legacy`, so it starts from a 1.5.2 data folder (Builder
holds 1234 shards, a sphere zone, a custom prefix and shop), and with a permissions.yml that gives ops
`shards.multiplier.1.5`, so every reward below is base x 1.5 (x 2 while the booster runs).

Server checks (RCON):
  * upgrade: the 1.5.2 balance, sphere zone, prefix and shop survive; new config keys are merged; deals arrive off
    in the old shop; the unused 1.x files are left alone;
  * zones: a box zone from the console, per-zone shards/interval/display name;
  * booster: start, info, and it survives a restart;
  * board: a leaderboard text display is spawned (and again after a restart);
  * log: no errors from the plugin, and none at all.
Client checks (--client, real keyboard and mouse):
  * zone: walking in shows the zone name and boss bars; payouts are base x rank x booster on the zone's interval;
    walking out prints the session summary;
  * home: /afk pays at home and moving cancels it;
  * teleport: /afk zone;
  * wand: left/right clicks set the corners without breaking the block; /shardmanager zone create makes the box;
  * permission: a zone with a permission ignores a player without it and tells them, and works once allowed;
  * shop: a Daily Deal bought with a click, charged once, and refused past the per-player limit;
  * kill reward with booster and rank multiplier; /shards history, booster, top GUI; pay limits and confirmation.

Console commands go over RCON, or through the console FIFO on servers whose RCON cannot run commands (Folia 1.20.1).
On Folia the console cannot touch the world, so world checks (blocks, items, summons, forceload) are run by the player.

  python3 tools/qa/shard_checks.py --server-dir DIR --log server.log [--client --chat-log client.log --shots DIR]
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from rcon import wait_for  # noqa: E402  (RCON, or the console FIFO where RCON cannot run commands)

ROOT = Path(__file__).resolve().parents[2]
QA = ROOT / 'tools' / 'qa'
PLAYER = 'Builder'
G = -61            # grass layer of the flat world; players stand at G + 1
FAILURES: list[str] = []
COUNT = [0]
DEAL_POOL = {  # id: (price, material, amount) from the default shop.yml
    'golden-glaze': (40, 'golden_apple', 2), 'comet-crate': (60, 'experience_bottle', 16),
    'nebula-rockets': (30, 'firework_rocket', 32), 'sprinkle-stack': (25, 'cookie', 32), 'meteor-ore': (120, 'diamond', 3),
}


def check(name, ok, detail=''):
    COUNT[0] += 1
    print(('PASS ' if ok else 'FAIL ') + name + (f'  [{str(detail)[:500]}]' if detail and not ok else ''), flush=True)
    if not ok:
        FAILURES.append(name)
    return ok


class Server:
    def __init__(self, port=25575):
        self.rcon = wait_for(port)

    def cmd(self, command):
        return self.rcon.command(command)

    def info(self, player=PLAYER):
        return self.cmd(f'shardmanager info {player}')

    def balance(self, player=PLAYER):
        m = re.search(r'balance=(-?\d+)', self.info(player))
        return int(m.group(1)) if m else None



# Server checks -------------------------------------------------------------------------------------------------------

def check_upgrade(s, server_dir):
    check('upgrade: 1.5.2 balance kept (1234)', s.balance() == 1234, s.info())
    zones = s.cmd('shardmanager zone list')
    check('upgrade: 1.5.2 sphere zone loads', 'legacy-lobby @ world sphere' in zones, zones)
    data = server_dir / 'plugins' / 'DonutShards'
    messages = (data / 'messages.yml').read_text()
    check('upgrade: custom prefix kept, new message keys merged', 'LegacyPrefix' in messages and 'afk-bossbar:' in messages)
    config = (data / 'config.yml').read_text()
    check('upgrade: new config keys merged, old values kept', 'boss-bar:' in config and 'boosters:' in config
          and re.search(r'bstats: false', config) is not None)
    shop = (data / 'shop.yml').read_text()
    check('upgrade: old shop keeps its categories, deals arrive switched off',
          'legacy-bread' in shop and 'daily-deals' not in shop and re.search(r'deals:\n(?:  .*\n)*?  enabled: false', shop) is not None, shop[:600])
    check('upgrade: unused 1.x files left untouched', (data / 'rewards.yml').read_text() == 'schema-version: 1\nrewards: {}\n')


def fresh_shop(s, server_dir):
    """The 1.5.2 shop keeps deals off; the deal checks use the 2.0 default shop."""
    (server_dir / 'plugins' / 'DonutShards' / 'shop.yml').unlink()
    s.cmd('shardmanager reload')
    shop = (server_dir / 'plugins' / 'DonutShards' / 'shop.yml').read_text()
    check('shop: a new shop.yml has Daily Deals switched on', re.search(r'deals:\n(?:.*\n)*?  enabled: true', shop) is not None)


def setup_world(s):
    # Console commands only touch global state: on Folia the console cannot run world commands (blocks, entities,
    # chunks); those are run by the player in the client checks, on the player's own region thread.
    for c in ['op Builder', 'gamerule doDaylightCycle false', 'gamerule doWeatherCycle false', 'time set noon', 'weather clear',
              'difficulty peaceful']:
        s.cmd(c)
    if ARGS.platform != 'folia':
        s.cmd('forceload add -16 -16 16 16')  # the board's chunk; on Folia the player does this (client checks)
    s.cmd('gamerule advance_time false')  # 26.x name; harmless elsewhere


def check_zone_console(s):
    out = s.cmd(f'shardmanager zone create sugar world 100 {G} 100 104 {G} 104')
    check('zone: box created from the console', 'Created AFK zone sugar: box (100, -61, 100) to (104, -61, 104)' in out, out)
    s.cmd('shardmanager zone set sugar shards 2')
    s.cmd('shardmanager zone set sugar interval 5')
    s.cmd('shardmanager zone set sugar display-name <gradient:#ffd166:#ff78b8>Sugar Orbit</gradient>')
    info = s.cmd('shardmanager zone info sugar')
    check('zone: per-zone shards, interval and display name saved', 'shards=2' in info and 'interval=5s' in info and 'Sugar Orbit' in info, info)
    bad = s.cmd('shardmanager zone create bad.name world 0 0 0 1 1 1')
    check('zone: invalid names refused', 'letters, digits' in bad, bad)


def check_booster(s):
    out = s.cmd('shardmanager booster start 2 20m Launch party')
    check('booster: started', 'Booster x2 started for 20m 00s' in out, out)
    info = s.cmd('shardmanager booster')
    check('booster: info shows multiplier, time and reason', re.search(r'Booster x2, (19m 5\d|20m 00)s left \(Launch party\)', info) is not None, info)
    bad = s.cmd('shardmanager booster start 50 1h')
    check('booster: multiplier above the configured maximum refused', 'at most 10' in bad, bad)
    check('booster: info player-facing multiplier', 'booster=2' in s.info(), s.info())


def check_board(s):
    out = s.cmd(f'shardmanager board create top world 0.5 {G + 3} 0.5')
    check('board: created', 'Created leaderboard board top' in out, out)
    check('board: listed', 'top @ world (1, -58, 1)' in s.cmd('shardmanager board list'))


def check_restart(args):
    s = Server()
    s.cmd('shardmanager board refresh')
    time.sleep(1)
    subprocess.run([str(QA / 'devrun.sh'), 'stop-server'], check=False)
    log = Path(args.log)
    before = log.read_text(errors='replace') if log.exists() else ''
    Path(args.restart_log).write_text(before)
    log.unlink(missing_ok=True)
    subprocess.run([str(QA / 'devrun.sh'), 'start-server', args.platform, args.minecraft], check=False)
    deadline = time.time() + 600
    while time.time() < deadline and 'DonutShards is ready' not in (log.read_text(errors='replace') if log.exists() else ''):
        time.sleep(3)
    time.sleep(2)
    s = Server()
    info = s.cmd('shardmanager booster')
    check('restart: booster still running', 'Booster x2' in info and 'Launch party' in info, info)
    check('restart: box zone and settings kept', 'shards=2' in s.cmd('shardmanager zone info sugar'))
    check('restart: board kept', 'top @ world' in s.cmd('shardmanager board list'))
    s.cmd('shardmanager board refresh')
    time.sleep(4)
    boards = s.cmd('shardmanager board list')
    check('restart: exactly one board display after respawn (no leftovers)', 'displays=1' in boards, boards)
    return s


def check_log(path):
    text = Path(path).read_text(errors='replace')
    lines = text.splitlines()
    ours = [l for l in lines if re.search(r'(ERROR|SEVERE|Exception)', l) and re.search(r'(?i)donutshards|nightbeam', l)]
    check('log: no errors from DonutShards', not ours, ours[:5])
    errors = [l for l in lines if re.search(r'\b(ERROR|SEVERE)\]', l) or 'Exception' in l]
    check('log: no errors at all', not errors, errors[:5])
    check('log: plugin enabled and ready', 'DonutShards is ready' in text)


# Client checks -------------------------------------------------------------------------------------------------------

def player_item_count(c, item, platform):
    """How many of an item the player holds, asked by the player (console world commands do not work on Folia).
    `execute if items` (1.20.5+) reports the item total; before that Paper's `/clear ... 0` does. Folia's /clear
    reports the number of players instead of items and Folia 1.20.1 has neither command: None there."""
    time.sleep(1.5)
    c.chat(f'/execute if items entity @s container.* minecraft:{item}')
    text = c.wait_chat(r'Test passed|Test failed|Unknown|Incorrect|Expected', 6) or ''
    m = re.search(r'Test passed[.,] [cC]ount: (\d+)', text)
    if m:
        return int(m.group(1))
    if 'Test failed' in text:
        return 0
    if platform == 'folia':
        return None
    time.sleep(1.5)
    c.chat(f'/clear @s minecraft:{item} 0')
    text = c.wait_chat(r'Found \d+ matching|No items were found', 6) or ''
    m = re.search(r'Found (\d+) matching', text)
    return int(m.group(1)) if m else 0


def client_checks(args):
    from client import Session
    c = Session(args.chat_log)
    srv = Server()
    shots = Path(args.shots) if args.shots else None

    def shot(name):
        if shots:
            c.shot(shots / f'{name}.png')

    c.cmd('clear Builder')
    c.cmd('gamemode creative Builder')
    c.cmd('effect give Builder minecraft:night_vision 600 0 true')
    # The board's chunk stays loaded (also after the restart check), so the board can be checked without a player.
    c.chat('/forceload add -16 -16 16 16')
    time.sleep(1)
    c.cmd('shardmanager board refresh')
    time.sleep(3)
    boards = c.cmd('shardmanager board list')
    check('board: one text display spawned', 'displays=1' in boards, boards)

    # Zone: walk in by teleport (the per-second check catches it), get paid on the zone interval.
    c.mark()
    c.tp(102.5, G + 1, 102.5, 0, 20)
    joined = c.wait_chat(r'Entered AFK zone Sugar Orbit', 10)
    check('client zone: entering shows the display name', joined is not None, c.chat_since_mark())
    time.sleep(11.5)
    shot('zone-bossbars')
    info = c.cmd('shardmanager info Builder')
    check('client zone: info shows the zone', 'mode=zone zone=sugar' in info and 'multiplier=1.5' in info, info)
    history = c.cmd('shardmanager history Builder')
    pays = re.findall(r'\+(\d+) reward:afk:sugar', history)
    check('client zone: payouts are 2 x 1.5 rank x 2 booster = 6 every 5 s', len(pays) >= 2 and all(p == '6' for p in pays), history)
    m = re.search(r'session=(\d+)', info)
    check('client zone: session total counts the payouts', m is not None and int(m.group(1)) >= 12, info)

    # Walk out: summary.
    c.mark()
    c.tp(102.5, G + 1, 104.2, 0, 0)  # facing +z, one step from the edge
    c.hold_key('w', 1.5)
    summary = c.wait_chat(r'You spent .+ in Sugar Orbit and earned \d+ shards', 8)
    check('client zone: walking out prints the session summary', summary is not None, c.chat_since_mark())
    check('client zone: out of the zone, mode none', 'mode=none' in c.cmd('shardmanager info Builder'))

    # Home AFK.
    c.tp(140.5, G + 1, 140.5, 0, 0)
    time.sleep(1)
    c.chat('/afk')
    check('client home: /afk enables home AFK', c.wait_chat(r'Stay-in-place AFK enabled', 8) is not None, c.chat_since_mark())
    time.sleep(2)
    check('client home: mode home', 'mode=home' in c.cmd('shardmanager info Builder'))
    shot('home-bossbar')
    c.mark()
    c.hold_key('w', 0.8)
    check('client home: moving cancels it', c.wait_chat(r'Home AFK cancelled because you moved', 8) is not None, c.chat_since_mark())

    # /afk zone teleport to the preferred zone.
    c.cmd('shardmanager zone set sugar interval 5')
    c.chat('/afk zone')
    check('client teleport: /afk zone', c.wait_chat(r'Teleported to AFK zone', 8) is not None, c.chat_since_mark())
    time.sleep(2)
    check('client teleport: lands in a zone', re.search(r'mode=zone zone=(sugar|legacy-lobby)', c.cmd('shardmanager info Builder')) is not None)

    # Wand.
    c.cmd('clear Builder')
    c.tp(150.5, G + 1, 150.5, 0, 0)
    c.chat('/shardmanager zone wand')
    check('client wand: given', c.wait_chat(r'Wand ready', 8) is not None, c.chat_since_mark())
    c.key('1')
    c.tp(120.5, G + 1, 120.5, 0, 90)
    time.sleep(1.2)
    c.mark()
    c.click(1)
    corner1 = c.wait_chat(r'Corner 1 set to 120, -61, 120', 6)
    check('client wand: left click sets corner 1', corner1 is not None, c.chat_since_mark())
    time.sleep(0.5)
    c.chat('/execute if block 120 -61 120 minecraft:grass_block run say block-kept')
    check('client wand: the clicked block is not broken', c.wait_chat(r'block-kept', 6) is not None, c.chat_since_mark())
    c.tp(124.5, G + 1, 123.5, 0, 90)
    time.sleep(1.2)
    c.mark()
    c.click(3)
    check('client wand: right click sets corner 2', c.wait_chat(r'Corner 2 set to 124, -61, 123.*5×1×4', 6) is not None, c.chat_since_mark())
    c.chat('/shardmanager zone create wandbox')
    time.sleep(1)
    info = c.cmd('shardmanager zone info wandbox')
    check('client wand: zone created from the selection', 'box (120, -61, 120) to (124, -61, 123)' in info, info)

    # Permission-gated zone.
    c.cmd('shardmanager zone set wandbox permission shards.zone.vip')
    c.mark()
    c.cmd('deop Builder')
    c.tp(122.5, G + 1, 121.5, 0, 0)
    check('client permission: a player without it is told', c.wait_chat(r'You need permission to earn in wandbox', 8) is not None, c.chat_since_mark())
    check('client permission: and does not earn there', 'mode=none' in c.cmd('shardmanager info Builder'))
    c.cmd('op Builder')
    time.sleep(2.5)
    check('client permission: with it (op) the zone works', 'zone=wandbox' in c.cmd('shardmanager info Builder'))

    # Shop deals.
    c.tp(160.5, G + 1, 160.5, 0, 0)
    c.cmd('clear Builder')
    c.cmd('shardmanager set Builder 1000')
    c.cmd('shardmanager zone set wandbox permission reset')
    c.chat('/shardshop')
    time.sleep(2.5)
    shot('shop')
    x, y = c.chest_slot(38)
    c.mark()
    c.click(1, x, y)
    time.sleep(2.5)
    balance = srv.balance()
    bought = [(k, v) for k, v in DEAL_POOL.items() if balance == 1000 - v[0]]
    check('client shop: clicking a Daily Deal charges its price', len(bought) == 1, f'balance {balance}')
    if bought:
        item, (price, material, amount) = bought[0]
        c.key('Escape')
        time.sleep(0.8)
        c.key('e')
        time.sleep(1.5)
        shot('deal-delivered')
        c.key('Escape')
        held = player_item_count(c, material, args.platform)
        if held is None:
            print(f'SKIP client shop: the deal item is delivered ({material} x{amount}; no item-count command on this '
                  f'server, see the deal-delivered screenshot)', flush=True)
        else:
            check('client shop: the deal item is delivered', held == amount, f'{material}: {held}, expected {amount}')
        c.chat('/shardshop')
        time.sleep(2.5)
        shot('shop-sold-out')
        c.mark()
        c.click(1, x, y)
        refused = c.wait_chat(r'Purchase failed: limit_reached', 6)
        check('client shop: the per-player limit refuses a second purchase', refused is not None, c.chat_since_mark())
        check('client shop: and does not charge', srv.balance() == 1000 - price)
    c.key('Escape')
    time.sleep(0.8)

    # Kill reward with booster and rank multiplier. A husk (it does not burn at noon), kills.yml default: 1 x 1.5 x 2 = 3.
    srv.cmd('difficulty easy')
    c.tp(170.5, G + 1, 170.5, facing=(170.5, G + 2.0, 172.5))
    time.sleep(1)
    c.chat(f'/summon minecraft:husk 170.5 {G + 1} 172.5 {{NoAI:1b,Health:1.0f,PersistenceRequired:1b,Silent:1b}}')
    time.sleep(1.5)
    before = srv.balance()
    after = before
    for _ in range(3):  # the first click after a teleport can be taken by the client to grab the mouse
        c.click(1)
        time.sleep(2)
        after = srv.balance()
        if after != before:
            break
    check('client kill: a husk killed with a click pays 1 x 1.5 x 2 = 3', after - before == 3, f'{before} -> {after}')
    srv.cmd('difficulty peaceful')

    # Player commands.
    c.chat('/shards history')
    check('client history: /shards history lists the ledger', c.wait_chat(r'Shard history[\s\S]*\+3 kill:mob-kill', 8) is not None, c.chat_since_mark())
    c.chat('/shards booster')
    check('client booster: /shards booster', c.wait_chat(r'Shard booster: x2 for \d+m \d+s Launch party', 8) is not None, c.chat_since_mark())
    c.chat('/shards pay Builder 200000')
    check('client pay: large payments ask for confirm', c.wait_chat(r'Sending 200000 shards to Builder\? Run', 8) is not None, c.chat_since_mark())
    c.chat('/shards pay Builder 5000000')
    check('client pay: limits enforced', c.wait_chat(r'Payments must be between 1 and 1000000 shards', 8) is not None, c.chat_since_mark())
    c.chat('/shards top')
    time.sleep(2.5)
    shot('top-gui')
    c.key('Escape')
    time.sleep(0.5)

    # Board view.
    c.tp(0.5, G + 1, 6.5, facing=(0.5, G + 3.2, 0.5))
    time.sleep(3)
    shot('board')


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--server-dir', required=True)
    ap.add_argument('--log', required=True)
    ap.add_argument('--client', action='store_true')
    ap.add_argument('--chat-log')
    ap.add_argument('--shots')
    ap.add_argument('--platform')
    ap.add_argument('--minecraft')
    ap.add_argument('--restart-log')
    ARGS = ap.parse_args()
    server = Server()
    check_upgrade(server, Path(ARGS.server_dir))
    fresh_shop(server, Path(ARGS.server_dir))
    setup_world(server)
    check_zone_console(server)
    check_booster(server)
    check_board(server)
    if ARGS.client:
        try:
            client_checks(ARGS)
        except Exception as error:  # noqa: BLE001 - reported as a failure
            check('client: ran to the end', False, repr(error))
    if ARGS.platform and ARGS.restart_log:
        try:
            check_restart(ARGS)
        except Exception as error:  # noqa: BLE001
            check('restart: ran to the end', False, repr(error))
        check_log(ARGS.restart_log)
    check_log(ARGS.log)
    print(f'{COUNT[0]} checks, {len(FAILURES)} failures' + (': ' + ', '.join(FAILURES) if FAILURES else ''))
    sys.exit(1 if FAILURES else 0)
