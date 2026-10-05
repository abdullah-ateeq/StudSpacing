# StudSpacing 📐

**StudSpacing** is a modern, feature-rich Android application built with **Jetpack Compose** and **Kotlin** designed for carpenters, builders, contractors, and DIY enthusiasts. It provides precise calculations for wall framing, stud placement, layout planning, and dimensional conversions.

---

## ✨ Key Features

- **Precise Stud Spacing Calculations**: Easily calculate the number of studs, layout marks, and exact positions for walls of any length.
- **Multiple Layout Modes**: 
  - Center-Out layout planning
  - Edge-In (Left or Right start)
  - Standard spacing presets (16" O.C., 24" O.C., or custom spacing)
- **Interactive Wall Diagrams**: Visual representations of stud placements, king studs, jack studs, and layout marks along the wall.
- **Flexible Unit Support**: Seamlessly work with Imperial (feet, inches, fractions) and Metric measurements with built-in unit conversion and formatting.
- **Calculation History**: Save, rename, and review past wall calculations for future reference on the job site.
- **Customizable Settings**: Configure default spacing units, preferences, and calculation rules.
- **Modern Android Architecture**: 
  - 100% Jetpack Compose UI with Material 3 design system
  - StateFlow & ViewModel reactive architecture
  - Local persistence using Jetpack DataStore / Repository pattern
  - Comprehensive unit testing suite

---

## 📱 Screenshots & UI

*Built with a clean, intuitive Material 3 interface supporting both light and dark themes.*

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Async & State**: Kotlin Coroutines & `StateFlow`
- **Persistence**: Jetpack DataStore
- **Testing**: JUnit 4 / 5, AndroidX Test

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 or higher
- Android SDK (API level 24 or higher)

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/StudSpacing.git
   cd StudSpacing
   ```
2. Open the project in Android Studio.
3. Sync Gradle and run the app on an emulator or physical Android device.

---

## 🧪 Running Tests

To run the unit test suite:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Developed with ❤️ by [Abdullah Ateeq](https://github.com/abdullah-ateeq)
