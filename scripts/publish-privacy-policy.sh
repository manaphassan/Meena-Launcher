#!/usr/bin/env bash
# Publishes the Meena site to GitHub Pages: the home page, docs/privacy-policy.md and
# docs/tester-guide.md.
#   https://manaphassan.github.io/Meena-Launcher/
#   https://manaphassan.github.io/Meena-Launcher/privacy-policy/
#   https://manaphassan.github.io/Meena-Launcher/tester-guide/
# Renders the Markdown with GitHub's own Markdown API (gh CLI, logged in), wraps it in a plain
# page, and commits it to the gh-pages branch through a worktree, so the current checkout is
# left alone. Run from the repo root, then push: git -C build/tmp/ghpages push
set -euo pipefail

site=build/tmp/ghpages
if [ ! -d "$site" ]; then
  git fetch origin gh-pages
  git worktree add "$site" gh-pages
fi

body=$(gh api markdown -F text=@docs/privacy-policy.md -f mode=gfm -f context=manaphassan/Meena-Launcher)
guide=$(gh api markdown -F text=@docs/tester-guide.md -f mode=gfm -f context=manaphassan/Meena-Launcher)

page() {
cat <<EOF
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>$1</title>
<link rel="icon" type="image/svg+xml" href="/Meena-Launcher/meena-logo.svg">
<style>
  body { margin: 0 auto; max-width: 760px; padding: 24px; font: 16px/1.6 "Segoe UI", system-ui, sans-serif; color: #111; background: #fff; }
  h1 { font-weight: 300; font-size: 2.2em; line-height: 1.2; }
  h2 { font-weight: 400; margin-top: 2em; }
  h3 { font-weight: 400; }
  a { color: #0050a0; }
  code { font-family: Consolas, monospace; font-size: 0.9em; background: #f2f2f2; padding: 0 4px; }
  table { border-collapse: collapse; width: 100%; display: block; overflow-x: auto; }
  th, td { text-align: left; vertical-align: top; padding: 6px 12px 6px 0; border-bottom: 1px solid #ddd; }
  hr { border: 0; border-top: 1px solid #ddd; margin: 24px 0; }
  .hero { display: flex; align-items: center; gap: 24px; margin-top: 24px; }
  .hero h1 { margin: 0; font-size: 3em; }
  .hero p { margin: 0; }
  img.wide { width: 100%; height: auto; display: block; margin: 24px 0; }
  ul.links { list-style: none; padding: 0; }
  ul.links li { margin: 6px 0; }
  .shots { display: flex; gap: 12px; margin: 24px 0; }
  .shots img { width: calc((100% - 24px) / 3); height: auto; display: block; }
  @media (prefers-color-scheme: dark) {
    body { color: #eee; background: #000; } a { color: #4fb3ff; }
    code { background: #1a1a1a; } th, td, hr { border-color: #333; }
  }
</style>
</head>
<body>
$2
</body>
</html>
EOF
}

mkdir -p "$site/privacy-policy" "$site/tester-guide"
page "Privacy Policy for Meena" "$body" > "$site/privacy-policy/index.html"
page "Meena tester guide" "$guide" > "$site/tester-guide/index.html"
cp store/logo/meena-logo.svg "$site/meena-logo.svg"
cp store/graphics/feature-graphic-1024x500.png "$site/feature-graphic.png"
mkdir -p "$site/screenshots"
cp store/screenshots/01-today.png store/screenshots/03-nearby-map.png store/screenshots/06-apps.png "$site/screenshots/"
repo=https://github.com/manaphassan/Meena-Launcher
page "Meena: a home screen for Android" "$(cat <<EOF
<div class="hero">
  <img src="meena-logo.svg" width="96" height="96" alt="Meena logo: a solid white pixel m on a blue square">
  <div><h1>Meena</h1><p>A home screen for Android. Clean, light, open, fast.</p></div>
</div>
<img class="wide" src="feature-graphic.png" width="1024" height="500" alt="meena: clean, light, open, fast, beside a grid of coloured tiles">
<p>Big type, plenty of space and flat colour, inspired by Windows Phone Metro. Swipe sideways through five pages:</p>
<ul>
  <li><strong>today</strong>: time, weather, favourite apps, your week, prayer times, a nearby map, notes, battery and connected devices</li>
  <li><strong>inbox</strong>: your alerts and chats</li>
  <li><strong>news</strong>: the feeds you choose, with a clean reader</li>
  <li><strong>money</strong>: exchange rates, your own Google Sheet and spending from bank alerts</li>
  <li><strong>apps</strong>: search, a letter index, and sorting by category or last used</li>
</ul>
<p>Every widget shows real information or says what it needs. No ads, no accounts, no tracking.</p>
<div class="shots">
  <img src="screenshots/01-today.png" width="1080" height="1920" alt="Today page with clock, weather, favourite tiles and forecast">
  <img src="screenshots/03-nearby-map.png" width="1080" height="1920" alt="Nearby map with 3 km and 5 km rings, compass and qibla line">
  <img src="screenshots/06-apps.png" width="1080" height="1920" alt="Apps page sorted by category">
</div>
<p>Version 0.1.0 is getting ready for a Google Play closed test. It is not on the Play Store yet.</p>
<h2>Links</h2>
<ul class="links">
  <li><a href="privacy-policy/">Privacy policy</a></li>
  <li><a href="tester-guide/">Tester guide</a></li>
  <li><a href="$repo">Source code on GitHub</a></li>
  <li>Contact: <a href="mailto:harussani@outlook.my">harussani@outlook.my</a></li>
  <li>Like Meena? <a href="https://paypal.me/manaphassan">Buy me a coffee on PayPal</a></li>
</ul>
EOF
)" > "$site/index.html"
# Plain static files: no Jekyll processing.
: > "$site/.nojekyll"

git -C "$site" add -A
if git -C "$site" diff --cached --quiet; then
  echo "privacy policy page already up to date"
else
  git -C "$site" commit -q -m "Update the Meena site"
  echo "committed on gh-pages; push with: git -C $site push"
fi
