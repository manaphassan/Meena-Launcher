# Google Play Console Compliance & Data Safety Guide

**Application:** Meena (`my.hana.meena`)  
**Target SDK:** 36 (Android 16 / 15)  
**Track:** Closed Test (Personal Developer Account)  
**Document Date:** October 2026 (updated 12 November 2026: Gemini brief removed; 13 November 2026: map, news pictures, rain outlook, live tiles, icon packs, Bluetooth battery)  

---

## 1. Google Play Data Safety Form Questionnaire Responses

Use the following exact values when completing the Google Play Console **Data Safety** section:

### 1.1 Overview Questions
- **Does your app collect or share any of the required user data types?**  
  **Yes.** Location rounded to about 1 km is sent to Open-Meteo for weather, the rain outlook and air quality. The map widget also loads map tiles from OpenFreeMap (its libraries are built into the app), and the tiles requested reveal the area around the user [I]. Everything else listed below is only processed on the device.
- **Is all of the user data collected by your app encrypted in transit?**  
  **Yes** (all requests for weather, map tiles, news RSS and news pictures, solat schedules and exchange rates use HTTPS; cleartext traffic is blocked by default at target SDK 36).
- **Do you provide a way for users to request that their data be deleted?**  
  **Yes** (users can clear all data directly via Android system App Settings or the in-app "reset to defaults" button; no remote servers store user data).

---

### 1.2 Data Type Breakdown Table

| Data Category | Data Type | Collected? | Shared? | Processed Ephemerally? | Required or Optional? | Purpose |
|---|---|---|---|---|---|---|
| **Location** | Precise and approximate location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) | Yes | **No** | **Yes** | Optional (fallback to manual solat zone) | App functionality (local weather, rain outlook, air quality, JAKIM prayer times, and the nearby map's tiles) |
| **Financial Info** | Purchase history (parsed bank alerts) | **No** (on-device only) | **No** | N/A | Optional (spending opt-in, off by default) | Not transmitted; parsed and stored on the device |
| **Health & Fitness** | Fitness info (Steps, distance, calories) | Yes | **No** | **Yes** | Optional (requires Health Connect permission) | App functionality (displaying daily activity summary on Today hub) |
| **Health & Fitness** | Health info (Sleep duration, heart rate) | Yes | **No** | **Yes** | Optional (requires Health Connect permission) | App functionality (displaying health metrics on Today hub) |
| **Calendar** | Calendar events | **No** (on-device only) | **No** | N/A | Optional (requires calendar permission) | Not transmitted; agenda shown on the Today hub |
| **Messages** | Other in-app messages / notification text | Yes | **No** | **Yes** | Optional (requires notification listener permission) | App functionality (Inbox hub alerts, chat summaries, live tile counts and chat names, and spending parser) |
| **App Info & Performance** | Installed apps | Yes | **No** | **Yes** | **Required** | Core launcher functionality (discovering, launching, and organizing apps) |
| **App activity** | App interactions (how often apps are opened from Meena) | **No** (on-device only) | **No** | N/A | Optional (frequent apps widget, off by default) | Not transmitted; ranks the frequent apps widget |
| **Device or other IDs** | Names and battery levels of connected Bluetooth devices | **No** (on-device only) | **No** | N/A | Optional (connected devices widget, off by default) | Not transmitted; shown on the Today hub |
| **Personal Identifiers** | Name, Email, Phone, Device IDs | **No** | **No** | N/A | N/A | Meena collects zero personal identifiers or device IDs |

**[S] Check before submitting:** health, messages and installed-apps rows say "Collected: Yes" but are never sent off the device. Play's [Data safety help](https://support.google.com/googleplay/android-developer/answer/10787469) says on-device-only data "does **not** need to be disclosed". Confirm whether to keep them or mark them not collected.

**[S] Also check:** whether map tile requests to OpenFreeMap count as sending approximate location. They carry no coordinates, only the tiles around the user's area. If Play treats that as collection, it is already covered by the Location row ("Collected: Yes"); mention OpenFreeMap in the privacy policy (done, §1.1).

**Not user data (no row needed):** news pictures are fetched from the feed sites the user added, and links the user taps (MET Malaysia rain radar, "open on" for news, "open in maps") open in the user's own browser or app; Meena sends those sites nothing.

---

## 2. Sensitive Permissions Declarations

### 2.1 Calendar Access (`android.permission.READ_CALENDAR`)
- **Justification for Google Play Review:**  
  *"Meena is an Android home screen launcher replacement. The `READ_CALENDAR` permission is used to display the user's upcoming appointments and events on the 'Today' panoramic home hub via Android's native CalendarContract. Event details are read locally and are never transmitted off the device. No write permissions (`WRITE_CALENDAR`) are requested; event creation delegates to the system calendar application via standard Android intents."*

### 2.2 Notification Listener Service (`BIND_NOTIFICATION_LISTENER_SERVICE`)
- **Justification for Google Play Review:**  
  *"Meena provides a unified 'Inbox' hub and an opt-in offline spending ledger. The notification listener service is required to read incoming message titles and alerts so users can review notifications directly from the launcher home screen without pulling down the notification shade. In addition, user-approved banking alerts are parsed in memory to record transaction amounts locally. No notification content is logged, persisted without encryption, or transmitted to any external server."*
- **Prominent Disclosure Implementation:**  
  A prominent modal dialog is presented to the user prior to directing them to system Notification Access settings, clearly stating that notifications are processed locally on-device.

### 2.3 Health Connect Read Scopes (`READ_STEPS`, `READ_DISTANCE`, etc.)
- **Justification for Google Play Review:**  
  *"Meena replaces the retired Google Fit APIs with Android Health Connect. Read-only permissions are used to aggregate and display the user's daily step count, active calories, sleep duration, and resting heart rate on the primary launcher home screen. Meena declares the mandatory `ACTION_SHOW_PERMISSIONS_RATIONALE` activity filter (and the Android 14+ `VIEW_PERMISSION_USAGE` alias), which opens the same privacy policy linked in Play Console."*

---

## 2.5 Privacy Policy URL

- **URL for Play Console (and the in-app link):** https://github.com/manaphassan/Meena-Launcher/blob/main/docs/privacy-policy.md
- **Shown in the app:** settings, then system, then about, then **privacy policy**; and from Health Connect's "privacy policy" link.
- **Before entering it in Play Console:** the page must load publicly. On 13 Nov 2026 it returned **404** because `docs/privacy-policy.md` is only on `phase/08-release`, not on `main`. Merge or push it to `main` first. Play requires an active, public, non-geofenced, non-PDF page ([User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)).

### 2.4 Nearby Devices (`BLUETOOTH_CONNECT`, plus `BLUETOOTH` with `maxSdkVersion=30`)
- **Use:** the optional connected devices widget lists Bluetooth devices that are already connected (`BluetoothManager.getConnectedDevices(GATT)`, the A2DP, HEADSET and input-device (HID host) profile proxies, and link broadcasts) and shows their names. It also shows a device's battery level when Android reports one in its battery broadcast. No scanning (`BLUETOOTH_SCAN` is not requested), no connecting, no location use.
- **Prominent disclosure:** a yes/no dialog (`disclosure_bluetooth_*`) explains what is read before the system permission prompt, and only appears when the user turns the widget on.

---

## 3. High-Risk Permissions Eliminated

In accordance with Google Play Developer Policy and Meena architecture rules:
1. **`QUERY_ALL_PACKAGES` Removed:**  
   The broad `QUERY_ALL_PACKAGES` permission has been completely removed from `AndroidManifest.xml`. App discovery is handled via Android system `LauncherApps` callbacks and targeted `<queries>` containing `android.intent.action.MAIN` with `android.intent.category.LAUNCHER`, plus the icon pack actions used by launchers (`org.adw.launcher.THEMES`, `com.novalauncher.THEME` and similar) so installed icon packs can be listed. `<queries>` entries are not permissions.
2. **Location: no background access.**  
   Meena requests `ACCESS_FINE_LOCATION` with `ACCESS_COARSE_LOCATION` (precise location approved by the owner on 14 Nov 2026, for the nearby map), only while it is open. On Android 12+ the user can keep it approximate. `ACCESS_BACKGROUND_LOCATION` is not requested. Coordinates sent to Open-Meteo are rounded to 2 decimals (about 1 km).

---

## 4. Financial Features Policy Declaration

- **App Category:** Personalization / Tools (Android Launcher).
- **Financial Services Declaration:**  
  Meena **does not** provide, facilitate, or promote financial services, banking, lending, credit issuance, or investment brokerage. The financial widget consists of:
  1. A manual local asset portfolio tracker (stored on-device, encrypted with an Android Keystore key; not sent anywhere).
  2. A read-only Google Sheets bridge where the user views their personal spreadsheet.
  3. An optional offline spending ledger parsed from device notification text.

---

## 5. Target Audience & Content Rating

- **Target Audience:** 13 years and older. Meena is not specifically targeted toward children. (Was 18+ only while the Gemini brief existed; see plan D13.)
- **Content Rating (IARC):** **Everyone** (Contains no violence, sexual content, gambling, user-generated content, or uncensored social interactions).
