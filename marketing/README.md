# Marketing

Listing copy and artwork for CurseForge, Modrinth and Hangar.

| Path | What it is |
|------|------------|
| `curseforge/description.md` | The project description (Markdown; also works on Modrinth). Images load from this repo's `master` branch (its default branch). |
| `curseforge/summary.txt` | The one-line summary (under 250 characters). |
| `images/banner.png` | 1800×600 header banner. |
| `images/icon.png` | 512×512 project icon (also `logo.png` inside the jar). |
| `images/card-*.png` | 1600×900 feature cards for the description and the gallery. |
| `images/header-*.png`, `images/divider.png` | Transparent section headers and a divider. |
| `images/screenshots/*.jpg` | Real in-game captures (Minecraft 26.3, Paper) used by the cards and the gallery. |
| `source/render.cjs` | Renders every PNG above from the screenshots and the bundled fonts. |
| `source/fonts/` | Fredoka and Outfit, both under the SIL Open Font License (licence files included). |

The look: a cosmic donut shop at night. A deep plum night sky with scattered stars behind everything, pink-glaze
titles in rounded Fredoka with a white-to-pink gradient, Outfit for the copy, cream photo frames ringed in each
feature's colour, sprinkle-shaped bullets and dividers, and one colour per feature (glaze pink for Sweet Spots, gold
for boosters, mint for Daily Deals, lavender for leaderboards, sky blue for the economy). The icon is a glazed donut
with a lavender crystal shard rising out of its hole.

## Regenerating the images

Needs Node.js and Playwright with Chromium:

```bash
node marketing/source/render.cjs              # everything
node marketing/source/render.cjs card-deals   # one asset
```

The release name, platforms and versions are at the top of `render.cjs`. After changing the icon, copy
`images/icon.png` to `src/main/resources/logo.png`.

## Retaking the screenshots

`tools/qa/marketing_shots.py` captures every screenshot from a real 26.3 client (1920×1080 under Xvfb) on a Paper
26.3 server with a normal world:

```bash
./gradlew release
tools/qa/devrun.sh prepare-server paper 26.3 --level-type 'minecraft\:normal'
tools/qa/devrun.sh start-server paper 26.3 && python3 tools/qa/rcon.py "op Builder"
DISPLAY=:1 tools/qa/devrun.sh start-client 26.3
python3 tools/qa/marketing_shots.py              # or some: overview shop board
```

The script builds a donut-shaped platform in the sky with commands (pink concrete glaze, terracotta dough, concrete
sprinkles, a few amethyst clusters and end rods), makes it a zone with `/shardmanager zone create`, gives it a payout,
interval and display name, places a leaderboard board over the hole and sets demo balances for the other names on
the leaderboard (`/shardmanager set`). Everything DonutShards shows in the pictures is the plugin running: boss bars,
chat messages, the wand, the shop and its Daily Deals tooltip, the top menu, the board and the zone outline. Every
screenshot is a single unedited frame.

| Screenshot | Scene |
|------------|-------|
| `overview.jpg` | The donut platform from above, HUD hidden: zone outline particles and the board over the hole |
| `sweetspot.jpg` | Standing in the zone: the AFK boss bar with the countdown and session total |
| `booster.jpg` | Right after `/shardmanager booster start`: the booster bar and the chat announcement |
| `board.jpg` | The leaderboard board up close, HUD hidden |
| `wand.jpg` | Two corners picked with the wand and the zone created (chat) |
| `summary.jpg` | Walking out of the zone: the session summary in chat |
| `shop.jpg` | `/shardshop` with the tooltip of a Daily Deal |
| `top.jpg` | The `/shards top` menu |
