# Changelog

All notable changes to Meena. Dates are when the work landed on `main`.
Versions follow `versionName` in `app/build.gradle.kts`.

## 0.2.1 (19 November 2026)

Installs over 0.2.0: same signing key, version code 3.

### New
- **Weather page** sits on a live sky drawn from the real readings: rain and snow fall as heavily as the forecast says and lean with the wind, clouds drift, storms flash, and clear skies show a turning sun or stars. Touch it: drops part around your finger, a tap ripples. Adds feels like, rain now, cloud, wind direction and the air quality band. Tap the temperature to switch units.
- **Wide tiles:** any single app or shortcut tile can be two cells wide, from its long-press menu.
- **App widgets:** drag the line under a widget to change its height.

### Changed
- Continuum: tap a settings row and its title flies up into the page's big title, and back on the way out. The music title in the inbox does the same into now playing. Off with less motion.
- Quick action icons are drawn and move while pressed; play and pause fold into each other, skip buttons lean.

### Privacy and build
- Weather now also reads wind direction and cloud cover from Open-Meteo, in the same request as before. Nothing new is sent.
- No new permissions or libraries.
- Debug builds show "debug" in the top right corner, so they can't be mistaken for a release.

## 0.2.0 (18 November 2026)

Installs over 0.1.0: same signing key, version code 2.

### New
- **Scanner** for QR codes and barcodes, read on the phone with ZXing. Tap to focus, pinch to zoom, and a light for dark places. Keeps up to 50 scans you save, encrypted. Off by default (settings, pages); asks for the camera when you first open it.
- **Favourite tiles:** up to 8 (was 6). Small tiles fit four to a row, icon only. Colourful tiles use each app's own icon colour. A tile can be an app shortcut, such as a chat with one person: pick one from the tile menu, or use "add shortcut" inside an app such as WhatsApp and say yes when Meena asks.
- **Screen time** on today: time on screen since midnight as an hour-by-hour bar chart, and the apps you used most, each with a bar for its share. Read from Android's own usage records, counting one app at a time so the total can't run past the clock. Turn on usage access for Meena when the widget asks.
- **App widgets** on today: put widgets from your other apps (a calendar, music, notes) on the today page. Apps that offer "add to home screen" for a widget can add it straight to Meena.
- **Notes:** start a line with `-` for a note, `*` for a task or `x` when it's done. Tap a task in an open note to tick it.
- **Inbox:** what's playing, with play, pause and skip; tap the track for a full now playing page with the album art, a huge title, a line to seek along and big controls. Alerts grouped by app; tap to see them all. Reply from the inbox when the app allows it. Pictures from notifications show as small previews. Each inbox widget shows its own number of items, up to 12.
- **Apps:** an a to z filter that shows one letter at a time.
- **Wallpaper** from your gallery, dimmed 40 to 90% so text stays easy to read.
- **Widget order:** move today's widgets up or down.
- Swipe down from the top of a page to open the quick panel.
- An optional link to the maker's digital card on today, and in about.

### Changed
- Settings has four tabs: personalize (now first, with quick switches), pages (with an on/off switch per widget), connections and about. Shake to hide moved to personalize; double tap to lock moved to about.
- Backups are restored from a file you pick, not from the clipboard.
- Sub-pages now turn like a door around the left edge: the old page turns away as the new one turns in, mirrored when going back. Group tiles tilt when pressed. The money text fields and the widget switches are square.
- Meena is described by its own brand, Meena UI, in the app and the store listing.
- Release builds are for ARM phones only (arm64-v8a and armeabi-v7a); x86 emulators can't install them. APK 3.98 MB, AAB 7.7 MB.

### Fixed
- Favourite tiles no longer disappear after an app is installed or removed.
- A large wallpaper photo no longer stalls the home screen: it is read in the background, at screen size.
- Today and apps do less work on each redraw.
- Shortcut tiles and notification pictures no longer call Android features that older versions don't have (found by lint).

### Privacy and build
- New: camera access, for the scanner only. Camera pictures are read in memory and never saved or sent. The camera is marked optional, so phones without one can still install Meena from Play.
- New: usage access (`PACKAGE_USAGE_STATS`) for screen time, which you turn on yourself in Android's settings, and the widget host permission (`BIND_APPWIDGET`), which Android never grants to Meena outright: it asks you before each new widget. Both approved by the owner on 18 November 2026.
- New libraries: CameraX 1.4.1 and ZXing core 3.5.4 (both Apache 2.0). Neither sends anything. An earlier draft used Google's ML Kit, which sends usage data to Google; it was replaced before release.
- R8 keep rules rewritten for kotlinx.serialization, OkHttp, CameraX and Health Connect.

### Known gaps
- Not yet seen running on the Sony SO-02J (Android 8.0): it installs, but Family Link blocked it during testing.
- On Android 7.0 and 7.1, apps can't add shortcuts or widgets to Meena themselves (that needs Android 8); pick them from Meena instead.
- Bursa stock prices still need your own Google Sheet, Bank Negara's API terms still haven't been read, and Meena is not on Google Play yet.

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
