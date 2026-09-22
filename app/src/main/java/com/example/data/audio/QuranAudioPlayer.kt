package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AyahRepeatMode(val labelAr: String, val labelEn: String, val repeatTimes: Int) {
    ONCE("مرة واحدة", "Once", 1),
    THREE_TIMES("٣ مرات", "3 Times", 3),
    FIVE_TIMES("٥ مرات", "5 Times", 5),
    LOOP("مستمر 🔁", "Loop 🔁", -1)
}

class QuranAudioPlayer(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "QuranAudioPlayer"

    private var mediaPlayer: MediaPlayer? = null
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

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "TTS initialization error", e)
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.playbackParams = mp.playbackParams.setSpeed(speed)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply playback speed: $speed", e)
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
            // Fallback to TTS offline pronunciation
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
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioUrl)
                setOnPreparedListener { mp ->
                    _isBuffering.value = false
                    _isPlaying.value = true
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentPlaybackSpeed != 1.0f) {
                            mp.playbackParams = mp.playbackParams.setSpeed(currentPlaybackSpeed)
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error setting playback params", e)
                    }
                    mp.start()
                }
                setOnCompletionListener {
                    handleAyahPlaybackCompletion(
                        surahNumber, verseNumber, verseText, reciterId, onComplete
                    )
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error on candidate $candidateIndex ($audioUrl): what=$what, extra=$extra")
                    _isBuffering.value = false
                    playAyahCandidate(
                        surahNumber, verseNumber, verseText, reciterId,
                        candidates, candidateIndex + 1, onComplete
                    )
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Ayah MediaPlayer for $audioUrl", e)
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
            // Schedule finish check based on text length
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

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioUrl)
                setOnPreparedListener { mp ->
                    _isBuffering.value = false
                    _isPlaying.value = true
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentPlaybackSpeed != 1.0f) {
                            mp.playbackParams = mp.playbackParams.setSpeed(currentPlaybackSpeed)
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error setting playback params", e)
                    }
                    mp.start()
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _isBuffering.value = false
                    _currentPlayingSurah.value = null
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Surah MediaPlayer error ($audioUrl): what=$what, extra=$extra")
                    _isPlaying.value = false
                    _isBuffering.value = false
                    playSurahCandidate(surahNumber, reciterId, candidates, candidateIndex + 1, onComplete)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Surah audio playback for $audioUrl", e)
            playSurahCandidate(surahNumber, reciterId, candidates, candidateIndex + 1, onComplete)
        }
    }

    fun togglePlayPause() {
        try {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        mp.pause()
                        _isPlaying.value = false
                    } else {
                        mp.start()
                        _isPlaying.value = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error toggling MediaPlayer state", e)
                    _isPlaying.value = false
                }
            } ?: run {
                try {
                    if (tts?.isSpeaking == true) {
                        tts?.stop()
                        _isPlaying.value = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping TTS in togglePlayPause", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in togglePlayPause", e)
            _isPlaying.value = false
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping player", e)
                }
                try {
                    mp.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing player", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling mediaPlayer in stopAudio", e)
        }
        mediaPlayer = null
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
        tts?.shutdown()
        tts = null
    }
}
