package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.example.data.local.QuranDatabase
import com.example.data.provider.QuranDataProvider
import com.example.notification.AzkarNotificationScheduler
import com.example.notification.DailyVerseScheduler
import com.example.notification.NotificationHelper
import com.example.notification.PrayerNotificationScheduler
import com.example.notification.ReadingReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HolyQuranApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    private val coroutineExceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled exception in applicationScope: ${throwable.localizedMessage}", throwable)
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler)

    companion object {
        private const val TAG = "HolyQuranApplication"
        var instance: HolyQuranApplication? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val startTime = System.currentTimeMillis()
        Log.d(TAG, " HolyQuranApplication.onCreate: Global application initialization starting on thread [${Thread.currentThread().name}]")

        // 1. Setup global uncaught exception handler to log anomalies safely
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread [${thread.name}]: ${throwable.localizedMessage}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // 2. Pre-initialize notification channels on the main thread
        try {
            Log.d(TAG, " HolyQuranApplication.onCreate: Setting up notification channels...")
            NotificationHelper.createNotificationChannels(this)
            Log.d(TAG, " HolyQuranApplication.onCreate: Notification channels created successfully.")
        } catch (e: Throwable) {
            Log.e(TAG, " Error creating notification channels in Application", e)
        }

        // 3. Async pre-warming of database and schedulers to ensure instantaneous UI startup
        applicationScope.launch {
            val bgStartTime = System.currentTimeMillis()
            Log.d(TAG, " HolyQuranApplication: Starting async background service pre-warming...")
            try {
                QuranDatabase.getDatabase(this@HolyQuranApplication)
                Log.d(TAG, " HolyQuranApplication: Database pre-warmed.")

                val locData = try {
                    com.example.util.LocationHelper.getSavedLocationAndSettings(this@HolyQuranApplication)
                } catch (e: Throwable) {
                    Log.w(TAG, "Location settings retrieval fallback", e)
                    null
                }

                if (locData != null) {
                    try {
                        val initialPrayerTimes = QuranDataProvider.getPrayerTimes(
                            lat = locData.latitude,
                            lng = locData.longitude,
                            methodCode = locData.calculationMethod,
                            madhhab = "Shafi",
                            isSummerTime = locData.isSummerTime,
                            fajrOffset = locData.fajrOffset,
                            dhuhrOffset = locData.dhuhrOffset,
                            asrOffset = locData.asrOffset,
                            maghribOffset = locData.maghribOffset,
                            ishaOffset = locData.ishaOffset
                        )
                        PrayerNotificationScheduler.schedulePrayerNotifications(
                            this@HolyQuranApplication,
                            initialPrayerTimes
                        )
                    } catch (e: Throwable) {
                        Log.w(TAG, "Prayer notification schedule skipped", e)
                    }
                }

                try { DailyVerseScheduler.scheduleDailyVerseNotification(this@HolyQuranApplication) } catch (e: Throwable) { Log.w(TAG, "DailyVerseScheduler error", e) }
                try { AzkarNotificationScheduler.scheduleAllAzkarNotifications(this@HolyQuranApplication) } catch (e: Throwable) { Log.w(TAG, "AzkarNotificationScheduler error", e) }
                try { ReadingReminderScheduler.scheduleAllReadingReminders(this@HolyQuranApplication) } catch (e: Throwable) { Log.w(TAG, "ReadingReminderScheduler error", e) }
                try { com.example.notification.AutoLocationScheduler.scheduleAutoLocationCheck(this@HolyQuranApplication) } catch (e: Throwable) { Log.w(TAG, "AutoLocationScheduler error", e) }
                try { com.example.worker.PrayerSyncScheduler.schedulePeriodicPrayerSync(this@HolyQuranApplication) } catch (e: Throwable) { Log.w(TAG, "PrayerSyncScheduler periodic error", e) }
                try { com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(this@HolyQuranApplication) } catch (e: Throwable) { Log.w(TAG, "PrayerSyncScheduler immediate error", e) }
                try { com.example.widget.PrayerTimesWidgetProvider.sendUpdateBroadcast(this@HolyQuranApplication) } catch (e: Throwable) { Log.w(TAG, "PrayerTimesWidget update error", e) }
                try { com.example.widget.DailyVerseWidgetProvider.sendUpdateBroadcast(this@HolyQuranApplication) } catch (e: Throwable) { Log.w(TAG, "DailyVerseWidget update error", e) }
                try { com.example.widget.ReadingStatsWidgetProvider.notifyWidgetUpdate(this@HolyQuranApplication) } catch (e: Throwable) { Log.w(TAG, "ReadingStatsWidget update error", e) }
                try { com.example.widget.AzkarWidgetProvider.sendUpdateBroadcast(this@HolyQuranApplication) } catch (e: Throwable) { Log.w(TAG, "AzkarWidget update error", e) }
                Log.d(TAG, " HolyQuranApplication: Background schedulers initialized in ${System.currentTimeMillis() - bgStartTime}ms.")
            } catch (e: Throwable) {
                Log.e(TAG, " Error during Application background initialization", e)
            }
        }
        Log.d(TAG, " HolyQuranApplication.onCreate: Main thread setup completed in ${System.currentTimeMillis() - startTime}ms.")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, " HolyQuranApplication.onLowMemory: System is critically low on memory, clearing caches.")
        try {
            NotificationHelper.clearBitmapCache()
            System.gc()
        } catch (e: Throwable) {
            Log.e(TAG, "Error during onLowMemory cleanup", e)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, " HolyQuranApplication.onTrimMemory: Level = $level")
        if (level >= TRIM_MEMORY_BACKGROUND) {
            try {
                NotificationHelper.clearBitmapCache()
            } catch (e: Throwable) {
                Log.e(TAG, "Error during onTrimMemory cleanup", e)
            }
        }
    }
}
