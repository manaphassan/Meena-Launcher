# Meena roadmap and milestones

Status on 18 November 2026. Plan of record: [`meena-ui-fix-plan.md`](meena-ui-fix-plan.md) (milestones, phases, decisions) and [`meena-ui-fix-plan-v2.md`](meena-ui-fix-plan-v2.md) (UI score phases). What shipped: [`CHANGELOG.md`](../CHANGELOG.md).

## Where we are

**Version 0.2.0**, published as a GitHub release. Built and tested on a TECNO KL8 (Android 14); installs on a Sony SO-02J (Android 8.0), where Family Link blocked it from opening during testing.
**Blocked on:** a personal Google Play developer account (owner; on hold for now, 16 Nov 2026).

| Check | Result (18 Nov 2026) |
|---|---|
| Unit tests | 258 live, 258 demo, 0 failed |
| Lint | 0 errors, 161 warnings |
| UI score (`meena-ui-score.md`) | 9.7 of 10; area 4 waits for the task test |
| Release build | APK 3.98 MB, AAB 7.7 MB, ARM only, signed with the upload key |

## Milestones

| Milestone | Contains | Status |
|---|---|---|
| **M1: closed-test build** | Foundation, widget framework, settings, Meena UI skin, solat, weather, calendar, news, apps, alerts, phone, copy, compliance | **Done in code.** Play closed test not started (no account) |
| **M2: money** | Portfolio, rates, quotes, Sheets bridge | **Done**, and extended: markets chart, ticker search, rates table, kijang emas |
| **M3: health + mail API** | Health Connect, air quality, Gmail API unread count | Health Connect and air quality **done**. Gmail API **dropped** (owner, 15 Nov); email comes from notifications |

## Next (in order)

1. **Play developer account** (owner; on hold for now). Then Play App Signing with the existing upload key (`D:\HaNa_Innovation\keys\meena-upload.jks`).
2. **Internal test**, then **closed test**: 12 testers opted in for 14 continuous days (personal account rule). Tester guide: https://manaphassan.github.io/Meena-Launcher/tester-guide/
3. **Task test** (`task-test-plan.md`) with 5 people, to measure UI area 4. Proposed: AIO Launcher against Meena on the Sony; waiting for the owner's yes.
4. **Play Console forms:** Data safety (answers in `play-store-compliance.md`), financial features declaration (check how the crypto and gold prices are answered), Health Connect declaration, content rating, target audience 13+.
5. **Production**, staged rollout 10 to 20%.

## Open decisions (owner)

| # | Question | Notes |
|---|---|---|
| 1 | Bank Negara API terms | Owner said yes to reading them in a browser (16 Nov); the agent still can't open the portal. Tell the agent if they ask for attribution or set limits, so the widget can follow them |
| 2 | Task test comparison | Proposed: AIO Launcher (A) against Meena (B) on the Sony SO-02J; fallback: the Sony's own home screen. See `task-test-plan.md` |
| 3 | Stock search inside Meena (Google sign-in to write the sheet) | Needs a new library, a Cloud setup and possibly Google review; not planned unless approved |

Dropped: "malaysia top 5" on the money page (owner, 16 Nov 2026).

## Backlog (not scheduled)

- Compose UI tests (only unit tests today).
- Lint warning clean-up (161, mostly unused resources).
- App widgets: let people resize a widget; today each one gets the height it asks for.
- Material buttons in settings still bring their own press effect; swap them for a Meena button with the scale and tilt press.
- Continuum: the tapped title flies to the next page's title.
- A wide (2-wide) favourite tile for live faces.
- Sparklines for stocks. Google's help says `GOOGLEFINANCE` history "cannot be downloaded or accessed via the Sheets API or Apps Script"; whether the CSV export Meena reads carries it is unverified [S].

## Won't do

- Yahoo Finance: no official finance API on developer.yahoo.com, and its terms bar this use; scraping is against the plan.
- Gmail API, paid quote APIs, `QUERY_ALL_PACKAGES`, background location.
- Google ML Kit for the scanner: it sends usage data to Google ([data disclosure](https://developers.google.com/ml-kit/android-data-disclosure)). ZXing is used instead.
