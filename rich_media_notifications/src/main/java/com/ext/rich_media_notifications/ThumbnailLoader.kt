package com.ext.rich_media_notifications

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * ThumbnailLoader - Handles asynchronous loading of images, GIFs, and video thumbnails
 *
 * Why Glide?
 * - Excellent GIF support (can extract frames or load animated)
 * - Built-in caching (memory + disk)
 * - Video frame extraction support
 * - Lifecycle-aware
 * - Efficient memory management
 * - Wide format support (JPEG, PNG, WebP, GIF, etc.)
 *
 * This class encapsulates all thumbnail loading logic and ensures no main thread blocking.
 */
object ThumbnailLoader {

    private const val TAG = "ThumbnailLoader"

    /**
     * Load an image thumbnail from URL or URI
     *
     * @param context Android context
     * @param source URL string or URI (file://, content://, http://, https://)
     * @param width Desired width in pixels (0 = original)
     * @param height Desired height in pixels (0 = original)
     * @return Bitmap or null if loading fails
     */
    suspend fun loadImageThumbnail(
        context: Context,
        source: String,
        width: Int = 0,
        height: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            suspendCancellableCoroutine { continuation ->
                var isResumed = false

                val requestBuilder = Glide.with(context.applicationContext)
                    .asBitmap()
                    .load(source)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (!isResumed) {
                                isResumed = true
                                Log.e(TAG, "Failed to load image: $source", e)
                                continuation.resume(null)
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            model: Any,
                            target: Target<Bitmap>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (!isResumed) {
                                isResumed = true
                                continuation.resume(resource)
                            }
                            return false
                        }
                    })

                // Apply size override if specified
                if (width > 0 && height > 0) {
                    requestBuilder.override(width, height)
                }

                requestBuilder.submit()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading image: $source", e)
            null
        }
    }

    /**
     * Load GIF thumbnail
     *
     * Android Notification Limitation: Standard notifications do NOT support animated GIFs.
     * The notification system only accepts static Bitmap images.
     *
     * Strategy:
     * 1. Extract first frame of GIF for notification thumbnail
     * 2. Optionally add "GIF" badge overlay to indicate it's animated
     * 3. When user taps notification, open app where GIF can play in ImageView
     *
     * @param context Android context
     * @param gifUrl URL or URI of GIF
     * @param width Desired width
     * @param height Desired height
     * @return First frame as Bitmap
     */
    suspend fun loadGifThumbnail(
        context: Context,
        gifUrl: String,
        width: Int = 0,
        height: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Glide automatically extracts first frame when loading as Bitmap
            suspendCancellableCoroutine { continuation ->
                var isResumed = false

                val requestBuilder = Glide.with(context.applicationContext)
                    .asBitmap() // Force static bitmap from GIF
                    .load(gifUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (!isResumed) {
                                isResumed = true
                                Log.e(TAG, "Failed to load GIF: $gifUrl", e)
                                continuation.resume(null)
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            model: Any,
                            target: Target<Bitmap>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (!isResumed) {
                                isResumed = true
                                continuation.resume(resource)
                            }
                            return false
                        }
                    })

                if (width > 0 && height > 0) {
                    requestBuilder.override(width, height)
                }

                requestBuilder.submit()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading GIF: $gifUrl", e)
            null
        }
    }

    /**
     * Extract video thumbnail from video file or URL
     *
     * Uses MediaMetadataRetriever for local files and Glide for remote videos
     * Extracts frame at specified time position
     *
     * @param context Android context
     * @param videoSource Video URL or file path
     * @param timeMs Time position in milliseconds to extract frame (default: 0)
     * @param width Desired width
     * @param height Desired height
     * @return Video frame as Bitmap
     */
    suspend fun loadVideoThumbnail(
        context: Context,
        videoSource: String,
        timeMs: Long = 0L,
        width: Int = 0,
        height: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Try Glide first (works for most video sources)
            suspendCancellableCoroutine<Bitmap?> { continuation ->
                var isResumed = false

                val requestBuilder = Glide.with(context.applicationContext)
                    .asBitmap()
                    .load(videoSource)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (!isResumed) {
                                isResumed = true
                                Log.e(
                                    TAG,
                                    "Glide failed for video: $videoSource, trying MediaMetadataRetriever"
                                )
                                // Fallback to MediaMetadataRetriever
                                val fallbackBitmap =
                                    extractVideoFrameWithRetriever(videoSource, timeMs)
                                continuation.resume(fallbackBitmap)
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            model: Any,
                            target: Target<Bitmap>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (!isResumed) {
                                isResumed = true
                                continuation.resume(resource)
                            }
                            return false
                        }
                    })

                if (width > 0 && height > 0) {
                    requestBuilder.override(width, height)
                }

                requestBuilder.submit()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading video thumbnail: $videoSource", e)
            null
        }
    }

    /**
     * Fallback method using MediaMetadataRetriever for local video files
     */
    private fun extractVideoFrameWithRetriever(videoSource: String, timeMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            when {
                videoSource.startsWith("content://") || videoSource.startsWith("file://") -> {
                    retriever.setDataSource(videoSource)
                }

                videoSource.startsWith("http://") || videoSource.startsWith("https://") -> {
                    retriever.setDataSource(videoSource, HashMap())
                }

                else -> {
                    retriever.setDataSource(videoSource)
                }
            }

            retriever.getFrameAtTime(
                timeMs * 1000, // Convert ms to microseconds
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } catch (e: Exception) {
            Log.e(TAG, "MediaMetadataRetriever failed: $videoSource", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
    }

    /**
     * Check if a URL points to a GIF file
     */
    fun isGifUrl(url: String): Boolean {
        return url.lowercase().endsWith(".gif") ||
                url.lowercase().contains(".gif?") ||
                url.lowercase().contains("image/gif")
    }

    /**
     * Check if a URL points to a video file
     */
    fun isVideoUrl(url: String): Boolean {
        val videoExtensions = listOf(".mp4", ".webm", ".mkv", ".avi", ".mov", ".3gp", ".flv")
        return videoExtensions.any { url.lowercase().contains(it) }
    }

    /**
     * Preload images for faster display (optional optimization)
     */
    fun preloadImage(context: Context, url: String) {
        try {
            Glide.with(context.applicationContext)
                .load(url)
                .preload()
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading image: $url", e)
        }
    }

    /**
     * Clear Glide cache (useful for testing or memory management)
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            Glide.get(context.applicationContext).clearDiskCache()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }
}