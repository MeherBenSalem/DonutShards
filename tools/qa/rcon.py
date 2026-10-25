#!/usr/bin/env python3
"""Minimal RCON client for driving QA servers.

  python3 tools/qa/rcon.py "list"
  python3 tools/qa/rcon.py --port 25575 --password qa "list" "shardmanager info Builder"
"""
from __future__ import annotations

import argparse
import os
import re
import socket
import struct
import time

COLOR = re.compile('§.')


class Rcon:
    def __init__(self, host='127.0.0.1', port=25575, password='qa', timeout=120.0):
        self.sock = socket.create_connection((host, port), timeout=timeout)
        self.next_id = 1
        if self._request(3, password)[0] == -1:
            raise PermissionError('RCON authentication failed')

    def _send(self, kind, body):
        rid = self.next_id
        self.next_id += 1
        data = struct.pack('<ii', rid, kind) + body.encode('utf-8') + b'\x00\x00'
        self.sock.sendall(struct.pack('<i', len(data)) + data)
        return rid

    def _recv(self, n):
        buf = b''
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError('RCON connection closed')
            buf += chunk
        return buf

    def _read(self):
        length = struct.unpack('<i', self._recv(4))[0]
        payload = self._recv(length)
        rid, kind = struct.unpack('<ii', payload[:8])
        return rid, kind, payload[8:-2].decode('utf-8', 'replace')

    def _request(self, kind, body):
        self._send(kind, body)
        rid, _, text = self._read()
        return rid, text

    def command(self, cmd):
        """Runs a console command and returns its output without colour codes."""
        return COLOR.sub('', self._request(2, cmd)[1])

    def close(self):
        self.sock.close()


class Console:
    """Server console through the FIFO devrun.sh connects to its stdin, for servers whose RCON cannot run commands
    (Folia 1.20.1 answers every RCON command with "Error executing: ... (null)"). Output is read back from the log."""
    LINE = re.compile(r'^\[\d\d:\d\d:\d\d [A-Z]+\]: ?(?:\[[^\]]*\] )?')
    ANSI = re.compile('\x1b\\[[0-9;]*m')

    def __init__(self, fifo, log):
        self.fifo = fifo
        self.log = log

    def command(self, cmd):
        start = os.path.getsize(self.log)
        with open(self.fifo, 'w') as f:
            f.write(cmd + '\n')
        deadline, last, quiet = time.time() + 6, start, time.time()
        while time.time() < deadline:
            size = os.path.getsize(self.log)
            if size != last:
                last, quiet = size, time.time()
            elif size > start and time.time() - quiet > 0.8:
                break
            time.sleep(0.15)
        with open(self.log, 'rb') as f:
            f.seek(start)
            text = f.read().decode('utf-8', 'replace')
        lines = [self.LINE.sub('', self.ANSI.sub('', line)) for line in text.splitlines()]
        return COLOR.sub('', '\n'.join(l for l in lines if l and not l.startswith('>') and 'Thread RCON Client' not in l))

    def close(self):
        pass


class Transport:
    """RCON, switching to the console FIFO for good the first time the server answers "Error executing" (Folia
    1.20.1's RCON fails every command, at times even after a first one succeeded)."""

    def __init__(self, rcon):
        self.rcon = rcon
        self.console = None

    def command(self, cmd):
        if self.console is None:
            out = self.rcon.command(cmd)
            if not out.startswith('Error executing'):
                return out
            home = os.environ.get('QA_HOME', '/tmp/donutshards-qa')
            self.console = Console(os.path.join(home, 'console.fifo'), os.path.join(home, 'server.log'))
        return self.console.command(cmd)

    def close(self):
        self.rcon.close()


def wait_for(port=25575, password='qa', seconds=600):
    deadline = time.time() + seconds
    while time.time() < deadline:
        try:
            return Transport(Rcon(port=port, password=password))
        except OSError:
            time.sleep(2)
    raise TimeoutError('RCON did not come up')


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--port', type=int, default=25575)
    ap.add_argument('--password', default='qa')
    ap.add_argument('commands', nargs='+')
    args = ap.parse_args()
    r = wait_for(port=args.port, password=args.password, seconds=30)
    for c in args.commands:
        print(f'> {c}\n{r.command(c)}')
    r.close()
