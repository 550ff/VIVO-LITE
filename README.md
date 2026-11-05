# VIVO LITE 1.4.2 — Final build candidate

VIVO LITE is a native, lightweight Android launcher tuned for the Vivo Y21L (Android 5.1.1 / API 22). It replaces the home-screen experience; it does **not** replace Android or install a custom ROM.

## Included experience

- Three static gradient wallpapers with a restrained accent colour
- Dark or light glass surfaces and adjustable app icon sizes
- Multi-page app grid, app search, folders, favourites dock and drag-to-organise
- Native Android widget hosting with per-widget height controls
- Control Centre with brightness, media volume, Wi-Fi, Bluetooth and flashlight shortcuts
- Optional ADW-compatible icon packs
- Local quick note and launcher preferences
- Optional notification view when Android permits notification-listener access

Smart Edge has been removed.

## Device compatibility and limits

- Minimum Android version: 5.0 (API 21); target SDK: 28
- Built with native Java Android Views; no third-party UI framework
- Wallpaper is drawn from gradients; there is no live blur or continuously running wallpaper animation
- Wi-Fi and Bluetooth changes are best-effort and can be blocked by Android or vendor firmware
- Flashlight use requires camera permission on Android 6.0 and newer; the camera is released when VIVO LITE leaves the foreground
- Notification access is optional. Android may disable notification listeners on low-RAM devices running Android 10 or earlier; VIVO LITE displays an explanation when the platform reports a low-RAM device
- Widgets depend on each widget provider and Android's app-widget host support
- Preferences stay on the device. There are no ads, analytics, or network requests. The notification listener runs only after the user explicitly enables notification access

## Build and install

1. Open **Actions** in this repository.
2. Run **Build VIVO LITE APK** on branch `v1.4-premium-experience`, or push a commit to that branch.
3. Download the `VIVO-LITE-V1.4.2-final-debug` artifact.
4. Extract `app-debug.apk` and install it on the Vivo Y21L.
5. Open Android's Home app settings and select **VIVO LITE** as the default Home app.

The artifact includes `app-debug.apk.sha256`; use it to verify the downloaded APK.

## Validation status

GitHub Actions verifies that the APK builds and that its package ID and version metadata match this release. A successful CI build confirms compilation and packaging, not real-device behaviour. Final acceptance still requires installing it on the Y21L and testing home selection, app launch, folders, widgets, torch access and vendor-specific quick settings.
