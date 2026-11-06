# VIVO LITE 1.5.7 — Premium motion and depth

VIVO LITE is a native, lightweight Android launcher tuned for the Vivo Y21L (Android 5.1.1 / API 22). It replaces the home-screen experience; it does **not** replace Android or install a custom ROM.

## Included experience

- Three static gradient wallpapers with a restrained accent colour
- Dark or light glass surfaces and adjustable app icon sizes
- Multi-page app grid, app search, folders, favourites dock and drag-to-organise
- Native Android widget hosting with per-widget height controls
- Control Centre with brightness, media volume, Wi-Fi, Bluetooth and flashlight shortcuts
- Optional ADW-compatible icon packs
- App-library scans pause when the launcher leaves the foreground and resume when it returns
- A failed package or icon-pack refresh preserves the last working app list and clears the loading state instead of leaving the home screen stuck
- App-grid cells use a single custom-drawn View instead of a nested layout, image view and text view; click, long-press, drag and accessibility descriptions remain supported
- Home wallpaper is rendered once per frame rather than redundantly by both the root and its parent frame
- App labels follow the selected light/dark text palette and use a consistent medium typeface; folder previews use cleaner spacing and matching glass surfaces
- Low-RAM devices rasterize icon backing bitmaps at a reduced resolution while retaining the existing on-screen icon size
- Notification app icons use the same restrained icon-plate treatment as the home grid
- Low-RAM devices use smaller icon backing bitmaps to reduce per-icon ARGB memory without changing the configured visible icon size
- Home-page swipes settle after the fling naturally stops, avoiding premature snap interruptions
- Transform-only entrance motion avoids full-view alpha fades; touch feedback uses cached easing curves and cancels conflicting press animations
- Page-settle animation reuses one runnable instead of allocating a new callback on every scroll update
- Home pages keep lightweight placeholders for distant pages and populate cells in small Choreographer-scheduled batches, instead of creating a full page in one main-thread burst
- Only the current page and immediate neighbours stay populated; distant page cells are released and rebuilt when they become relevant
- A shared motion engine aligns press and release durations to the display refresh interval and uses compositor-friendly transforms
- Home-page build scheduling prioritizes the newly visible page and prefetches the next page in the actual swipe direction, so reverse swipes are not queued behind stale forward-prefetch work
- Page-cell construction uses a 2 ms per-frame time budget and a two-cell hard cap, yielding when the frame is already late instead of blindly inflating three cells on every callback
- Page-cell construction pauses during finger movement and fling settling, then resumes after the page lands
- Distant page hierarchies are trimmed only after scrolling settles, avoiding view removal and layout work during active swipes
- Programmatic page jumps keep the current page tied to the real scroll position, preventing stale-position callbacks from cancelling destination-page preparation
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
3. Download the `VIVO-LITE-V1.5.6-final-debug` artifact.
4. Extract `app-debug.apk` and install it on the Vivo Y21L.
5. Open Android's Home app settings and select **VIVO LITE** as the default Home app.

The artifact includes `app-debug.apk.sha256`; use it to verify the downloaded APK.

## Validation status

GitHub Actions verifies that the APK builds and that its package ID and version metadata match this release. A successful CI build confirms compilation and packaging, not real-device behaviour. Final acceptance still requires installing it on the Y21L and testing home selection, app launch, folders, widgets, torch access and vendor-specific quick settings.


### 1.5.6 follow-up polish

- Adds native icon-origin scale-up app launches on API 21+ with a safe fallback to the existing transition.
- Restores the compact 2×2 folder icon preview.
- Fixes the custom app-cell text measurement and home-page cell type compilation issues.
- Retains the native Java/View rendering architecture with no additional runtime dependency.


## VIVO LITE 1.5.7 — Premium motion and depth

- Full-screen launcher surfaces now enter with a 300 ms scale, fade and vertical settle using a modern cubic motion curve.
- Full-screen transitions use a temporary hardware layer, released at animation completion rather than held in memory.
- Home-page swipes use a restrained depth/parallax treatment: adjacent pages scale and fade slightly while moving, using temporary GPU layers that are released when the page settles or the launcher pauses.
- App icon plates now have an accent-tinted radial glow, a vertical glass gradient, a fine accent rim and a soft top sheen.
- Folder dialogs use a more expressive overshoot entrance and a cleaner scale-down exit.
- Press feedback is more visible and releases over a slightly longer easing curve.
- No third-party rendering engine or AndroidX animation dependency was added; the implementation uses platform APIs available from API 21.

Hardware layers consume graphics memory, so VIVO LITE enables them only for active page motion and short surface transitions. Actual smoothness and memory use still require testing on the physical Y21L.
