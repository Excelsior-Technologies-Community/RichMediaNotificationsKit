# Rich Media Notifications Kit

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-21%2B-orange.svg)](#)
[![](https://jitpack.io/v/Excelsior-Technologies-Community/RichMediaNotificationsKit.svg)](https://jitpack.io/#Excelsior-Technologies-Community/RichMediaNotificationsKit)

**Rich Media Notifications Library** is an Android library that simplifies creating rich, modern notifications. Supports text, big images, GIFs (first frame with badge), video thumbnails, and full media playback controls with play/pause/next/prev actions.

---

## 📸 Preview

<img src="app/src/main/assets/Video.gif"
       alt="Rich Media Notification Library Demo" 
      height="320"/>

---

## ✨ Features

- **Text Notifications**: Simple or expandable with BigTextStyle
- **Big Picture Style**: Load and display remote images automatically
- **GIF Support**: Shows first frame with customizable badge overlay
- **Video Thumbnails**: Auto-generates thumbnails from remote/local videos
- **Media Playback Controls**: Full MediaStyle notifications with play/pause/next/prev actions
- **Async Loading**: Uses Glide for efficient thumbnail loading and caching
- **Permission Handling**: Supports Android 13+ POST_NOTIFICATIONS permission
- **Persistent Media Notifications**: Foreground service for ongoing playback
- **Easy Customization**: Fluent builder pattern and coroutine support
- **Lightweight**: Built on NotificationCompat for broad compatibility

---

## 📦 Installation

**Step 1:** Add JitPack repository to your root `build.gradle` (or `settings.gradle` for newer projects):

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2:** Add dependency to your app module's `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.Excelsior-Technologies-Community:RichMediaNotificationsKit:1.0.2'  
    implementation 'com.github.bumptech.glide:glide:4.16.0'  // Required for thumbnail loading
}
```

Add required permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" /> 
```

---

## 🚀 Usage

### Initialization

```kotlin
// Call once, e.g., in Application.onCreate()
RichMediaNotificationManager.init(this)
```

### Text Notification - Kotlin

```kotlin
RichMediaNotificationManager.Builder(this)
    .setTitle("New Message")
    .setMessage("This is a long text notification that expands automatically when pulled down.")
    .setSmallIcon(R.drawable.ic_notification)
    .setAutoCancel(true)
    .show()
```

### Big Image Notification - Kotlin

```kotlin
RichMediaNotificationManager.Builder(this)
    .setTitle("Beautiful Landscape")
    .setMessage("Check out this stunning view!")
    .setImage("https://picsum.photos/800/600")
    .setSmallIcon(R.drawable.ic_notification)
    .setMaxImageSize(1024, 768)
    .show()
```

### GIF Notification - Kotlin

```kotlin
RichMediaNotificationManager.Builder(this)
    .setTitle("Funny Animation")
    .setMessage("Tap to view full GIF in app")
    .setGif("https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif")
    .setSmallIcon(R.drawable.ic_notification)
    .show()
```

### Video Thumbnail Notification - Kotlin

```kotlin
RichMediaNotificationManager.Builder(this)
    .setTitle("New Video")
    .setMessage("Introduction to Android Notifications")
    .setVideo("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    .setSmallIcon(R.drawable.ic_notification)
    .show()
```

### Media Playback Notification - Kotlin

```kotlin
val notificationId = 1000

RichMediaNotificationManager.Builder(this)
    .setNotificationId(notificationId)
    .setTitle("Now Playing")
    .setMessage("Music Player")
    .setMediaPlayback(
        mediaTitle = "Bohemian Rhapsody",
        mediaArtist = "Queen",
        mediaAlbumArt = "https://picsum.photos/512",
        isPlaying = true
    )
    .setMediaControls(showPlayPause = true, showNext = true, showPrev = true)
    .setSmallIcon(R.drawable.ic_notification)
    .show()

// Handle control actions
NotificationActionReceiver.setActionCallback(object : NotificationActionReceiver.ActionCallback {
    override fun onPlayClicked(notificationId: Int, sessionTag: String?) { /* Resume playback */ }
    override fun onPauseClicked(notificationId: Int, sessionTag: String?) { /* Pause playback */ }
    override fun onNextClicked(notificationId: Int, sessionTag: String?) { /* Next track */ }
    override fun onPrevClicked(notificationId: Int, sessionTag: String?) { /* Previous track */ }
    override fun onStopClicked(notificationId: Int, sessionTag: String?) { /* Stop and dismiss */ }
})
```

---

## 💻 Programmatic Builder Methods

Key methods in `RichMediaNotificationManager.Builder`:

```kotlin
fun setTitle(title: String)
fun setMessage(message: String)
fun setSmallIcon(iconRes: Int)
fun setImage(url: String)
fun setGif(url: String)
fun setVideo(url: String)
fun setMediaPlayback(mediaTitle: String, mediaArtist: String, mediaAlbumArt: String? = null, isPlaying: Boolean = false)
fun setMediaControls(showPlayPause: Boolean, showNext: Boolean, showPrev: Boolean)
fun setAutoCancel(autoCancel: Boolean)
fun setMaxImageSize(width: Int, height: Int)
fun setNotificationId(id: Int)
fun show()  // Asynchronous, uses coroutines
```

---

## 📄 License

```
MIT License

Copyright (c) 2025 Excelsior Technologies

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---
