package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.notification.QuranAudioPlaybackService

class QuranAudioPlayerWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "QuranAudioWidget"
        const val ACTION_UPDATE_QURAN_AUDIO_WIDGET = "com.example.ACTION_UPDATE_QURAN_AUDIO_WIDGET"
        const val ACTION_WIDGET_PLAY_PAUSE = "com.example.ACTION_WIDGET_AUDIO_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.example.ACTION_WIDGET_AUDIO_NEXT"
        const val ACTION_WIDGET_PREV = "com.example.ACTION_WIDGET_AUDIO_PREV"

        // Live in-memory cache of audio status for instant widget rendering
        @Volatile
        var cachedTitle: String = "سورة الفاتحة"
        @Volatile
        var cachedSubtitle: String = "مشاري راشد العفاسي"
        @Volatile
        var cachedStatus: String = "جاهز للتشغيل"
        @Volatile
        var cachedBadge: String = "سورة كاملة"
        @Volatile
        var cachedIsPlaying: Boolean = false
        @Volatile
        var cachedProgressPercent: Int = 0

        fun notifyWidgetUpdate(
            context: Context,
            title: String? = null,
            subtitle: String? = null,
            status: String? = null,
            badge: String? = null,
            isPlaying: Boolean? = null,
            progressPercent: Int? = null
        ) {
            try {
                title?.let { cachedTitle = it }
                subtitle?.let { cachedSubtitle = it }
                status?.let { cachedStatus = it }
                badge?.let { cachedBadge = it }
                isPlaying?.let { cachedIsPlaying = it }
                progressPercent?.let { cachedProgressPercent = it.coerceIn(0, 100) }

                val intent = Intent(context, QuranAudioPlayerWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_QURAN_AUDIO_WIDGET
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying QuranAudioPlayerWidget update", e)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.quran_audio_widget)

                views.setTextViewText(R.id.widget_audio_title, cachedTitle)
                views.setTextViewText(R.id.widget_audio_subtitle, cachedSubtitle)
                views.setTextViewText(R.id.widget_audio_status, cachedStatus)
                views.setTextViewText(R.id.widget_badge_text, cachedBadge)

                // Update cumulative progress bar
                views.setProgressBar(R.id.widget_audio_progress, 100, cachedProgressPercent, false)

                // Play/Pause icon state with crisp custom vector drawables
                val playPauseIcon = if (cachedIsPlaying) {
                    R.drawable.ic_widget_pause
                } else {
                    R.drawable.ic_widget_play
                }
                views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

                // Intent to open Quran Audio screen
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "quran_audio")
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    9201,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_audio_container, openPendingIntent)

                // Play/Pause button intent
                val toggleIntent = Intent(context, QuranAudioPlayerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_PLAY_PAUSE
                }
                val togglePendingIntent = PendingIntent.getBroadcast(
                    context,
                    9202,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_play_pause, togglePendingIntent)

                // Previous button intent
                val prevIntent = Intent(context, QuranAudioPlayerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_PREV
                }
                val prevPendingIntent = PendingIntent.getBroadcast(
                    context,
                    9203,
                    prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

                // Next button intent
                val nextIntent = Intent(context, QuranAudioPlayerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_NEXT
                }
                val nextPendingIntent = PendingIntent.getBroadcast(
                    context,
                    9204,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating QuranAudioPlayerWidget", e)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            super.onReceive(context, intent)
            val action = intent.action ?: return

            when (action) {
                ACTION_WIDGET_PLAY_PAUSE -> {
                    QuranAudioPlaybackService.onTogglePlayPause?.invoke()
                }
                ACTION_WIDGET_NEXT -> {
                    QuranAudioPlaybackService.onNextTrack?.invoke()
                }
                ACTION_WIDGET_PREV -> {
                    QuranAudioPlaybackService.onPrevTrack?.invoke()
                }
                ACTION_UPDATE_QURAN_AUDIO_WIDGET, AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, QuranAudioPlayerWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                        onUpdate(context, appWidgetManager, appWidgetIds)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in QuranAudioPlayerWidgetProvider.onReceive", e)
        }
    }
}
