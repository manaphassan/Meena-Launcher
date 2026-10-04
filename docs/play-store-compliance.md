# Google Play Console Compliance & Data Safety Guide

**Application:** Meena (`my.hana.meena`)  
**Target SDK:** 36 (Android 16 / 15)  
**Track:** Closed Test (Personal Developer Account)  
**Document Date:** October 2026  

---

## 1. Google Play Data Safety Form Questionnaire Responses

Use the following exact values when completing the Google Play Console **Data Safety** section:

### 1.1 Overview Questions
- **Does your app collect or share any of the required user data types?**  
  **Yes** (collected ephemerally on-device to power core launcher features; **Zero data is shared** with third parties).
- **Is all of the user data collected by your app encrypted in transit?**  
  **Yes** (all outbound HTTP requests for weather, news RSS, solat schedules, and Gemini API use HTTPS / TLS 1.3).
- **Do you provide a way for users to request that their data be deleted?**  
  **Yes** (users can clear all data directly via Android system App Settings or the in-app "reset to defaults" button; no remote servers store user data).

---

### 1.2 Data Type Breakdown Table

| Data Category | Data Type | Collected? | Shared? | Processed Ephemerally? | Required or Optional? | Purpose |
|---|---|---|---|---|---|---|
| **Location** | Approximate Location (`ACCESS_COARSE_LOCATION`) | Yes | **No** | **Yes** | Optional (fallback to manual solat zone) | App functionality (local weather & JAKIM prayer times) |
| **Financial Info** | Other financial info (Purchase / Transaction amounts) | Yes | **No** | **Yes** | Optional (requires explicit opt-in toggle) | Personal finance tracking (parsed locally in memory from selected bank push notifications) |
| **Health & Fitness** | Fitness info (Steps, distance, calories) | Yes | **No** | **Yes** | Optional (requires Health Connect permission) | App functionality (displaying daily activity summary on Today hub) |
| **Health & Fitness** | Health info (Sleep duration, heart rate) | Yes | **No** | **Yes** | Optional (requires Health Connect permission) | App functionality (displaying health metrics on Today hub) |
| **Calendar** | Calendar events | Yes | **No** | **Yes** | Optional (requires calendar permission) | App functionality (displaying upcoming agenda events on Today hub) |
| **Messages** | Other in-app messages / notification text | Yes | **No** | **Yes** | Optional (requires notification listener permission) | App functionality (Inbox hub alerts, chat summaries, and spending parser) |
| **App Info & Performance** | Installed apps | Yes | **No** | **Yes** | **Required** | Core launcher functionality (discovering, launching, and organizing apps) |
| **Personal Identifiers** | Name, Email, Phone, Device IDs | **No** | **No** | N/A | N/A | Meena collects zero personal identifiers or device IDs |

---

## 2. Sensitive Permissions Declarations

### 2.1 Calendar Access (`android.permission.READ_CALENDAR`)
- **Justification for Google Play Review:**  
  *"Meena is an Android home screen launcher replacement. The `READ_CALENDAR` permission is used strictly to display the user's upcoming appointments and events on the 'Today' panoramic home hub via Android's native CalendarContract. All event details are read locally in memory and never transmitted off the user's device. No write permissions (`WRITE_CALENDAR`) are requested; event creation delegates to the system calendar application via standard Android intents."*

### 2.2 Notification Listener Service (`BIND_NOTIFICATION_LISTENER_SERVICE`)
- **Justification for Google Play Review:**  
  *"Meena provides a unified 'Inbox' hub and an opt-in offline spending ledger. The notification listener service is required to read incoming message titles and alerts so users can review notifications directly from the launcher home screen without pulling down the notification shade. In addition, user-approved banking alerts are parsed in memory to record transaction amounts locally. No notification content is logged, persisted without encryption, or transmitted to any external server."*
- **Prominent Disclosure Implementation:**  
  A prominent modal dialog is presented to the user prior to directing them to system Notification Access settings, clearly stating that notifications are processed locally on-device.

### 2.3 Health Connect Read Scopes (`READ_STEPS`, `READ_DISTANCE`, etc.)
- **Justification for Google Play Review:**  
  *"Meena replaces the retired Google Fit APIs with Android Health Connect. Read-only permissions are used to aggregate and display the user's daily step count, active calories, sleep duration, and resting heart rate on the primary launcher home screen. Meena declares the mandatory `ACTION_SHOW_PERMISSIONS_RATIONALE` activity filter and links directly to its in-app privacy policy."*

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
  1. A manual local asset portfolio tracker (stored on-device with Android Keystore encryption).
  2. A read-only Google Sheets bridge where the user views their personal spreadsheet.
  3. An optional offline spending ledger parsed from device notification text.

---

## 5. Target Audience & Content Rating

- **Target Audience:** 13 years and older (or 18+ depending on region). Meena is not specifically targeted toward children.
- **Content Rating (IARC):** **Everyone** (Contains no violence, sexual content, gambling, user-generated content, or uncensored social interactions).
