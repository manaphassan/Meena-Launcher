# Privacy Policy for Meena

**Effective Date:** 4 October 2026  
**Application:** Meena (Package: `my.hana.meena`)  
**Developer:** HaNa Innovation  
**Contact:** privacy@hana.my  

---

## 1. Overview & Architecture Charter

Meena is a personal Android launcher designed around the Meena UI principles: *clean, light, open, fast*. 

We believe that your personal phone is your private space. Unlike conventional commercial launchers, Meena is built upon a strict **zero-cloud, on-device architecture**:
- **Zero Third-Party Telemetry:** We do not include any analytics SDKs, crash trackers, advertising identifiers, or tracking pixels.
- **Zero Remote Databases:** We do not operate remote user databases, authentication servers, or logging backends.
- **Local In-Memory Processing:** All data read from your device (notifications, calendar events, bank transaction alerts, and health metrics) is processed strictly in local device memory and stays on your physical hardware.

---

## 2. Information Handled & Permissions

Meena requests only the minimal set of Android permissions necessary to provide core launcher features. Each permission is described below, along with its exact purpose and handling guarantees:

### 2.1 Approximate / Coarse Location (`android.permission.ACCESS_COARSE_LOCATION`)
- **Purpose:** Used solely to resolve your local prayer timetable (JAKIM zone) and query current weather and atmospheric conditions (via public Open-Meteo APIs).
- **Handling:** Only coarse (city/regional level) coordinates are retrieved ephemerally. Meena **never** requests precise GPS location (`ACCESS_FINE_LOCATION`), never monitors background location, and never saves or logs your location history.

### 2.2 Calendar Access (`android.permission.READ_CALENDAR`)
- **Purpose:** Displays your upcoming daily agenda events directly on the Today panoramic hub.
- **Handling:** Events are read locally using the Android system `CalendarContract` provider. Meena has read-only access, does not modify your calendars, and never transmits your event titles, attendees, or notes off your device.

### 2.3 Notification Listener (`android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`)
- **Purpose:** Powers the Inbox hub (alerts and active chats) and the opt-in personal spending tracker.
- **Handling:** 
  - Incoming notification text and titles are inspected strictly in memory on-device.
  - Notifications from non-whitelisted apps are immediately discarded.
  - For the opt-in spending tracker, amounts and merchant names from supported banking/e-wallet apps are parsed locally in memory.
  - No notification text, personal messages, or payment details are ever saved to plain logs or uploaded anywhere.

### 2.4 Health Connect Permissions (`androidx.health.connect.client`)
- **Requested Types:** `READ_STEPS`, `READ_DISTANCE`, `READ_TOTAL_CALORIES_BURNED`, `READ_SLEEP`, `READ_HEART_RATE`.
- **Purpose:** Displays aggregated daily fitness summaries (steps taken, distance walked, calories burned, sleep hours, resting heart rate) directly on the Today hub.
- **Handling:** 
  - Read-only access requested via Android Health Connect API.
  - Data is fetched locally and rendered in memory.
  - Health data is never transmitted across the network, never sold, and never shared with advertisers or third parties.

### 2.5 Generative AI Brief (Gemini API Integration)
- **Purpose:** Generates a concise 2–3 line daily summary brief on the Today hub.
- **Handling:** 
  - **User-Initiated Only:** The brief feature is disabled by default and requires the user to voluntarily provide their personal Google Gemini API key.
  - **Prominent Disclosure:** Before generating a brief, Meena explicitly discloses that selected context items (only the categories selected by the user, such as weather, prayer times, or agenda) are transmitted directly from your phone to Google Generative Language APIs.
  - **Key Storage:** Your API key is encrypted using hardware-backed Android Keystore cryptography (Tink AEAD) and never logged or included in backups.

### 2.6 Installed Applications (`LauncherApps` & Queries)
- **Purpose:** Core launcher functionality to discover and display installed applications on the Apps hub, and allow launching, pinning, or uninstalling apps.
- **Handling:** Applications are queried locally via the Android system `LauncherApps` API. The broad `QUERY_ALL_PACKAGES` permission has been completely eliminated in favor of targeted intent filters. Your app list is never shared or profiled.

---

## 3. Data Storage & Security

- **Android Keystore Encryption:** API keys, custom finance holdings, and user configurations are encrypted with Android Keystore-backed keys.
- **No System Cloud Backup (`allowBackup="false"`):** Android Auto Backup is explicitly disabled to prevent sensitive settings, tokens, or locally parsed balances from syncing to cloud drive backups.
- **Data Deletion:** You can delete all locally stored settings, custom feeds, and cached data at any time by selecting **Clear Storage / Clear Data** in Android App Info or tapping **reset to defaults** in Meena Settings.

---

## 4. Financial Features Policy Compliance

Meena is not a bank, lender, credit broker, or payment processor:
- Meena does not process financial transactions, initiate money transfers, or hold customer balances.
- The optional spending widget acts strictly as an offline digital ledger, parsing transaction amounts from notification texts generated by user-installed banking apps that the user has explicitly authorized.

---

## 5. Children's Privacy

Meena is a general productivity launcher intended for individuals aged 13 and older (or the applicable age in your jurisdiction). Meena does not knowingly solicit or collect personal information from children under 13.

---

## 6. Updates & Inquiries

We may update this Privacy Policy from time to time to reflect system updates or Google Play policy modifications. Material changes will be highlighted within the launcher application settings.

For questions regarding this policy or Meena's privacy architecture, contact:
- **Email:** privacy@hana.my
- **Repository:** https://github.com/manaphassan/Meena-Launcher
