# 🔗 Linksi — Link Saver App

A beautiful Material 3 Android app to save, organize, and rediscover your links. Appears in the Android share sheet from any app.

---

## ✨ Features

| Feature | Description |
|---|---|
| 💾 **Save Links** | Save any URL with auto-fetched title, description & favicon |
| 📤 **Share Sheet** | Appears in Android's share menu from Chrome, YouTube, Twitter, etc. |
| 📁 **Folders** | Organize links into custom folders with emoji + color |
| 🔍 **Full-text Search** | Search by title, domain, description, or tags |
| ❤️ **Favorites** | Star important links for quick access |
| 🏷️ **Tags** | Add custom tags to links for flexible organization |
| 📊 **Grid / List View** | Toggle between compact list and visual grid |
| 🔽 **Sort & Filter** | Sort by date, title, domain; filter by favorites/unread |
| 🌙 **Dark Mode** | Full Material You dynamic color + dark theme support |
| ✅ **Read tracking** | Mark links as read/unread with visual indicator |

---

## 📁 Folder Structure

```
Linksi/
├── build.gradle                          ← Root build config
├── settings.gradle                       ← Module settings
├── gradle.properties                     ← JVM + Android flags
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties     ← Gradle 8.4
│
└── app/
    ├── build.gradle                      ← App deps & build config
    ├── proguard-rules.pro                ← Release obfuscation rules
    │
    └── src/main/
        ├── AndroidManifest.xml           ← Permissions, activities, share intent
        │
        ├── res/
        │   ├── values/
        │   │   ├── strings.xml
        │   │   └── themes.xml            ← Base + Transparent theme
        │   ├── xml/
        │   │   └── network_security_config.xml
        │   └── mipmap-*/                 ← App icons (add your own)
        │
        └── java/com/linksi/app/
            │
            ├── LinksApplication.kt       ← @HiltAndroidApp entry point
            ├── MainActivity.kt           ← Edge-to-edge Compose host
            │
            ├── domain/model/
            │   └── Models.kt             ← Link, Folder, SortOption, FilterOption
            │
            ├── data/
            │   ├── db/
            │   │   ├── Entities.kt       ← Room @Entity classes
            │   │   ├── Daos.kt           ← LinkDao + FolderDao with Flow queries
            │   │   └── LinksDatabase.kt  ← @Database class
            │   └── repository/
            │       └── LinkRepository.kt ← Single source of truth + mappers
            │
            ├── di/
            │   └── AppModule.kt          ← Hilt @Provides for DB, DAOs
            │
            ├── utils/
            │   └── MetadataFetcher.kt    ← Jsoup scraper for title/desc/favicon
            │
            └── ui/
                ├── theme/
                │   ├── Theme.kt          ← Material 3 color schemes (light/dark)
                │   └── Typography.kt     ← Type scale
                │
                ├── screens/
                │   ├── HomeScreen.kt     ← Main screen (search, filters, list/grid)
                │   ├── HomeViewModel.kt  ← StateFlow UI state + all business logic
                │   ├── TopBar.kt         ← TopAppBar with view toggle & sort
                │   └── ShareReceiverActivity.kt ← Bottom sheet for share intents
                │
                └── components/
                    ├── LinkCards.kt      ← LinkCard (list) + LinkGridCard (grid)
                    └── Dialogs.kt        ← AddLink, AddFolder, FolderPicker, Sort
```

---

## 🛠️ How to Build

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17+ |
| Android SDK | API 34 |
| Gradle | 8.4 (auto-downloaded) |

---

## 🚀 Possible Next Features

- [x] Bulk selection
- [x] Import/export links (JSON/CSV)
      

---

*Built with Jetpack Compose + Material 3 · Minimum Android 8.0 (API 26)*
