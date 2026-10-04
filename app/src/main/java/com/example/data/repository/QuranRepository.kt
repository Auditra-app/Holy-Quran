package com.example.data.repository

import com.example.data.local.*
import com.example.data.provider.QuranDataProvider
import com.example.data.provider.QuranSearchIndexer
import com.example.data.provider.UniversalSearchResult
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class QuranRepository(private val quranDao: QuranDao) {

    // Bookmarks Flow
    val allBookmarks: Flow<List<Bookmark>> = quranDao.getAllBookmarks()
        .map { list ->
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
        .catch { e ->
            android.util.Log.e("QuranRepository", "Error in allBookmarks flow", e)
            emit(emptyList())
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
    val readingProgress: Flow<ReadingProgress> = quranDao.getReadingProgress()
        .map { entity ->
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
        .catch { e ->
            android.util.Log.e("QuranRepository", "Error in readingProgress flow", e)
            emit(ReadingProgress())
        }

    val readingHistory: Flow<List<ReadingHistoryEntity>> = quranDao.getAllReadingHistory()
        .catch { e ->
            android.util.Log.e("QuranRepository", "Error in readingHistory flow", e)
            emit(emptyList())
        }

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

    // User Settings with Permanent Dual-Persistence Backup across App Updates
    val userSettings: Flow<UserSettingsEntity> = quranDao.getUserSettings()
        .map { entity ->
            if (entity != null) {
                backupSettingsToPrefs(entity)
                entity
            } else {
                val restored = restoreSettingsFromPrefs()
                if (restored != null) {
                    try { quranDao.saveUserSettings(restored) } catch (_: Throwable) {}
                    restored
                } else {
                    UserSettingsEntity()
                }
            }
        }
        .catch { e ->
            android.util.Log.e("QuranRepository", "Error in userSettings flow, restoring backup", e)
            emit(restoreSettingsFromPrefs() ?: UserSettingsEntity())
        }

    suspend fun saveSettings(settings: UserSettingsEntity) {
        quranDao.saveUserSettings(settings)
        backupSettingsToPrefs(settings)
    }

    private fun backupSettingsToPrefs(settings: UserSettingsEntity) {
        try {
            val ctx = com.example.HolyQuranApplication.instance ?: return
            val prefs = ctx.getSharedPreferences("quran_settings_permanent_backup", android.content.Context.MODE_PRIVATE)
            val json = org.json.JSONObject().apply {
                put("languageCode", settings.languageCode)
                put("themeMode", settings.themeMode)
                put("fontSizeSp", settings.fontSizeSp.toDouble())
                put("enableDailyVerseNotification", settings.enableDailyVerseNotification)
                put("enableReadingReminderNotification", settings.enableReadingReminderNotification)
                put("enableInactivityNotification", settings.enableInactivityNotification)
                put("enableAzkarNotification", settings.enableAzkarNotification)
                put("enableMorningAzkar", settings.enableMorningAzkar)
                put("enableEveningAzkar", settings.enableEveningAzkar)
                put("morningAzkarHour", settings.morningAzkarHour)
                put("morningAzkarMinute", settings.morningAzkarMinute)
                put("eveningAzkarHour", settings.eveningAzkarHour)
                put("eveningAzkarMinute", settings.eveningAzkarMinute)
                put("notificationTimeHour", settings.notificationTimeHour)
                put("notificationTimeMinute", settings.notificationTimeMinute)
                put("calculationMethod", settings.calculationMethod)
                put("madhhab", settings.madhhab)
                put("preferredReciterId", settings.preferredReciterId)
                put("showEnglishTranslation", settings.showEnglishTranslation)
                put("enableTajweed", settings.enableTajweed)
                put("keepScreenOn", settings.keepScreenOn)
                put("nightModeTheme", settings.nightModeTheme)
                put("enableNightSchedule", settings.enableNightSchedule)
                put("nightScheduleStartHour", settings.nightScheduleStartHour)
                put("nightScheduleStartMinute", settings.nightScheduleStartMinute)
                put("nightScheduleEndHour", settings.nightScheduleEndHour)
                put("nightScheduleEndMinute", settings.nightScheduleEndMinute)
                put("isSummerTime", settings.isSummerTime)
                put("takbeerAzanOnly", settings.takbeerAzanOnly)
                put("selectedAzanSound", settings.selectedAzanSound)
                put("enabledPrayersJson", settings.enabledPrayersJson)
                put("silentPrayersJson", settings.silentPrayersJson)
                put("selectedTafsirBook", settings.selectedTafsirBook)
                put("audioPlaybackSpeed", settings.audioPlaybackSpeed.toDouble())
                put("dailyPageGoal", settings.dailyPageGoal)
                put("khatmahTargetDays", settings.khatmahTargetDays)
                put("khatmahStartDate", settings.khatmahStartDate)
                put("khatmahCurrentPage", settings.khatmahCurrentPage)
                put("khatmahDailyGoalPages", settings.khatmahDailyGoalPages)
                put("khatmahCompletedWirdsCount", settings.khatmahCompletedWirdsCount)
                put("khatmahLastCompletedDate", settings.khatmahLastCompletedDate)
                put("homeGridOrderJson", settings.homeGridOrderJson)
                put("enableHaptic", settings.enableHaptic)
                put("hapticType", settings.hapticType)
                put("fajrOffsetMins", settings.fajrOffsetMins)
                put("dhuhrOffsetMins", settings.dhuhrOffsetMins)
                put("asrOffsetMins", settings.asrOffsetMins)
                put("maghribOffsetMins", settings.maghribOffsetMins)
                put("ishaOffsetMins", settings.ishaOffsetMins)
                put("quranScriptType", settings.quranScriptType)
                put("trackedPrayersJson", settings.trackedPrayersJson)
                put("enablePrayerFollowUpNotification", settings.enablePrayerFollowUpNotification)
                put("prayerFollowUpDelayMinutes", settings.prayerFollowUpDelayMinutes)
                put("enableAzanOnSilent", settings.enableAzanOnSilent)
            }
            prefs.edit().putString("settings_json", json.toString()).apply()
        } catch (e: Throwable) {
            android.util.Log.e("QuranRepository", "Error backing up settings to prefs", e)
        }
    }

    private fun restoreSettingsFromPrefs(): UserSettingsEntity? {
        try {
            val ctx = com.example.HolyQuranApplication.instance ?: return null
            val prefs = ctx.getSharedPreferences("quran_settings_permanent_backup", android.content.Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("settings_json", null) ?: return null
            val obj = org.json.JSONObject(jsonStr)
            val def = UserSettingsEntity()
            return UserSettingsEntity(
                id = 1,
                languageCode = obj.optString("languageCode", def.languageCode),
                themeMode = obj.optString("themeMode", def.themeMode),
                fontSizeSp = obj.optDouble("fontSizeSp", def.fontSizeSp.toDouble()).toFloat(),
                enableDailyVerseNotification = obj.optBoolean("enableDailyVerseNotification", def.enableDailyVerseNotification),
                enableReadingReminderNotification = obj.optBoolean("enableReadingReminderNotification", def.enableReadingReminderNotification),
                enableInactivityNotification = obj.optBoolean("enableInactivityNotification", def.enableInactivityNotification),
                enableAzkarNotification = obj.optBoolean("enableAzkarNotification", def.enableAzkarNotification),
                enableMorningAzkar = obj.optBoolean("enableMorningAzkar", def.enableMorningAzkar),
                enableEveningAzkar = obj.optBoolean("enableEveningAzkar", def.enableEveningAzkar),
                morningAzkarHour = obj.optInt("morningAzkarHour", def.morningAzkarHour),
                morningAzkarMinute = obj.optInt("morningAzkarMinute", def.morningAzkarMinute),
                eveningAzkarHour = obj.optInt("eveningAzkarHour", def.eveningAzkarHour),
                eveningAzkarMinute = obj.optInt("eveningAzkarMinute", def.eveningAzkarMinute),
                notificationTimeHour = obj.optInt("notificationTimeHour", def.notificationTimeHour),
                notificationTimeMinute = obj.optInt("notificationTimeMinute", def.notificationTimeMinute),
                calculationMethod = obj.optString("calculationMethod", def.calculationMethod),
                madhhab = obj.optString("madhhab", def.madhhab),
                preferredReciterId = obj.optString("preferredReciterId", def.preferredReciterId),
                showEnglishTranslation = obj.optBoolean("showEnglishTranslation", def.showEnglishTranslation),
                enableTajweed = obj.optBoolean("enableTajweed", def.enableTajweed),
                keepScreenOn = obj.optBoolean("keepScreenOn", def.keepScreenOn),
                nightModeTheme = obj.optString("nightModeTheme", def.nightModeTheme),
                enableNightSchedule = obj.optBoolean("enableNightSchedule", def.enableNightSchedule),
                nightScheduleStartHour = obj.optInt("nightScheduleStartHour", def.nightScheduleStartHour),
                nightScheduleStartMinute = obj.optInt("nightScheduleStartMinute", def.nightScheduleStartMinute),
                nightScheduleEndHour = obj.optInt("nightScheduleEndHour", def.nightScheduleEndHour),
                nightScheduleEndMinute = obj.optInt("nightScheduleEndMinute", def.nightScheduleEndMinute),
                isSummerTime = obj.optBoolean("isSummerTime", def.isSummerTime),
                takbeerAzanOnly = obj.optBoolean("takbeerAzanOnly", def.takbeerAzanOnly),
                selectedAzanSound = obj.optString("selectedAzanSound", def.selectedAzanSound),
                enabledPrayersJson = obj.optString("enabledPrayersJson", def.enabledPrayersJson),
                silentPrayersJson = obj.optString("silentPrayersJson", def.silentPrayersJson),
                selectedTafsirBook = obj.optString("selectedTafsirBook", def.selectedTafsirBook),
                audioPlaybackSpeed = obj.optDouble("audioPlaybackSpeed", def.audioPlaybackSpeed.toDouble()).toFloat(),
                dailyPageGoal = obj.optInt("dailyPageGoal", def.dailyPageGoal),
                khatmahTargetDays = obj.optInt("khatmahTargetDays", def.khatmahTargetDays),
                khatmahStartDate = obj.optLong("khatmahStartDate", def.khatmahStartDate),
                khatmahCurrentPage = obj.optInt("khatmahCurrentPage", def.khatmahCurrentPage),
                khatmahDailyGoalPages = obj.optInt("khatmahDailyGoalPages", def.khatmahDailyGoalPages),
                khatmahCompletedWirdsCount = obj.optInt("khatmahCompletedWirdsCount", def.khatmahCompletedWirdsCount),
                khatmahLastCompletedDate = obj.optLong("khatmahLastCompletedDate", def.khatmahLastCompletedDate),
                homeGridOrderJson = obj.optString("homeGridOrderJson", def.homeGridOrderJson),
                enableHaptic = obj.optBoolean("enableHaptic", def.enableHaptic),
                hapticType = obj.optString("hapticType", def.hapticType),
                fajrOffsetMins = obj.optInt("fajrOffsetMins", def.fajrOffsetMins),
                dhuhrOffsetMins = obj.optInt("dhuhrOffsetMins", def.dhuhrOffsetMins),
                asrOffsetMins = obj.optInt("asrOffsetMins", def.asrOffsetMins),
                maghribOffsetMins = obj.optInt("maghribOffsetMins", def.maghribOffsetMins),
                ishaOffsetMins = obj.optInt("ishaOffsetMins", def.ishaOffsetMins),
                quranScriptType = obj.optString("quranScriptType", def.quranScriptType),
                trackedPrayersJson = obj.optString("trackedPrayersJson", def.trackedPrayersJson),
                enablePrayerFollowUpNotification = obj.optBoolean("enablePrayerFollowUpNotification", def.enablePrayerFollowUpNotification),
                prayerFollowUpDelayMinutes = obj.optInt("prayerFollowUpDelayMinutes", def.prayerFollowUpDelayMinutes),
                enableAzanOnSilent = obj.optBoolean("enableAzanOnSilent", def.enableAzanOnSilent)
            )
        } catch (e: Throwable) {
            android.util.Log.e("QuranRepository", "Error restoring settings from prefs", e)
            return null
        }
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

        // 3. User Settings (Complete app settings)
        val settings = quranDao.getUserSettingsOnce() ?: restoreSettingsFromPrefs() ?: UserSettingsEntity()
        val settingsObj = org.json.JSONObject().apply {
            put("languageCode", settings.languageCode)
            put("themeMode", settings.themeMode)
            put("fontSizeSp", settings.fontSizeSp.toDouble())
            put("enableDailyVerseNotification", settings.enableDailyVerseNotification)
            put("enableReadingReminderNotification", settings.enableReadingReminderNotification)
            put("enableInactivityNotification", settings.enableInactivityNotification)
            put("enableAzkarNotification", settings.enableAzkarNotification)
            put("enableMorningAzkar", settings.enableMorningAzkar)
            put("enableEveningAzkar", settings.enableEveningAzkar)
            put("morningAzkarHour", settings.morningAzkarHour)
            put("morningAzkarMinute", settings.morningAzkarMinute)
            put("eveningAzkarHour", settings.eveningAzkarHour)
            put("eveningAzkarMinute", settings.eveningAzkarMinute)
            put("notificationTimeHour", settings.notificationTimeHour)
            put("notificationTimeMinute", settings.notificationTimeMinute)
            put("calculationMethod", settings.calculationMethod)
            put("madhhab", settings.madhhab)
            put("preferredReciterId", settings.preferredReciterId)
            put("showEnglishTranslation", settings.showEnglishTranslation)
            put("enableTajweed", settings.enableTajweed)
            put("keepScreenOn", settings.keepScreenOn)
            put("nightModeTheme", settings.nightModeTheme)
            put("enableNightSchedule", settings.enableNightSchedule)
            put("nightScheduleStartHour", settings.nightScheduleStartHour)
            put("nightScheduleStartMinute", settings.nightScheduleStartMinute)
            put("nightScheduleEndHour", settings.nightScheduleEndHour)
            put("nightScheduleEndMinute", settings.nightScheduleEndMinute)
            put("isSummerTime", settings.isSummerTime)
            put("takbeerAzanOnly", settings.takbeerAzanOnly)
            put("selectedAzanSound", settings.selectedAzanSound)
            put("enabledPrayersJson", settings.enabledPrayersJson)
            put("silentPrayersJson", settings.silentPrayersJson)
            put("selectedTafsirBook", settings.selectedTafsirBook)
            put("audioPlaybackSpeed", settings.audioPlaybackSpeed.toDouble())
            put("dailyPageGoal", settings.dailyPageGoal)
            put("khatmahTargetDays", settings.khatmahTargetDays)
            put("khatmahStartDate", settings.khatmahStartDate)
            put("khatmahCurrentPage", settings.khatmahCurrentPage)
            put("khatmahDailyGoalPages", settings.khatmahDailyGoalPages)
            put("khatmahCompletedWirdsCount", settings.khatmahCompletedWirdsCount)
            put("khatmahLastCompletedDate", settings.khatmahLastCompletedDate)
            put("homeGridOrderJson", settings.homeGridOrderJson)
            put("enableHaptic", settings.enableHaptic)
            put("hapticType", settings.hapticType)
            put("fajrOffsetMins", settings.fajrOffsetMins)
            put("dhuhrOffsetMins", settings.dhuhrOffsetMins)
            put("asrOffsetMins", settings.asrOffsetMins)
            put("maghribOffsetMins", settings.maghribOffsetMins)
            put("ishaOffsetMins", settings.ishaOffsetMins)
            put("quranScriptType", settings.quranScriptType)
            put("trackedPrayersJson", settings.trackedPrayersJson)
            put("enablePrayerFollowUpNotification", settings.enablePrayerFollowUpNotification)
            put("prayerFollowUpDelayMinutes", settings.prayerFollowUpDelayMinutes)
            put("enableAzanOnSilent", settings.enableAzanOnSilent)
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

        // 6. Favorite Azkar
        val favAzkarList = quranDao.getFavoriteAzkarIdsOnce()
        val favAzkarArray = org.json.JSONArray()
        favAzkarList.forEach { id -> favAzkarArray.put(id) }
        json.put("favoriteAzkar", favAzkarArray)

        // 7. Prayer Records
        val prayerRecords = quranDao.getAllPrayerRecordsOnce()
        val prayerArray = org.json.JSONArray()
        prayerRecords.forEach { pr ->
            val obj = org.json.JSONObject().apply {
                put("dateString", pr.dateString)
                put("prayerKey", pr.prayerKey)
                put("isCompleted", pr.isCompleted)
                put("isOnTime", pr.isOnTime)
                if (pr.completedTimestamp != null) {
                    put("completedTimestamp", pr.completedTimestamp)
                }
            }
            prayerArray.put(obj)
        }
        json.put("prayerRecords", prayerArray)

        // 8. Location and Prayer Calculation Preferences
        val ctx = com.example.HolyQuranApplication.instance
        if (ctx != null) {
            try {
                val locData = com.example.util.LocationHelper.getSavedLocationAndSettings(ctx)
                val locObj = org.json.JSONObject().apply {
                    put("latitude", locData.latitude)
                    put("longitude", locData.longitude)
                    put("cityName", locData.cityName)
                    put("calculationMethod", locData.calculationMethod)
                    put("isSummerTime", locData.isSummerTime)
                    put("fajrOffset", locData.fajrOffset)
                    put("dhuhrOffset", locData.dhuhrOffset)
                    put("asrOffset", locData.asrOffset)
                    put("maghribOffset", locData.maghribOffset)
                    put("ishaOffset", locData.ishaOffset)
                }
                json.put("locationSettings", locObj)
            } catch (e: Throwable) {
                android.util.Log.w("QuranRepository", "Error backing up location settings", e)
            }

            // 9. Group Khatmas Data
            try {
                val groupKhatmaPrefs = ctx.getSharedPreferences("group_khatma_prefs", android.content.Context.MODE_PRIVATE)
                val khatmasStr = groupKhatmaPrefs.getString("khatmas_list_json", null)
                if (!khatmasStr.isNullOrBlank()) {
                    json.put("groupKhatmasRaw", khatmasStr)
                }
                val khatmaUser = groupKhatmaPrefs.getString("saved_user_name", null)
                if (!khatmaUser.isNullOrBlank()) {
                    json.put("groupKhatmaUserName", khatmaUser)
                }
            } catch (e: Throwable) {
                android.util.Log.w("QuranRepository", "Error backing up group khatma data", e)
            }
        }

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

            // Restore Settings (Comprehensive)
            if (json.has("userSettings")) {
                val obj = json.getJSONObject("userSettings")
                val current = quranDao.getUserSettingsOnce() ?: restoreSettingsFromPrefs() ?: UserSettingsEntity()
                val settings = UserSettingsEntity(
                    id = 1,
                    languageCode = obj.optString("languageCode", current.languageCode),
                    themeMode = obj.optString("themeMode", current.themeMode),
                    fontSizeSp = obj.optDouble("fontSizeSp", current.fontSizeSp.toDouble()).toFloat(),
                    enableDailyVerseNotification = obj.optBoolean("enableDailyVerseNotification", current.enableDailyVerseNotification),
                    enableReadingReminderNotification = obj.optBoolean("enableReadingReminderNotification", current.enableReadingReminderNotification),
                    enableInactivityNotification = obj.optBoolean("enableInactivityNotification", current.enableInactivityNotification),
                    enableAzkarNotification = obj.optBoolean("enableAzkarNotification", current.enableAzkarNotification),
                    enableMorningAzkar = obj.optBoolean("enableMorningAzkar", current.enableMorningAzkar),
                    enableEveningAzkar = obj.optBoolean("enableEveningAzkar", current.enableEveningAzkar),
                    morningAzkarHour = obj.optInt("morningAzkarHour", current.morningAzkarHour),
                    morningAzkarMinute = obj.optInt("morningAzkarMinute", current.morningAzkarMinute),
                    eveningAzkarHour = obj.optInt("eveningAzkarHour", current.eveningAzkarHour),
                    eveningAzkarMinute = obj.optInt("eveningAzkarMinute", current.eveningAzkarMinute),
                    notificationTimeHour = obj.optInt("notificationTimeHour", current.notificationTimeHour),
                    notificationTimeMinute = obj.optInt("notificationTimeMinute", current.notificationTimeMinute),
                    calculationMethod = obj.optString("calculationMethod", current.calculationMethod),
                    madhhab = obj.optString("madhhab", current.madhhab),
                    preferredReciterId = obj.optString("preferredReciterId", current.preferredReciterId),
                    showEnglishTranslation = obj.optBoolean("showEnglishTranslation", current.showEnglishTranslation),
                    enableTajweed = obj.optBoolean("enableTajweed", current.enableTajweed),
                    keepScreenOn = obj.optBoolean("keepScreenOn", current.keepScreenOn),
                    nightModeTheme = obj.optString("nightModeTheme", current.nightModeTheme),
                    enableNightSchedule = obj.optBoolean("enableNightSchedule", current.enableNightSchedule),
                    nightScheduleStartHour = obj.optInt("nightScheduleStartHour", current.nightScheduleStartHour),
                    nightScheduleStartMinute = obj.optInt("nightScheduleStartMinute", current.nightScheduleStartMinute),
                    nightScheduleEndHour = obj.optInt("nightScheduleEndHour", current.nightScheduleEndHour),
                    nightScheduleEndMinute = obj.optInt("nightScheduleEndMinute", current.nightScheduleEndMinute),
                    isSummerTime = obj.optBoolean("isSummerTime", current.isSummerTime),
                    takbeerAzanOnly = obj.optBoolean("takbeerAzanOnly", current.takbeerAzanOnly),
                    selectedAzanSound = obj.optString("selectedAzanSound", current.selectedAzanSound),
                    enabledPrayersJson = obj.optString("enabledPrayersJson", current.enabledPrayersJson),
                    silentPrayersJson = obj.optString("silentPrayersJson", current.silentPrayersJson),
                    selectedTafsirBook = obj.optString("selectedTafsirBook", current.selectedTafsirBook),
                    audioPlaybackSpeed = obj.optDouble("audioPlaybackSpeed", current.audioPlaybackSpeed.toDouble()).toFloat(),
                    dailyPageGoal = obj.optInt("dailyPageGoal", current.dailyPageGoal),
                    khatmahTargetDays = obj.optInt("khatmahTargetDays", current.khatmahTargetDays),
                    khatmahStartDate = obj.optLong("khatmahStartDate", current.khatmahStartDate),
                    khatmahCurrentPage = obj.optInt("khatmahCurrentPage", current.khatmahCurrentPage),
                    khatmahDailyGoalPages = obj.optInt("khatmahDailyGoalPages", current.khatmahDailyGoalPages),
                    khatmahCompletedWirdsCount = obj.optInt("khatmahCompletedWirdsCount", current.khatmahCompletedWirdsCount),
                    khatmahLastCompletedDate = obj.optLong("khatmahLastCompletedDate", current.khatmahLastCompletedDate),
                    homeGridOrderJson = obj.optString("homeGridOrderJson", current.homeGridOrderJson),
                    enableHaptic = obj.optBoolean("enableHaptic", current.enableHaptic),
                    hapticType = obj.optString("hapticType", current.hapticType),
                    fajrOffsetMins = obj.optInt("fajrOffsetMins", current.fajrOffsetMins),
                    dhuhrOffsetMins = obj.optInt("dhuhrOffsetMins", current.dhuhrOffsetMins),
                    asrOffsetMins = obj.optInt("asrOffsetMins", current.asrOffsetMins),
                    maghribOffsetMins = obj.optInt("maghribOffsetMins", current.maghribOffsetMins),
                    ishaOffsetMins = obj.optInt("ishaOffsetMins", current.ishaOffsetMins),
                    quranScriptType = obj.optString("quranScriptType", current.quranScriptType),
                    trackedPrayersJson = obj.optString("trackedPrayersJson", current.trackedPrayersJson),
                    enablePrayerFollowUpNotification = obj.optBoolean("enablePrayerFollowUpNotification", current.enablePrayerFollowUpNotification),
                    prayerFollowUpDelayMinutes = obj.optInt("prayerFollowUpDelayMinutes", current.prayerFollowUpDelayMinutes),
                    enableAzanOnSilent = obj.optBoolean("enableAzanOnSilent", current.enableAzanOnSilent)
                )
                saveSettings(settings)
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

            // Restore Favorite Azkar
            if (json.has("favoriteAzkar")) {
                val array = json.getJSONArray("favoriteAzkar")
                for (i in 0 until array.length()) {
                    val azkarId = array.getInt(i)
                    quranDao.addFavoriteAzkar(FavoriteAzkarEntity(azkarId))
                }
            }

            // Restore Prayer Records
            if (json.has("prayerRecords")) {
                val array = json.getJSONArray("prayerRecords")
                val records = mutableListOf<PrayerRecordEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    records.add(
                        PrayerRecordEntity(
                            id = 0,
                            dateString = obj.getString("dateString"),
                            prayerKey = obj.getString("prayerKey"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            isOnTime = obj.optBoolean("isOnTime", true),
                            completedTimestamp = if (obj.has("completedTimestamp")) obj.optLong("completedTimestamp") else null
                        )
                    )
                }
                if (records.isNotEmpty()) {
                    quranDao.insertPrayerRecords(records)
                }
            }

            val ctx = com.example.HolyQuranApplication.instance
            if (ctx != null) {
                // Restore Location Settings
                if (json.has("locationSettings")) {
                    try {
                        val locObj = json.getJSONObject("locationSettings")
                        com.example.util.LocationHelper.saveLocationAndSettings(
                            context = ctx,
                            lat = locObj.optDouble("latitude", 30.0444),
                            lng = locObj.optDouble("longitude", 31.2357),
                            cityName = locObj.optString("cityName", "القاهرة • Cairo"),
                            calcMethod = locObj.optString("calculationMethod", "EGYPT"),
                            isSummerTime = locObj.optBoolean("isSummerTime", false),
                            fajrOffset = locObj.optInt("fajrOffset", 0),
                            dhuhrOffset = locObj.optInt("dhuhrOffset", 0),
                            asrOffset = locObj.optInt("asrOffset", 0),
                            maghribOffset = locObj.optInt("maghribOffset", 0),
                            ishaOffset = locObj.optInt("ishaOffset", 0)
                        )
                    } catch (e: Throwable) {
                        android.util.Log.w("QuranRepository", "Error restoring location settings", e)
                    }
                }

                // Restore Group Khatmas Data
                try {
                    val groupKhatmaPrefs = ctx.getSharedPreferences("group_khatma_prefs", android.content.Context.MODE_PRIVATE)
                    val editor = groupKhatmaPrefs.edit()
                    if (json.has("groupKhatmasRaw")) {
                        editor.putString("khatmas_list_json", json.getString("groupKhatmasRaw"))
                    }
                    if (json.has("groupKhatmaUserName")) {
                        editor.putString("saved_user_name", json.getString("groupKhatmaUserName"))
                    }
                    editor.apply()
                } catch (e: Throwable) {
                    android.util.Log.w("QuranRepository", "Error restoring group khatmas data", e)
                }

                // Refresh Schedulers and Widgets
                try {
                    com.example.notification.DailyVerseScheduler.scheduleDailyVerseNotification(ctx)
                    com.example.notification.AzkarNotificationScheduler.scheduleAllAzkarNotifications(ctx)
                    com.example.notification.ReadingReminderScheduler.scheduleAllReadingReminders(ctx)
                    com.example.widget.PrayerTimesWidgetProvider.sendUpdateBroadcast(ctx)
                    com.example.widget.AzkarWidgetProvider.sendUpdateBroadcast(ctx)
                    com.example.worker.PrayerSyncScheduler.enqueueImmediatePrayerSync(ctx)
                } catch (e: Throwable) {
                    android.util.Log.w("QuranRepository", "Error refreshing schedulers after import", e)
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
        .catch { e ->
            android.util.Log.e("QuranRepository", "Error in favoriteSurahIds flow", e)
            emit(emptyList())
        }

    suspend fun toggleFavoriteSurah(surahId: Int, isFavorite: Boolean) {
        if (isFavorite) {
            quranDao.addFavoriteSurah(FavoriteSurahEntity(surahId))
        } else {
            quranDao.removeFavoriteSurah(surahId)
        }
    }

    val favoriteAzkarIds: Flow<List<Int>> = quranDao.getFavoriteAzkarIds()
        .catch { e ->
            android.util.Log.e("QuranRepository", "Error in favoriteAzkarIds flow", e)
            emit(emptyList())
        }

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
