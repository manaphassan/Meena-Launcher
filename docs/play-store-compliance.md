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
  **Yes.** Most data is only processed on the device. The exception is the optional Gemini brief (§1.3): when the user adds their own Gemini API key, the items they tick are sent to Google's Generative Language API.
- **Is all of the user data collected by your app encrypted in transit?**  
  **Yes** (all requests for weather, news RSS, solat schedules and the Gemini API use HTTPS; cleartext traffic is blocked by default at target SDK 36).
- **Do you provide a way for users to request that their data be deleted?**  
  **Yes** (users can clear all data directly via Android system App Settings or the in-app "reset to defaults" button; no remote servers store user data).

---

### 1.2 Data Type Breakdown Table

| Data Category | Data Type | Collected? | Shared? | Processed Ephemerally? | Required or Optional? | Purpose |
|---|---|---|---|---|---|---|
| **Location** | Approximate Location (`ACCESS_COARSE_LOCATION`) | Yes | See §1.3 | **No** if the Gemini brief is on, otherwise **Yes** | Optional (fallback to manual solat zone) | App functionality (local weather & JAKIM prayer times; place name in the Gemini brief) |
| **Financial Info** | Purchase history (spending count and total for today) | Yes, only if the brief's spending source is ticked | See §1.3 | **No** (sent to Gemini) | Optional (spending opt-in and brief source both off by default) | App functionality (Gemini brief). Parsing of bank notifications itself stays on-device |
| **Health & Fitness** | Fitness info (Steps, distance, calories) | Yes | **No** | **Yes** | Optional (requires Health Connect permission) | App functionality (displaying daily activity summary on Today hub) |
| **Health & Fitness** | Health info (Sleep duration, heart rate) | Yes | **No** | **Yes** | Optional (requires Health Connect permission) | App functionality (displaying health metrics on Today hub) |
| **Calendar** | Calendar events (today's event titles and times) | Yes, when the Gemini brief is on | See §1.3 | **No** (sent to Gemini) | Optional (calendar permission, Gemini key, and the brief's calendar source) | App functionality (Gemini brief). Agenda display on the Today hub is on-device only |
| **Messages** | Other in-app messages / notification text | Yes | **No** | **Yes** | Optional (requires notification listener permission) | App functionality (Inbox hub alerts, chat summaries, and spending parser) |
| **App Info & Performance** | Installed apps | Yes | **No** | **Yes** | **Required** | Core launcher functionality (discovering, launching, and organizing apps) |
| **Personal Identifiers** | Name, Email, Phone, Device IDs | **No** | **No** | N/A | N/A | Meena collects zero personal identifiers or device IDs |

### 1.3 Gemini brief: data sent to Google

Source: `GeminiBriefRepository.buildContextString` and `generateContent` (endpoint `generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`).

- **When:** only after the user saves their own Gemini API key. The brief then refreshes automatically when the Today hub opens (cached for 2 hours) and when the user taps it.
- **What is sent, per ticked source:**

| Brief source | Default | Sent to Google |
|---|---|---|
| calendar | on | today's event times and titles |
| weather | on | temperature, condition, place name |
| solat | on | next prayer name and time |
| spending | off | number of payments today and total in rm |

- **Not ephemeral:** under the [Gemini API Additional Terms](https://ai.google.dev/gemini-api/terms) (effective 23 Mar 2026), Unpaid Services content is used by Google "to provide, improve, and develop Google products" and "human reviewers may read, annotate, and process your API input and output." Data sent by the brief must therefore not be marked as processed ephemerally.
- **Collected vs shared:** per [Provide information for Google Play's Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469), data transmitted off the device counts as **collected**. Transfers "based on a specific user-initiated action, where the user reasonably expects the data to be shared, or based on a prominent in-app disclosure and consent" do not need to be declared as **shared**. The app shows a disclosure (`disclosure_gemini_body`) before setup, so "not shared" may apply. **[S] Owner to decide**; declaring "shared" is the conservative choice.
- **Disclosure:** the in-app text says details are sent to Google when generating a brief. It does not say this happens automatically when the Today hub opens. **[S] Owner to decide** whether to add that.

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
