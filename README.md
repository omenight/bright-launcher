# Bright

A minimal, lightweight, open-source launcher for Android TV.

Bright does a few things and nothing else: a clock, your favorite apps, and all of your apps. No categories, no sorting modes, no ads, no tracking.

## Install as the home screen

```shell
adb install bright.apk
adb shell cmd package set-home-activity com.omenight.bright/.MainActivity
```

To go back to the stock launcher, run `set-home-activity` again with its component
(`adb shell cmd package query-activities -c android.intent.category.HOME -a android.intent.action.MAIN` lists them).

## Usage

- **OK** opens an app.
- **Hold OK** opens the app menu: add to or remove from Favorites, hide it, or open its system app info.
- **Menu**, or **Up** to the gear and **OK**, opens settings with Wallpaper, Hidden apps, System settings and the Bright version row. In the wallpaper list, moving over one previews it, **OK** keeps it and **Back** returns to the menu.
- Bright checks GitHub releases every few hours and downloads a new version in the background. A dot on the gear means it is ready, the **Bright** row in settings installs it. The first install asks you to allow Bright as an app source.
- On first launch a short guide explains these and offers to make Bright the default home.

## Building

Requires JDK 17 and the Android SDK with platform 37.

```shell
./gradlew assembleDebug     # app/build/outputs/apk/debug/
./gradlew assembleRelease   # minified, unsigned unless a release key is configured
```

To sign release builds, put a `keystore.properties` next to `settings.gradle.kts` (it is gitignored):

```properties
storeFile=/path/to/release.p12
storePassword=...
keyAlias=...
keyPassword=...
```

## License

GPL-3.0, see [LICENSE](LICENSE).
