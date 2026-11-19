# Google Play Store Release & Rollout Guide

**Application:** Meena (`my.hana.meena`)  
**Release Version:** 0.2.1 (versionCode: 3)
**Track Schedule:** Internal Test → Closed Test (14 days) → Production Staged Rollout
**Document Date:** October 2026 (checklist rerun 13 November 2026; updated 16 November 2026 for 0.1.0; 18 November 2026 for 0.2.0; 19 November 2026 for 0.2.1)  

---

## 1. Release Build Verification Checklist

Before uploading the release bundle to Google Play Console, confirm all verification gates:

Rechecked 19 Nov 2026 in two places. The published 0.2.1 APK (`meena-0.2.1.apk` from the GitHub release, tag `v0.2.1`, SHA-256 `e279d5ca…788173035`) was checked for signing, 16 KB alignment, badging and dex contents. A fresh release build of `phase/10-signoff` at `6630243` (after 0.2.1, not released) was used for tests, lint, sizes and the smoke test. That build still says 0.2.1, version code 3, so **bump to version code 4 before the next release**.

- [x] Package Name: `my.hana.meena` (Permanent unique identifier)
- [x] Compilation: Target SDK 36, Min SDK 24, Compose 2026 BOM
- [x] Bundle Output: `app/build/outputs/bundle/liveRelease/app-live-release.aab` (`./gradlew clean bundleLiveRelease assembleLiveRelease --no-build-cache`, 19 Nov 2026, `6630243`)
- [x] Bundle Size: **7.51 MiB** (7,875,142 bytes, `6630243`; within the 15 MB charter limit). Release APK 4.06 MB (4,056,290 bytes); the published 0.2.1 APK is 4.04 MB (4,036,902 bytes). ARM only (`abiFilters` arm64-v8a, armeabi-v7a; owner, 18 Nov 2026)
- [x] Baseline profile: `baseline.prof` packaged in the bundle and in both APKs (19 Nov 2026)
- [x] Release signing: bundle and APK signed with the upload key (`CN=HaNa Innovation, O=HaNa Innovation, C=MY`, SHA-256 `b9e59f8c…696d`, checked with `apksigner verify` on the published 0.2.1 APK and the `6630243` APK, 19 Nov 2026). Same key since 0.1.0, so each release installs over the last. Turn on Play App Signing at the first upload
- [x] Privacy policy URL live: https://manaphassan.github.io/Meena-Launcher/privacy-policy/ (GitHub Pages, `gh-pages` branch). Republished 19 Nov 2026 (`e63b954`) with live faces (two names, email subject, music tile) and coin prices from Frankfurter; HTTP 200, checked live
- [x] R8 & ProGuard: Minification enabled, resource shrinking enabled. No `com.google.mlkit`, `datatransport` or Firebase classes in the release (checked in `mapping.txt` and the dex files of the `6630243` build, and the dex of the published 0.2.1 APK, 19 Nov 2026)
- [x] 16 KB Page Alignment: `zipalign -c -P 16` passes on the published 0.2.1 APK and the `6630243` APK (19 Nov 2026). On 0.2.0 every `LOAD` segment was 16 KB aligned in all eight native libraries (`libandroidx.graphics.path.so`, `libdatastore_shared_counter.so`, and CameraX's `libimage_processing_util_jni.so` and `libsurface_util_jni.so`, for both ABIs) (18 Nov 2026); not rechecked per library since
- [x] Automated Tests: `testLiveDebugUnitTest` 286 and `testDemoDebugUnitTest` 286, 0 failed (`6630243`, 19 Nov 2026)
- [x] Android Lint (`lintLiveDebug`): 0 errors, 166 warnings (`6630243`, 19 Nov 2026). Fixed for 0.2.0: shortcut calls guarded for Android 7.0, notification message pictures limited to Android 11+, backup messages read before the file picker opens
- [x] Camera features: `android.hardware.camera` and `.autofocus` are `uses-feature-not-required` in `aapt2 dump badging`, so Play won't hide Meena from phones without them. The `6630243` APK has the same package line, permissions, features and ABIs as the published 0.2.1 APK (19 Nov 2026)
- [x] Release smoke test (19 Nov 2026), `6630243` release APK on a TECNO KL8 (Android 14): `adb install -r` over the published 0.2.1 kept its data (first install date unchanged), and it opened as the home app with nothing in the crash log. The owner checked live faces, including the music tile, on the demo build of the same code. Not run on the Sony this time
- [x] Release smoke test (18 Nov 2026), 0.2.0 release APK on a TECNO KL8 (Android 14): installs over the earlier release build and keeps its data, no crashes. The owner checked the ZXing scanner, a WhatsApp "add shortcut" tile, adding an app widget and the widget previews. Screen time checked on the device by the agent (8h 44m at 11:10 pm, hourly chart). Not seen on screen: the now playing page (nothing was playing). Sony SO-02J (Android 8.0): installs, but Family Link blocked it from opening

---

## 2. Release Track 1: Internal Testing Track

The Internal Testing track provides instant distribution (bypassing Google Play standard review queues) for developer device verification.

1. **Navigate:** Google Play Console $\to$ **Testing** $\to$ **Internal testing**.
2. **Create Release:**
   - Upload `app-live-release.aab`.
   - Release name: `0.2.1 (3) - Internal Pre-Release`.
   - Release notes: Copy from `distribution/metadata/en-US/release_notes.txt`.
3. **Tester List:** Add developer Google accounts to the internal tester email list.
4. **Accept & Install:** Open the internal opt-in link on your Android test devices and install.

---

## 3. Release Track 2: Closed Testing (Personal Account Mandate)

> [!IMPORTANT]
> Because Meena uses a personal Google Play Developer account, Google Play mandates that **at least 12 testers must be opted in to your closed test continuously for at least 14 days** before you can apply for production access.

### 3.1 Setup Steps
1. **Navigate:** Google Play Console $\to$ **Testing** $\to$ **Closed testing**.
2. **Create Track:** Name the track (e.g. `Meena Alpha Testers`).
3. **Testers:**
   - Create an email list containing $\ge 15$ participants (allowing a margin over the 12 required).
   - Ensure all testers open the web/Android invitation link and tap **Become a Tester**.
4. **Create Release:**
   - Promote the build from Internal testing or upload `app-live-release.aab`.
   - Submit for Google Play closed testing review.
5. **14-Day Monitoring:**
   - Once approved, verify all 12+ testers download or keep the app installed.
   - Track active opt-ins in Play Console: **Closed testing $\to$ Track status**.
   - Do not remove testers during the continuous 14-day window.

---

## 4. Production Application & Staged Rollout

### 4.1 Applying for Production Access
After 14 continuous days with 12+ opted-in testers, a button will appear on the Dashboard: **Apply for production**.
Google will ask questions regarding:
- How you recruited your testers.
- What feedback you received (e.g., UI performance on different screen sizes, battery efficiency).
- What bugs or improvements were implemented during testing.

### 4.2 Staged Rollout Strategy
Once production access is granted, release Meena using a progressive staged rollout to safeguard user experience:

| Stage | Duration | Target Percentage | Monitoring Gate |
|---|---|---|---|
| **Phase 1** | 48 hours | **10%** | Monitor Crash rate (< 1.09%) and ANR rate (< 0.47%) in Android Vitals |
| **Phase 2** | 48 hours | **20%** | Inspect user reviews and device battery reports |
| **Phase 3** | 48 hours | **50%** | Verify backend quote API response caps and RSS stability |
| **Phase 4** | Ongoing | **100%** | Full worldwide availability |

---

## 5. Post-Launch Maintenance

- **Health Connect Scopes:** Check Google Play Console declarations for any updated Health Connect health categories.
- **Freshness Review:** Monitor user-configured RSS feeds and ensure zero memory leaks.
