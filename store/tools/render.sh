#!/usr/bin/env bash
# Renders an HTML or SVG file to PNG with headless Chrome. Usage: render.sh <input> <output.png> <width> <height>
set -euo pipefail
chrome="/c/Program Files/Google/Chrome/Application/chrome.exe"
in=$(cygpath -w "$(realpath "$1")"); out=$(cygpath -w "$(realpath -m "$2")")
"$chrome" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
  --default-background-color=00000000 --window-size="$3,$4" --screenshot="$out" "file:///${in//\\//}" >/dev/null 2>&1
ls -la "$2"
