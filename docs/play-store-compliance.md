# Google Play Console Compliance & Data Safety Guide

**Application:** Meena (`my.hana.meena`)  
**Target SDK:** 36 (Android 16 / 15)  
**Track:** Closed Test (Personal Developer Account)  
**Document Date:** October 2026 (updated 12 November 2026: Gemini brief removed; 13 November 2026: map, news pictures, rain outlook, live tiles, icon packs, Bluetooth battery; 14 November 2026: Data safety rows checked against Play's help page, rain radar link gone, backup and transfer excluded; 15 November 2026: markets prices, double tap to lock through device admin; 18 November 2026: 0.2.0 scanner with camera and ZXing, music controls, quick replies, notification pictures, app shortcuts, wallpaper)  

---

## 1. Google Play Data Safety Form Questionnaire Responses

Use the following exact values when completing the Google Play Console **Data Safety** section:

### 1.1 Overview Questions
- **Does your app collect or share any of the required user data types?**  
  **Yes.** Location rounded to about 1 km is sent to Open-Meteo for weather, the rain outlook and air quality. The map widget also loads map tiles from OpenFreeMap (its libraries are built into the app), and the tiles requested reveal the area around the user [I]. Everything else listed below is only processed on the device.
- **Is all of the user data collected by your app encrypted in transit?**  
  **Yes** (all requests for weather, map tiles, news RSS and news pictures, solat schedules, exchange rates, markets and kijang emas prices use HTTPS; cleartext traffic is blocked by default at target SDK 36).
- **Do you provide a way for users to request that their data be deleted?**  
  **Yes** (users can clear all data directly via Android system App Settings or the in-app "reset to defaults" button; no remote servers store user data).

---

### 1.2 Data Type Breakdown Table

Checked against Play's [Data safety help](https://support.google.com/googleplay/android-developer/answer/10787469) on 14 Nov 2026:
- "Collect" means sending data off the device.
- Data that is only processed on the device "does **not** need to be disclosed".
- Location sharper than 3 km² counts as **precise**, including location that can be inferred. Meena rounds coordinates to 2 decimals, a cell of about 1.1 × 1.1 km (about 1.2 km²), so what reaches Open-Meteo, and the map tiles fetched around the user, count as precise.

**Declare in the form (the only data that leaves the phone):**

| Data Category | Data Type | Collected? | Shared? | Processed Ephemerally? | Required or Optional? | Purpose |
|---|---|---|---|---|---|---|
| **Location** | Precise location (rounded to about 1 km, and inferred from map tiles) | Yes | **No** [I] | **Yes** | Optional (fallback to manual solat zone; weather and map show "needs setup") | App functionality (local weather, rain outlook, air quality, JAKIM prayer zone, nearby map tiles) |
| **Location** | Approximate location (when the user picks approximate on Android 12+) | Yes | **No** [I] | **Yes** | Optional | Same as above |

[I] "Shared: No": Open-Meteo and OpenFreeMap get coordinates or tiles only to answer the request, and the user turned the feature on. Play's help lists two exceptions to "sharing": service providers, and transfers based on a user-initiated action. Both fit, but that reading is mine, not Google's.

**On the phone only, not declared** (per the help page above): parsed bank alerts, Health Connect readings, calendar events, notification and chat text, notification pictures, replies typed in the inbox, what's playing, installed apps and their shortcuts, app-open counts, Bluetooth device names and battery levels, camera frames and saved scans, the chosen wallpaper, notes and holdings. None of these is sent anywhere. The privacy policy still lists them, so users can see what Meena reads.

| Data | Where it stays |
|---|---|
| Purchase history (parsed bank alerts) | Phone only; spending widget is opt-in, off by default |
| Fitness and health info (Health Connect) | Phone only; shown on Today |
| Calendar events | Phone only |
| Notification and chat text | Phone only; chat names on live tiles are emoji-stripped and never sent |
| Notification pictures, inbox replies, what's playing | Phone only; a reply is handed to the posting app's own reply action, which sends it |
| Camera frames, saved scans | Phone only; frames are decoded in memory by ZXing and dropped; saved scans (max 50) are Keystore-encrypted |
| Chosen wallpaper | Phone only; read through Android's picker, not copied |
| Installed apps, app-open counts | Phone only |
| Bluetooth device names and battery | Phone only; widget off by default |
| Personal identifiers, device IDs | Not collected at all |

**Not user data (no row needed):** news pictures are fetched from the feed sites the user added. Links the user taps ("open on" for news, "open in maps") open in the user's own browser or app, and Meena sends those sites nothing.

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

- **URL for Play Console (and the in-app link):** https://manaphassan.github.io/Meena-Launcher/privacy-policy/
- **Hosting:** GitHub Pages from the `gh-pages` branch (static HTML rendered from `docs/privacy-policy.md` by GitHub's Markdown API). Checked live on 14 Nov 2026: HTTP 200, public, not a PDF. After editing the policy, rebuild and push `gh-pages` so the page matches: `bash scripts/publish-privacy-policy.sh`, then `git -C build/tmp/ghpages push`.
- **Shown in the app:** settings, then personalize, then privacy, then **privacy policy**; and from Health Connect's "privacy policy" link.
- **Play's requirement:** an active, public, non-geofenced, non-PDF page ([User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)). The Pages URL above meets it; the old `github.com/.../blob/main/...` link returned 404 and is no longer used.

### 2.4 Nearby Devices (`BLUETOOTH_CONNECT`, plus `BLUETOOTH` with `maxSdkVersion=30`)
- **Use:** the optional connected devices widget lists Bluetooth devices that are already connected (`BluetoothManager.getConnectedDevices(GATT)`, the A2DP, HEADSET and input-device (HID host) profile proxies, and link broadcasts) and shows their names. It also shows a device's battery level when Android reports one in its battery broadcast. No scanning (`BLUETOOTH_SCAN` is not requested), no connecting, no location use.
- **Prominent disclosure:** a yes/no dialog (`disclosure_bluetooth_*`) explains what is read before the system permission prompt, and only appears when the user turns the widget on.

### 2.6 Device admin (double tap to lock)
- **Use:** `MeenaDeviceAdminReceiver` declares only `<force-lock/>` (`res/xml/device_admin.xml`) and calls `DevicePolicyManager.lockNow()` when the user double taps an empty spot. Off by default; turning it on opens Android's own `ACTION_ADD_DEVICE_ADMIN` page with an explanation; turning it off calls `removeActiveAdmin()`. Approved by the owner on 15 Nov 2026. `BIND_DEVICE_ADMIN` protects the receiver; it is not a `uses-permission`.
- **Platform:** force-lock is still available to ordinary (non device owner) apps ([device admin deprecation](https://developers.google.com/android/work/device-admin-deprecation): "The following policies will continue to be available... USES_POLICY_FORCE_LOCK"). After `lockNow()` "the device must be unlocked using strong authentication (PIN, pattern, or password)" ([DevicePolicyManager](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#lockNow())); the settings page says so.
- **Play policy:** no declaration form for device admin was found in the Play Policy Center. The [Malware policy](https://support.google.com/googleplay/android-developer/answer/9888380) asks apps to "obtain explicit user consent for any device management features", "don't use device admin features to block uninstallation" and "provide a clear and easy way for users to remove your app". Meena: consent through Android's own page, one switch to turn it off, and the settings page says to turn it off before uninstalling. Whether no other page (User Data, Mobile Unwanted Software) adds a requirement is [S] unverified.
- **Data safety:** nothing is collected; no row needed.

### 2.7 Markets prices
- **Source:** Frankfurter v2 blended rates (`api.frankfurter.dev/v2/rates`, base MYR, quotes XAU, BTC, ETH, XAG), keyless. Its [FAQ](https://frankfurter.dev) says blended rates are "free to use" commercially, caching is "encouraged", and requests "qualify as Data Not Collected under Apple and Google Play guidelines". No attribution is required; the widget names the source anyway.
- **Financial features declaration:** Meena only displays prices; it is not an exchange or wallet. Whether showing crypto prices changes any answer in Play's financial features declaration is [S] unverified; the owner checks the form when filling it in.

### 2.8 Kijang Emas prices
- **Source:** Bank Negara Malaysia Open API, keyless: `https://api.bnm.gov.my/public/kijang-emas/year/{y}/month/{m}` with `Accept: application/vnd.BNM.API.v1+json` (without it the API answers 404). One call per refresh, two at the start of a month; at most every 3 hours. An empty month answers 404 "No records found.", read as "no prices yet".
- **Terms:** the API portal (apikijangportal.bnm.gov.my) blocks automated readers, so its terms of use and rate limits are [S] unverified by the agent. The owner approved building it on 15 Nov 2026; read the terms in a browser before release.
- **Labels:** BNM's "selling" is shown as "you pay" and "buying" as "you get back". It is the coin's price, not spot gold, and the widget says so.

### 2.9 Camera (scanner)
- **Use:** `android.permission.CAMERA`, approved by the owner, asked with Android's runtime prompt the first time the scanner opens. CameraX 1.4.1 previews and analyses frames; ZXing core 3.5.4 decodes the brightness plane in memory. No frame is saved or sent. `android.hardware.camera` and `android.hardware.camera.autofocus` are declared `required="false"`, because Play otherwise treats them as required and hides the app from phones without them ([uses-feature](https://developer.android.com/guide/topics/manifest/uses-feature-element), "Permissions that imply feature requirements").
- **Libraries:** ZXing core is Apache 2.0 ([POM](https://repo1.maven.org/maven2/com/google/zxing/zxing-parent/3.5.4/zxing-parent-3.5.4.pom)) and has no network code. ML Kit was tried first and removed before release: Google's [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure) says its SDKs collect device and app information, per-installation identifiers and performance metrics "for diagnostics and usage analytics", which Meena's no-analytics rule does not allow. The 0.2.0 release has no `com.google.mlkit`, `datatransport` or Firebase classes (checked in `mapping.txt` and the dex files).
- **Data safety:** nothing leaves the phone; no row needed.

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
  4. A markets widget showing daily reference prices for gold, silver, bitcoin and ether (Frankfurter), labelled "a rough guide, not for buying or selling". No trading, wallets or advice.
  5. A kijang emas widget showing Bank Negara Malaysia's published buy and sell prices for the Kijang Emas coin.

---

## 5. Target Audience & Content Rating

- **Target Audience:** 13 years and older. Meena is not specifically targeted toward children. (Was 18+ only while the Gemini brief existed; see plan D13.)
- **Content Rating (IARC):** **Everyone** (Contains no violence, sexual content, gambling, user-generated content, or uncensored social interactions).
