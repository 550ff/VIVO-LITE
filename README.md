# VIVO LITE V1.0

A monochrome, glass-inspired lightweight Android launcher designed for the Vivo Y21L (Android 5.1.1 / API 22).

## Included
- Dark monochrome home screen with a bundled lightweight XML gradient wallpaper
- Clock, date, app search, app grid and favourites dock
- App folders with rename, open and remove-from-folder actions
- App long-press actions, hidden apps and app information
- Basic drag/drop folder creation and grid reordering using the API 11 `startDrag` API
- Glass-inspired control centre with brightness slider and system settings shortcuts
- Native Java Android Views and lightweight animations
- Local preferences; no network, analytics, ads or background service

## Build with GitHub Actions
1. Upload the project files to the `main` branch of `550ff/VIVO-LITE`, preserving the `.github` workflow.
2. Open **Actions** → **Build VIVO LITE APK** → **Run workflow**.
3. Open the completed run and download the `VIVO-LITE-V1.0-debug` artifact.
4. Extract `app-debug.apk` and install it on the Vivo Y21L.
5. Select VIVO LITE as the Home app when Android asks.

## Compatibility notes
- Minimum SDK 21; target SDK 28.
- Uses the legacy `View.startDrag()` API for compatibility with Android 5.1.
- Wi-Fi/Bluetooth toggles are best-effort and may be restricted by vendor firmware. Long-press or use system settings if unavailable.
- Brightness writing is best-effort; firmware may require system settings.
- Glass is simulated with translucent dark surfaces and borders rather than expensive live blur.

## Important
This is an Android launcher, not a replacement Android operating system or custom ROM. Test the debug APK on the target device before treating V1.0 as stable.
