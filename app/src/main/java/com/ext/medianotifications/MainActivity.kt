package com.ext.medianotification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ext.medianotifications.R
import com.ext.medianotifications.databinding.ActivityMainBinding
import com.ext.rich_media_notifications.NotificationActionReceiver
import com.ext.rich_media_notifications.RichMediaNotificationManager

/**
 * MainActivity - Demo application showing all notification types
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isPlaying = false
    private var currentMediaNotificationId = -1

    // Permission launcher for Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Notification permission denied. Notifications won't be shown.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the library
        RichMediaNotificationManager.init(this)

        // Request notification permission on Android 13+
        requestNotificationPermission()

        // Setup button click listeners
        setupClickListeners()

        // Setup media control action callback
        setupMediaActionCallback()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale and request permission
                    Toast.makeText(
                        this,
                        "Notification permission needed to show rich media notifications",
                        Toast.LENGTH_LONG
                    ).show()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Text-only notification
        binding.btnTextNotification.setOnClickListener {
            showTextNotification()
        }

        // Big image notification
        binding.btnImageNotification.setOnClickListener {
            showImageNotification()
        }

        // GIF notification
        binding.btnGifNotification.setOnClickListener {
            showGifNotification()
        }

        // Video thumbnail notification
        binding.btnVideoNotification.setOnClickListener {
            showVideoNotification()
        }

        // Media playback notification
        binding.btnMediaNotification.setOnClickListener {
            showMediaNotification()
        }
    }

    /**
     * Example 1: Simple text-only notification
     */
    private fun showTextNotification() {
        RichMediaNotificationManager.Builder(this)
            .setTitle("Text Notification")
            .setMessage(
                "This is a simple text notification with title and message. " +
                        "It will expand to show the full text when you pull it down."
            )
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .show()
    }

    /**
     * Example 2: Big picture style notification with image
     */
    private fun showImageNotification() {
        RichMediaNotificationManager.Builder(this)
            .setTitle("Beautiful Landscape")
            .setMessage("Check out this stunning photo!")
            .setImage("https://picsum.photos/800/600") // Random beautiful image
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setMaxImageSize(1024, 768)
            .show()
    }

    /**
     * Example 3: GIF notification (shows first frame + badge)
     *
     * Note: Android notifications cannot show animated GIFs.
     * The library shows the first frame with a "GIF" badge.
     * Tapping the notification opens the app where GIF can be displayed in an ImageView.
     */
    private fun showGifNotification() {
        RichMediaNotificationManager.Builder(this)
            .setTitle("Animated Cat GIF")
            .setMessage("Tap to view the animated GIF in app")
            .setGif("https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif") // Cat GIF
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .show()
    }

    /**
     * Example 4: Video notification with auto-generated thumbnail
     *
     * For demo purposes, using a sample video URL.
     * For local files, use: file:///storage/emulated/0/Download/video.mp4
     */
    private fun showVideoNotification() {
        RichMediaNotificationManager.Builder(this)
            .setTitle("New Video Available")
            .setMessage("Watch: Introduction to Kotlin Coroutines")
            .setVideo("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .show()
    }

    /**
     * Example 5: Media playback notification with controls
     *
     * This creates a persistent notification with play/pause/next/prev controls.
     * Demonstrates MediaStyle notification for audio/video playback.
     */
    private fun showMediaNotification() {
        isPlaying = true
        currentMediaNotificationId = 2000

        RichMediaNotificationManager.Builder(this)
            .setNotificationId(currentMediaNotificationId)
            .setTitle("Now Playing")
            .setMessage("Spotify") // App name or context
            .setMediaPlayback(
                mediaTitle = "Bohemian Rhapsody",
                mediaArtist = "Queen",
                mediaAlbumArt = "https://picsum.photos/512/512", // Album art
                isPlaying = isPlaying
            )
            .setMediaControls(
                showPlayPause = true,
                showNext = true,
                showPrev = true
            )
            .setSmallIcon(R.drawable.ic_notification)
            .show()
    }

    /**
     * Setup callback to handle media control actions
     */
    private fun setupMediaActionCallback() {
        NotificationActionReceiver.setActionCallback(object :
            NotificationActionReceiver.ActionCallback {
            override fun onPlayClicked(notificationId: Int, sessionTag: String?) {
                isPlaying = true
                updateMediaNotification()
            }

            override fun onPauseClicked(notificationId: Int, sessionTag: String?) {
                isPlaying = false
                updateMediaNotification()
            }

            override fun onNextClicked(notificationId: Int, sessionTag: String?) {
                // In a real app, load next track and update notification
                updateMediaNotificationWithNewTrack("Next Song Title", "Another Artist")
            }

            override fun onPrevClicked(notificationId: Int, sessionTag: String?) {
                // In a real app, load previous track and update notification
                updateMediaNotificationWithNewTrack("Previous Song Title", "Previous Artist")
            }

            override fun onStopClicked(notificationId: Int, sessionTag: String?) {
                isPlaying = false
            }
        })
    }

    /**
     * Update existing media notification with play/pause state
     */
    private fun updateMediaNotification() {
        if (currentMediaNotificationId != -1) {
            RichMediaNotificationManager.Builder(this)
                .setNotificationId(currentMediaNotificationId)
                .setTitle("Now Playing")
                .setMessage("Spotify")
                .setMediaPlayback(
                    mediaTitle = "Bohemian Rhapsody",
                    mediaArtist = "Queen",
                    mediaAlbumArt = "https://picsum.photos/512/512",
                    isPlaying = isPlaying
                )
                .setMediaControls(
                    showPlayPause = true,
                    showNext = true,
                    showPrev = true
                )
                .setSmallIcon(R.drawable.ic_notification)
                .show()
        }
    }

    /**
     * Update media notification with new track information
     */
    private fun updateMediaNotificationWithNewTrack(trackTitle: String, artist: String) {
        if (currentMediaNotificationId != -1) {
            RichMediaNotificationManager.Builder(this)
                .setNotificationId(currentMediaNotificationId)
                .setTitle("Now Playing")
                .setMessage("Spotify")
                .setMediaPlayback(
                    mediaTitle = trackTitle,
                    mediaArtist = artist,
                    mediaAlbumArt = "https://picsum.photos/512/512",
                    isPlaying = isPlaying
                )
                .setMediaControls(
                    showPlayPause = true,
                    showNext = true,
                    showPrev = true
                )
                .setSmallIcon(R.drawable.ic_notification)
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear callback when activity is destroyed
        NotificationActionReceiver.clearActionCallback()
    }
}