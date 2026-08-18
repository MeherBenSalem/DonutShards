#!/usr/bin/env node
/**
 * Local publish for DonutShards — Modrinth + CurseForge.
 *
 * Usage:
 *   node scripts/publish-local.mjs --version 1.3.0 [--platforms both|modrinth|curseforge]
 *
 * Requires env (or repo-root .env):
 *   MODRINTH_TOKEN, CURSEFORGE_TOKEN, CURSEFORGE_API_KEY
 *   MODRINTH_ID (default 4krPhA6H), CURSEFORGE_ID (default 1606311)
 *   DEFAULT_LOADER (default paper), DEFAULT_GAME_VERSION (default 1.20.1)
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, '..');

function loadDotEnv(file) {
  if (!fs.existsSync(file)) return;
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith('#')) continue;
    const i = t.indexOf('=');
    if (i <= 0) continue;
    const key = t.slice(0, i).trim();
    let val = t.slice(i + 1).trim();
    if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'"))) {
      val = val.slice(1, -1);
    }
    if (!process.env[key]) process.env[key] = val;
  }
}

loadDotEnv(path.join(root, '.env'));
loadDotEnv(path.join(process.env.USERPROFILE || '', '.nightbeam', '.env'));

function arg(name, fallback) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : fallback;
}

const version = arg('version');
const platforms = (arg('platforms', 'both') || 'both').toLowerCase();
if (!version) {
  console.error('Usage: node scripts/publish-local.mjs --version 1.3.0 [--platforms both|modrinth|curseforge]');
  process.exit(1);
}

for (const k of ['MODRINTH_TOKEN', 'CURSEFORGE_TOKEN', 'CURSEFORGE_API_KEY']) {
  if (!process.env[k]) {
    console.error(`Missing ${k}. Set it in .env or the environment.`);
    process.exit(1);
  }
}

const MODRINTH_ID = process.env.MODRINTH_ID || '4krPhA6H';
const CURSEFORGE_ID = process.env.CURSEFORGE_ID || '1606311';
const DEFAULT_LOADER = (process.env.DEFAULT_LOADER || 'paper').toLowerCase();
const DEFAULT_GAME_VERSION = process.env.DEFAULT_GAME_VERSION || '1.20.1';

const jarName = `DonutShards-${version}-paper-folia-mc1.20.1-26.1.2.jar`;
const jarCandidates = [
  path.join(root, 'releases', jarName),
  path.join(root, 'build', 'release', jarName),
];
const jar = jarCandidates.find((p) => fs.existsSync(p));
if (!jar) {
  console.error(`No jar found. Expected one of:\n${jarCandidates.join('\n')}`);
  process.exit(1);
}

function resolveChangelog(ver) {
  const candidates = [
    path.join(root, 'CHANGELOG.md'),
    path.join(root, 'PATCH_NOTES.md'),
    ...fs.readdirSync(root).filter(
      (f) => f.toLowerCase().endsWith(`-${ver}-patchnotes.md`) || f.toLowerCase() === `${ver}-patchnotes.md`,
    ).map((f) => path.join(root, f)),
  ];
  for (const f of candidates) {
    if (fs.existsSync(f)) return fs.readFileSync(f, 'utf8');
  }
  return `Release ${ver}`;
}

const changelog = resolveChangelog(version);
const LOADER_IDS = { fabric: 7499, forge: 7498, neoforge: 10150, quilt: 9153 };
const PAPER_LIKE = new Set(['paper', 'bukkit', 'spigot', 'folia', 'purpur']);

function detect(jarPath) {
  const n = path.basename(jarPath).toLowerCase();
  let loader = DEFAULT_LOADER;
  let gv = DEFAULT_GAME_VERSION;
  const mc = n.match(/(?:^|-)mc(1\.\d+(?:\.\d+)?)(?:-|$)/);
  if (mc) gv = mc[1];
  return { loader, gv };
}

function flattenVersions(payload) {
  const flat = [];
  const data = payload?.data ?? payload;
  if (!Array.isArray(data)) return flat;
  for (const entry of data) {
    if (Array.isArray(entry?.versions)) {
      for (const v of entry.versions) flat.push(v);
    } else if (entry && entry.id != null && entry.name) {
      flat.push(entry);
    }
  }
  return flat;
}

function findVersionId(flat, name) {
  const exact = flat.find((v) => v.name === name || v.slug === name);
  if (exact) return exact.id;
  const loose = flat.find((v) => String(v.name).toLowerCase() === String(name).toLowerCase());
  return loose?.id;
}

function findMcVersionId(flat, want) {
  const semver = /^\d+\.\d+(?:\.\d+)?$/;
  const exact = flat.find((v) => semver.test(String(v.name)) && v.name === want);
  if (exact) return exact.id;
  return findVersionId(flat, want);
}

const { loader, gv } = detect(jar);
const doModrinth = platforms === 'both' || platforms === 'modrinth';
const doCurse = platforms === 'both' || platforms === 'curseforge';

console.log(`Publishing ${path.basename(jar)} (${loader}, MC ${gv}) to ${platforms}`);

await (async () => {
  if (doModrinth) {
    const gameVersions = [gv];
    const body = {
      name: `${version} · ${loader} · ${gameVersions[0]}`,
      version_number: `${version}+${loader}-${gameVersions[0]}`,
      changelog,
      dependencies: [],
      game_versions: gameVersions,
      version_type: 'release',
      loaders: [loader === 'paper' ? 'paper' : loader],
      featured: false,
      status: 'listed',
      project_id: MODRINTH_ID,
      file_parts: ['file_0'],
      primary_file: 'file_0',
    };
    const form = new FormData();
    form.append('data', JSON.stringify(body));
    form.append('file_0', new Blob([fs.readFileSync(jar)]), path.basename(jar));
    const mrRes = await fetch('https://api.modrinth.com/v2/version', {
      method: 'POST',
      headers: { Authorization: process.env.MODRINTH_TOKEN },
      body: form,
    });
    const mrText = await mrRes.text();
    if (!mrRes.ok) throw new Error(`Modrinth ${mrRes.status} ${mrText.slice(0, 500)}`);
    console.log('Modrinth OK', body.version_number, mrText.slice(0, 160));
  }

  if (!doCurse) return;

  let flat = [];
  const uploadGv = await fetch('https://minecraft.curseforge.com/api/game/versions', {
    headers: { 'X-Api-Token': process.env.CURSEFORGE_TOKEN },
  });
  if (uploadGv.ok) {
    flat = flattenVersions(await uploadGv.json());
    console.log('CurseForge versions via upload API:', flat.length);
  }
  if (!flat.length) {
    const gvRes = await fetch('https://api.curseforge.com/v1/games/432/versions', {
      headers: { 'x-api-key': process.env.CURSEFORGE_API_KEY },
    });
    const gvText = await gvRes.text();
    if (!gvRes.ok) throw new Error(`CurseForge versions ${gvRes.status} ${gvText.slice(0, 300)}`);
    flat = flattenVersions(JSON.parse(gvText));
    console.log('CurseForge versions via core API:', flat.length);
  }
  if (!flat.length) throw new Error('No CurseForge game versions resolved');

  const want = gv || DEFAULT_GAME_VERSION;
  const mcId = findMcVersionId(flat, want);
  if (mcId == null) throw new Error(`No CurseForge game version id for ${want}`);

  const isPaperLike = PAPER_LIKE.has(loader);
  const gameVersions = [mcId];
  if (!isPaperLike) {
    const loaderId = LOADER_IDS[loader];
    if (loaderId != null) gameVersions.push(loaderId);
    for (const tag of ['Client', 'Server']) {
      const id = findVersionId(flat, tag);
      if (id != null) gameVersions.push(id);
    }
  }

  const meta = {
    changelog,
    changelogType: 'markdown',
    displayName: `${version} · ${loader} · ${want}`,
    gameVersions,
    releaseType: 'release',
  };
  if (isPaperLike) meta.gameVersionNames = ['Client', 'Server'];

  const cfForm = new FormData();
  cfForm.append('metadata', JSON.stringify(meta));
  cfForm.append('file', new Blob([fs.readFileSync(jar)]), path.basename(jar));
  const cfRes = await fetch(
    `https://minecraft.curseforge.com/api/projects/${CURSEFORGE_ID}/upload-file`,
    { method: 'POST', headers: { 'X-Api-Token': process.env.CURSEFORGE_TOKEN }, body: cfForm },
  );
  const cfText = await cfRes.text();
  if (!cfRes.ok) throw new Error(`CurseForge ${cfRes.status} ${cfText.slice(0, 500)}`);
  console.log('CurseForge OK', meta.displayName, cfText.slice(0, 160));
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
