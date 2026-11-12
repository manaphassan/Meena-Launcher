# Privacy Policy for Meena

**Effective Date:** 12 November 2026  
**Application:** Meena (Package: `my.hana.meena`)  
**Developer:** HaNa Innovation  
**Contact:** privacy@hana.my  

---

## 1. Overview & Architecture Charter

Meena is a personal Android launcher designed around the Meena UI principles: *clean, light, open, fast*. 

We believe that your personal phone is your private space. Meena does not run its own servers or collect your data:
- **Zero Third-Party Telemetry:** We do not include any analytics SDKs, crash trackers, advertising identifiers, or tracking pixels.
- **Zero Remote Databases:** We do not operate remote user databases, authentication servers, or logging backends.
- **Local In-Memory Processing:** Notifications, bank transaction alerts, calendar events and health metrics are processed on your device and never leave it.

### 1.1 Online services Meena contacts
Meena talks directly to these services from your phone; it has no servers of its own. Each service receives only what is listed here, plus your IP address as part of any internet connection.

| Service | When | What it receives |
|---|---|---|
| Open-Meteo (weather and air quality) | Weather or air quality widget is on | Approximate coordinates |
| CARTO map tiles, unpkg (map library) | Map widget is on | The map area around your approximate location |
| JAKIM e-Solat | Prayer times widget is on | Your prayer zone code |
| Frankfurter (exchange rates) | Rates widget is on | Your base currency |
| Google Sheets | You link a sheet | A request for the sheet you linked |
| News feeds you add | News page is on | A request for each feed |

---

## 2. Information Handled & Permissions

Meena requests only the minimal set of Android permissions necessary to provide core launcher features. Each permission is described below, along with its exact purpose and handling guarantees:

### 2.1 Approximate / Coarse Location (`android.permission.ACCESS_COARSE_LOCATION`)
- **Purpose:** Used to resolve your local prayer timetable (JAKIM zone), query current weather and air quality (via public Open-Meteo APIs), and show the optional map widget.
- **Handling:** Only coarse (city/regional level) coordinates are used. Meena **never** requests precise GPS location (`ACCESS_FINE_LOCATION`), never monitors background location, and does not keep a location history.

### 2.2 Calendar Access (`android.permission.READ_CALENDAR`)
- **Purpose:** Displays your upcoming daily agenda events directly on the Today panoramic hub.
- **Handling:** Events are read locally using the Android system `CalendarContract` provider. Meena has read-only access, does not modify your calendars, and never sends your event titles, attendees, or notes off your device.

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

### 2.5 Installed Applications (`LauncherApps` & Queries)
- **Purpose:** Core launcher functionality to discover and display installed applications on the Apps hub, and allow launching, pinning, or uninstalling apps.
- **Handling:** Applications are queried locally via the Android system `LauncherApps` API. The broad `QUERY_ALL_PACKAGES` permission has been completely eliminated in favor of targeted intent filters. Your app list is never shared or profiled.

---

## 3. Data Storage & Security

- **Android Keystore Encryption:** Your portfolio holdings and Google Sheet link are encrypted with an AES-256-GCM key held in the Android Keystore. Holdings saved by earlier versions are encrypted automatically the next time you open the money page. Earlier versions offered an optional Gemini brief; any Gemini API key they saved is deleted when you update.
- **App-Private Storage:** Other settings and cached weather, rates and news are stored in Meena's private app storage, which other apps cannot read.
- **No System Cloud Backup (`allowBackup="false"`):** Android Auto Backup is explicitly disabled to prevent sensitive settings, tokens, or locally parsed balances from syncing to cloud drive backups.
- **Data Deletion:** You can delete all locally stored settings, custom feeds, and cached data at any time by selecting **Clear Storage / Clear Data** in Android App Info or tapping **reset to defaults** in Meena Settings.

---

## 4. Financial Features Policy Compliance

Meena is not a bank, lender, credit broker, or payment processor:
- Meena does not process financial transactions, initiate money transfers, or hold customer balances.
- The optional spending widget acts strictly as an offline digital ledger, parsing transaction amounts from notification texts generated by user-installed banking apps that the user has explicitly authorized.

---

## 5. Age Requirement & Children's Privacy

Meena is a general productivity launcher intended for people aged 13 and older (or the minimum age in your country, if higher). Meena is not directed at children and does not knowingly collect personal information from children under 13.

---

## 6. Updates & Inquiries

We may update this Privacy Policy from time to time to reflect system updates or Google Play policy modifications. Material changes will be highlighted within the launcher application settings.

For questions regarding this policy or Meena's privacy architecture, contact:
- **Email:** privacy@hana.my
- **Repository:** https://github.com/manaphassan/Meena-Launcher
