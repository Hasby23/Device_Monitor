# Device Monitor

**Device Monitor** is a native Android application for real-time system performance monitoring, hardware metric tracking, background telemetry recording, and floating HUD overlay rendering. 

Built with modern Android practices: **Jetpack Compose**, **Material 3**, **Room Database**, **Shizuku API**, and **Repository Pattern**.

---

## Key Features

- **Device & Hardware Specifications**: Displays manufacturer name, model, System-on-Chip (SoC) hardware specs, and Android OS version.
- **Battery Telemetry**: Real-time tracking of battery charge percentage (%) and battery temperature (°C) via system broadcast receivers.
- **FPS & Frame Rate Monitoring**: Measure live rendering performance using Android `Choreographer` or parse `dumpsys SurfaceFlinger` via **Shizuku** for precise layer statistics.
- **Session Telemetry Recorder**: Record performance metrics (timestamps, FPS, battery levels, temperatures) every second and persist them locally.
- **Session History & Analytics**: Browse saved recording sessions, view detailed telemetry breakdowns, and delete entries.
- **Draggable Floating Overlay HUD**: Real-time floating overlay window displaying live FPS, temperature, battery level, and active app name via **Shizuku** over any app.

---

## Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Architecture**: MVVM + Repository Pattern (`AppRepository` as Single Source of Truth)
- **Database / Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) with KSP (Kotlin Symbol Processing)
- **Shizuku Integration**: `rikka.shizuku` API for non-root / ADB system shell commands

---

## Project Structure

```
com.example.devicemonitor/
├── MainActivity.kt               # Entry point & Scaffold bottom navigation
├── db/
│   ├── Record.kt                 # Room Entity, DAO, TypeConverters & AppDatabase
│   └── RecordViewModel.kt        # ViewModel for Room database operations
├── repository/
│   └── AppRepository.kt          # Single Source of Truth for shared state & metrics
├── overlay/
│   ├── OverlayManager.kt         # Draggable floating window manager (WindowManager)
│   ├── OverlayScreen.kt          # Floating HUD Compose view layout
│   └── OverlayLifecycleOwner.kt  # Custom lifecycle owner for overlay Compose Views
├── shizuku/
│   ├── ShizukuManager.kt         # Shizuku binder listener & permission handler
│   └── SurfaceFlingerParser.kt   # Parser for dumpsys SurfaceFlinger layer stats
├── screen/
│   ├── StartingScreen.kt         # Dashboard for live stats & performance recording
│   └── ResultScreen.kt           # Session history list & detail breakdown view
└── ui/theme/                     # Material Design 3 theme, colors, and typography
```

---

## Getting Started

### Prerequisites

* **Android Studio**: Ladybug / 2026.1+ or newer
* **JDK**: Java 11 or 17
* **Minimum SDK**: API 24 (Android 7.0)
* **Target SDK**: API 37
* **(Optional) Shizuku**: Installed and running on the test device for advanced SurfaceFlinger stats and floating HUD overlay features.

### Building the App

1. Clone the repository:
   ```bash
   git clone https://github.com/Hasby23/Device_Monitor.git
   cd DeviceMonitor
   ```

2. Open the project in **Android Studio**.

3. Build the debug APK via Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

4. Run the app on an Android device or emulator (`API 24+`).

---
