// Uploads the verified DonutShards release jar to CurseForge (NightBeam convention: CurseForge only since 24 Nov 2026;
// the existing Modrinth project gets no new versions).
// Env: VERSION, JAR, DRY_RUN ("true": print the metadata and upload nothing), CURSEFORGE_TOKEN, CURSEFORGE_ID.
const fs = require('fs');
const path = require('path');
const { detectRequired } = require(path.join(__dirname, '..', '.github', 'scripts', 'detect-jar-meta.js'));
const { VERSION: version, JAR: jar, DRY_RUN } = process.env;
const dryRun = DRY_RUN === 'true';
const meta = detectRequired(jar);
if (meta.version !== version) throw new Error(`Jar version ${meta.version} != ${version}`);
const changelog = fs.readFileSync(path.join('patchnotes', `${version}.md`), 'utf8');
const prerelease = version.includes('-');
const name = path.basename(jar);

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
console.log('CurseForge project:', process.env.CURSEFORGE_ID || 'NOT SET (set the repo variable CURSEFORGE_ID)');
console.log('CurseForge:', JSON.stringify({ ...curse, changelog: `${changelog.length} chars` }));
(async () => {
  if (dryRun) {
    console.log('Dry run: nothing uploaded.');
    return;
  }
  const bytes = fs.readFileSync(jar);
  if (!process.env.CURSEFORGE_ID) throw new Error('Set the repo variable CURSEFORGE_ID');
  if (!process.env.CURSEFORGE_TOKEN) throw new Error('CURSEFORGE_TOKEN missing');
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
})().catch((e) => { console.error(e.message); process.exit(1); });
