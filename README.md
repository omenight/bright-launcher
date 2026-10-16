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
- **Hold OK** pins or unpins it in Favorites.
- **Menu**, or **Up** to the gear and **OK**, opens settings. Moving over a wallpaper previews it, **OK** keeps it.
- On first launch a short guide explains these and offers to make Bright the default home.

## Building

Requires JDK 17 and the Android SDK with platform 37.

```shell
./gradlew assembleDebug     # app/build/outputs/apk/debug/
./gradlew assembleRelease   # minified, unsigned
```

## License

GPL-3.0, see [LICENSE](LICENSE).
