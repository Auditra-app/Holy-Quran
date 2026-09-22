package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.QuranDatabase
import com.example.data.provider.QuranDataProvider
import com.example.util.LocationHelper
import com.example.widget.PrayerTimesWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoLocationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AutoLocationReceiver triggered: fetching updated location (5 times per day schedule)")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (LocationHelper.hasLocationPermission(context)) {
                    LocationHelper.getCurrentLocation(context) { lat, lng, cityName ->
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                LocationHelper.saveLocationAndSettings(context, lat, lng, cityName)

                                val db = QuranDatabase.getDatabase(context)
                                val settings = db.quranDao().getUserSettingsOnce()
                                if (settings != null) {
                                    val prayerTimes = QuranDataProvider.getPrayerTimes(
                                        lat = lat,
                                        lng = lng,
                                        methodCode = settings.calculationMethod,
                                        madhhab = settings.madhhab,
                                        isSummerTime = settings.isSummerTime,
                                        fajrOffset = settings.fajrOffsetMins,
                                        dhuhrOffset = settings.dhuhrOffsetMins,
                                        asrOffset = settings.asrOffsetMins,
                                        maghribOffset = settings.maghribOffsetMins,
                                        ishaOffset = settings.ishaOffsetMins
                                    )

                                    PrayerNotificationScheduler.schedulePrayerNotifications(context, prayerTimes)
                                    PrayerTimesWidgetProvider.sendUpdateBroadcast(context)
                                    com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(context)
                                    Log.d(TAG, "Auto location check completed: $cityName ($lat, $lng)")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating DB in AutoLocationReceiver", e)
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Location permission missing for auto-check")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in AutoLocationReceiver", e)
            } finally {
                // Reschedule next check (5 times per day schedule)
                AutoLocationScheduler.scheduleAutoLocationCheck(context)
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AutoLocationReceiver"
    }
}
