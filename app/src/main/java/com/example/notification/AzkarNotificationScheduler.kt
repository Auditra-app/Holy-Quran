package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.QuranDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object AzkarNotificationScheduler {

    private const val TAG = "AzkarNotificationSched"

    fun scheduleAllAzkarNotifications(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val settings = db.quranDao().getUserSettingsOnce() ?: com.example.data.local.UserSettingsEntity()

                if (!settings.enableAzkarNotification) {
                    Log.d(TAG, "Azkar notifications disabled in settings. Cancelling alarms.")
                    cancelAzkarNotifications(context)
                    return@launch
                }

                if (settings.enableMorningAzkar) {
                    scheduleMorningAzkar(context, settings.morningAzkarHour, settings.morningAzkarMinute)
                } else {
                    cancelAlarm(context, requestCode = 9001)
                }

                if (settings.enableEveningAzkar) {
                    scheduleEveningAzkar(context, settings.eveningAzkarHour, settings.eveningAzkarMinute)
                } else {
                    cancelAlarm(context, requestCode = 9002)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in scheduleAllAzkarNotifications", e)
            }
        }
    }

    fun scheduleMorningAzkar(context: Context, hour: Int = 6, minute: Int = 30) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        scheduleAlarm(context, cal.timeInMillis, isMorning = true, requestCode = 9001)
    }

    fun scheduleEveningAzkar(context: Context, hour: Int = 17, minute: Int = 0) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        scheduleAlarm(context, cal.timeInMillis, isMorning = false, requestCode = 9002)
    }

    fun scheduleTestAzkarNotification(context: Context, isMorning: Boolean, delaySeconds: Long = 2) {
        NotificationHelper.createNotificationChannels(context)
        val triggerTimeMs = System.currentTimeMillis() + (delaySeconds * 1000)
        scheduleAlarm(context, triggerTimeMs, isMorning, requestCode = if (isMorning) 9091 else 9092)
    }

    fun cancelAzkarNotifications(context: Context) {
        cancelAlarm(context, 9001)
        cancelAlarm(context, 9002)
    }

    private fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AzkarNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleAlarm(context: Context, triggerTimeMs: Long, isMorning: Boolean, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AzkarNotificationReceiver::class.java).apply {
            putExtra("is_morning", isMorning)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
            Log.d(TAG, "Scheduled Azkar Alarm (isMorning=$isMorning) for $triggerTimeMs")
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm. Falling back to inexact alarm", se)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Fallback failed to schedule Azkar alarm", e)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to schedule Azkar alarm, using inexact fallback", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Inexact fallback failed", ex)
            }
        }
    }
}
