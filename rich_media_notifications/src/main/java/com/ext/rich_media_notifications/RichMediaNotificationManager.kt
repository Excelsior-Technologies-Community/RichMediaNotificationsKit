package com.ext.rich_media_notifications

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * RichMediaNotificationManager - Main public API for creating rich media notifications
 *
 * Features:
 * - Text-only notifications
 * - Big picture style (images)
 * - GIF notifications (first frame + tap to view)
 * - Video notifications (auto thumbnail)
 * - Media playback with controls (MediaStyle)
 *
 * Usage:
 * ```
 * RichMediaNotificationManager.Builder(context)
 *     .setTitle("Hello")
 *     .setMessage("This is a notification")
 *     .setImage("https://example.com/image.jpg")
 *     .show()
 * ```
 */
class RichMediaNotificationManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "RichMediaNotifMgr"

        // Default notification ID counter
        private var notificationIdCounter = 1000

        /**
         * Initialize the library with default channels
         */
        fun init(context: Context) {
            NotificationUtils.createDefaultChannels(context)
            Log.d(TAG, "RichMediaNotificationManager initialized")
        }

        /**
         * Get next available notification ID
         */
        private fun getNextNotificationId(): Int {
            return notificationIdCounter++
        }
    }

    /**
     * Builder class for fluent notification creation
     */
    class Builder(private val context: Context) {

        // Required fields
        private var title: String = ""
        private var message: String = ""

        // Optional fields
        private var notificationId: Int = getNextNotificationId()
        private var channelId: String = NotificationUtils.DEFAULT_CHANNEL_ID
        private var smallIcon: Int = android.R.drawable.ic_dialog_info
        private var color: Int = Color.parseColor("#2196F3")
        private var autoCancel: Boolean = true
        private var ongoing: Boolean = false
        private var priority: Int = NotificationCompat.PRIORITY_DEFAULT

        // Media fields
        private var imageUrl: String? = null
        private var gifUrl: String? = null
        private var videoUrl: String? = null
        private var largeIconBitmap: Bitmap? = null

        // Media playback fields
        private var mediaTitle: String? = null
        private var mediaArtist: String? = null
        private var mediaAlbumArt: String? = null
        private var showPlayPause: Boolean = true
        private var showNext: Boolean = true
        private var showPrev: Boolean = true
        private var isPlaying: Boolean = false

        // Advanced options
        private var maxImageWidth: Int = 1024
        private var maxImageHeight: Int = 512
        private var placeholderDrawable: Int = R.drawable.rmn_placeholder_image
        private var addGifBadge: Boolean = true
        private var gifBadgeText: String = "GIF"

        /**
         * Set notification title (required)
         */
        fun setTitle(title: String) = apply {
            this.title = title
        }

        /**
         * Set notification message/content (required)
         */
        fun setMessage(message: String) = apply {
            this.message = message
        }

        /**
         * Set custom notification ID
         */
        fun setNotificationId(id: Int) = apply {
            this.notificationId = id
        }

        /**
         * Set notification channel ID
         */
        fun setChannelId(channelId: String) = apply {
            this.channelId = channelId
        }

        /**
         * Set small icon (shown in status bar)
         */
        fun setSmallIcon(iconRes: Int) = apply {
            this.smallIcon = iconRes
        }

        /**
         * Set notification accent color
         */
        fun setColor(color: Int) = apply {
            this.color = color
        }

        /**
         * Set auto-cancel behavior
         */
        fun setAutoCancel(autoCancel: Boolean) = apply {
            this.autoCancel = autoCancel
        }

        /**
         * Set ongoing (cannot be dismissed)
         */
        fun setOngoing(ongoing: Boolean) = apply {
            this.ongoing = ongoing
        }

        /**
         * Set notification priority (pre-API 26)
         */
        fun setPriority(priority: Int) = apply {
            this.priority = priority
        }

        /**
         * Set large icon bitmap
         */
        fun setLargeIcon(bitmap: Bitmap) = apply {
            this.largeIconBitmap = bitmap
        }

        /**
         * Set image URL for BigPictureStyle notification
         */
        fun setImage(imageUrl: String) = apply {
            this.imageUrl = imageUrl
        }

        /**
         * Set GIF URL (shows first frame + "GIF" badge)
         */
        fun setGif(gifUrl: String) = apply {
            this.gifUrl = gifUrl
        }

        /**
         * Set video URL (auto-extracts thumbnail)
         */
        fun setVideo(videoUrl: String) = apply {
            this.videoUrl = videoUrl
        }

        /**
         * Set maximum image dimensions
         */
        fun setMaxImageSize(width: Int, height: Int) = apply {
            this.maxImageWidth = width
            this.maxImageHeight = height
        }

        /**
         * Set placeholder drawable resource
         */
        fun setPlaceholder(drawableRes: Int) = apply {
            this.placeholderDrawable = drawableRes
        }

        /**
         * Configure media playback notification
         *
         * @param mediaTitle Track/video title
         * @param mediaArtist Artist/channel name
         * @param mediaAlbumArt Album art URL
         * @param isPlaying Current playback state
         */
        fun setMediaPlayback(
            mediaTitle: String,
            mediaArtist: String,
            mediaAlbumArt: String? = null,
            isPlaying: Boolean = false
        ) = apply {
            this.mediaTitle = mediaTitle
            this.mediaArtist = mediaArtist
            this.mediaAlbumArt = mediaAlbumArt
            this.isPlaying = isPlaying
            this.channelId = NotificationUtils.MEDIA_CHANNEL_ID
            this.ongoing = true
        }

        /**
         * Configure media control buttons visibility
         */
        fun setMediaControls(
            showPlayPause: Boolean = true,
            showNext: Boolean = true,
            showPrev: Boolean = true
        ) = apply {
            this.showPlayPause = showPlayPause
            this.showNext = showNext
            this.showPrev = showPrev
        }

        /**
         * Build and return notification (does not show it)
         */
        suspend fun build(): Notification = withContext(Dispatchers.IO) {
            // Ensure channel exists
            NotificationUtils.createNotificationChannel(context, channelId)


            // Determine notification type and build accordingly
            when {
                mediaTitle != null -> buildMediaNotification()
                videoUrl != null -> buildVideoNotification()
                gifUrl != null -> buildGifNotification()
                imageUrl != null -> buildImageNotification()
                else -> buildTextNotification()
            }
        }

        /**
         * Build and show notification immediately
         */
        fun show() {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val notification = build()

                    // Show notification
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            NotificationManagerCompat.from(context)
                                .notify(notificationId, notification)
                        } else {
                            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                        }
                    } else {
                        NotificationManagerCompat.from(context).notify(notificationId, notification)
                    }

                    // If media notification, start foreground service
                    if (mediaTitle != null && ongoing) {
                        val serviceIntent =
                            Intent(context, MediaNotificationService::class.java).apply {
                                putExtra(
                                    MediaNotificationService.EXTRA_NOTIFICATION_ID,
                                    notificationId
                                )
                                putExtra(MediaNotificationService.EXTRA_NOTIFICATION, notification)
                            }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }

                    Log.d(TAG, "Notification shown with ID: $notificationId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing notification", e)
                }
            }
        }

        /**
         * Build simple text-only notification
         */
        private suspend fun buildTextNotification(): Notification {
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setColor(color)
                .setPriority(priority)
                .setAutoCancel(autoCancel)
                .setOngoing(ongoing)
                .setContentIntent(NotificationUtils.createContentIntent(context, notificationId))

            // Add large icon if provided
            largeIconBitmap?.let {
                builder.setLargeIcon(it)
            }

            // Use BigTextStyle for long messages
            if (message.length > 40) {
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                        .setBigContentTitle(title)
                )
            }

            return builder.build()
        }

        /**
         * Build image notification with BigPictureStyle
         */
        private suspend fun buildImageNotification(): Notification {
            val bitmap = imageUrl?.let { url ->
                ThumbnailLoader.loadImageThumbnail(
                    context = context,
                    source = url,
                    width = maxImageWidth,
                    height = maxImageHeight
                )
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setColor(color)
                .setPriority(priority)
                .setAutoCancel(autoCancel)
                .setOngoing(ongoing)
                .setContentIntent(NotificationUtils.createContentIntent(context, notificationId))

            if (bitmap != null) {
                // Scale bitmap if needed
                val scaledBitmap = NotificationUtils.scaleBitmap(
                    bitmap,
                    maxImageWidth,
                    maxImageHeight
                )

                builder.setLargeIcon(scaledBitmap)
                    .setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(scaledBitmap)
                            .bigLargeIcon(null as Bitmap?) // Hide large icon when expanded
                            .setBigContentTitle(title)
                            .setSummaryText(message)
                    )
            } else {
                Log.w(TAG, "Failed to load image, showing text notification")
                // Fallback to text notification
                builder.setContentText("$message (Image failed to load)")
            }

            return builder.build()
        }

        /**
         * Build GIF notification
         *
         * Android limitation: Notifications cannot display animated GIFs.
         * Strategy: Show first frame with "GIF" badge, open app on tap for full animation.
         */
        private suspend fun buildGifNotification(): Notification {
            var bitmap = gifUrl?.let { url ->
                ThumbnailLoader.loadGifThumbnail(
                    context = context,
                    gifUrl = url,
                    width = maxImageWidth,
                    height = maxImageHeight
                )
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText("$message (Tap to view animated GIF)")
                .setColor(color)
                .setPriority(priority)
                .setAutoCancel(autoCancel)
                .setOngoing(ongoing)
                .setContentIntent(NotificationUtils.createContentIntent(context, notificationId))

            if (bitmap != null) {
                // Add GIF badge overlay
                if (addGifBadge) {
                    bitmap = NotificationUtils.addBadgeOverlay(
                        bitmap,
                        gifBadgeText,
                        Color.parseColor("#FF6B6B"),
                        Color.WHITE
                    )
                }

                val scaledBitmap = NotificationUtils.scaleBitmap(
                    bitmap,
                    maxImageWidth,
                    maxImageHeight
                )

                builder.setLargeIcon(scaledBitmap)
                    .setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(scaledBitmap)
                            .bigLargeIcon(null as Bitmap?)
                            .setBigContentTitle(title)
                            .setSummaryText("Animated GIF - Tap to view")
                    )
            } else {
                Log.w(TAG, "Failed to load GIF, showing text notification")
                builder.setContentText("$message (GIF failed to load)")
            }

            return builder.build()
        }

        /**
         * Build video notification with auto-generated thumbnail
         */
        private suspend fun buildVideoNotification(): Notification {
            val bitmap = videoUrl?.let { url ->
                ThumbnailLoader.loadVideoThumbnail(
                    context = context,
                    videoSource = url,
                    timeMs = 0L,
                    width = maxImageWidth,
                    height = maxImageHeight
                )
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText("$message (Tap to play video)")
                .setColor(color)
                .setPriority(priority)
                .setAutoCancel(autoCancel)
                .setOngoing(ongoing)
                .setContentIntent(NotificationUtils.createContentIntent(context, notificationId))

            if (bitmap != null) {
                val scaledBitmap = NotificationUtils.scaleBitmap(
                    bitmap,
                    maxImageWidth,
                    maxImageHeight
                )

                builder.setLargeIcon(scaledBitmap)
                    .setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(scaledBitmap)
                            .bigLargeIcon(null as Bitmap?)
                            .setBigContentTitle(title)
                            .setSummaryText("Video - Tap to play")
                    )
            } else {
                Log.w(TAG, "Failed to extract video thumbnail, showing text notification")
                builder.setContentText("$message (Video thumbnail unavailable)")
            }

            return builder.build()
        }

        /**
         * Build media playback notification with controls (MediaStyle)
         */
        private suspend fun buildMediaNotification(): Notification {
            // Load album art if provided
            val albumArtBitmap = mediaAlbumArt?.let { url ->
                ThumbnailLoader.loadImageThumbnail(
                    context = context,
                    source = url,
                    width = 512,
                    height = 512
                )
            }

            // Create MediaSession
            val mediaSession = MediaSessionCompat(context, "RichMediaNotification_$notificationId")
            mediaSession.isActive = true

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon)
                .setContentTitle(mediaTitle ?: title)
                .setContentText(mediaArtist ?: message)
                .setSubText(message)
                .setColor(color)
                .setPriority(priority)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(NotificationUtils.createContentIntent(context, notificationId))

            // Set album art
            if (albumArtBitmap != null) {
                builder.setLargeIcon(albumArtBitmap)
            }

            // Add action buttons
            val actions = mutableListOf<NotificationCompat.Action>()
            val compactIndices = mutableListOf<Int>()

            if (showPrev) {
                actions.add(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_media_previous,
                        context.getString(R.string.rmn_action_prev),
                        NotificationUtils.createActionIntent(
                            context,
                            NotificationActionReceiver.ACTION_PREV,
                            notificationId * 10 + 1
                        )
                    ).build()
                )
                if (compactIndices.size < 3) compactIndices.add(actions.size - 1)
            }

            if (showPlayPause) {
                val playPauseIcon = if (isPlaying) {
                    android.R.drawable.ic_media_pause
                } else {
                    android.R.drawable.ic_media_play
                }

                val playPauseAction = if (isPlaying) {
                    NotificationActionReceiver.ACTION_PAUSE
                } else {
                    NotificationActionReceiver.ACTION_PLAY
                }

                val playPauseLabel = if (isPlaying) {
                    context.getString(R.string.rmn_action_pause)
                } else {
                    context.getString(R.string.rmn_action_play)
                }

                actions.add(
                    NotificationCompat.Action.Builder(
                        playPauseIcon,
                        playPauseLabel,
                        NotificationUtils.createActionIntent(
                            context,
                            playPauseAction,
                            notificationId * 10 + 2
                        )
                    ).build()
                )
                if (compactIndices.size < 3) compactIndices.add(actions.size - 1)
            }

            if (showNext) {
                actions.add(
                    NotificationCompat.Action.Builder(
                        android.R.drawable.ic_media_next,
                        context.getString(R.string.rmn_action_next),
                        NotificationUtils.createActionIntent(
                            context,
                            NotificationActionReceiver.ACTION_NEXT,
                            notificationId * 10 + 3
                        )
                    ).build()
                )
                if (compactIndices.size < 3) compactIndices.add(actions.size - 1)
            }

            // Add all actions to builder
            actions.forEach { builder.addAction(it) }

            // Apply MediaStyle
            val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(*compactIndices.toIntArray())

            builder.setStyle(mediaStyle)

            return builder.build()
        }
    }
}