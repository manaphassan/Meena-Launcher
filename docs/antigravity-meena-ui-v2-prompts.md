# Meena UI v2: Antigravity instruction pack

**For:** Google Antigravity (Gemini) working in `D:\HaNa_Innovation\meena_launcher`
**Source plan:** `meena-ui-ab-report-and-fix-plan.md` (v2). Copy it to `docs/meena-ui-fix-plan-v2.md` before starting.
**Decisions already made by the owner (do not reopen):** delete `StartHub`, `CornerNavHub`, `LiveTile` if unreferenced; remove `slate` theme, font picker, blur and "modern border" settings; bundle a font (Selawik if the licence allows, else another OFL font); favourite icons monochrome by default, cap 8; new defaults for fresh installs only; replace Toasts with `MetroMessage`.

Labels: **[F]** fact from code · **[I]** inference · **[S]** unverified, check before acting.
[S] Antigravity's rules-file conventions were not checked. If it does not read `GEMINI.md` automatically, paste section A at the start of each session.

---

## A. Session preamble (paste once per session)

```
You are the lead Android engineer for Meena (Kotlin, Jetpack Compose) at D:\HaNa_Innovation\meena_launcher.
Read GEMINI.md first. Then read docs/meena-ui-fix-plan-v2.md.

Precedence: 1) my latest message, 2) docs/meena-ui-fix-plan-v2.md (v2 wins over docs/meena-ui-fix-plan.md where they conflict on UI), 3) GEMINI.md, 4) existing code.
If two of these conflict, stop and ask. Do not pick silently.

Goal: Meena UI reaches 10/10 on the rubric in section 1 of the v2 plan. Content first, like AIO Launcher. Strict Metro: flat, square, lowercase, one accent, no borders, no circles, no cards, no Toasts.

Rules for every task:
- Work only on the phase I name. One phase per branch: phase/NN-short-name. Small commits.
- Restate the goal and acceptance checks in 3 lines before changing anything.
- Read only the files you need. No drive-by refactors, renames or formatting sweeps.
- Never fabricate data in live code. No sample values, no hardcoded weather, prices or times.
- Do not add dependencies, permissions or bump versions without asking. List name, version, licence, size impact, reason.
- Do not run destructive commands (rm -rf, git reset --hard, git clean, force-push) without asking.
- Never say something builds, passes or works unless you ran it. Show the command and the last 15 lines of output. If you could not run it, write "not run" and why.
- Label claims [F] fact from code, [I] inference, [S] unverified.
- Finish with the report format in section C, then stop and wait for me.
```

---

## B. Phase prompts (paste one at a time, in order)

### Phase 0: gates first
```
Phase 0 of docs/meena-ui-fix-plan-v2.md: automated charter gates. Branch: phase/00-gates.

Add three unit tests under app/src/test/ with no new dependencies:
1. CharterLintTest scans app/src/main/java/**/ui/** and counts: CircleShape; .border(; Card(; Surface( with elevation; Toast; Icons. (Default, Filled, Rounded, AutoMirrored.Filled); FontWeight.Bold/SemiBold/ExtraBold; fontSize below 12.sp; Color(0x and Color.White/Gray/Black outside theme/; emoji and the symbols • ✕ ▲ ▼ → ⚡; numeric padding( or spacedBy( literals not in {0, 6, 12, 24}.
   Store the current counts in app/charter-baseline.txt. The test fails if any count rises above its baseline. Allowlist: uppercase() used only for matching logic.
2. ContrastTest uses the existing calculateContrastRatio in theme/Color.kt over every text/background token pair in both themes. Require at least 4.5:1, except MeenaTextDisabled.
3. StringsCaseTest fails if any value in res/values/strings.xml has an uppercase letter, with an allowlist file for proper nouns and placeholders.

Acceptance: ./gradlew testDebugUnitTest passes at baseline. Report the baseline counts and any ContrastTest failure (do not fix them in this phase).
```

### Phase 1: delete dead code
```
Phase 1: delete dead code. Branch: phase/01-dead-code.

1. Search the whole repo for references to StartHub, CornerNavHub, LiveTile and ForecastCard (code, tests, docs).
2. Report every reference you find. If a file is unreferenced outside itself, delete it and any imports. If something references it, stop and ask.
3. Do not touch anything else.

Acceptance: ./gradlew assembleDebug testDebugUnitTest passes; charter-baseline.txt lowered to the new counts; no behaviour change.
```

### Phase 2: calm the chrome
```
Phase 2: calm the chrome. Branch: phase/02-chrome. Files: ui/components/CollapsibleWidget.kt, ui/hubs/*.kt (margins only).

- CollapsibleWidget: remove the "less"/"more" text and the drawBehind hairline. The title is plain lowercase text in the primary text colour (not the accent). Tapping the title collapses or expands. Persist the state in WidgetConfig.isCollapsed. [S] First check whether it is local state only; if so, wire it to settings.
- Set a 24dp page margin in every hub. Remove the extra 24dp padding on the pinned block in TodayHub.
- Do not change widget content.

Acceptance: no widget prints less/more; every Today item starts at x = 24dp; screenshots at 360dp; charter-baseline.txt lowered.
```

### Phase 3: merge and default-off
```
Phase 3: merge and default-off. Branch: phase/03-merge. Files: data/model/UserSettings.kt, ui/hubs/TodayHub.kt.

In defaultWidgetMap:
- Default off (isEnabled = false): widget-calendar-month, widget-notes, widget-health-device, widget-health-telemetry, widget-health-aqi, widget-map-radar, widget-device-telemetry.
- Merge widget-tasks into the agenda widget: events first, then tasks. Title "agenda". Remove the duplicate "today's events" title.
- Merge health-telemetry and health-aqi into one "health" widget. Move air quality to the weather detail page (Phase 4 creates it; for now keep the aqi code reachable).
- Remove widget-health-device from Today. It is a setup step that belongs in settings -> accounts -> health.
- Keep migrate(). New defaults are for fresh installs only.

Acceptance: a fresh install (clear app data) shows, in order: clock + date, forecast, solat, agenda. Unit test for defaultWidgetMap order and enabled flags.
```

### Phase 4: forecast tile row
```
Phase 4: 5-day forecast as tiles in one row. Branch: phase/04-forecast. Files: ui/hubs/TodayHub.kt, new ui/components/ForecastTiles.kt, new weather detail sub-page.

Read data/system/WeatherRepository.kt first. [S] Check whether it keeps the raw weather code or only a condition string, and key the glyph map on whichever exists.

Build ForecastTiles:
- One Row of exactly 5 tiles, Modifier.weight(1f), 6dp gap, square corners, accent fill, white text. About 58dp wide at 360dp.
- Each tile: 3-letter lowercase day, thin-stroke single-colour glyph (clear, partly cloudy, cloudy, rain, storm, fog) as vector drawables, high temperature, low temperature smaller. Min text 12sp. Today's tile at full accent; the others at lower-opacity accent.
- Respect the temperature unit setting (c/f).
- TalkBack contentDescription per tile, e.g. "friday, 30 degrees, rain".
- Tapping the row opens a weather detail sub-page (an app bar is allowed there): hourly, rain %, humidity, wind, air quality, "updated x ago - provider".
- States from WidgetState: NeedsSetup -> one text row "allow location"; Loading -> MetroDottedProgress; Error -> "couldn't reach the weather. tap to retry"; Empty -> hide the block. Never render placeholder tiles.
- Remove the old vertical forecast list and the settings forecast_days, show_rain_prob, show_details.

Acceptance: 5 tiles in one row at 320, 360 and 411dp and at 130% font scale; both units work; touch targets at least 48dp; unit test for the day/temperature/glyph mapping. Screenshots attached.
```

### Phase 5: favourite tiles
```
Phase 5: favourite tiles. Branch: phase/05-favourites. Files: ui/hubs/TodayHub.kt, new ui/components/FavouriteTiles.kt, ui/hubs/AppsHub.kt (wording), data/model/UserSettings.kt, MainActivity.kt (pin handlers).

- Under the forecast, show small square tiles only: 4 per row, max 8, accent fill, white monochrome app icon (colour filter; add a look setting, default on), lowercase 12sp label bottom-left. A trailing "+" tile opens the apps page while fewer than 8 are pinned.
- Live part: an unread count badge per tile from NotificationRepository.notificationsFlow grouped by packageName. Hide it at 0. Never a fake number. No new permission.
- Long-press a tile: a flat lowercase menu with "move" and "remove".
- AppsHub wording: "add to favourites" / "remove from favourites".
- Skip tiles whose package is not installed; never show a raw package name.
- UserSettings: pinnedAppIds default = emptyList(). Delete pinnedAppSizes and all size UI. Add "widget-favourites" to defaultWidgetMap (position 2, maxLinesOrItems = 8).
- [S] Check SettingsRepository for ignoreUnknownKeys so saved settings that still contain pinnedAppSizes load.
- Zero favourites: show one "add a favourite" row and nothing else.

Acceptance: 0, 3, 8 favourites screenshots; badge appears and clears with a real notification; tests for the grouping logic and the 8 cap.
```

### Phase 6: theme and type
```
Phase 6: theme and type. Branch: phase/06-theme. Files: theme/*.kt, res/font, res/values/font_certs.xml, ui/** (colour literals).

1. Font: bundle a Metro-style font in res/font. The charter names Selawik; [S] check its licence in the official source and cite the URL. If it is unusable, propose one OFL font and ask before adding. Report the size added. Delete the Google Fonts provider, font_certs.xml and the fontFamily setting. Keep fontScalePercent.
2. Scale: page title 42sp light, section 24sp, body 15-17sp, captions at least 12sp. [S] Read the rest of Type.kt and fix labelSmall if it is under 12sp. Replace Bold/SemiBold with Light/Normal/Medium.
3. Accent only for tiles, today marker, progress, links, and the key word of a page title. Remove accent from widget titles and the per-brand notification colours (WhatsApp, Gmail, Telegram).
4. Themes: dark and light only. Remove "slate", acrylicBlurDp and borderStyle (settings, UI and code).
5. Replace every Color.White/Gray/Black and Color(0x...) in ui/ with MaterialTheme.colorScheme.* or MeenaText* tokens. Keep MeenaProfitGreen and MeenaLossRed as named tokens.
6. Add MeenaSpacing(s = 6.dp, m = 12.dp, l = 24.dp) and snap spacing literals to it.

Acceptance: ContrastTest passes in both themes; light-theme screenshots show no invisible text; charter-baseline.txt shows 0 for colour, weight, size and spacing patterns.
```

### Phase 7: other pages (run as 7a to 7f, one prompt each)
```
Phase 7a-7f: bring each remaining file to charter. One sub-phase per prompt, branch phase/07x-name. Do only the file I name.

7a InboxHub.kt: remove the "all caught up" box (empty -> nothing); remove brand colours; dismiss targets at least 48dp.
7b NewsHub.kt: replace the 2 Card( with plain rows; remove bold, sub-12sp sizes, Icons.* and colour literals.
7c FinanceHub.kt: colour literals -> tokens; compare its Dialog and indication use with MetroDialog and MetroIndication and use the shared ones.
7d AppsHub.kt: replace the custom properties/uninstall Box+Dialog with MetroDialog (title, one short question, yes/no side by side); lowercase letter headers (uppercase only for matching); remove bold, sub-12sp sizes, Icons.*, colour literals, the 2 Toasts.
7e SettingsScreen.kt: replace filled icons (ArrowBack, Close) with thin-stroke vector drawables [S: check the icon set licence]; remove the .border(; [S] confirm the Slider (L~1671) is on a sub-page and not in the pivot, and that the LazyRow (L~252) is the pivot header and not content, else move them; use MetroDialog for the 3 dialogs; fix the sub-12sp size; replace Color.Gray/White (about 80).
7f MainActivity.kt + new ui/components/MetroMessage.kt: replace all 11 Toast.makeText with MetroMessage (one lowercase line, bottom-left, no box, 3 s, text from strings.xml). Check that failure messages do not say "opened".

Acceptance for each: build and tests pass; its patterns drop to 0 in charter-baseline.txt; before/after screenshots.
```

### Phase 8: content-first layer
```
Phase 8: content-first layer. Branch: phase/08-content-first. Files: ui/hubs/TodayHub.kt, ui/hubs/InboxHub.kt, settings widgets list.

- Next line under the clock: the sooner of the next calendar event (DeviceCalendarRepository) or the next prayer (JakimSolatRepository), e.g. "asr in 42m". Hidden if neither source has data. Never a sample.
- Setup digest: widgets in NeedsSetup leave the stream. One line at the bottom of Today ("3 things to set up") opens a list in settings. The individual setup rows live only in settings -> widgets.
- Default maxLinesOrItems 5 -> 3 for collapsed widgets. Detail is behind the title tap.
- Inbox: one line per notification (app, title), max 6, grouped by app.

Acceptance: on a fresh install with location and calendar granted, the first screen at 411x914dp (no scroll) shows clock, date, next line, 5 forecast tiles, favourites, and the first widget's content. With nothing granted, Today shows clock, date, favourites and the digest only. Screenshots for both.
```

### Phase 9: data honesty, speed, accessibility
```
Phase 9: data honesty, speed, accessibility. Branch: phase/09-quality.

Data: audit app/src/main/java/**/data/** against docs/meena_ui_build_plan.md section 3.1. Find literal values (prices, weather, steps, times, device names, balances). List each with file:line. Fix: no source -> NeedsSetup or Empty. Add one unit test per repository that returns no value without a source or cache hit. Anything demo-only must be in the demo source set.

Speed: measure `adb shell am start -W` 10 times cold and 10 times warm and report the median. Gates: cold <= 1.2 s, warm <= 0.5 s. [I] Stop the 1-second clock/telemetry loops when the screen is not visible (repeatOnLifecycle). Do not add Macrobenchmark (new dependency) without asking.

Accessibility: contentDescription on tiles and icons; touch targets at least 48dp; no clipping at 130% font scale; reduced motion honoured.

Acceptance: a table of the numbers and any gate that failed.
```

### Phase 10: sign-off
```
Phase 10: sign-off. Branch: phase/10-signoff. Do not change feature code unless a gate fails.

1. Run ./gradlew assembleDebug testDebugUnitTest lint. Show the last lines.
2. Confirm every count in charter-baseline.txt is 0 (allowlist excepted), ContrastTest and StringsCaseTest pass.
3. Re-score the six areas with the rubric in section 1 of the v2 plan. Give each area a score and the gate that passed or failed. A single failed gate means that area is below 10.
4. Write docs/meena-ui-score.md with before/after scores, screenshots (320, 360, 411dp; dark and light; 130% font), speed numbers and command output.
5. List anything you could not verify as [S].
```

---

## C. Report format (every task)

```
goal (1 line)
branch + commits
files changed (path: what)
commands run + last 15 lines of output (or "not run: reason")
charter-baseline.txt: before -> after
acceptance checks: pass / fail / not run, each
screenshots attached (sizes)
[S] items and questions for the owner
stop
```

## D. Order and effort [I]

0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7a-f -> 8 -> 9 -> 10. About 15 working days. Phases 0-3 are safe to merge first. If a phase fails a gate, fix it before starting the next.
