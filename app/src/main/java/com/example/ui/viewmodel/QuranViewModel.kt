package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.QuranDatabase
import com.example.data.local.UserSettingsEntity
import com.example.data.repository.QuranRepository
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.data.audio.AyahRepeatMode
import com.example.data.audio.QuranAudioPlayer

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val db = QuranDatabase.getDatabase(application)
    private val repository = QuranRepository(db.quranDao())
    private val audioPlayer = QuranAudioPlayer(application)

    val isPlayingAudio: StateFlow<Boolean> = audioPlayer.isPlaying
    val isBufferingAudio: StateFlow<Boolean> = audioPlayer.isBuffering
    val currentPlayingVerse: StateFlow<Pair<Int, Int>?> = audioPlayer.currentPlayingVerse
    val currentPlayingSurah: StateFlow<Int?> = audioPlayer.currentPlayingSurah
    val repeatMode: StateFlow<AyahRepeatMode> = audioPlayer.repeatMode

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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = try { repository.universalSearch("") } catch (e: Throwable) { com.example.data.provider.UniversalSearchResult("", 0, emptyList(), emptyList(), emptyList()) }
        )

    val filteredSurahs: StateFlow<List<Surah>> = combine(
        _searchQuery,
        favoriteSurahIds
    ) { query, favIds ->
        try {
            val list = repository.searchQuran(query)
            list.map { surah ->
                surah.copy(isFavorite = favIds.contains(surah.id))
            }
        } catch (e: Throwable) {
            android.util.Log.e("QuranViewModel", "Error filtering surahs", e)
            emptyList()
        }
    }.stateIn(
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
        if (isPlayingAudio.value) {
            audioPlayer.togglePlayPause()
        } else {
            val surah = repository.getSurahs().lastOrNull { it.startPage <= _currentPage.value } ?: repository.getSurahs().first()
            audioPlayer.playSurahAudio(surah.id, _selectedReciter.value.id)
        }
    }

    fun playSpecificSurahAudio(surahId: Int) {
        audioPlayer.playSurahAudio(surahId, _selectedReciter.value.id)
    }

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

    fun updateKhatmahTargetDays(days: Int) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(khatmahTargetDays = days.coerceAtLeast(1)))
        }
    }

    fun resetKhatmahProgress() {
        viewModelScope.launch {
            repository.updateReadingProgress(1, "الفاتحة", 1)
            val current = userSettings.value
            repository.saveSettings(current.copy(khatmahStartDate = System.currentTimeMillis()))
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

