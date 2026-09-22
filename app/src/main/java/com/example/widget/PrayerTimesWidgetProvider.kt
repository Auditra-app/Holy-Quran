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
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.provider.QuranDataProvider
import com.example.domain.model.PrayerTime
import com.example.util.LocationHelper
import java.util.Calendar
import java.util.Locale

class PrayerTimesWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Prayer widget enabled - updating and scheduling alarm")
        sendUpdateBroadcast(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Prayer widget disabled - canceling AlarmManager refresh")
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
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            super.onReceive(context, intent)
            if (intent.action == ACTION_UPDATE_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, PrayerTimesWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onReceive", e)
        }
    }

    companion object {
        private const val TAG = "PrayerTimesWidget"
        const val ACTION_UPDATE_WIDGET = "com.example.ACTION_UPDATE_PRAYER_WIDGET"
        private const val WIDGET_ALARM_REQUEST_CODE = 88001
        private const val PRAYER_WIDGET_CLICK_REQUEST_CODE = 7001

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val locSettings = LocationHelper.getSavedLocationAndSettings(context)
                val views = RemoteViews(context.packageName, R.layout.prayer_times_widget)

                val prayerTimes = QuranDataProvider.calculatePrayerTimes(
                    latitude = locSettings.latitude,
                    longitude = locSettings.longitude,
                    calculationMethod = locSettings.calculationMethod,
                    isSummerTime = locSettings.isSummerTime,
                    fajrOffset = locSettings.fajrOffset,
                    dhuhrOffset = locSettings.dhuhrOffset,
                    asrOffset = locSettings.asrOffset,
                    maghribOffset = locSettings.maghribOffset,
                    ishaOffset = locSettings.ishaOffset
                )

                // Update Location Text Header
                views.setTextViewText(R.id.widget_location_text, locSettings.cityName)

                // Calculate next prayer & target millisecond
                val nextPrayerInfo = calculateNextPrayerAndCountdown(prayerTimes)

                views.setTextViewText(R.id.widget_next_prayer_title, "الصلاة القادمة: ${nextPrayerInfo.prayerNameAr}")
                views.setTextViewText(R.id.widget_next_prayer_time, nextPrayerInfo.prayerTimeFormatted)

                val nowMs = System.currentTimeMillis()
                val diffMs = nextPrayerInfo.targetPrayerTimeMs - nowMs

                if (diffMs > 0) {
                    val baseElapsed = SystemClock.elapsedRealtime() + diffMs
                    views.setChronometer(R.id.widget_chronometer, baseElapsed, null, true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        views.setChronometerCountDown(R.id.widget_chronometer, true)
                    }
                    views.setViewVisibility(R.id.widget_chronometer, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_countdown_text, android.view.View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_chronometer, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_countdown_text, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widget_countdown_text, "الآن")
                }

                // Highlight next prayer in 5-column grid
                prayerTimes.forEach { prayer ->
                    val isNext = (prayer.nameEnglish == nextPrayerInfo.prayerNameEn)
                    val containerId = when (prayer.nameEnglish) {
                        "Fajr" -> R.id.widget_fajr_container
                        "Dhuhr" -> R.id.widget_dhuhr_container
                        "Asr" -> R.id.widget_asr_container
                        "Maghrib" -> R.id.widget_maghrib_container
                        "Isha" -> R.id.widget_isha_container
                        else -> 0
                    }
                    val textId = when (prayer.nameEnglish) {
                        "Fajr" -> R.id.widget_fajr_text
                        "Dhuhr" -> R.id.widget_dhuhr_text
                        "Asr" -> R.id.widget_asr_text
                        "Maghrib" -> R.id.widget_maghrib_text
                        "Isha" -> R.id.widget_isha_text
                        else -> 0
                    }

                    if (containerId != 0 && textId != 0) {
                        val label = when (prayer.nameEnglish) {
                            "Fajr" -> "الفجر"
                            "Dhuhr" -> "الظهر"
                            "Asr" -> "العصر"
                            "Maghrib" -> "المغرب"
                            "Isha" -> "العشاء"
                            else -> prayer.nameArabic
                        }
                        val timeOnly = prayer.timeFormatted.replace(" AM", "").replace(" PM", "").replace("ص", "").replace("م", "").trim()
                        views.setTextViewText(textId, "$label\n$timeOnly")
                        if (isNext) {
                            views.setInt(containerId, "setBackgroundColor", android.graphics.Color.parseColor("#D4AF37"))
                            views.setTextColor(textId, android.graphics.Color.parseColor("#0B2A22"))
                        } else {
                            views.setInt(containerId, "setBackgroundColor", android.graphics.Color.parseColor("#184A3B"))
                            views.setTextColor(textId, android.graphics.Color.parseColor("#E0E0E0"))
                        }
                    }
                }

                // Launch App intent when widget tapped
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = "com.example.ACTION_NAVIGATE_PRAYER_TIMES"
                    data = Uri.parse("holyquran://widget/prayer_times")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("navigate_to", "prayer_times")
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    PRAYER_WIDGET_CLICK_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)

                // Schedule alarm to wake up exactly at the target prayer time to update widget
                scheduleAlarmForTargetTime(context, nextPrayerInfo.targetPrayerTimeMs)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating app widget", e)
            }
        }

        fun sendUpdateBroadcast(context: Context) {
            try {
                val intent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_WIDGET
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending widget update broadcast", e)
            }
        }

        private fun scheduleAlarmForTargetTime(context: Context, targetMs: Long) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_WIDGET
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    WIDGET_ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerAt = if (targetMs > System.currentTimeMillis()) {
                    targetMs + 1000L
                } else {
                    System.currentTimeMillis() + 1800_000L // Fallback 30 min
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    } catch (e: Exception) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    }
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling widget alarm", e)
            }
        }

        fun scheduleAlarm(context: Context) {
            sendUpdateBroadcast(context)
        }

        fun cancelAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_WIDGET
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    WIDGET_ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling widget alarm", e)
            }
        }

        private fun parseTimeToHourAndMinute(timeStr: String): Pair<Int, Int>? {
            return try {
                val clean = timeStr.trim().uppercase()
                    .replace('٠', '0').replace('١', '1').replace('٢', '2')
                    .replace('٣', '3').replace('٤', '4').replace('٥', '5')
                    .replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
                val isPm = clean.contains("PM") || clean.contains("م")
                val isAm = clean.contains("AM") || clean.contains("ص")

                val digitsOnly = clean.replace("[^0-9:]".toRegex(), "").trim()
                val parts = digitsOnly.split(":")
                if (parts.size >= 2) {
                    var hour = parts[0].toIntOrNull() ?: return null
                    val minute = parts[1].toIntOrNull() ?: return null

                    if (isPm && hour < 12) {
                        hour += 12
                    } else if (isAm && hour == 12) {
                        hour = 0
                    }
                    Pair(hour, minute)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun calculateNextPrayerAndCountdown(prayerTimes: List<PrayerTime>): NextPrayerWidgetData {
            val now = Calendar.getInstance()
            val nowMs = now.timeInMillis

            var bestNextPrayer: PrayerTime? = null
            var bestTargetMs: Long = Long.MAX_VALUE

            for (prayer in prayerTimes) {
                if (prayer.nameEnglish == "Sunrise") continue
                val parsedTime = parseTimeToHourAndMinute(prayer.timeFormatted) ?: continue
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, parsedTime.first)
                    set(Calendar.MINUTE, parsedTime.second)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                var targetMs = cal.timeInMillis
                if (targetMs <= nowMs) {
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    targetMs = cal.timeInMillis
                }

                if (targetMs > nowMs && targetMs < bestTargetMs) {
                    bestTargetMs = targetMs
                    bestNextPrayer = prayer
                }
            }

            val selectedPrayer = bestNextPrayer ?: prayerTimes.firstOrNull { it.nameEnglish == "Fajr" } ?: prayerTimes.first()
            val finalTargetMs = if (bestTargetMs != Long.MAX_VALUE) bestTargetMs else (nowMs + 60_000L)

            return NextPrayerWidgetData(
                prayerNameAr = selectedPrayer.nameArabic,
                prayerNameEn = selectedPrayer.nameEnglish,
                prayerTimeFormatted = selectedPrayer.timeFormatted,
                targetPrayerTimeMs = finalTargetMs
            )
        }
    }
}

data class NextPrayerWidgetData(
    val prayerNameAr: String,
    val prayerNameEn: String,
    val prayerTimeFormatted: String,
    val targetPrayerTimeMs: Long
)
