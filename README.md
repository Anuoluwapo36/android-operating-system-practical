# Android Operating System Practical

An Android app that demonstrates a **complete offline-first data layer**: a paginated REST feed backed by a Room database cache, with pull-to-refresh, client-side search, bookmarks, and dark mode. Built as a practical exercise for Android development — the kind of project that shows you understand how real apps handle network, persistence, and UI state together.

![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin)
![Room](https://img.shields.io/badge/Room-2.6.1-3DDC84?style=for-the-badge&logo=sqlite)
![Retrofit](https://img.shields.io/badge/Retrofit-2.9.0-3DDC84?style=for-the-badge)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Data Flow](#data-flow)
- [Getting Started](#getting-started)
- [Building](#building)
- [Configuration](#configuration)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
- [Roadmap](#roadmap)
- [Known Issues](#known-issues)
- [License](#license)

---

## Overview

The app fetches posts from a public REST API and displays them in an infinite-scrolling list. Every page fetched is written to a local Room database, so **the app shows real content immediately on launch, before the network responds** — and keeps working offline.

That caching behaviour is the core of the project. The feed is backed by three cooperating pieces:

| Layer | Responsibility |
|---|---|
| **Retrofit + OkHttp** | Fetch pages of posts from the API |
| **Room database** | Persist every fetched post locally; serve the cache on cold start |
| **RecyclerView + Adapter** | Render the list with pagination, a loading row, and an end-of-data row |

On top of that sit the usual app features: pull-to-refresh, live search filtering, bookmarks persisted to a second table, a details screen with a loaded image, and a dark mode toggle.

---

## Features

- **Infinite scroll pagination** — the next page loads automatically as the user scrolls near the end
- **Offline-first caching** — Room is the source of truth on launch; the network refreshes it in the background
- **Pull-to-refresh** — swipe down to clear the cache and re-fetch from page one
- **Live search** — filters the loaded list by title as you type, no network round-trip
- **Bookmarks** — saved to their own Room table, with a dedicated screen and empty state
- **Details view** — full post body plus an image loaded with Glide (with placeholder)
- **Dark mode** — toggled from the toolbar via `AppCompatDelegate`, persisted by the system
- **Cache timestamp** — shows when the local cache was last written
- **Multi-view-type adapter** — one `RecyclerView` handles posts, a loading spinner, and an end-of-list row
- **Background threading** — all database work runs on a single-thread executor, results posted back to the UI thread

---

## Tech Stack

### Core
| | |
|---|---|
| **Language** | Java (app logic), Kotlin (Compose theme scaffold + tests) |
| **Min / Target SDK** | 24 (Android 7.0) / 36 |
| **Compile SDK** | 36 |
| **Build** | Gradle 8.13 (Kotlin DSL) + version catalog |
| **AGP / Kotlin** | 8.11.0-alpha07 / 2.0.21 |
| **View binding** | Classic `findViewById` (no view binding enabled) |

### Networking
| Library | Version | Role |
|---|---|---|
| Retrofit | 2.9.0 | REST client |
| OkHttp Logging Interceptor | 4.12.0 | Request/response logging |
| Gson Converter | 2.9.0 | JSON → model mapping |

### Data
| Library | Version | Role |
|---|---|---|
| Room (runtime + compiler) | 2.6.1 | SQLite ORM — cache and bookmarks |
| SharedPreferences | — | Cache timestamp storage |

### UI
| Library | Version | Role |
|---|---|---|
| AppCompat | 1.7.0 | `AppCompatActivity`, `SearchView`, toolbar |
| Material Components | 1.12.0 | `MaterialCardView`, toolbar theme |
| SwipeRefreshLayout | 1.2.0 | Pull-to-refresh container |
| RecyclerView | 1.1.0 *(transitive)* | List rendering |
| Glide | 4.16.0 | Image loading in the details screen |
| Compose BOM / Material3 | 2024.09.00 | Theme scaffold only — **the UI is XML, not Compose** |

---

## Project Structure

```text
android-operating-system-practical-main/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml              # Version catalog
│   └── wrapper/                        # Gradle 8.13 wrapper (JAR included)
├── gradlew / gradlew.bat
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/operatingsystempraticalclass/
        │   │   ├── MainActivity.java        # Feed, pagination, refresh, search
        │   │   ├── DetailsActivity.java     # Single post + image
        │   │   ├── BookMarksActivity.java   # Saved posts list
        │   │   ├── PostAdapter.java         # RecyclerView adapter (3 view types)
        │   │   ├── ApiService.java          # Retrofit interface
        │   │   ├── RetrofitClient.java      # Retrofit singleton
        │   │   ├── Post.java                # API model (Gson)
        │   │   ├── AppDatabase.java         # Room database singleton
        │   │   ├── PostEntity.java          # Room entity — posts table
        │   │   ├── PostDao.java             # Room DAO — posts
        │   │   ├── BookMarksEntity.java     # Room entity — bookmarks table
        │   │   ├── BookmarkDao.java         # Room DAO — bookmarks
        │   │   ├── CacheManager.java        # SharedPreferences cache timestamp
        │   │   └── ui/theme/                # Compose Color/Theme/Type
        │   └── res/
        │       ├── layout/                  # 6 layouts
        │       ├── values/                  # colors, strings, themes
        │       ├── values-night/            # dark-mode colors + themes
        │       ├── drawable/                # launcher foreground/background
        │       ├── mipmap-*/                # launcher icons
        │       └── xml/                     # backup + data extraction rules
        ├── test/                            # ExampleUnitTest.kt
        └── androidTest/                     # ExampleInstrumentedTest.kt
```

---

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│  MainActivity                                              │
│  ├── SwipeRefreshLayout + RecyclerView + SearchView         │
│  ├── Pagination state (currentPage, isLoading, isLastPage)  │
│  └── Single-thread Executor for all DB work                 │
└───────────────┬──────────────────────────┬─────────────────┘
                │                          │
                ▼                          ▼
      ┌───────────────────┐      ┌──────────────────────┐
      │   RetrofitClient  │      │      AppDatabase     │
      │   + ApiService    │      │   (Room singleton)  │
      │  GET /posts?page  │      │  ┌───────────────┐  │
      └─────────┬─────────┘      │  │ PostDao      │  │
                │                │  │ BookmarkDao  │  │
                │ JSON           │  └───────────────┘  │
                ▼                └──────────┬───────────┘
        ┌───────────────┐                   │
        │     Post      │                   ▼
        │  (Gson model) │◄── map to ──► PostEntity / BookMarksEntity
        └───────────────┘
```

**Layer responsibilities:**

- **Model** — `Post` is the network DTO; `PostEntity` and `BookMarksEntity` are the Room tables. Entities are mapped to `Post` objects when the cache is read, so the adapter only ever deals with one type.
- **Repository-style boundary** — `RetrofitClient` is the remote source, `AppDatabase` is the local source. `MainActivity` orchestrates both and decides which wins.
- **UI** — `PostAdapter` holds two lists: `postOriginal` (the full loaded set) and `postsDelay` (what's currently displayed). Search filters the second without touching the first.

### Threading model

All Room calls are blocking, so every one is dispatched to a single-thread `ExecutorService`. Results are posted back to the main thread with a `Handler(Looper.getMainLooper())`. No `LiveData` or `Flow` is used — this is a deliberate, simple approach.

---

## Data Flow

### Cold start

```
onCreate
   ├── loadBookmarkIds()   → DB → set of bookmarked post IDs → adapter
   ├── loadCache()         → DB → populate list immediately from cache
   └── loadPost(true)      → clear cache → fetch page 1 → write to cache
```

The cached posts render **before** the network call resolves. When the response arrives, the list is replaced with fresh data.

### Pagination

The scroll listener watches for the last visible item nearing the end of the list:

```java
if (!isLoading && !isLastPage) {
    if ((visibleItemCount + firstVisibleItem) >= totalItemCount - 1) {
        loadPost(false);
    }
}
```

`loadPost(false)` appends to the existing list; `loadPost(true)` clears everything and starts over. `isLoading` prevents duplicate requests, `isLastPage` stops pagination once a short page comes back.

### Bookmark toggle

```
tap ★
  └── executor:
        ├── already bookmarked → deleteByPostId(id)
        └── else               → insert(BookMarksEntity)
        └── back to UI thread:
              ├── adapter.updateBookMark(id, !wasBookmarked)
              └── Toast "Saved to bookmarks" / "Removed from bookmarks"
```

---

## Getting Started

### Prerequisites

| Requirement | Version |
|---|---|
| **Android Studio** | Ladybug (2024.2) or newer — needed for AGP 8.11 alpha |
| **JDK** | **17 or 21** | See the note below |
| **Android SDK** | Platform 36 + Build Tools 36 |
| **Gradle** | 8.13 — via the bundled wrapper, no install needed |

> **Use JDK 17 or 21 — not JDK 26.** The Kotlin 2.0.21 compiler bundled with this project cannot parse newer JDK version strings and fails the build with a bare `IllegalArgumentException: 26.0.1`. Android Studio's bundled JDK (`Android Studio/jbr`, currently 21.0.9) works out of the box.

### Clone and open

```bash
git clone <your-repo-url>
cd android-operating-system-practical-main
```

Open the folder in Android Studio. Gradle sync runs automatically — the wrapper downloads Gradle 8.13 on first run.

---

## Building

### From Android Studio

1. Open the project folder.
2. Wait for the Gradle sync to finish.
3. Select an emulator or a connected device.
4. Press **Run ▶**.

### From the command line

```bash
# Debug APK
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug

# Unit tests
./gradlew test

# Lint
./gradlew lint
```

**Output:** `app/build/outputs/apk/debug/app-debug.apk` (verified at ~15 MB)

### If Gradle can't find the SDK

```
SDK location not found. Define a valid SDK location with an ANDROID_HOME
environment variable or by setting the sdk.dir path in your project's
local properties file.
```

Create `local.properties` in the project root (this file is gitignored):

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

Or set the environment variable:

```bash
export ANDROID_HOME="$HOME/AppData/Local/Android/Sdk"
```

---

## Configuration

### Base URL

`RetrofitClient.java`:

```java
private static final String BASE_URL = "https://jsonplaceholder.typicode.com/";
```

The app uses [JSONPlaceholder](https://jsonplaceholder.typicode.com/), a free fake REST API — no API key or account needed.

### Page size

`MainActivity.java`:

```java
private static final int PAGE_SIZE = 2;
```

Set to `2` so pagination is easy to observe with only 100 posts available. Increase to `10` or `20` for normal use.

### Cache timestamp

`CacheManager.java` stores the last successful write time in `SharedPreferences` under the key `cache_timestamp`, file `cache_prefs`. `MainActivity.updateCacheTimeDisplay()` renders it as `Cache: HH:mm:ss`, or `Cache: Never` when no cache exists.

---

## Database Schema

Room database `post_db`, version 2, with `fallbackToDestructiveMigration()` enabled.

### `posts`

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER | **Primary key** |
| `title` | TEXT | |
| `body` | TEXT | |

**DAO — `PostDao`**

| Query | Purpose |
|---|---|
| `SELECT * FROM posts ORDER BY id ASC` | Read the whole cache |
| `INSERT ... ON CONFLICT REPLACE` | Bulk upsert a fetched page |
| `DELETE FROM posts` | Clear on refresh |

### `bookmarks`

| Column | Type | Notes |
|---|---|---|
| `postId` | INTEGER | **Primary key** |
| `title` | TEXT | |
| `body` | TEXT | |

**DAO — `BookmarkDao`**

| Query | Purpose |
|---|---|
| `SELECT * FROM bookmarks ORDER BY postId ASC` | Read all bookmarks |
| `INSERT ... ON CONFLICT REPLACE` | Save a bookmark |
| `DELETE FROM bookmarks WHERE postId = :postId` | Remove a bookmark |
| `SELECT EXISTS(...)` | Check whether a post is bookmarked |

---

## API Reference

**Base URL:** `https://jsonplaceholder.typicode.com/`

### `GET /posts`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `_page` | int | 1 | Page number |
| `_limit` | int | 10 | Items per page |

**Response** — array of objects:

```json
[
  {
    "userId": 1,
    "id": 1,
    "title": "sunt aut facere repellat provident",
    "body": "quia et suscipit suscipit recusandae consequuntur expedita..."
  }
]
```

`Post` maps these with Gson `@SerializedName` annotations.

### Images

The details screen doesn't use an image from the API — JSONPlaceholder posts have none. Instead it derives a deterministic placeholder URL from the post ID:

```
https://picsum.photos/id/{postId % 100}/400/200
```

---

## Roadmap

- [ ] Replace `findViewById` with View Binding
- [ ] Move database and network work into a proper Repository layer
- [ ] Use `LiveData` or `Flow` instead of a manual `Handler` + `ExecutorService`
- [ ] Add real unit tests — currently only the generated `assertEquals(4, 2 + 2)` placeholder
- [ ] Add a `PostDetail` screen for comments (the API provides `/posts/{id}/comments`)
- [ ] Add pagination state to the toolbar or a dedicated footer
- [ ] Add error and empty states for the main feed
- [ ] Support offline bookmark operations with a pending-sync queue
- [ ] Bump AGP to a stable release
- [ ] Add a `LICENSE` file
- [ ] Rename the package away from `com.example.*`

---

## Known Issues

Issues worth fixing before this goes further.

### 1. The build fails on JDK 26

The Kotlin 2.0.21 compiler cannot parse the JDK 26 version string:

```
java.lang.IllegalArgumentException: 26.0.1
    at org.jetbrains.kotlin.com.intellij.util.lang.JavaVersion.parse(JavaVersion.java:307)
```

This surfaces as a bare `26.0.1` in the Gradle failure message with no explanation, which makes it hard to diagnose. The fix is either to build with JDK 17/21 (Android Studio's bundled JBR), or to upgrade Kotlin to a version that understands JDK 26.

### 2. AGP is pinned to an alpha

```toml
agp = "8.11.0-alpha07"
```

An alpha plugin requires a matching Android Studio preview. Anyone cloning the repo without it will fail at configuration time. Switch to a stable AGP release.

### 3. `androidx.recyclerview` is not declared as a dependency

`RecyclerView` is imported and used in three classes, but there is no `implementation("androidx.recyclerview:recyclerview:...")` line in `app/build.gradle.kts`. It currently resolves transitively via Material Components at version `1.1.0`. Transitive resolution can change without notice — declare it explicitly.

### 4. `CacheManager` stores a timestamp that is never read back correctly

`updateCacheTimeDisplay()` fetches the stored timestamp but then formats the **current** time instead:

```java
long timestamp = CacheManager.getCacheTimestamp(this);
if (timestamp == 0) {
    cacheTime.setText("Cache: Never");
} else {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    String cache_time = sdf.format(new Date());   // should be new Date(timestamp)
    cacheTime.setText("Cache: " + cache_time);
}
```

The label therefore always shows the time the screen was rendered, not when the cache was written. Pass the stored value: `sdf.format(new Date(timestamp))`.

### 5. The search listener is registered inside the scroll listener

In `MainActivity.init()`, `searchView.setOnQueryTextListener(...)` is called from inside `recyclerView.addOnScrollListener(...)`'s `onScrolled` override. The listener is re-registered on every scroll event. It works, but the registration belongs in `init()` alongside the other setup — move it out of the scroll callback.

### 6. `.idea/` files are mostly committed

The root `.gitignore` excludes only a handful of `.idea` entries (`caches`, `libraries`, `modules.xml`, `workspace.xml`, `navEditor.xml`, `assetWizardSettings.xml`). These files are **not** ignored and would be committed:

```
.idea/.gitignore          .idea/appInsightsSettings.xml
.idea/.name               .idea/deploymentTargetSelector.xml
.idea/AndroidProjectSystem.xml   .idea/deviceManager.xml
.idea/gradle.xml          .idea/migrations.xml
.idea/misc.xml            .idea/runConfigurations.xml
.idea/vcs.xml
```

Most projects ignore the whole `.idea/` directory. Add `.idea/` to `.gitignore` — or at minimum `misc.xml`, `gradle.xml`, and `runConfigurations.xml`, which are the most machine-specific.

### 7. `usesCleartextTraffic="true"`

Enabled in the manifest. The app only talks to HTTPS endpoints, so this can be turned off.

### 8. `DetailsActivity` uses a deprecated API

`getIntent().getSerializableExtra(EXTRA_POST)` is deprecated on Android 13+ (API 33) in favour of `IntentCompat.getSerializableExtra(...)`. The build emits a deprecation note. `Post` implements `Serializable`, which is also slower than Parcelable for this use case.

### 9. `Post.userId` is parsed but never used

The field has a `@SerializedName` mapping and is deserialized by Gson, but there is no getter and it is never read. Either use it or drop it.

### 10. Naming and metadata

- Package is `com.example.operatingsystempraticalclass` — "pratical" is a typo for "practical", and `com.example.*` is rejected by Google Play.
- `strings.xml` has `app_name` = `"Operating System Pratical Class"` — same typo, and it is the name shown under the launcher icon.
- `settings.gradle.kts` has `rootProject.name = "Operating System Pratical Class"` — same typo.
- The repo name is misspelled in the same way.
- No `LICENSE` file, though the README references one.

### 11. Room migration policy

`fallbackToDestructiveMigration()` is enabled and the database is at version 2. Any schema change wipes the local cache. Fine for a learning project, but it should be called out — and a real migration strategy added before shipping.

---

## License

Released under the [MIT License](LICENSE).

Post data comes from [JSONPlaceholder](https://jsonplaceholder.typicode.com/) and images from [Lorem Picsum](https://picsum.photos/) — both free placeholder services.

---

## Author

Built by **Anuoluwapo Majekobaje Emmanuel** — CS undergraduate at Nanchang University.
