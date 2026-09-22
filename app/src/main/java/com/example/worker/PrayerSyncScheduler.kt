package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PrayerSyncScheduler {

    private const val TAG = "PrayerSyncScheduler"

    /**
     * Schedules a background periodic WorkManager task that runs 5 times a day (every 4 hours 48 minutes)
     * to check location, recalculate prayer times, refresh database cache, update notifications, and refresh widgets.
     */
    fun schedulePeriodicPrayerSync(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            val periodicWorkRequest = PeriodicWorkRequestBuilder<PrayerSyncWorker>(
                288, TimeUnit.MINUTES,
                30, TimeUnit.MINUTES
            ).build()

            workManager.enqueueUniquePeriodicWork(
                PrayerSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
            Log.d(TAG, "Periodic prayer sync scheduled successfully (5 times a day / 288 min interval).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic prayer sync", e)
        }
    }

    /**
     * Triggers an immediate background synchronization using WorkManager
     * whenever settings or location are modified or after system boot.
     */
    fun enqueueImmediatePrayerSync(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<PrayerSyncWorker>()
                .build()

            workManager.enqueueUniqueWork(
                PrayerSyncWorker.ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest
            )
            Log.d(TAG, "Immediate prayer sync enqueued successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue immediate prayer sync", e)
        }
    }
}
