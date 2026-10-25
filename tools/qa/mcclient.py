#!/usr/bin/env python3
"""Downloads and launches a vanilla Minecraft client for QA (offline name, no account needed).

The client joins the local QA server directly (quick play). Files go to $QA_HOME (default /tmp/donutshards-qa):
versions, libraries and assets are shared between versions; each version gets its own game directory with an
options.txt that skips the first-launch screens.

  python3 tools/qa/mcclient.py fetch 26.3
  python3 tools/qa/mcclient.py launch 26.3 [--port 25565] [--java /usr/lib/jvm/java-25-openjdk-amd64/bin/java]

Sounds and music are not downloaded (the client runs muted); everything else the client loads is.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
import urllib.request
import uuid
import zipfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
ASSET_HOST = 'https://resources.download.minecraft.net'
PLAYER = 'Builder'
HOME = Path(os.environ.get('QA_HOME', '/tmp/donutshards-qa'))


def get(url, dest: Path | None = None, sha1: str | None = None):
    if dest and dest.exists() and (not sha1 or hashlib.sha1(dest.read_bytes()).hexdigest() == sha1):
        return dest
    for attempt in range(4):
        try:
            with urllib.request.urlopen(url, timeout=120) as response:
                data = response.read()
            break
        except Exception:  # noqa: BLE001 - retried, then raised
            if attempt == 3:
                raise
    if dest is None:
        return data
    dest.parent.mkdir(parents=True, exist_ok=True)
    tmp = dest.with_suffix(dest.suffix + '.part')
    tmp.write_bytes(data)
    tmp.replace(dest)
    return dest


def allowed(rules):
    if not rules:
        return True
    result = False
    for rule in rules:
        os_rule = rule.get('os')
        if 'features' in rule:
            continue
        if os_rule and os_rule.get('name') not in (None, 'linux'):
            continue
        if os_rule and os_rule.get('arch') not in (None, 'x86_64', 'x64'):
            continue
        result = rule['action'] == 'allow'
    return result


def version_json(version: str) -> dict:
    path = HOME / 'versions' / version / f'{version}.json'
    if not path.exists():
        manifest = json.loads(get(MANIFEST))
        entry = next(v for v in manifest['versions'] if v['id'] == version)
        get(entry['url'], path, entry.get('sha1'))
    return json.loads(path.read_text())


def fetch(version: str) -> dict:
    data = version_json(version)
    jobs = []
    client = data['downloads']['client']
    jobs.append((client['url'], HOME / 'versions' / version / f'{version}.jar', client['sha1']))
    for lib in data['libraries']:
        if not allowed(lib.get('rules')):
            continue
        downloads = lib.get('downloads', {})
        art = downloads.get('artifact')
        if art:
            jobs.append((art['url'], HOME / 'libraries' / art['path'], art['sha1']))
        native = lib.get('natives', {}).get('linux')
        if native and native in downloads.get('classifiers', {}):
            c = downloads['classifiers'][native]
            jobs.append((c['url'], HOME / 'libraries' / c['path'], c['sha1']))
    index = data['assetIndex']
    index_path = HOME / 'assets' / 'indexes' / f"{index['id']}.json"
    get(index['url'], index_path, index['sha1'])
    for name, obj in json.loads(index_path.read_text())['objects'].items():
        if name.startswith(('minecraft/sounds/', 'minecraft/music/')) or name.endswith('.ogg'):
            continue
        h = obj['hash']
        jobs.append((f'{ASSET_HOST}/{h[:2]}/{h}', HOME / 'assets' / 'objects' / h[:2] / h, h))
    with ThreadPoolExecutor(16) as pool:
        list(pool.map(lambda job: get(*job), jobs))
    return data


def classpath(version: str, data: dict) -> list[Path]:
    paths = []
    natives = HOME / 'versions' / version / 'natives'
    natives.mkdir(parents=True, exist_ok=True)
    for lib in data['libraries']:
        if not allowed(lib.get('rules')):
            continue
        downloads = lib.get('downloads', {})
        if 'artifact' in downloads:
            path = HOME / 'libraries' / downloads['artifact']['path']
            paths.append(path)
            if 'natives-linux' in path.name:
                extract(path, natives)
        native = lib.get('natives', {}).get('linux')
        if native and native in downloads.get('classifiers', {}):
            extract(HOME / 'libraries' / downloads['classifiers'][native]['path'], natives)
    paths.append(HOME / 'versions' / version / f'{version}.jar')
    return paths


def extract(jar: Path, into: Path):
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if name.endswith('.so') and ('x64' in name or 'arm' not in name):
                target = into / Path(name).name
                if not target.exists():
                    target.write_bytes(z.read(name))


def offline_uuid(name: str) -> str:
    digest = bytearray(hashlib.md5(('OfflinePlayer:' + name).encode()).digest())
    digest[6] = (digest[6] & 0x0F) | 0x30
    digest[8] = (digest[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(digest)))


OPTIONS = """onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
tutorialStep:none
pauseOnLostFocus:false
soundCategory_master:0.0
renderDistance:8
simulationDistance:6
guiScale:3
fov:0.0
narrator:0
chatScale:1.0
darkMojangStudiosBackground:true
"""


def launch(version: str, port: int, java: str, width: int, height: int):
    data = fetch(version)
    game = HOME / 'clients' / version
    game.mkdir(parents=True, exist_ok=True)
    options = game / 'options.txt'
    if not options.exists():
        options.write_text(OPTIONS)
    cp = os.pathsep.join(str(p) for p in classpath(version, data))
    natives = HOME / 'versions' / version / 'natives'
    cmd = [java, '-Xmx2G', '-Xss2M', f'-Djava.library.path={natives}', f'-Dorg.lwjgl.librarypath={natives}',
           '-Dminecraft.launcher.brand=donutshards-qa', '-cp', cp, data['mainClass'],
           '--username', PLAYER, '--version', version, '--gameDir', str(game), '--assetsDir', str(HOME / 'assets'),
           '--assetIndex', data['assetIndex']['id'], '--uuid', offline_uuid(PLAYER).replace('-', ''),
           '--accessToken', '0', '--userType', 'legacy', '--versionType', 'release',
           '--width', str(width), '--height', str(height), '--quickPlayMultiplayer', f'localhost:{port}']
    env = dict(os.environ)
    env.setdefault('DISPLAY', ':1')
    env.update(LIBGL_ALWAYS_SOFTWARE='1', GALLIUM_DRIVER='llvmpipe', SDL_VIDEO_FORCE_EGL='1')
    log = open(HOME / f'client-{version}.log', 'w')
    proc = subprocess.Popen(cmd, cwd=game, env=env, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    (HOME / 'client.pid').write_text(str(proc.pid))
    print(proc.pid)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('action', choices=['fetch', 'launch'])
    ap.add_argument('version')
    ap.add_argument('--port', type=int, default=25565)
    ap.add_argument('--java', default=None)
    ap.add_argument('--width', type=int, default=1920)
    ap.add_argument('--height', type=int, default=1080)
    a = ap.parse_args()
    if a.action == 'fetch':
        fetch(a.version)
        print('fetched', a.version)
    else:
        java = a.java or ('/usr/lib/jvm/java-21-openjdk-amd64/bin/java' if a.version.startswith('1.')
                          else '/usr/lib/jvm/java-25-openjdk-amd64/bin/java')
        launch(a.version, a.port, java, a.width, a.height)
        sys.exit(0)
