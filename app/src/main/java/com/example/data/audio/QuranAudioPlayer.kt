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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            _isBuffering.value = true
                        }
                        Player.STATE_READY -> {
                            _isBuffering.value = false
                            _isPlaying.value = player.isPlaying
                        }
                        Player.STATE_ENDED -> {
                            _isBuffering.value = false
                            _isPlaying.value = false
                            val callback = activeCompletionCallback
                            activeCompletionCallback = null
                            callback?.invoke()
                        }
                        Player.STATE_IDLE -> {
                            _isBuffering.value = false
                            _isPlaying.value = false
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        _isBuffering.value = false
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.w(TAG, "ExoPlayer playback error: ${error.message}")
                    _isBuffering.value = false
                    _isPlaying.value = false
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

    fun setRepeatMode(mode: AyahRepeatMode) {
        _repeatMode.value = mode
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
                "https://server7.mp3quran.net/basit/$surahStr.mp3"
            )
            "husary" -> listOf(
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
                "https://server8.mp3quran.net/afs/$surahStr.mp3",
                "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/$surahNumber.mp3"
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
        stopAudio()
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

    fun playSurahAudio(surahNumber: Int, reciterId: String, onComplete: () -> Unit = {}) {
        val candidates = getSurahAudioCandidates(reciterId, surahNumber)
        playSurahCandidate(surahNumber, reciterId, candidates, 0, onComplete)
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
            stopAudio()
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
                    player.pause()
                    _isPlaying.value = false
                } else {
                    if (player.playbackState == Player.STATE_ENDED) {
                        player.seekTo(0)
                    }
                    player.play()
                    _isPlaying.value = true
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

    fun stopAudio() {
        activeCompletionCallback = null
        activeErrorCallback = null
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
