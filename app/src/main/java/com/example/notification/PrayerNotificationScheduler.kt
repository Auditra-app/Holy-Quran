package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.provider.QuranDataProvider
import com.example.domain.model.PrayerTime
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PrayerNotificationScheduler {

    private const val TAG = "PrayerNotificationSched"

    fun canonicalPrayerKey(nameEn: String, nameAr: String = ""): String {
        val en = nameEn.lowercase()
        val ar = nameAr.lowercase()
        return when {
            en.contains("fajr") || ar.contains("فجر") -> "fajr"
            en.contains("sunrise") || ar.contains("شروق") -> "sunrise"
            en.contains("dhuhr") || ar.contains("ظهر") -> "dhuhr"
            en.contains("asr") || ar.contains("عصر") -> "asr"
            en.contains("maghrib") || ar.contains("مغرب") -> "maghrib"
            en.contains("isha") || ar.contains("عشاء") -> "isha"
            else -> en.replace("[^a-z0-9]".toRegex(), "").trim()
        }
    }

    fun schedulePrayerNotifications(
        context: Context,
        prayerTimesToday: List<PrayerTime>? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel previous pending intents to prevent duplicates
        cancelPrayerNotifications(context)

        val loc = LocationHelper.getSavedLocationAndSettings(context)
        val nowMs = System.currentTimeMillis()
        val dedupPrefs = context.getSharedPreferences("prayer_dedup_prefs", Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        var scheduledCount = 0

        // Schedule for the next 7 days (today + 6 upcoming days) so alarms are always accurate and persistent
        for (dayOffset in 0..6) {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val dateString = dateFormat.format(targetCal.time)

            val prayerTimes = if (dayOffset == 0 && !prayerTimesToday.isNullOrEmpty()) {
                prayerTimesToday
            } else {
                QuranDataProvider.getPrayerTimes(
                    lat = loc.latitude,
                    lng = loc.longitude,
                    methodCode = loc.calculationMethod,
                    madhhab = "Shafi",
                    isSummerTime = loc.isSummerTime,
                    fajrOffset = loc.fajrOffset,
                    dhuhrOffset = loc.dhuhrOffset,
                    asrOffset = loc.asrOffset,
                    maghribOffset = loc.maghribOffset,
                    ishaOffset = loc.ishaOffset,
                    date = targetCal
                )
            }

            prayerTimes.forEachIndexed { prayerIndex, prayer ->
                try {
                    val prayerKey = canonicalPrayerKey(prayer.nameEnglish, prayer.nameArabic)
                    val parsedTime = parseTimeToHourAndMinute(prayer.timeFormatted)
                    if (parsedTime != null) {
                        val prayerCal = (targetCal.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, parsedTime.first)
                            set(Calendar.MINUTE, parsedTime.second)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        // Check if already triggered today
                        val triggeredKey = "triggered_${dateString}_${prayerKey}"
                        val lastTriggeredTime = dedupPrefs.getLong(triggeredKey, 0L)
                        val isAlreadyTriggered = (dayOffset == 0) && (lastTriggeredTime > 0L) && ((nowMs - lastTriggeredTime) < 18 * 60 * 60 * 1000L)

                        // ONLY schedule if the prayer time is strictly in the future (at least 10s from now)
                        val isFuture = if (dayOffset == 0) {
                            prayerCal.timeInMillis > (nowMs + 10_000L)
                        } else {
                            prayerCal.timeInMillis > nowMs
                        }

                        if (isFuture && !isAlreadyTriggered) {
                            val triggerTime = prayerCal.timeInMillis
                            val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                                action = "com.example.ACTION_PRAYER_ALARM_${dayOffset}_$prayerIndex"
                                data = android.net.Uri.parse("holyquran://prayer_alarm/${dayOffset}/${prayerIndex}")
                                putExtra(PrayerNotificationReceiver.EXTRA_PRAYER_NAME_AR, prayer.nameArabic)
                                putExtra(PrayerNotificationReceiver.EXTRA_PRAYER_NAME_EN, prayer.nameEnglish)
                                putExtra(PrayerNotificationReceiver.EXTRA_PRAYER_TIME, prayer.timeFormatted)
                            }

                            // Unique requestCode per day & prayer slot
                            val requestCode = 10000 + (dayOffset * 10) + prayerIndex
                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            scheduleAlarm(context, alarmManager, triggerTime, pendingIntent)
                            scheduledCount++
                            Log.d(TAG, "Scheduled ${prayer.nameEnglish} ($prayerIndex, Day +$dayOffset) at ${java.util.Date(triggerTime)}")
                        } else if (dayOffset == 0) {
                            Log.d(TAG, "Skipping passed/already-triggered prayer for today: ${prayer.nameEnglish} (${prayer.timeFormatted})")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error scheduling prayer alarm for ${prayer.nameEnglish}", e)
                }
            }
        }
        Log.d(TAG, "Total prayer notifications scheduled: $scheduledCount across 7 days")

        // Schedule interactive follow-up reminders ("Have you prayed?") for tracked prayers
        PrayerFollowUpScheduler.scheduleFollowUpNotifications(context)
    }

    fun parseTimeToHourAndMinute(timeStr: String): Pair<Int, Int>? {
        return try {
            val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
            var converted = timeStr.trim()
            for (i in 0..9) {
                converted = converted.replace(arabicDigits[i], '0' + i)
            }

            val clean = converted.uppercase()
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

    fun scheduleTestNotification(context: Context, delaySeconds: Long = 2) {
        NotificationHelper.createNotificationChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            action = "com.example.ACTION_TEST_PRAYER_ALARM"
            data = android.net.Uri.parse("holyquran://prayer_alarm/test")
            putExtra(PrayerNotificationReceiver.EXTRA_PRAYER_NAME_AR, "الفجر (اختبار)")
            putExtra(PrayerNotificationReceiver.EXTRA_PRAYER_NAME_EN, "Fajr (Test)")
            putExtra(PrayerNotificationReceiver.EXTRA_PRAYER_TIME, "الآن")
        }

        if (delaySeconds <= 0 || alarmManager == null) {
            context.sendBroadcast(intent)
            return
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMs = System.currentTimeMillis() + (delaySeconds * 1000L)
        scheduleAlarm(context, alarmManager, triggerAtMs, pendingIntent)
        Log.d(TAG, "Scheduled Test Prayer Notification in $delaySeconds seconds")
    }

    fun cancelPrayerNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (dayOffset in 0..6) {
            for (prayerIndex in 0..6) {
                val requestCode = 10000 + (dayOffset * 10) + prayerIndex
                val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                    action = "com.example.ACTION_PRAYER_ALARM_${dayOffset}_$prayerIndex"
                    data = android.net.Uri.parse("holyquran://prayer_alarm/${dayOffset}/${prayerIndex}")
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
        }
        val testIntent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            action = "com.example.ACTION_TEST_PRAYER_ALARM"
            data = android.net.Uri.parse("holyquran://prayer_alarm/test")
        }
        val testPending = PendingIntent.getBroadcast(
            context,
            9999,
            testIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (testPending != null) {
            alarmManager.cancel(testPending)
            testPending.cancel()
        }
    }

    private fun scheduleAlarm(context: Context, alarmManager: AlarmManager, triggerTimeMs: Long, pendingIntent: PendingIntent) {
        // 1. ALWAYS try setAlarmClock first as it is the highest-priority wake mechanism in Android (bypasses Doze, App Standby, wakes locked mobile)
        try {
            val showIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, com.example.MainActivity::class.java).apply {
                    action = "com.example.ACTION_NAVIGATE_PRAYER_TIMES"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val clockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            Log.d(TAG, "Successfully scheduled with setAlarmClock for timeMs=$triggerTimeMs")
            return
        } catch (e: Throwable) {
            Log.w(TAG, "setAlarmClock failed, trying exact alarm fallbacks", e)
        }

        // 2. Exact alarm fallback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
            Log.d(TAG, "Successfully scheduled with exact alarm fallback for timeMs=$triggerTimeMs")
        } catch (e: Throwable) {
            Log.e(TAG, "Exact alarm failed, falling back to setAndAllowWhileIdle", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                }
            } catch (ex: Throwable) {
                Log.e(TAG, "All alarm scheduling failed", ex)
            }
        }
    }
}
