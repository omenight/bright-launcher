# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Bright is a minimal Android TV launcher (Kotlin, Jetpack Compose for TV), application id `com.omenight.bright`, GPL-3.0. The product goal is deliberate restraint: all apps, favorites, clock, network status, wallpaper. Push back on features that drift toward the category/sorting-heavy launchers this replaces, and keep the APK small (release is about 1 MB).

## Commands

```shell
./gradlew assembleDebug                 # debug APK, id suffix .debug so it installs next to release
./gradlew assembleRelease               # R8 minified + resource shrinking, unsigned
./gradlew lintDebug                     # report in app/build/reports/lint-results-debug.html
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity com.omenight.bright.debug/com.omenight.bright.MainActivity
```

- The Gradle daemon runs on JDK 17 via `gradle/gradle-daemon-jvm.properties` (the machine default is a newer JDK that Gradle/Kotlin may not support).
- Current AndroidX releases require compileSdk 37; the SDK lives in `/opt/android-sdk` (root-owned, packages come from the AUR, no `sdkmanager`).
- Measure performance on a real low-end box with a release build signed by the debug key (`zipalign` + `apksigner` from build-tools), never a debug build. Frame times come from `adb shell dumpsys gfxinfo <pkg> framestats`, cold start from `am start -W` medians over several alternating runs. After `adb install` the app is not yet compiled with its profile, so run `adb shell cmd package compile -f -m speed-profile <pkg>` to see what users get after idle dexopt.
- `app/src/main/baseline-prof.txt` marks the whole app package hot; Compose and AndroidX profiles are merged in by AGP.
- AGP 9 has built-in Kotlin, so only `com.android.application` and the Compose compiler plugin are applied. Versions are in `gradle/libs.versions.toml`.

## Architecture

- `data/AppRepository` lists launchable apps by querying both `LEANBACK_LAUNCHER` and `LAUNCHER` activities (TV entry point wins per package), refreshes on `LauncherApps.Callback` package events, loads card art (TV banner, else icon) lazily into an LRU cache, and launches via the leanback launch intent first. Package visibility relies on the `<queries>` block in the manifest, not `QUERY_ALL_PACKAGES`.
- `data/FavoritesStore` keeps favorites as an ordered, comma-joined package list in Preferences DataStore. Favorites of uninstalled apps are kept so a reinstall restores them.
- `HomeViewModel` combines both flows into one `HomeState`. `ui/HomeScreen.kt` holds `LauncherRoot`, which switches between two screens with a fade. Home is a fixed layout (clock pinned top-left, network status and settings gear top-right, favorites `LazyRow` at the bottom ending in an All apps tile) and must never scroll vertically. `AllAppsScreen` is the only vertically scrolling grid.
- Back and the Home button (delivered as a new intent to the `singleTask` activity) close All apps; Back on home is swallowed. Initial focus goes through `RequestFocusOnEnter`, which waits a frame because lazy items attach during layout.
- Liquid glass is baked, not live (`ui/glass/`). `WallpaperBaker` renders the wallpaper and its blurred copies once on the CPU, off the main thread (`BackdropEngine`, started in `MainActivity.onCreate` so the bake overlaps Compose startup). `Modifier.glass` draws the slice of the frosted bitmap under the element at its root position, so nothing is blurred per frame. This is deliberate. The reference box (Mali-G31, API 29) has no `RenderEffect`/`RuntimeShader`, and live blur libraries (Haze, Kyant Backdrop) either do nothing or stutter there. Glass must use rounded-rect `clip`, never `clipPath`, and must not add full-screen layers.
- Wallpapers are `WallpaperPreset` entries (a base gradient plus glows) rendered by `WallpaperBaker`, no image assets. `BackdropEngine` bakes the saved preset, or the one being previewed, off the main thread and exposes it as a `StateFlow`. Settings live in `SettingsStore` and favorites in `FavoritesStore`, both on the single DataStore in `data/Preferences.kt` (never declare a second `preferencesDataStore` for the same file).
- Settings is a glass side panel (`ui/SettingsPanel`), opened by the Menu key or the gear in the top-right corner (Up from the dock). It traps D-pad focus with `focusProperties { onExit = { cancelFocusChange() } }` only while open, and closing it returns focus to wherever it was opened from.
- `ui/WelcomeGuide` shows once (the `welcomed` flag in `SettingsStore`) and can request the home role through `data/HomeRole`. Boxes that refuse third-party homes bounce the picker straight back, so a return within a second shows the `adb set-home-activity` hint instead.
- Icons are drawn in code (`ui/Glyphs.kt`), the APK ships no icon set. The debug build is labelled "Bright Debug" via `app/src/debug/res`.
- Every tile is a `ui/BrightCard`, the single card style. Visual tokens live in `ui/theme/Theme.kt` (`BrightColors`). Focus is shown by lift (scale plus a dark drop shadow), a brighter specular rim and a one-off glint, never by borders or colored glows, and labels appear only on the focused card; keep new UI consistent with that.
