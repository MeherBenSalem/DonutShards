// Renders the listing artwork in marketing/images/ from real in-game captures (marketing/images/screenshots/) and the
// bundled OFL fonts (Fredoka, Outfit). Usage (from the repository root): node marketing/source/render.cjs [name...]
// Requires the `playwright` package with a Chromium build (NODE_PATH="$(npm root -g)" if installed globally).
const path = require('path');
const fs = require('fs');
const { chromium } = require('playwright');

// The release and what it runs on. One jar covers every version below.
const RELEASE = { version: '2.0', name: 'Sweet Spots' };
const PLATFORMS = ['Paper', 'Folia', 'Purpur'];
const VERSIONS = ['1.20.1', '1.21.x', '26.1.2', '26.2', '26.3'];

// The palette: a night-sky dough, pink glaze, lavender, sprinkle gold, mint and sky blue.
const C = {
  night: '#150b26', plum: '#2a1648', glaze: '#ff78b8', lilac: '#8f7cff', gold: '#ffd166', mint: '#7ff0c8',
  sky: '#6fd3ff', cream: '#fff3f8', dough: '#e8a86a',
};

const root = path.resolve(__dirname, '..', '..');
const out = path.join(root, 'marketing', 'images');
const shots = path.join(out, 'screenshots');
const fonts = path.join(__dirname, 'fonts');
const url = (file) => 'file://' + file;
const shot = (name) => url(path.join(shots, name));

// Seeded sprinkles so every render is identical.
function sprinkles(count, w, h, seed, colors = [C.glaze, C.gold, C.mint, C.sky, C.cream, C.lilac], opacity = 0.9) {
  let s = seed;
  const rnd = () => ((s = (s * 1103515245 + 12345) % 2147483648) / 2147483648);
  let html = '';
  for (let i = 0; i < count; i++) {
    const x = Math.round(rnd() * w), y = Math.round(rnd() * h), r = Math.round(rnd() * 180);
    const c = colors[Math.floor(rnd() * colors.length)];
    html += `<span class="sprinkle" style="left:${x}px;top:${y}px;transform:rotate(${r}deg);background:${c};opacity:${opacity}"></span>`;
  }
  return html;
}

function stars(count, w, h, seed) {
  let s = seed;
  const rnd = () => ((s = (s * 1103515245 + 12345) % 2147483648) / 2147483648);
  let html = '';
  for (let i = 0; i < count; i++) {
    const size = rnd() < 0.15 ? 3 : 2;
    html += `<span class="star" style="left:${Math.round(rnd() * w)}px;top:${Math.round(rnd() * h)}px;width:${size}px;height:${size}px;opacity:${(0.3 + rnd() * 0.6).toFixed(2)}"></span>`;
  }
  return html;
}

const css = `
@font-face { font-family: Display; font-weight: 300 700; src: url('${url(path.join(fonts, 'fredoka.ttf'))}'); }
@font-face { font-family: Body; font-weight: 100 900; src: url('${url(path.join(fonts, 'outfit.ttf'))}'); }
* { box-sizing: border-box; }
html, body { margin: 0; background: ${C.night}; color: ${C.cream}; font-family: Body, sans-serif; font-weight: 500; }
.stage { position: relative; overflow: hidden;
         background: radial-gradient(ellipse at 80% -10%, rgba(255,120,184,.30) 0%, rgba(255,120,184,0) 45%),
                     radial-gradient(ellipse at 0% 110%, rgba(143,124,255,.35) 0%, rgba(143,124,255,0) 50%),
                     linear-gradient(165deg, ${C.plum} 0%, ${C.night} 70%); }
.star { position: absolute; border-radius: 50%; background: #fff; }
.sprinkle { position: absolute; width: 22px; height: 7px; border-radius: 4px; box-shadow: 0 2px 0 rgba(0,0,0,.25); }
.display { font-family: Display, sans-serif; font-weight: 700; letter-spacing: -.005em; }
.title { background: linear-gradient(180deg, #fff 0%, #ffd9ec 40%, ${C.glaze} 75%, #e2569b 100%);
         -webkit-background-clip: text; background-clip: text; color: transparent; }
.shadow { filter: drop-shadow(0 5px 0 rgba(21,11,38,.9)) drop-shadow(0 0 28px rgba(255,120,184,.35)); }
.kicker { font-weight: 800; letter-spacing: .22em; font-size: 22px; text-transform: uppercase; }
.backdrop { position: absolute; inset: -40px; background-size: cover; background-position: center;
            filter: blur(14px) brightness(.28) saturate(1.3) hue-rotate(-10deg); }
.frame { position: relative; overflow: hidden; background: #000; border-radius: 22px; border: 6px solid ${C.cream};
         box-shadow: 0 0 0 4px rgba(21,11,38,.9), 0 0 0 10px var(--ring, ${C.glaze}), 0 28px 60px rgba(0,0,0,.6); }
.frame img { display: block; }
.label { position: absolute; display: inline-flex; align-items: center; gap: 10px; padding: 8px 18px 7px; border-radius: 999px;
         font-weight: 800; font-size: 18px; letter-spacing: .08em; text-transform: uppercase; color: ${C.night}; background: ${C.cream};
         box-shadow: 0 6px 18px rgba(0,0,0,.45); }
.dot { display: inline-block; width: 12px; height: 12px; border-radius: 50%; }
ul.points { list-style: none; padding: 0; margin: 0; }
ul.points li { position: relative; padding-left: 40px; margin: 0 0 15px; font-size: 27px; line-height: 1.3; color: #f6e6f0; }
ul.points li::before { content: ''; position: absolute; left: 0; top: 13px; width: 24px; height: 8px; border-radius: 4px;
                       transform: rotate(-30deg); background: var(--accent, ${C.glaze}); box-shadow: 0 2px 0 rgba(0,0,0,.3); }
b.hi { color: #fff; font-weight: 800; }
.stats { display: flex; margin-top: 24px; border-radius: 18px; overflow: hidden; background: rgba(21,11,38,.9);
         border: 3px solid rgba(255,243,248,.22); box-shadow: 0 18px 50px rgba(0,0,0,.5); }
.stats > div { flex: 1; padding: 13px 22px 11px; border-left: 3px solid rgba(255,243,248,.10); }
.stats > div:first-child { border-left: 0; }
.stats .t { font-weight: 800; font-size: 15px; letter-spacing: .16em; color: #c7a9d8; text-transform: uppercase; }
.stats .v { font-family: Display, sans-serif; font-weight: 700; font-size: 34px; color: ${C.cream}; margin-top: 2px; white-space: nowrap; }
.pill { display: inline-block; border-radius: 999px; padding: 8px 22px 7px; margin-right: 10px; font-weight: 800; font-size: 21px;
        letter-spacing: .05em; color: ${C.night}; background: ${C.glaze}; }
.donut { position: absolute; border-radius: 50%;
         background: radial-gradient(circle closest-side, transparent 0 36%, #b86f3a 36.5% 39%, ${C.dough} 39.5% 45%, ${C.glaze} 45.5% 84%, #e2569b 84.5% 88%, ${C.dough} 88.5% 96%, #b86f3a 96.5% 99.5%, transparent 100%); }
`;

const page = (width, height, body, transparent = false) => `<!doctype html><html><head><meta charset="utf-8"><style>${css}
body { width: ${width}px; height: ${height}px; } .stage { width: ${width}px; height: ${height}px; }
${transparent ? 'html, body, .stage { background: transparent; }' : ''}</style></head>
<body><div class="stage">${body}</div></body></html>`;

// An exact region (x, y, w, h in 1920x1080 pixels) of a capture, scaled to W pixels wide.
const region = (file, x, y, w, h, W) => `<div style="position:relative;width:${W}px;height:${Math.round(W * h / w)}px;overflow:hidden">
    <img src="${shot(file)}" style="position:absolute;max-width:none;width:${W * 1920 / w}px;left:${-x * W / w}px;top:${-y * W / w}px"></div>`;
const framed = (inner, color = C.glaze, style = '') => `<div class="frame" style="--ring:${color};${style}">${inner}</div>`;
const label = (text, color, pos) => `<span class="label" style="${pos}"><span class="dot" style="background:${color}"></span>${text}</span>`;
const stats = (items) => `<div class="stats">${items.map(([t, v, flex]) =>
  `<div${flex ? ` style="flex:${flex}"` : ''}><div class="t">${t}</div><div class="v">${v}</div></div>`).join('')}</div>`;

// A feature card: copy on the left, artwork on the right.
const card = ({ kicker, title, points, art, artWidth = 920, backdrop = 'overview.jpg', color = C.glaze, seed = 7 }) => page(1600, 900, `
  <div class="backdrop" style="background-image:url('${shot(backdrop)}')"></div>${stars(70, 1600, 900, seed)}
  <div style="position:absolute;left:78px;top:0;bottom:0;width:${1600 - artWidth - 160}px;display:flex;flex-direction:column;justify-content:center;--accent:${color}">
    <div class="kicker" style="color:${color}">${kicker}</div>
    <div class="display title shadow" style="font-size:72px;line-height:1;margin:14px 0 26px">${title}</div>
    <ul class="points">${points.map((point) => `<li>${point.endsWith('>') ? point : point.replace(/ (\S+)$/, '&nbsp;$1')}</li>`).join('')}</ul>
  </div>
  <div style="position:absolute;right:70px;top:50%;transform:translateY(-50%);width:${artWidth}px">${art}</div>`);

const versionText = VERSIONS.join(' · ');

const header = (title, color = C.glaze) => page(1400, 120, `
  <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:26px">
    <div style="position:relative;width:260px;height:30px">${sprinkles(9, 240, 22, title.length * 7, undefined, 0.85)}</div>
    <div class="donut" style="position:relative;width:54px;height:54px"></div>
    <div class="display title shadow" style="font-size:60px;white-space:nowrap">${title}</div>
    <div class="donut" style="position:relative;width:54px;height:54px"></div>
    <div style="position:relative;width:260px;height:30px">${sprinkles(9, 240, 22, title.length * 13, undefined, 0.85)}</div>
  </div>`, true);

const divider = () => page(1400, 44, `
  <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:18px">
    <div style="height:4px;width:470px;border-radius:2px;background:linear-gradient(90deg,transparent,${C.lilac})"></div>
    ${[C.glaze, C.gold, C.mint, C.sky, C.lilac].map((c, i) => `<span class="sprinkle" style="position:relative;display:inline-block;background:${c};transform:rotate(${i % 2 ? 30 : -30}deg)"></span>`).join('')}
    <div style="height:4px;width:470px;border-radius:2px;background:linear-gradient(270deg,transparent,${C.lilac})"></div>
  </div>`, true);

// The icon: a glazed donut with a crystal shard rising out of its hole, on the night sky.
const icon = () => page(512, 512, `
  <div style="position:absolute;inset:0;border-radius:110px;overflow:hidden;background:radial-gradient(circle at 50% 40%, ${C.plum} 0%, ${C.night} 75%)">
    ${stars(40, 512, 512, 3)}
    <div class="donut" style="left:46px;top:70px;width:420px;height:420px;filter:drop-shadow(0 16px 20px rgba(0,0,0,.55))"></div>
    <div style="position:absolute;left:46px;top:70px;width:420px;height:420px;transform:scale(.955);transform-origin:220px 220px">${(() => {
      // Sprinkles on the glaze ring only.
      let s = 11; const rnd = () => ((s = (s * 1103515245 + 12345) % 2147483648) / 2147483648);
      let h = '';
      const colors = [C.gold, C.mint, C.sky, C.cream, C.lilac];
      for (let i = 0; i < 26; i++) {
        const a = rnd() * Math.PI * 2, r = 220 * (0.52 + rnd() * 0.28);
        h += `<span class="sprinkle" style="width:30px;height:10px;left:${220 + Math.cos(a) * r - 15}px;top:${220 + Math.sin(a) * r - 5}px;transform:rotate(${Math.round(rnd() * 180)}deg);background:${colors[i % colors.length]}"></span>`;
      }
      return h;
    })()}</div>
    <div style="position:absolute;left:206px;top:44px;width:100px;height:250px;
                clip-path:polygon(50% 0%, 100% 30%, 80% 100%, 20% 100%, 0% 30%);
                background:linear-gradient(115deg, #fff 0%, #d9ccff 22%, ${C.lilac} 48%, #5b3fd6 78%, #3a238f 100%);
                filter:drop-shadow(0 0 22px rgba(143,124,255,.9))"></div>
    <div style="position:absolute;left:206px;top:44px;width:100px;height:250px;
                clip-path:polygon(50% 0%, 50% 100%, 20% 100%, 0% 30%);background:rgba(255,255,255,.28)"></div>
  </div>
  <div style="position:absolute;inset:0;border-radius:110px;border:10px solid ${C.glaze};box-shadow:inset 0 0 0 5px ${C.night}"></div>`, true);

const assets = {
  icon,

  banner: () => page(1800, 600, `
    <img src="${shot('overview.jpg')}" style="position:absolute;right:-170px;top:-70px;width:1180px;-webkit-mask-image:linear-gradient(90deg,transparent 0%,#000 32%);mask-image:linear-gradient(90deg,transparent 0%,#000 32%)">
    <div style="position:absolute;inset:0;background:linear-gradient(90deg,rgba(21,11,38,.98) 0%,rgba(21,11,38,.93) 38%,rgba(21,11,38,.30) 66%,rgba(21,11,38,0) 85%)"></div>
    ${stars(90, 900, 600, 21)}
    <div style="position:absolute;left:96px;top:66px;width:1100px">
      <div class="kicker" style="color:${C.gold}">${RELEASE.version} · ${RELEASE.name}</div>
      <div class="display title shadow" style="display:inline-block;font-size:122px;line-height:1;margin:12px 0 18px;white-space:nowrap">DonutShards</div>
      <div style="font-size:34px;color:${C.cream};line-height:1.28;font-weight:700">AFK zones you draw with a wand, boss-bar countdowns,<br>server boosters, Daily Deals and leaderboards in the world.</div>
      <div style="margin-top:30px">${PLATFORMS.map((l) => `<span class="pill">${l}</span>`).join('')}
        <span style="font-size:22px;color:#e6c9de;margin-left:6px;font-weight:800">${versionText}</span></div>
    </div>`),

  'card-sweetspots': () => card({
    kicker: 'The headline', title: 'Sweet Spots', color: C.glaze, backdrop: 'sweetspot.jpg',
    points: ['Draw AFK zones with the <b class="hi">Sweet Spot Wand</b>: two clicks, one command',
      'Each zone has its <b class="hi">own payout, interval, permission and name</b>',
      'A <b class="hi">boss bar</b> counts down to the next payout and adds up the session',
      'Your own timer: walk out and it stops, walk back and it starts over'],
    art: `<div style="position:relative">${framed(region('sweetspot.jpg', 160, 0, 1600, 900, 920))}
      <div class="frame" style="--ring:${C.gold};position:absolute;right:-24px;bottom:-70px;width:420px">${region('wand.jpg', 0, 760, 1100, 300, 420)}</div>
      ${label('Corners set with the wand', C.gold, 'right:-4px;bottom:-112px;font-size:14px')}</div>
      <div style="margin-top:96px;width:470px">${stats([['Shapes', 'Box · Sphere'], ['Per zone', '4 settings']])}</div>` }),

  'card-boosters': () => card({
    kicker: 'Events', title: 'Shard<br>Boosters', color: C.gold, backdrop: 'booster.jpg', seed: 9,
    points: ['<b class="hi">/shardmanager booster start 2 1h Weekend</b>',
      'Every player sees a booster bar with the time left', 'Survives a restart; ends on its own',
      'Rank multipliers too: <b class="hi">shards.multiplier.1.5</b>'],
    art: `${framed(region('booster.jpg', 160, 0, 1600, 900, 920), C.gold)}
      ${stats([['Booster', '×2'], ['Rank', '×1.5'], ['Payout', 'base × both']])}` }),

  'card-deals': () => card({
    kicker: 'Shop', title: 'Daily<br>Deals', color: C.mint, backdrop: 'shop.jpg', seed: 13,
    points: ['A row of offers that <b class="hi">changes every day</b>', 'The same deals for every player, restart or not',
      'A live countdown and a <b class="hi">per-player limit</b> in the lore', 'Limits are checked with the payment: no double buys'],
    art: framed(region('shop.jpg', 640, 180, 920, 840, 780), C.mint, 'margin-left:60px'), artWidth: 840 }),

  'card-boards': () => card({
    kicker: 'Bragging rights', title: 'Leaderboards<br>in the world', color: C.lilac, backdrop: 'board.jpg', seed: 17,
    points: ['<b class="hi">/shardmanager board create top</b> places a live top 10', 'No hologram plugin needed',
      'Refreshes on its own, Folia-safe, never duplicates', 'Plus the <b class="hi">/shards top</b> menu with player heads'],
    art: `<div style="position:relative">${framed(region('board.jpg', 360, 60, 1200, 675, 920), C.lilac)}
      <div class="frame" style="--ring:${C.glaze};position:absolute;right:-24px;bottom:-80px;width:380px">${region('top.jpg', 620, 160, 680, 560, 380)}</div></div>` }),

  'card-economy': () => card({
    kicker: 'The economy', title: 'Fair by<br>design', color: C.sky, backdrop: 'overview.jpg', seed: 23,
    points: ['A SQL ledger: every shard has a source, <b class="hi">/shards history</b> shows it',
      'Atomic payments with tax, limits and a confirm step', 'Kill rewards, home AFK and Vault conversion',
      'Session summary when you leave a zone'],
    art: `${framed(region('summary.jpg', 0, 740, 1300, 340, 920), C.sky)}
      ${stats([['Storage', 'SQLite · MariaDB'], ['API', 'async + events']])}` }),

  'card-platforms': () => card({
    kicker: 'For server owners', title: 'One jar,<br>every server', color: C.glaze, backdrop: 'overview.jpg', seed: 29,
    points: ['<b class="hi">Paper, Folia and Purpur</b>, Minecraft 1.20.1 to 26.3', 'Region-safe scheduling, made for Folia',
      'Upgrades from 1.x keep balances, zones and edits', 'PlaceholderAPI, Vault, bStats, no other plugin needed'],
    art: `${framed(region('overview.jpg', 0, 0, 1920, 1080, 920))}
      ${stats([['Platforms', 'Paper · Folia · Purpur', 2], ['Java', '17+']])}` }),
};

const HEADERS = {
  'header-whats-new': ['What’s New in 2.0', C.glaze], 'header-sweetspots': ['Sweet Spots', C.glaze],
  'header-boosters': ['Boosters', C.gold], 'header-deals': ['Daily Deals', C.mint],
  'header-boards': ['Leaderboards', C.lilac], 'header-economy': ['The Economy', C.sky],
  'header-commands': ['Commands', C.glaze], 'header-downloads': ['Download', C.glaze],
  'header-faq': ['FAQ', C.glaze], 'header-gallery': ['Gallery', C.glaze],
};
for (const [name, [title, color]] of Object.entries(HEADERS)) {
  assets[name] = () => header(title, color);
}
assets.divider = divider;

(async () => {
  const wanted = process.argv.slice(2);
  const names = wanted.length ? wanted : Object.keys(assets);
  const browser = await chromium.launch();
  const context = await browser.newContext({ deviceScaleFactor: 1 });
  for (const name of names) {
    if (!assets[name]) {
      console.error('unknown asset', name);
      continue;
    }
    const file = path.join(__dirname, `.render-${name}.html`);
    fs.writeFileSync(file, assets[name]());
    const p = await context.newPage();
    await p.goto(url(file));
    await p.evaluate(() => document.fonts.ready);
    await p.waitForTimeout(200);
    const { width, height } = await p.evaluate(() => ({ width: document.body.scrollWidth, height: document.body.scrollHeight }));
    await p.setViewportSize({ width, height });
    const transparent = name.startsWith('header') || name === 'divider' || name === 'icon';
    await p.screenshot({ path: path.join(out, `${name}.png`), omitBackground: transparent });
    await p.close();
    fs.unlinkSync(file);
    console.log('rendered', name);
  }
  await browser.close();
})();
