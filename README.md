# Android Operating System Practical

An Android app that shows how a real app handles network data, local storage, and the screen together. It loads a list of posts from a public API, keeps a copy on the phone so the content is there the moment the app opens, and lets the user refresh, search, bookmark, and switch to dark mode.

![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin)
![Room](https://img.shields.io/badge/Room-2.6.1-3DDC84?style=for-the-badge&logo=sqlite)
![Retrofit](https://img.shields.io/badge/Retrofit-2.9.0-3DDC84?style=for-the-badge)

---

## What It Does

The app opens on a list of posts. That list is served from a local database first, so content shows up straight away rather than after a loading spinner. In the background the app refreshes from the internet, and when the new data arrives the list updates in place.

Scrolling to the bottom of the list loads the next page automatically, so the user never has to tap anything to keep reading. When there are no more pages, a small footer marks the end of the list.

Pulling down on the list clears the local copy and starts again from the first page, which is the usual way to check for new content.

The search bar at the top filters the posts already loaded by title as the user types. It runs entirely on the phone, so results appear instantly without a network request.

Every post has a bookmark button. Tapping it saves the post to a second table on the phone, and a dedicated screen lists everything saved. Saved posts keep their state when you come back to the main list, so a filled or empty star always reflects what is actually stored.

Tapping a post opens a detail screen with the full text and an image loaded over the network. A menu item in the toolbar switches between light and dark mode.

---

## Features

- **Cached feed** — posts are stored on the device and shown immediately on launch, before the network responds
- **Automatic pagination** — the next page loads as the user scrolls toward the end of the list
- **Pull to refresh** — swipe down to clear the cache and fetch fresh content
- **Live search** — filters loaded posts by title as you type, with no network request
- **Bookmarks** — saved posts persist on the device and get their own screen with an empty state
- **Post detail** — full text plus an image loaded with Glide and a placeholder while it loads
- **Dark mode** — toggled from the toolbar and remembered by the system
- **Cache age** — a bar showing when the local copy was last written
- **Mixed content types** — one list handles posts, the loading indicator, and the end-of-list marker
- **Background work** — all database calls run off the main thread, with results posted back safely

---

## How It's Built

The app talks to a free public API that returns placeholder posts. Requests go through Retrofit, with Gson converting the JSON into Java objects.

Everything that comes back is also written to an on-device database using Room. That database has two tables — one for posts, one for bookmarks — and is the app's source of truth on launch. The screen reads from the database first and lets the network refresh it afterwards, which is why the app still works with no connection.

Because the database work is blocking, it is dispatched to a background executor and the results are handed back to the main thread. A single adapter renders the list, keeping two lists inside it: the full set of loaded posts, and the subset currently being displayed. Search filters the second without touching the first, so clearing the search restores everything instantly.

The screens are built with the standard Android view system using XML layouts. The project also includes the usual Kotlin theme scaffold, but the app's own UI is written in Java with XML.

---

## Tech Stack

| | |
|---|---|
| **Language** | Java for the app, Kotlin for the theme scaffold |
| **Networking** | Retrofit, Gson, OkHttp logging interceptor |
| **Storage** | Room database, SharedPreferences |
| **UI** | AppCompat, Material Components, SwipeRefreshLayout, RecyclerView, Glide |
| **Build** | Gradle with a version catalog |

---

## Running the Project

Open the project in Android Studio and let the Gradle sync finish, then press Run and pick an emulator or a connected device. The app works against a free public API, so there is nothing to configure or sign up for.

The project needs **JDK 17 or 21**, which is what Android Studio ships with, and **Android SDK Platform 36**.

---

## License

Released under the [MIT License](LICENSE).

Post data comes from JSONPlaceholder and images from Lorem Picsum — both free placeholder services.

---

## Author

Built by **Anuoluwapo Majekobaje Emmanuel** — CS undergraduate at Nanchang University.
