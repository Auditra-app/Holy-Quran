package com.example.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AzanAudioPlayer {

    private const val TAG = "AzanAudioPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPrayerName = MutableStateFlow<String?>(null)
    val currentPrayerName: StateFlow<String?> = _currentPrayerName.asStateFlow()

    private val _currentSoundCode = MutableStateFlow<String?>(null)
    val currentSoundCode: StateFlow<String?> = _currentSoundCode.asStateFlow()

    private var currentPlaybackVolume = 1.0f

    fun isDeviceSilentOrMuted(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false

        // 1. Check Ringer Mode: SILENT (0) or VIBRATE (1)
        val ringerMode = am.ringerMode
        if (ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            Log.d(TAG, "Device ringer mode is not NORMAL (mode=$ringerMode). Device is silent/vibrate.")
            return true
        }

        // 2. Check stream volumes: if notification, ring, or media volume is 0
        val ringVol = try { am.getStreamVolume(AudioManager.STREAM_RING) } catch (e: Exception) { 0 }
        val notifVol = try { am.getStreamVolume(AudioManager.STREAM_NOTIFICATION) } catch (e: Exception) { 0 }
        val musicVol = try { am.getStreamVolume(AudioManager.STREAM_MUSIC) } catch (e: Exception) { 0 }

        if (ringVol == 0 || notifVol == 0 || musicVol == 0) {
            Log.d(TAG, "Stream volume is 0 (ring=$ringVol, notif=$notifVol, music=$musicVol). Device is silent/muted.")
            return true
        }

        // 3. Check Stream Mute state (API 23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (am.isStreamMute(AudioManager.STREAM_NOTIFICATION) ||
                    am.isStreamMute(AudioManager.STREAM_RING) ||
                    am.isStreamMute(AudioManager.STREAM_MUSIC)) {
                    Log.d(TAG, "Stream is explicitly muted in AudioManager.")
                    return true
                }
            } catch (e: Exception) {}

            // 4. Check Do Not Disturb (DND) Filter
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                val filter = nm?.currentInterruptionFilter ?: android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                if (filter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL &&
                    filter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                    Log.d(TAG, "Do Not Disturb (DND) filter active: $filter")
                    return true
                }
            } catch (e: Exception) {}
        }

        return false
    }

    private val safetyTimeoutRunnable = Runnable {
        Log.w(TAG, "Safety timeout reached (5 minutes), stopping Azan audio")
        stop()
    }

    private val takbeerEndRunnable = Runnable {
        Log.d(TAG, "Takbeer-only duration reached (after opening Takbeerat), starting smooth fade-out")
        performSmoothFadeAndStop()
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                try {
                    val duckVol = currentPlaybackVolume * 0.3f
                    mediaPlayer?.setVolume(duckVol, duckVol)
                } catch (e: Exception) {}
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                try {
                    mediaPlayer?.setVolume(currentPlaybackVolume, currentPlaybackVolume)
                } catch (e: Exception) {}
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                try {
                    val duckVol = currentPlaybackVolume * 0.5f
                    mediaPlayer?.setVolume(duckVol, duckVol)
                } catch (e: Exception) {}
            }
        }
    }

    fun getSoundResId(soundCode: String): Int {
        return when (soundCode) {
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
    }

    @Synchronized
    fun playAzan(
        context: Context,
        soundCode: String,
        prayerName: String = "الصلاة",
        isTakbeerOnly: Boolean = false,
        enableAzanOnSilent: Boolean = false
    ) {
        val appContext = context.applicationContext
        Log.d(TAG, "playAzan called: soundCode=$soundCode, prayer=$prayerName, isTakbeerOnly=$isTakbeerOnly")

        if (soundCode == "silent" || soundCode == "silent_tone") {
            Log.d(TAG, "Silent sound selected, skipping audio playback")
            return
        }

        val isSilentMode = isDeviceSilentOrMuted(appContext)
        if (isSilentMode) {
            Log.d(TAG, "Device is in Silent/Vibrate mode or muted. Skipping Azan audio playback.")
            return
        }

        // Standard playback volume scale relative to system volume
        currentPlaybackVolume = 1.0f

        // Stop any current playback cleanly
        stopInternal()

        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        acquireWakeLock(appContext)
        requestAudioFocus()

        val soundResId = getSoundResId(soundCode)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        var mp: MediaPlayer? = null

        // 1. Primary initialization
        try {
            mp = MediaPlayer.create(appContext, soundResId, audioAttributes, 0)
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer.create with AudioAttributes failed, falling back", e)
            mp = null
        }

        // 2. Standard fallback
        if (mp == null) {
            try {
                mp = MediaPlayer.create(appContext, soundResId)
            } catch (e: Exception) {
                Log.w(TAG, "MediaPlayer.create fallback failed", e)
                mp = null
            }
        }

        // 3. FileDescriptor fallback
        if (mp == null) {
            try {
                val afd = appContext.resources.openRawResourceFd(soundResId)
                if (afd != null) {
                    mp = MediaPlayer().apply {
                        setAudioAttributes(audioAttributes)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        prepare()
                    }
                    afd.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "FileDescriptor initialization failed", e)
                mp = null
            }
        }

        // 4. Uri fallback
        if (mp == null) {
            try {
                val soundUri = Uri.parse("android.resource://${appContext.packageName}/$soundResId")
                mp = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(appContext, soundUri)
                    prepare()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Uri initialization failed", e)
                mp = null
            }
        }

        if (mp != null) {
            mediaPlayer = mp
            try {
                mp.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                mp.setVolume(currentPlaybackVolume, currentPlaybackVolume)
                mp.isLooping = false
                mp.setOnCompletionListener {
                    Log.d(TAG, "Azan playback completed naturally")
                    stop()
                }
                mp.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stop()
                    true
                }
                mp.start()

                _isPlaying.value = true
                _currentPrayerName.value = prayerName
                _currentSoundCode.value = soundCode

                // Set 5-minute safety timeout
                handler.removeCallbacks(safetyTimeoutRunnable)
                handler.postDelayed(safetyTimeoutRunnable, 5 * 60 * 1000L)

                // Schedule takbeer-only stop if selected (or takbeer sound selected)
                handler.removeCallbacks(takbeerEndRunnable)
                if (isTakbeerOnly || soundCode == "takbeer") {
                    val takbeerDelayMs = if (soundCode == "takbeer") 17 * 1000L else 16 * 1000L
                    handler.postDelayed(takbeerEndRunnable, takbeerDelayMs)
                }

                Log.d(TAG, "Azan sound successfully started with volume $currentPlaybackVolume (silentMode=$isSilentMode, duration: ${mp.duration}ms)")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MediaPlayer", e)
                stop()
            }
        } else {
            Log.e(TAG, "Failed to initialize MediaPlayer for Azan sound")
            stop()
        }
    }

    private fun performSmoothFadeAndStop() {
        val steps = 10
        val stepDelay = 250L // Total 2.5s fade out
        var currentStep = 0
        val baseVol = currentPlaybackVolume

        val fadeRunnable = object : Runnable {
            override fun run() {
                currentStep++
                val volume = (baseVol * (1.0f - (currentStep.toFloat() / steps))).coerceAtLeast(0.0f)
                try {
                    mediaPlayer?.setVolume(volume, volume)
                } catch (e: Exception) {}

                if (currentStep < steps && _isPlaying.value) {
                    handler.postDelayed(this, stepDelay)
                } else {
                    stop()
                }
            }
        }
        handler.post(fadeRunnable)
    }

    @Synchronized
    fun stop() {
        Log.d(TAG, "AzanAudioPlayer.stop() called")
        stopInternal()
    }

    private fun stopInternal() {
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
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        abandonAudioFocus()
        releaseWakeLock()

        _isPlaying.value = false
        _currentPrayerName.value = null
        _currentSoundCode.value = null
    }

    private fun acquireWakeLock(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "HolyQuran:AzanAudioPlayerWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(5 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        } finally {
            wakeLock = null
        }
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
}
