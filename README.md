<p align="center">
  <img src="store/logo/meena-logo.svg" width="96" height="96" alt="Meena logo: a solid white pixel m on a blue square">
</p>

<h1 align="center">Meena</h1>

<p align="center">A home screen for Android. Clean, light, open, fast.</p>

<p align="center">
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.3-purple.svg?style=flat-square&logo=kotlin" alt="Kotlin 2.3"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack_Compose-2026.03-green.svg?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose 2026.03"></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-7.0%20(API%2024)%20to%2016%20(API%2036)-blue.svg?style=flat-square&logo=android" alt="Android 7.0 to 16"></a>
  <img src="https://img.shields.io/badge/version-0.1.0-black.svg?style=flat-square" alt="Version 0.1.0">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-black.svg?style=flat-square" alt="Apache 2.0"></a>
</p>

![Meena feature graphic](store/graphics/feature-graphic-1024x500.png)

<p align="center">
  <img src="store/screenshots/01-today.png" width="30%" alt="Today page with clock, weather, favourite tiles and forecast">
  <img src="store/screenshots/03-nearby-map.png" width="30%" alt="Nearby map with 3 km and 5 km rings, compass and qibla line">
  <img src="store/screenshots/06-apps.png" width="30%" alt="Apps page sorted by category">
</p>

Meena is an Android launcher in the **Meena UI** style, inspired by Windows Phone Metro and AIO Launcher. Big type, plenty of space, flat colour and square corners. No cards, no boxes, no made-up numbers. It is written in Kotlin with Jetpack Compose.

**Status:** 0.1.0, the first public build ([download from Releases](https://github.com/manaphassan/Meena-Launcher/releases/latest)). Getting ready for a Google Play closed test; it is not on the Play Store yet. What's next: [roadmap](docs/roadmap.md). What changed: [changelog](CHANGELOG.md).

- Website: https://manaphassan.github.io/Meena-Launcher/
- Privacy policy: https://manaphassan.github.io/Meena-Launcher/privacy-policy/
- Tester guide: https://manaphassan.github.io/Meena-Launcher/tester-guide/ (source: [docs/tester-guide.md](docs/tester-guide.md))

---

## Five pages

Swipe sideways through five pages.

| Page | What's on it |
|---|---|
| **today** | Clock, date and hijri date, with place, low and high, wind and next alarm. Weather with an animated icon and forecast. Up to six favourite app tiles, plus a tile that holds four small ones. Your week from the calendar. JAKIM prayer times, with the zone picked from your location. A nearby map with 3 km and 5 km rings, compass, qibla line and sun path. Notes. Battery with charge speed, time to full and time left. Connected Bluetooth devices with battery. Health Connect steps, distance, calories, heart rate and sleep. |
| **inbox** | Chats, email and alerts from your notifications, grouped by app, read on the phone. |
| **news** | RSS and Atom feeds you choose, with pictures and a reader view. |
| **money** | **Markets:** a year of gold, silver, bitcoin and ether on one chart (daily prices from Frankfurter); search and add other coins and metals. **Kijang Emas** coin prices from Bank Negara Malaysia. **Exchange rates** in a table with the day's change; add any currency. **Portfolio and watchlist** from your own Google Sheet (`GOOGLEFINANCE`). **Spending** picked up from bank alerts. |
| **apps** | Every app with search and a letter index. Sort a to z, by category or by last used. Icon packs. |

Every widget can be shown or hidden, moved to another page, or set to start folded, and shows 3 or 5 lines (settings, pages, widget length).

Also in settings, system: **shake to hide** chats, email, alerts, notes and money, and **double tap to lock** an empty spot (off by default; uses Android's device admin with the lock-screen ability only).

### Bursa stocks

There is no free stock price source Meena may use in a public app, so stock prices come from your own Google Sheet:

1. In Google Sheets, import [`docs/finance-sheet.csv`](docs/finance-sheet.csv) (file, import, upload; tick "convert text to numbers, dates, and formulas"). Edit the rows to your stocks. Codes are Google Finance's, like `KLSE:FFB` or `KLSE:SENHENG`, not the stock number.
2. Share it so anyone with the link can view.
3. Paste the link in Meena: settings, connections, money. Add the stocks you own as holdings there (units; cost is optional).

---

## Design rules

- **Type and space, not boxes.** Hierarchy comes from size and spacing. No cards, borders or shadows.
- **Square corners.** No rounded shapes, apart from icons that are circles themselves.
- **Real or nothing.** Each widget shows real information, or says it needs setup, is loading, is out of date, or failed. It never shows sample numbers.
- **Lowercase, short, plain.** All text is lowercase and in plain words.
- **Press feedback** is a slight scale and tilt, not a ripple.

---

## Privacy

- No ads, no accounts, no analytics.
- Notes, portfolio, sheet link and keys are encrypted with an Android Keystore key. Backup is off.
- Weather gets your location rounded to about 1 km. The map libraries are bundled in the app.
- Notification text, balances and health data stay on the phone.

The full list of online services and what each one receives is in the [privacy policy](docs/privacy-policy.md).

---

## Tech

- **Language:** Kotlin 2.3
- **UI:** Jetpack Compose (BOM 2026.03.01) with custom Meena UI tokens
- **Architecture:** single activity, `StateFlow`, coroutines
- **Storage:** AndroidX DataStore, with Keystore encryption for secrets
- **Network:** OkHttp, with a streaming XML parser for feeds
- **Health:** Health Connect
- **SDK:** min 24 (Android 7.0), target 36 (Android 16)

---

## Build

You need Android Studio, JDK 17 and Android SDK 36.

| Flavour | What it is |
|---|---|
| `live` | The real app. This is what ships. |
| `demo` | Installs next to `live` as `my.hana.meena.demo` for side-by-side testing. It has no sample data. |

```bash
# debug build
./gradlew assembleLiveDebug

# unit tests and lint
./gradlew testDebugUnitTest lint

# install on a connected phone
./gradlew installLiveDebug

# release build, signed with the upload key in keystore.properties
# (first time: node scripts/make-upload-key.js)
./gradlew bundleLiveRelease
```

Store assets (icon, feature graphic, listing text) are in [`store/`](store/).

### Install the release APK

Download `meena-0.1.0.apk` from [Releases](https://github.com/manaphassan/Meena-Launcher/releases/latest) and open it on your phone (Android 7.0 or newer; allow installing from your browser when asked). Then pick Meena as your home app when it asks.

The APK is signed with Meena's upload key. When Meena reaches Google Play, Play signs it with a different key, so you'll need to uninstall this APK before installing from Play.

---

## Documents

| Document | What it is |
|---|---|
| [Roadmap and milestones](docs/roadmap.md) | Where Meena is, what's next, open decisions |
| [Changelog](CHANGELOG.md) | What each version added |
| [Development plan](docs/meena-ui-fix-plan.md) | Milestones, phases, decisions D1 to D17, integration notes |
| [UI plan v2](docs/meena-ui-fix-plan-v2.md) and [UI score](docs/meena-ui-score.md) | The Meena UI rubric and the 9.7 / 10 sign-off |
| [AIO reference](docs/aio-reference.md) | What Meena takes from AIO Launcher, and what's done |
| [Privacy policy](docs/privacy-policy.md) | Every online service and what it receives |
| [Play compliance](docs/play-store-compliance.md) and [rollout guide](docs/play-store-rollout-guide.md) | Data safety answers, permissions, release checklist |
| [Tester guide](docs/tester-guide.md) and [task test plan](docs/task-test-plan.md) | For closed test testers, and the 5-person task test |

---

## Support

- Questions and bug reports: harussani@outlook.my, or open an issue.
- If Meena is useful to you, you can buy me a coffee at [paypal.me/manaphassan](https://paypal.me/manaphassan). Thank you!

---

## License

Apache License 2.0. See [LICENSE](LICENSE).
