# Disguised Phone

A personal-security launcher replacement: shows a near-empty black screen
instead of your real home screen, cancels incoming notifications, and
restores full access only after a double-tap-and-fingerprint gesture.

## Build

This is a standard Gradle Android project.

```bash
# In Android Studio: File > Open > select this folder, then Run.
# Or from command line with Gradle installed:
./gradlew assembleDebug
# APK lands at app/build/outputs/apk/debug/app-debug.apk
```

Termux can't run the Android Gradle Plugin directly (needs full Android SDK +
JDK 17), so the practical path is: pull this project into Android Studio on
a laptop, or use Termux only to `git push` it somewhere and build in CI
(e.g. a GitHub Actions workflow with `actions/setup-java` + the Android SDK
action), matching how you already deploy Discovr's APK via GitHub Actions.

## First-time setup on the phone

1. Install the APK, open it once (this launches `SetupActivity`).
2. Tap **"Grant notification access"** → find "Phone" in the list → enable.
   (Android requires this to be granted manually; no app can self-grant it.)
3. Tap **"Set as default Home app"** → choose "Phone" → Always.
4. Press the physical Home button. You'll now see a black screen instead of
   your real launcher.
5. Double-tap the bottom ~15% of the screen → confirm fingerprint → the
   screen switches to a plain list of your installed apps. Tap the "🔒" row
   at the top, or double-tap the same zone again, to re-hide instantly.

## Real limitations, stated plainly

- **No root = no true hiding.** This controls what's *shown* on the home
  screen and cancels notifications as they arrive. It cannot remove apps
  from the system, block Recents/app-switcher entirely, or stop someone who
  digs into Settings > Apps and sees "Phone" (app id `com.ops.disguisedphone`)
  installed alongside everything else.
- **Fingerprint sensor taps aren't a raw OS event on 3rd-party apps.** There's
  no API to literally listen to the hardware sensor. The double-tap zone at
  the bottom of the screen is a software approximation — it feels right where
  a rear/under-display sensor usually sits, then triggers real
  `BiometricPrompt` fingerprint auth.
- **Notification Access can't be silently granted** — Android forces a manual
  toggle in Settings the first time, by design (this permission sees the
  content of every notification).
- **The disguise app itself is visible** as an installed app named "Phone"
  with its own icon; it isn't invisible to someone checking installed
  packages directly.
- If you ever want this reviewed by Play Protect / distributed, requesting
  "Notification Access" + "Default Home app" together will need a clear
  in-app explanation (privacy/focus framing) to avoid automated policy flags,
  even for personal/sideloaded use this doesn't matter.

## Files

- `DisguiseActivity.kt` – the fake home screen + unlock/lock logic
- `NotificationBlockerService.kt` – cancels notifications while locked
- `DisguiseState.kt` – shared on/off flag (SharedPreferences)
- `SetupActivity.kt` – one-time permission/launcher setup screen
