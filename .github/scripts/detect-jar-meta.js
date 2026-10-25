#!/usr/bin/env node
'use strict';

/**
 * Reads the publish metadata of a DonutShards release jar from its file name and release/supported-minecraft.json.
 *
 * Release naming: donutshards-{loader}-{minecraft}-{version}.jar where {minecraft} is one version (26.3) or the
 * supported range (1.20.1-26.3). DonutShards is one Paper plugin jar for every supported version; the range must
 * match the first and last entries of supported-minecraft.json, so a stale list can never be published.
 * Legacy 1.x names (DonutShards-1.5.2-paper-folia-mc1.20.1-26.3.jar) are still understood.
 */

const fs = require('fs');
const path = require('path');

const MC = String.raw`(?:26(?:\.\d+)+|1\.\d+(?:\.\d+)?)`;
const VERSION = String.raw`\d+\.\d+\.\d+(?:-[0-9A-Za-z.]+)?`;
const LOADERS = ['paper', 'folia', 'purpur', 'spigot', 'bukkit'];
const CURRENT = new RegExp(`^donutshards-(${LOADERS.join('|')})-(${MC})(?:-(${MC}))?-(${VERSION})\\.jar$`, 'i');
const LEGACY = new RegExp(`^donutshards-(${VERSION})-paper-folia-mc(${MC})-(${MC})\\.jar$`, 'i');

function loadSupported(file = path.join(__dirname, '..', '..', 'release', 'supported-minecraft.json')) {
  const parsed = JSON.parse(fs.readFileSync(file, 'utf8'));
  const loaders = (parsed.loaders || []).map((s) => String(s).toLowerCase());
  const gameVersions = (parsed.game_versions || []).map(String);
  if (!loaders.length || !gameVersions.length) {
    throw new Error(`${file} must list loaders and game_versions`);
  }
  for (const l of loaders) {
    if (!LOADERS.includes(l)) throw new Error(`Unknown loader in ${file}: ${l}`);
  }
  return { loaders, gameVersions };
}

/** @returns {{loader: string, from: string, to: string, version: string} | null} */
function parseName(jar) {
  const name = path.basename(jar);
  const current = name.match(CURRENT);
  if (current) {
    return { loader: current[1].toLowerCase(), from: current[2], to: current[3] || current[2], version: current[4] };
  }
  const legacy = name.match(LEGACY);
  if (legacy) {
    return { loader: 'paper', from: legacy[2], to: legacy[3], version: legacy[1] };
  }
  return null;
}

/**
 * @param {string} jar path or name
 * @param {{loaders: string[], gameVersions: string[]}} supported
 */
function detectRequired(jar, supported = loadSupported()) {
  const parsed = parseName(jar);
  if (!parsed) {
    throw new Error(`Unrecognised release jar name: ${path.basename(jar)} (expected donutshards-{loader}-{minecraft}-{version}.jar)`);
  }
  const first = supported.gameVersions[0];
  const last = supported.gameVersions[supported.gameVersions.length - 1];
  let gameVersions;
  if (parsed.from === parsed.to) {
    if (!supported.gameVersions.includes(parsed.from)) {
      throw new Error(`${path.basename(jar)} names Minecraft ${parsed.from}, which supported-minecraft.json does not list`);
    }
    gameVersions = [parsed.from];
  } else {
    if (parsed.from !== first || parsed.to !== last) {
      throw new Error(`${path.basename(jar)} covers ${parsed.from}-${parsed.to} but supported-minecraft.json lists ${first}-${last}`);
    }
    gameVersions = supported.gameVersions.slice();
  }
  if (!supported.loaders.includes(parsed.loader)) {
    throw new Error(`${path.basename(jar)}: loader ${parsed.loader} is not in supported-minecraft.json`);
  }
  return { loader: parsed.loader, loaders: supported.loaders.slice(), gameVersions, version: parsed.version };
}

module.exports = { parseName, detectRequired, loadSupported };

if (require.main === module) {
  const jar = process.argv[2];
  if (!jar) {
    console.error('Usage: detect-jar-meta.js <jar-path-or-name>');
    process.exit(2);
  }
  try {
    console.log(JSON.stringify(detectRequired(jar)));
  } catch (e) {
    console.error(e.message);
    process.exit(1);
  }
}
