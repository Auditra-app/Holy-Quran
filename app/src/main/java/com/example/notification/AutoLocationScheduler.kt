package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object AutoLocationScheduler {

    private const val TAG = "AutoLocationScheduler"
    private const val AUTO_LOCATION_REQUEST_CODE = 99100
    const val ACTION_AUTO_LOCATION_CHECK = "com.example.ACTION_AUTO_LOCATION_CHECK"

    fun scheduleAutoLocationCheck(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AutoLocationReceiver::class.java).apply {
                action = ACTION_AUTO_LOCATION_CHECK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                AUTO_LOCATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set next trigger to 4 hours and 48 minutes from now (5 times a day: 24h / 5 = 4h 48m)
            val calendar = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 288)
            }
            val triggerAtMs = calendar.timeInMillis

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
            Log.d(TAG, "Auto location check scheduled (5 times a day) at ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling auto location check", e)
        }
    }

    fun cancelAutoLocationCheck(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AutoLocationReceiver::class.java).apply {
                action = ACTION_AUTO_LOCATION_CHECK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                AUTO_LOCATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling auto location check", e)
        }
    }
}
