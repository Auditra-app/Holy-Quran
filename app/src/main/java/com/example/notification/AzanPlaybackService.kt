package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AzanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var mediaSession: MediaSession? = null
    private var hardwareControlReceiver: BroadcastReceiver? = null
    private var volumeObserver: ContentObserver? = null
    private var sensorManager: SensorManager? = null
    private var sensorEventListener: SensorEventListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isTakbeerOnly = false
    private var currentPlaybackVolume = 1.0f
    private var playbackStartTimestamp = 0L

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "AudioFocus transient loss, ducking volume")
                try {
                    val duckVol = currentPlaybackVolume * 0.3f
                    mediaPlayer?.setVolume(duckVol, duckVol)
                } catch (e: Exception) {
                    Log.e(TAG, "Error ducking volume", e)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AudioFocus regained, restoring volume")
                try {
                    mediaPlayer?.setVolume(currentPlaybackVolume, currentPlaybackVolume)
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring volume", e)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AudioFocus permanent loss -> stopping Azan playback")
                stopAzan()
            }
        }
    }

    private val safetyTimeoutRunnable = Runnable {
        Log.w(TAG, "Safety timeout reached (6 minutes), releasing Azan service")
        stopAzan()
    }

    private val takbeerEndRunnable = Runnable {
        Log.d(TAG, "Takbeer-only duration reached (after opening Takbeerat), starting smooth fade-out")
        performSmoothFadeAndStop()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AzanPlaybackService created")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        playbackStartTimestamp = System.currentTimeMillis()
        acquireWakeLock()
        initMediaSession()
        registerHardwareControlReceiver()
        registerVolumeObserver()
        registerFlipToMuteSensors()
    }

    private var initialAlarmVol: Int = -1
    private var initialMusicVol: Int = -1

    private fun initMediaSession() {
        try {
            mediaSession = MediaSession(this, "HolyQuranAzanSession").apply {
                setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSession.FLAG_HANDLES_MEDIA_BUTTONS)
                setPlaybackState(
                    PlaybackState.Builder()
                        .setActions(
                            PlaybackState.ACTION_STOP or
                            PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_PLAY_PAUSE or
                            PlaybackState.ACTION_SKIP_TO_NEXT or
                            PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        )
                        .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                        .build()
                )

                // Intercept hardware volume button clicks across the entire system (lock screen & background)
                val volumeProvider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 100) {
                    override fun onAdjustVolume(direction: Int) {
                        val elapsed = System.currentTimeMillis() - playbackStartTimestamp
                        if (direction != 0 && elapsed > 2500L && _isAzanPlaying.value) {
                            Log.d(TAG, "Hardware volume rocker pressed via VolumeProvider (direction=$direction) -> Muting Azan")
                            stopAzan()
                        }
                    }
                    override fun onSetVolumeTo(volume: Int) {
                        val elapsed = System.currentTimeMillis() - playbackStartTimestamp
                        // Ignore default OS volume initialization (100)
                        if (volume != 100 && elapsed > 2500L && _isAzanPlaying.value) {
                            Log.d(TAG, "Hardware volume set via VolumeProvider ($volume) -> Muting Azan")
                            stopAzan()
                        }
                    }
                }
                setPlaybackToRemote(volumeProvider)

                setCallback(object : MediaSession.Callback() {
                    override fun onStop() {
                        val elapsed = System.currentTimeMillis() - playbackStartTimestamp
                        if (elapsed > 1000L) {
                            Log.d(TAG, "MediaSession onStop received -> Muting Azan")
                            stopAzan()
                        }
                    }
                    override fun onPause() {
                        val elapsed = System.currentTimeMillis() - playbackStartTimestamp
                        if (elapsed > 1000L) {
                            Log.d(TAG, "MediaSession onPause received -> Muting Azan")
                            stopAzan()
                        }
                    }
                    override fun onSkipToNext() {
                        val elapsed = System.currentTimeMillis() - playbackStartTimestamp
                        if (elapsed > 1000L) {
                            Log.d(TAG, "MediaSession onSkipToNext received -> Muting Azan")
                            stopAzan()
                        }
                    }
                    override fun onSkipToPrevious() {
                        val elapsed = System.currentTimeMillis() - playbackStartTimestamp
                        if (elapsed > 1000L) {
                            Log.d(TAG, "MediaSession onSkipToPrevious received -> Muting Azan")
                            stopAzan()
                        }
                    }
                    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                        val elapsed = System.currentTimeMillis() - playbackStartTimestamp
                        if (elapsed > 1000L) {
                            Log.d(TAG, "MediaSession onMediaButtonEvent received -> Muting Azan")
                            stopAzan()
                            return true
                        }
                        return false
                    }
                })
                isActive = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaSession", e)
        }
    }

    private var activeNotifId = NOTIFICATION_ID

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand with action: $action")

        if (action == ACTION_STOP_AZAN || action == ACTION_MUTE_AZAN) {
            stopAzan(removeFromTray = false)
            return START_NOT_STICKY
        }

        playbackStartTimestamp = System.currentTimeMillis()
        acquireWakeLock()

        val prayerNameAr = intent?.getStringExtra(EXTRA_PRAYER_NAME_AR) ?: "الصلاة"
        val prayerTime = intent?.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val selectedAzanSound = intent?.getStringExtra(EXTRA_SELECTED_AZAN_SOUND) ?: "alafasy"
        activeNotifId = intent?.getIntExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID) ?: NOTIFICATION_ID
        isTakbeerOnly = intent?.getBooleanExtra(EXTRA_TAKBEER_AZAN_ONLY, false) ?: false

        _isAzanPlaying.value = true
        _currentPrayerName.value = prayerNameAr
        _currentAzanSound.value = selectedAzanSound

        startForegroundWithNotification(prayerNameAr, prayerTime, selectedAzanSound, isTakbeerOnly, activeNotifId)
        playAzanAudio(selectedAzanSound, isTakbeerOnly)

        // Safety timeout to ensure no battery drain if player fails to complete (6 minutes max)
        handler.removeCallbacks(safetyTimeoutRunnable)
        handler.postDelayed(safetyTimeoutRunnable, 6 * 60 * 1000L)

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "HolyQuran:AzanPlaybackWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(6 * 60 * 1000L) // 6 minutes max
                Log.d(TAG, "WakeLock acquired successfully for Azan playback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        } finally {
            wakeLock = null
        }
    }

    private fun registerHardwareControlReceiver() {
        try {
            hardwareControlReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action ?: return
                    val elapsed = System.currentTimeMillis() - playbackStartTimestamp

                    when (action) {
                        "android.media.VOLUME_CHANGED_ACTION" -> {
                            if (elapsed > 2500L && _isAzanPlaying.value) {
                                Log.d(TAG, "Hardware volume rocker broadcast detected -> Muting Azan")
                                stopAzan()
                            }
                        }
                        AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                            if (elapsed > 2500L && _isAzanPlaying.value) {
                                Log.d(TAG, "Headset unplugged during Azan -> Muting Azan")
                                stopAzan()
                            }
                        }
                        Intent.ACTION_HEADSET_PLUG -> {
                            if (elapsed > 3000L && _isAzanPlaying.value) {
                                val state = intent.getIntExtra("state", -1)
                                if (state == 0) { // Unplugged
                                    Log.d(TAG, "Headset unplugged event during Azan -> Muting Azan")
                                    stopAzan()
                                }
                            }
                        }
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(Intent.ACTION_HEADSET_PLUG)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(hardwareControlReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(hardwareControlReceiver, filter)
            }
            Log.d(TAG, "Registered comprehensive hardware control receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering hardware control receiver", e)
        }
    }

    private fun unregisterHardwareControlReceiver() {
        try {
            hardwareControlReceiver?.let {
                unregisterReceiver(it)
                hardwareControlReceiver = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering hardware control receiver", e)
        }
    }

    private fun registerVolumeObserver() {
        try {
            volumeObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    val elapsed = System.currentTimeMillis() - playbackStartTimestamp
                    if (_isAzanPlaying.value && elapsed > 2500L) {
                        val currentAlarm = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: -1
                        val currentMusic = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: -1
                        if ((initialAlarmVol != -1 && currentAlarm != initialAlarmVol) ||
                            (initialMusicVol != -1 && currentMusic != initialMusicVol)) {
                            Log.d(TAG, "Volume change detected via ContentObserver (alarm: $initialAlarmVol->$currentAlarm, music: $initialMusicVol->$currentMusic) -> Muting Azan")
                            stopAzan()
                        }
                    }
                }
            }
            contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeObserver!!
            )
            Log.d(TAG, "Registered volume ContentObserver")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering volume ContentObserver", e)
        }
    }

    private fun unregisterVolumeObserver() {
        try {
            volumeObserver?.let {
                contentResolver.unregisterContentObserver(it)
                volumeObserver = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering volume observer", e)
        }
    }

    private fun registerFlipToMuteSensors() {
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

            sensorEventListener = object : SensorEventListener {
                private var hasBeenFaceUp = false
                private var faceDownStartTime = 0L
                private var hasBeenUncovered = false
                private var proximityStartTime = 0L

                override fun onSensorChanged(event: SensorEvent?) {
                    val elapsed = System.currentTimeMillis() - playbackStartTimestamp
                    if (!_isAzanPlaying.value || elapsed < 3500L) return
                    if (event == null) return

                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val z = event.values[2]
                        if (z > -2.0f) {
                            // Device is being held or face up
                            hasBeenFaceUp = true
                            faceDownStartTime = 0L
                        } else if (hasBeenFaceUp && z < -7.0f) {
                            // Deliberately flipped face down on a flat surface
                            val now = System.currentTimeMillis()
                            if (faceDownStartTime == 0L) {
                                faceDownStartTime = now
                            } else if (now - faceDownStartTime > 500L) {
                                Log.d(TAG, "Flip-to-mute detected: Device placed face down (z=$z) -> Muting Azan")
                                stopAzan()
                            }
                        } else {
                            faceDownStartTime = 0L
                        }
                    } else if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                        val distance = event.values[0]
                        val maxRange = event.sensor.maximumRange
                        if (distance > 3.0f || distance >= maxRange) {
                            hasBeenUncovered = true
                            proximityStartTime = 0L
                        } else if (hasBeenUncovered && distance <= 1.5f) {
                            val now = System.currentTimeMillis()
                            if (proximityStartTime == 0L) {
                                proximityStartTime = now
                            } else if (now - proximityStartTime > 500L) {
                                Log.d(TAG, "Proximity mute detected: Sensor covered (distance=$distance) -> Muting Azan")
                                stopAzan()
                            }
                        } else {
                            proximityStartTime = 0L
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            accelSensor?.let { sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL) }
            proximitySensor?.let { sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL) }
            Log.d(TAG, "Registered flip-to-mute sensors (Accelerometer & Proximity)")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering flip-to-mute sensors", e)
        }
    }

    private fun unregisterFlipToMuteSensors() {
        try {
            sensorEventListener?.let {
                sensorManager?.unregisterListener(it)
                sensorEventListener = null
            }
            sensorManager = null
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering sensors", e)
        }
    }

    private fun startForegroundWithNotification(
        prayerNameAr: String,
        prayerTime: String,
        selectedAzanSound: String,
        isTakbeerOnly: Boolean,
        notificationId: Int
    ) {
        val channelId = CHANNEL_PRAYER_ACTIVE_PLAYBACK
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تشغيل الأذان المبارك (Active Azan)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعار تشغيل الأذان وإمكانية الإيقاف الفوري"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_PRAYER_TIMES"
            data = Uri.parse("holyquran://service/azan_playback?prayer=${Uri.encode(prayerNameAr)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "prayer_times")
            putExtra("prayer_name_ar", prayerNameAr)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Open Reader / Mushaf
        val readerIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_READER"
            data = Uri.parse("holyquran://notification/reader_from_prayer")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "reader")
        }
        val readerPendingIntent = PendingIntent.getActivity(
            this,
            6092,
            readerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Stop / Mute Azan
        val stopIntent = Intent(this, AzanPlaybackService::class.java).apply {
            action = ACTION_STOP_AZAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2002,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundLabel = when (selectedAzanSound) {
            "alafasy_2" -> "الشيخ مشاري العفاسي (2)"
            "alafasy_3" -> "الشيخ مشاري العفاسي (فجر 1)"
            "alafasy_4" -> "الشيخ مشاري العفاسي (4)"
            "alafasy_5" -> "الشيخ مشاري العفاسي (فجر 2)"
            "qatami" -> "الشيخ ناصر القطامي"
            "makkah" -> "أذان الحرم المكي"
            "madinah" -> "أذان المسجد النبوي"
            "alaqsa" -> "أذان المسجد الأقصى"
            "takbeer" -> "تكبيرات الآذان"
            else -> "الشيخ مشاري العفاسي (1)"
        }

        val modeSuffix = if (isTakbeerOnly && selectedAzanSound != "takbeer") " (التكبيرات فقط)" else ""
        val title = "🕌 حان الآن موعد صلاة $prayerNameAr"
        val timeSuffix = if (prayerTime.isNotBlank()) " ($prayerTime)" else ""
        val body = "الله أكبر • أذان $prayerNameAr يرفع الآن بصوت $soundLabel$modeSuffix"

        val appLogoBitmap = NotificationHelper.getAppLogoBitmap(this)

        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_PRAYER_TIMES"
            data = Uri.parse("holyquran://notification/prayer_fullscreen?prayer=${Uri.encode(prayerNameAr)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("navigate_to", "prayer_times")
            putExtra("prayer_name_ar", prayerNameAr)
            putExtra("from_prayer_alarm", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            notificationId + 500,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_quran)
            .apply {
                if (appLogoBitmap != null) setLargeIcon(appLogoBitmap)
            }
            .setContentTitle(title)
            .setContentText(body)
            .setSubText("مواقيت الصلاة")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                R.drawable.ic_stat_quran,
                "📖 المصحف",
                readerPendingIntent
            )
            .addAction(
                R.drawable.ic_stat_quran,
                "⏹️ كتم الأذان",
                stopPendingIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText("الله أكبر، الله أكبر • حان الآن موعد صلاة $prayerNameAr$timeSuffix$modeSuffix\n\nحي على الصلاة، حي على الفلاح 🤲")
            )
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(notificationId, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error", e)
        }
    }

    private fun playAzanAudio(selectedAzanSound: String, isTakbeerOnly: Boolean, enableAzanOnSilent: Boolean = false) {
        if (selectedAzanSound == "silent" || selectedAzanSound == "silent_tone") {
            Log.d(TAG, "Silent mode selected, skipping audio")
            stopAzan()
            return
        }

        val isSilentMode = AzanAudioPlayer.isDeviceSilentOrMuted(applicationContext)
        if (isSilentMode) {
            Log.d(TAG, "Device is silent/muted/vibrate/DND. Skipping audio playback.")
            stopAzan()
            return
        }

        // Stop any external/standalone AzanAudioPlayer to prevent duplicate audio
        try {
            AzanAudioPlayer.stop()
        } catch (e: Exception) {}

        // Standard playback volume scale relative to system volume
        currentPlaybackVolume = 1.0f

        val soundResId = when (selectedAzanSound) {
            "alafasy_2" -> R.raw.alafasy_2
            "alafasy_3" -> R.raw.alafasy_3
            "alafasy_4" -> R.raw.alafasy_4
            "alafasy_5" -> R.raw.alafasy_5
            "qatami" -> R.raw.qatami
            "makkah" -> R.raw.makkah
            "madinah" -> R.raw.madinah
            "alaqsa" -> R.raw.alaqsa
            "takbeer" -> R.raw.takbeer
            else -> R.raw.alafasy
        }

        try {
            requestAudioFocus()

            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {}
            mediaPlayer = null

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            // 1. Primary initialization with AudioAttributes
            try {
                mediaPlayer = MediaPlayer.create(applicationContext, soundResId, audioAttributes, 0)
            } catch (e: Exception) {
                Log.w(TAG, "MediaPlayer.create with AudioAttributes failed, trying standard create", e)
                mediaPlayer = null
            }

            // 2. Standard create fallback
            if (mediaPlayer == null) {
                try {
                    mediaPlayer = MediaPlayer.create(applicationContext, soundResId)
                } catch (e: Exception) {
                    Log.w(TAG, "MediaPlayer.create standard failed, trying FileDescriptor", e)
                    mediaPlayer = null
                }
            }

            // 3. Fallback File Descriptor
            if (mediaPlayer == null) {
                try {
                    val afd = resources.openRawResourceFd(soundResId)
                    if (afd != null) {
                        mediaPlayer = MediaPlayer().apply {
                            setAudioAttributes(audioAttributes)
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            prepare()
                        }
                        afd.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "FileDescriptor initialization failed", e)
                    mediaPlayer = null
                }
            }

            // 4. Fallback Uri
            if (mediaPlayer == null) {
                try {
                    val soundUri = Uri.parse("android.resource://$packageName/$soundResId")
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(audioAttributes)
                        setDataSource(applicationContext, soundUri)
                        prepare()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Fallback Uri failed", e)
                    mediaPlayer = null
                }
            }

            mediaPlayer?.apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setVolume(currentPlaybackVolume, currentPlaybackVolume)
                isLooping = false
                setOnCompletionListener {
                    Log.d(TAG, "Azan playback completed naturally")
                    stopAzan()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopAzan()
                    true
                }
                start()
                Log.d(TAG, "Azan audio playback started successfully with volume $currentPlaybackVolume (silentMode=$isSilentMode, duration: ${duration}ms, takbeerOnly: $isTakbeerOnly)")
            } ?: run {
                Log.e(TAG, "Unable to initialize MediaPlayer for Azan")
                stopAzan()
            }

            // If Takbeer-only mode is selected (or Takbeer sound selected), schedule stop strictly after opening Takbeerat (الله أكبر الله أكبر ، الله أكبر الله أكبر ~16s)
            handler.removeCallbacks(takbeerEndRunnable)
            if (isTakbeerOnly || selectedAzanSound == "takbeer") {
                val takbeerDelayMs = if (selectedAzanSound == "takbeer") 17 * 1000L else 16 * 1000L
                handler.postDelayed(takbeerEndRunnable, takbeerDelayMs)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during playAzanAudio", e)
            stopAzan()
        }
    }

    private fun performSmoothFadeAndStop() {
        val steps = 10
        val stepDelay = 250L // Total 2.5 seconds fade-out
        var currentStep = 0
        val baseVol = currentPlaybackVolume

        val fadeRunnable = object : Runnable {
            override fun run() {
                currentStep++
                val volume = (baseVol * (1.0f - (currentStep.toFloat() / steps))).coerceAtLeast(0.0f)
                try {
                    mediaPlayer?.setVolume(volume, volume)
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting fade volume", e)
                }

                if (currentStep < steps && _isAzanPlaying.value) {
                    handler.postDelayed(this, stepDelay)
                } else {
                    stopAzan()
                }
            }
        }
        handler.post(fadeRunnable)
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                audioManager?.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager?.abandonAudioFocusRequest(it)
                    audioFocusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus", e)
        }
    }

    private fun stopAzan(removeFromTray: Boolean = false) {
        Log.d(TAG, "Stopping Azan playback and terminating service (removeFromTray=$removeFromTray)")
        handler.removeCallbacks(safetyTimeoutRunnable)
        handler.removeCallbacks(takbeerEndRunnable)

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media player", e)
        } finally {
            mediaPlayer = null
        }

        try {
            mediaSession?.let {
                it.isActive = false
                it.release()
                mediaSession = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaSession", e)
        }

        abandonAudioFocus()
        releaseWakeLock()
        unregisterHardwareControlReceiver()
        unregisterVolumeObserver()
        unregisterFlipToMuteSensors()

        _isAzanPlaying.value = false
        _currentPrayerName.value = null
        _currentAzanSound.value = null

        try {
            if (removeFromTray) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        } catch (e: Exception) {
            try {
                @Suppress("DEPRECATION")
                stopForeground(removeFromTray)
            } catch (ex: Exception) {}
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AzanPlaybackService onDestroy")
        stopAzan(removeFromTray = false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AzanPlaybackService"
        const val NOTIFICATION_ID = 6999
        const val CHANNEL_PRAYER_ACTIVE_PLAYBACK = "AZAN_ACTIVE_PLAYBACK_CHANNEL_V3"

        const val ACTION_PLAY_AZAN = "com.example.action.PLAY_AZAN"
        const val ACTION_STOP_AZAN = "com.example.action.STOP_AZAN"
        const val ACTION_MUTE_AZAN = "com.example.action.MUTE_AZAN"

        const val EXTRA_PRAYER_NAME_AR = "extra_prayer_name_ar"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_SELECTED_AZAN_SOUND = "extra_selected_azan_sound"
        const val EXTRA_TAKBEER_AZAN_ONLY = "extra_takbeer_azan_only"
        const val EXTRA_ENABLE_AZAN_ON_SILENT = "extra_enable_azan_on_silent"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        private val _isAzanPlaying = MutableStateFlow(false)
        val isAzanPlaying: StateFlow<Boolean> = _isAzanPlaying.asStateFlow()

        private val _currentPrayerName = MutableStateFlow<String?>(null)
        val currentPrayerName: StateFlow<String?> = _currentPrayerName.asStateFlow()

        private val _currentAzanSound = MutableStateFlow<String?>(null)
        val currentAzanSound: StateFlow<String?> = _currentAzanSound.asStateFlow()

        private var lastStartedPrayer: String? = null
        private var lastStartedTime: Long = 0L

        fun start(
            context: Context,
            prayerNameAr: String,
            prayerTime: String,
            selectedAzanSound: String,
            takbeerAzanOnly: Boolean = false,
            notificationId: Int = 6001
        ) {
            val now = System.currentTimeMillis()
            val isTest = prayerNameAr.contains("اختبار")
            val prayerKey = PrayerNotificationScheduler.canonicalPrayerKey(prayerNameAr, prayerNameAr)
            val lastKey = lastStartedPrayer?.let { PrayerNotificationScheduler.canonicalPrayerKey(it, it) } ?: ""

            if (!isTest && (prayerKey == lastKey) && (now - lastStartedTime < 10 * 1000L)) {
                Log.w(TAG, "Azan playback already started recently (${(now - lastStartedTime) / 1000}s ago) for '$prayerNameAr'.")
                return
            }

            lastStartedPrayer = prayerNameAr
            lastStartedTime = now

            val intent = Intent(context, AzanPlaybackService::class.java).apply {
                action = ACTION_PLAY_AZAN
                putExtra(EXTRA_PRAYER_NAME_AR, prayerNameAr)
                putExtra(EXTRA_PRAYER_TIME, prayerTime)
                putExtra(EXTRA_SELECTED_AZAN_SOUND, selectedAzanSound)
                putExtra(EXTRA_TAKBEER_AZAN_ONLY, takbeerAzanOnly)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service, attempting direct startService", e)
                try {
                    context.startService(intent)
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to start service directly", ex)
                }
            }
        }

        fun stop(context: Context) {
            try {
                AzanAudioPlayer.stop()
            } catch (e: Exception) {}

            val intent = Intent(context, AzanPlaybackService::class.java).apply {
                action = ACTION_STOP_AZAN
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop AzanPlaybackService", e)
            }
            _isAzanPlaying.value = false
            _currentPrayerName.value = null
            _currentAzanSound.value = null
        }
    }
}
