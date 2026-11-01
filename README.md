# VIVO LITE V0.1

A minimal Android home-screen prototype for the Vivo Y21L. This is an Android app/launcher, **not a replacement operating system or ROM**. It does not modify partitions, bootloader, recovery, or firmware.

## What it currently does
- Dark minimal home screen
- Clock and date
- Three-column grid of installed launchable apps
- Tap an app to launch it
- Can be selected as the Android Home app

## Build without installing development tools on your PC
1. Create a GitHub repository named `VIVO-LITE`.
2. Upload the contents of this project to the repository (keep the `.github` folder).
3. Open the repository's **Actions** tab.
4. Select **Build VIVO LITE APK** and run it, or push to the `main` branch.
5. Wait for the workflow to finish successfully.
6. Open the completed workflow run and download the `VIVO-LITE-V0.1-debug` artifact.
7. Extract the ZIP; it contains `app-debug.apk`.
8. Transfer the APK to the Vivo Y21L and install it. Android may ask you to allow installation from that source.

## Notes
- Minimum Android version declared: API 21.
- The project uses Java and Android's built-in UI classes; no third-party UI libraries.
- The APK is unsigned for Play Store distribution (Android can install a debug APK).
- Build dependencies are downloaded by the hosted GitHub runner.
