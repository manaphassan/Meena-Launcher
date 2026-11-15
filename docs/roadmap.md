# Meena roadmap and milestones

Status on 16 November 2026. Plan of record: [`meena-ui-fix-plan.md`](meena-ui-fix-plan.md) (milestones, phases, decisions) and [`meena-ui-fix-plan-v2.md`](meena-ui-fix-plan-v2.md) (UI score phases). What shipped: [`CHANGELOG.md`](../CHANGELOG.md).

## Where we are

**Version 0.1.0**, published as a GitHub release. Built and tested on the emulator and a TECNO KL8 (Android 14); starts on a Sony SO-02J (Android 8.0).
**Blocked on:** a personal Google Play developer account (owner).

| Check | Result (16 Nov 2026) |
|---|---|
| Unit tests | 238 live, 163 demo, 0 failed |
| Lint | 0 errors, 136 warnings |
| UI score (`meena-ui-score.md`) | 9.7 of 10; area 4 waits for the task test |
| Release build | APK 2.96 MB, AAB 6.0 MB, signed with the upload key |

## Milestones

| Milestone | Contains | Status |
|---|---|---|
| **M1: closed-test build** | Foundation, widget framework, settings, Meena UI skin, solat, weather, calendar, news, apps, alerts, phone, copy, compliance | **Done in code.** Play closed test not started (no account) |
| **M2: money** | Portfolio, rates, quotes, Sheets bridge | **Done**, and extended: markets chart, ticker search, rates table, kijang emas |
| **M3: health + mail API** | Health Connect, air quality, Gmail API unread count | Health Connect and air quality **done**. Gmail API **dropped** (owner, 15 Nov); email comes from notifications |

## Next (in order)

1. **Play developer account** (owner). Then Play App Signing with the existing upload key (`D:\HaNa_Innovation\keys\meena-upload.jks`).
2. **Internal test**, then **closed test**: 12 testers opted in for 14 continuous days (personal account rule). Tester guide: https://manaphassan.github.io/Meena-Launcher/tester-guide/
3. **Task test** (`task-test-plan.md`) with 5 people, to measure UI area 4. Waiting on the owner's choice of what to compare against.
4. **Play Console forms:** Data safety (answers in `play-store-compliance.md`), financial features declaration (check how the crypto and gold prices are answered), Health Connect declaration, content rating, target audience 13+.
5. **Production**, staged rollout 10 to 20%.

## Open decisions (owner)

| # | Question | Notes |
|---|---|---|
| 1 | "Malaysia top 5" on the money page: biggest 5 companies, today's movers, or the KLCI index? | Movers aren't possible from a sheet; the KLCI code on Google Finance isn't found yet |
| 2 | Bank Negara API terms | The portal blocks automated reading; read it in a browser before release |
| 3 | Task test comparison: stock launcher (proposed), 3 vs 5 lines, or Meena alone | `task-test-plan.md` |
| 4 | Stock search inside Meena (Google sign-in to write the sheet) | Needs a new library, a Cloud setup and possibly Google review; not planned unless approved |

## Backlog (not scheduled)

- "malaysia top 5" once decided.
- KLCI index on the money page, if a code that works in the sheet is found.
- Compose UI tests (only unit tests today).
- Lint warning clean-up (136, mostly unused resources).
- Sparklines for stocks. Google's help says `GOOGLEFINANCE` history "cannot be downloaded or accessed via the Sheets API or Apps Script"; whether the CSV export Meena reads carries it is unverified [S].

## Won't do

- Yahoo Finance: no official finance API on developer.yahoo.com, and its terms bar this use; scraping is against the plan.
- Gmail API, paid quote APIs, `QUERY_ALL_PACKAGES`, background location.
