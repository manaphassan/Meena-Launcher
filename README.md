# Meena Launcher 📱
Metro UI Minimalist Launcher for Android

A modern Android Home Screen / Launcher crafted with **100% Kotlin & Jetpack Compose**. Inspired by the Windows Phone / Metro UI panoramic canvas, Meena Launcher combines clean typography, fluid Live Tiles, and Malaysian localization features.

---

## ✨ Features

- **Panoramic Pivot Canvas:** Seamless horizontal navigation (`HorizontalPager`) across 4 main hubs:
  - 🏠 **Start Hub:** Customizable Live Tiles (`1x1`, `2x2`, `4x2`) with 3D flip animations, badge counters, and modular collapsible widgets (Weather Radar, News Feed, Device Telemetry).
  - 📅 **Agenda Hub:** Integrated **JAKIM Waktu Solat** prayer time schedule with real-time countdown calculations, solar indicators, interactive monthly calendar overview, and quick notes/task checklist.
  - 💳 **Finance Hub:** Real-time Malaysian banking & e-wallet transaction tracking (Maybank MAE, TNG eWallet, CIMB Octo, GrabPay, Google Wallet), spending visualizer curves, and foreign exchange rates.
  - 🚀 **Apps Hub:** Complete alphabetical jump-list drawer querying installed device packages with real-time search, pin-to-start, and custom tile sizing.
- **Radial Corner Navigation:** Single-thumb ergonomics supporting left-handed or right-handed modes with auto-hide during list scroll.
- **Action Center:** Top pull-down panel displaying real-time telemetry (RAM, storage, battery, network throughput Rx/Tx, screen brightness) and quick shortcuts.
- **Deep Customization:** AMOLED Black, Slate Dark, and Light themes, custom accent colors, Segoe UI typography scaling, and backup export/import.

---

## 🛠️ Tech Stack & Requirements

- **Language:** Kotlin 2.3.20
- **UI Framework:** Jetpack Compose (Compose BOM `2026.03.01`, Material 3)
- **Minimum SDK:** Android 7.0 (API 24)
- **Target SDK:** Android 16 (API 36)
- **JVM Toolchain:** Java 17
- **Architecture:** Single-Activity, Coroutines & StateFlow, DataStore Preferences, Background Services (`NotificationListenerService`).

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 17
- Android SDK 36

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License
This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
