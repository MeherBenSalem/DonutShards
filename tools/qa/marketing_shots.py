#!/usr/bin/env python3
"""Captures the listing screenshots (marketing/images/screenshots/) from a real client on a real server.

Setup (see marketing/README.md): a Paper 26.3 server prepared with a normal world and the release jar, and the client
joined:

  tools/qa/devrun.sh prepare-server paper 26.3 --level-type 'minecraft\\:normal'
  tools/qa/devrun.sh start-server paper 26.3 && python3 tools/qa/rcon.py "op Builder"
  DISPLAY=:1 tools/qa/devrun.sh start-client 26.3
  python3 tools/qa/marketing_shots.py [scene ...]

The script builds a donut-shaped AFK platform in the sky with commands (glaze, dough edge, sprinkles), makes it a
zone with the plugin's own commands, places a leaderboard board over the hole, and captures each scene. Everything
DonutShards shows (boss bars, chat, the shop, the menus, the board, the particle outline) is the plugin running.
The leaderboard names other than Builder are demo balances set with /shardmanager set.
"""
from __future__ import annotations

import math
import random
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from client import Session  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'marketing' / 'images' / 'screenshots'
Y = 140                 # platform layer; players stand at Y + 1
CHAT_LOG = '/tmp/donutshards-qa/client-26.3.log'
DEMO = [('Sprinkles', 48210), ('CosmicCruller', 35975), ('GlazeRunner', 29120), ('NebulaNibbles', 21440),
        ('StarDustin', 16880), ('Bearclaw', 12505), ('MoonPie', 9310), ('Orbit_Olly', 6020), ('Crumbsworth', 3875)]


def save(c, name):
    png = OUT / f'{name}.png'
    c.shot(png)
    from PIL import Image
    Image.open(png).convert('RGB').save(OUT / f'{name}.jpg', quality=92)
    png.unlink()
    print('captured', name)


def build(c):
    rnd = random.Random(7214950366214)
    c.cmd(f'fill -9 {Y - 1} -9 9 {Y + 6} 9 minecraft:air')
    sprinkle = ['yellow_concrete', 'lime_concrete', 'light_blue_concrete', 'white_concrete', 'purple_concrete', 'cyan_concrete']
    for dx in range(-8, 9):
        for dz in range(-8, 9):
            d = math.hypot(dx, dz)
            if not 2.6 <= d <= 7.9:
                continue
            if d < 3.5 or d > 7.0:
                block = 'brown_terracotta' if d < 3.0 or d > 7.5 else 'orange_terracotta'
            else:
                block = rnd.choice(sprinkle) if rnd.random() < 0.13 else 'pink_concrete'
            c.cmd(f'setblock {dx} {Y} {dz} minecraft:{block}')
    for x, z in [(0, -8), (0, 8), (-8, 0), (8, 0)]:
        c.cmd(f'setblock {x} {Y + 1} {z} minecraft:amethyst_cluster')
    for x, z in [(5, 5), (-5, 5), (5, -5), (-5, -5)]:
        c.cmd(f'setblock {x} {Y + 1} {z} minecraft:end_rod')


def setup(c):
    for cmd in ['op Builder', 'gamemode creative Builder', 'clear Builder', 'gamerule doDaylightCycle false', 'gamerule advance_time false',
                'gamerule doWeatherCycle false', 'weather clear', 'time set 12600', 'difficulty peaceful',
                'effect give Builder minecraft:night_vision 99999 0 true', 'forceload add -16 -16 16 16',
                'shardmanager booster stop', 'shardmanager zone delete sugar', 'shardmanager board delete top']:
        c.cmd(cmd)
    build(c)
    c.cmd(f'shardmanager zone create sugar world -7 {Y} -7 7 {Y} 7')
    c.cmd('shardmanager zone set sugar shards 3')
    c.cmd('shardmanager zone set sugar interval 20')
    c.cmd('shardmanager zone set sugar display-name <gradient:#ffd166:#ff78b8>Sugar Orbit</gradient>')
    for name, balance in DEMO:
        c.cmd(f'shardmanager set {name} {balance}')
    c.cmd('shardmanager set Builder 2480')
    c.cmd(f'shardmanager board create top world 0.5 {Y + 3.2} 0.5')
    c.cmd('shardmanager board refresh')
    time.sleep(3)


def look(c, x, y, z, at):
    # Creative players fall unless flying: an invisible barrier holds the camera where it is put.
    c.cmd(f'setblock {math.floor(x)} {math.floor(y) - 1} {math.floor(z)} minecraft:barrier keep')
    c.tp(x, y, z, facing=at)
    time.sleep(2.5)


def scene_overview(c):
    c.hud(False)
    look(c, -10.5, Y + 7, 10.5, (0.5, Y + 1.8, 0.5))
    time.sleep(3)
    save(c, 'overview')
    c.hud(True)


def scene_sweetspot(c):
    c.cmd('shardmanager booster stop')
    look(c, 5.5, Y + 1, 2.5, (0.5, Y + 3.0, 0.5))
    time.sleep(24)          # one payout (shards in chat/action bar) and a running session
    time.sleep(12)
    save(c, 'sweetspot')


def scene_booster(c):
    look(c, -5.5, Y + 1, 1.5, (0.5, Y + 3.0, 0.5))
    c.cmd('shardmanager booster start 2 2h Weekend Sugar Rush')
    time.sleep(4)
    save(c, 'booster')


def scene_board(c):
    c.hud(False)
    look(c, 0.5, Y + 1, 6.5, (0.5, Y + 3.6, 0.5))
    c.cmd('shardmanager board refresh')
    time.sleep(3)
    save(c, 'board')
    c.hud(True)


def scene_wand(c):
    c.cmd('clear Builder')
    c.chat('/shardmanager zone wand')
    time.sleep(1)
    c.key('1')
    look(c, 30.5, Y + 1, 30.5, (30.5, Y, 32.5))
    c.cmd(f'fill 26 {Y} 26 36 {Y} 36 minecraft:smooth_quartz')
    c.cmd(f'setblock 30 {Y} 32 minecraft:pink_concrete')
    c.cmd(f'setblock 34 {Y} 35 minecraft:pink_concrete')
    time.sleep(1.5)
    c.click(1)
    time.sleep(1)
    look(c, 33.5, Y + 1, 33.5, (34.5, Y, 35.5))
    c.click(3)
    time.sleep(1)
    c.chat('/shardmanager zone create cloud-nine')
    time.sleep(1.5)
    save(c, 'wand')
    c.cmd('shardmanager zone delete cloud-nine')
    c.cmd(f'fill 26 {Y} 26 36 {Y} 36 minecraft:air')


def scene_summary(c):
    c.cmd('clear Builder')
    look(c, 1.5, Y + 1, 5.5, (1.5, Y + 1.6, 5.5 + 10))
    time.sleep(45)          # earn in the zone (boosted), then step off the platform edge onto a ledge
    c.cmd(f'fill -2 {Y} 8 4 {Y} 14 minecraft:smooth_quartz replace minecraft:air')
    c.hold_key('w', 1.6)
    time.sleep(2)
    save(c, 'summary')
    c.cmd(f'fill -2 {Y} 8 4 {Y} 14 minecraft:air replace minecraft:smooth_quartz')


def scene_shop(c):
    look(c, 4.5, Y + 1, 0.5, (0.5, Y + 3.0, 0.5))
    c.chat('/shardshop')
    time.sleep(2.5)
    x, y = c.chest_slot(40)
    c.xdo('mousemove', str(x), str(y))
    time.sleep(1.2)
    save(c, 'shop')
    c.key('Escape')


def scene_top(c):
    c.chat('/shards top')
    time.sleep(3)
    x, y = c.chest_slot(0)
    c.xdo('mousemove', str(x), str(y))
    time.sleep(1.2)
    save(c, 'top')
    c.key('Escape')


SCENES = {'overview': scene_overview, 'sweetspot': scene_sweetspot, 'booster': scene_booster, 'board': scene_board,
          'wand': scene_wand, 'summary': scene_summary, 'shop': scene_shop, 'top': scene_top}

if __name__ == '__main__':
    OUT.mkdir(parents=True, exist_ok=True)
    session = Session(CHAT_LOG)
    wanted = sys.argv[1:] or list(SCENES)
    setup(session)
    for scene in wanted:
        SCENES[scene](session)
