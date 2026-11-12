# AIO Launcher reference (measured)

Measured on 12 Nov 2026 from AIO Launcher 7.6.2 (`ru.execbit.aiolauncher`) on a TECNO KL8 (720 × 1600 px, 320 dpi, so 1 dp = 2 px; font scale 1.0). Values come from `uiautomator dump` bounds and screenshots of the owner's home screen. The screenshots were deleted afterwards because they showed personal content; only layout facts are kept here.

Reference only: we study layout and behaviour. We do not copy AIO code, icons, fonts or assets.

## Structure
- One vertical scrolling list of widgets (RecyclerView). No pages, no pager.
- Floating round search button, bottom right.
- Widget order on the owner's phone: weather forecast row, clock and weather, monitor (battery), AI summary, connected devices, my apps, frequent apps, wikipedia, news feed, gmail, telegram, monthly calendar, calendar (agenda list), calendar (7-day strip), quotes chart, exchange rates, finance list, map, notes, calculator.

## Widget anatomy

| Part | Measurement |
|---|---|
| Page margin (left and right) | 16 px = **8 dp** |
| Header text | 33 px tall (about 16.5 dp line), small, muted blue-grey, sentence case ("Clock and weather") |
| Header suffix | " —" after the title on some widgets, " •" on others [I: collapsible vs live] |
| Divider | 2 px = **1 dp** line, full content width, directly under the header text |
| Divider to content | 24–32 px = **12–16 dp** |
| Content to next header | 35 px = **about 17 dp** |
| Body text line | about 34 px = **17 dp** per line (monospace pixel font in the owner's theme) |

## Specific widgets

| Widget | Layout |
|---|---|
| Forecast row | 7 equal columns across the width: day label (2 letters) top right of icon, temperature below |
| Clock and weather | Large clock (134 px = 67 dp tall) left, weather icon and temperature right. Info line below: "date, place  low … high, wind"; second line "Alarm: next alarm" |
| Monitor | Single pill: "Battery: 60%  32.0°" (battery and temperature) |
| App rows | 6 icons per row, 96 px = **48 dp** squares, 22 px = **11 dp** gaps, edge to edge |
| Text feeds (news, gmail, telegram, wikipedia) | Plain lines "title - source", a dot marks unread, no images |
| Calendar strip | 7 days in one row: big date number (49 px), weekday as superscript (28 px), event dots under the number, weekend dates in red |
| Monthly calendar | Week numbers in the first column, Mon–Su header, arrows to change month |
| Agenda | Coloured dot + "title - date" per line |
| Quotes | One multi-line chart (1Y) for several symbols, then "symbol = price currency (date)" per line in the line's colour |
| Exchange rates | One line: "1 USD = 4.08 MYR    1 JPY = 0.03 MYR" |
| Finance | "name = price currency change%" per line; "name = -" when no data |
| Notes | Coloured highlight blocks with date; blurred on the home screen [I: privacy mode] |
