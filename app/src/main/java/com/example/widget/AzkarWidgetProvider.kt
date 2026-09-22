package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.provider.AzkarDataProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AzkarWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Azkar widget enabled - sending initial update broadcast")
        sendUpdateBroadcast(context)
        scheduleNextTransitionAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Azkar widget disabled - canceling transition alarm")
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
        scheduleNextTransitionAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            super.onReceive(context, intent)
            val action = intent.action ?: return
            val appWidgetManager = AppWidgetManager.getInstance(context)

            if (action == ACTION_SELECT_AZKAR_WIDGET_CATEGORY) {
                val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "morning"
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )

                Log.d(TAG, "Category tab clicked: $category for widgetId: $appWidgetId")

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().apply {
                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        putString("widget_category_$appWidgetId", category)
                    }
                    putString("last_selected_category", category)
                }.apply()

                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_azkar_list)
                } else {
                    val componentName = ComponentName(context, AzkarWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                        for (id in appWidgetIds) {
                            updateAppWidget(context, appWidgetManager, id)
                            appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_azkar_list)
                        }
                    }
                }
            } else if (action == ACTION_COUNT_AZKAR_WIDGET) {
                val zikrId = intent.getIntExtra(EXTRA_ZIKR_ID, -1)
                val targetCount = intent.getIntExtra(EXTRA_ZIKR_TARGET_COUNT, 1)
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )

                if (zikrId != -1) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val today = getTodayDateString()
                    val countKey = "count_${today}_$zikrId"
                    val currentCount = prefs.getInt(countKey, 0)
                    val nextCount = if (currentCount < targetCount) currentCount + 1 else 0
                    prefs.edit().putInt(countKey, nextCount).apply()

                    Log.d(TAG, "Count updated for zikr $zikrId: $currentCount -> $nextCount (target: $targetCount)")

                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_azkar_list)
                    } else {
                        val componentName = ComponentName(context, AzkarWidgetProvider::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                        if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                            for (id in appWidgetIds) {
                                appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_azkar_list)
                            }
                        }
                    }
                }
            } else if (action == ACTION_UPDATE_AZKAR_WIDGET ||
                action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
                action == Intent.ACTION_TIME_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED
            ) {
                // Clear manual category overrides on automated time-slot transitions so the time-based tab activates
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                for (key in prefs.all.keys) {
                    if (key.startsWith("widget_category_") || key == "last_selected_category") {
                        editor.remove(key)
                    }
                }
                editor.apply()

                val componentName = ComponentName(context, AzkarWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_azkar_list)
                    }
                }
                scheduleNextTransitionAlarm(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in AzkarWidgetProvider.onReceive", e)
        }
    }

    companion object {
        private const val TAG = "AzkarWidget"
        const val PREFS_NAME = "azkar_widget_preferences"
        const val ACTION_UPDATE_AZKAR_WIDGET = "com.example.ACTION_UPDATE_AZKAR_WIDGET"
        const val ACTION_SELECT_AZKAR_WIDGET_CATEGORY = "com.example.ACTION_SELECT_AZKAR_WIDGET_CATEGORY"
        const val ACTION_COUNT_AZKAR_WIDGET = "com.example.ACTION_COUNT_AZKAR_WIDGET"
        const val EXTRA_CATEGORY = "extra_azkar_category"
        const val EXTRA_ZIKR_ID = "extra_zikr_id"
        const val EXTRA_ZIKR_TARGET_COUNT = "extra_zikr_target_count"

        private const val AZKAR_WIDGET_ALARM_REQUEST_CODE = 88003
        private const val REQ_CODE_APP_LAUNCH = 7010

        fun getTodayDateString(): String {
            return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        }

        fun getDefaultCategory(calendar: Calendar = Calendar.getInstance()): String {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val timeInMinutes = hour * 60 + minute

            return when {
                // Dawn to 11:00 AM (04:00 to 11:00 inclusive) -> Morning Azkar
                timeInMinutes in (4 * 60)..(11 * 60) -> "morning"
                // 11:01 AM to 15:59 (3:59 PM) -> Duas & Tasbih
                timeInMinutes in (11 * 60 + 1)..(15 * 60 + 59) -> "daily"
                // 16:00 (4:00 PM) to 20:00 (8:00 PM inclusive) -> Evening Azkar
                timeInMinutes in (16 * 60)..(20 * 60) -> "evening"
                // 20:01 (8:01 PM) till Dawn (< 04:00 AM) -> Duas & Tasbih
                else -> "daily"
            }
        }

        fun getNextTransitionCalendar(now: Calendar = Calendar.getInstance()): Calendar {
            val transitionPoints = listOf(
                Pair(4, 0),    // 04:00 (Dawn -> Morning)
                Pair(11, 1),   // 11:01 (Post-11 AM -> Duas & Tasbih)
                Pair(16, 0),   // 16:00 (4 PM -> Evening Azkar)
                Pair(20, 1)    // 20:01 (Post-8 PM -> Duas & Tasbih till dawn)
            )

            for ((hour, min) in transitionPoints) {
                val target = Calendar.getInstance().apply {
                    timeInMillis = now.timeInMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (target.timeInMillis > now.timeInMillis) {
                    return target
                }
            }

            // If all transitions for today have passed, return tomorrow's 04:00 AM
            return Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 4)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.azkar_widget)

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val activeCategory = prefs.getString("widget_category_$appWidgetId", null)
                    ?: prefs.getString("last_selected_category", null)
                    ?: getDefaultCategory()

                // Badge & Header Text
                val (badgeText, count) = when (activeCategory) {
                    "morning" -> {
                        val count = AzkarDataProvider.AZKAR_COLLECTION.count { it.categoryId == "morning" }
                        "☀️ أذكار الصباح ($count)" to count
                    }
                    "evening" -> {
                        val count = AzkarDataProvider.AZKAR_COLLECTION.count { it.categoryId == "evening" }
                        "🌙 أذكار المساء ($count)" to count
                    }
                    "daily" -> {
                        val count = AzkarDataProvider.AZKAR_COLLECTION.count { it.categoryId in listOf("daily", "post_prayer", "istikhara", "wake", "sleep") }
                        "📿 تسبيح وأدعية ($count)" to count
                    }
                    else -> "الأذكار والأدعية" to 0
                }

                // App logo and title
                views.setImageViewResource(R.id.widget_app_logo, R.mipmap.ic_launcher_round)
                views.setTextViewText(R.id.widget_app_title, "الأذكار والأدعية")
                views.setTextViewText(R.id.widget_azkar_badge, badgeText)

                // Header click launches main app
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val launchPendingIntent = PendingIntent.getActivity(
                    context,
                    REQ_CODE_APP_LAUNCH,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_header, launchPendingIntent)

                // Category Tabs Styling: Highlight the active tab
                val isMorning = activeCategory == "morning"
                val isEvening = activeCategory == "evening"
                val isTasbih = activeCategory == "daily"

                // Morning Tab Style
                views.setInt(
                    R.id.widget_btn_morning,
                    "setBackgroundResource",
                    if (isMorning) R.drawable.bg_widget_time_pill_active else R.drawable.bg_widget_time_pill
                )
                views.setTextColor(
                    R.id.widget_btn_morning,
                    if (isMorning) android.graphics.Color.parseColor("#0B2A22") else android.graphics.Color.parseColor("#E0E0E0")
                )

                // Evening Tab Style
                views.setInt(
                    R.id.widget_btn_evening,
                    "setBackgroundResource",
                    if (isEvening) R.drawable.bg_widget_time_pill_active else R.drawable.bg_widget_time_pill
                )
                views.setTextColor(
                    R.id.widget_btn_evening,
                    if (isEvening) android.graphics.Color.parseColor("#0B2A22") else android.graphics.Color.parseColor("#E0E0E0")
                )

                // Tasbih Tab Style
                views.setInt(
                    R.id.widget_btn_tasbih,
                    "setBackgroundResource",
                    if (isTasbih) R.drawable.bg_widget_time_pill_active else R.drawable.bg_widget_time_pill
                )
                views.setTextColor(
                    R.id.widget_btn_tasbih,
                    if (isTasbih) android.graphics.Color.parseColor("#0B2A22") else android.graphics.Color.parseColor("#E0E0E0")
                )

                // Broadcast PendingIntents for Category Tab Switching directly inside Widget
                // Morning Tab
                val morningIntent = Intent(context, AzkarWidgetProvider::class.java).apply {
                    action = ACTION_SELECT_AZKAR_WIDGET_CATEGORY
                    putExtra(EXTRA_CATEGORY, "morning")
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("holyquran://widget/select_category/morning/$appWidgetId")
                }
                val morningPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 1,
                    morningIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_morning, morningPendingIntent)

                // Evening Tab
                val eveningIntent = Intent(context, AzkarWidgetProvider::class.java).apply {
                    action = ACTION_SELECT_AZKAR_WIDGET_CATEGORY
                    putExtra(EXTRA_CATEGORY, "evening")
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("holyquran://widget/select_category/evening/$appWidgetId")
                }
                val eveningPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 2,
                    eveningIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_evening, eveningPendingIntent)

                // Tasbih & Duas Tab
                val tasbihIntent = Intent(context, AzkarWidgetProvider::class.java).apply {
                    action = ACTION_SELECT_AZKAR_WIDGET_CATEGORY
                    putExtra(EXTRA_CATEGORY, "daily")
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("holyquran://widget/select_category/daily/$appWidgetId")
                }
                val tasbihPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 3,
                    tasbihIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_tasbih, tasbihPendingIntent)

                // Bind RemoteViewsService for Scrollable ListView (stable data URI)
                val serviceIntent = Intent(context, AzkarWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("holyquran://widget/service/$appWidgetId")
                }
                views.setRemoteAdapter(R.id.widget_azkar_list, serviceIntent)
                views.setEmptyView(R.id.widget_azkar_list, R.id.widget_empty_view)

                // PendingIntent Template for Item / Counter Clicks in ListView
                val countIntent = Intent(context, AzkarWidgetProvider::class.java).apply {
                    action = ACTION_COUNT_AZKAR_WIDGET
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val countPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 4,
                    countIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setPendingIntentTemplate(R.id.widget_azkar_list, countPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating Azkar widget", e)
            }
        }

        fun sendUpdateBroadcast(context: Context) {
            try {
                val intent = Intent(context, AzkarWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_AZKAR_WIDGET
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending Azkar widget update broadcast", e)
            }
        }

        private fun scheduleNextTransitionAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val now = Calendar.getInstance()
                val target = getNextTransitionCalendar(now)

                val updateIntent = Intent(context, AzkarWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_AZKAR_WIDGET
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    AZKAR_WIDGET_ALARM_REQUEST_CODE,
                    updateIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        target.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        target.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled next Azkar widget transition alarm at: ${target.time}")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling Azkar widget transition alarm", e)
            }
        }

        private fun cancelAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val updateIntent = Intent(context, AzkarWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_AZKAR_WIDGET
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    AZKAR_WIDGET_ALARM_REQUEST_CODE,
                    updateIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling Azkar widget alarm", e)
            }
        }
    }
}
