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

## Content to bring into Meena (D14: content only, Metro layout)

| # | AIO content | Meena today | Needs | Status |
|---|---|---|---|---|
| 1 | Clock info line: place, low and high, wind | time, date, hijri only | nothing new (Open-Meteo data already fetched) | **done** |
| 2 | Next alarm under the clock | none | `AlarmManager.getNextAlarmClock()`, no permission ([docs](https://developer.android.com/reference/android/app/AlarmManager#getNextAlarmClock())) | **done** |
| 3 | Battery percent and battery temperature | phone widget (off by default) | check what it shows | todo |
| 4 | 7-day calendar strip with event dots, weekends marked | month grid, agenda list | nothing new | **done** (off by default; all-day events placed by UTC date; tap a day opens the calendar) |
| 5 | Week numbers in the month grid | month grid | nothing new | **done** (ISO weeks, event dots, weekends muted; replaces the duplicate event list) |
| 6 | Quote list "symbol price (date)" with a dash when missing | portfolio, watchlist from sheet | nothing new | todo |
| 7 | Multi-symbol price chart | none | chart drawn with Compose canvas, no library | todo |
| 8 | More currency pairs | 9 pairs grid | nothing new | todo |
| 9 | Frequent apps row | favourites only | count launches inside Meena (no permission); system usage stats need a special permission, so not that | **done** (approved) |
| 10 | Notes hidden on home until tapped | notes widget | nothing new | todo |
| 11 | Connected Bluetooth devices | none | `BLUETOOTH_CONNECT` on Android 12+ (approved) | **done**: profile queries plus `ACTION_ACL_CONNECTED`/`DISCONNECTED` broadcasts, so input-device (HID) bands appear once they connect while Meena runs; HID host has no public query API |
| 12 | Wikipedia article | none | Wikimedia REST API `page/random/summary` with an identifying User-Agent ([access policy](https://www.mediawiki.org/wiki/Wikimedia_APIs/Access_policy)); the Wikifeeds featured feed is avoided because it is scheduled for deprecation from July 2026 ([Wikifeeds API](https://www.mediawiki.org/wiki/Wikifeeds_API)). Content is CC BY-SA, so the widget credits Wikipedia | **done** |
| 13 | Gmail and Telegram lists | inbox alerts and chats from notifications | per-app filter on existing notifications, no new permission | todo |
| 14 | Calculator | none | nothing new | todo |
| 15 | Map with place name | map radar widget | already shows map | **done**: OpenFreeMap dark vector style ([terms](https://openfreemap.org/tos/)), free with no key, drawn by MapLibre GL JS 5.24.1 (BSD-3) through `@maplibre/maplibre-gl-leaflet` 0.1.4 (ISC), approved 13 Nov 2026. CARTO raster tiles now need a key ([basemap-styles](https://github.com/CartoDB/basemap-styles)) |
