#!/usr/bin/env node
/**
 * Local publish for DonutShards — Modrinth + CurseForge.
 *
 * Usage:
 *   node scripts/publish-local.mjs --version 1.4.0 [--platforms both|modrinth|curseforge]
 *
 * Requires env (or repo-root .env):
 *   MODRINTH_TOKEN, CURSEFORGE_TOKEN, CURSEFORGE_API_KEY
 *   MODRINTH_ID (default 4krPhA6H), CURSEFORGE_ID (default 1606311)
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
  console.error('Usage: node scripts/publish-local.mjs --version 1.4.0 [--platforms both|modrinth|curseforge]');
  process.exit(1);
}

const doModrinth = platforms === 'both' || platforms === 'modrinth';
const doCurse = platforms === 'both' || platforms === 'curseforge';

for (const k of [
  ...(doModrinth ? ['MODRINTH_TOKEN'] : []),
  ...(doCurse ? ['CURSEFORGE_TOKEN', 'CURSEFORGE_API_KEY'] : []),
]) {
  if (!process.env[k]) {
    console.error(`Missing ${k}. Set it in .env or the environment.`);
    process.exit(1);
  }
}

const MODRINTH_ID = process.env.MODRINTH_ID || '4krPhA6H';
const CURSEFORGE_ID = process.env.CURSEFORGE_ID || '1606311';

const supportPath = path.join(root, 'release', 'supported-minecraft.json');
const support = JSON.parse(fs.readFileSync(supportPath, 'utf8'));
const gameVersions = Array.isArray(support) ? support : support.game_versions;
const loaders = Array.isArray(support)
  ? ['paper', 'folia', 'purpur', 'spigot', 'bukkit']
  : support.loaders;
if (!Array.isArray(gameVersions) || gameVersions.length === 0) {
  console.error('release/supported-minecraft.json has no game versions');
  process.exit(1);
}
if (!Array.isArray(loaders) || loaders.length === 0) {
  console.error('release/supported-minecraft.json has no loaders');
  process.exit(1);
}

const jarName = `DonutShards-${version}-paper-folia-mc1.20.1-26.2.jar`;
const jarCandidates = [
  path.join(root, 'releases', jarName),
  path.join(root, 'build', 'release', jarName),
];
const jar = jarCandidates.find((p) => fs.existsSync(p));
if (!jar) {
  console.error(`No jar found. Expected one of:\n${jarCandidates.join('\n')}`);
  process.exit(1);
}

function sectionForVersion(markdown, ver) {
  const lines = markdown.split(/\r?\n/);
  const start = lines.findIndex((line) => {
    const t = line.trim();
    return t === `## ${ver}` || t === `# ${ver}` || t.startsWith(`## ${ver} `) || t.startsWith(`# ${ver} `);
  });
  if (start < 0) return null;
  const rest = lines.slice(start + 1);
  const end = rest.findIndex((line) => /^##\s/.test(line.trim()));
  const body = (end < 0 ? rest : rest.slice(0, end)).join('\n').trim();
  return body || null;
}

function resolveChangelog(ver) {
  const named = fs.readdirSync(root).filter(
    (f) =>
      f.toLowerCase().endsWith(`-${ver}-patchnotes.md`) ||
      f.toLowerCase() === `${ver}-patchnotes.md`,
  );
  for (const f of named) {
    const p = path.join(root, f);
    if (fs.existsSync(p)) return fs.readFileSync(p, 'utf8');
  }
  for (const f of ['CHANGELOG.md', 'PATCH_NOTES.md']) {
    const p = path.join(root, f);
    if (!fs.existsSync(p)) continue;
    const full = fs.readFileSync(p, 'utf8');
    const section = sectionForVersion(full, ver);
    if (section) return section;
    return full;
  }
  return `Release ${ver}`;
}

const changelog = resolveChangelog(version);

console.log(`Publishing ${path.basename(jar)} (${loaders.join('+')}, ${gameVersions.length} MC versions) to ${platforms}`);

await (async () => {
  if (doModrinth) {
    const body = {
      name: version,
      version_number: version,
      changelog,
      dependencies: [],
      game_versions: gameVersions,
      version_type: 'release',
      loaders,
      featured: true,
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
    console.log('Modrinth OK', version, loaders.join('+'), gameVersions.length, 'MC versions');
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

  const gameVersionNames = [...gameVersions, 'Client', 'Server'];
  const meta = {
    changelog,
    changelogType: 'markdown',
    displayName: version,
    gameVersionNames,
    releaseType: 'release',
  };

  const cfForm = new FormData();
  cfForm.append('metadata', JSON.stringify(meta));
  cfForm.append('file', new Blob([fs.readFileSync(jar)]), path.basename(jar));
  const cfRes = await fetch(
    `https://minecraft.curseforge.com/api/projects/${CURSEFORGE_ID}/upload-file`,
    { method: 'POST', headers: { 'X-Api-Token': process.env.CURSEFORGE_TOKEN }, body: cfForm },
  );
  const cfText = await cfRes.text();
  if (!cfRes.ok) throw new Error(`CurseForge ${cfRes.status} ${cfText.slice(0, 500)}`);
  console.log('CurseForge OK', version, gameVersionNames.length, 'tags', path.basename(jar));
})().catch((e) => {
  console.error(e);
  process.exit(1);
});

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
