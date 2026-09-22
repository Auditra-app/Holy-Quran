package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.local.PrayerRecordEntity
import com.example.data.local.QuranDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerTrackerWidgetProvider : AppWidgetProvider() {

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
        val action = intent.action ?: return
        Log.d(TAG, "onReceive: action=$action")

        if (action == ACTION_UPDATE_WIDGET || action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PrayerTrackerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        } else if (action == ACTION_TOGGLE_PRAYER) {
            val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: return
            val prayerNameAr = intent.getStringExtra(EXTRA_PRAYER_NAME_AR) ?: "الصلاة"
            val dateString = intent.getStringExtra(EXTRA_DATE_STRING)
                ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = QuranDatabase.getDatabase(context)
                    val dao = db.quranDao()
                    val existing = dao.getPrayerRecord(dateString, prayerKey)
                    val newCompleted = !(existing?.isCompleted ?: false)

                    val updated = PrayerRecordEntity(
                        id = existing?.id ?: 0,
                        dateString = dateString,
                        prayerKey = prayerKey,
                        isCompleted = newCompleted,
                        isOnTime = if (newCompleted) true else false,
                        completedTimestamp = if (newCompleted) System.currentTimeMillis() else null
                    )
                    dao.insertPrayerRecord(updated)

                    // Refresh widget in place
                    sendUpdateBroadcast(context)

                    Handler(Looper.getMainLooper()).post {
                        val msg = if (newCompleted) {
                            "تقبل الله! تم تسجيل صلاة $prayerNameAr ✅"
                        } else {
                            "تم إلغاء تحديد صلاة $prayerNameAr"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error toggling prayer in widget", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }

    companion object {
        private const val TAG = "PrayerTrackerWidget"
        const val ACTION_UPDATE_WIDGET = "com.example.ACTION_UPDATE_PRAYER_TRACKER_WIDGET"
        const val ACTION_TOGGLE_PRAYER = "com.example.ACTION_TOGGLE_TRACKER_PRAYER"

        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_PRAYER_NAME_AR = "extra_prayer_name_ar"
        const val EXTRA_DATE_STRING = "extra_date_string"

        private const val TRACKER_CLICK_REQUEST_CODE = 7101

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val db = QuranDatabase.getDatabase(context)
                    val records = db.quranDao().getPrayerRecordsForDateOnce(todayStr)

                    val views = RemoteViews(context.packageName, R.layout.prayer_tracker_widget)

                    val standardPrayers = listOf(
                        Triple("fajr", "الفجر", R.id.widget_tracker_fajr to R.id.widget_fajr_icon),
                        Triple("dhuhr", "الظهر", R.id.widget_tracker_dhuhr to R.id.widget_dhuhr_icon),
                        Triple("asr", "العصر", R.id.widget_tracker_asr to R.id.widget_asr_icon),
                        Triple("maghrib", "المغرب", R.id.widget_tracker_maghrib to R.id.widget_maghrib_icon),
                        Triple("isha", "العشاء", R.id.widget_tracker_isha to R.id.widget_isha_icon)
                    )

                    var completedCount = 0

                    standardPrayers.forEachIndexed { index, (key, nameAr, viewIds) ->
                        val (containerId, iconId) = viewIds
                        val isDone = records.firstOrNull { it.prayerKey.equals(key, ignoreCase = true) }?.isCompleted == true

                        if (isDone) {
                            completedCount++
                            views.setTextViewText(iconId, "✅")
                            views.setInt(containerId, "setBackgroundColor", Color.parseColor("#1B5E20")) // Green
                        } else {
                            views.setTextViewText(iconId, "⬜")
                            views.setInt(containerId, "setBackgroundColor", Color.parseColor("#184A3B")) // Emerald dark
                        }

                        // Set pending intent for toggling this prayer
                        val toggleIntent = Intent(context, PrayerTrackerWidgetProvider::class.java).apply {
                            action = ACTION_TOGGLE_PRAYER
                            putExtra(EXTRA_PRAYER_KEY, key)
                            putExtra(EXTRA_PRAYER_NAME_AR, nameAr)
                            putExtra(EXTRA_DATE_STRING, todayStr)
                            data = Uri.parse("holyquran://tracker_widget/toggle/$key/$todayStr")
                        }
                        val togglePendingIntent = PendingIntent.getBroadcast(
                            context,
                            8000 + index,
                            toggleIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(containerId, togglePendingIntent)
                    }

                    // Update Progress Badge
                    views.setTextViewText(R.id.widget_tracker_progress, "$completedCount / 5 صلوات")

                    // Main Click intent -> Navigate to Prayer Tracker Screen
                    val mainIntent = Intent(context, MainActivity::class.java).apply {
                        action = "com.example.ACTION_NAVIGATE_PRAYER_TRACKER"
                        data = Uri.parse("holyquran://widget/prayer_tracker")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("navigate_to", "prayer_tracker")
                    }
                    val mainPendingIntent = PendingIntent.getActivity(
                        context,
                        TRACKER_CLICK_REQUEST_CODE,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_tracker_container, mainPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_tracker_header, mainPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating prayer tracker widget", e)
                }
            }
        }

        fun sendUpdateBroadcast(context: Context) {
            try {
                val intent = Intent(context, PrayerTrackerWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_WIDGET
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending tracker widget update broadcast", e)
            }
        }
    }
}
