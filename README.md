# PermScript Studio ⚡

> Zero-compile in-app JavaScript development studio for Android hardware and system APIs.

---

## 📱 Download & Install APK

A prebuilt, ready-to-install Android APK is included directly in this repository:

👉 **[Download PermScript-Studio.apk](./apk/PermScript-Studio.apk)** (Direct file path: `apk/PermScript-Studio.apk`)

### Installation Steps on Android:
1. Download the `PermScript-Studio.apk` file to your Android phone (or download it via browser directly from this repo).
2. Tap the downloaded APK in your device's notification tray or **Downloads** folder.
3. If prompted by Android, tap **Settings** and allow **Install unknown apps** for your browser or file manager.
4. Tap **Install** and open **PermScript Studio**!

---

## 🚀 Features

- **Zero Compile Execution**: Write JavaScript / HTML5 apps and run them immediately inside a high-performance native sandbox.
- **Direct Android Hardware Access (`window.Android`)**:
  - 🔦 **Flashlight**: Toggle LED torch (`Android.toggleFlashlight(state)`)
  - 📳 **Haptic Feedback & Waveforms**: Pulse vibrations and custom patterns (`Android.vibrate`, `Android.vibratePattern`)
  - 🔊 **Text-to-Speech**: Real-time voice synthesis (`Android.speak(text)`)
  - 🎙️ **Acoustic Decibel Meter**: Live RMS microphone sound meter streaming (`Android.startSoundMeter()`)
  - 🧭 **Sensors & Compass**: Real-time accelerometer, gyroscope, and orientation HUD (`Android.startSensorStream()`)
  - 📍 **GPS Location**: Live geolocation with latitude, longitude, altitude, speed, and accuracy (`Android.getLocation()`)
  - 🔔 **Heads-Up Notifications**: Post native system notifications (`Android.notify(title, message)`)
  - 🔋 **Battery & System**: Battery percentage, charging status, network connection, and device specs
  - 📋 **Contacts, Calendar & Clipboard**: Inspect address book, calendar events, and copy/paste text
- **Built-in Code Studio**: Monospace editor with line numbers, quick-insert snippets, multiple mini-app management, and export/duplication.
- **Live Debug Console**: Captures `console.log`, `console.warn`, `console.error`, and `Android.log` messages with level filtering and one-click copy.
- **Permissions Hub**: Centralized dashboard to inspect and activate runtime Android hardware permissions.

---

## 🛠️ Building from Source

To build from source using Android Studio or command line Gradle:

```bash
# Clone the repository
git clone <your-repo-url>
cd <repo-folder>

# Build the debug APK
gradle :app:assembleDebug
```

The compiled APK will be output to `app/build/outputs/apk/debug/app-debug.apk`.
