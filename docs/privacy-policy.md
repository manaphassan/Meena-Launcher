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
- **Local In-Memory Processing:** Notifications, bank transaction alerts and health metrics are processed on your device and never leave it. Calendar events, weather and a spending total leave your device only if you turn on the optional Gemini brief and choose to include them (see 2.5).

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
| Google Gemini API | You add your own Gemini key | The brief items you tick (see 2.5) |

---

## 2. Information Handled & Permissions

Meena requests only the minimal set of Android permissions necessary to provide core launcher features. Each permission is described below, along with its exact purpose and handling guarantees:

### 2.1 Approximate / Coarse Location (`android.permission.ACCESS_COARSE_LOCATION`)
- **Purpose:** Used to resolve your local prayer timetable (JAKIM zone), query current weather and air quality (via public Open-Meteo APIs), and show the optional map widget.
- **Handling:** Only coarse (city/regional level) coordinates are used. Meena **never** requests precise GPS location (`ACCESS_FINE_LOCATION`), never monitors background location, and does not keep a location history. If the Gemini brief includes weather, your place name is sent to Google with the brief (see 2.5).

### 2.2 Calendar Access (`android.permission.READ_CALENDAR`)
- **Purpose:** Displays your upcoming daily agenda events directly on the Today panoramic hub.
- **Handling:** Events are read locally using the Android system `CalendarContract` provider. Meena has read-only access and does not modify your calendars. Attendees and notes never leave your device. If you turn on the Gemini brief with its calendar source ticked, the titles and times of today's events are sent to Google to write the brief (see 2.5).

### 2.3 Notification Listener (`android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`)
- **Purpose:** Powers the Inbox hub (alerts and active chats) and the opt-in personal spending tracker.
- **Handling:** 
  - Incoming notification text and titles are inspected strictly in memory on-device.
  - Notifications from non-whitelisted apps are immediately discarded.
  - For the opt-in spending tracker, amounts and merchant names from supported banking/e-wallet apps are parsed locally in memory.
  - No notification text, personal messages, or payment details are ever saved to plain logs or uploaded anywhere. If you tick spending as a Gemini brief source (off by default), only today's number of payments and total amount are sent to Google.

### 2.4 Health Connect Permissions (`androidx.health.connect.client`)
- **Requested Types:** `READ_STEPS`, `READ_DISTANCE`, `READ_TOTAL_CALORIES_BURNED`, `READ_SLEEP`, `READ_HEART_RATE`.
- **Purpose:** Displays aggregated daily fitness summaries (steps taken, distance walked, calories burned, sleep hours, resting heart rate) directly on the Today hub.
- **Handling:** 
  - Read-only access requested via Android Health Connect API.
  - Data is fetched locally and rendered in memory.
  - Health data is never transmitted across the network, never sold, and never shared with advertisers or third parties.

### 2.5 Generative AI Brief (Gemini API Integration)
- **Purpose:** Generates a short 2–3 line daily summary on the Today hub.
- **Who can use it:** You must be 18 or older, as required by Google's [Gemini API Additional Terms of Service](https://ai.google.dev/gemini-api/terms).
- **Handling:** 
  - **Off by default:** The brief stays off until you enter your own Google Gemini API key. Meena shows a disclosure explaining what is sent before you set it up.
  - **What is sent:** Only the sources you tick: today's calendar event titles and times, current weather and place name, the next prayer name and time, and (off by default) today's number of payments and total spent. Requests go directly from your phone to Google's Generative Language API using your key.
  - **When it is sent:** When the Today hub opens (at most once every 2 hours) and whenever you tap the brief.
  - **How Google uses it:** Your requests are governed by Google's terms for your API key, not by Meena. On the free tier, Google states it uses submitted content to improve its products and that human reviewers may read it. Do not include sources you consider private.
  - **Key Storage:** Your API key is encrypted with an AES-256-GCM key held in the Android Keystore, and is never logged or included in backups. Removing the key in Settings stops all requests.

### 2.6 Installed Applications (`LauncherApps` & Queries)
- **Purpose:** Core launcher functionality to discover and display installed applications on the Apps hub, and allow launching, pinning, or uninstalling apps.
- **Handling:** Applications are queried locally via the Android system `LauncherApps` API. The broad `QUERY_ALL_PACKAGES` permission has been completely eliminated in favor of targeted intent filters. Your app list is never shared or profiled.

---

## 3. Data Storage & Security

- **Android Keystore Encryption:** Your Gemini API key, portfolio holdings and Google Sheet link are encrypted with an AES-256-GCM key held in the Android Keystore. Holdings saved by earlier versions are encrypted automatically the next time you open the money page.
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

Meena is intended for people aged 18 and older. Meena is not directed at children and does not knowingly collect personal information from anyone under 18.

---

## 6. Updates & Inquiries

We may update this Privacy Policy from time to time to reflect system updates or Google Play policy modifications. Material changes will be highlighted within the launcher application settings.

For questions regarding this policy or Meena's privacy architecture, contact:
- **Email:** privacy@hana.my
- **Repository:** https://github.com/manaphassan/Meena-Launcher
