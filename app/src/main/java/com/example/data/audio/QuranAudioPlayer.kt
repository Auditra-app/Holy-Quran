package com.example.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.domain.model.AudioListeningScope
import com.example.domain.model.PlayableVerseItem
import com.example.notification.QuranAudioPlaybackService
import com.example.widget.QuranAudioPlayerWidgetProvider
import java.io.File
import java.util.Locale

enum class AyahRepeatMode(val labelAr: String, val labelEn: String, val repeatTimes: Int) {
    ONCE("مرة واحدة", "Once", 1),
    THREE_TIMES("٣ مرات", "3 Times", 3),
    FIVE_TIMES("٥ مرات", "5 Times", 5),
    LOOP("مستمر 🔁", "Loop 🔁", -1)
}

@OptIn(UnstableApi::class)
class QuranAudioPlayer(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "QuranAudioPlayer"
        private const val MAX_CACHE_SIZE = 200 * 1024 * 1024L // 200MB limit

        @Volatile
        private var simpleCache: SimpleCache? = null

        @Synchronized
        private fun getSimpleCache(context: Context): SimpleCache {
            return simpleCache ?: run {
                val cacheDir = File(context.cacheDir, "quran_audio_cache")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
                val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
                SimpleCache(cacheDir, evictor, databaseProvider).also { simpleCache = it }
            }
        }
    }

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingSurah = MutableStateFlow<Int?>(null)
    val currentPlayingSurah: StateFlow<Int?> = _currentPlayingSurah.asStateFlow()

    private val _currentPlayingVerse = MutableStateFlow<Pair<Int, Int>?>(null) // Pair(surah, verse)
    val currentPlayingVerse: StateFlow<Pair<Int, Int>?> = _currentPlayingVerse.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _repeatMode = MutableStateFlow(AyahRepeatMode.ONCE)
    val repeatMode: StateFlow<AyahRepeatMode> = _repeatMode.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _activeTitle = MutableStateFlow("سورة الفاتحة")
    val activeTitle: StateFlow<String> = _activeTitle.asStateFlow()

    private val _activeSubtitle = MutableStateFlow("مشاري راشد العفاسي")
    val activeSubtitle: StateFlow<String> = _activeSubtitle.asStateFlow()

    private val _activeScope = MutableStateFlow(AudioListeningScope.SURAH)
    val activeScope: StateFlow<AudioListeningScope> = _activeScope.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _totalQueueCount = MutableStateFlow(1)
    val totalQueueCount: StateFlow<Int> = _totalQueueCount.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _sessionProgressFraction = MutableStateFlow(0f)
    val sessionProgressFraction: StateFlow<Float> = _sessionProgressFraction.asStateFlow()

    private val _sessionElapsedMs = MutableStateFlow(0L)
    val sessionElapsedMs: StateFlow<Long> = _sessionElapsedMs.asStateFlow()

    private val _sessionTotalMs = MutableStateFlow(0L)
    val sessionTotalMs: StateFlow<Long> = _sessionTotalMs.asStateFlow()

    private var currentQueueVerses: List<PlayableVerseItem> = emptyList()

    private var currentPlaybackSpeed: Float = 1.0f
    private var remainingLoops = 1

    private var activeCompletionCallback: (() -> Unit)? = null
    private var activeErrorCallback: (() -> Unit)? = null

    private val cacheDataSourceFactory: CacheDataSource.Factory by lazy {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
        CacheDataSource.Factory()
            .setCache(getSimpleCache(context.applicationContext))
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private val mediaSourceFactory by lazy {
        DefaultMediaSourceFactory(context.applicationContext)
            .setDataSourceFactory(cacheDataSourceFactory)
    }

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "TTS initialization error", e)
        }
        QuranAudioPlaybackService.onTogglePlayPause = { togglePlayPause() }
        QuranAudioPlaybackService.onNextTrack = { skipToNext() }
        QuranAudioPlaybackService.onPrevTrack = { skipToPrevious() }
        QuranAudioPlaybackService.onStopPlayback = { stopAudio(stopService = false) }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: run {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            val player = ExoPlayer.Builder(context.applicationContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()

            player.addListener(object : Player.Listener {
                override fun onIsLoadingChanged(isLoading: Boolean) {
                    _isBuffering.value = isLoading && !player.isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    exoPlayer?.let { p ->
                        val idx = p.currentMediaItemIndex
                        _currentQueueIndex.value = idx
                        if (idx in currentQueueVerses.indices) {
                            val v = currentQueueVerses[idx]
                            _currentPlayingVerse.value = Pair(v.surahNumber, v.verseNumber)
                            _currentPlayingSurah.value = v.surahNumber
                            _activeTitle.value = "سورة ${v.surahName.ifBlank { v.surahNumber.toString() }} - آية ${v.verseNumber}"
                            notifyPlaybackStateChanged(_isPlaying.value)
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            _isBuffering.value = true
                        }
                        Player.STATE_READY -> {
                            _isBuffering.value = false
                            _isPlaying.value = player.isPlaying
                            val d = player.duration
                            if (d > 0) {
                                _duration.value = d
                            }
                            startProgressUpdates()
                        }
                        Player.STATE_ENDED -> {
                            _isBuffering.value = false
                            _isPlaying.value = false
                            stopProgressUpdates()
                            _currentPosition.value = _duration.value
                            notifyPlaybackStateChanged(false)
                            val callback = activeCompletionCallback
                            activeCompletionCallback = null
                            callback?.invoke()
                        }
                        Player.STATE_IDLE -> {
                            _isBuffering.value = false
                            _isPlaying.value = false
                            stopProgressUpdates()
                            notifyPlaybackStateChanged(false)
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    notifyPlaybackStateChanged(isPlaying)
                    if (isPlaying) {
                        _isBuffering.value = false
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.w(TAG, "ExoPlayer playback error: ${error.message}")
                    _isBuffering.value = false
                    _isPlaying.value = false
                    stopProgressUpdates()
                    val errCb = activeErrorCallback
                    activeErrorCallback = null
                    errCb?.invoke()
                }
            })

            try {
                mediaSession = MediaSession.Builder(context.applicationContext, player)
                    .setId("QuranAudioSession")
                    .build()
            } catch (e: Exception) {
                Log.w(TAG, "MediaSession setup warning", e)
            }

            exoPlayer = player
            player
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ar"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = playerScope.launch {
            while (isActive) {
                exoPlayer?.let { p ->
                    val pos = p.currentPosition.coerceAtLeast(0L)
                    _currentPosition.value = pos
                    val d = p.duration
                    if (d > 0) {
                        _duration.value = d
                    }

                    // Update Cumulative Session Progress (Full Reading Time)
                    if (_activeScope.value == AudioListeningScope.SURAH || currentQueueVerses.isEmpty()) {
                        val surahDur = if (d > 0) d else _duration.value
                        _sessionTotalMs.value = surahDur
                        _sessionElapsedMs.value = pos
                        if (surahDur > 0) {
                            _sessionProgressFraction.value = (pos.toFloat() / surahDur.toFloat()).coerceIn(0f, 1f)
                        } else {
                            _sessionProgressFraction.value = 0f
                        }
                    } else {
                        val totalCount = currentQueueVerses.size.coerceAtLeast(1)
                        val currIdx = p.currentMediaItemIndex.coerceIn(0, totalCount - 1)
                        _currentQueueIndex.value = currIdx

                        val vDur = if (d > 0) d else 6000L
                        val vFrac = if (vDur > 0) (pos.toFloat() / vDur.toFloat()).coerceIn(0f, 1f) else 0f
                        val overallFrac = ((currIdx.toFloat() + vFrac) / totalCount.toFloat()).coerceIn(0f, 1f)
                        _sessionProgressFraction.value = overallFrac

                        val estTotalMs = totalCount * 6500L
                        _sessionTotalMs.value = estTotalMs
                        _sessionElapsedMs.value = (overallFrac * estTotalMs).toLong()
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    fun seekToSessionFraction(fraction: Float) {
        val safeFrac = fraction.coerceIn(0f, 1f)
        _sessionProgressFraction.value = safeFrac
        if (_activeScope.value == AudioListeningScope.SURAH || currentQueueVerses.isEmpty()) {
            val d = if (exoPlayer?.duration != null && exoPlayer!!.duration > 0) exoPlayer!!.duration else _duration.value
            if (d > 0) {
                val targetMs = (safeFrac * d).toLong()
                seekTo(targetMs)
            }
        } else {
            val totalCount = currentQueueVerses.size.coerceAtLeast(1)
            val targetIndex = (safeFrac * totalCount).toInt().coerceIn(0, totalCount - 1)
            try {
                exoPlayer?.seekTo(targetIndex, 0L)
                _currentQueueIndex.value = targetIndex
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking to session fraction in queue", e)
            }
        }
    }

    fun rewind10Seconds() {
        try {
            exoPlayer?.let { p ->
                val current = p.currentPosition
                val newPos = (current - 10000L).coerceAtLeast(0L)
                p.seekTo(newPos)
                _currentPosition.value = newPos
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rewinding 10 seconds", e)
        }
    }

    fun forward10Seconds() {
        try {
            exoPlayer?.let { p ->
                val current = p.currentPosition
                val maxDur = if (p.duration > 0) p.duration else Long.MAX_VALUE
                val newPos = (current + 10000L).coerceAtMost(maxDur)
                p.seekTo(newPos)
                _currentPosition.value = newPos
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding 10 seconds", e)
        }
    }

    fun hasActiveTrack(): Boolean {
        val p = exoPlayer ?: return false
        return p.playbackState != Player.STATE_IDLE && p.currentMediaItem != null
    }

    fun isPaused(): Boolean {
        val p = exoPlayer ?: return false
        return hasActiveTrack() && !p.isPlaying && p.playbackState != Player.STATE_ENDED
    }

    fun pauseAudio() {
        try {
            exoPlayer?.let { p ->
                if (p.isPlaying) {
                    p.pause()
                    _isPlaying.value = false
                    stopProgressUpdates()
                    notifyPlaybackStateChanged(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio", e)
        }
    }

    fun resumeAudio() {
        try {
            exoPlayer?.let { p ->
                if (p.playbackState == Player.STATE_ENDED) {
                    p.seekTo(0L)
                }
                p.play()
                _isPlaying.value = true
                startProgressUpdates()
                notifyPlaybackStateChanged(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming audio", e)
        }
    }

    private fun notifyPlaybackStateChanged(isPlaying: Boolean) {
        val statusText = if (_activeScope.value == AudioListeningScope.SURAH || currentQueueVerses.isEmpty()) {
            if (isPlaying) "تلاوة كاملة • جاري الاستماع" else "تلاوة كاملة • متوقف مؤقتاً"
        } else {
            "الآية ${_currentQueueIndex.value + 1} من ${currentQueueVerses.size}"
        }
        if (isPlaying) {
            QuranAudioPlaybackService.start(context.applicationContext, _activeTitle.value, _activeSubtitle.value, true)
        } else {
            if (QuranAudioPlaybackService.isServiceRunning.value) {
                QuranAudioPlaybackService.start(context.applicationContext, _activeTitle.value, _activeSubtitle.value, false)
            }
        }
        val progressPercent = (_sessionProgressFraction.value * 100).toInt().coerceIn(0, 100)
        QuranAudioPlayerWidgetProvider.notifyWidgetUpdate(
            context = context.applicationContext,
            title = _activeTitle.value,
            subtitle = _activeSubtitle.value,
            status = statusText,
            badge = _activeScope.value.titleAr,
            isPlaying = isPlaying,
            progressPercent = progressPercent
        )
    }

    fun seekTo(positionMs: Long) {
        try {
            exoPlayer?.seekTo(positionMs)
            _currentPosition.value = positionMs
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking ExoPlayer", e)
        }
    }

    fun hasNextItem(): Boolean {
        return exoPlayer?.hasNextMediaItem() == true
    }

    fun hasPreviousItem(): Boolean {
        return exoPlayer?.hasPreviousMediaItem() == true
    }

    fun skipToNext() {
        try {
            exoPlayer?.let { p ->
                if (p.hasNextMediaItem()) {
                    p.seekToNextMediaItem()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping to next", e)
        }
    }

    fun skipToPrevious() {
        try {
            exoPlayer?.let { p ->
                if (p.currentPosition > 3000L) {
                    p.seekTo(0L)
                } else if (p.hasPreviousMediaItem()) {
                    p.seekToPreviousMediaItem()
                } else {
                    p.seekTo(0L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping to previous", e)
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }
        _sleepTimerRemainingSeconds.value = minutes * 60
        sleepTimerJob = playerScope.launch {
            var remaining = minutes * 60
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            _sleepTimerRemainingSeconds.value = null
            stopAudio()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSeconds.value = null
    }

    fun setRepeatMode(mode: AyahRepeatMode) {
        _repeatMode.value = mode
        exoPlayer?.let { player ->
            when (mode) {
                AyahRepeatMode.LOOP -> player.repeatMode = Player.REPEAT_MODE_ALL
                else -> player.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        currentPlaybackSpeed = speed
        exoPlayer?.let { player ->
            player.playbackParameters = PlaybackParameters(speed)
        }
    }

    private fun normalizeReciterKey(reciterId: String): String {
        return when (reciterId.trim().lowercase()) {
            "1", "alafasy", "afs", "mishary", "mishary rashid alafasy", "ar.alafasy" -> "alafasy"
            "2", "abdulbasit", "basit", "abdul_basit", "abdul basit abdul samad" -> "abdulbasit"
            "3", "husary", "hussary", "husr", "al-husary", "mahmoud khalil al-husary" -> "husary"
            "minshawi", "minsh", "siddiq", "mohamed siddiq al-minshawi" -> "minshawi"
            "muaiqly", "maher", "maher al-muaiqly" -> "muaiqly"
            "ghamdi", "s_gmd", "saad", "saad al-ghamdi" -> "ghamdi"
            "4", "shuraim", "shur", "saud", "saud al-shuraim" -> "shuraim"
            "sudais", "sds", "abdurrahman", "abdul rahman al-sudais" -> "sudais"
            "5", "ajamy", "ajm", "ahmed", "ahmed al-ajmi" -> "ajamy"
            else -> "alafasy"
        }
    }

    private fun getSurahAudioCandidates(reciterId: String, surahNumber: Int): List<String> {
        val surahStr = surahNumber.toString().padStart(3, '0')
        val reciterKey = normalizeReciterKey(reciterId)
        return when (reciterKey) {
            "abdulbasit" -> listOf(
                "https://cdn.islamic.network/quran/audio-surah/128/ar.abdulbasitmurattal/$surahNumber.mp3",
                "https://server7.mp3quran.net/basit/$surahStr.mp3"
            )
            "husary" -> listOf(
                "https://cdn.islamic.network/quran/audio-surah/128/ar.husary/$surahNumber.mp3",
                "https://server13.mp3quran.net/husr/$surahStr.mp3"
            )
            "minshawi" -> listOf(
                "https://server10.mp3quran.net/minsh/$surahStr.mp3"
            )
            "muaiqly" -> listOf(
                "https://server12.mp3quran.net/maher/$surahStr.mp3"
            )
            "ghamdi" -> listOf(
                "https://server7.mp3quran.net/s_gmd/$surahStr.mp3"
            )
            "shuraim" -> listOf(
                "https://server7.mp3quran.net/shur/$surahStr.mp3"
            )
            "sudais" -> listOf(
                "https://server11.mp3quran.net/sds/$surahStr.mp3"
            )
            "ajamy" -> listOf(
                "https://server10.mp3quran.net/ajm/$surahStr.mp3"
            )
            else -> listOf(
                "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/$surahNumber.mp3",
                "https://download.quranicaudio.com/quran/mishaari_raashid_al_3afaasee/$surahStr.mp3",
                "https://server8.mp3quran.net/afs/$surahStr.mp3"
            )
        }
    }

    private fun getAyahAudioCandidates(reciterId: String, surahNumber: Int, verseNumber: Int): List<String> {
        val surahStr = surahNumber.toString().padStart(3, '0')
        val verseStr = verseNumber.toString().padStart(3, '0')
        val reciterKey = normalizeReciterKey(reciterId)

        val folder = when (reciterKey) {
            "abdulbasit" -> "Abdul_Basit_Murattal_192kbps"
            "husary" -> "Husary_128kbps"
            "minshawi" -> "Minshawy_Murattal_128kbps"
            "muaiqly" -> "MaherAlMuaiqly128kbps"
            "ghamdi" -> "Ghamadi_40kbps"
            "shuraim" -> "Saood_ash-Shuraym_128kbps"
            "sudais" -> "Abdurrahmaan_As-Sudais_192kbps"
            "ajamy" -> "Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaballah.net"
            else -> "Alafasy_128kbps"
        }

        val urls = mutableListOf("https://everyayah.com/data/$folder/$surahStr$verseStr.mp3")
        if (reciterKey == "alafasy") {
            urls.add("https://verses.quran.com/Alafasy/mp3/$surahStr$verseStr.mp3")
        }
        return urls
    }

    fun playAyahAudio(
        surahNumber: Int,
        verseNumber: Int,
        verseText: String,
        reciterId: String,
        mode: AyahRepeatMode = _repeatMode.value,
        onComplete: () -> Unit = {}
    ) {
        _repeatMode.value = mode
        remainingLoops = if (mode == AyahRepeatMode.LOOP) 9999 else mode.repeatTimes

        val candidates = getAyahAudioCandidates(reciterId, surahNumber, verseNumber)
        playAyahCandidate(surahNumber, verseNumber, verseText, reciterId, candidates, 0, onComplete)
    }

    private fun playAyahCandidate(
        surahNumber: Int,
        verseNumber: Int,
        verseText: String,
        reciterId: String,
        candidates: List<String>,
        candidateIndex: Int,
        onComplete: () -> Unit
    ) {
        if (candidateIndex >= candidates.size) {
            Log.w(TAG, "All audio candidates failed for Ayah $surahNumber:$verseNumber, falling back to TTS")
            _isBuffering.value = false
            speakWithTts(verseText) {
                handleAyahPlaybackCompletion(
                    surahNumber, verseNumber, verseText, reciterId, onComplete
                )
            }
            return
        }

        val audioUrl = candidates[candidateIndex]
        stopAudio(stopService = false)
        _isBuffering.value = true
        _currentPlayingVerse.value = Pair(surahNumber, verseNumber)

        try {
            val player = getOrCreatePlayer()
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle("آية $verseNumber - سورة $surahNumber")
                .setArtist(normalizeReciterKey(reciterId))
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(audioUrl)
                .setMediaMetadata(mediaMetadata)
                .build()

            activeCompletionCallback = {
                handleAyahPlaybackCompletion(surahNumber, verseNumber, verseText, reciterId, onComplete)
            }

            activeErrorCallback = {
                playAyahCandidate(surahNumber, verseNumber, verseText, reciterId, candidates, candidateIndex + 1, onComplete)
            }

            player.setMediaItem(mediaItem)
            player.playbackParameters = PlaybackParameters(currentPlaybackSpeed)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing Ayah candidate $audioUrl", e)
            playAyahCandidate(
                surahNumber, verseNumber, verseText, reciterId,
                candidates, candidateIndex + 1, onComplete
            )
        }
    }

    private fun handleAyahPlaybackCompletion(
        surahNumber: Int,
        verseNumber: Int,
        verseText: String,
        reciterId: String,
        onComplete: () -> Unit
    ) {
        if (_repeatMode.value == AyahRepeatMode.LOOP || remainingLoops > 1) {
            if (_repeatMode.value != AyahRepeatMode.LOOP) {
                remainingLoops--
            }
            val candidates = getAyahAudioCandidates(reciterId, surahNumber, verseNumber)
            playAyahCandidate(surahNumber, verseNumber, verseText, reciterId, candidates, 0, onComplete)
        } else {
            _isPlaying.value = false
            _isBuffering.value = false
            _currentPlayingVerse.value = null
            onComplete()
        }
    }

    private fun speakWithTts(text: String, onDone: () -> Unit) {
        if (ttsReady && tts != null) {
            _isPlaying.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AYAH_TTS_ID")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                _isPlaying.value = false
                onDone()
            }, (text.length * 120L).coerceAtLeast(3000L))
        } else {
            _isPlaying.value = false
            onDone()
        }
    }

    fun playDirectAudioUrl(
        url: String,
        title: String,
        subtitle: String,
        onComplete: () -> Unit = {}
    ) {
        stopAudio(stopService = false)
        _activeScope.value = AudioListeningScope.SURAH
        _activeTitle.value = title
        _activeSubtitle.value = subtitle
        _currentQueueIndex.value = 0
        _totalQueueCount.value = 1
        currentQueueVerses = emptyList()

        try {
            val player = getOrCreatePlayer()
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(subtitle)
                        .build()
                )
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            startProgressUpdates()
            notifyPlaybackStateChanged(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing direct audio URL", e)
        }
    }

    fun playSurahAudio(
        surahNumber: Int,
        reciterId: String,
        surahName: String = "",
        reciterDisplayName: String = "",
        onComplete: () -> Unit = {}
    ) {
        _activeScope.value = AudioListeningScope.SURAH
        _activeTitle.value = if (surahName.isNotBlank()) "سورة $surahName" else "سورة $surahNumber"
        _activeSubtitle.value = reciterDisplayName.ifBlank { normalizeReciterKey(reciterId) }
        _currentQueueIndex.value = 0
        _totalQueueCount.value = 1
        currentQueueVerses = emptyList()

        val candidates = getSurahAudioCandidates(reciterId, surahNumber)
        playSurahCandidate(surahNumber, reciterId, candidates, 0, onComplete)
    }

    fun playVerseQueue(
        verses: List<PlayableVerseItem>,
        reciterId: String,
        title: String,
        subtitle: String,
        scope: AudioListeningScope,
        startIndex: Int = 0,
        mode: AyahRepeatMode = _repeatMode.value,
        onComplete: () -> Unit = {}
    ) {
        if (verses.isEmpty()) return
        stopAudio(stopService = false)
        _isBuffering.value = true
        _activeScope.value = scope
        _activeTitle.value = title
        _activeSubtitle.value = subtitle
        _repeatMode.value = mode
        currentQueueVerses = verses
        _totalQueueCount.value = verses.size
        val safeIndex = startIndex.coerceIn(0, verses.lastIndex)
        _currentQueueIndex.value = safeIndex

        val firstVerse = verses[safeIndex]
        _currentPlayingVerse.value = Pair(firstVerse.surahNumber, firstVerse.verseNumber)
        _currentPlayingSurah.value = firstVerse.surahNumber

        try {
            val player = getOrCreatePlayer()
            when (mode) {
                AyahRepeatMode.LOOP -> player.repeatMode = Player.REPEAT_MODE_ALL
                else -> player.repeatMode = Player.REPEAT_MODE_OFF
            }

            val mediaItems = verses.map { v ->
                val urls = getAyahAudioCandidates(reciterId, v.surahNumber, v.verseNumber)
                val uri = urls.first()
                val metadata = MediaMetadata.Builder()
                    .setTitle("سورة ${v.surahName.ifBlank { v.surahNumber.toString() }} - آية ${v.verseNumber}")
                    .setArtist(subtitle)
                    .build()
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId("${v.surahNumber}:${v.verseNumber}")
                    .setMediaMetadata(metadata)
                    .build()
            }

            activeCompletionCallback = {
                _isPlaying.value = false
                _isBuffering.value = false
                _currentPlayingVerse.value = null
                stopProgressUpdates()
                onComplete()
            }

            player.setMediaItems(mediaItems, safeIndex, 0L)
            player.playbackParameters = PlaybackParameters(currentPlaybackSpeed)
            player.prepare()
            player.playWhenReady = true
            player.play()
            startProgressUpdates()
            notifyPlaybackStateChanged(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing verse queue", e)
            _isBuffering.value = false
            _isPlaying.value = false
        }
    }

    private fun playSurahCandidate(
        surahNumber: Int,
        reciterId: String,
        candidates: List<String>,
        candidateIndex: Int,
        onComplete: () -> Unit
    ) {
        if (candidateIndex >= candidates.size) {
            Log.e(TAG, "All audio candidates failed for Surah $surahNumber with reciter $reciterId")
            _isPlaying.value = false
            _isBuffering.value = false
            _currentPlayingSurah.value = null
            return
        }

        val audioUrl = candidates[candidateIndex]
        try {
            stopAudio(stopService = false)
            _isBuffering.value = true
            _currentPlayingSurah.value = surahNumber

            val player = getOrCreatePlayer()
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle("سورة $surahNumber")
                .setArtist(normalizeReciterKey(reciterId))
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(audioUrl)
                .setMediaMetadata(mediaMetadata)
                .build()

            activeCompletionCallback = {
                _isPlaying.value = false
                _isBuffering.value = false
                _currentPlayingSurah.value = null
                onComplete()
            }

            activeErrorCallback = {
                _isPlaying.value = false
                _isBuffering.value = false
                playSurahCandidate(surahNumber, reciterId, candidates, candidateIndex + 1, onComplete)
            }

            player.setMediaItem(mediaItem)
            player.playbackParameters = PlaybackParameters(currentPlaybackSpeed)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Surah audio playback for $audioUrl", e)
            playSurahCandidate(surahNumber, reciterId, candidates, candidateIndex + 1, onComplete)
        }
    }

    fun togglePlayPause() {
        try {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    pauseAudio()
                } else if (hasActiveTrack()) {
                    resumeAudio()
                }
            } ?: run {
                if (tts?.isSpeaking == true) {
                    tts?.stop()
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in togglePlayPause", e)
            _isPlaying.value = false
        }
    }

    fun stopAudio(stopService: Boolean = true) {
        activeCompletionCallback = null
        activeErrorCallback = null
        stopProgressUpdates()
        _currentPosition.value = 0L
        _sessionProgressFraction.value = 0f
        _sessionElapsedMs.value = 0L
        if (stopService) {
            QuranAudioPlaybackService.stop(context.applicationContext)
        }
        QuranAudioPlayerWidgetProvider.notifyWidgetUpdate(
            context = context.applicationContext,
            isPlaying = false
        )
        try {
            exoPlayer?.let { player ->
                if (player.isPlaying || player.playbackState != Player.STATE_IDLE) {
                    player.stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ExoPlayer", e)
        }
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        _isPlaying.value = false
        _isBuffering.value = false
        _currentPlayingSurah.value = null
        _currentPlayingVerse.value = null
    }

    fun playWordAudio(word: String) {
        stopAudio()
        if (ttsReady && tts != null) {
            _isPlaying.value = true
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "WORD_AUDIO_ID")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                _isPlaying.value = false
            }, (word.length * 200L).coerceAtLeast(1500L))
        }
    }

    fun release() {
        stopProgressUpdates()
        sleepTimerJob?.cancel()
        try {
            playerScope.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling player scope", e)
        }
        stopAudio()
        try {
            mediaSession?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MediaSession", e)
        }
        mediaSession = null

        try {
            exoPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing ExoPlayer", e)
        }
        exoPlayer = null

        tts?.shutdown()
        tts = null
    }
}
