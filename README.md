# ShreshtaGuard

YouTube screen-time guardian for Shreshta (age 3).

After **5 continuous minutes** of YouTube usage, a full-screen warning video
plays automatically and cannot be dismissed until it finishes.

---

## Before building — add your warning video

Replace the placeholder file with your actual video:

```
app/src/main/res/raw/warning_video.mp4
```

Requirements (from PRD):
- Format: MP4 (H.264 + AAC)
- Orientation: Portrait
- Resolution: 720p or 1080p
- Duration: 15–45 seconds recommended
- Language: Kannada

---

## First-time setup on the device (parent does this once)

1. Install the debug APK on the child's phone.
2. Open **ShreshtaGuard**.
3. Tap **Grant** next to *Usage Access* → enable ShreshtaGuard in the list.
4. Tap **Grant** next to *Display Over Other Apps* → toggle it on.
5. Tap **Start Monitoring** — a persistent notification appears.

The app now runs silently in the background and survives reboots.

---

## Build

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## Architecture

| Component | Role |
|---|---|
| `MainActivity` | Parent onboarding — permission checklist + start button |
| `UsageMonitorService` | Foreground service; polls `UsageStatsManager` every 10 s |
| `WarningOverlayActivity` | Full-screen ExoPlayer overlay; blocks back/home during playback |
| `BootReceiver` | Restarts the service after device reboot |
| `res/raw/warning_video.mp4` | **Parent-supplied** warning video |

## Permissions

| Permission | Why |
|---|---|
| `PACKAGE_USAGE_STATS` | Detect foreground app (granted manually in Settings) |
| `SYSTEM_ALERT_WINDOW` | Draw overlay above YouTube (granted manually in Settings) |
| `FOREGROUND_SERVICE` | Keep monitoring service alive |
| `RECEIVE_BOOT_COMPLETED` | Auto-start after reboot |
| `MODIFY_AUDIO_SETTINGS` | Force volume to maximum during video |
| `POST_NOTIFICATIONS` | Show persistent "active" notification (Android 13+) |
