# Yared Hymn Tracker — Capacitor Android build

This repo builds the app into a real installable Android APK using
GitHub's own cloud servers.

## How it works

1. `www/index.html` is the entire app (single file, offline-capable).
2. `assets/icon.png` is the app icon (Saint Yared). The workflow runs
   `@capacitor/assets` to generate every required Android icon size
   and adaptive-icon layer from it automatically.
3. `native-templates/` holds the native Android files that give the app
   real background playback with a notification (play/pause/next/previous,
   survives the screen turning off), a daily practice reminder, and a
   home screen widget:
   - `PlaybackService.java` — foreground Service showing the persistent
     playback notification; also persists current state for the widget.
   - `PlaybackNotificationPlugin.java` — bridges the web page's JS to
     that Service.
   - `AudioFileSaverPlugin.java` — saves audio into a real
     `Music/Yared music/<month>/<celebration>/` folder structure.
   - `ReminderPlugin.java` / `ReminderReceiver.java` / `BootReceiver.java` —
     schedules a daily local notification ("N hymns due for review"),
     and reschedules it after the device reboots (alarms don't survive
     a restart on their own).
   - `DueHymnsWidgetProvider.java` + `res/layout/widget_due_hymns.xml` +
     `res/xml/due_hymns_widget_info.xml` — a home screen widget showing
     up to 3 due hymn titles plus play/pause/next/previous controls.
   - `MainActivity.java` — registers all of the above, requests the
     microphone + notification permissions on first launch, and
     captures audio files handed to the app via "Open with" from a
     file manager or a share sheet.
   - `IncomingAudioPlugin.java` — holds that captured audio for the
     JS side to pick up and feed straight into the Add wizard.
4. `.github/workflows/build-android.yml` runs on every push to `main`
   (or manually via **Run workflow**) and, entirely on GitHub's
   servers:
   - installs Node, a JDK, and the Android SDK
   - adds the Android platform via Capacitor (`npx cap add android`)
   - copies `www/` into the native project (`npx cap sync android`)
   - generates the app icon from `assets/icon.png`
   - adds the required permissions to the Android manifest
   - registers the playback Service, the reminder/boot receivers, and
     the widget provider in the manifest
   - adds the MediaStyle notification dependency to Gradle
   - copies every file from `native-templates/` into the native project
   - builds a debug APK with Gradle
   - uploads the APK as a downloadable build artifact

**Why background playback needs native code:** Android suspends a
WebView's JavaScript (and any HTML5 `<audio>` playing in it) within
seconds of the screen turning off or the app going to the background —
that's an OS battery protection, not something fixable from the web
page itself. A foreground Service with a visible notification is the
only way around it, which is why this piece lives in `native-templates/`
rather than in `www/index.html`.

**Scope notes on the reminder and widget:**
- The daily reminder uses an inexact repeating alarm (no special
  "exact alarm" permission needed) — Android may fire it within a
  window around your chosen time rather than the exact minute, which
  is a reasonable trade-off for a once-a-day reminder.
- The widget shows up to 3 due hymn titles as fixed lines, not an
  infinitely scrollable list — a true scrollable widget list needs a
  much larger native component (a RemoteViewsService) that wasn't
  worth the added complexity for a 3-line summary.
- Widget playback controls work reliably whenever the app has an
  active session (you were using it, then backgrounded it). If the
  app process has been fully killed by Android, tapping a control
  button opens the app instead of silently controlling playback —
  there's no way around that without a much larger always-on native
  architecture.

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
