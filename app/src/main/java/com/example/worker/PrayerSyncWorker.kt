package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.QuranDatabase
import com.example.data.provider.QuranDataProvider
import com.example.notification.PrayerNotificationScheduler
import com.example.util.*
import com.example.widget.DailyVerseWidgetProvider
import com.example.widget.PrayerTimesWidgetProvider
import com.example.widget.ReadingStatsWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class PrayerSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "PrayerSyncWorker"
        const val WORK_NAME = "PrayerSyncPeriodicWork"
        const val ONE_TIME_WORK_NAME = "PrayerSyncOneTimeWork"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting background prayer sync worker execution...")

            val db = QuranDatabase.getDatabase(applicationContext)
            val settings = db.quranDao().getUserSettingsOnce()

            val locSettings = LocationHelper.getSavedLocationAndSettings(applicationContext)
            var latitude = locSettings.latitude
            var longitude = locSettings.longitude
            var cityName = locSettings.cityName

            if (LocationHelper.hasLocationPermission(applicationContext)) {
                try {
                    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                        LocationHelper.getCurrentLocation(applicationContext) { lat, lng, city ->
                            latitude = lat
                            longitude = lng
                            cityName = city
                            LocationHelper.saveLocationAndSettings(applicationContext, lat, lng, city)
                            if (continuation.isActive) {
                                continuation.resume(Unit) {}
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Could not fetch fresh location in PrayerSyncWorker, using saved location", e)
                }
            }

            val calcMethod = settings?.calculationMethod ?: locSettings.calculationMethod
            val madhhab = settings?.madhhab ?: "Shafi"
            val isSummerTime = settings?.isSummerTime ?: locSettings.isSummerTime

            // 1. Calculate offline prayer times for today using stored/active settings
            val prayerTimes = QuranDataProvider.getPrayerTimes(
                lat = latitude,
                lng = longitude,
                methodCode = calcMethod,
                madhhab = madhhab,
                isSummerTime = isSummerTime,
                fajrOffset = locSettings.fajrOffset,
                dhuhrOffset = locSettings.dhuhrOffset,
                asrOffset = locSettings.asrOffset,
                maghribOffset = locSettings.maghribOffset,
                ishaOffset = locSettings.ishaOffset
            )

            // 2. Schedule exact prayer alarms for upcoming prayer times
            PrayerNotificationScheduler.schedulePrayerNotifications(applicationContext, prayerTimes)

            // 3. Update all widgets so they reflect latest accurate prayer times and countdowns
            PrayerTimesWidgetProvider.sendUpdateBroadcast(applicationContext)
            DailyVerseWidgetProvider.sendUpdateBroadcast(applicationContext)
            ReadingStatsWidgetProvider.notifyWidgetUpdate(applicationContext)
            com.example.widget.AzkarWidgetProvider.sendUpdateBroadcast(applicationContext)

            Log.d(TAG, "Background prayer sync completed successfully for city: $cityName ($latitude, $longitude)")
            Result.success()
        } catch (e: CancellationException) {
            Log.d(TAG, "PrayerSyncWorker was cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during background prayer sync execution", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
