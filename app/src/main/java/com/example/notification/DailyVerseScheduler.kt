package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.provider.QuranDataProvider
import com.example.util.LocationHelper
import java.util.Calendar
import kotlin.math.abs

object DailyVerseScheduler {

    private const val TAG = "DailyVerseScheduler"

    // Base interval hours: 8:00 AM, 11:00 AM, 2:00 PM (14), 5:00 PM (17), 8:00 PM (20)
    private val BASE_HOURS = listOf(8, 11, 14, 17, 20)

    fun scheduleDailyVerseNotification(context: Context, baseHour: Int = 8, baseMinute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val loc = LocationHelper.getSavedLocationAndSettings(context)
        val nowMs = System.currentTimeMillis()

        // Schedule across 7 days so notifications are continuous and resilient
        for (dayOffset in 0..6) {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            // Get prayer times for this day to avoid conflicts
            val prayerTimes = try {
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
            } catch (e: Exception) {
                emptyList()
            }

            // Convert prayer times to total minutes of day
            val prayerMinutesList = prayerTimes.mapNotNull { prayer ->
                PrayerNotificationScheduler.parseTimeToHourAndMinute(prayer.timeFormatted)?.let { (h, m) ->
                    h * 60 + m
                }
            }

            BASE_HOURS.forEachIndexed { slotIndex, baseH ->
                val baseMins = baseH * 60
                var adjustedMins = baseMins

                // Ensure random verse notification is at least 25 minutes apart from any prayer time
                var hasConflict = true
                var attempts = 0
                while (hasConflict && attempts < 10) {
                    val conflictPrayer = prayerMinutesList.find { pMins -> abs(adjustedMins - pMins) < 25 }
                    if (conflictPrayer != null) {
                        // Shift 25 minutes past prayer time
                        adjustedMins = conflictPrayer + 25
                        attempts++
                    } else {
                        hasConflict = false
                    }
                }

                // Bound adjusted minutes strictly between 8:00 AM (480 mins) and 10:00 PM (1320 mins)
                if (adjustedMins > 1320) {
                    adjustedMins = 1320
                }
                if (adjustedMins < 480) {
                    adjustedMins = 480
                }

                val targetHour = adjustedMins / 60
                val targetMinute = adjustedMins % 60

                val slotCal = (targetCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, targetMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (slotCal.timeInMillis > nowMs) {
                    val intent = Intent(context, DailyVerseReceiver::class.java).apply {
                        putExtra("slot_index", slotIndex)
                    }
                    val requestCode = 8880 + (dayOffset * 10) + slotIndex
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    scheduleAlarmSafely(alarmManager, slotCal.timeInMillis, pendingIntent)
                    Log.d(TAG, "Scheduled Daily Verse Slot $slotIndex ($targetHour:$targetMinute) Day +$dayOffset for ${slotCal.time}")
                }
            }
        }
    }

    private fun scheduleAlarmSafely(alarmManager: AlarmManager, triggerTimeMs: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val clockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, pendingIntent)
                    alarmManager.setAlarmClock(clockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val clockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, pendingIntent)
                alarmManager.setAlarmClock(clockInfo, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing, falling back to inexact alarm", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (ex: Throwable) {
                Log.e(TAG, "Failed inexact alarm fallback", ex)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error scheduling Daily Verse alarm, trying inexact fallback", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (ex: Throwable) {
                Log.e(TAG, "Failed fallback for Daily Verse slot", ex)
            }
        }
    }

    fun scheduleTestDailyVerseNotification(context: Context, delaySeconds: Long = 2) {
        NotificationHelper.createNotificationChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMs = System.currentTimeMillis() + (delaySeconds * 1000)

        val intent = Intent(context, DailyVerseReceiver::class.java).apply {
            putExtra("slot_index", 0)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            8899,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmSafely(alarmManager, triggerTimeMs, pendingIntent)
        Log.d(TAG, "Scheduled Test Daily Verse Notification in $delaySeconds seconds")
    }
}
