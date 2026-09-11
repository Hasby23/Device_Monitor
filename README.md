# 📱 Device Monitor

**Device Monitor** is a native Android application for real-time system performance monitoring, hardware metric tracking, and telemetry recording.

---

## ✨ Features

- 📱 **Device & Hardware Information**: Displays manufacturer name, device model, System-on-Chip (SoC) hardware specs, and Android OS version.
- 🔋 **Battery Telemetry**: Real-time tracking of battery charge percentage (%) and battery temperature (°C) via system broadcast receivers.
- ⚡ **Frame Rate (FPS) Monitoring**: Measure fps using Android's `Choreographer` frame callbacks or `adb dumpsys SurfaceFlinger` command when using Shizuku for more accurate result.
- 💾 **Storage & Memory Diagnostics**: Real-time monitoring of total and used internal storage and RAM.
- 🔴 **Session Performance Recorder**: Sample telemetry metrics (timestamps, FPS, battery levels, temperatures) every second and persist them locally.
- 📜 **Session History & Analytics**: View saved performance recording sessions, browse detailed session stats, and delete obsolete entries.
- 🖼️ **Floating Overlay HUD**: Compact floating readout view for FPS, temperature, battery level, and active app overlay when using Shizuku.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Database / Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) with KSP (Kotlin Symbol Processing)


---

## 📂 Project Structure

```
com.example.devicemonitor/
├── MainActivity.kt               # Main entry point & Scaffold bottom navigation
├── StartingScreen.kt             # Dashboard for live stats & performance recording
├── ResultScreen.kt               # Session history list & detail breakdown view
├── Record.kt                     # Room Entity, DAO, Converters, and AppDatabase setup
├── RecordViewModel.kt            # ViewModel for managing Room record state
├── SurfaceFlingerParser.kt       # Parser for Android SurfaceFlinger layer statistics
├── overlay/
│   ├── OverlayScreen.kt          # Floating HUD overlay UI composable
│   └── OverlayLifecycleOwner.kt  # Custom lifecycle owner for floating window overlays
└── ui/theme/                     # Material 3 Color, Type, and Theme definitions
```

---

## 🚀 Getting Started

### Prerequisites

* **Android Studio**: Ladybug / 2026.1+ or newer
* **JDK**: Java 11 or 17
* **Minimum SDK**: API 24 (Android 7.0)
* **Target SDK**: API 37

### Building the App

1. Clone the repository:
   ```bash
   git clone <project>
   cd DeviceMonitor
   ```

2. Open the project in **Android Studio**.

3. Build the debug APK via Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

4. Run the app on an Android device.

---
