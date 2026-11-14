# Meena UI score: phase 10 sign-off

Branch `phase/10-signoff`, 14 Nov 2026. Rubric: section 1 of `docs/meena-ui-fix-plan-v2.md`.
Tags: **[F]** checked in code or by a command, **[S]** not verified yet, **[I]** my reading.

## Result

| # | Area (weight) | Before | Now | Gates passed | Gates failed or not verified |
|---|---|---|---|---|---|
| 1 | Metro structure (1) | 8 | **9** | 5 pages (`pageCount = { 5 }`) [F]; back returns to today (`BackHandler` on `currentPage != 0`) [F]; no `TopAppBar`, `Scaffold` or `FloatingActionButton` anywhere [F] | Cold ≤ 1.2 s and warm ≤ 0.5 s not measured [S]. Peek titles and the swipe-down panel were checked in earlier phases, not re-run here [S] |
| 2 | Typography (1) | 7 | **10** | Selawik bundled in `res/font`, no Google Fonts provider and no `font_certs.xml` [F]; display 42sp light, section 24sp light, body 16sp, every caption style ≥ 12sp (`Type.kt`) [F]; `StringsCaseTest` lowercase, plurals included [F]; 0 bold or semibold in `ui/` [F]; 0 symbol glyphs in code and in `strings.xml` [F] | `bodyMedium` is 14sp. I read it as secondary text, not body, so it doesn't fail the 15–17sp gate [I]. Screenshots not taken [S] |
| 3 | Aesthetic purity (1) | 5 | **10** | `charter-baseline.txt` all 0: circles, borders, cards, elevation, Toasts, filled icons, colour literals, emoji [F]; ripple off everywhere (`MetroIndication`, `indication = null`) [F]; no per-brand colours [F]; every `RoundedCornerShape` is `0.dp` [F] | Three themes (dark, light, paper), not two. Paper was your call after the plan, so it counts as compliant [I]. The news picture blur is a privacy filter for feeds you mark, not a decorative blur [I] |
| 4 | Content focus, AIO goal (2) | 4 | **[S]** | Favourites cap 6 (`FavouriteRules.MAX`) and group tiles [F]; simple Today map preview [F] | The 411 × 914dp first-screen check, the 3-line rule and the 1-tap favourite test all need the phone and testers [S] |
| 5 | Layout discipline (1) | 5 | **8** | Spacing only 0/6/12/24dp (ratchet 0) [F]; touch targets ≥ 48dp on new screens [F]; palette text on every page colour ≥ 4.5:1 in all three themes (`ContrastTest`) [F] | **Failed: accent used as link text on the light theme.** 5 of 10 accents are below 4.5:1 on white, including the default blue at 2.78:1 (table below) [F]. 130% font scale not tested [S] |
| 6 | Data honesty (1) | 7 | **10** | No sample, fake, `RM` amount or "x ago" literals in `main/` (grep audit below) [F]; widgets use `WidgetState`: 9 data repositories (weather, solat, health, news, finance, rates, spending, wikipedia, devices) [F]; the news fallback list is empty (`initialFallbackArticles = emptyList()`) [F] | none |

Weighted: before (8+7+5+4×2+5+7)/7 = **5.7**. Now: areas 4 and 5 can't reach 10 until the open items below are done. Without area 4: (9+10+10+8+10)/5 = **9.4** [I].

## Failed gate: accent as text on the light theme

Measured with `calculateContrastRatio` from `theme/Color.kt` (the accent on the page background, and white text on the accent):

| accent | dark (on black) | light (on white) |
|---|---|---|
| blue (default) | 7.56 | **2.78** |
| crimson | **4.43** | 4.74 |
| mango | 9.07 | **2.31** |
| emerald | 4.64 | 4.53 |
| magenta | **4.17** | 5.04 |
| purple | **3.98** | 5.28 |
| lime | 10.47 | **2.00** |
| teal | 7.40 | **2.84** |
| brown | **3.45** | 6.09 |
| pink | 7.42 | **2.83** |

Paper ignores the accent (black on white, 21:1). The theme page already warns when an accent is hard to read (`theme_accent_hard`). It does not fix the colour.

Proposed fix (not done, needs your yes): keep the accent as chosen for tiles and fills, and give link text a darker or lighter shade of the same hue, picked so it reaches 4.5:1 on the page. `ContrastTest` would then check every swatch in every theme.

## Speed

Not measured. The phone was locked for most of the session, then you were using Meena. Run with the phone unlocked:

```
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
```

The results land in `macrobenchmark/build/outputs/connected_android_test_additional_output/`. Use the median `timeToInitialDisplayMs` for cold and warm.

## Screenshots

Not taken [S]. Still needed: 320, 360 and 411dp; dark, light and paper; 100% and 130% font.

## Changes in this phase

| Commit | Change |
|---|---|
| `667236f` | `StringsCaseTest` now also checks plural items, banned copy words (GEMINI.md rule 8) and symbol glyphs. `CharterLintTest` also counts arrow, cross and tick glyphs, plus `text = "+"`. Two labels lost their `+`: `new event`, `add event` |
| `d11bde9` | Backup rules exclude all app data from cloud backup and device-to-device transfer. On Android 12+, `allowBackup="false"` alone may not stop transfer ([Auto Backup docs](https://developer.android.com/identity/data/autobackup)) |
| `1cddfd1` | Ships the Selawik licence. The [SIL OFL 1.1](https://github.com/microsoft/Selawik) needs the notice with each copy; it now shows with the map licences on the about page |

## Command output

```
./gradlew assembleDebug testDebugUnitTest lint
...
> Task :app:lint
BUILD SUCCESSFUL in 7m 11s
88 actionable tasks: 47 executed, 2 from cache, 39 up-to-date
```

- **Unit tests:** 192, 0 failures (`app/build/test-results/testLiveDebugUnitTest`).
- **Charter ratchet:** every count 0. `CircleShape`, `border`, `Card`, `elevation`, `Toast`, `Icons`, `FontWeight`, `fontSizeBelow12sp`, `ColorHex`, `ColorNamed`, `ColorLiterals`, `emojiAndSymbols`, `nonStandardSpacing`, `uppercaseDisplay` = 0.
- **Lint:** 0 errors, 128 warnings, 2 hints. The biggest warning groups are `UnusedResources` 58, `UseKtx` 29, `PluralsCandidate` 10, `GradleDependency` 9.
  - `ButtonCase` and `Typos` want "OK" in capitals, which the lowercase rule forbids.
  - The version warnings are blocked by GEMINI.md rule 3.

Data audit (no hits apart from image downscaling and a comment):

```
grep -rniE '\b(lorem|dummy|fake|sample|mock)\b' app/src/main/java
grep -rnE '"RM ?[0-9]' app/src/main/java
grep -rnE '"[0-9]+m ago"|"good morning' app/src/main/java
```

## Not verified [S]

1. Startup times, cold and warm.
2. First-screen content at 411 × 914dp, and at 320dp with 130% font.
3. The A/B task test with 5 or more people (plan section 5). This needs testers; the closed test can cover it.
4. TalkBack walk-through of every page.
5. Light and paper themes on the map.
6. The checks still pending on the phone:
   - the qibla buzz
   - the charging speed label
   - the MAE/TNG/Grab/Wallet group tile
   - the uninstall flow, which now goes through app info
