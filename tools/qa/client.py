#!/usr/bin/env python3
"""Drives the running QA client (on an X display) together with its server (over RCON).

Commands go to the server console; keys and mouse go to the game window with xdotool; captures are the X display's
pixels (ImageMagick `import`). What the player sees in chat is read back from the client's log ("[CHAT]" lines).

  from client import Session
  s = Session(chat_log='/tmp/donutshards-qa/client-26.3.log')
  s.chat('/afk'); s.wait_chat('Stay-in-place AFK enabled'); s.shot('out.png')
"""
from __future__ import annotations

import math
import os
import re
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from rcon import wait_for  # noqa: E402

PLAYER = 'Builder'
GUI_SCALE = 3  # options.txt written by mcclient.py


class Session:
    def __init__(self, chat_log, display=':1', port=25575):
        self.env = dict(os.environ, DISPLAY=display)
        self.rcon = wait_for(port)
        self.chat_log = Path(chat_log)
        self.hud_hidden = False
        self.mark()

    # Server side
    def cmd(self, *commands):
        out = ''
        for c in commands:
            out = self.rcon.command(c)
        return out

    def tp(self, x, y, z, yaw=0.0, pitch=0.0, facing=None, eye=1.62):
        if facing:
            dx, dy, dz = facing[0] - x, facing[1] - (y + eye), facing[2] - z
            yaw = math.degrees(math.atan2(-dx, dz))
            pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
        return self.cmd(f'tp {PLAYER} {x} {y} {z} {yaw:.2f} {pitch:.2f}')

    # Chat, read from the client log
    def mark(self):
        self.offset = self.chat_log.stat().st_size if self.chat_log.exists() else 0

    def chat_since_mark(self):
        if not self.chat_log.exists():
            return ''
        with open(self.chat_log, 'rb') as f:
            f.seek(self.offset)
            text = f.read().decode('utf-8', 'replace')
        return '\n'.join(line.split('[CHAT] ', 1)[1] for line in text.splitlines() if '[CHAT] ' in line)

    def wait_chat(self, pattern, seconds=15):
        deadline = time.time() + seconds
        while time.time() < deadline:
            text = self.chat_since_mark()
            if re.search(pattern, text):
                return text
            time.sleep(0.5)
        return None

    # Client side
    def xdo(self, *args):
        subprocess.run(['xdotool', *args], env=self.env, check=False, capture_output=True)

    def window(self):
        out = subprocess.run(['xdotool', 'search', '--name', 'Minecraft'], env=self.env, capture_output=True, text=True).stdout.split()
        return out[-1] if out else None

    def focus(self):
        w = self.window()
        if w:
            # Xvfb has no window manager, so focus the window directly.
            self.xdo('windowfocus', '--sync', w)
        return w

    def key(self, *keys, delay=0.2):
        self.focus()
        for k in keys:
            self.xdo('key', '--delay', '60', k)
            time.sleep(delay)

    def hold_key(self, key, seconds):
        self.focus()
        self.xdo('keydown', key)
        time.sleep(seconds)
        self.xdo('keyup', key)

    def type(self, text):
        self.focus()
        self.xdo('type', '--delay', '35', text)

    def chat(self, text):
        """Types a chat line or /command as the player."""
        self.mark()
        self.key('t')
        time.sleep(0.6)
        self.type(text)
        time.sleep(0.3)
        self.key('Return')
        time.sleep(0.4)

    def click(self, button=1, x=None, y=None):
        self.focus()
        if x is not None:
            self.xdo('mousemove', str(x), str(y))
            time.sleep(0.25)
        self.xdo('click', str(button))

    @staticmethod
    def chest_slot(slot, rows=6, width=1920, height=1080):
        """Screen pixel at the centre of a chest-screen slot (vanilla layout at GUI_SCALE)."""
        sw, sh = width // GUI_SCALE, height // GUI_SCALE
        image_h = 114 + rows * 18
        left, top = (sw - 176) // 2, (sh - image_h) // 2
        row, col = divmod(slot, 9)
        return (left + 8 + col * 18 + 8) * GUI_SCALE, (top + 18 + row * 18 + 8) * GUI_SCALE

    def hud(self, visible):
        if visible == self.hud_hidden:
            self.key('F1')
            self.hud_hidden = not visible

    def shot(self, path, crop=None):
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(['import', '-window', 'root', str(path)], env=self.env, check=True)
        if crop:
            from PIL import Image
            Image.open(path).crop(crop).save(path)
        return path

    @staticmethod
    def wait(seconds):
        time.sleep(seconds)
