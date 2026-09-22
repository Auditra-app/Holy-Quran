package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.provider.QuranDataProvider
import java.util.Calendar

class DailyVerseWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Daily Verse widget enabled - scheduling midnight update alarm")
        scheduleAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Daily Verse widget disabled - canceling update alarm")
        cancelAlarm(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            super.onReceive(context, intent)
            if (intent.action == ACTION_UPDATE_DAILY_VERSE_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, DailyVerseWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    onUpdate(context, appWidgetManager, appWidgetIds)
                }
                scheduleAlarm(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in DailyVerseWidgetProvider.onReceive", e)
        }
    }

    companion object {
        private const val TAG = "DailyVerseWidget"
        const val ACTION_UPDATE_DAILY_VERSE_WIDGET = "com.example.ACTION_UPDATE_DAILY_VERSE_WIDGET"
        private const val DAILY_VERSE_WIDGET_ALARM_REQUEST_CODE = 88002
        private const val DAILY_VERSE_WIDGET_CLICK_REQUEST_CODE = 7002

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.daily_verse_widget)
                val dailyVerse = QuranDataProvider.getDailyVerseForToday()

                // Display App Logo
                views.setImageViewResource(R.id.widget_app_logo, R.mipmap.ic_launcher_round)

                // Display App Title
                views.setTextViewText(R.id.widget_app_title, "القرآن الكريم")

                // Format Quranic verse text with brackets
                val formattedVerseText = "﴿ ${dailyVerse.textArabic} ﴾"
                views.setTextViewText(R.id.widget_verse_text, formattedVerseText)

                // Format Surah name and Verse number reference
                val formattedReference = "سورة ${dailyVerse.surahNameArabic} • الآية ${dailyVerse.verseNumber}"
                views.setTextViewText(R.id.widget_verse_reference, formattedReference)

                // PendingIntent to launch app when widget is tapped
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = "com.example.ACTION_NAVIGATE_DAILY_VERSE"
                    data = Uri.parse("holyquran://widget/daily_verse")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("navigate_to", "daily_verse")
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    DAILY_VERSE_WIDGET_CLICK_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating daily verse widget", e)
            }
        }

        fun sendUpdateBroadcast(context: Context) {
            try {
                val intent = Intent(context, DailyVerseWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_DAILY_VERSE_WIDGET
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending daily verse widget update broadcast", e)
            }
        }

        fun scheduleAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, DailyVerseWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_DAILY_VERSE_WIDGET
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    DAILY_VERSE_WIDGET_ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Schedule next refresh at next midnight or in 6 hours
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 5)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling daily verse widget alarm", e)
            }
        }

        fun cancelAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, DailyVerseWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_DAILY_VERSE_WIDGET
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    DAILY_VERSE_WIDGET_ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling daily verse widget alarm", e)
            }
        }
    }
}
