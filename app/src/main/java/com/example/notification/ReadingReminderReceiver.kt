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

class ReadingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReadingReminderReceiver", "Received reading reminder alarm broadcast.")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val settings = db.quranDao().getUserSettingsOnce()

                if (settings?.enableReadingReminderNotification != false) {
                    val progress = db.quranDao().getReadingProgressOnce()
                    val lastPage = progress?.lastPage ?: 1
                    val lastSurah = progress?.lastSurahName ?: if (settings?.languageCode == "ar") "الفاتحة" else "Al-Fatiha"
                    val isArabic = settings?.languageCode == "ar"

                    NotificationHelper.createNotificationChannels(context)

                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val builder = NotificationHelper.buildReadingReminderNotification(
                        context = context,
                        lastPage = lastPage,
                        lastSurah = lastSurah,
                        isArabic = isArabic
                    )

                    notificationManager.notify(780, builder.build())
                    Log.d("ReadingReminderReceiver", "Reading reminder notification posted successfully.")
                } else {
                    Log.d("ReadingReminderReceiver", "Reading reminder is disabled in user settings. Skipping notification.")
                }

                // Reschedule alarm for next day
                ReadingReminderScheduler.scheduleAllReadingReminders(context)
            } catch (e: Exception) {
                Log.e("ReadingReminderReceiver", "Error handling reading reminder broadcast", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
