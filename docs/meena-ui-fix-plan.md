# Meena UI: Fix & Build Plan

**Project:** Meena (`D:\HaNa_Innovation\meena_launcher`) · **App name:** Meena · **Design language:** Meena UI · **Plan date:** 2 Oct 2026
**Inspired by:** Metro UI (Windows Phone) + AIO Launcher · **Goal:** Play Store release, every widget live and configurable.

Labels: **[F]** fact from the code or a cited source · **[I]** inference · **[S]** speculation / verify before relying on it.

> **Status, 16 Nov 2026:** version 0.1.0 is built and published as a GitHub release. M1, M2 and the Health Connect part of M3 are done in code; the Gmail API part of M3 is dropped. Play testing waits for the developer account. Current milestones, next steps and open decisions: [`roadmap.md`](roadmap.md). What shipped: [`CHANGELOG.md`](../CHANGELOG.md).

---

## 0. Conclusion

The current app has the right skeleton (pager, jump list, accent palette) but fails the new brief in four places: data is partly hardcoded, widgets have no per-widget settings, the Settings screen covers none of the 11 required areas, and the copy is technical and inconsistent. This plan fixes all four and aligns the UI to the Meena UI charter.

Scope grew a lot versus the earlier plan. **Estimate: ~55–70 focused working days solo (≈11–14 weeks)** [I], shipped in three milestones so a Play-ready build lands early:

| Milestone | Contains | Gate | Status (16 Nov 2026) |
|---|---|---|---|
| **M1: closed-test build** | Foundation, widget framework, full Settings shell, Meena UI skin, solat, weather, calendar, news, apps, alerts, phone, copy, compliance | Play closed test | Done in code; closed test waits for the Play account |
| **M2: money** | Finance (portfolio, rates, quotes, Sheets bridge). Gemini brief dropped (D13) | Production rollout | Done; adds markets chart, ticker search, rates table and kijang emas (D15, D16) |
| **M3: health + mail API** | Health Connect, AQI, Gmail API unread count | Staged update | Health Connect and AQI done; Gmail API dropped (D17) |

Three things you asked for **cannot be built literally**; the plan substitutes (details in §1):
1. **Google Fit** is deprecated. Use **Health Connect**.
2. **Google Finance** has no public API. Use a **Google Sheets bridge** (your own `GOOGLEFINANCE()` sheet) plus a licensed quote provider.
3. **"Realtime"** cannot mean tick-level for every source. The plan defines a freshness target per source (§3.4) and shows "updated x ago" on every widget.

---

## 1. Decisions & deviations (read first)

| # | Decision | Default in this plan | Why |
|---|---|---|---|
| D1 | Google Fit integration | Replace with **Health Connect** | Google Fit APIs are deprecated and Google tells developers to migrate to Health Connect; Fitbit Web API also shuts down in Sept 2026 [F, cited in §9] |
| D2 | Google Finance integration | **Sheets bridge is the primary free path** (user picks a sheet containing `GOOGLEFINANCE()` formulas, which covers Bursa Malaysia [I: verify]); paid quote API only optional | No official Google Finance API exists [I: verify]; `GOOGLEFINANCE` is a Sheets function and its data can be delayed |
| D3 | Home screen chrome | **No app bar and no floating button on home pages**; app bar only on sub-pages (settings, detail) | The Windows Phone guidelines deck says app bars and floating buttons are not allowed on panorama pages. **This reverses my earlier "replace FAB with App Bar" advice.** |
| D4 | Page count | **5 pages** (confirmed): today · inbox · news · money · apps (health widgets live on *today*) | Charter: "fierce reduction". Was 6 |
| D5 | Calendar integration | Read **device calendars via CalendarContract**; Google API optional later | Needs only the READ_CALENDAR permission, no OAuth verification; Google-synced calendars already appear there [I] |
| D6 | Gmail integration | M1: unread/senders from Gmail **notifications** (already permitted). M3: Gmail API **unread count only** | Most Gmail scopes are "restricted" and trigger a security assessment [S: verify]; `gmail.labels` may be non-sensitive [S] |
| D7 | Store name | **Meena** (decided). "Meena UI" names the design language (About, settings header). Discovery comes from the short description and Personalization category, not title keywords. | Short, matches the charter; search Play and MyIPO for collisions before upload |
| D8 | Package name | `my.hana.meena` (decided; permanent after first upload) | `com.example.*` is rejected by Play; avoid product words and employer/client namespaces |
| D9 | Demo data | Only in a `demo` build flavor, never in `release` | Meets "no hardcoded data" while keeping screenshots possible |
| D10 | Language | **English only** (no `ms` strings, no language setting) | Your decision; cuts localisation work |
| D11 | APIs | **Free tiers only** (details in §5) | Your decision; some sources will be delayed or lack coverage |
| D12 | Play account | **Personal** account | The 12-tester / 14-day closed test applies if the account was created after 13 Nov 2023 |
| D13 | Gemini brief | **Dropped** (owner, 12 Nov 2026). Target audience stays 13+. | [Gemini API terms](https://ai.google.dev/gemini-api/terms): 18+ only, "not for consumer use", paid-only in EEA/CH/UK, free-tier input used to improve products and may be human-reviewed. Default model `gemini-1.5-flash` and fallback `gemini-2.0-flash` are shut down ([deprecations](https://ai.google.dev/gemini-api/docs/deprecations)) |
| D14 | AIO Launcher as reference | **Copy content, not layout** (owner, 12 Nov 2026). Meena shows the same kinds of information AIO does (see `docs/aio-reference.md`, content gap list below); every screen keeps the Metro/Zune layout, type, spacing and rules in §2. | Owner decision. Taking ideas for what to show is fine; copying the look risks Play impersonation issues [S: verify policy before release] |

---

## 2. Meena UI charter

### 2.1 Principles → rules → how we check

| Principle | Concrete rule | Check |
|---|---|---|
| **Clean** | No cards, borders, or boxes around content. Hierarchy comes from type size, weight, and spacing only. | Lint list of banned modifiers (`.border`, `Card`, `Surface` with elevation) in `ui/` |
| **Light** | No decoration. One accent colour. AAB under ~15 MB. Few dependencies. | Size budget in CI; dependency review per PR |
| **Open** | 24dp page margins, spacing scale 6/12/24dp, generous line height, nothing crowded. | Screenshot review against spacing tokens |
| **Fast** | Feels instant: warm start ≤ 500 ms, cold ≤ 1.2 s to first usable frame on a mid-range phone; scroll jank < 1%; no blocking network on the main thread; cached data shown first, refreshed after. | Macrobenchmark + baseline profile |
| **Focus on primary tasks** | Home answers three questions fast: what's now, what's next, what needs me. Launching apps is never more than one gesture away. | Task walk-through test (§8) |
| **Do a lot with very little** | One widget = one idea, 1–3 lines, drill-in for detail. Max 5 pages. | Widget spec rule: ≤ 3 lines collapsed |
| **Fierce reduction** | Every element must earn its place; remove any label that repeats its title (e.g. "battery: 80%" → "80%"). | Copy review (§6) |
| **Delightful whitespace** | Prefer space over dividers. No hairlines between widgets unless needed for a11y. | Design review |
| **Full-bleed canvas** | Edge-to-edge, transparent system bars, wallpaper or black behind everything, content may run off the right edge to hint at the next page. | Visual check on gesture and 3-button nav devices |

### 2.2 Rules adopted from the Windows Phone UI guidelines deck

Source: *Windows Phone UI Design Guidelines* (SlideShare, Neelima Salvi, 79 slides). [F: fetched]

| Deck rule | Meena UI application |
|---|---|
| Use the 10 theme accents in dark and light: blue, red, mango, green, magenta, purple, lime, teal, brown, pink | Accent picker shows these 10 + custom hex. Default = blue (`#00A4EF`-family). |
| Segoe WP; text lowercase, exception for section titles; keep case consistent app-wide | Selawik (open Segoe-metric font; verify licence). **Everything lowercase**, including titles and buttons. |
| Headings left-aligned, no backgrounds, borders, underlines or decoration | Page and section titles are plain text only |
| 24px side margins, consistent element alignment and spacing, "don't break the grid" | 24dp margins; all content snaps to the same left edge |
| Buttons: no "selected" state | No highlighted/selected buttons. Choices use full-screen **list pickers** and **toggle switches**, never segmented buttons |
| Pivots need ≥ 2 pages, hold homogeneous info (like tabs), never used for wizards or master-detail | Used only where content is truly tab-like (e.g. calendar: agenda / month) |
| **No toggle switches, sliders, maps, browsers, or horizontal scrollers inside pivots** | Settings is a pivot of *link lists only*; every toggle and slider lives on a sub-page |
| Panorama panes scroll vertically *or* horizontally, not both; no floating buttons; **no app bars on panorama** | Home pages scroll vertically only; no FAB or app bar on home (D3) |
| Dialogs: title, short question, **yes / no side by side at the bottom**, lowercase | One `MetroDialog` component for every confirm |
| Language & tone: no jargon, hex codes, or technical assumptions; "users are human beings" | Copy deck (§6) |
| No spelling mistakes | Spell-check pass on the English strings in CI |
| Simple two-colour icons | Thin-stroke single-colour glyphs only; no emoji |
| Start tile / splash simple and on-brand | Plain black splash with the Meena emblem; no animated splash |

### 2.3 What the two reference screenshots add

- **Hohm panorama (screenshot 2):** oversized, light, lowercase titles that bleed off the right edge to hint at the next page ("all electric gas water", "network devices"); a single accent (orange); thin line glyphs beside list rows; small grey labels over values ("right now / this month"); thin flat progress lines; full-bleed background image with content on top. **Applied:** cropped titles on every page, one accent, thin glyphs in lists only, label-over-value rows, wallpaper as full-bleed with a dim control.
- **WP7 shots (screenshot 1):** huge light page titles ("people recent", "profile what's new") with the next title dimmed and cropped; a flat calendar grid titled in lowercase ("sep 2010") with today as a solid accent square; an app bar of three circle-outline glyphs on *detail* pages. **Applied:** dimmed next-title peek, flat month grid, app bar on detail and settings sub-pages only. The image caption reads "Figure 4 - Bad", so I used it only for visual language, not as a pattern to copy wholesale. [I]

### 2.4 Design tokens

| Token | Value |
|---|---|
| Background | `#000000` (dark) · `#FFFFFF` (light) · optional full-bleed wallpaper with dim |
| Text | primary 100% · secondary ≥ 70% · **never below 4.5:1 contrast** (min `#767676` on black) |
| Accent | 10 swatches + custom; accent is used for titles' key words, progress, today marker, links |
| Type | Selawik Light for titles (page 42sp, section 24sp), Semilight/Regular body 15–17sp, caption 12sp **minimum** |
| Spacing | 6 / 12 / 24 dp scale; 24dp side margin |
| Shape | Square corners everywhere; **zero circles** except where the icon glyph itself is a circle (app-bar outline icons) |
| Icons | Thin-stroke single colour (Fluent System Icons *regular*; verify licence) |
| Touch targets | ≥ 48dp, even when the glyph is small |
| Press feedback | Scale to 0.96 + slight tilt (global `MetroIndication`), no ripple |
| Motion | Turnstile on page entry, staggered "feather" of list items, ≤ 250 ms; respects system "remove animations" and the Meena motion setting |
| Progress | Dotted indeterminate bar for loading; flat thin line for values |

### 2.5 Shell & navigation (AIO-inspired, Metro-correct)

- **Pages:** `today` · `inbox` · `news` · `money` · `apps`. Titles are oversized and cropped at the right edge; swiping parallax-syncs the title row using **measured** title widths (fixes the fixed 135dp bug).
- **AIO influence:** each page is one vertical stream of text-first widgets; widgets can be reordered, collapsed or hidden; no boxes.
- **Gestures (replace the FAB):** swipe down from top = quick panel; swipe from the screen edge opposite your handedness = jump to apps with search focused; long-press empty space or a page title = "customise this page" (widget on/off + order); back = return to *today*.
- **Handedness:** the setting mirrors which edge carries the apps gesture and where the quick panel's settings link sits.
- **Quick panel (action centre):** four live tiles (wi-fi, bluetooth, torch, settings) showing real state; below them, notifications.
- **Settings:** a pivot (`widgets` · `accounts` · `look`) of link lists. Every item opens a sub-page with an app bar (save / cancel in the bar).
- **Apps page:** keep the accent letter headers and jump-list grid (closest to authentic). Add: monochrome icon option, flat lowercase long-press menu, search that is always one gesture away.

---

## 3. Widgets: the "live, never hardcoded" contract

### 3.1 Rules (enforced, not aspirational)

1. **No fabricated values in `release`.** No literal prices, temperatures, device names, step counts, balances, dates, greetings with facts, or "120Hz LTPO"-style strings. A greeting like "good morning" is derived from the clock; everything else comes from a source.
2. **Every widget is a `WidgetSpec`** with: `id`, `title`, `source`, `refreshPolicy`, `settingsSchema`, and a state machine `needsSetup → loading → ready (with updatedAt) → stale → error → empty`.
3. **Cache is not hardcoding.** Last good result may be shown, labelled "updated 12 min ago", never presented as fresh.
4. **Static reference lists are allowed** (JAKIM zone catalogue, ISO currency list, accent swatches, bank-app package allowlist). They are catalogues, not data about the user or the world. Refresh them from the live source when one exists.
5. **No source = no widget content.** Show a `needsSetup` row ("add a city") or hide the widget, never a sample.
6. **CI guards:** (a) `release` build must not include the `demo` source set; (b) unit test fails if any `WidgetSpec` repository returns a non-empty value without a source call or a cache hit; (c) lint rule flags numeric/string literals in `data/**` that are not in an allowlist file.

### 3.2 Common settings for every widget

Every widget gets, in **settings → widgets → [widget name]**: `show on [page]` toggle · position (drag in the widgets list) · collapsed by default · max lines/items · refresh (auto / manual only) · "clear this widget's data". Widget-specific options are in the table.

### 3.3 Widget catalogue (new IDs; old keys migrate)

| Widget (page) | Old key | Live source | Widget-specific settings | Needs-setup state |
|---|---|---|---|---|
| **clock** (today) | `widget-clock-weather` | System clock | time/date format, seconds, hijri on/off + day adjust, week number | none |
| **weather** (today) | `widget-clock-weather`, `widget-weather-forecast` | Weather API (§5.6) | location (auto / city), units °C/°F, forecast days 0–5 | "pick a place" |
| **solat** (today) | agenda hub | JAKIM e-solat by zone (§5.5) | zone / auto, which prayers show, imsak/syuruk on/off, jumaat label | "where are you?" |
| **agenda** (today) | `widget-calendar-month`, `widget-today-summary` | CalendarContract (§5.2) | calendars on/off, days ahead 1–14, all-day on/off, month grid on/off | "allow calendar" |
| **brief** (today) | `widget-today-summary` | Dropped (D13); the old key is removed on upgrade | n/a | n/a |
| **health** (today) | `widget-health-*` | Health Connect (§5.7) | metrics on/off, goals, data-from app | "connect health" |
| **air** (today) | `widget-health-aqi` | AQI API (§5.6) | location, unit | "pick a place" |
| **alerts** (inbox) | `widget-notifications` | NotificationListenerService | max items, which apps, show text on/off | "allow notification access" |
| **chats** (inbox) | `widget-conversations` | NotificationListenerService (messaging apps) | which messaging apps, max threads, show preview on/off | same as above |
| **mail** (inbox) | new | Gmail notifications (M1), Gmail API unread count (M3) | accounts, show sender/subject on/off | "allow notification access" |
| **news** (news) | `widget-news-feed` | RSS/Atom from your feeds (§5.4) | feeds (manage), items 3–20, mix mode, refresh | "add a feed" |
| **portfolio** (money) | `widget-finance-charts` | Quote provider + FX (§5.3) | holdings (manage), base currency, show amounts on/off, group by | "add your first holding" |
| **watchlist** (money) | `widget-finance-charts` | Quote provider | symbols (manage), chart range | "add a symbol" |
| **rates** (money) | `widget-exchange-rates` | FX API (§5.3) | base, pairs (manage), decimals | "pick a currency" |
| **spending** (money) | `widget-spending-summary` | Parsed bank notifications | opt-in, which bank apps, period | "turn on spending" (off by default) |
| **notes** (today) | `widget-notes` | Local (user-written) | max notes | "write a note" |
| **tasks** (today) | `widget-tasks` | Local (user-written) | show completed | "add a task" |
| **phone** (today) | `widget-device-telemetry` | Battery/memory/storage/data/brightness APIs | which stats show | none |
| **nearby** (today) | `widget-map-radar` | Map/radar tiles (provider TBD) | radius, layer | "allow location" |

### 3.4 Freshness targets ("realtime" defined)

| Source | Mechanism | Target freshness | Caveat |
|---|---|---|---|
| Clock, battery, memory, storage, data used | Local callbacks / short polling while visible | ≤ 1–5 s | Truly live |
| Notifications, chats, Gmail notifications | `NotificationListenerService` push | Instant | Needs user permission |
| Calendar | `ContentObserver` on CalendarContract | Near-instant after sync | Google→device sync delay is outside our control |
| Solat | Month fetched once, cached, refreshed monthly | Exact to the day | Prayer times are published schedules, not streams |
| Weather / AQI | HTTP poll, 15–30 min | ≤ 30 min | Provider model update cadence |
| News | Poll on resume + WorkManager (platform minimum 15 min) | ≤ 15–30 min | Background limits [F] |
| Stock quotes | Poll 1–5 min in market hours | Provider delay (often 15 min on free tiers) | Not tick data |
| Sheets/GOOGLEFINANCE | Poll on resume + 15 min | Up to ~20 min delayed [S] | Google-side delay |
| FX rates | Poll 15–60 min | Provider cadence (often daily) | Reference rates, not live trading |
| Health | Health Connect changes + poll while visible | Depends on band sync | Companion app must sync first |

Each widget shows its own "updated x ago" in the caption line.

---

## 4. Settings spec

### 4.1 Information architecture

Updated 13 Nov 2026 (owner: regroup settings, Zune HD look). Four pivots, link lists only; toggles stay on sub-pages.

```
settings (pivot, link lists only; no switches here)
├─ pages                 → widgets grouped by the page they are on (today, inbox, news, money); on/off status; tap → widget sub-page
├─ connections
│   ├─ where you are     → prayer times (zone)
│   ├─ your day          → calendar, notifications, health
│   └─ reading and money → news feeds, money (portfolio, sheet link, rates)
├─ personalize
│   ├─ look              → theme and accent, text size
│   └─ feel              → date and time, hand and touch (hand, vibration, motion)
└─ system
    ├─ your data         → privacy, backup and restore
    └─ meena             → about meena ui
```

Original plan, kept for history:

```
settings (pivot, link lists only; no switches here)
├─ widgets
│   ├─ all widgets        → list with on/off state text; tap → widget sub-page; "reorder"
│   └─ per-widget pages   → on/off toggle, page, order, widget-specific options (§3.3)
├─ accounts
│   ├─ google             → sign-in / disconnect; shows what each feature uses
│   ├─ calendar           → pick calendars
│   ├─ mail               → gmail
│   ├─ finance            → provider key, sheet bridge, portfolio, rates
│   └─ health             → health connect, data-from app, companion app
└─ look & feel
    ├─ date & time
    ├─ theme
    ├─ solat location
    ├─ news feeds
    ├─ language, hand, haptics, motion
    └─ backup, privacy, about meena ui
```

### 4.2 Required items: field-level spec

| # | Requirement | Fields & behaviour | Storage / validation |
|---|---|---|---|
| 1 | **Date & time format** | 12h / 24h / follow system · date pattern (list picker with **live preview using today's date**) · first day of week · seconds on/off · hijri on/off + adjust −2…+2 days · week numbers | DataStore; preview rendered via `DateTimeFormatter` |
| 2 | **Gemini API key** | Dropped (D13). | Any saved key is deleted on upgrade. |
| 3 | **Theme management** | Base: dark / light / follow system · accent: 10 WP swatches + custom hex · wallpaper picker + dim · font (Selawik / system) + size · motion: full / reduced / off · haptics · handedness · export / import theme (JSON) · reset | Contrast check blocks accents that make text < 3:1 in large text or < 4.5:1 in body; warn instead of silently allowing |
| 4 | **Waktu solat location** | "use my location (coarse)" or pick **state → zone** (two list-picker steps) · shows zone code and name · prayers shown · show imsak/syuruk · jumaat label on Fridays | Zone catalogue refreshed from JAKIM when reachable; coarse location only; if location denied, fall back to manual |
| 5 | **Google Calendar integration** | Allow calendar access · list of device calendars grouped by account, each with on/off + colour dot · days ahead · all-day on/off · "add account" (opens system) · "new event" opens the calendar app via intent | READ_CALENDAR only; we never write |
| 6 | **Gmail integration** | M1: choose Gmail accounts whose notifications count · show sender/subject on/off. M3: connect for unread count (label counts only) | Gmail API only in M3 after scope check (D6) |
| 7 | **Google Finance integration** | "connect a google sheet": Drive picker (user selects one file) · map columns (symbol, price, change) · refresh · disconnect · explain delay | `drive.file` scope via picker; read-only ranges; no server |
| 8 | **News feed management** | Add by URL (auto-discovers the feed from a website address) · test before saving · name · on/off · reorder · remove (yes/no dialog) · per-feed item cap · mix mode (newest first / round-robin) · open links in (in-app tab / browser) · import/export OPML · "suggested" list is optional, nothing preinstalled except what the user keeps | Validate by fetch + parse; reject non-HTTPS |
| 9 | **Finance portfolio management** | Add holding: search symbol → exchange → quantity → average cost → currency → group name (e.g. your own account names) · edit · **delete with yes/no dialog** · show amounts on/off (hide on lock screen) · base currency | Local encrypted storage; no cloud; empty by default |
| 10 | **Exchange rate selection** | Base currency · add/remove quote currencies (list from provider's live currency list) · source · decimals · show inverse | Offline cache of last rates |
| 11 | **Health companion app / device + Google Fit** | "connect health connect" (per-metric permissions) · **data-from** picker (apps that write to Health Connect, with priority) · metrics on/off · step goal · sleep goal · "open companion app" (pick any installed fitness app) · note explaining that Google Fit is retired (D1) | Health Connect permissions; no raw health data leaves the phone |

### 4.3 Extra settings the above implies

`notification access` (status + open system screen + which apps feed alerts/spending) · `spending` opt-in + bank-app allowlist + "clear spending data" · `backup` (export/import everything except secrets) · `privacy` (what each feature reads, per integration; "disconnect all") · `about meena ui` (version, licences, privacy policy link).

---

## 5. Integration notes

### 5.1 Gemini brief (dropped, D13)
- Removed on 12 Nov 2026. Notes kept for history only.
- Produces a 2–3 line summary from **only** the sources the user ticks. No key → widget shows "add your gemini key".
- Cost and quota are the user's. Handle 429/blocked with a human message.
- Disclose in Data Safety that selected content is sent to Google's API on user action.

### 5.2 Calendar
- `CalendarContract` queries for instances in the next N days; `ContentObserver` for live updates. Handle no-account and permission-denied states.
- "Manage calendar" = choose visible calendars, colours, range. Creating events launches the calendar app (`ACTION_INSERT`) so we need no write permission. [I]

### 5.3 Finance (free-only)
- **Quotes, primary:** the **Sheets bridge**. The user's own sheet runs `GOOGLEFINANCE()`, which supports Bursa Malaysia tickers [I: verify], and the app reads the picked file. Free, no key, may be delayed up to ~20 min [S].
- **Quotes, optional second source:** a free-tier API with the user's own key. Free tiers usually lack Bursa coverage or have tight daily caps (Alpha Vantage, Finnhub, Twelve Data are candidates; **coverage test first**) [S]. **Checked 15 Nov 2026:** none of Alpha Vantage, Twelve Data, Finnhub, Marketstack, FMP or Stooq allows showing Bursa prices in a public app on its free tier, so no second source is used.
- **Remove** the unofficial Yahoo endpoint. (Done. Yahoo's developer site lists no finance API, checked 16 Nov 2026.)
- **Sheet template (16 Nov 2026):** `docs/finance-sheet.csv`, using Google Finance's `KLSE:NAME` codes (`KLSE:FFB`, not the stock number 5306). `GOOGLEFINANCE` has no name attribute, so names are typed in.
- **FX (free, no key):** Frankfurter (ECB reference rates, daily) is the default candidate; Bank Negara Malaysia OpenAPI is the alternative. Verify MYR coverage and terms [S]. **Built (16 Nov 2026):** Frankfurter v2 blended rates for any of 161 currencies, plus metals and 35 coins for the markets chart; its FAQ allows commercial use and caching. Bank Negara supplies Kijang Emas prices (terms unread, [S]).
- **Portfolio:** pure local data entry; prices and totals computed live. Seed with nothing. The old hardcoded portfolio values and watchlist are deleted.
- **Candles:** fix the cache to emit via `StateFlow`; drop the synthetic fallback. If the free source has no history, hide the chart rather than fake it.

### 5.4 News
- RSS/Atom parser with ETag/If-Modified-Since, per-feed error isolation (one bad feed never blanks the widget), HTML entity decoding via a proper parser, images off by default for Light/Fast.
- Remove the hardcoded fallback articles and fake relative times.

### 5.5 Solat
- Primary: JAKIM e-solat (official) per zone, month cached. Hijri date comes from the API response, not a literal. Fallback for GPS-based lookup: a calculation API with a JAKIM method (verify) [S].
- Delete the fixed timetable, fake monthly variance and hardcoded Hijri strings (two of them currently disagree [F]).
- **Out of scope for v1:** azan alarms (exact-alarm permission policy) [I].

### 5.6 Weather & air quality
- Free defaults: **Open-Meteo** for weather and air quality (no key; free for non-commercial use, so confirm a free, ad-free personal app qualifies) [S]. Alternative: Malaysia's open-data weather forecast API [S].
- **Cut `nearby` (map/radar) from v1**: free radar tile terms are unclear and it does not serve a primary task.
- Delete the hardcoded "31° clear", 5-day forecast, humidity 72%, UV 8, and Cheras station.

### 5.7 Health
- Health Connect for steps, sleep, heart rate, distance, active energy. Handle "Health Connect not installed" (Android 9–13) and "no data yet" [F, cited in §9]. Needs the Health Connect permissions declaration on Play.
- Device name shown is the **data-origin app** the user picked, never a hardcoded band model.
- Fitbit users: Fitbit Web API is retired in Sept 2026; they sync via Health Connect [F, cited]. A direct Google Health API integration would need server-side work and a security assessment and is **not planned**.

---

## 6. Copywriting: Meena voice

### 6.1 Voice rules
1. **lowercase everywhere** (titles, buttons, labels). Proper nouns stay as written (Wi-Fi → "wi-fi" is fine).
2. **Short and human.** Titles 1–3 words; sentences under ~10 words; say what happened and what to do.
3. **No jargon.** Banned: telemetry, RAM, LTPO, realtime sync, permission, intent, package, canvas, context, proactive.
4. **No ALL CAPS, no emoji, no arrows or symbols in text** (✕ ▲ ▼ → ⚡).
5. **Errors are kind and useful:** "couldn't reach the news. try again?" not codes.
6. **Casual, not cutesy.** Friendly but never jokes in errors or money/health/prayer text.
7. **English only.** Spell-check the strings in CI.
8. **Dialogs:** title + one question + `yes` / `no` side by side.

### 6.2 Page and widget names

| Now | Meena UI |
|---|---|
| start / agenda / news / finance / health / apps | **today** / (merged) / **news** / **money** / (merged) / **apps**; new **inbox** |
| system telemetry | **phone** |
| notification stream | **alerts** |
| conversations | **chats** |
| today • summary | **brief** |
| action center | **quick panel** |
| Meena Launcher (in-app) | **meena** (app) · **meena ui** (design language) |

### 6.3 String rewrites (examples taken from the current code)

| Where | Current | New (en) |
|---|---|---|
| Alerts, no access | NOTIFICATION ACCESS REQUIRED / Tap to grant permission so real-time notifications show up. | want your alerts here? / turn on notification access. meena reads them on your phone only. |
| Alerts, collapsed | Permission Required | needs a quick okay |
| Alerts, count | N unread notifications | n new |
| Alerts, empty | All caught up • Real-time notifications active | all quiet |
| Alerts, action | CLEAR ALL NOTIFICATIONS | clear all |
| Brief | Gemini Flash Proactive Context Active | brief is on |
| Brief, needs key | (none) | add your gemini key to get a brief |
| Phone | BATTERY: 80% / Discharging • Optimal | battery · 80% / on battery |
| Phone | MEMORY (RAM) / Avail: x GB | memory / x gb free |
| Phone | DATA TRAFFIC: ↓ ↑ | data used today |
| Phone | DISPLAY & BRIGHTNESS: Level 70% • 120Hz LTPO | screen · 70% |
| Weather | 5-DAY FORECAST • HUMIDITY 72% • UV 8 | next 5 days |
| Quick panel | AMOLED 0dp Pure Metro • System Ready | nothing new |
| Quick panel | CLOSE ✕ / ALL SETTINGS → | close / all settings |
| Quick panel tiles | Wi-Fi: Connected (static) | wi-fi · *live state* |
| Apps, search | Search apps... | search |
| Apps, empty | No apps found matching "x" | nothing called "x" |
| Apps, menu | Pin to Start / Display tile on main panoramic canvas | pin to today |
| Apps, menu | App Info & Permissions / Open Android system settings & permissions | app info |
| Apps, menu | Uninstall App / Delete application package from device | uninstall |
| Apps, jump | choose letter | jump to |
| Toasts | "Calculator opened" / "Camera intent launched" (shown on **failure**) | couldn't open calculator |
| Toasts | App x not installed | x isn't installed |
| Dialog | CLOSE | ok |
| Delete holding | (none) | delete this holding? / yes / no |
| Generic error | (none) | something went wrong. try again? |
| Offline | (none) | you're offline. showing what we had. |
| Settings label | font: Segoe UI | font |
| About | README: "Metro UI Minimalist Launcher" | meena · built on meena ui: clean, light, open, fast |

Add the full `strings.xml`; no hardcoded user-facing strings in Kotlin. [F: many strings are currently literals in composables]

---

## 7. Phased plan

### Phase 0: Decisions (0.5 day)
Confirm D1–D12 (§1), search Play and MyIPO for "Meena" collisions, and register the personal Play developer account (identity verification takes time).

### Phase 1: Foundation (3 days)
- Rename `applicationId` + namespace; move packages.
- `allowBackup=false`; remove template backup rules.
- Replace `queryIntentActivities`/`QUERY_ALL_PACKAGES` with `LauncherApps` + callback (also fixes new-install refresh and work profiles).
- Release signing, R8 + resource shrinking, real `proguard-rules.pro`.
- Fix cold-start `defaultHubIndex` bug (load settings before building the pager).
- Delete hardcoded personal data from source.
- Set up flavors: `release` and `demo`.
- **Exit:** signed `bundleRelease` works; fresh install shows newly installed apps live.

### Phase 2: Widget framework + Settings shell (6–8 days)
- `WidgetSpec`, `WidgetRepository`, per-widget state machine, `updatedAt` plumbing, cache layer.
- Settings store with schema per widget; **migration** from old `enabledWidgets` keys.
- Settings IA (§4.1) with all screens stubbed and navigable; on/off and ordering fully working.
- Secrets store (Keystore/Tink) for API keys.
- CI guards from §3.1.
- **Exit:** every existing widget can be toggled, ordered, and shows a correct needs-setup state; zero fabricated values remain.

### Phase 3: Integrations (can run in parallel with Phase 4)
| Item | Days | Milestone |
|---|---|---|
| Solat (JAKIM, zones, cache, hijri) (Done; zone picked from location, 15 Nov) | 2 | M1 |
| Weather + AQI (Done) | 2–3 | M1 (AQI M3) |
| Calendar (CalendarContract + manager) (Done) | 2 | M1 |
| News manager (add/validate/OPML, parser hardening) (Done) | 2–3 | M1 |
| Notifications: alerts/chats/mail(notif), spending opt-in + allowlist (Done) | 3 | M1 |
| Quick panel live state (wi-fi, bluetooth, torch) (Done) | 1 | M1 |
| Finance: provider, portfolio CRUD, FX, Sheets bridge (Done; plus Frankfurter markets, ticker search, BNM kijang emas) | 6–8 | M2 |
| Gemini brief (dropped, D13) | 0 | n/a |
| Health Connect (Done) | 3 | M3 |
| Gmail API unread count (dropped, D17) | 0 | n/a |

### Phase 4: Meena UI skin (8–10 days, parallel from Phase 2)
1. Tokens + Selawik + thin icon set; delete emoji and filled icons.
2. Components: `MetroIndication`, `MetroDialog`, `MetroListPicker`, `MetroToggle`, `MetroAppBar` (sub-pages only), dotted progress.
3. Shell: five panorama pages, measured cropped titles, edge gestures, quick panel gesture.
4. Screens in order: apps → today → inbox → settings → news → money.
5. Motion: turnstile, staggered feather-in, reduced-motion support.
6. Accessibility: 48dp targets, `contentDescription`, contrast checks.
- **Exit:** screenshot review against §2 and the two reference images; re-score at least 80/100 on the Metro-fidelity rubric.

### Phase 5: Copy (1–2 days)
Move every string to resources, apply §6, spell-check in CI, review all error and empty states.

### Phase 6: Compliance (3 days + external waits)
- Prominent disclosure before notification access; privacy policy; Data Safety (apps list, coarse location, notification-derived financial info, calendar, health); financial-features declaration; Health Connect permissions declaration; content rating; target audience.
- Remove `QUERY_ALL_PACKAGES`; coarse location only; justify READ_CALENDAR.
- Replace Yahoo scraping and any trademark-style naming (`GoogleFinance*`).

### Phase 7: QA (4–5 days)
Unit tests (parsers, formatters, migration, state machines), Compose UI tests (gestures, pickers, dialogs), device matrix (API 24, 33, 36; work profile; tablet), macrobenchmark + baseline profile, lint, StrictMode, Accessibility Scanner, 16 KB page-size check, offline and low-data runs.

### Phase 8: Release
Internal → closed test (personal account: 12 testers opted in for 14 continuous days if the account was created after 13 Nov 2023) → apply for production → staged rollout 10–20%, watch ANR/crash before widening.

**Status (16 Nov 2026):** phases 0 to 7 done. Phase 8: release signing done (upload key), 0.1.0 published as a GitHub release; Play tracks wait for the developer account.

---

## 8. Traceability & acceptance

| Your requirement | Where | Done when |
|---|---|---|
| Whole UI called **Meena UI**, inspired by Metro UI + AIO | §2, §6.2 | About screen and splash say "meena ui"; shell follows §2.5 |
| Clean, Light, Open, Fast + sub-points | §2.1 | Each rule's check passes (lint, size, benchmark, review) |
| Follow the WP guidelines deck | §2.2 | Checklist in §2.2 all ticked in review |
| All data realtime, not hardcoded | §3 | CI guards green; no literals in `data/**`; every widget shows "updated x ago" |
| Each widget configurable + on/off in Settings | §3.2–3.3, §4 | All 19 widgets have a settings sub-page with a working toggle |
| Date & time format | §4.2 #1 | Preview matches the widget immediately |
| Gemini API key | §4.2 #2 | Dropped (D13); no Gemini code or saved key remains |
| Theme management | §4.2 #3 | Dark/light/accent/wallpaper/export-import work and pass contrast rules |
| Waktu solat location | §4.2 #4 | Auto and manual zone both produce correct times for that zone |
| Google Calendar integration + manage | §4.2 #5 | Calendars can be hidden/shown; events update live |
| Gmail integration | §4.2 #6 | Unread/senders appear from notifications (M1); API count (M3) |
| Google Finance integration | §4.2 #7 | Picked sheet drives the watchlist; delay explained |
| News feed management | §4.2 #8 | Add/test/reorder/remove works; bad feed doesn't break others |
| Portfolio add/delete | §4.2 #9 | Empty by default; delete asks yes/no; totals use live FX |
| Exchange rate selection | §4.2 #10 | Base + pairs editable; offline cache shown |
| Health companion + Google Fit | §4.2 #11, D1 | Health Connect connected; data-from app selectable |
| General, casual copy | §6 | Copy review; no banned words |
| Save plan as .md | this file | done |

---

## 9. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Free quote APIs lack Bursa coverage or are delayed | Money page weaker | Coverage test first; Sheets bridge as second path; always show "updated" time |
| Gmail restricted scopes / assessment | Delay | M1 ships notification-based mail; M3 only if `gmail.labels`-level count works |
| Notification-listener rejection on Play | Blocks release | Prominent disclosure, opt-in, on-device-only wording, minimal retention |
| Health Connect declaration / review time | Delays M3 | Start the declaration early; ship M1 without health |
| Selawik or icon-set licence mismatch | Rework | Confirm licences before Phase 4 |
| Scope: 19 widgets × settings | Overrun | Framework first (Phase 2), widgets are then thin; ship by milestone |
| "Realtime" expectations | Disappointment | Freshness table (§3.4) shown in settings → about |
| Closed-test requirement on a personal account | ~3 weeks before production | Recruit 12 testers during Phase 7; keep them opted in 14 continuous days |

## 10. Decisions log

| Question | Answer (2 Oct 2026) |
|---|---|
| Play store name | **Meena** |
| Page count | 5 is enough |
| Package name | **`my.hana.meena`** |
| Quote API budget | Free only |
| UI language | English only |
| Play account | Personal |
| Gemini brief (12 Nov 2026) | Dropped; target audience 13+ (D13) |
| AIO Launcher (12 Nov 2026) | Copy AIO's **content** (what each widget shows), never its layout. Layout, type, spacing and every rule in §2 stay Metro/Zune (D14) |
| Free price sources (15 Nov 2026) | **Frankfurter v2** (keyless, free for commercial use) for exchange rates, metals and coins; ticker search uses its own list (D15) |
| Kijang Emas (15 Nov 2026) | **Bank Negara Malaysia Open API**, keyless; terms still to be read by the owner (D16) |
| Gmail API (15 Nov 2026) | Skipped; email comes from notifications (D17) |
| Double tap to lock (15 Nov 2026) | Device admin with force-lock only, off by default |
| Bursa stocks (16 Nov 2026) | Stay on the owner's Google Sheet (`docs/finance-sheet.csv`). Yahoo Finance has no official finance API and is not used |
| Version (14 Nov 2026) | `versionName` 0.1.0; first GitHub release 16 Nov 2026 |

## 11. Sources to verify before building

- Windows Phone UI Design Guidelines deck (SlideShare, N. Salvi): https://www.slideshare.net/slideshow/windows-phone-ui-design-guidelines/14216758
- Google Fit APIs deprecated → Health Connect; Fitbit Web API shuts down Sept 2026 → Google Health API / Health Connect (see developer.android.com Health Connect migration FAQ and Google's Health API migration page).
- Play requirements: target API 36 from 31 Aug 2026; 12 testers / 14 days for personal accounts created after 13 Nov 2023.
- To verify: Selawik licence, Fluent System Icons licence, Gmail scope classifications, `drive.file` access to Sheets API, JAKIM e-solat endpoints, weather/AQI/FX provider terms, Bursa symbol coverage.
