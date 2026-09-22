package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.QuranDatabase
import com.example.data.provider.QuranDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            Log.d("BootReceiver", "Device boot/time change detected: $action. Rescheduling alarms...")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = QuranDatabase.getDatabase(context)
                    val settings = db.quranDao().getUserSettingsOnce()

                    val loc = com.example.util.LocationHelper.getSavedLocationAndSettings(context)
                    val madhhab = settings?.madhhab ?: "Shafi"

                    val prayerTimes = QuranDataProvider.getPrayerTimes(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        methodCode = loc.calculationMethod,
                        madhhab = madhhab,
                        isSummerTime = loc.isSummerTime,
                        fajrOffset = loc.fajrOffset,
                        dhuhrOffset = loc.dhuhrOffset,
                        asrOffset = loc.asrOffset,
                        maghribOffset = loc.maghribOffset,
                        ishaOffset = loc.ishaOffset
                    )

                    // Schedule Prayer Alarms
                    PrayerNotificationScheduler.schedulePrayerNotifications(context, prayerTimes)
                    PrayerFollowUpScheduler.scheduleFollowUpNotifications(context)

                    // Schedule Daily Verse Alarm
                    val hour = settings?.notificationTimeHour ?: 9
                    val minute = settings?.notificationTimeMinute ?: 0
                    if (settings?.enableDailyVerseNotification != false) {
                        DailyVerseScheduler.scheduleDailyVerseNotification(context, hour, minute)
                    }

                    // Schedule Azkar Reminders
                    AzkarNotificationScheduler.scheduleAllAzkarNotifications(context)

                    // Schedule Reading Reminder
                    ReadingReminderScheduler.scheduleAllReadingReminders(context)

                    // Schedule Auto Location Check
                    AutoLocationScheduler.scheduleAutoLocationCheck(context)

                    // Enqueue WorkManager prayer sync
                    com.example.worker.PrayerSyncScheduler.schedulePeriodicPrayerSync(context)
                    com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(context)

                    // Update Home Screen Widgets
                    com.example.widget.PrayerTimesWidgetProvider.sendUpdateBroadcast(context)
                    com.example.widget.PrayerTrackerWidgetProvider.sendUpdateBroadcast(context)
                    com.example.widget.DailyVerseWidgetProvider.sendUpdateBroadcast(context)
                    com.example.widget.ReadingStatsWidgetProvider.notifyWidgetUpdate(context)
                    com.example.widget.AzkarWidgetProvider.sendUpdateBroadcast(context)
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
