package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.QuranDatabase
import com.example.data.provider.QuranDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyVerseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val settings = db.quranDao().getUserSettingsOnce()

                if (settings?.enableDailyVerseNotification != false) {
                    val slotIndex = intent.getIntExtra("slot_index", 0)
                    val dailyVerse = QuranDataProvider.getNotificationVerseForSlot(slotIndex)

                    val isArabic = settings?.languageCode == "ar"
                    val referenceText = if (isArabic) {
                        "سورة ${dailyVerse.surahNameArabic} - آية ${dailyVerse.verseNumber}"
                    } else {
                        "Surah ${dailyVerse.surahNameEnglish} - Verse ${dailyVerse.verseNumber}"
                    }

                    NotificationHelper.createNotificationChannels(context)

                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val builder = NotificationHelper.buildDailyVerseNotification(
                        context = context,
                        ayaText = dailyVerse.textArabic,
                        surahAndVerseRef = referenceText,
                        isArabic = isArabic,
                        slotIndex = slotIndex
                    )

                    // Unique notification ID per slot (770, 771, 772, 773)
                    notificationManager.notify(770 + slotIndex, builder.build())
                }

                // Reschedule for tomorrow
                DailyVerseScheduler.scheduleDailyVerseNotification(context)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
