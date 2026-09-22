package com.example.data.repository

import com.example.data.local.*
import com.example.data.provider.QuranDataProvider
import com.example.data.provider.QuranSearchIndexer
import com.example.data.provider.UniversalSearchResult
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class QuranRepository(private val quranDao: QuranDao) {

    // Bookmarks Flow
    val allBookmarks: Flow<List<Bookmark>> = quranDao.getAllBookmarks().map { list ->
        list.map {
            Bookmark(
                id = it.id,
                pageNumber = it.pageNumber,
                surahName = it.surahName,
                verseNumber = it.verseNumber,
                dateCreated = it.dateCreated,
                note = it.note
            )
        }
    }

    suspend fun addBookmark(pageNumber: Int, surahName: String, verseNumber: Int? = null, note: String = "") {
        quranDao.insertBookmark(
            BookmarkEntity(
                pageNumber = pageNumber,
                surahName = surahName,
                verseNumber = verseNumber,
                dateCreated = System.currentTimeMillis(),
                note = note
            )
        )
    }

    suspend fun updateBookmark(bookmark: Bookmark) {
        quranDao.updateBookmark(
            BookmarkEntity(
                id = bookmark.id,
                pageNumber = bookmark.pageNumber,
                surahName = bookmark.surahName,
                verseNumber = bookmark.verseNumber,
                dateCreated = bookmark.dateCreated,
                note = bookmark.note
            )
        )
    }

    suspend fun deleteBookmark(id: Long) {
        quranDao.deleteBookmarkById(id)
    }

    suspend fun isPageBookmarked(pageNumber: Int): Boolean {
        return quranDao.getBookmarkByPage(pageNumber) != null
    }

    // Reading Progress & History
    val readingProgress: Flow<ReadingProgress> = quranDao.getReadingProgress().map { entity ->
        if (entity != null) {
            ReadingProgress(
                lastPage = entity.lastPage,
                lastSurahName = entity.lastSurahName,
                lastVerseNumber = entity.lastVerseNumber,
                readingDurationMinutes = entity.readingDurationMinutes,
                lastReadTimestamp = entity.lastReadTimestamp
            )
        } else {
            ReadingProgress()
        }
    }

    val readingHistory: Flow<List<ReadingHistoryEntity>> = quranDao.getAllReadingHistory()

    suspend fun updateReadingProgress(pageNumber: Int, surahName: String, verseNumber: Int = 1) {
        val current = quranDao.getReadingProgress().firstOrNull()
        val existingDuration = current?.readingDurationMinutes ?: 0L
        quranDao.saveReadingProgress(
            ReadingProgressEntity(
                id = 1,
                lastPage = pageNumber,
                lastSurahName = surahName,
                lastVerseNumber = verseNumber,
                readingDurationMinutes = existingDuration,
                lastReadTimestamp = System.currentTimeMillis()
            )
        )

        // Find Surah details to record in history
        val surahList = QuranDataProvider.SURAHS
        val surah = surahList.firstOrNull { it.nameArabic == surahName }
            ?: surahList.lastOrNull { it.startPage <= pageNumber }
            ?: surahList.first()

        quranDao.insertReadingHistory(
            ReadingHistoryEntity(
                pageNumber = pageNumber,
                surahNumber = surah.id,
                surahNameArabic = surah.nameArabic,
                surahNameEnglish = surah.nameEnglish,
                verseNumber = verseNumber,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun addReadingDurationMinutes(addedMinutes: Long) {
        val current = quranDao.getReadingProgress().firstOrNull()
        val newDuration = (current?.readingDurationMinutes ?: 0L) + addedMinutes
        if (current != null) {
            quranDao.saveReadingProgress(
                current.copy(readingDurationMinutes = newDuration)
            )
        } else {
            quranDao.saveReadingProgress(
                ReadingProgressEntity(id = 1, readingDurationMinutes = newDuration)
            )
        }
    }

    suspend fun deleteReadingHistoryItem(id: Long) {
        quranDao.deleteReadingHistoryById(id)
    }

    suspend fun clearReadingHistory() {
        quranDao.clearAllReadingHistory()
    }

    // User Settings
    val userSettings: Flow<UserSettingsEntity> = quranDao.getUserSettings().map { entity ->
        entity ?: UserSettingsEntity()
    }

    suspend fun saveSettings(settings: UserSettingsEntity) {
        quranDao.saveUserSettings(settings)
    }

    // Export / Import Backup Data
    suspend fun exportBackupJson(): String {
        val json = org.json.JSONObject()

        // 1. Bookmarks
        val bookmarksList = quranDao.getAllBookmarksOnce()
        val bookmarksArray = org.json.JSONArray()
        bookmarksList.forEach { b ->
            val obj = org.json.JSONObject().apply {
                put("id", b.id)
                put("surahName", b.surahName)
                put("verseNumber", b.verseNumber ?: 1)
                put("pageNumber", b.pageNumber)
                put("note", b.note)
                put("dateCreated", b.dateCreated)
            }
            bookmarksArray.put(obj)
        }
        json.put("bookmarks", bookmarksArray)

        // 2. Reading Progress
        val progress = quranDao.getReadingProgressOnce()
        if (progress != null) {
            val progressObj = org.json.JSONObject().apply {
                put("lastPage", progress.lastPage)
                put("lastSurahName", progress.lastSurahName)
                put("lastVerseNumber", progress.lastVerseNumber)
                put("readingDurationMinutes", progress.readingDurationMinutes)
                put("lastReadTimestamp", progress.lastReadTimestamp)
            }
            json.put("readingProgress", progressObj)
        }

        // 3. User Settings
        val settings = quranDao.getUserSettingsOnce() ?: UserSettingsEntity()
        val settingsObj = org.json.JSONObject().apply {
            put("languageCode", settings.languageCode)
            put("themeMode", settings.themeMode)
            put("fontSizeSp", settings.fontSizeSp.toDouble())
            put("preferredReciterId", settings.preferredReciterId)
            put("calculationMethod", settings.calculationMethod)
            put("madhhab", settings.madhhab)
            put("isSummerTime", settings.isSummerTime)
            put("enabledPrayersJson", settings.enabledPrayersJson)
            put("silentPrayersJson", settings.silentPrayersJson)
            put("enableNightSchedule", settings.enableNightSchedule)
            put("selectedTafsirBook", settings.selectedTafsirBook)
            put("audioPlaybackSpeed", settings.audioPlaybackSpeed.toDouble())
            put("dailyPageGoal", settings.dailyPageGoal)
            put("khatmahTargetDays", settings.khatmahTargetDays)
            put("khatmahStartDate", settings.khatmahStartDate)
            put("homeGridOrderJson", settings.homeGridOrderJson)
            put("fajrOffsetMins", settings.fajrOffsetMins)
            put("dhuhrOffsetMins", settings.dhuhrOffsetMins)
            put("asrOffsetMins", settings.asrOffsetMins)
            put("maghribOffsetMins", settings.maghribOffsetMins)
            put("ishaOffsetMins", settings.ishaOffsetMins)
            put("quranScriptType", settings.quranScriptType)
        }
        json.put("userSettings", settingsObj)

        // 4. Reading History
        val historyList = quranDao.getAllReadingHistoryOnce()
        val historyArray = org.json.JSONArray()
        historyList.forEach { h ->
            val obj = org.json.JSONObject().apply {
                put("id", h.id)
                put("pageNumber", h.pageNumber)
                put("surahNumber", h.surahNumber)
                put("surahNameArabic", h.surahNameArabic)
                put("surahNameEnglish", h.surahNameEnglish)
                put("verseNumber", h.verseNumber)
                put("timestamp", h.timestamp)
            }
            historyArray.put(obj)
        }
        json.put("readingHistory", historyArray)

        // 5. Favorite Surahs
        val favList = quranDao.getFavoriteSurahIdsOnce()
        val favArray = org.json.JSONArray()
        favList.forEach { id -> favArray.put(id) }
        json.put("favoriteSurahs", favArray)

        return json.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonString)

            // Restore Bookmarks
            if (json.has("bookmarks")) {
                val array = json.getJSONArray("bookmarks")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val bookmark = BookmarkEntity(
                        id = obj.optLong("id", 0L),
                        surahName = obj.optString("surahName", ""),
                        verseNumber = obj.optInt("verseNumber", 1),
                        pageNumber = obj.optInt("pageNumber", 1),
                        note = obj.optString("note", ""),
                        dateCreated = obj.optLong("dateCreated", System.currentTimeMillis())
                    )
                    quranDao.insertBookmark(bookmark)
                }
            }

            // Restore Reading Progress
            if (json.has("readingProgress")) {
                val obj = json.getJSONObject("readingProgress")
                val progress = ReadingProgressEntity(
                    id = 1,
                    lastPage = obj.optInt("lastPage", 1),
                    lastSurahName = obj.optString("lastSurahName", "الفاتحة"),
                    lastVerseNumber = obj.optInt("lastVerseNumber", 1),
                    readingDurationMinutes = obj.optLong("readingDurationMinutes", 0L),
                    lastReadTimestamp = obj.optLong("lastReadTimestamp", System.currentTimeMillis())
                )
                quranDao.saveReadingProgress(progress)
            }

            // Restore Settings
            if (json.has("userSettings")) {
                val obj = json.getJSONObject("userSettings")
                val current = quranDao.getUserSettingsOnce() ?: UserSettingsEntity()
                val settings = current.copy(
                    languageCode = obj.optString("languageCode", current.languageCode),
                    themeMode = obj.optString("themeMode", current.themeMode),
                    fontSizeSp = obj.optDouble("fontSizeSp", current.fontSizeSp.toDouble()).toFloat(),
                    preferredReciterId = obj.optString("preferredReciterId", current.preferredReciterId),
                    calculationMethod = obj.optString("calculationMethod", current.calculationMethod),
                    madhhab = obj.optString("madhhab", current.madhhab),
                    isSummerTime = obj.optBoolean("isSummerTime", current.isSummerTime),
                    enabledPrayersJson = obj.optString("enabledPrayersJson", current.enabledPrayersJson),
                    silentPrayersJson = obj.optString("silentPrayersJson", current.silentPrayersJson),
                    enableNightSchedule = obj.optBoolean("enableNightSchedule", current.enableNightSchedule),
                    selectedTafsirBook = obj.optString("selectedTafsirBook", current.selectedTafsirBook),
                    audioPlaybackSpeed = obj.optDouble("audioPlaybackSpeed", current.audioPlaybackSpeed.toDouble()).toFloat(),
                    dailyPageGoal = obj.optInt("dailyPageGoal", current.dailyPageGoal),
                    khatmahTargetDays = obj.optInt("khatmahTargetDays", current.khatmahTargetDays),
                    khatmahStartDate = obj.optLong("khatmahStartDate", current.khatmahStartDate),
                    homeGridOrderJson = obj.optString("homeGridOrderJson", current.homeGridOrderJson),
                    fajrOffsetMins = obj.optInt("fajrOffsetMins", current.fajrOffsetMins),
                    dhuhrOffsetMins = obj.optInt("dhuhrOffsetMins", current.dhuhrOffsetMins),
                    asrOffsetMins = obj.optInt("asrOffsetMins", current.asrOffsetMins),
                    maghribOffsetMins = obj.optInt("maghribOffsetMins", current.maghribOffsetMins),
                    ishaOffsetMins = obj.optInt("ishaOffsetMins", current.ishaOffsetMins),
                    quranScriptType = obj.optString("quranScriptType", current.quranScriptType)
                )
                quranDao.saveUserSettings(settings)
            }

            // Restore Reading History
            if (json.has("readingHistory")) {
                val array = json.getJSONArray("readingHistory")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val history = ReadingHistoryEntity(
                        id = obj.optLong("id", 0L),
                        pageNumber = obj.optInt("pageNumber", 1),
                        surahNumber = obj.optInt("surahNumber", 1),
                        surahNameArabic = obj.optString("surahNameArabic", ""),
                        surahNameEnglish = obj.optString("surahNameEnglish", ""),
                        verseNumber = obj.optInt("verseNumber", 1),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    quranDao.insertReadingHistory(history)
                }
            }

            // Restore Favorite Surahs
            if (json.has("favoriteSurahs")) {
                val array = json.getJSONArray("favoriteSurahs")
                for (i in 0 until array.length()) {
                    val surahId = array.getInt(i)
                    quranDao.addFavoriteSurah(FavoriteSurahEntity(surahId))
                }
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("QuranRepository", "Error importing backup json", e)
            false
        }
    }

    // Favorites
    val favoriteSurahIds: Flow<List<Int>> = quranDao.getFavoriteSurahIds()

    suspend fun toggleFavoriteSurah(surahId: Int, isFavorite: Boolean) {
        if (isFavorite) {
            quranDao.addFavoriteSurah(FavoriteSurahEntity(surahId))
        } else {
            quranDao.removeFavoriteSurah(surahId)
        }
    }

    val favoriteAzkarIds: Flow<List<Int>> = quranDao.getFavoriteAzkarIds()

    suspend fun toggleFavoriteAzkar(azkarId: Int, isFavorite: Boolean) {
        if (isFavorite) {
            quranDao.addFavoriteAzkar(FavoriteAzkarEntity(azkarId))
        } else {
            quranDao.removeFavoriteAzkar(azkarId)
        }
    }

    // Static / Provider Data
    fun getSurahs(): List<Surah> = QuranDataProvider.SURAHS

    fun getJuzList(): List<Juz> = QuranDataProvider.JUZ_LIST

    fun getVersesForPage(pageNumber: Int): List<Verse> = QuranDataProvider.getVersesForPage(pageNumber)

    fun getDailyVerse(): DailyVerse = QuranDataProvider.getDailyVerseForToday()

    fun getHadiths(): List<HadithItem> = com.example.data.provider.HadithDataProvider.HADITHS_500

    fun getReciters(): List<Reciter> = QuranDataProvider.RECITERS

    fun getPrayerTimes(
        lat: Double = 30.0444,
        lng: Double = 31.2357,
        method: String = "EGYPT",
        madhhab: String = "Shafi",
        isSummerTime: Boolean = false,
        fajrOffset: Int = 0,
        dhuhrOffset: Int = 0,
        asrOffset: Int = 0,
        maghribOffset: Int = 0,
        ishaOffset: Int = 0
    ): List<PrayerTime> =
        QuranDataProvider.calculatePrayerTimes(
            lat = lat,
            lng = lng,
            method = method,
            isSummerTime = isSummerTime,
            madhhab = madhhab,
            fajrOffset = fajrOffset,
            dhuhrOffset = dhuhrOffset,
            asrOffset = asrOffset,
            maghribOffset = maghribOffset,
            ishaOffset = ishaOffset
        )

    fun getQiblaInfo(lat: Double = 24.7136, lng: Double = 46.6753): Pair<Double, Double> =
        QuranDataProvider.calculateQibla(lat, lng)

    fun searchQuran(query: String): List<Surah> {
        return universalSearch(query).surahResults.map { it.surah }
    }

    fun universalSearch(query: String): UniversalSearchResult {
        return QuranSearchIndexer.search(query)
    }

    // Offline Quranic Text & Metadata Room Storage
    val allSurahMetadataFromDb: Flow<List<SurahMetadataEntity>> = quranDao.getAllSurahMetadata()

    fun getVersesByPageFromDb(pageNumber: Int): Flow<List<QuranVerseEntity>> = quranDao.getVersesByPage(pageNumber)

    fun getVersesBySurahFromDb(surahNumber: Int): Flow<List<QuranVerseEntity>> = quranDao.getVersesBySurah(surahNumber)

    suspend fun saveSurahMetadataToDb(surahs: List<SurahMetadataEntity>) {
        quranDao.insertSurahMetadata(surahs)
    }

    suspend fun saveVersesToDb(verses: List<QuranVerseEntity>) {
        quranDao.insertVerses(verses)
    }

    // Prayer Tracker Repository Methods
    fun getPrayerRecordsForDate(dateString: String): Flow<List<PrayerRecordEntity>> {
        return quranDao.getPrayerRecordsForDate(dateString)
    }

    suspend fun getPrayerRecordsForDateOnce(dateString: String): List<PrayerRecordEntity> {
        return quranDao.getPrayerRecordsForDateOnce(dateString)
    }

    fun getPrayerRecordsBetween(startDate: String, endDate: String): Flow<List<PrayerRecordEntity>> {
        return quranDao.getPrayerRecordsBetween(startDate, endDate)
    }

    fun getAllPrayerRecords(): Flow<List<PrayerRecordEntity>> {
        return quranDao.getAllPrayerRecords()
    }

    suspend fun togglePrayerRecord(dateString: String, prayerKey: String, isOnTime: Boolean = true) {
        val existing = quranDao.getPrayerRecord(dateString, prayerKey)
        val newStatus = !(existing?.isCompleted ?: false)
        val record = PrayerRecordEntity(
            id = existing?.id ?: 0,
            dateString = dateString,
            prayerKey = prayerKey,
            isCompleted = newStatus,
            isOnTime = if (newStatus) isOnTime else true,
            completedTimestamp = if (newStatus) System.currentTimeMillis() else null
        )
        quranDao.insertPrayerRecord(record)
    }

    suspend fun setPrayerRecord(dateString: String, prayerKey: String, isCompleted: Boolean, isOnTime: Boolean = true) {
        val existing = quranDao.getPrayerRecord(dateString, prayerKey)
        val record = PrayerRecordEntity(
            id = existing?.id ?: 0,
            dateString = dateString,
            prayerKey = prayerKey,
            isCompleted = isCompleted,
            isOnTime = isOnTime,
            completedTimestamp = if (isCompleted) System.currentTimeMillis() else null
        )
        quranDao.insertPrayerRecord(record)
    }
}
