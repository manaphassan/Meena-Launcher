#!/usr/bin/env bash
# Publishes docs/privacy-policy.md to GitHub Pages:
#   https://manaphassan.github.io/Meena-Launcher/privacy-policy/
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

page() {
cat <<EOF
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>$1</title>
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

mkdir -p "$site/privacy-policy"
page "Privacy Policy for Meena" "$body" > "$site/privacy-policy/index.html"
page "Meena" '<h1>Meena</h1><p>An Android launcher in the Meena UI style: clean, light, open, fast.</p><p><a href="privacy-policy/">Privacy policy</a></p>' > "$site/index.html"
# Plain static files: no Jekyll processing.
: > "$site/.nojekyll"

git -C "$site" add -A
if git -C "$site" diff --cached --quiet; then
  echo "privacy policy page already up to date"
else
  git -C "$site" commit -q -m "Update the Meena privacy policy"
  echo "committed on gh-pages; push with: git -C $site push"
fi
