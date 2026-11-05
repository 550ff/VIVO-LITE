# VIVO LITE V1.4 — Premium Experience

A refined, lightweight Android launcher for the Vivo Y21L (Android 5.1.1 / API 22). VIVO LITE focuses on a calm, premium home screen while keeping the implementation native and practical for older hardware.

## Experience
- Deep midnight gradient wallpapers with subtle accent lighting
- Dark glass surfaces with quieter borders and consistent typography
- Refined home screen, clock, app collection, dock and page navigation
- Redesigned control centre with quick controls, brightness and media volume
- App library, folders, widgets, icon packs, notifications and Smart Edge tools
- Local preferences; no ads, analytics, network calls or background service

## Compatibility
- Minimum SDK 21; target SDK 28
- Native Java Android Views; no third-party UI framework
- Static gradient rendering; no live blur or continuously running wallpaper animation
- Wi-Fi and Bluetooth toggles are best-effort and may be restricted by vendor firmware
- Notification access and brightness modification require explicit Android system permission
- This is a launcher, not a replacement Android OS or custom ROM

## Build with GitHub Actions
1. Open **Actions** → **Build VIVO LITE APK**.
2. Run the workflow on branch `v1.4-premium-experience` (or push a commit to that branch).
3. Download the `VIVO-LITE-V1.4-premium-debug` artifact.
4. Extract `app-debug.apk`, install it on the Vivo Y21L and select VIVO LITE as the Home app.

## Testing note
A successful GitHub Actions build confirms compilation only. Real-device testing is still required for launcher selection, widgets, torch access, notification access, and vendor-specific quick settings behaviour.