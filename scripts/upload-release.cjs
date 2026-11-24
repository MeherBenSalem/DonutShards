// Uploads the verified DonutShards release jar to Modrinth and CurseForge (moved out of publish.yml unchanged).
// Env: VERSION, JAR, DRY_RUN ("true": print the metadata and upload nothing), PLATFORMS (both|modrinth|curseforge),
//      MODRINTH_TOKEN, MODRINTH_ID, CURSEFORGE_TOKEN, CURSEFORGE_ID.
const fs = require('fs');
const path = require('path');
const { detectRequired } = require(path.join(__dirname, '..', '.github', 'scripts', 'detect-jar-meta.js'));
const { VERSION: version, JAR: jar, DRY_RUN, PLATFORMS: platforms } = process.env;
const dryRun = DRY_RUN === 'true';
const meta = detectRequired(jar);
if (meta.version !== version) throw new Error(`Jar version ${meta.version} != ${version}`);
const changelog = fs.readFileSync(path.join('patchnotes', `${version}.md`), 'utf8');
const prerelease = version.includes('-');
const doModrinth = platforms !== 'curseforge';
const doCurse = platforms !== 'modrinth';
const name = path.basename(jar);

const modrinth = {
  name: `DonutShards ${version}`,
  version_number: version,
  changelog,
  dependencies: [],
  game_versions: meta.gameVersions,
  version_type: prerelease ? 'beta' : 'release',
  loaders: meta.loaders,
  featured: true,
  status: 'listed',
  project_id: process.env.MODRINTH_ID,
  file_parts: ['file_0'],
  primary_file: 'file_0',
};
// CurseForge Bukkit-plugin projects take game version names.
const curse = {
  changelog,
  changelogType: 'markdown',
  displayName: `DonutShards ${version}`,
  releaseType: prerelease ? 'beta' : 'release',
  // Same tags as the 1.5.x uploads that CurseForge accepted: every game version plus Client and Server.
  gameVersionNames: [...meta.gameVersions, 'Client', 'Server'],
};
console.log('Jar:', name);
console.log('Modrinth:', JSON.stringify({ ...modrinth, changelog: `${changelog.length} chars` }));
console.log('CurseForge:', JSON.stringify({ ...curse, changelog: `${changelog.length} chars` }));
(async () => {
  if (dryRun) {
    console.log('Dry run: nothing uploaded.');
    return;
  }
  const bytes = fs.readFileSync(jar);
  if (doModrinth) {
    if (!process.env.MODRINTH_TOKEN || !process.env.MODRINTH_ID) throw new Error('MODRINTH_TOKEN / MODRINTH_ID missing');
    const form = new FormData();
    form.append('data', JSON.stringify(modrinth));
    form.append('file_0', new Blob([bytes]), name);
    const res = await fetch('https://api.modrinth.com/v2/version', {
      method: 'POST',
      headers: { Authorization: process.env.MODRINTH_TOKEN, 'User-Agent': 'NightBeam/DonutShards publish' },
      body: form,
    });
    const text = await res.text();
    if (!res.ok) throw new Error(`Modrinth ${res.status} ${text.slice(0, 500)}`);
    console.log('Modrinth OK', text.slice(0, 160));
  }
  if (doCurse) {
    if (!process.env.CURSEFORGE_TOKEN || !process.env.CURSEFORGE_ID) throw new Error('CURSEFORGE_TOKEN / CURSEFORGE_ID missing');
    const form = new FormData();
    form.append('metadata', JSON.stringify(curse));
    form.append('file', new Blob([bytes]), name);
    const res = await fetch(`https://minecraft.curseforge.com/api/projects/${encodeURIComponent(process.env.CURSEFORGE_ID)}/upload-file`, {
      method: 'POST',
      headers: { 'X-Api-Token': process.env.CURSEFORGE_TOKEN },
      body: form,
    });
    const text = await res.text();
    if (!res.ok) throw new Error(`CurseForge ${res.status} ${text.slice(0, 500)}`);
    console.log('CurseForge OK', text.slice(0, 160));
  }
})().catch((e) => { console.error(e.message); process.exit(1); });
