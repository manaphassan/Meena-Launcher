# Changelog

All notable changes to Meena. Dates are when the work landed on `main`.
Versions follow `versionName` in `app/build.gradle.kts`.

## 0.1.0 (16 November 2026): first public build

The first build published as a GitHub release, ahead of the Google Play closed test.
It replaces the October prototype tagged `v1.0.0`, which was never released.

### Pages
- **today:** clock, date and hijri date, with place, low and high, wind and next alarm. Weather with an animated icon and a 5-day forecast. Up to six favourite tiles, plus one tile that holds four small apps; live tiles show counts and the next event. Your week from the calendar, with what's next. JAKIM prayer times, with the zone picked from your location. Optional: notes (edit, list, reminder), month grid with week numbers, health from Health Connect, connected Bluetooth devices with battery, phone battery with charge speed and time left, a nearby map with 3 and 5 km rings, compass, qibla line and sun path, frequent apps, Wikipedia.
- **inbox:** chats, email and alerts from your notifications, grouped by app, with 24-hour times.
- **news:** RSS and Atom feeds you choose, with pictures, a reader view and a blur for feeds you mark.
- **money:**
  - markets: a year of gold, silver, bitcoin and ether on one chart, from Frankfurter's free daily prices; search and add other coins and metals.
  - kijang emas: Bank Negara Malaysia's coin prices, what you pay and what you get back.
  - exchange rates as a table with the day's change; add any of 161 currencies.
  - portfolio and watchlist from your own Google Sheet (template in `docs/finance-sheet.csv`); holdings may leave out cost or price.
  - spending picked up from bank alerts, on the phone only.
- **apps:** search, letter index, sort a to z, by category or by last used; icon packs.

### Settings and system
- Four pivots: pages, connections, personalize, system.
- Widget length: 3 or 5 lines per widget.
- Dark, light and paper themes; ten accents; custom home title.
- Shake to hide chats, email, alerts, notes and money.
- Double tap an empty spot to lock the phone (device admin, force-lock only, off by default).
- Asks once to become the home app.

### Privacy and build
- No ads, accounts or analytics. Backup and device transfer are off.
- Notes, holdings and the sheet link are encrypted with an Android Keystore key.
- Weather gets location rounded to about 1 km; map libraries are bundled.
- Release builds use R8 and a baseline profile, and are signed with the upload key.

### Known gaps
- Bursa stock prices need your own Google Sheet; there is no free stock source Meena may use in a public app.
- Bank Negara's API terms have not been read yet.
- Not on Google Play yet: waiting for a developer account.
