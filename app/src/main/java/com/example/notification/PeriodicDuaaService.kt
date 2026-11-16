package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Dedicated Foreground Media Service for playing authentic periodic supplications and Azkar.
 * Runs in the foreground with mediaPlayback type to prevent Android OS from prematurely
 * terminating audio playback during screen-off, Doze mode, or background process freezes.
 * Guarantees that every Duaa is played completely from beginning to end without any
 * artificial seconds limitation.
 */
class PeriodicDuaaService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())

    // Safety watchdog: generous 10 minutes timeout in case of indefinite network stalls,
    // protecting battery while never interrupting any authentic supplication.
    private val safetyWatchdogRunnable = Runnable {
        Log.w(TAG, "Safety watchdog reached (10 minutes). Stopping service to protect battery.")
        stopDuaaPlayback()
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                try {
                    mediaPlayer?.setVolume(0.3f, 0.3f)
                } catch (e: Exception) {}
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                try {
                    mediaPlayer?.setVolume(1.0f, 1.0f)
                } catch (e: Exception) {}
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                try {
                    mediaPlayer?.setVolume(0.5f, 0.5f)
                } catch (e: Exception) {}
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_PLAY_PERIODIC_DUAA
        Log.d(TAG, "PeriodicDuaaService onStartCommand with action: $action")

        if (action == ACTION_STOP_PERIODIC_DUAA) {
            stopDuaaPlayback()
            return START_NOT_STICKY
        }

        val isTest = intent?.getBooleanExtra(EXTRA_IS_TEST, false) ?: false
        startDuaaFlow(isTest)

        return START_NOT_STICKY
    }

    private fun startDuaaFlow(isTest: Boolean) {
        if (!isTest && PeriodicDuaaManager.isQuietHoursNow(this)) {
            Log.d(TAG, "Quiet hours active. Skipping voice playback.")
            stopSelf()
            return
        }

        if (AzanAudioPlayer.isDeviceSilentOrMuted(this)) {
            Log.d(TAG, "Device is silent or muted. Skipping voice playback.")
            stopSelf()
            return
        }

        val voiceSource = PeriodicDuaaManager.getVoiceSource(this)
        val prefs = getSharedPreferences("periodic_duaa_prefs", Context.MODE_PRIVATE)

        val lastIdx = prefs.getInt("last_authentic_duaa_idx", -1)
        val nextIdx = (lastIdx + 1) % PeriodicDuaaManager.AUTHENTIC_DUAA_LIST.size
        prefs.edit().putInt("last_authentic_duaa_idx", nextIdx).apply()

        val item = PeriodicDuaaManager.AUTHENTIC_DUAA_LIST[nextIdx]
        val voiceDisplayName = PeriodicDuaaManager.getVoiceDisplayName(voiceSource)
        val audioUrl = PeriodicDuaaManager.getAudioUrlForItem(item, voiceSource)

        // Show foreground notification immediately (Android 5-second requirement)
        createNotificationChannel()
        val notification = buildDuaaNotification(item.text, voiceDisplayName)
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service notification", e)
        }

        playAudioCompletely(audioUrl)
    }

    private fun playAudioCompletely(audioUrl: String) {
        val mediaVol = try { audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0 } catch (e: Exception) { 0 }
        val notifVol = try { audioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 0 } catch (e: Exception) { 0 }

        val (usageType, streamType) = if (mediaVol > 0 || notifVol == 0) {
            Pair(AudioAttributes.USAGE_MEDIA, AudioManager.STREAM_MUSIC)
        } else {
            Pair(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, AudioManager.STREAM_NOTIFICATION)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(usageType)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        requestAudioFocus(audioAttributes, streamType)

        // Set safety watchdog for 10 minutes
        handler.removeCallbacks(safetyWatchdogRunnable)
        handler.postDelayed(safetyWatchdogRunnable, 10 * 60 * 1000L)

        fun playLocalFallback() {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(applicationContext, R.raw.takbeer)?.apply {
                    setAudioAttributes(audioAttributes)
                    setVolume(1.0f, 1.0f)
                    setOnCompletionListener {
                        Log.d(TAG, "Fallback audio completed.")
                        stopDuaaPlayback()
                    }
                    setOnErrorListener { _, _, _ ->
                        stopDuaaPlayback()
                        true
                    }
                    start()
                }
                if (mediaPlayer == null) {
                    stopDuaaPlayback()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback audio failed", e)
                stopDuaaPlayback()
            }
        }

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setVolume(1.0f, 1.0f)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(audioUrl)
                setOnPreparedListener { mp ->
                    Log.d(TAG, "MediaPlayer prepared. Playing full authentic duaa...")
                    mp.start()
                }
                setOnCompletionListener {
                    Log.d(TAG, "Duaa playback completed completely and naturally.")
                    stopDuaaPlayback()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Remote duaa stream failed (what=$what, extra=$extra), playing fallback")
                    playLocalFallback()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating MediaPlayer for duaa $audioUrl", e)
            playLocalFallback()
        }
    }

    private fun requestAudioFocus(audioAttributes: AudioAttributes, streamType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                audioFocusChangeListener,
                streamType,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {}
        audioFocusRequest = null
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "HolyQuran::PeriodicDuaaServiceWakeLock"
                )
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(10 * 60 * 1000L) // 10 min safe ceiling
                Log.d(TAG, "Acquired WakeLock for full duaa playback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Released WakeLock after full duaa playback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        }
        wakeLock = null
    }

    private fun stopDuaaPlayback() {
        handler.removeCallbacks(safetyWatchdogRunnable)
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null

        abandonAudioFocus()
        releaseWakeLock()

        // Detach foreground so notification remains visible for reading without holding service
        try {
            stopForeground(STOP_FOREGROUND_DETACH)
        } catch (e: Exception) {
            try {
                @Suppress("DEPRECATION")
                stopForeground(false)
            } catch (ex: Exception) {}
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "التذكير بالأدعية والأذكار",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تذكير صوتي دوري بالأدعية النبوية والقرآنية بأصوات شيوخ معروفين"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildDuaaNotification(duaaText: String, reciterSubtext: String): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "azkar")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            9503,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PeriodicDuaaService::class.java).apply {
            action = ACTION_STOP_PERIODIC_DUAA
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            9504,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quran)
            .setContentTitle("🌸 تذكير بالأدعية والأذكار")
            .setContentText(duaaText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(duaaText))
            .setSubText(reciterSubtext)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف الصوت", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDuaaPlayback()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PeriodicDuaaService"
        const val CHANNEL_ID = "periodic_duaa_channel"
        const val NOTIFICATION_ID = 9501

        const val ACTION_PLAY_PERIODIC_DUAA = "com.example.notification.ACTION_PLAY_PERIODIC_DUAA"
        const val ACTION_STOP_PERIODIC_DUAA = "com.example.notification.ACTION_STOP_PERIODIC_DUAA"
        const val EXTRA_IS_TEST = "extra_is_test"

        fun start(context: Context, isTest: Boolean = false) {
            val intent = Intent(context, PeriodicDuaaService::class.java).apply {
                action = ACTION_PLAY_PERIODIC_DUAA
                putExtra(EXTRA_IS_TEST, isTest)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service, attempting normal startService", e)
                try {
                    context.startService(intent)
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to start service directly", ex)
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PeriodicDuaaService::class.java).apply {
                action = ACTION_STOP_PERIODIC_DUAA
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {}
        }
    }
}
