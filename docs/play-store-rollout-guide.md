# Google Play Store Release & Rollout Guide

**Application:** Meena (`my.hana.meena`)  
**Release Version:** 0.1.0 (versionCode: 1)
**Track Schedule:** Internal Test → Closed Test (14 days) → Production Staged Rollout
**Document Date:** October 2026 (checklist rerun 13 November 2026; updated 16 November 2026 for 0.1.0)  

---

## 1. Release Build Verification Checklist

Before uploading the release bundle to Google Play Console, confirm all verification gates:

- [x] Package Name: `my.hana.meena` (Permanent unique identifier)
- [x] Compilation: Target SDK 36, Min SDK 24, Compose 2026 BOM
- [x] Bundle Output: `app/build/outputs/bundle/liveRelease/app-live-release.aab` (`./gradlew bundleLiveRelease`, rerun 16 Nov 2026 at commit `78e60c5`)
- [x] Bundle Size: **5.72 MiB** (5,998,847 bytes; within the 15 MB charter limit). Release APK 2.96 MB (2,955,481 bytes)
- [x] Baseline profile: `baseline.prof` packaged in the bundle
- [x] Release signing: bundle and APK signed with the upload key (`CN=HaNa Innovation, O=HaNa Innovation, C=MY`, SHA-256 `b9e59f8c…696d`, checked with `apksigner verify` and `keytool -printcert`, 16 Nov 2026). Turn on Play App Signing at the first upload
- [x] Privacy policy URL live: https://manaphassan.github.io/Meena-Launcher/privacy-policy/ (GitHub Pages, `gh-pages` branch; HTTP 200 on 14 Nov 2026)
- [x] R8 & ProGuard: Minification enabled, resource shrinking enabled
- [x] 16 KB Page Alignment: `zipalign -c -P 16` passes and every `LOAD` segment is 16 KB aligned for `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so` (checked 12 Nov on the live debug APK; the 13 Nov bundle still ships only these two libraries)
- [x] Automated Tests: `testLiveDebugUnitTest` 238 and `testDemoDebugUnitTest` 163, 0 failed (16 Nov 2026)
- [x] Android Lint (`lint`): 0 errors, 136 warnings (mostly `UnusedResources`) (16 Nov 2026)
- [x] Release smoke test: R8 release APK installed on the emulator; today, money (markets, kijang emas, rates), add ticker and the device admin prompt all work, no crashes (16 Nov 2026)

---

## 2. Release Track 1: Internal Testing Track

The Internal Testing track provides instant distribution (bypassing Google Play standard review queues) for developer device verification.

1. **Navigate:** Google Play Console $\to$ **Testing** $\to$ **Internal testing**.
2. **Create Release:**
   - Upload `app-live-release.aab`.
   - Release name: `0.1.0 (1) - Internal Pre-Release`.
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
