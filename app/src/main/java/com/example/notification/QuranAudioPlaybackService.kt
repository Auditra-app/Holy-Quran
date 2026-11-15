package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuranAudioPlaybackService : Service() {

    companion object {
        private const val TAG = "QuranAudioPlayback"
        private const val NOTIFICATION_ID = 9101
        private const val CHANNEL_ID = "quran_audio_playback_channel"

        const val ACTION_START = "com.example.notification.ACTION_START_QURAN_PLAYBACK"
        const val ACTION_PAUSE = "com.example.notification.ACTION_PAUSE_QURAN_PLAYBACK"
        const val ACTION_RESUME = "com.example.notification.ACTION_RESUME_QURAN_PLAYBACK"
        const val ACTION_TOGGLE = "com.example.notification.ACTION_TOGGLE_QURAN_PLAYBACK"
        const val ACTION_STOP = "com.example.notification.ACTION_STOP_QURAN_PLAYBACK"
        const val ACTION_NEXT = "com.example.notification.ACTION_NEXT_QURAN_PLAYBACK"
        const val ACTION_PREV = "com.example.notification.ACTION_PREV_QURAN_PLAYBACK"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        // Callbacks for service actions
        var onTogglePlayPause: (() -> Unit)? = null
        var onNextTrack: (() -> Unit)? = null
        var onPrevTrack: (() -> Unit)? = null
        var onStopPlayback: (() -> Unit)? = null

        fun start(
            context: Context,
            title: String,
            subtitle: String,
            isPlaying: Boolean
        ) {
            try {
                val intent = Intent(context, QuranAudioPlaybackService::class.java).apply {
                    action = if (isPlaying) ACTION_RESUME else ACTION_PAUSE
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_SUBTITLE, subtitle)
                    putExtra(EXTRA_IS_PLAYING, isPlaying)
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting QuranAudioPlaybackService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, QuranAudioPlaybackService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping QuranAudioPlaybackService", e)
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var currentTitle: String = "تلاوة القرآن الكريم"
    private var currentSubtitle: String = "الاستماع للقرآن"
    private var isPlayingState: Boolean = false

    override fun onCreate() {
        super.onCreate()
        _isServiceRunning.value = true
        createNotificationChannel()
        acquireWakeLock()
        updateNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: run {
            updateNotification()
            return START_STICKY
        }

        when (action) {
            ACTION_STOP -> {
                stopForegroundPlayback()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> {
                onTogglePlayPause?.invoke()
                updateNotification()
            }
            ACTION_NEXT -> {
                onNextTrack?.invoke()
                updateNotification()
            }
            ACTION_PREV -> {
                onPrevTrack?.invoke()
                updateNotification()
            }
            ACTION_START, ACTION_RESUME, ACTION_PAUSE -> {
                currentTitle = intent.getStringExtra(EXTRA_TITLE) ?: currentTitle
                currentSubtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: currentSubtitle
                isPlayingState = intent.getBooleanExtra(EXTRA_IS_PLAYING, isPlayingState)
                updateNotification()
            }
            else -> {
                updateNotification()
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "HolyQuran::AudioPlaybackWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(3 * 60 * 60 * 1000L) // 3 hours max safeguard
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire wakelock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing wakelock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تلاوة القرآن الكريم"
            val descriptionText = "إشعارات التحكم في الاستماع للقرآن الكريم في الخلفية"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "quran_audio")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            8001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action
        val prevIntent = Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(this, 8002, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Toggle Play/Pause Action
        val toggleIntent = Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_TOGGLE }
        val togglePendingIntent = PendingIntent.getService(this, 8003, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Next Action
        val nextIntent = Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 8004, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Stop Action
        val stopIntent = Intent(this, QuranAudioPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 8005, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quran)
            .setContentTitle(currentTitle)
            .setContentText(currentSubtitle)
            .setSubText("القرآن الكريم")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlayingState)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_previous, "السابق", prevPendingIntent)
            .addAction(
                if (isPlayingState) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlayingState) "إيقاف مؤقت" else "تشغيل",
                togglePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "التالي", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stopPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground", e)
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (inner: Exception) {
                Log.e(TAG, "Failed to notify fallback", inner)
            }
        }
    }

    private fun stopForegroundPlayback() {
        onStopPlayback?.invoke()
        _isServiceRunning.value = false
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
