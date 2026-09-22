package com.example.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import com.example.R
import com.example.data.local.QuranDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerNameAr = intent.getStringExtra(EXTRA_PRAYER_NAME_AR) ?: "الصلاة"
        val prayerNameEn = intent.getStringExtra(EXTRA_PRAYER_NAME_EN) ?: "Prayer"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""

        Log.d("PrayerNotifReceiver", "onReceive triggered for prayer: $prayerNameAr ($prayerNameEn) at $prayerTime")

        val isTest = prayerNameAr.contains("اختبار") || prayerNameEn.contains("Test", ignoreCase = true)

        // Match prayer key accurately from either Arabic or English names
        val prayerKey = PrayerNotificationScheduler.canonicalPrayerKey(prayerNameEn, prayerNameAr)

        // Strict Deduplication Guard: Ignore duplicate alarms for the same prayer on the same day
        if (!isTest) {
            val prefs = context.getSharedPreferences("prayer_dedup_prefs", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val dedupKey = "triggered_${todayDateStr}_${prayerKey}"
            val lastTriggeredForPrayer = prefs.getLong(dedupKey, 0L)
            val diffMs = now - lastTriggeredForPrayer

            if (lastTriggeredForPrayer > 0L && diffMs < 18 * 60 * 60 * 1000L) {
                Log.w("PrayerNotifReceiver", "Duplicate prayer notification/Azan for '$prayerKey' on $todayDateStr suppressed (already triggered ${diffMs / 60000}m ago).")
                return
            }

            prefs.edit()
                .putLong(dedupKey, now)
                .putString("last_prayer_key", prayerKey)
                .putLong("last_prayer_time", now)
                .apply()
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HolyQuran:PrayerAdhanWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(120 * 1000L) // 2-minute wake lock
        }

        // Turn on screen from lock screen
        @Suppress("DEPRECATION")
        val screenWakeLock = try {
            powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "HolyQuran:PrayerScreenTurnOnWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(25 * 1000L)
            }
        } catch (e: Exception) {
            Log.w("PrayerNotifReceiver", "Could not acquire screen wake lock", e)
            null
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val settings = db.quranDao().getUserSettingsOnce()

                val enabledPrayers = settings?.enabledPrayersJson?.split(",")?.map { it.trim().lowercase() }
                    ?.filter { it.isNotEmpty() }
                    ?: listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")

                val silentPrayers = settings?.silentPrayersJson?.split(",")?.map { it.trim().lowercase() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()

                val isEnabled = isTest || enabledPrayers.isEmpty() || enabledPrayers.any { prayerKey.contains(it) || it.contains(prayerKey) }

                if (!isEnabled) {
                    Log.d("PrayerNotifReceiver", "Notification disabled for prayer: $prayerNameEn")
                    return@launch
                }

                val isGlobalSilent = settings?.selectedAzanSound == "silent_tone" || settings?.selectedAzanSound == "silent"
                val isSilent = if (isTest) isGlobalSilent else (isGlobalSilent || prayerKey == "sunrise" || silentPrayers.any { prayerKey.contains(it) || it.contains(prayerKey) })

                val selectedAzanSound = settings?.selectedAzanSound ?: "alafasy"
                val takbeerAzanOnly = settings?.takbeerAzanOnly ?: false

                // 1. Ensure notification channels are created
                NotificationHelper.createNotificationChannels(context)

                val notifId = when {
                    isTest -> 6099
                    prayerKey == "fajr" -> 6001
                    prayerKey == "sunrise" -> 6002
                    prayerKey == "dhuhr" -> 6003
                    prayerKey == "asr" -> 6004
                    prayerKey == "maghrib" -> 6005
                    prayerKey == "isha" -> 6006
                    else -> 6099
                }

                // 2. If audible Azan is enabled, check device silent/vibrate state first
                val isDeviceSilent = AzanAudioPlayer.isDeviceSilentOrMuted(context)

                if (!isSilent && prayerKey != "sunrise" && !isDeviceSilent) {
                    Log.d("PrayerNotifReceiver", "Playing audible Azan for $prayerNameAr (sound=$selectedAzanSound, takbeerOnly=$takbeerAzanOnly)")
                    
                    // Start AzanPlaybackService for audio playback and foreground controls (SINGLE playback engine)
                    try {
                        AzanPlaybackService.start(
                            context = context,
                            prayerNameAr = prayerNameAr,
                            prayerTime = prayerTime,
                            selectedAzanSound = selectedAzanSound,
                            takbeerAzanOnly = takbeerAzanOnly,
                            notificationId = notifId
                        )
                        Log.d("PrayerNotifReceiver", "Started AzanPlaybackService successfully for $prayerNameAr")
                    } catch (e: Exception) {
                        Log.w("PrayerNotifReceiver", "AzanPlaybackService start failed, falling back to AzanAudioPlayer: ${e.message}")
                        // Fallback ONLY if Foreground Service failed
                        AzanAudioPlayer.playAzan(
                            context = context,
                            soundCode = selectedAzanSound,
                            prayerName = prayerNameAr,
                            isTakbeerOnly = takbeerAzanOnly
                        )
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val builder = NotificationHelper.buildPrayerNotification(
                            context = context,
                            prayerNameAr = prayerNameAr,
                            prayerTimeFormatted = prayerTime,
                            isSilent = false,
                            selectedAzanSound = selectedAzanSound
                        )
                        notificationManager.notify(notifId, builder.build())
                    }
                } else {
                    // 3. For silent mode, Sunrise, or when device is on Silent/Vibrate/DND, display standard visual notification only without audio
                    Log.d("PrayerNotifReceiver", "Posting visual notification only (isSilent=$isSilent, isDeviceSilent=$isDeviceSilent) for $prayerNameAr")
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val builder = NotificationHelper.buildPrayerNotification(
                        context = context,
                        prayerNameAr = prayerNameAr,
                        prayerTimeFormatted = prayerTime,
                        isSilent = true,
                        selectedAzanSound = selectedAzanSound
                    )
                    notificationManager.notify(notifId, builder.build())
                    Log.d("PrayerNotifReceiver", "Posted visual prayer notification [ID: $notifId] for $prayerNameAr successfully")
                }
            } catch (e: Exception) {
                Log.e("PrayerNotifReceiver", "Error in prayer notification receiver", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (e: Exception) {}
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PRAYER_NAME_AR = "extra_prayer_name_ar"
        const val EXTRA_PRAYER_NAME_EN = "extra_prayer_name_en"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
    }
}
