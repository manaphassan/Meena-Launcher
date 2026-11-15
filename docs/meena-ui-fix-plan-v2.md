# Meena UI: review, full-score target, and fix plan (v2)

**Project:** `D:\HaNa_Innovation\meena_launcher` · **Date:** 5 Oct 2026 · **Supersedes:** v1 (same file)
**Read for v2:** `TodayHub`, `StartHub`, `CollapsibleWidget`, `LiveTile`, `CornerNavHub`, `MainActivity`, `UserSettings`, `Color`, `Type`, `Theme`, and pattern scans of `InboxHub`, `NewsHub`, `FinanceHub`, `AppsHub`, `SettingsScreen`.
**Not read:** repositories under `data/**`, `WeatherRepository`, `SettingsRepository`, `MetroDialog`, the bottom of `Type.kt`. Anything depending on them is marked [S].
**Labels:** **[F]** read in code · **[I]** inference · **[S]** speculation, verify first.

**v2 changes:** adds a scoring rubric with a measurable gate per area; adds Phase 0 (automated gates), Phase 6 (theme and type), Phase 7 (other pages), Phase 8 (content-first layer), Phase 9 (data honesty, speed, accessibility), Phase 10 (sign-off). Phases 1–5 keep their numbers.

> **Status, 16 Nov 2026:** phases 0 to 9 done. Phase 10 sign-off scored **9.7 / 10** (`meena-ui-score.md`); the 3-line rule is built (widget length setting, 3 or 5 lines), and the task test for area 4 is still to run (`task-test-plan.md`). See [`roadmap.md`](roadmap.md).

---

## 0. Conclusion

Baseline: **5.7 / 10** (content focus double-weighted; I rounded it to 5.5 in chat). Target: **10 / 10 on the rubric below**, reached in 11 phases, about 15 working days [I].

Two caveats:
1. 10/10 is by this rubric. Code gates prove most of it. Final sign-off needs the on-device test in Phase 10, because "feels fast" and "content first" are not provable from source.
2. Small, uniform favourite tiles with a notification badge count as compliant. That is your decision and it is not the mosaic Metro you excluded earlier.

---

## 1. Rubric: what 10/10 means

Each area is scored 0–10. Gates are pass/fail; an area scores 10 only when every gate passes.

| # | Area (weight) | Now | 10/10 gates | Checked by |
|---|---|---|---|---|
| 1 | Metro structure (1) | 8 | 5 pages; peek titles from measured widths; swipe-down quick panel; back returns to *today*; no app bar or FAB on home pages; cold start ≤ 1.2 s and warm ≤ 0.5 s on a mid-range phone | Checklist + `adb shell am start -W` ×10 |
| 2 | Typography (1) | 7 | Bundled Metro-style font (no network font fetch); page title 42sp light, section 24sp, body 15–17sp, caption ≥ 12sp; all user text lowercase; no bold/semibold; no emoji or symbol glyphs in text | Lint test + `strings.xml` test + screenshots |
| 3 | Aesthetic purity (1) | 5 | Zero: borders, circles, cards, elevation, blur, ripple, filled Material icons, Toasts, per-brand colours; one accent only; dark and light themes only | Lint ratchet at 0 + screenshots |
| 4 | Content focus, AIO goal (2) | 4 | First screen (no scroll, 411×914dp) shows clock, date, a live "next" line, 5 forecast tiles, favourites row, and the first widget's content; 0 "needs setup" rows in the stream; ≤ 3 lines per widget; launching a favourite = 1 tap, 0 scrolls | Screenshot + task test |
| 5 | Layout discipline (1) | 5 | 24dp page margin everywhere; spacing only from 6/12/24dp tokens; touch targets ≥ 48dp; text contrast ≥ 4.5:1 in both themes; no clipping at 130% font scale | Lint ratchet + contrast test + TalkBack/font-scale pass |
| 6 | Data honesty (1) | 7 | No literal data in `data/**`; every widget shows `needsSetup/loading/ready/stale/error/empty`; no sample values outside the `demo` flavour | Unit test + grep audit |

Score = (1 + 2 + 3 + 2×4 + 5 + 6) ÷ 7. Baseline: (8+7+5+8+5+7) ÷ 7 = 5.7.

---

## 2. Findings

### 2.1 Content focus (area 4)
- **C1 [F]** 12 Today widgets are default-on (`defaultWidgetMap`, positions 0–11).
- **C2 [F]** Favourite tiles render last in `TodayHub` (L1174+), after every widget.
- **C3 [F]** Weather shows in the clock widget, the forecast summary, and the brief input. Battery is drawn twice in `device status`. `widget-tasks` is titled "today's events" and duplicates the calendar.
- **C4 [F]** `less/more` is printed on every `CollapsibleWidget`, plus a hairline and an accent-coloured title.
- **C5 [F]** Setup prompts ("allow location", "needs setup", "add your gemini key…") take stream space instead of being summarised once.

### 2.2 Aesthetic purity (area 3)
| Where | Finding [F] |
|---|---|
| `StartHub.kt` | 10 `.border(`, ALL CAPS strings, emoji and symbol glyphs. **Looks dead**: `MainActivity` imports only `TodayHub`. |
| `CornerNavHub.kt` | 5 `CircleShape`, 2 circle borders. No call found in `MainActivity`. |
| `LiveTile.kt` | Flip, 3D tilt, back face. No usage found in either hub. |
| `NewsHub.kt` | 2 `Card(`, 7 bold/semibold, 1 `Icons.*`, 5 font sizes < 12sp |
| `AppsHub.kt` | Own Box+Dialog for properties/uninstall (not `MetroDialog`), 3 bold, 1 `Icons.*`, 2 sizes < 12sp, `uppercase()` for letters |
| `SettingsScreen.kt` | Filled icons (`ArrowBack`, `Close`), 1 `.border(`, 1 `Slider`, 1 `LazyRow` (L252), 1 size < 12sp |
| `MainActivity.kt` | 11 `Toast.makeText` |
| Notification rows | Per-brand colours (WhatsApp green, Gmail red, Telegram blue). Breaks the one-accent rule. |
| `UserSettings` | `acrylicBlurDp`, `borderStyle = "modern"`, `fontFamily` (Inter, Outfit…), `slate` theme: none of these exist in Metro |

### 2.3 Typography and theme (area 2)
- **T1 [F]** Font is Plus Jakarta Sans via the Google Fonts provider, not the charter's Selawik. It needs Play Services and a network fetch, which hurts "fast" on a first launch.
- **T2 [F]** The comment on `MeenaTextMuted` says 6.3:1; `#888888` on black computes to about 5.9:1 [I, my arithmetic]. It still passes 4.5:1.
- **T3 [F]** `MeenaTextDisabled #555555` is about 2.8:1 [I]. Acceptable only for truly disabled items.
- **T4 [F]** Hard-coded colours bypass the theme: `Color.White`/`Gray` ≈ 80 in Settings, 22 in Apps, 12 in Finance, 10 in Main, 6 in News; `Color(0x…)` literals in 6 files. **[I]** the *light* theme cannot render correctly while white literals sit on a white background. Verify on device.
- **T5 [S]** `labelSmall` is used for most captions. Its size is in the unread part of `Type.kt`; the charter needs ≥ 12sp.

### 2.4 Layout (area 5)
- **L1 [F]** The `LazyColumn` pads 20dp; the pinned block adds 24dp on top (tiles at 44dp).
- **L2 [F]** Spacing literals include 4, 6, 8, 10, 12, 16, 20, 24dp. The charter scale is 6/12/24.
- **L3 [F]** The notification row has a 3dp accent bar and a 22dp dismiss target (below 48dp).

### 2.5 Favourites and forecast
- **F1 [F]** `AppsHub` already pins apps; `TodayHub` ignores `pinnedAppSizes` (`1x1/2x2/4x2` stored, never rendered). Defaults are Gmail and Telegram; an uninstalled app shows its raw package name.
- **W1 [F]** The forecast is a collapsed vertical list. `weatherData.daily` already has day, min, max, condition and rain probability.
- **W2 [S]** `WeatherRepository` was not read. Check whether it keeps the raw weather code or only the `condition` string; it decides how the glyph map is keyed.

### 2.6 Already right [F]
Five-page pager, measured peek-title parallax, `metroFeatherIn` with reduced-motion support, dotted progress, scale-and-tilt press feedback, 0dp `Shapes`, `WidgetState` machine, freshness captions, per-widget enable/position config, a `calculateContrastRatio` helper, and string resources for errors.

---

## 3. Add / remove / merge

**Remove:** `StartHub`, `CornerNavHub`, `LiveTile` (after a repo-wide grep); `less/more` and per-widget hairlines; `pinnedAppSizes`; forecast settings `forecast_days`, `show_rain_prob`, `show_details`; default favourites; the `widget-health-device` widget from Today; `acrylicBlurDp`, `borderStyle`, `fontFamily` settings; the `slate` theme; per-brand notification colours; the Google Fonts provider and `font_certs.xml`.

**Merge:** `widget-tasks` into agenda; `health-telemetry` + `health-aqi` into `health` (air quality moves to the weather detail page); battery/memory/storage into one `phone` line.

**Default off:** month calendar, notes, health, radar, phone.

**Add:** forecast tile row; favourite tiles with a live unread badge; weather detail sub-page; `widget-favourites` config entry; live "next" line; setup digest; `MeenaSpacing` tokens; `MetroMessage` (replaces Toast); charter lint test; contrast test; strings case test.

**Resulting Today order:** clock + date → next line → forecast tiles → favourites → solat → agenda → (brief, only once a Gemini key exists) → setup digest.

---

## 4. Fix plan

Rules for every phase: work on a phase branch; run `./gradlew assembleDebug testDebugUnitTest lint`; paste the last lines of output; take screenshots at 320, 360 and 411dp. Follow `GEMINI.md`, including "stop and ask" for new dependencies, permissions and version bumps.

### Phase 0: Gates first (0.5 day) → enables every later gate
- Add `CharterLintTest` (unit test, no new dependency) scanning `app/src/main/java/**/ui/**` for: `CircleShape`, `.border(`, `Card(`, `Surface(` with elevation, `Toast`, `Icons.` (Default/Filled/Rounded/AutoMirrored.Filled), `FontWeight.Bold|SemiBold|ExtraBold`, `fontSize = N.sp` with N < 12, `Color(0x` and `Color.White|Gray|Black` outside `theme/`, emoji and symbol characters (`• ✕ ▲ ▼ → ⚡` and the emoji ranges), numeric `padding(`/`spacedBy(` literals not in {0, 6, 12, 24}.
- **Ratchet:** store today's counts in `charter-baseline.txt`; the test fails if any count rises, and each phase lowers it. Phase 10 requires all counts at 0 (ticker `uppercase()` stays on an allowlist).
- Add `ContrastTest` using the existing `calculateContrastRatio` over every text/background token pair in both themes (≥ 4.5:1, except `MeenaTextDisabled`).
- Add `StringsCaseTest`: every `strings.xml` value is lowercase except an allowlist for placeholders and proper nouns.
- Acceptance: tests run and pass at baseline counts; baseline numbers recorded.

### Phase 1: Delete dead code (0.5 day) → area 3
- Repo-wide grep for `StartHub`, `CornerNavHub`, `LiveTile`, `ForecastCard`. If unreferenced, delete them and their imports.
- Acceptance: build passes, no behaviour change, lint counts drop (~900 lines gone).

### Phase 2: Calm the chrome (1 day) → areas 3, 4, 5
- `CollapsibleWidget`: remove `less/more` and the hairline. Title is plain text in the primary text colour (not accent). Tap title to collapse; persist the state in `WidgetConfig.isCollapsed` [S: confirm it is not local state only].
- Page margin 24dp in every hub; remove the extra pinned-block padding.
- Acceptance: no widget prints `less`/`more`; every Today item starts at x = 24dp.

### Phase 3: Merge and default-off (1 day) → area 4
- Apply the merges and default-off list from §3. Keep `migrate()`.
- Acceptance: a fresh install shows the five blocks in §3's order.

### Phase 4: Forecast tile row (1.5 days) → areas 4, 5
- One `Row` of exactly 5 tiles, `weight(1f)`, 6dp gap, square, accent fill, white text. ~58dp wide at 360dp.
- Content: 3-letter lowercase day, thin-stroke glyph (clear, partly cloudy, cloudy, rain, storm, fog), high temperature, low temperature smaller. Today's tile at full accent; others at a lower-opacity accent.
- Tap opens the weather detail sub-page (app bar allowed there): hourly, rain %, humidity, wind, air quality, "updated x ago · provider".
- States: `NeedsSetup` → one text row; `Loading` → dotted progress; `Error` → "couldn't reach the weather. tap to retry"; `Empty` → hidden. No placeholder tiles, ever.
- Glyph key: weather code if the repository keeps it, else the condition string [S: W2]. TalkBack label per tile, e.g. "friday, 30 degrees, rain".
- Acceptance: 5 tiles in one row at 320/360/411dp and 130% font scale; °F works; tiles ≥ 48dp.

### Phase 5: Favourite tiles (1.5 days) → areas 4, 5
- `FavouriteTiles` under the forecast. 4 per row, square, max 8, accent fill, white monochrome icon (colour filter on the app icon; look setting, default on), lowercase 12sp label bottom-left. Trailing "+" opens the apps page while fewer than 8.
- **Live part:** unread count badge from `NotificationRepository.notificationsFlow` grouped by `packageName`; hidden at 0; no new permission.
- Long-press → flat lowercase menu: `move`, `remove`. `AppsHub` wording: "add to favourites" / "remove from favourites".
- Skip uninstalled packages. `pinnedAppIds` default → empty; delete `pinnedAppSizes`; add `widget-favourites` (position 2, max 8). Confirm saved settings still load after the field removal [S: check `ignoreUnknownKeys` in `SettingsRepository`].
- Acceptance: 0 favourites → one "add a favourite" row; 8 → two tidy rows; badge follows real notifications.

### Phase 6: Theme and type (2 days) → areas 2, 3, 5
- **Font:** bundle a Metro-style font in `res/font` (charter names Selawik; check its licence [S]; if unusable, bundle another OFL font). Delete the Google Fonts provider, `font_certs.xml` and the `fontFamily` setting. This is an asset, not a dependency; report the size added. Keep `fontScalePercent`.
- **Scale:** page title 42sp light, section 24sp, body 15–17sp, caption ≥ 12sp. Confirm `labelSmall` ≥ 12sp [S: T5]. Replace bold/semibold with Light/Normal/Medium.
- **Accent:** only for tiles, today marker, progress, links, and the key word of a page title. Remove accent from widget titles and the per-brand notification colours.
- **Themes:** dark and light only. Remove `slate`, `acrylicBlurDp` and `borderStyle`.
- **Tokens:** replace every `Color.White/Gray/Black` and `Color(0x…)` in `ui/` with `MaterialTheme.colorScheme.*` or `MeenaText*`; keep named status tokens (`MeenaProfitGreen`, `MeenaLossRed`) in `Color.kt`.
- **Spacing:** add `MeenaSpacing { s = 6.dp, m = 12.dp, l = 24.dp }` and snap all literals to it.
- Acceptance: `ContrastTest` passes in both themes; light theme screenshots show no invisible text; lint counts for color, weight, size and spacing drop to 0.

### Phase 7: Other pages (3 days) → areas 2, 3, 5
- **InboxHub:** drop brand colours and the "all caught up" box (empty → nothing); 1 bold; dismiss targets ≥ 48dp.
- **NewsHub:** replace 2 `Card(` with plain rows; fix bold, sub-12sp sizes, the `Icons.*` use and colour literals.
- **FinanceHub:** colour literals and `Color.White` → tokens; check the `Dialog` against `MetroDialog` and the `indication` use [S].
- **AppsHub:** use `MetroDialog` for properties/uninstall; lowercase letter headers (uppercase only for matching logic); fix bold, sub-12sp sizes, `Icons.*`, colour literals, 2 Toasts.
- **SettingsScreen:** replace filled icons with thin-stroke vector drawables (check the icon set licence [S]); remove the `.border(`; confirm the `Slider` is on a sub-page and not in the pivot, and that the `LazyRow` at L252 is the pivot header rather than content [S]; use `MetroDialog` for the 3 dialogs; fix the sub-12sp size and `Color.Gray/White` (≈ 80).
- **MainActivity:** replace 11 `Toast` calls with `MetroMessage`: one lowercase line, bottom-left, no box, 3 s, messages from `strings.xml`.
- Acceptance: lint baseline file shows 0 for all `ui/` patterns.

### Phase 8: Content-first layer (1.5 days) → area 4
- **Next line:** under the clock, one live line: the sooner of the next calendar event or next prayer ("asr in 42m", "standup 10:30"). Hidden when neither source has data. Never a sample.
- **Setup digest:** widgets in `needsSetup` leave the stream. One line at the bottom of Today ("3 things to set up") opens a list in settings. Individual setup rows stay inside settings → widgets.
- **Three-line rule:** `maxLinesOrItems` default 5 → 3; detail lives behind the title tap.
- **Inbox:** one line per notification (app, title), max 6, grouped by app.
- Acceptance: on a fresh install with location and calendar granted, the first screen at 411×914dp meets the area 4 gates; with nothing granted, Today is clock, date, favourites and the digest only.

### Phase 9: Data honesty, speed, accessibility (1.5 days) → areas 1, 5, 6
- **Data:** audit `data/**` against `meena_ui_build_plan.md` §3.1: no literal values; each repository returns `NeedsSetup`/`Empty` with no source; unit test per repository. Anything in the `demo` flavour stays out of `live` [S: repositories unread].
- **Speed:** measure `adb shell am start -W` ×10 cold and warm; gate 1.2 s and 0.5 s. Stop the 1 s clock/telemetry loops when the page or screen is not visible (`repeatOnLifecycle`) [I]. A Macrobenchmark module needs a new dependency, so ask first; the `am start` gate needs none.
- **Accessibility:** TalkBack labels on tiles, targets ≥ 48dp, 130% font scale without clipping, reduced motion honoured.
- Acceptance: numbers recorded in the report; no gate failed.

### Phase 10: Sign-off (1 day)
- Run lint ratchet (all 0), `ContrastTest`, `StringsCaseTest`, `assembleDebug testDebugUnitTest lint`.
- On-device A/B and task test (§5). Re-score with the rubric. Write `docs/meena-ui-score.md` with before/after, screenshots and command output.
- If any gate fails, the area stays below 10 and the score is reported as is.

**Effort [I]:** ≈ 15 working days (0.5 + 0.5 + 1 + 1 + 1.5 + 1.5 + 2 + 3 + 1.5 + 1.5 + 1).

**Projected score [I]:** after Phase 3 ≈ 7.0 · after Phase 5 ≈ 7.8 · after Phase 7 ≈ 9.0 · after Phase 9 ≈ 9.7 · after Phase 10 sign-off = 10 only if every gate passes.

---

## 5. On-device test (Phase 10)

1. Debug-only `layoutVariant = A | B` switch (not in release).
2. 5+ people, same mid-range phone and installed apps. Tasks: open a favourite; find tomorrow's weather; find the next prayer; turn one widget off; find the 5th day's forecast.
3. Record time, taps, scrolls and errors per task, plus the number of visible elements on the first screen (from a screenshot).
4. **B passes if:** median time to launch a favourite is lower, scrolls to reach it are 0, first-screen element count is lower, no task has more errors than A.
5. Repeat the first-screen check at 320dp and 130% font scale.

---

## 6. Open items (defaults apply if you don't answer)

1. Delete `StartHub`, `CornerNavHub`, `LiveTile` outright (git keeps history)? **Default: yes.**
2. Remove the `slate` theme, font picker, blur and "modern border" settings? **Default: yes.**
3. Bundled font: Selawik if its licence allows, else another OFL font? **Default: yes.**
4. Favourite icons monochrome by default, cap of 8? **Default: yes.**
5. New defaults for fresh installs only (app unpublished)? **Default: yes.**
6. Replace Toasts with `MetroMessage`? **Default: yes.**
