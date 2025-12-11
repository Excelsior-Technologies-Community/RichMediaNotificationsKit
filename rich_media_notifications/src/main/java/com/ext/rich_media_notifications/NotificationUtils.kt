package com.ext.rich_media_notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

/**
 * NotificationUtils - Helper functions for creating notification channels,
 * PendingIntents, and bitmap manipulations
 */
object NotificationUtils {

    const val DEFAULT_CHANNEL_ID = "rmn_default_channel"
    const val MEDIA_CHANNEL_ID = "rmn_media_channel"

    /**
     * Create or update a notification channel (API 26+)
     *
     * @param context Android context
     * @param channelId Unique channel identifier
     * @param channelName User-visible channel name
     * @param channelDescription Channel description
     * @param importance NotificationManager importance level (1-5)
     * @param enableSound Enable sound for notifications
     * @param enableVibration Enable vibration
     * @param enableLights Enable LED lights
     * @param showBadge Show app icon badge
     */
    fun createNotificationChannel(
        context: Context,
        channelId: String = DEFAULT_CHANNEL_ID,
        channelName: String = context.getString(R.string.rmn_default_channel_name),
        channelDescription: String = context.getString(R.string.rmn_default_channel_description),
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        enableSound: Boolean = true,
        enableVibration: Boolean = true,
        enableLights: Boolean = true,
        showBadge: Boolean = true
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                setShowBadge(showBadge)

                if (enableSound) {
                    setSound(
                        android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                        null
                    )
                } else {
                    setSound(null, null)
                }

                if (enableVibration) {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 250, 250)
                } else {
                    enableVibration(false)
                }

                if (enableLights) {
                    enableLights(true)
                    lightColor = Color.BLUE
                } else {
                    enableLights(false)
                }
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create default notification channels for the library
     */
    fun createDefaultChannels(context: Context) {
        // Default channel for general rich media notifications
        createNotificationChannel(
            context = context,
            channelId = DEFAULT_CHANNEL_ID,
            channelName = context.getString(R.string.rmn_default_channel_name),
            channelDescription = context.getString(R.string.rmn_default_channel_description),
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )

        // Media channel for playback controls
        createNotificationChannel(
            context = context,
            channelId = MEDIA_CHANNEL_ID,
            channelName = context.getString(R.string.rmn_media_channel_name),
            channelDescription = context.getString(R.string.rmn_media_channel_description),
            importance = NotificationManager.IMPORTANCE_LOW, // Low to avoid sound during playback
            enableSound = false,
            enableVibration = false
        )
    }

    /**
     * Create a PendingIntent for opening the app when notification is tapped
     *
     * @param context Android context
     * @param requestCode Unique request code for this intent
     * @return PendingIntent configured for app launch
     */
    fun createContentIntent(context: Context, requestCode: Int = 0): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().apply {
                setPackage(context.packageName)
            }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    /**
     * Create a PendingIntent for notification action buttons
     *
     * @param context Android context
     * @param action Action string (e.g., ACTION_PLAY)
     * @param requestCode Unique request code
     * @return PendingIntent for broadcast receiver
     */
    fun createActionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    /**
     * Scale bitmap to fit notification dimensions while maintaining aspect ratio
     *
     * @param bitmap Original bitmap
     * @param maxWidth Maximum width in pixels
     * @param maxHeight Maximum height in pixels
     * @return Scaled bitmap
     */
    fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Create a circular bitmap (useful for large icons)
     *
     * @param bitmap Source bitmap
     * @return Circular cropped bitmap
     */
    fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode =
            android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)

        val left = (bitmap.width - size) / 2f
        val top = (bitmap.height - size) / 2f
        canvas.drawBitmap(bitmap, -left, -top, paint)

        return output
    }

    /**
     * Add a text badge overlay to bitmap (e.g., "GIF" label)
     *
     * @param bitmap Original bitmap
     * @param badgeText Text to display on badge
     * @param backgroundColor Badge background color
     * @param textColor Badge text color
     * @return Bitmap with badge overlay
     */
    fun addBadgeOverlay(
        bitmap: Bitmap,
        badgeText: String,
        backgroundColor: Int = Color.parseColor("#FF6B6B"),
        textColor: Int = Color.WHITE
    ): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = bitmap.height * 0.12f
            color = textColor
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val textBounds = android.graphics.Rect()
        paint.getTextBounds(badgeText, 0, badgeText.length, textBounds)

        val padding = bitmap.height * 0.02f
        val badgeWidth = textBounds.width() + padding * 4
        val badgeHeight = textBounds.height() + padding * 2

        val left = bitmap.width - badgeWidth - padding * 2
        val top = padding * 2

        // Draw badge background
        val bgPaint = Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            left, top, left + badgeWidth, top + badgeHeight,
            padding, padding, bgPaint
        )

        // Draw text
        canvas.drawText(
            badgeText,
            left + badgeWidth / 2,
            top + badgeHeight / 2 + textBounds.height() / 2,
            paint
        )

        return output
    }

    /**
     * Get notification priority based on importance (for pre-API 26)
     */
    fun importanceToPriority(importance: Int): Int {
        return when (importance) {
            NotificationManager.IMPORTANCE_MIN -> NotificationCompat.PRIORITY_MIN
            NotificationManager.IMPORTANCE_LOW -> NotificationCompat.PRIORITY_LOW
            NotificationManager.IMPORTANCE_HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationManager.IMPORTANCE_MAX -> NotificationCompat.PRIORITY_MAX
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    /**
     * Convert dp to pixels
     */
    fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * Get NotificationManager instance
     */
    fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}