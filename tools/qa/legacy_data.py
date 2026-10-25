#!/usr/bin/env python3
"""Writes a DonutShards 1.5.2 data folder (what an existing server has before upgrading) for the upgrade checks:
1.5.2 config.yml with a custom value, messages.yml with a custom prefix, a sphere zone, shop.yml with a custom
category, and a SQLite ledger in the 1.5.2 schema where Builder holds 1234 shards.

  python3 tools/qa/legacy_data.py <server>/plugins/DonutShards
"""
import hashlib
import sqlite3
import sys
import time
import uuid
from pathlib import Path


def offline_uuid(name):
    digest = bytearray(hashlib.md5(('OfflinePlayer:' + name).encode()).digest())
    digest[6] = (digest[6] & 0x0F) | 0x30
    digest[8] = (digest[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(digest)))


SCHEMA = [
    "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY, checksum VARCHAR(64) NOT NULL, applied_at BIGINT NOT NULL)",
    "CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, last_name VARCHAR(16), balance BIGINT NOT NULL DEFAULT 0, total_earned BIGINT NOT NULL DEFAULT 0, total_spent BIGINT NOT NULL DEFAULT 0, last_reward BIGINT NOT NULL DEFAULT 0, reward_streak INTEGER NOT NULL DEFAULT 0, afk_seconds BIGINT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0)",
    "CREATE TABLE IF NOT EXISTS transactions (id VARCHAR(36) PRIMARY KEY, correlation_id VARCHAR(36) NOT NULL, player_uuid VARCHAR(36) NOT NULL, related_uuid VARCHAR(36), type VARCHAR(32) NOT NULL, amount BIGINT NOT NULL, previous_balance BIGINT NOT NULL, new_balance BIGINT NOT NULL, source VARCHAR(128) NOT NULL, created_at BIGINT NOT NULL, server_id VARCHAR(64) NOT NULL, administrator_uuid VARCHAR(36), metadata TEXT NOT NULL, idempotency_key VARCHAR(128) NOT NULL UNIQUE, rolled_back INTEGER NOT NULL DEFAULT 0)",
    "CREATE INDEX IF NOT EXISTS idx_transactions_player_time ON transactions(player_uuid, created_at)",
    "CREATE TABLE IF NOT EXISTS purchases (id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, item_id VARCHAR(128) NOT NULL, price BIGINT NOT NULL, state VARCHAR(24) NOT NULL, idempotency_key VARCHAR(128) NOT NULL UNIQUE, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL)",
    "CREATE TABLE IF NOT EXISTS reward_history (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid VARCHAR(36) NOT NULL, reward_id VARCHAR(128) NOT NULL, amount BIGINT NOT NULL, created_at BIGINT NOT NULL)",
    "CREATE TABLE IF NOT EXISTS afk_sessions (id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, zone_id VARCHAR(128) NOT NULL, started_at BIGINT NOT NULL, ended_at BIGINT)",
    "CREATE TABLE IF NOT EXISTS shop_limits (scope_key VARCHAR(255) PRIMARY KEY, count BIGINT NOT NULL, resets_at BIGINT NOT NULL)",
    "CREATE TABLE IF NOT EXISTS audit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, category VARCHAR(64) NOT NULL, player_uuid VARCHAR(36), detail TEXT NOT NULL, created_at BIGINT NOT NULL)",
    "CREATE TABLE IF NOT EXISTS permission_grants (id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, permission VARCHAR(255) NOT NULL, expires_at BIGINT, purchase_id VARCHAR(36))",
    "CREATE TABLE IF NOT EXISTS network_fingerprints (fingerprint VARCHAR(128) NOT NULL, player_uuid VARCHAR(36) NOT NULL, last_seen BIGINT NOT NULL, PRIMARY KEY(fingerprint, player_uuid))",
]

FILES = {
    'config.yml': """schema-version: 1
server-id: "server-1"
maximum-balance: 9000000000000000
transfer:
  minimum: 1
  maximum: 1000000
  tax-basis-points: 0
  confirmation-threshold: 100000
leaderboard:
  page-size: 10
  cache-seconds: 60
rewards:
  interval-seconds: 60
  zone-shards: 1
  require-zone-or-home: true
afk:
  auto-rejoin-in-zone: true
  home-mode:
    enabled: true
    shard-multiplier: 0.5
  zone-teleport:
    preferred-zone: ""
  zone-particles:
    enabled: true
    particle: END_ROD
    count-per-point: 1
    ring-points: 32
    vertical-samples: 3
    interval-seconds: 2
    view-radius: 64
debug: false
bstats: false
update-check: false
""",
    'messages.yml': """schema-version: 1
prefix: "<gold>LegacyPrefix</gold> "
balance: "<gray><player>'s balance: <light_purple><balance></light_purple></gray>"
""",
    'zones.yml': """schema-version: 1
default-radius: 10.0
zones:
  legacy-lobby:
    world: world
    x: 200.5
    y: -60.0
    z: 200.5
    radius: 4.0
""",
    'shop.yml': """schema-version: 1
title: "<gold>Legacy Shop"
size: 27
categories:
  legacy:
    slot: 4
    icon: {material: BOOK, name: "<gold>Legacy"}
    items:
      legacy-bread:
        slot: 13
        price: 5
        material: BREAD
        amount: 1
        name: "Legacy Bread"
""",
    'rewards.yml': "schema-version: 1\nrewards: {}\n",
    'anti-abuse.yml': "schema-version: 1\nenabled: true\n",
}


def main(folder):
    folder = Path(folder)
    folder.mkdir(parents=True, exist_ok=True)
    for name, text in FILES.items():
        (folder / name).write_text(text)
    db = sqlite3.connect(folder / 'donutshards.db')
    for sql in SCHEMA:
        db.execute(sql)
    builder = offline_uuid('Builder')
    now = int(time.time() * 1000)
    db.execute("INSERT INTO players(uuid, balance, total_earned) VALUES(?, 1234, 1234)", (builder,))
    db.execute("INSERT INTO transactions VALUES(?,?,?,NULL,'ADMIN_GIVE',1234,0,1234,'admin:give',?,'server-1',NULL,'{}',?,0)",
               (str(uuid.uuid4()), str(uuid.uuid4()), builder, now - 86_400_000, 'legacy-seed'))
    db.commit()
    db.close()


if __name__ == '__main__':
    main(sys.argv[1])
