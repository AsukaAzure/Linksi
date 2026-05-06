# Linksi — Link Saver for Android

A Material 3 Android app to save, organize, and rediscover links. Appears in the Android share sheet from any app.

---

## Features

- Save any URL with auto-fetched title, description, and favicon
- Appears in Android's share menu from Chrome, YouTube, Twitter, and more
- Organize links into custom folders with icon and color
- Full-text search by title, domain, and description
- Favorites, read/unread tracking
- Grid and list view toggle
- Sort by date, title, domain — filter by favorites or unread
- Bulk select, move, and delete
- Import and export links as JSON or CSV
- Import browser bookmarks from Chrome, Firefox, or Safari
- Material You dynamic color with dark mode support

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, StateFlow |
| Database | Room |
| Dependency Injection | Hilt |
| Image Loading | Coil |
| Metadata Scraping | Jsoup |
| Navigation | Compose Navigation |
| Async | Kotlin Coroutines |
| Minimum SDK | API 26 (Android 8.0) |
| Target SDK | API 34 |

---

## Project Structure

├── data
│   ├── db
│   │   ├── Daos.kt
│   │   ├── Entities.kt
│   │   └── LinksDatabase.kt
│   └── repository
│       └── LinkRepository.kt
│
├── di
│   └── AppModule.kt
│
├── domain
│   └── model
│       └── Models.kt
│
├── LinksApplication.kt
├── MainActivity.kt
│
├── ui
│   ├── components
│   │   ├── Dialogs.kt
│   │   └── LinkCards.kt
│   │
│   ├── screens
│   │   ├── FolderScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   ├── InAppBrowser.kt
│   │   ├── SettingsScreen.kt
│   │   ├── SettingsViewModel.kt
│   │   ├── ShareReceiverActivity.kt
│   │   └── TopBar.kt
│   │
│   └── theme
│       ├── Theme.kt
│       └── Typography.kt
│
└── utils
    ├── ImportExportManager.kt
    └── MetadataFetcher.kt

---

## Building

### Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK API 34
- Gradle 8.4 (downloaded automatically)

### Steps

1. Clone the repository

```bash
git clone https://github.com/AsukaAzure/linksi.git
cd linksi
```

2. Open in Android Studio
   - File → Open → select the project folder
   - Wait for Gradle sync to complete

3. Run on a device or emulator
   - Select a device from the toolbar
   - Click Run or press Shift+F10

### Building a release APK

```bash
./gradlew assembleRelease
```

The output will be at `app/release/app-release.apk`. You will need a signing config set up in `build.gradle` or Android Studio before building a signed release.

---

###ToDo
[x] Folder Structure
[x] Import/Export
[x] Appearance in share page
[ ] In App Browser

---

*Built with Jetpack Compose and Material 3*
