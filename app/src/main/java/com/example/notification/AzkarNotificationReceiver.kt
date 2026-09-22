package com.example.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.QuranDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AzkarNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isMorning = intent.getBooleanExtra("is_morning", true)
        Log.d("AzkarNotificationReceiver", "Received Azkar notification alarm. isMorning=$isMorning")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val settings = db.quranDao().getUserSettingsOnce()

                if (settings?.enableAzkarNotification == false) {
                    Log.d("AzkarNotificationReceiver", "Azkar notifications disabled in settings. Skipping.")
                    return@launch
                }

                if (isMorning && settings?.enableMorningAzkar == false) {
                    Log.d("AzkarNotificationReceiver", "Morning Azkar notifications disabled. Skipping.")
                    return@launch
                }

                if (!isMorning && settings?.enableEveningAzkar == false) {
                    Log.d("AzkarNotificationReceiver", "Evening Azkar notifications disabled. Skipping.")
                    return@launch
                }

                val isArabic = settings?.languageCode == "ar"

                NotificationHelper.createNotificationChannels(context)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val builder = NotificationHelper.buildAzkarNotification(
                    context = context,
                    isMorning = isMorning,
                    isArabic = isArabic
                )

                val notificationId = if (isMorning) 9001 else 9002
                notificationManager.notify(notificationId, builder.build())

                // Update Azkar widget to match time transition
                com.example.widget.AzkarWidgetProvider.sendUpdateBroadcast(context)

                // Reschedule for next day using custom user settings time
                val hour = if (isMorning) (settings?.morningAzkarHour ?: 6) else (settings?.eveningAzkarHour ?: 17)
                val minute = if (isMorning) (settings?.morningAzkarMinute ?: 30) else (settings?.eveningAzkarMinute ?: 0)

                if (isMorning) {
                    AzkarNotificationScheduler.scheduleMorningAzkar(context, hour, minute)
                } else {
                    AzkarNotificationScheduler.scheduleEveningAzkar(context, hour, minute)
                }
            } catch (e: Exception) {
                Log.e("AzkarNotificationReceiver", "Error triggering Azkar notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
