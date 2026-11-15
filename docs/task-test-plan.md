# Meena task test plan (phase 10, plan section 5)

Goal: check rubric area 4 (content focus) with real people, so `docs/meena-ui-score.md` can move from 9 [I] to a measured score.
Source: `docs/meena-ui-fix-plan-v2.md` section 5.

## Decision needed: what is "A"?

The plan compares layout A with layout B through a debug-only switch. Layout A was the old home screen (`StartHub`, `CornerNavHub`), which was deleted in phase 1, so that switch can't be built without bringing dead code back.

| Option | A | B | Notes |
|---|---|---|---|
| **4 (proposed, 16 Nov)** | AIO Launcher, set up with the same widgets | Meena | The fairest test of rubric area 4 ("content focus, AIO goal"): same content, different layout. Run on the Sony SO-02J, so no one sees the owner's messages. Needs AIO installed there from Play (free). |
| 1 (fallback) | The phone's own home screen (Xperia Home on the Sony, HiOS on the TECNO) | Meena | What people already use. Likely weak on T3 and T5: I don't expect either stock screen to show prayer times or a 5-day forecast without another app [S: not checked]. |
| 2 | Meena at 5 lines | Meena at 3 lines | No code needed now (settings, pages, widget length). Only tells which length is better, not whether Meena beats anything. Good as a short extra round. |
| 3 | none | Meena | Checks Meena against fixed targets only (below). Weakest evidence. |

Until the owner says yes to option 4, the rest of this plan reads "A" as option 1. With option 4, read "stock launcher" below as "AIO Launcher".

## People and phone

- **5 or more people**, ideally from the closed test testers, and at least one who has never used Meena. Record people as P1, P2 and so on, never by name.
- **One phone for everyone**: a mid-range phone with the same apps and home setup for every session.
- **No personal data.** The owner's TECNO shows his messages, name and location, so either:
  - use a spare phone or a second Android user with neutral data (check that the TECNO offers multiple users: [S]), or
  - before each session, turn on Do Not Disturb, clear notifications, and set Meena's inbox and money widgets off.
- Nothing is recorded on video. The facilitator writes everything on the sheet.

## Setup (once, before the first session)

Set up both home screens the same way:

| Item | A (stock launcher) | B (Meena) |
|---|---|---|
| Favourite to open in T1 | Calculator on the first home page | Calculator as a favourite tile on today |
| Weather | The stock weather widget, if there is one | Weather and forecast widgets on (the default) |
| Prayer times | A prayer app on the first home page, if one is installed | Prayer times widget on (the default) |
| Widget to turn off in T4 | Any one widget on the home page | Any one widget, for example notes |

Switching home screen: Settings, search **home app**, pick A or B.

## Session (about 15 minutes per person)

1. Say: "This is a test of two home screens, not a test of you. Think out loud if you like. I can't help during a task." Ask if they're happy to continue.
2. **Order:** odd-numbered people do A first, even-numbered people do B first.
3. For each home screen, give each task in this order. Start timing on "go". Stop when the right app opens or they say the answer out loud.
4. Reset the home screen between people (re-pin the favourite, turn the widget back on, return to the first page).

| # | Task (read out exactly) | Done when |
|---|---|---|
| T1 | "Open the calculator." | The calculator is open |
| T2 | "What will the weather be tomorrow?" | They say tomorrow's weather |
| T3 | "When is the next prayer?" | They say the next prayer and its time |
| T4 | "Turn off one of the widgets on the home screen." | A widget is gone from the home screen |
| T5 | "What's the weather in five days?" | They say the forecast for the fifth day |

If someone gives up or passes 90 seconds, stop the task and record it as failed.

## What to record

One row per person, home screen and task, in `docs/task-test-results.csv`:

| Column | Meaning |
|---|---|
| `participant` | P1, P2 ... |
| `variant` | `A` or `B` |
| `order` | `1` if this home screen came first for this person, else `2` |
| `task` | `T1` to `T5` |
| `seconds` | Time from "go" to done, in whole seconds |
| `taps` | Taps, including long-presses |
| `scrolls` | Separate scroll or swipe gestures (page swipes count) |
| `errors` | Wrong turns: opening the wrong thing, backing out, asking for help |
| `success` | `1` done, `0` failed or gave up |

## First-screen count

Measure once per home screen, with nothing open and the home screen on its first page. Count the items on screen that show text or have a label, without saving what they say:

```bash
adb shell uiautomator dump /sdcard/ui.xml
adb shell "grep -o -E '(text|content-desc)=\"[^\"]+\"' /sdcard/ui.xml | wc -l"
adb shell rm /sdcard/ui.xml
```

Then repeat it for B at 320dp width and 130% text (on a 1080px wide screen, 320dp is density 540):

```bash
adb shell wm density 540
adb shell settings put system font_scale 1.3
# take the count again, and look for cut-off text
adb shell wm density reset
adb shell settings put system font_scale 1.0
```

## Pass rule (from the plan)

B passes if all of these are true:

1. Median time for T1 (open a favourite) is lower on B than on A.
2. Every B run of T1 has 0 scrolls.
3. B's first-screen count is lower than A's.
4. For every task, B's total errors are no higher than A's.

Fixed targets that also apply to B alone (rubric area 4): T1 takes 1 tap; the first screen shows the clock, date, favourites and all 5 forecast days without scrolling.

## Scoring

```bash
node scripts/task-test-summary.js docs/task-test-results.csv <A first-screen count> <B first-screen count>
```

It prints medians and totals per task and home screen, then each pass rule as `pass` or `FAIL`. Copy that output into `docs/meena-ui-score.md` (area 4) with the date and the phone used.
