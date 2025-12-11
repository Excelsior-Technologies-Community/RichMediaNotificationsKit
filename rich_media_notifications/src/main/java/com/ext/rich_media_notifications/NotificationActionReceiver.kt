package com.ext.rich_media_notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * NotificationActionReceiver - Handles action button clicks in notifications
 *
 * Actions supported:
 * - ACTION_PLAY: Start/resume media playback
 * - ACTION_PAUSE: Pause media playback
 * - ACTION_NEXT: Skip to next track
 * - ACTION_PREV: Go to previous track
 * - ACTION_STOP: Stop playback and dismiss notification
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionRcvr"

        // Action constants
        const val ACTION_PLAY = "com.ext.rich_media_notifications.ACTION_PLAY"
        const val ACTION_PAUSE = "com.ext.rich_media_notifications.ACTION_PAUSE"
        const val ACTION_NEXT = "com.ext.rich_media_notifications.ACTION_NEXT"
        const val ACTION_PREV = "com.ext.rich_media_notifications.ACTION_PREV"
        const val ACTION_STOP = "com.ext.rich_media_notifications.ACTION_STOP"

        // Extra keys
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_MEDIA_SESSION_TAG = "media_session_tag"

        // Callback interface for action handling
        private var actionCallback: ActionCallback? = null

        /**
         * Set callback to handle action button clicks
         */
        fun setActionCallback(callback: ActionCallback) {
            actionCallback = callback
        }

        /**
         * Clear action callback
         */
        fun clearActionCallback() {
            actionCallback = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val sessionTag = intent.getStringExtra(EXTRA_MEDIA_SESSION_TAG)

        Log.d(TAG, "Received action: $action, notificationId: $notificationId")

        when (action) {
            ACTION_PLAY -> {
                handlePlayAction(context, notificationId, sessionTag)
            }

            ACTION_PAUSE -> {
                handlePauseAction(context, notificationId, sessionTag)
            }

            ACTION_NEXT -> {
                handleNextAction(context, notificationId, sessionTag)
            }

            ACTION_PREV -> {
                handlePrevAction(context, notificationId, sessionTag)
            }

            ACTION_STOP -> {
                handleStopAction(context, notificationId, sessionTag)
            }
        }
    }

    private fun handlePlayAction(context: Context, notificationId: Int, sessionTag: String?) {
        Log.d(TAG, "Play action triggered")
        actionCallback?.onPlayClicked(notificationId, sessionTag)

        // If no callback registered, start MediaNotificationService
        if (actionCallback == null) {
            val serviceIntent = Intent(context, MediaNotificationService::class.java).apply {
                putExtra(MediaNotificationService.EXTRA_ACTION, ACTION_PLAY)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_MEDIA_SESSION_TAG, sessionTag)
            }
            context.startService(serviceIntent)
        }
    }

    private fun handlePauseAction(context: Context, notificationId: Int, sessionTag: String?) {
        Log.d(TAG, "Pause action triggered")
        actionCallback?.onPauseClicked(notificationId, sessionTag)

        if (actionCallback == null) {
            val serviceIntent = Intent(context, MediaNotificationService::class.java).apply {
                putExtra(MediaNotificationService.EXTRA_ACTION, ACTION_PAUSE)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_MEDIA_SESSION_TAG, sessionTag)
            }
            context.startService(serviceIntent)
        }
    }

    private fun handleNextAction(context: Context, notificationId: Int, sessionTag: String?) {
        Log.d(TAG, "Next action triggered")
        actionCallback?.onNextClicked(notificationId, sessionTag)

        if (actionCallback == null) {
            val serviceIntent = Intent(context, MediaNotificationService::class.java).apply {
                putExtra(MediaNotificationService.EXTRA_ACTION, ACTION_NEXT)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_MEDIA_SESSION_TAG, sessionTag)
            }
            context.startService(serviceIntent)
        }
    }

    private fun handlePrevAction(context: Context, notificationId: Int, sessionTag: String?) {
        Log.d(TAG, "Previous action triggered")
        actionCallback?.onPrevClicked(notificationId, sessionTag)

        if (actionCallback == null) {
            val serviceIntent = Intent(context, MediaNotificationService::class.java).apply {
                putExtra(MediaNotificationService.EXTRA_ACTION, ACTION_PREV)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_MEDIA_SESSION_TAG, sessionTag)
            }
            context.startService(serviceIntent)
        }
    }

    private fun handleStopAction(context: Context, notificationId: Int, sessionTag: String?) {
        Log.d(TAG, "Stop action triggered")
        actionCallback?.onStopClicked(notificationId, sessionTag)

        // Always dismiss notification on stop
        val notificationManager = NotificationUtils.getNotificationManager(context)
        notificationManager.cancel(notificationId)

        // Stop service if running
        val serviceIntent = Intent(context, MediaNotificationService::class.java)
        context.stopService(serviceIntent)
    }

    /**
     * Callback interface for handling action button clicks
     */
    interface ActionCallback {
        fun onPlayClicked(notificationId: Int, sessionTag: String?)
        fun onPauseClicked(notificationId: Int, sessionTag: String?)
        fun onNextClicked(notificationId: Int, sessionTag: String?)
        fun onPrevClicked(notificationId: Int, sessionTag: String?)
        fun onStopClicked(notificationId: Int, sessionTag: String?)
    }
}