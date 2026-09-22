package com.example.widget

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
import com.example.data.local.QuranDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReadingStatsWidgetProvider : AppWidgetProvider() {

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
            if (intent.action == ACTION_UPDATE_READING_STATS_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, ReadingStatsWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    onUpdate(context, appWidgetManager, appWidgetIds)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in ReadingStatsWidgetProvider.onReceive", e)
        }
    }

    companion object {
        private const val TAG = "ReadingStatsWidget"
        const val ACTION_UPDATE_READING_STATS_WIDGET = "com.example.ACTION_UPDATE_READING_STATS_WIDGET"
        private const val READING_STATS_WIDGET_CLICK_REQUEST_CODE = 7003

        fun notifyWidgetUpdate(context: Context) {
            try {
                val intent = Intent(context, ReadingStatsWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_READING_STATS_WIDGET
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending widget update broadcast", e)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val views = RemoteViews(context.packageName, R.layout.reading_stats_widget)

                    val db = QuranDatabase.getDatabase(context)
                    val dao = db.quranDao()

                    val progress = dao.getReadingProgressOnce()
                    val history = dao.getAllReadingHistoryOnce()
                    val settings = dao.getUserSettingsOnce()

                    val dailyGoalPages = settings?.dailyPageGoal ?: 10

                    // Calculate today's pages read
                    val calToday = Calendar.getInstance()
                    val todayYear = calToday.get(Calendar.YEAR)
                    val todayDay = calToday.get(Calendar.DAY_OF_YEAR)

                    val todayReadHistory = history.filter {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay
                    }

                    val todayPagesCount = todayReadHistory.map { it.pageNumber }.distinct().size

                    // Calculate Streak
                    val activeDays = history.map {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                    }.distinct().size.coerceAtLeast(1)

                    val streakCount = (activeDays).coerceAtLeast(1)

                    // Format Strings
                    val streakStr = "🔥 سلسلة: $streakCount أيام متتالية"
                    val pagesTodayStr = "📄 $todayPagesCount / $dailyGoalPages صفحة"

                    val lastSurah = progress?.lastSurahName ?: "الفاتحة"
                    val lastPage = progress?.lastPage ?: 1
                    val lastReadStr = "📖 آخر قراءة: سورة $lastSurah • ص $lastPage"

                    views.setTextViewText(R.id.widget_streak_text, streakStr)
                    views.setTextViewText(R.id.widget_pages_today_text, pagesTodayStr)
                    views.setTextViewText(R.id.widget_last_read_text, lastReadStr)

                    // Intent to launch Reading Analytics screen
                    val intent = Intent(context, MainActivity::class.java).apply {
                        action = "com.example.ACTION_NAVIGATE_READING_ANALYTICS"
                        data = Uri.parse("holyquran://widget/reading_analytics")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("navigate_to", "reading_analytics")
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        READING_STATS_WIDGET_CLICK_REQUEST_CODE,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating ReadingStatsWidget", e)
                }
            }
        }
    }
}
