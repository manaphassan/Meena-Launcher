# Meena

> A clean, light, open, and fast Android launcher inspired by Windows Phone Metro UI and AIO Launcher.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-purple.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-2026.03-green.svg?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Android](https://img.shields.io/badge/Android-7.0%20(API%2024)%20--%2016%20(API%2036)-blue.svg?style=flat-square&logo=android)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache_2.0-black.svg?style=flat-square)](LICENSE)

**Meena** reimagines the Android home screen around typographic clarity, flat geometry, and continuous lateral flow. Built from scratch with 100% Kotlin and Jetpack Compose, Meena delivers an authentic **Meena UI** experience: zero rounded card clutter, pure AMOLED contrast, and live on-device intelligence.

---

## 5 Canonical Pages

Meena organizes your mobile workflow across five panoramic canvas hubs:

| Hub | Purpose | Default Modules |
|---|---|---|
| **today** | Daily essentials & health | Clock, forecast, AI brief, JAKIM prayer times, calendar, tasks, notes, health telemetry, air quality, map radar, and phone status. |
| **inbox** | Communications center | Realtime conversation threads (WhatsApp, Telegram, SMS) and unified actionable notifications. |
| **news** | Curated reading | Dynamic RSS/Atom reader with source filters, instant categorization, and live article parsing. |
| **money** | Personal finance | Watchlist candlestick charts, live exchange rates, and bank notification spending insights. |
| **apps** | Application drawer | Alphabetical jump-list index with instant search, tile sizing, and pinning. |

---

## Design Principles

- **Typographic Hierarchy:** Content is defined by scale and spacing, not decorative cards, borders, drop shadows, or container boxes.
- **Pure Flat Canvas:** 0dp square corners throughout. Visual rhythm is driven by crisp accent lines and authentic Metro turnstile transitions.
- **Live, Never Fabricated:** Real status or explicit needs-setup states. No mock numbers, placeholder temperatures, or fake timestamps.
- **Modular Widgets:** Every widget can be repositioned, assigned to any page, or collapsed via the centralized settings pivot.
- **Single-Thumb Ergonomics:** Corner navigation hub engineered for natural reach with native left-handed and right-handed modes.

---

## Tech Stack

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3 foundation with custom Meena UI tokens)
- **Architecture:** Single-Activity, reactive StateFlow streams, Coroutines
- **Storage:** AndroidX DataStore with encrypted preference management
- **Networking & Parsing:** OkHttp3 with native streaming XML pull parsing
- **Minimum Target:** Android 7.0 (API 24) · **Target SDK:** Android 16 (API 36)

---

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17
- Android SDK 36

### Build Variants
- `demo`: Offline demonstration data for testing and development.
- `live`: Production-grade build utilizing live system APIs and verified user keys.

### Common Commands

```bash
# Build live debug APK
./gradlew assembleLiveDebug

# Run unit tests across all variants
./gradlew testDemoDebugUnitTest testLiveDebugUnitTest

# Install directly to connected device
./gradlew installLiveDebug
```

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
