# VIVO LITE V0.2

A lightweight native Android launcher for the Vivo Y21L (Android 5.1.1 / API 22). This is an app, not a custom ROM.

## V0.2 features
- Dark charcoal interface with restrained cyan accent
- Live clock and date
- Search installed launchable apps
- Four- or five-column app grid
- Long-press an app to open, pin/unpin favourites, view app info, or hide it
- Favourites dock
- Settings for grid, clock, dock, hidden apps, refresh, and Android Home settings
- Native Android views; no third-party UI libraries or background services

## Build using GitHub Actions
1. Upload/replace the project files in your `VIVO-LITE` repository, keeping the `.github` folder.
2. Open the repository's **Actions** tab.
3. Select **Build VIVO LITE APK** and tap **Run workflow** (or push to `main`).
4. Open the completed run and download `VIVO-LITE-V0.2-debug`.
5. Extract the downloaded artifact ZIP and install `app-debug.apk` on the Vivo Y21L.
6. Choose VIVO LITE as the Home app when Android asks.

## Compatibility
- Minimum SDK: API 21 (Android 5.0); target SDK: 28.
- Java native Android UI, no third-party libraries.
- Debug APK is intended for testing, not Play Store distribution.
