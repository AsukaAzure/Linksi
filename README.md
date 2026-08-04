# Linksi — Link Saver for Android

<div align="center">

[![Android API 26+](https://img.shields.io/badge/Android-API%2026%2B-green?logo=android)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?logo=android)](https://developer.android.com/jetpack/compose)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-6200EA?logo=materialdesign)](https://m3.material.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

*Organize your digital life, one link at a time*

[Features](#features) • [Tech Stack](#tech-stack) • [Getting Started](#getting-started) • [Building](#building) • [Contributing](#contributing)

</div>

---

## Overview

**Linksi** is a modern, Material Design 3 Android app that helps you save, organize, and rediscover links from anywhere. Save links from Chrome, YouTube, Twitter, Reddit, and any other app using the native Android share sheet. Built with Jetpack Compose for a smooth, responsive experience.

Perfect for researchers, content curators, developers, and anyone who finds too many interesting links and needs a better way to organize them.

---
<img width="5016" height="2823" alt="linksi_poster" src="https://github.com/user-attachments/assets/a7650e12-942c-42b4-ab44-fc23fc95ef4a" />

## Features

### Core Functionality
- Save & Auto-Fetch: Save any URL with auto-fetched title, description, and favicon
- Share Sheet Integration: Appears in Android's native share menu from Chrome, YouTube, Twitter, Reddit, and more
- Custom Folders: Organize links into custom folders with custom icons and colors
- Full-Text Search: Search by title, domain, and description across all saved links
- Favorites & Status Tracking: Mark links as favorites and track read/unread status

### Organization & Navigation
- Multiple Views: Toggle between grid and list view layouts
- Smart Sorting: Sort by date, title, or domain — filter by favorites or unread
- Bulk Actions: Select multiple links to move or delete at once
- Trash Bin: Recover deleted links within 30 days before permanent deletion
- AI Link Organizer: Automatically categorize and move links into folders using AI

### Browsing & Import/Export
- Inbuilt Browser: Quick view and browse saved links without leaving the app
- Import/Export: Save links as JSON, CSV, or HTML — share your collection
- Browser Bookmarks Import: Import bookmarks from Chrome, Firefox, and Safari
- Reminders: Get notifications to review your saved links

### Security & Personalization
- App Lock & Folder Lock: PIN or biometric authentication for sensitive links
- Material You Design: Dynamic color theming based on your device's wallpaper
- Dark Mode Support: Beautiful dark mode for comfortable viewing at any time
- Smooth Performance: Built with Jetpack Compose for responsive, fluid interactions

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| UI Framework | Jetpack Compose, Material Design 3 |
| Architecture | MVVM + StateFlow |
| Database | Room Persistence Library |
| Dependency Injection | Hilt |
| Image Loading | Coil |
| Web Scraping | Jsoup |
| Navigation | Jetpack Compose Navigation |
| Async Operations | Kotlin Coroutines + Flow |
| Minimum SDK | API 26 (Android 8.0) |
| Target SDK | API 34 (Android 14) |
| Language | 100% Kotlin |

---

## Project Structure

```
app/src/main/java/com/linksi/app/
├── data/
│   ├── db/
│   │   ├── Daos.kt              # Database access objects
│   │   ├── Entities.kt          # Room database entities
│   │   └── LinksDatabase.kt     # Room database configuration
│   └── repository/
│       └── LinkRepository.kt    # Data access abstraction layer
├── di/
│   └── AppModule.kt             # Hilt dependency injection setup
├── domain/
│   └── model/
│       └── Models.kt            # Domain models and data classes
├── ui/
│   ├── components/
│   │   ├── Dialogs.kt           # Reusable dialog components
│   │   └── LinkCards.kt         # Link display card components
│   ├── screens/
│   │   ├── FolderScreen.kt      # Folder view implementation
│   │   ├── HomeScreen.kt        # Main home screen UI
│   │   ├── HomeViewModel.kt     # Home screen business logic
│   │   ├── InAppBrowser.kt      # Built-in web browser
│   │   ├── SettingsScreen.kt    # Settings & preferences UI
│   │   ├── SettingsViewModel.kt # Settings business logic
│   │   ├── ShareReceiverActivity.kt # Share intent handler
│   │   └── TopBar.kt            # App bar and navigation
│   └── theme/
│       ├── Theme.kt             # Material 3 theme configuration
│       └── Typography.kt        # Text styles and typography
├── utils/
│   ├── ImportExportManager.kt   # Import/export functionality
│   ├── MetadataFetcher.kt       # URL metadata scraping logic
│   └── ...
├── LinksApplication.kt          # Application class
└── MainActivity.kt              # Main activity entry point
```

---

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- Android Studio: [Hedgehog (2023.1.1)](https://developer.android.com/studio) or newer
- JDK: Version 17 or later
- Android SDK: API level 34 with build tools
- Gradle: 8.4+ (automatically managed)

### Quick Start

1. Clone the repository
   ```bash
   git clone https://github.com/AsukaAzure/Linksi.git
   cd Linksi
   ```

2. Open in Android Studio
   - Launch Android Studio
   - Select File → Open → Navigate to the Linksi folder
   - Wait for Gradle to sync and index the project

3. Run the app
   - Connect an Android device (API 26+) or start an emulator
   - Select your device from the toolbar
   - Click the Run button or press Shift + F10

---

## Building

### Debug Build

```bash
./gradlew build
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

Note: For a signed release APK, configure your signing key in `build.gradle` or through Android Studio's build signing setup.

### Run Tests

```bash
./gradlew test
```

---

## Roadmap

### Completed
- Folder structure and organization
- Import/Export (JSON, CSV, HTML formats)
- Share sheet integration
- Built-in browser
- Reminders and notifications
- Trash bin with 30-day retention
- AI-powered link organization
- Security features (App Lock & Folder Lock)

---

## Contributing

Contributions are welcome! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to your branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request with a clear description

### Development Guidelines

- Follow Kotlin naming conventions and Android best practices
- Use Compose best practices for UI components
- Keep business logic in ViewModels
- Write meaningful commit messages
- Test your changes on multiple API levels

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Designed following [Material Design 3](https://m3.material.io) guidelines
- Database powered by [Room](https://developer.android.com/topic/libraries/architecture/room)
- Dependency injection with [Hilt](https://dagger.dev/hilt/)

---

## Support

Found a bug or have a feature request? [Open an issue](https://github.com/AsukaAzure/Linksi/issues) on GitHub.

---

<div align="center">

Made with love using Kotlin & Jetpack Compose

[Back to Top](#linksi--link-saver-for-android)

</div>
