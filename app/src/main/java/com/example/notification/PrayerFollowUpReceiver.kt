package com.example.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.data.local.PrayerRecordEntity
import com.example.data.local.QuranDatabase
import com.example.widget.PrayerTrackerWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerFollowUpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: "fajr"
        val prayerNameAr = intent.getStringExtra(EXTRA_PRAYER_NAME_AR) ?: "الصلاة"
        val prayerNameEn = intent.getStringExtra(EXTRA_PRAYER_NAME_EN) ?: "Prayer"
        val dateString = intent.getStringExtra(EXTRA_DATE_STRING)
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val defaultNotifId = PrayerFollowUpScheduler.getPrayerNotificationId(prayerKey)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, defaultNotifId)

        Log.d(TAG, "onReceive: action=$action, prayer=$prayerKey, date=$dateString, notifId=$notificationId")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val dao = db.quranDao()
                val settings = dao.getUserSettingsOnce()

                when (action) {
                    ACTION_TRIGGER_PRAYER_FOLLOWUP -> {
                        if (settings?.enablePrayerFollowUpNotification == false) {
                            Log.d(TAG, "Follow-up notifications disabled in settings. Skipping.")
                            return@launch
                        }

                        val trackedList = (settings?.trackedPrayersJson ?: "fajr,dhuhr,asr,maghrib,isha")
                            .split(",").map { it.trim().lowercase() }
                        if (!trackedList.contains(prayerKey.lowercase())) {
                            Log.d(TAG, "Prayer $prayerKey is not tracked by user. Skipping follow-up.")
                            return@launch
                        }

                        // Check deduplication in SharedPreferences for this specific date and prayer
                        val prefs = context.getSharedPreferences("prayer_followup_dedup", Context.MODE_PRIVATE)
                        val shownKey = "followup_shown_${dateString}_${prayerKey.lowercase()}"
                        val lastShownTimestamp = prefs.getLong(shownKey, 0L)
                        val now = System.currentTimeMillis()
                        // If shown within 12 hours for the same prayer and date, suppress duplicate notification
                        if (lastShownTimestamp > 0 && (now - lastShownTimestamp < 12 * 60 * 60 * 1000L)) {
                            Log.d(TAG, "Follow-up notification already shown for $prayerKey on $dateString. Suppressing duplicate.")
                            return@launch
                        }

                        // Check if already completed in DB
                        val existingRecord = dao.getPrayerRecord(dateString, prayerKey)
                        if (existingRecord?.isCompleted == true) {
                            Log.d(TAG, "Prayer $prayerKey for $dateString is already completed. Skipping notification.")
                            return@launch
                        }

                        // Mark as shown immediately before notifying
                        prefs.edit().putLong(shownKey, now).apply()

                        // Build and show follow-up notification
                        val builder = NotificationHelper.buildPrayerFollowUpNotification(
                            context = context,
                            prayerKey = prayerKey,
                            prayerNameAr = prayerNameAr,
                            prayerNameEn = prayerNameEn,
                            dateString = dateString,
                            notificationId = notificationId
                        )
                        notificationManager?.notify(notificationId, builder.build())
                        Log.d(TAG, "Successfully displayed single follow-up notification for $prayerKey on $dateString (ID: $notificationId)")
                    }

                    ACTION_PRAYER_CONFIRM_YES -> {
                        // User answered "Yes, I prayed"
                        val existingRecord = dao.getPrayerRecord(dateString, prayerKey)
                        val updated = PrayerRecordEntity(
                            id = existingRecord?.id ?: 0,
                            dateString = dateString,
                            prayerKey = prayerKey,
                            isCompleted = true,
                            isOnTime = true,
                            completedTimestamp = System.currentTimeMillis()
                        )
                        dao.insertPrayerRecord(updated)

                        // Cancel the interactive question notification
                        notificationManager?.cancel(notificationId)

                        // Update Widgets
                        PrayerTrackerWidgetProvider.sendUpdateBroadcast(context)

                        // Show confirmation toast
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "تقبل الله طاعتكم! تم تسجيل صلاة $prayerNameAr في متابع الصلوات 🤲",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    ACTION_PRAYER_CONFIRM_NO -> {
                        // User answered "Not yet"
                        val existingRecord = dao.getPrayerRecord(dateString, prayerKey)
                        val updated = PrayerRecordEntity(
                            id = existingRecord?.id ?: 0,
                            dateString = dateString,
                            prayerKey = prayerKey,
                            isCompleted = false,
                            isOnTime = false,
                            completedTimestamp = null
                        )
                        dao.insertPrayerRecord(updated)

                        // Update Widgets
                        PrayerTrackerWidgetProvider.sendUpdateBroadcast(context)

                        // Display motivation notification
                        val motivationBuilder = NotificationHelper.buildPrayerMotivationNotification(
                            context = context,
                            prayerKey = prayerKey,
                            prayerNameAr = prayerNameAr,
                            dateString = dateString
                        )
                        notificationManager?.notify(notificationId, motivationBuilder.build())

                        // Show motivational toast
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "خير الأعمال.. الصلاة على وقتها 🕌 بارك الله فيك، لا تؤخر الصلاة",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing follow-up action: $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PrayerFollowUpReceiver"
        const val ACTION_TRIGGER_PRAYER_FOLLOWUP = "com.example.notification.ACTION_TRIGGER_PRAYER_FOLLOWUP"
        const val ACTION_PRAYER_CONFIRM_YES = "com.example.notification.ACTION_PRAYER_CONFIRM_YES"
        const val ACTION_PRAYER_CONFIRM_NO = "com.example.notification.ACTION_PRAYER_CONFIRM_NO"

        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_PRAYER_NAME_AR = "extra_prayer_name_ar"
        const val EXTRA_PRAYER_NAME_EN = "extra_prayer_name_en"
        const val EXTRA_DATE_STRING = "extra_date_string"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        const val BASE_NOTIFICATION_ID = 40000
    }
}
