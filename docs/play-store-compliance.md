# Google Play Console Compliance & Data Safety Guide

**Application:** Meena (`my.hana.meena`)  
**Target SDK:** 36 (Android 16 / 15)  
**Track:** Closed Test (Personal Developer Account)  
**Document Date:** October 2026 (updated 12 November 2026: Gemini brief removed)  

---

## 1. Google Play Data Safety Form Questionnaire Responses

Use the following exact values when completing the Google Play Console **Data Safety** section:

### 1.1 Overview Questions
- **Does your app collect or share any of the required user data types?**  
  **Yes.** Approximate location is sent to Open-Meteo for weather and air quality (§1.2). Everything else listed below is only processed on the device.
- **Is all of the user data collected by your app encrypted in transit?**  
  **Yes** (all requests for weather, news RSS, solat schedules and exchange rates use HTTPS; cleartext traffic is blocked by default at target SDK 36).
- **Do you provide a way for users to request that their data be deleted?**  
  **Yes** (users can clear all data directly via Android system App Settings or the in-app "reset to defaults" button; no remote servers store user data).

---

### 1.2 Data Type Breakdown Table

| Data Category | Data Type | Collected? | Shared? | Processed Ephemerally? | Required or Optional? | Purpose |
|---|---|---|---|---|---|---|
| **Location** | Approximate Location (`ACCESS_COARSE_LOCATION`) | Yes | **No** | **Yes** | Optional (fallback to manual solat zone) | App functionality (local weather, air quality & JAKIM prayer times) |
| **Financial Info** | Purchase history (parsed bank alerts) | **No** (on-device only) | **No** | N/A | Optional (spending opt-in, off by default) | Not transmitted; parsed and stored on the device |
| **Health & Fitness** | Fitness info (Steps, distance, calories) | Yes | **No** | **Yes** | Optional (requires Health Connect permission) | App functionality (displaying daily activity summary on Today hub) |
| **Health & Fitness** | Health info (Sleep duration, heart rate) | Yes | **No** | **Yes** | Optional (requires Health Connect permission) | App functionality (displaying health metrics on Today hub) |
| **Calendar** | Calendar events | **No** (on-device only) | **No** | N/A | Optional (requires calendar permission) | Not transmitted; agenda shown on the Today hub |
| **Messages** | Other in-app messages / notification text | Yes | **No** | **Yes** | Optional (requires notification listener permission) | App functionality (Inbox hub alerts, chat summaries, and spending parser) |
| **App Info & Performance** | Installed apps | Yes | **No** | **Yes** | **Required** | Core launcher functionality (discovering, launching, and organizing apps) |
| **App activity** | App interactions (how often apps are opened from Meena) | **No** (on-device only) | **No** | N/A | Optional (frequent apps widget, off by default) | Not transmitted; ranks the frequent apps widget |
| **Device or other IDs** | Names of connected Bluetooth devices | **No** (on-device only) | **No** | N/A | Optional (connected devices widget, off by default) | Not transmitted; shown on the Today hub |
| **Personal Identifiers** | Name, Email, Phone, Device IDs | **No** | **No** | N/A | N/A | Meena collects zero personal identifiers or device IDs |

**[S] Check before submitting:** health, messages and installed-apps rows say "Collected: Yes" but are never sent off the device. Play's [Data safety help](https://support.google.com/googleplay/android-developer/answer/10787469) says on-device-only data "does **not** need to be disclosed". Confirm whether to keep them or mark them not collected.

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
  *"Meena replaces the retired Google Fit APIs with Android Health Connect. Read-only permissions are used to aggregate and display the user's daily step count, active calories, sleep duration, and resting heart rate on the primary launcher home screen. Meena declares the mandatory `ACTION_SHOW_PERMISSIONS_RATIONALE` activity filter and links directly to its in-app privacy policy."*

### 2.4 Nearby Devices (`BLUETOOTH_CONNECT`, plus `BLUETOOTH` with `maxSdkVersion=30`)
- **Use:** the optional connected devices widget lists Bluetooth devices that are already connected (`BluetoothManager.getConnectedDevices(GATT)` and the A2DP and HEADSET profile proxies) and shows their names. No scanning (`BLUETOOTH_SCAN` is not requested), no connecting, no location use.
- **Prominent disclosure:** a yes/no dialog (`disclosure_bluetooth_*`) explains what is read before the system permission prompt, and only appears when the user turns the widget on.

---

## 3. High-Risk Permissions Eliminated

In accordance with Google Play Developer Policy and Meena architecture rules:
1. **`QUERY_ALL_PACKAGES` Removed:**  
   The broad `QUERY_ALL_PACKAGES` permission has been completely removed from `AndroidManifest.xml`. App discovery is handled via Android system `LauncherApps` callbacks and targeted `<queries>` containing `android.intent.action.MAIN` with `android.intent.category.LAUNCHER`.
2. **`ACCESS_FINE_LOCATION` Removed:**  
   Meena requests only `ACCESS_COARSE_LOCATION`. Precise GPS coordinates and background location monitoring are not used.

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
