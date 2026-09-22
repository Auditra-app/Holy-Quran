package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.local.QuranDatabase
import com.example.data.provider.QuranDataProvider
import com.example.domain.model.TrackedPrayerType
import com.example.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PrayerFollowUpScheduler {

    private const val TAG = "PrayerFollowUpScheduler"
    private const val BASE_REQUEST_CODE = 50000

    private val PRAYER_KEYS = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

    fun getPrayerSlotIndex(prayerKey: String): Int {
        val idx = PRAYER_KEYS.indexOf(prayerKey.lowercase().trim())
        return if (idx >= 0) idx else 5
    }

    fun getPrayerNotificationId(prayerKey: String): Int {
        return when (prayerKey.lowercase().trim()) {
            "fajr" -> 50001
            "dhuhr" -> 50002
            "asr" -> 50003
            "maghrib" -> 50004
            "isha" -> 50005
            else -> 50006
        }
    }

    private fun getRequestCode(dateString: String, prayerKey: String): Int {
        val cleanDate = dateString.replace("-", "")
        val dateVal = cleanDate.toIntOrNull() ?: (dateString.hashCode().let { if (it < 0) -it else it })
        val prayerSlot = getPrayerSlotIndex(prayerKey)
        return BASE_REQUEST_CODE + ((dateVal % 1000) * 10) + prayerSlot
    }

    fun scheduleFollowUpNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val settings = db.quranDao().getUserSettingsOnce()

                if (settings?.enablePrayerFollowUpNotification == false) {
                    Log.d(TAG, "Follow-up notifications disabled. Cancelling existing alarms.")
                    cancelAllFollowUps(context)
                    return@launch
                }

                val delayMinutes = settings?.prayerFollowUpDelayMinutes ?: 30
                val trackedKeys = (settings?.trackedPrayersJson ?: "fajr,dhuhr,asr,maghrib,isha")
                    .split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }

                val loc = LocationHelper.getSavedLocationAndSettings(context)
                val nowMs = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                var scheduledCount = 0

                // Schedule for next 7 days
                for (dayOffset in 0..6) {
                    val targetCal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, dayOffset)
                    }
                    val dateString = dateFormat.format(targetCal.time)

                    val prayerTimes = QuranDataProvider.getPrayerTimes(
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

                    prayerTimes.forEach { prayer ->
                        val prayerKey = prayer.nameEnglish.lowercase().trim()
                        if (prayerKey == "sunrise") return@forEach // Skip sunrise for prayers tracking
                        if (!trackedKeys.contains(prayerKey)) return@forEach

                        val parsedTime = PrayerNotificationScheduler.parseTimeToHourAndMinute(prayer.timeFormatted)
                        if (parsedTime != null) {
                            val prayerCal = (targetCal.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, parsedTime.first)
                                set(Calendar.MINUTE, parsedTime.second)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                                add(Calendar.MINUTE, delayMinutes)
                            }

                            if (prayerCal.timeInMillis > nowMs + 60_000L) {
                                val requestCode = getRequestCode(dateString, prayerKey)
                                val notifId = getPrayerNotificationId(prayerKey)

                                val intent = Intent(context, PrayerFollowUpReceiver::class.java).apply {
                                    action = PrayerFollowUpReceiver.ACTION_TRIGGER_PRAYER_FOLLOWUP
                                    data = Uri.parse("holyquran://prayer_followup/$dateString/$prayerKey")
                                    putExtra(PrayerFollowUpReceiver.EXTRA_PRAYER_KEY, prayerKey)
                                    putExtra(PrayerFollowUpReceiver.EXTRA_PRAYER_NAME_AR, prayer.nameArabic)
                                    putExtra(PrayerFollowUpReceiver.EXTRA_PRAYER_NAME_EN, prayer.nameEnglish)
                                    putExtra(PrayerFollowUpReceiver.EXTRA_DATE_STRING, dateString)
                                    putExtra(PrayerFollowUpReceiver.EXTRA_NOTIFICATION_ID, notifId)
                                }

                                val pendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    requestCode,
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )

                                scheduleExactAlarm(alarmManager, prayerCal.timeInMillis, pendingIntent)
                                scheduledCount++
                                Log.d(TAG, "Scheduled follow-up for $prayerKey on $dateString at ${prayerCal.time}")
                            }
                        }
                    }
                }
                Log.d(TAG, "Total prayer follow-up alarms scheduled: $scheduledCount")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling prayer follow-up alarms", e)
            }
        }
    }

    fun cancelAllFollowUps(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()

            for (dayOffset in 0..6) {
                val targetCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
                val dateString = dateFormat.format(targetCal.time)

                for (prayerKey in PRAYER_KEYS) {
                    val requestCode = getRequestCode(dateString, prayerKey)
                    val intent = Intent(context, PrayerFollowUpReceiver::class.java).apply {
                        action = PrayerFollowUpReceiver.ACTION_TRIGGER_PRAYER_FOLLOWUP
                        data = Uri.parse("holyquran://prayer_followup/$dateString/$prayerKey")
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
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling follow-up alarms", e)
        }
    }

    private fun scheduleExactAlarm(
        alarmManager: AlarmManager,
        triggerAtMs: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }
}
