package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.QuranDatabase
import com.example.data.local.UserSettingsEntity
import com.example.data.repository.QuranRepository
import com.example.domain.model.*
import com.example.data.provider.QuranQuartersProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.data.audio.AyahRepeatMode
import com.example.data.audio.QuranAudioPlayer

data class ActiveWirdSession(
    val startPage: Int,
    val endPage: Int,
    val targetPages: Int
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val db = QuranDatabase.getDatabase(application)
    private val repository = QuranRepository(db.quranDao())
    private val audioPlayer = QuranAudioPlayer(application)

    val isPlayingAudio: StateFlow<Boolean> = audioPlayer.isPlaying
    val isBufferingAudio: StateFlow<Boolean> = audioPlayer.isBuffering
    val currentPlayingVerse: StateFlow<Pair<Int, Int>?> = audioPlayer.currentPlayingVerse
    val currentPlayingSurah: StateFlow<Int?> = audioPlayer.currentPlayingSurah
    val repeatMode: StateFlow<AyahRepeatMode> = audioPlayer.repeatMode

    val audioCurrentPosition: StateFlow<Long> = audioPlayer.currentPosition
    val audioDuration: StateFlow<Long> = audioPlayer.duration
    val audioSessionProgressFraction: StateFlow<Float> = audioPlayer.sessionProgressFraction
    val audioSessionElapsedMs: StateFlow<Long> = audioPlayer.sessionElapsedMs
    val audioSessionTotalMs: StateFlow<Long> = audioPlayer.sessionTotalMs
    val audioActiveTitle: StateFlow<String> = audioPlayer.activeTitle
    val audioActiveSubtitle: StateFlow<String> = audioPlayer.activeSubtitle
    val audioActiveScope: StateFlow<AudioListeningScope> = audioPlayer.activeScope
    val audioCurrentQueueIndex: StateFlow<Int> = audioPlayer.currentQueueIndex
    val audioTotalQueueCount: StateFlow<Int> = audioPlayer.totalQueueCount
    val audioSleepTimerRemainingSeconds: StateFlow<Int?> = audioPlayer.sleepTimerRemainingSeconds

    val userSettings: StateFlow<UserSettingsEntity> = repository.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettingsEntity()
        )

    // Reading Progress State
    val readingProgress: StateFlow<ReadingProgress> = repository.readingProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReadingProgress()
        )

    // Reading History Flow
    val readingHistory: StateFlow<List<com.example.data.local.ReadingHistoryEntity>> = repository.readingHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Bookmarks Flow

    // User Location State (lat, lng, cityName)
    private val _userLocation = MutableStateFlow<Triple<Double, Double, String>?>(null)
    val userLocation: StateFlow<Triple<Double, Double, String>?> = _userLocation.asStateFlow()

    fun setUserGPSLocation(lat: Double, lng: Double, cityName: String, context: Context? = null) {
        _userLocation.value = Triple(lat, lng, cityName)
        if (context != null) {
            com.example.util.LocationHelper.saveLocationAndSettings(context, lat, lng, cityName)
            com.example.widget.PrayerTimesWidgetProvider.sendUpdateBroadcast(context)
            com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(context)
        }
    }
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Favorite Surahs Flow
    val favoriteSurahIds: StateFlow<List<Int>> = repository.favoriteSurahIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Favorite Azkar Flow
    val favoriteAzkarIds: StateFlow<List<Int>> = repository.favoriteAzkarIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleFavoriteAzkar(azkarId: Int, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavoriteAzkar(azkarId, isFav)
        }
    }

    // Reader State
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _currentVerses = MutableStateFlow<List<Verse>>(emptyList())
    val currentVerses: StateFlow<List<Verse>> = _currentVerses.asStateFlow()

    private val _selectedVerseForDetail = MutableStateFlow<Verse?>(null)
    val selectedVerseForDetail: StateFlow<Verse?> = _selectedVerseForDetail.asStateFlow()

    // Search Query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val universalSearchResult: StateFlow<com.example.data.provider.UniversalSearchResult> = _searchQuery
        .map { query ->
            try {
                repository.universalSearch(query)
            } catch (e: Throwable) {
                android.util.Log.e("QuranViewModel", "Error in universalSearch mapping", e)
                com.example.data.provider.UniversalSearchResult("", 0, emptyList(), emptyList(), emptyList())
            }
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.example.data.provider.QuranSearchIndexer.search("")
        )

    val filteredSurahs: StateFlow<List<Surah>> = combine(
        _searchQuery,
        favoriteSurahIds
    ) { query, favIds ->
        try {
            val list = if (query.isBlank()) repository.getSurahs() else repository.searchQuran(query)
            list.map { surah ->
                surah.copy(isFavorite = favIds.contains(surah.id))
            }
        } catch (e: Throwable) {
            android.util.Log.e("QuranViewModel", "Error filtering surahs", e)
            emptyList()
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = try { repository.getSurahs() } catch (e: Throwable) { emptyList() }
    )

    // Audio Recitation State
    private val _selectedReciter = MutableStateFlow(
        try { repository.getReciters().firstOrNull() ?: Reciter("alafasy", "مشاري راشد العفاسي", "Mishary Rashid Alafasy", "https://server8.mp3quran.net/afs/") }
        catch (e: Throwable) { Reciter("alafasy", "مشاري راشد العفاسي", "Mishary Rashid Alafasy", "https://server8.mp3quran.net/afs/") }
    )
    val selectedReciter: StateFlow<Reciter> = _selectedReciter.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Daily Verse
    val dailyVerse: DailyVerse = try { repository.getDailyVerse() } catch (e: Throwable) { com.example.data.provider.QuranDataProvider.getDailyVerseForToday() }

    // Hadiths
    val hadiths: List<HadithItem> = try { repository.getHadiths() } catch (e: Throwable) { emptyList() }

    // Reciters List
    val reciters: List<Reciter> = try { repository.getReciters() } catch (e: Throwable) { emptyList() }

    init {
        // Load initial reader page
        viewModelScope.launch {
            try {
                readingProgress.collectLatest { progress ->
                    try {
                        _currentPage.value = progress.lastPage
                        loadPageData(progress.lastPage)
                    } catch (e: Throwable) {
                        android.util.Log.e("QuranViewModel", "Error in readingProgress collector item", e)
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("QuranViewModel", "Error in readingProgress collector flow", e)
            }
        }

        // Sync settings with audio player
        viewModelScope.launch {
            try {
                userSettings.collectLatest { settings ->
                    val reciterList = repository.getReciters()
                    val matched = reciterList.firstOrNull {
                        it.id.equals(settings.preferredReciterId, ignoreCase = true) ||
                        (settings.preferredReciterId == "1" && it.id == "alafasy") ||
                        (settings.preferredReciterId == "2" && it.id == "abdulbasit") ||
                        (settings.preferredReciterId == "3" && it.id == "husary") ||
                        (settings.preferredReciterId == "4" && it.id == "shuraim") ||
                        (settings.preferredReciterId == "5" && it.id == "ajamy")
                    }
                    if (matched != null) {
                        _selectedReciter.value = matched
                    }
                    if (settings.audioPlaybackSpeed > 0f) {
                        _playbackSpeed.value = settings.audioPlaybackSpeed
                        audioPlayer.setPlaybackSpeed(settings.audioPlaybackSpeed)
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("QuranViewModel", "Error syncing settings to audioPlayer", e)
            }
        }
    }

    fun jumpToPage(page: Int) {
        try {
            val targetPage = page.coerceIn(1, 604)
            _currentPage.value = targetPage
            loadPageData(targetPage)
            // Completely isolate Wird mode: do NOT update free reading progress/bookmark if user is in an active Wird session
            if (_activeWirdSession.value != null) {
                updateWirdProgressPage(targetPage)
            } else {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val surahList = repository.getSurahs()
                        val surah = surahList.lastOrNull { it.startPage <= targetPage } ?: surahList.firstOrNull()
                        if (surah != null) {
                            repository.updateReadingProgress(targetPage, surah.nameArabic)
                            com.example.widget.ReadingStatsWidgetProvider.notifyWidgetUpdate(getApplication())
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("QuranViewModel", "Error updating reading progress in jumpToPage", e)
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("QuranViewModel", "Error in jumpToPage", e)
        }
    }

    private fun loadPageData(page: Int) {
        try {
            _currentVerses.value = repository.getVersesForPage(page)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val fetched = com.example.data.provider.QuranPageFetcher.fetchAndCachePage(page)
                    if (fetched != null && _currentPage.value == page) {
                        _currentVerses.value = fetched
                    }
                    if (page < 604) {
                        com.example.data.provider.QuranPageFetcher.fetchAndCachePage(page + 1)
                    }
                    if (page > 1) {
                        com.example.data.provider.QuranPageFetcher.fetchAndCachePage(page - 1)
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("QuranViewModel", "Error in async page fetching", e)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("QuranViewModel", "Error in loadPageData", e)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoriteSurah(surahId: Int, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavoriteSurah(surahId, isFav)
        }
    }

    fun addCurrentPageBookmark(note: String = "") {
        addBookmarkForPage(_currentPage.value, note)
    }

    fun addBookmarkForPage(page: Int, note: String = "") {
        val surah = repository.getSurahs().lastOrNull { it.startPage <= page } ?: repository.getSurahs().first()
        viewModelScope.launch {
            repository.addBookmark(
                pageNumber = page,
                surahName = surah.nameArabic,
                note = note
            )
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    fun updateBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.updateBookmark(bookmark)
        }
    }

    fun setSelectedVerseForDetail(verse: Verse?) {
        _selectedVerseForDetail.value = verse
    }

    fun playAyahAudio(verse: Verse, mode: AyahRepeatMode = repeatMode.value) {
        audioPlayer.playAyahAudio(
            surahNumber = verse.surahId,
            verseNumber = verse.verseNumber,
            verseText = verse.textUthmani,
            reciterId = _selectedReciter.value.id,
            mode = mode
        )
    }

    fun playWordAudio(word: String) {
        audioPlayer.playWordAudio(word)
    }

    fun setAyahRepeatMode(mode: AyahRepeatMode) {
        audioPlayer.setRepeatMode(mode)
    }

    fun navigateToNextVerse() {
        val current = _selectedVerseForDetail.value ?: return
        val verses = _currentVerses.value
        val index = verses.indexOfFirst { it.id == current.id }
        if (index != -1 && index + 1 < verses.size) {
            _selectedVerseForDetail.value = verses[index + 1]
        }
    }

    fun navigateToPreviousVerse() {
        val current = _selectedVerseForDetail.value ?: return
        val verses = _currentVerses.value
        val index = verses.indexOfFirst { it.id == current.id }
        if (index > 0) {
            _selectedVerseForDetail.value = verses[index - 1]
        }
    }

    fun toggleAudioPlayback() {
        if (audioPlayer.hasActiveTrack()) {
            audioPlayer.togglePlayPause()
        } else {
            val surah = repository.getSurahs().lastOrNull { it.startPage <= _currentPage.value } ?: repository.getSurahs().first()
            playSurahAudioDetailed(surah.id, _selectedReciter.value.id)
        }
    }

    fun hasActiveAudio(): Boolean = audioPlayer.hasActiveTrack()
    fun isAudioPaused(): Boolean = audioPlayer.isPaused()

    fun resumeAudioPlayback() {
        audioPlayer.resumeAudio()
    }

    fun pauseAudioPlayback() {
        audioPlayer.pauseAudio()
    }

    fun playSpecificSurahAudio(surahId: Int) {
        val surah = repository.getSurahs().firstOrNull { it.id == surahId } ?: repository.getSurahs().first()
        audioPlayer.playSurahAudio(
            surahNumber = surahId,
            reciterId = _selectedReciter.value.id,
            surahName = surah.nameArabic,
            reciterDisplayName = _selectedReciter.value.nameArabic
        )
    }

    fun playSurahAudioDetailed(surahId: Int, reciterId: String? = null) {
        val reciter = reciterId?.let { id -> reciters.firstOrNull { it.id == id } } ?: _selectedReciter.value
        val surah = repository.getSurahs().firstOrNull { it.id == surahId } ?: repository.getSurahs().first()
        audioPlayer.playSurahAudio(
            surahNumber = surah.id,
            reciterId = reciter.id,
            surahName = surah.nameArabic,
            reciterDisplayName = reciter.nameArabic
        )
    }

    fun playJuzAudioDetailed(juzNumber: Int, reciterId: String? = null) {
        val reciter = reciterId?.let { id -> reciters.firstOrNull { it.id == id } } ?: _selectedReciter.value
        val juz = repository.getJuzList().firstOrNull { it.number == juzNumber } ?: repository.getJuzList().first()
        val nextJuz = repository.getJuzList().firstOrNull { it.number == juzNumber + 1 }
        val startPage = juz.startPage
        val endPage = nextJuz?.let { it.startPage - 1 } ?: 604
        val allVerses = mutableListOf<PlayableVerseItem>()
        for (p in startPage..endPage) {
            val pageVerses = repository.getVersesForPage(p)
            pageVerses.forEach { v ->
                if (allVerses.none { it.surahNumber == v.surahId && it.verseNumber == v.verseNumber }) {
                    allVerses.add(PlayableVerseItem(v.surahId, v.verseNumber, v.surahNameArabic, v.textUthmani, v.pageNumber))
                }
            }
        }
        audioPlayer.playVerseQueue(
            verses = allVerses,
            reciterId = reciter.id,
            title = juz.nameArabic,
            subtitle = reciter.nameArabic,
            scope = AudioListeningScope.JUZ
        )
    }

    fun playPageAudioDetailed(pageNumber: Int, reciterId: String? = null) {
        val reciter = reciterId?.let { id -> reciters.firstOrNull { it.id == id } } ?: _selectedReciter.value
        val page = pageNumber.coerceIn(1, 604)
        val verses = repository.getVersesForPage(page).map {
            PlayableVerseItem(it.surahId, it.verseNumber, it.surahNameArabic, it.textUthmani, it.pageNumber)
        }
        val surah = repository.getSurahs().lastOrNull { it.startPage <= page }
        audioPlayer.playVerseQueue(
            verses = verses,
            reciterId = reciter.id,
            title = "صفحة $page ${surah?.let { "(${it.nameArabic})" } ?: ""}",
            subtitle = reciter.nameArabic,
            scope = AudioListeningScope.PAGE
        )
    }

    fun playQuarterAudioDetailed(quarterId: Int, reciterId: String? = null) {
        val reciter = reciterId?.let { id -> reciters.firstOrNull { it.id == id } } ?: _selectedReciter.value
        val quarter = QuranQuartersProvider.getQuarterById(quarterId) ?: QuranQuartersProvider.QUARTERS.first()
        val nextQuarter = QuranQuartersProvider.getQuarterById(quarterId + 1)

        val surahs = repository.getSurahs()
        val verses = mutableListOf<PlayableVerseItem>()

        val startSurah = quarter.surahId
        val startAyah = quarter.startAyah
        val endSurah = nextQuarter?.surahId ?: 114
        val endAyah = nextQuarter?.let {
            if (it.startAyah > 1) it.startAyah - 1
            else {
                surahs.firstOrNull { s -> s.id == it.surahId - 1 }?.versesCount ?: 1
            }
        } ?: 6

        if (startSurah == endSurah) {
            val s = surahs.firstOrNull { it.id == startSurah }
            val sName = s?.nameArabic ?: quarter.surahNameArabic
            for (a in startAyah..endAyah.coerceAtLeast(startAyah)) {
                verses.add(PlayableVerseItem(startSurah, a, sName))
            }
        } else {
            val s1 = surahs.firstOrNull { it.id == startSurah }
            val count1 = s1?.versesCount ?: startAyah
            for (a in startAyah..count1) {
                verses.add(PlayableVerseItem(startSurah, a, s1?.nameArabic ?: quarter.surahNameArabic))
            }
            for (sid in (startSurah + 1) until endSurah) {
                val smid = surahs.firstOrNull { it.id == sid }
                if (smid != null) {
                    for (a in 1..smid.versesCount) {
                        verses.add(PlayableVerseItem(sid, a, smid.nameArabic))
                    }
                }
            }
            val sEnd = surahs.firstOrNull { it.id == endSurah }
            if (sEnd != null) {
                for (a in 1..endAyah.coerceAtMost(sEnd.versesCount)) {
                    verses.add(PlayableVerseItem(endSurah, a, sEnd.nameArabic))
                }
            }
        }

        val title = "ربع ${quarter.id}: ${quarter.firstWords}"
        audioPlayer.playVerseQueue(
            verses = verses,
            reciterId = reciter.id,
            title = title,
            subtitle = reciter.nameArabic,
            scope = AudioListeningScope.QUARTER
        )
    }

    fun playAyahRangeAudioDetailed(
        surahId: Int,
        startAyah: Int,
        endAyah: Int,
        reciterId: String? = null
    ) {
        val reciter = reciterId?.let { id -> reciters.firstOrNull { it.id == id } } ?: _selectedReciter.value
        val surah = repository.getSurahs().firstOrNull { it.id == surahId } ?: repository.getSurahs().first()
        val sAyah = startAyah.coerceIn(1, surah.versesCount)
        val eAyah = endAyah.coerceIn(sAyah, surah.versesCount)

        val verses = (sAyah..eAyah).map { a ->
            PlayableVerseItem(surah.id, a, surah.nameArabic)
        }

        val title = "سورة ${surah.nameArabic} (الآيات $sAyah - $eAyah)"
        audioPlayer.playVerseQueue(
            verses = verses,
            reciterId = reciter.id,
            title = title,
            subtitle = reciter.nameArabic,
            scope = AudioListeningScope.AYAH_RANGE
        )
    }

    fun playAzkarAudio(isMorning: Boolean) {
        val url = if (isMorning) {
            "https://archive.org/download/sheikh-mishary-rashid-alafasy-azkar/Sheikh%20Mishary%20Rashid%20Alafasy%20-%20%D8%A3%D8%B0%D9%83%D8%A7%D8%B1%20%D8%A7%D9%84%D8%B5%D8%A8%D8%A7%D8%AD.mp3"
        } else {
            "https://archive.org/download/sheikh-mishary-rashid-alafasy-azkar/Sheikh%20Mishary%20Rashid%20Alafasy%20-%20%D8%A3%D8%B0%D9%83%D8%A7%D8%B1%20%D8%A7%D9%84%D9%85%D8%B3%D8%A7%D8%A1.mp3"
        }
        val title = if (isMorning) "أذكار الصباح بصوت الشيخ مشاري العفاسي" else "أذكار المساء بصوت الشيخ مشاري العفاسي"
        val subtitle = "الشيخ مشاري راشد العفاسي"
        audioPlayer.playDirectAudioUrl(url, title, subtitle)
    }

    fun seekAudio(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
    }

    fun seekAudioSessionFraction(fraction: Float) {
        audioPlayer.seekToSessionFraction(fraction)
    }

    fun rewind10Seconds() {
        audioPlayer.rewind10Seconds()
    }

    fun forward10Seconds() {
        audioPlayer.forward10Seconds()
    }

    fun skipNextTrack() {
        if (audioPlayer.hasNextItem()) {
            audioPlayer.skipToNext()
        } else if (audioPlayer.activeScope.value == AudioListeningScope.SURAH || audioPlayer.totalQueueCount.value <= 1) {
            val currentSurah = audioPlayer.currentPlayingSurah.value ?: 1
            val nextSurah = if (currentSurah < 114) currentSurah + 1 else 1
            playSurahAudioDetailed(nextSurah, _selectedReciter.value.id)
        } else {
            audioPlayer.skipToNext()
        }
    }

    fun skipPreviousTrack() {
        if (audioPlayer.hasPreviousItem()) {
            audioPlayer.skipToPrevious()
        } else if (audioPlayer.activeScope.value == AudioListeningScope.SURAH || audioPlayer.totalQueueCount.value <= 1) {
            val pos = audioPlayer.currentPosition.value
            if (pos > 3000L) {
                audioPlayer.seekTo(0L)
            } else {
                val currentSurah = audioPlayer.currentPlayingSurah.value ?: 1
                val prevSurah = if (currentSurah > 1) currentSurah - 1 else 114
                playSurahAudioDetailed(prevSurah, _selectedReciter.value.id)
            }
        } else {
            audioPlayer.skipToPrevious()
        }
    }

    fun setSleepTimer(minutes: Int) {
        audioPlayer.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        audioPlayer.cancelSleepTimer()
    }

    fun getQuartersList(): List<QuranQuarter> = QuranQuartersProvider.QUARTERS

    fun stopAudioPlayback() {
        audioPlayer.stopAudio()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stopAudio()
    }

    fun setReciter(reciter: Reciter) {
        _selectedReciter.value = reciter
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(preferredReciterId = reciter.id))
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        audioPlayer.setPlaybackSpeed(speed)
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(audioPlaybackSpeed = speed))
        }
    }

    fun exportBackupData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportBackupJson()
                onResult(json)
            } catch (e: Exception) {
                android.util.Log.e("QuranViewModel", "Error exporting backup", e)
                onResult("")
            }
        }
    }

    fun importBackupData(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.importBackupJson(jsonString)
                onResult(success)
            } catch (e: Exception) {
                android.util.Log.e("QuranViewModel", "Error importing backup", e)
                onResult(false)
            }
        }
    }

    fun updateMadhhab(madhhab: String, context: Context? = null) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(madhhab = madhhab))
            if (context != null) {
                com.example.widget.PrayerTimesWidgetProvider.sendUpdateBroadcast(context)
                com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(context)
            }
        }
    }

    fun updateSelectedAzanSound(sound: String) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(selectedAzanSound = sound))
        }
    }

    fun updatePrayerAlerts(enabledPrayersJson: String, silentPrayersJson: String, context: Context? = null) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(
                current.copy(
                    enabledPrayersJson = enabledPrayersJson,
                    silentPrayersJson = silentPrayersJson
                )
            )
            if (context != null) {
                val prayerTimes = getPrayerTimes()
                com.example.notification.PrayerNotificationScheduler.schedulePrayerNotifications(context, prayerTimes)
            }
        }
    }

    fun updateSelectedTafsirBook(bookId: String) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(selectedTafsirBook = bookId))
        }
    }

    fun updateQuranScript(scriptId: String) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(quranScriptType = scriptId))
        }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(languageCode = languageCode))
        }
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(themeMode = themeMode))
        }
    }

    fun updateNightModeSettings(
        nightModeTheme: String? = null,
        enableNightSchedule: Boolean? = null,
        startHour: Int? = null,
        startMinute: Int? = null,
        endHour: Int? = null,
        endMinute: Int? = null
    ) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(
                current.copy(
                    nightModeTheme = nightModeTheme ?: current.nightModeTheme,
                    enableNightSchedule = enableNightSchedule ?: current.enableNightSchedule,
                    nightScheduleStartHour = startHour ?: current.nightScheduleStartHour,
                    nightScheduleStartMinute = startMinute ?: current.nightScheduleStartMinute,
                    nightScheduleEndHour = endHour ?: current.nightScheduleEndHour,
                    nightScheduleEndMinute = endMinute ?: current.nightScheduleEndMinute
                )
            )
        }
    }

    fun toggleEnglishTranslation() {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(showEnglishTranslation = !current.showEnglishTranslation))
        }
    }

    fun updateTajweedSetting(enableTajweed: Boolean) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(enableTajweed = enableTajweed))
        }
    }

    fun updateKeepScreenOn(keepScreenOn: Boolean) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(keepScreenOn = keepScreenOn))
        }
    }

    fun updateFontSize(fontSizeSp: Float) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(fontSizeSp = fontSizeSp))
        }
    }

    fun updateNotificationSetting(dailyVerse: Boolean, reminder: Boolean, inactivity: Boolean, context: Context? = null) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(
                current.copy(
                    enableDailyVerseNotification = dailyVerse,
                    enableReadingReminderNotification = reminder,
                    enableInactivityNotification = inactivity
                )
            )
            val ctx = context ?: getApplication<Application>()
            com.example.notification.ReadingReminderScheduler.scheduleAllReadingReminders(ctx)
            if (dailyVerse) {
                com.example.notification.DailyVerseScheduler.scheduleDailyVerseNotification(ctx, current.notificationTimeHour, current.notificationTimeMinute)
            }
        }
    }

    fun updateNotificationTime(hour: Int, minute: Int, context: Context? = null) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(
                current.copy(
                    notificationTimeHour = hour,
                    notificationTimeMinute = minute
                )
            )
            val ctx = context ?: getApplication<Application>()
            com.example.notification.ReadingReminderScheduler.scheduleAllReadingReminders(ctx)
            if (current.enableDailyVerseNotification) {
                com.example.notification.DailyVerseScheduler.scheduleDailyVerseNotification(ctx, hour, minute)
            }
        }
    }

    fun updateAzkarNotificationSetting(
        enableAzkar: Boolean,
        enableMorning: Boolean,
        enableEvening: Boolean,
        morningHour: Int,
        morningMinute: Int,
        eveningHour: Int,
        eveningMinute: Int,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(
                current.copy(
                    enableAzkarNotification = enableAzkar,
                    enableMorningAzkar = enableMorning,
                    enableEveningAzkar = enableEvening,
                    morningAzkarHour = morningHour,
                    morningAzkarMinute = morningMinute,
                    eveningAzkarHour = eveningHour,
                    eveningAzkarMinute = eveningMinute
                )
            )
            val ctx = context ?: getApplication<Application>()
            com.example.notification.AzkarNotificationScheduler.scheduleAllAzkarNotifications(ctx)
        }
    }

    fun updatePrayerMethod(method: String, context: Context? = null) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(calculationMethod = method))
            if (context != null) {
                com.example.util.LocationHelper.saveCalculationSettings(context, calcMethod = method)
                com.example.widget.PrayerTimesWidgetProvider.sendUpdateBroadcast(context)
                com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(context)
            }
        }
    }

    fun updateSummerTimeSetting(isSummerTime: Boolean, context: Context? = null) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(isSummerTime = isSummerTime))
            if (context != null) {
                com.example.util.LocationHelper.saveCalculationSettings(context, isSummerTime = isSummerTime)
                com.example.widget.PrayerTimesWidgetProvider.sendUpdateBroadcast(context)
                com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(context)
            }
        }
    }

    fun updateTakbeerAzanSetting(takbeerOnly: Boolean) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(takbeerAzanOnly = takbeerOnly))
        }
    }

    fun updateEnableAzanOnSilent(enable: Boolean) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(enableAzanOnSilent = enable))
        }
    }

    fun updateHapticSettings(enableHaptic: Boolean? = null, hapticType: String? = null) {
        viewModelScope.launch {
            val current = userSettings.value
            val updated = current.copy(
                enableHaptic = enableHaptic ?: current.enableHaptic,
                hapticType = hapticType ?: current.hapticType
            )
            repository.saveSettings(updated)
        }
    }

    fun triggerHaptic(context: Context) {
        // Haptic feedback feature disabled
    }

    fun updateDailyPageGoal(goalPages: Int) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(dailyPageGoal = goalPages.coerceAtLeast(1)))
        }
    }

    // Dedicated Standalone Khatmah / Wird Session State
    private val _activeWirdSession = MutableStateFlow<ActiveWirdSession?>(null)
    val activeWirdSession: StateFlow<ActiveWirdSession?> = _activeWirdSession.asStateFlow()

    private val wirdPrefs by lazy {
        getApplication<Application>().getSharedPreferences("wird_session_prefs", Context.MODE_PRIVATE)
    }

    fun getWirdResumePage(startPage: Int, targetPages: Int): Int {
        val savedStart = wirdPrefs.getInt("wird_start_page", -1)
        val savedCurrent = wirdPrefs.getInt("wird_current_page", -1)
        val endPage = (startPage + targetPages - 1).coerceAtMost(604)
        if (savedStart == startPage && savedCurrent in startPage..endPage) {
            return savedCurrent
        }
        return startPage
    }

    fun startWirdSession(startPage: Int, targetPages: Int) {
        val end = (startPage + targetPages - 1).coerceAtMost(604)
        val resumePage = getWirdResumePage(startPage, targetPages)
        _activeWirdSession.value = ActiveWirdSession(startPage, end, targetPages)
        jumpToPage(resumePage)
    }

    fun updateWirdProgressPage(page: Int) {
        val session = _activeWirdSession.value ?: return
        if (page in session.startPage..session.endPage) {
            wirdPrefs.edit()
                .putInt("wird_start_page", session.startPage)
                .putInt("wird_current_page", page)
                .apply()
        }
    }

    fun clearSavedWirdProgress() {
        wirdPrefs.edit().clear().apply()
    }

    fun completeCurrentWird() {
        val session = _activeWirdSession.value
        viewModelScope.launch {
            clearSavedWirdProgress()
            val current = userSettings.value
            val endPage = session?.endPage ?: (current.khatmahCurrentPage + current.khatmahDailyGoalPages - 1).coerceAtMost(604)
            val nextStartPage = if (endPage >= 604) 1 else endPage + 1
            val newCompletedCount = current.khatmahCompletedWirdsCount + 1
            repository.saveSettings(
                current.copy(
                    khatmahCurrentPage = nextStartPage,
                    khatmahCompletedWirdsCount = newCompletedCount,
                    khatmahLastCompletedDate = System.currentTimeMillis()
                )
            )
            _activeWirdSession.value = null
            // Restore current reader page to user's preserved free reading bookmark
            val lastFreePage = readingProgress.value.lastPage.coerceIn(1, 604)
            _currentPage.value = lastFreePage
            loadPageData(lastFreePage)
        }
    }

    fun completeWirdDirectly() {
        viewModelScope.launch {
            clearSavedWirdProgress()
            val current = userSettings.value
            val goalPages = current.khatmahDailyGoalPages.coerceAtLeast(1)
            val endPage = (current.khatmahCurrentPage + goalPages - 1).coerceAtMost(604)
            val nextStartPage = if (endPage >= 604) 1 else endPage + 1
            val newCompletedCount = current.khatmahCompletedWirdsCount + 1
            repository.saveSettings(
                current.copy(
                    khatmahCurrentPage = nextStartPage,
                    khatmahCompletedWirdsCount = newCompletedCount,
                    khatmahLastCompletedDate = System.currentTimeMillis()
                )
            )
            if (_activeWirdSession.value != null) {
                _activeWirdSession.value = null
                val lastFreePage = readingProgress.value.lastPage.coerceIn(1, 604)
                _currentPage.value = lastFreePage
                loadPageData(lastFreePage)
            }
        }
    }

    fun exitWirdSession(revertToFreeReading: Boolean = true) {
        if (_activeWirdSession.value != null) {
            _activeWirdSession.value = null
            if (revertToFreeReading) {
                val lastFreePage = readingProgress.value.lastPage.coerceIn(1, 604)
                _currentPage.value = lastFreePage
                loadPageData(lastFreePage)
            }
        }
    }

    fun setKhatmahDailyGoalPages(pages: Int) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(khatmahDailyGoalPages = pages.coerceIn(1, 604)))
        }
    }

    fun updateKhatmahTargetDays(days: Int) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(khatmahTargetDays = days.coerceAtLeast(1)))
        }
    }

    fun resetKhatmahProgress() {
        viewModelScope.launch {
            clearSavedWirdProgress()
            val current = userSettings.value
            repository.saveSettings(
                current.copy(
                    khatmahStartDate = System.currentTimeMillis(),
                    khatmahCurrentPage = 1,
                    khatmahCompletedWirdsCount = 0,
                    khatmahLastCompletedDate = 0L
                )
            )
            if (_activeWirdSession.value != null) {
                _activeWirdSession.value = null
                val lastFreePage = readingProgress.value.lastPage.coerceIn(1, 604)
                _currentPage.value = lastFreePage
                loadPageData(lastFreePage)
            }
        }
    }

    fun deleteReadingHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteReadingHistoryItem(id)
        }
    }

    fun clearReadingHistory() {
        viewModelScope.launch {
            repository.clearReadingHistory()
        }
    }

    fun getSurahs(): List<Surah> = repository.getSurahs()

    fun getJuzList(): List<Juz> = repository.getJuzList()

    fun getPrayerTimes(lat: Double? = null, lng: Double? = null): List<PrayerTime> {
        val settings = userSettings.value
        val loc = _userLocation.value
        val finalLat = lat ?: loc?.first ?: 30.0444
        val finalLng = lng ?: loc?.second ?: 31.2357
        return repository.getPrayerTimes(
            lat = finalLat,
            lng = finalLng,
            method = settings.calculationMethod,
            madhhab = settings.madhhab,
            isSummerTime = settings.isSummerTime,
            fajrOffset = settings.fajrOffsetMins,
            dhuhrOffset = settings.dhuhrOffsetMins,
            asrOffset = settings.asrOffsetMins,
            maghribOffset = settings.maghribOffsetMins,
            ishaOffset = settings.ishaOffsetMins
        )
    }

    fun updatePrayerOffsets(
        fajr: Int,
        dhuhr: Int,
        asr: Int,
        maghrib: Int,
        isha: Int,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val current = userSettings.value
            val updated = current.copy(
                fajrOffsetMins = fajr,
                dhuhrOffsetMins = dhuhr,
                asrOffsetMins = asr,
                maghribOffsetMins = maghrib,
                ishaOffsetMins = isha
            )
            repository.saveSettings(updated)
            if (context != null) {
                com.example.util.LocationHelper.saveCalculationSettings(
                    context,
                    fajrOffset = fajr,
                    dhuhrOffset = dhuhr,
                    asrOffset = asr,
                    maghribOffset = maghrib,
                    ishaOffset = isha
                )
                com.example.widget.PrayerTimesWidgetProvider.sendUpdateBroadcast(context)
                com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(context)
            }
        }
    }

    fun updateCalculationSettings(
        method: String,
        isSummer: Boolean,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val current = userSettings.value
            val updated = current.copy(
                calculationMethod = method,
                isSummerTime = isSummer
            )
            repository.saveSettings(updated)
            if (context != null) {
                com.example.util.LocationHelper.saveCalculationSettings(
                    context,
                    calcMethod = method,
                    isSummerTime = isSummer
                )
                com.example.widget.PrayerTimesWidgetProvider.sendUpdateBroadcast(context)
                com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(context)
            }
        }
    }

    fun updateMadhhab(madhhab: String) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(madhhab = madhhab))
        }
    }

    fun updateAzanSettings(takbeerOnly: Boolean, sound: String) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(takbeerAzanOnly = takbeerOnly, selectedAzanSound = sound))
        }
    }

    fun resetPrayerSettings(context: Context? = null) {
        viewModelScope.launch {
            val current = userSettings.value
            val resetSettings = current.copy(
                calculationMethod = "EGYPT",
                madhhab = "Shafi",
                isSummerTime = false,
                fajrOffsetMins = 0,
                dhuhrOffsetMins = 0,
                asrOffsetMins = 0,
                maghribOffsetMins = 0,
                ishaOffsetMins = 0
            )
            repository.saveSettings(resetSettings)
            if (context != null) {
                com.example.util.LocationHelper.saveCalculationSettings(
                    context,
                    calcMethod = "EGYPT",
                    isSummerTime = false,
                    fajrOffset = 0,
                    dhuhrOffset = 0,
                    asrOffset = 0,
                    maghribOffset = 0,
                    ishaOffset = 0
                )
                com.example.widget.PrayerTimesWidgetProvider.sendUpdateBroadcast(context)
                com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(context)
            }
        }
    }

    fun updateHomeGridOrder(orderRoutes: List<String>) {
        viewModelScope.launch {
            val current = userSettings.value
            val orderJson = orderRoutes.joinToString(",")
            repository.saveSettings(current.copy(homeGridOrderJson = orderJson))
        }
    }

    fun resetHomeGridOrder() {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(homeGridOrderJson = ""))
        }
    }

    fun getQiblaInfo(lat: Double? = null, lng: Double? = null): Pair<Double, Double> {
        val loc = _userLocation.value
        val finalLat = lat ?: loc?.first ?: 24.7136
        val finalLng = lng ?: loc?.second ?: 46.6753
        return repository.getQiblaInfo(lat = finalLat, lng = finalLng)
    }

    // ==========================================
    // PRAYER TRACKER STATE & KPI LOGIC
    // ==========================================
    private val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    private val dayNameArFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale("ar"))
    private val dayNameEnFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH)

    private val _selectedTrackerDate = MutableStateFlow(getTodayDateString())
    val selectedTrackerDate: StateFlow<String> = _selectedTrackerDate.asStateFlow()

    fun getTodayDateString(): String = dateFormat.format(java.util.Date())

    fun setSelectedTrackerDate(dateStr: String) {
        _selectedTrackerDate.value = dateStr
    }

    val allPrayerRecords: StateFlow<List<com.example.data.local.PrayerRecordEntity>> = repository.getAllPrayerRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val weeklyTrackerSummaries: StateFlow<List<DayPrayerTrackerSummary>> = combine(
        allPrayerRecords,
        userSettings,
        _selectedTrackerDate
    ) { records, settings, selectedDate ->
        val trackedKeys = settings.trackedPrayersJson.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val trackedTypes = trackedKeys.mapNotNull { TrackedPrayerType.fromKey(it) }

        // Calculate 7 days for the current selected week (Saturday to Friday)
        val cal = java.util.Calendar.getInstance()
        try {
            val date = dateFormat.parse(selectedDate)
            if (date != null) cal.time = date
        } catch (_: Exception) {}

        // Go to Saturday of the current week (Islamic standard start of week)
        val currentDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val diffToSaturday = if (currentDayOfWeek == java.util.Calendar.SATURDAY) 0 else ((currentDayOfWeek + 7 - java.util.Calendar.SATURDAY) % 7)
        cal.add(java.util.Calendar.DAY_OF_MONTH, -diffToSaturday)

        val days = mutableListOf<DayPrayerTrackerSummary>()
        val todayStr = getTodayDateString()

        for (i in 0 until 7) {
            val dStr = dateFormat.format(cal.time)
            val dNameAr = dayNameArFormat.format(cal.time)
            val dNameEn = dayNameEnFormat.format(cal.time)
            val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val dayRecords = records.filter { it.dateString == dStr }

            val prayerItems = trackedTypes.map { type ->
                val rec = dayRecords.firstOrNull { it.prayerKey.equals(type.key, ignoreCase = true) }
                PrayerStatusItem(
                    type = type,
                    isCompleted = rec?.isCompleted ?: false,
                    isOnTime = rec?.isOnTime ?: true,
                    completedTimestamp = rec?.completedTimestamp
                )
            }

            val completedCount = prayerItems.count { it.isCompleted }
            days.add(
                DayPrayerTrackerSummary(
                    dateString = dStr,
                    dayNameAr = dNameAr,
                    dayNameEn = dNameEn,
                    dayOfMonth = dayOfMonth,
                    isToday = (dStr == todayStr),
                    prayers = prayerItems,
                    completionCount = completedCount,
                    totalTrackedCount = prayerItems.size
                )
            )
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        days
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val prayerTrackerKPIs: StateFlow<PrayerTrackerKPIs> = combine(
        allPrayerRecords,
        userSettings,
        weeklyTrackerSummaries
    ) { records, settings, weeklySummaries ->
        val trackedKeys = settings.trackedPrayersJson.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val totalTrackedPerDay = if (trackedKeys.isNotEmpty()) trackedKeys.size else 5
        val todayStr = getTodayDateString()

        // Daily
        val todayRecords = records.filter { it.dateString == todayStr && trackedKeys.contains(it.prayerKey.lowercase()) }
        val dailyCompleted = todayRecords.count { it.isCompleted }

        // Weekly
        val weeklyCompleted = weeklySummaries.sumOf { it.completionCount }
        val weeklyTotal = weeklySummaries.sumOf { it.totalTrackedCount }

        // Monthly (last 30 days)
        val cal30 = java.util.Calendar.getInstance()
        cal30.add(java.util.Calendar.DAY_OF_MONTH, -29)
        val thirtyDaysAgoStr = dateFormat.format(cal30.time)

        val monthlyRecords = records.filter { it.dateString >= thirtyDaysAgoStr && trackedKeys.contains(it.prayerKey.lowercase()) }
        val monthlyCompleted = monthlyRecords.count { it.isCompleted }
        val monthlyTotal = totalTrackedPerDay * 30

        // Streak calculation (consecutive days ending today or yesterday where all tracked prayers were completed)
        var streak = 0
        val calStreak = java.util.Calendar.getInstance()
        while (streak < 365) {
            val dStr = dateFormat.format(calStreak.time)
            val dRecords = records.filter { it.dateString == dStr && trackedKeys.contains(it.prayerKey.lowercase()) }
            val completed = dRecords.count { it.isCompleted }
            if (completed >= totalTrackedPerDay && totalTrackedPerDay > 0) {
                streak++
                calStreak.add(java.util.Calendar.DAY_OF_MONTH, -1)
            } else if (dStr == todayStr && completed > 0) {
                // If today is partially done, check yesterday
                calStreak.add(java.util.Calendar.DAY_OF_MONTH, -1)
                val yStr = dateFormat.format(calStreak.time)
                val yRecords = records.filter { it.dateString == yStr && trackedKeys.contains(it.prayerKey.lowercase()) }
                if (yRecords.count { it.isCompleted } >= totalTrackedPerDay) {
                    streak++
                    calStreak.add(java.util.Calendar.DAY_OF_MONTH, -1)
                } else {
                    break
                }
            } else {
                break
            }
        }

        // On-Time Rate Percent
        val completedRecords = records.filter { it.isCompleted && trackedKeys.contains(it.prayerKey.lowercase()) }
        val onTimeCount = completedRecords.count { it.isOnTime }
        val onTimeRate = if (completedRecords.isNotEmpty()) (onTimeCount * 100 / completedRecords.size) else 100

        // Prayer Breakdown (% completion per prayer in last 30 days)
        val breakdown = mutableMapOf<String, Int>()
        trackedKeys.forEach { key ->
            val keyRecords = monthlyRecords.filter { it.prayerKey.equals(key, ignoreCase = true) }
            val completedForKey = keyRecords.count { it.isCompleted }
            val percent = (completedForKey * 100 / 30).coerceIn(0, 100)
            breakdown[key] = percent
        }

        // Best Day of the week
        val bestDay = weeklySummaries.maxByOrNull { it.completionCount }?.dayNameAr ?: "الجمعة"

        PrayerTrackerKPIs(
            dailyCompleted = dailyCompleted,
            dailyTotal = totalTrackedPerDay,
            weeklyCompleted = weeklyCompleted,
            weeklyTotal = if (weeklyTotal > 0) weeklyTotal else totalTrackedPerDay * 7,
            monthlyCompleted = monthlyCompleted,
            monthlyTotal = monthlyTotal,
            currentStreakDays = streak,
            onTimeRatePercent = onTimeRate,
            prayerBreakdown = breakdown,
            bestDayNameAr = bestDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrayerTrackerKPIs()
    )

    fun togglePrayerStatus(dateString: String, prayerKey: String, isOnTime: Boolean = true, context: Context? = null) {
        viewModelScope.launch {
            repository.togglePrayerRecord(dateString, prayerKey, isOnTime)
            if (context != null) {
                com.example.widget.PrayerTrackerWidgetProvider.sendUpdateBroadcast(context)
            }
        }
    }

    fun setPrayerStatus(dateString: String, prayerKey: String, isCompleted: Boolean, isOnTime: Boolean = true, context: Context? = null) {
        viewModelScope.launch {
            repository.setPrayerRecord(dateString, prayerKey, isCompleted, isOnTime)
            if (context != null) {
                com.example.widget.PrayerTrackerWidgetProvider.sendUpdateBroadcast(context)
            }
        }
    }

    fun updateTrackedPrayers(prayersList: List<String>, context: Context? = null) {
        viewModelScope.launch {
            val current = userSettings.value
            val json = prayersList.joinToString(",")
            repository.saveSettings(current.copy(trackedPrayersJson = json))
            if (context != null) {
                com.example.widget.PrayerTrackerWidgetProvider.sendUpdateBroadcast(context)
                com.example.notification.PrayerFollowUpScheduler.scheduleFollowUpNotifications(context)
            }
        }
    }

    fun updatePrayerFollowUpSettings(enabled: Boolean, delayMinutes: Int, context: Context? = null) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(
                current.copy(
                    enablePrayerFollowUpNotification = enabled,
                    prayerFollowUpDelayMinutes = delayMinutes
                )
            )
            if (context != null) {
                if (enabled) {
                    com.example.notification.PrayerFollowUpScheduler.scheduleFollowUpNotifications(context)
                } else {
                    com.example.notification.PrayerFollowUpScheduler.cancelAllFollowUps(context)
                }
            }
        }
    }
}

