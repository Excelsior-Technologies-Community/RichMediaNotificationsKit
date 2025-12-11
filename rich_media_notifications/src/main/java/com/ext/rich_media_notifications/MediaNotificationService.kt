package com.ext.rich_media_notifications

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * MediaNotificationService - Foreground service for persistent media playback notifications
 *
 * This service keeps media notifications alive and handles media control actions.
 * Required for continuous playback controls and to prevent notification dismissal.
 */
class MediaNotificationService : Service() {

    companion object {
        private const val TAG = "MediaNotifService"
        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_NOTIFICATION = "extra_notification"

        private const val FOREGROUND_NOTIFICATION_ID = 1001
    }

    private var currentNotificationId: Int = FOREGROUND_NOTIFICATION_ID
    private var isPlaying: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MediaNotificationService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.getStringExtra(EXTRA_ACTION)
        val notificationId = intent.getIntExtra(
            NotificationActionReceiver.EXTRA_NOTIFICATION_ID,
            FOREGROUND_NOTIFICATION_ID
        )

        currentNotificationId = notificationId

        when (action) {
            NotificationActionReceiver.ACTION_PLAY -> {
                isPlaying = true
                Log.d(TAG, "Media playback started")
            }

            NotificationActionReceiver.ACTION_PAUSE -> {
                isPlaying = false
                Log.d(TAG, "Media playback paused")
            }

            NotificationActionReceiver.ACTION_NEXT -> {
                Log.d(TAG, "Skip to next track")
            }

            NotificationActionReceiver.ACTION_PREV -> {
                Log.d(TAG, "Previous track")
            }

            NotificationActionReceiver.ACTION_STOP -> {
                stopForegroundService()
                return
            }

            else -> {
                // Start as foreground service with notification
                startForegroundWithNotification(intent)
            }
        }
    }

    /**
     * Start service in foreground mode with provided notification
     */
    private fun startForegroundWithNotification(intent: Intent) {
        // Create or retrieve notification
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_NOTIFICATION, android.app.Notification::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_NOTIFICATION)
        }

        if (notification != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    currentNotificationId,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(currentNotificationId, notification)
            }
            Log.d(TAG, "Service started in foreground")
        } else {
            // Create default notification if none provided
            startForeground(currentNotificationId, createDefaultNotification())
            Log.d(TAG, "Service started with default notification")
        }
    }

    /**
     * Create a default fallback notification
     */
    private fun createDefaultNotification(): android.app.Notification {
        NotificationUtils.createDefaultChannels(this)

        return NotificationCompat.Builder(this, NotificationUtils.MEDIA_CHANNEL_ID)
            .setContentTitle("Media Playback")
            .setContentText("Playing...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Stop foreground service and remove notification
     */
    private fun stopForegroundService() {
        Log.d(TAG, "Stopping foreground service")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MediaNotificationService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}