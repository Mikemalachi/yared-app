# Yared Hymn Tracker — Capacitor Android build

This repo builds the app into a real installable Android APK using
GitHub's own cloud servers.

## How it works

1. `www/index.html` is the entire app (single file, offline-capable).
2. `assets/icon.png` is the app icon (Saint Yared). The workflow runs
   `@capacitor/assets` to generate every required Android icon size
   and adaptive-icon layer from it automatically.
3. `native-templates/` holds three small native Android files that give
   the app **real background playback with a notification** (play/pause/
   next/previous, survives the screen turning off):
   - `PlaybackService.java` — a foreground Service that shows the
     persistent notification and keeps the process alive.
   - `PlaybackNotificationPlugin.java` — a small Capacitor plugin that
     bridges the web page's JS to that Service.
   - `MainActivity.java` — registers the plugin and requests the
     microphone + notification permissions on first launch.
3. `.github/workflows/build-android.yml` runs on every push to `main`
   (or manually via **Run workflow**) and, entirely on GitHub's
   servers:
   - installs Node, a JDK, and the Android SDK
   - adds the Android platform via Capacitor (`npx cap add android`)
   - copies `www/` into the native project (`npx cap sync android`)
   - adds the required permissions (mic, notifications, foreground
     service, wake lock) to the Android manifest
   - registers the background-playback Service in the manifest
   - copies the three files from `native-templates/` into the native
     project
   - builds a debug APK with Gradle
   - uploads the APK as a downloadable build artifact

**Why background playback needs native code:** Android suspends a
WebView's JavaScript (and any HTML5 `<audio>` playing in it) within
seconds of the screen turning off or the app going to the background —
that's an OS battery protection, not something fixable from the web
page itself. A foreground Service with a visible notification is the
only way around it, which is why this piece lives in `native-templates/`
rather than in `www/index.html`.

## Getting your APK

1. Go to the **Actions** tab of this repo.
2. Open the latest **Build Android APK** run (or click **Run workflow**
   to trigger one manually).
3. Wait for it to finish (a few minutes).
4. Scroll down to **Artifacts** and download `yared-hymn-tracker-apk`
   (it's a zip containing `app-debug.apk`).
5. Unzip it and install `app-debug.apk` — allow install-from-this-source
   the first time.

This is a debug-signed build, which is fine for installing on your own
device. It is not set up for Play Store distribution.

## Updating the app

Replace `www/index.html` with a newer version and commit — the
workflow re-runs automatically and produces a fresh APK. The native
files in `native-templates/` only need to change if you want to modify
the background-playback behavior itself.
