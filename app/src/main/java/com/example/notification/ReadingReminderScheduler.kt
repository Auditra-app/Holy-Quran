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

object ReadingReminderScheduler {

    private const val TAG = "ReadingReminderSched"
    private const val REQUEST_CODE = 9080

    fun scheduleAllReadingReminders(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val settings = db.quranDao().getUserSettingsOnce() ?: com.example.data.local.UserSettingsEntity()

                if (!settings.enableReadingReminderNotification) {
                    Log.d(TAG, "Reading reminder disabled in settings. Cancelling alarm.")
                    cancelReadingReminder(context)
                    return@launch
                }

                val hour = settings.notificationTimeHour.takeIf { it in 0..23 } ?: 20
                val minute = settings.notificationTimeMinute.takeIf { it in 0..59 } ?: 0

                scheduleReadingReminder(context, hour, minute)
            } catch (e: Exception) {
                Log.e(TAG, "Error in scheduleAllReadingReminders", e)
            }
        }
    }

    fun scheduleReadingReminder(context: Context, hour: Int = 20, minute: Int = 0) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReadingReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmSafely(alarmManager, cal.timeInMillis, pendingIntent)
        Log.d(TAG, "Scheduled Reading Reminder alarm for ${cal.time}")
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
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm. Falling back to inexact alarm", se)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Fallback failed to schedule Reading Reminder alarm", e)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to schedule Reading Reminder alarm", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Inexact fallback failed", ex)
            }
        }
    }

    fun scheduleTestReadingReminder(context: Context, delaySeconds: Long = 2) {
        NotificationHelper.createNotificationChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMs = System.currentTimeMillis() + (delaySeconds * 1000)

        val intent = Intent(context, ReadingReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9081,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmSafely(alarmManager, triggerTimeMs, pendingIntent)
        Log.d(TAG, "Scheduled test reading reminder in $delaySeconds seconds.")
    }

    fun cancelReadingReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReadingReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled Reading Reminder alarm.")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling Reading Reminder alarm", e)
        }
    }
}
