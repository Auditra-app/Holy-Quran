package com.example.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.R
import com.example.data.local.QuranDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AzkarNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isMorning = intent.getBooleanExtra("is_morning", true)
        Log.d("AzkarNotificationReceiver", "Received Azkar notification alarm. isMorning=$isMorning")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val settings = db.quranDao().getUserSettingsOnce()

                if (settings?.enableAzkarNotification == false) {
                    Log.d("AzkarNotificationReceiver", "Azkar notifications disabled in settings. Skipping.")
                    return@launch
                }

                if (isMorning && settings?.enableMorningAzkar == false) {
                    Log.d("AzkarNotificationReceiver", "Morning Azkar notifications disabled. Skipping.")
                    return@launch
                }

                if (!isMorning && settings?.enableEveningAzkar == false) {
                    Log.d("AzkarNotificationReceiver", "Evening Azkar notifications disabled. Skipping.")
                    return@launch
                }

                val isArabic = settings?.languageCode == "ar"

                NotificationHelper.createNotificationChannels(context)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val builder = NotificationHelper.buildAzkarNotification(
                    context = context,
                    isMorning = isMorning,
                    isArabic = isArabic
                )

                val notificationId = if (isMorning) 9001 else 9002
                notificationManager.notify(notificationId, builder.build())

                // Play Azkar Voice Reminder Aloud (Sheikh Mishary Rashid Alafasy) if device is not silent
                playAzkarVoiceReminder(context.applicationContext, isMorning)

                // Update Azkar widget to match time transition
                com.example.widget.AzkarWidgetProvider.sendUpdateBroadcast(context)

                // Reschedule for next day using custom user settings time
                val hour = if (isMorning) (settings?.morningAzkarHour ?: 6) else (settings?.eveningAzkarHour ?: 17)
                val minute = if (isMorning) (settings?.morningAzkarMinute ?: 30) else (settings?.eveningAzkarMinute ?: 0)

                if (isMorning) {
                    AzkarNotificationScheduler.scheduleMorningAzkar(context, hour, minute)
                } else {
                    AzkarNotificationScheduler.scheduleEveningAzkar(context, hour, minute)
                }
            } catch (e: Exception) {
                Log.e("AzkarNotificationReceiver", "Error triggering Azkar notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun playAzkarVoiceReminder(context: Context, isMorning: Boolean) {
        try {
            if (AzanAudioPlayer.isDeviceSilentOrMuted(context)) {
                Log.d("AzkarNotificationReceiver", "Device is silent or muted. Skipping Azkar voice playback.")
                return
            }

            val audioUrl = if (isMorning) {
                "https://archive.org/download/sheikh-mishary-rashid-alafasy-azkar/Sheikh%20Mishary%20Rashid%20Alafasy%20-%20%D8%A3%D8%B0%D9%83%D8%A7%D8%B1%20%D8%A7%D9%84%D8%B5%D8%A8%D8%A7%D8%AD.mp3"
            } else {
                "https://archive.org/download/sheikh-mishary-rashid-alafasy-azkar/Sheikh%20Mishary%20Rashid%20Alafasy%20-%20%D8%A3%D8%B0%D9%83%D8%A7%D8%B1%20%D8%A7%D9%84%D9%85%D8%B3%D8%A7%D8%A1.mp3"
            }

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val mediaVol = try { audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0 } catch (e: Exception) { 0 }
            val notifVol = try { audioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 0 } catch (e: Exception) { 0 }

            val (usageType, streamType) = if (mediaVol > 0 || notifVol == 0) {
                Pair(AudioAttributes.USAGE_MEDIA, AudioManager.STREAM_MUSIC)
            } else {
                Pair(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, AudioManager.STREAM_NOTIFICATION)
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(usageType)
                .build()

            var focusRequest: AudioFocusRequest? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .build()
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    streamType,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }

            fun playLocalFallback() {
                try {
                    MediaPlayer.create(context, R.raw.takbeer)?.apply {
                        setAudioAttributes(audioAttributes)
                        setVolume(1.0f, 1.0f)
                        setOnCompletionListener {
                            it.release()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                                audioManager?.abandonAudioFocusRequest(focusRequest)
                            }
                        }
                        setOnErrorListener { mp, _, _ ->
                            mp.release()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                                audioManager?.abandonAudioFocusRequest(focusRequest)
                            }
                            true
                        }
                        start()
                    }
                } catch (ex: Exception) {
                    Log.w("AzkarNotificationReceiver", "Local takbeer fallback failed", ex)
                }
            }

            MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setVolume(1.0f, 1.0f)
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(audioUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnCompletionListener { mp ->
                    mp.release()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                        audioManager?.abandonAudioFocusRequest(focusRequest)
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    Log.w("AzkarNotificationReceiver", "Remote azkar audio failed ($what, $extra), playing local fallback")
                    mp.release()
                    playLocalFallback()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AzkarNotificationReceiver", "Failed initializing Azkar voice reminder audio", e)
            try {
                MediaPlayer.create(context, R.raw.takbeer)?.apply {
                    setOnCompletionListener { it.release() }
                    start()
                }
            } catch (ex: Exception) {}
        }
    }
}
