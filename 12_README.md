# Sankalp — Personal Sadhana Android App

A polished, GitHub-ready Android app for daily sankalp, mantra counting and private devotional media.

## Included
- Daily Sankalp text with local persistence
- Mantra counter: 108 and 1001 targets
- Haptic tap feedback
- Audio, video and image import/delete
- Persistent media URI access
- Open media with the device's compatible app
- In-app Google/WebView browser
- Settings + dark-mode switch
- Double-back exit behavior
- Custom Sankalp icon and devotional UI
- GitHub Actions workflow that builds the APK automatically

## Build without Android Studio
1. Upload this folder to a GitHub repository.
2. Push to the `main` branch (or open **Actions → Build Sankalp APK → Run workflow**).
3. GitHub Actions builds `app-debug.apk`.
4. Open the completed workflow run and download the **Sankalp-debug-apk** artifact.
5. Transfer the APK to your Android phone and install it. Android may ask you to allow installation from that source.

## Local command-line build
Requires JDK 17 and a recent Gradle installation:

```bash
gradle :app:assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## Package
`com.sankalp.app`

## Note
This repository intentionally avoids requiring Android Studio. GitHub Actions is the primary build path. A future production release can add signed release builds, richer audio playback, background playback, backup/export, notifications and a full local database.
