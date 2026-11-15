# Privacy Policy for Meena

**Effective Date:** 14 November 2026  
**Last Updated:** 15 November 2026 (new contact email, shake to hide, email section, markets prices, double tap to lock)  
**Application:** Meena (Package: `my.hana.meena`)  
**Developer:** HaNa Innovation  
**Contact:** harussani@outlook.my  

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
| Open-Meteo (weather, rain outlook and air quality) | Weather or air quality widget is on | Coordinates rounded to about 1 km |
| OpenFreeMap map tiles (may be served through Cloudflare) | Map widget is on | The map area you are looking at, around your location. The map libraries (Leaflet, MapLibre) are built into the app, so nothing else is fetched to draw it. |
| JAKIM e-Solat | Prayer times widget is on | Your prayer zone code |
| Frankfurter (exchange rates, and daily gold, silver, bitcoin and ether prices) | Rates or markets widget is on | Your base currency (ringgit) and which prices to look up; nothing about you |
| Google Sheets | You link a sheet | A request for the sheet you linked |
| News feeds you add | News page is on | A request for each feed, and for the pictures in its posts (thumbnails and the article reader) |
| Wikipedia (Wikimedia REST API) | Wikipedia widget is on | A request for a random article; nothing about you |

Some links open outside Meena, in your browser or another app, only when you tap them: a news article's "open on" link and links inside articles, and "open in maps". Meena sends those sites nothing; what they see is your own visit.

---

## 2. Information Handled & Permissions

Meena requests only the minimal set of Android permissions necessary to provide core launcher features. Each permission is described below, along with its exact purpose and handling guarantees:

### 2.1 Location (`android.permission.ACCESS_FINE_LOCATION`, `android.permission.ACCESS_COARSE_LOCATION`)
- **Purpose:** Used to resolve your local prayer timetable (JAKIM zone), query current weather and air quality (via public Open-Meteo APIs), and show where you are on the optional map widget.
- **Handling:** On Android 12 and later you choose precise or approximate when asked. Your location is used only while Meena is open: Meena never monitors background location (`ACCESS_BACKGROUND_LOCATION` is not requested) and does not keep a location history. Precise coordinates stay on your phone; weather and air-quality requests carry coordinates rounded to about 1 km.

### 2.2 Calendar Access (`android.permission.READ_CALENDAR`)
- **Purpose:** Displays your upcoming daily agenda events directly on the Today panoramic hub.
- **Handling:** Events are read locally using the Android system `CalendarContract` provider. Meena has read-only access, does not modify your calendars, and never sends your event titles, attendees, or notes off your device.

### 2.3 Notification Listener (`android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`)
- **Purpose:** Powers the Inbox hub (alerts and active chats), unread counts and chat names on live favourite tiles, and the opt-in personal spending tracker.
- **Handling:** 
  - Incoming notification text and titles are inspected strictly in memory on-device.
  - Live tiles show only how many notifications an app has and, for chats, who they are from (the sender or group name), never the message itself. Showing names can be turned off in settings.
  - The opt-in spending tracker only reads notifications from an allowlist of banking and e-wallet apps; amounts and merchant names are parsed locally in memory. Notifications from other apps are never parsed for spending.
  - No notification text, personal messages, or payment details are ever saved to plain logs or uploaded anywhere.

### 2.4 Health Connect Permissions (`androidx.health.connect.client`)
- **Requested Types:** `READ_STEPS`, `READ_DISTANCE`, `READ_TOTAL_CALORIES_BURNED`, `READ_SLEEP`, `READ_HEART_RATE`.
- **Purpose:** Displays aggregated daily fitness summaries (steps taken, distance walked, calories burned, sleep hours, resting heart rate) directly on the Today hub.
- **Handling:** 
  - Read-only access requested via Android Health Connect API.
  - Data is fetched locally and rendered in memory.
  - Health data is never transmitted across the network, never sold, and never shared with advertisers or third parties.

### 2.5 Nearby Devices (`android.permission.BLUETOOTH_CONNECT`; `BLUETOOTH` on Android 11 and older)
- **Purpose:** The optional connected devices widget shows which Bluetooth devices are connected right now, such as a band, watch or headphones.
- **Handling:** Meena only reads the names of devices that are already connected and, when a device reports one to Android, its battery level. It does not scan for new devices, does not connect to anything, and never logs, stores or sends device names or battery levels. The permission is asked for only when you turn the widget on, after a short explanation.

### 2.6 Apps You Open
- **Purpose:** The optional frequent apps widget shows the apps you open most from Meena.
- **Handling:** Meena counts launches it makes itself, on your phone only. It does not read Android's usage history. You can wipe the counts at any time with **clear app history** in the widget's settings.

### 2.7 Installed Applications (`LauncherApps` & Queries)
- **Purpose:** Core launcher functionality to discover and display installed applications on the Apps hub, and allow launching, pinning, or uninstalling apps.
- **Handling:** Applications are queried locally via the Android system `LauncherApps` API. The broad `QUERY_ALL_PACKAGES` permission has been completely eliminated in favor of targeted intent filters. Your app list is never shared or profiled.
- **Icon packs:** If you choose an icon pack, Meena finds installed icon packs through the same kind of targeted filters and reads the pack's own icon list from it on your phone. Nothing about your apps or the pack leaves the device.

### 2.8 Motion Sensor (shake to hide)
- **Purpose:** Shaking the phone blurs chats, email, alerts, notes and money on the home pages, and shaking again shows them.
- **Handling:** While Meena is on screen and **shake to hide** is on, it reads the accelerometer to notice a shake. The readings are used straight away and never stored or sent. This sensor needs no Android permission. Turn it off in **settings, system, shake to hide**.

### 2.9 Email Notifications
- **Purpose:** The inbox's email section lists new mail from email apps on your phone, such as Gmail.
- **Handling:** It uses the same notification access as alerts and chats (section 2.3): Meena shows the sender and subject that the email app put in its notification. It does not sign in to your email account or read your mailbox.

### 2.10 Screen Lock (device admin, double tap to lock)
- **Purpose:** If you turn on **double tap to lock**, double tapping an empty spot on a home page turns the screen off and locks the phone.
- **Handling:** Android asks you first, on its own "activate device admin app" page. Meena asks for one device admin ability only: locking the screen (`force-lock`). It cannot wipe the phone, change your password, see your PIN, or do anything else. Nothing is read, stored or sent. After a double tap lock, Android asks for your PIN, pattern or password (not your fingerprint) for the next unlock. It is off by default. While it is on, Android won't let you uninstall Meena, so turn it off first in **settings, system, double tap to lock**; that hands the ability back straight away.

---

## 3. Data Storage & Security

- **Android Keystore Encryption:** Your portfolio holdings, Google Sheet link and notes are encrypted with an AES-256-GCM key held in the Android Keystore. Holdings saved by earlier versions are encrypted automatically the next time you open the money page. Earlier versions offered an optional Gemini brief; any Gemini API key they saved is deleted when you update.
- **App-Private Storage:** Other settings and cached weather, rates, news and news pictures are stored in Meena's private app storage, which other apps cannot read.
- **No System Cloud Backup (`allowBackup="false"`):** Android Auto Backup is explicitly disabled to prevent sensitive settings, tokens, or locally parsed balances from syncing to cloud drive backups. On Android 12 and later, backup rules also exclude all app data from cloud backup and from device-to-device transfer, so nothing is copied to a new phone.
- **Data Deletion:** You can delete all locally stored settings, custom feeds, and cached data at any time by selecting **Clear Storage / Clear Data** in Android App Info or tapping **reset to defaults** in Meena Settings.

---

## 4. Financial Features Policy Compliance

Meena is not a bank, lender, credit broker, or payment processor:
- Meena does not process financial transactions, initiate money transfers, or hold customer balances.
- The optional spending widget acts strictly as an offline digital ledger, parsing transaction amounts from notification texts generated by user-installed banking apps that the user has explicitly authorized.
- The markets widget shows daily reference prices for gold, silver, bitcoin and ether, for information only. They are not live quotes and not advice, and Meena does not let you buy, sell or hold any of them. Prices of crypto assets can rise and fall sharply.

---

## 5. Age Requirement & Children's Privacy

Meena is a general productivity launcher intended for people aged 13 and older (or the minimum age in your country, if higher). Meena is not directed at children and does not knowingly collect personal information from children under 13.

---

## 6. Updates & Inquiries

We may update this Privacy Policy from time to time to reflect app changes or Google Play policy updates. When we do, the effective date at the top changes and the updated policy is published at the address below.

For questions regarding this policy or Meena's privacy architecture, contact:
- **Email:** harussani@outlook.my
- **Repository:** https://github.com/manaphassan/Meena-Launcher
- **This policy online:** https://manaphassan.github.io/Meena-Launcher/privacy-policy/
