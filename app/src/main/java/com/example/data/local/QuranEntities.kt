package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageNumber: Int,
    val surahName: String,
    val verseNumber: Int? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val id: Int = 1,
    val lastPage: Int = 1,
    val lastSurahName: String = "الفاتحة",
    val lastVerseNumber: Int = 1,
    val readingDurationMinutes: Long = 0,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val languageCode: String = "ar", // "ar" or "en"
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val fontSizeSp: Float = 22f,
    val enableDailyVerseNotification: Boolean = true,
    val enableReadingReminderNotification: Boolean = true,
    val enableInactivityNotification: Boolean = true,
    val enableAzkarNotification: Boolean = true,
    val enableMorningAzkar: Boolean = true,
    val enableEveningAzkar: Boolean = true,
    val morningAzkarHour: Int = 6,
    val morningAzkarMinute: Int = 30,
    val eveningAzkarHour: Int = 17,
    val eveningAzkarMinute: Int = 0,
    val notificationTimeHour: Int = 9,
    val notificationTimeMinute: Int = 0,
    val calculationMethod: String = "MWL", // Muslim World League
    val madhhab: String = "Shafi",
    val preferredReciterId: String = "alafasy",
    val showEnglishTranslation: Boolean = true,
    val enableTajweed: Boolean = true,
    val keepScreenOn: Boolean = true,
    val nightModeTheme: String = "SEPIA", // "SEPIA" or "DARK"
    val enableNightSchedule: Boolean = false,
    val nightScheduleStartHour: Int = 20, // Default 8:00 PM
    val nightScheduleStartMinute: Int = 0,
    val nightScheduleEndHour: Int = 6,   // Default 6:00 AM
    val nightScheduleEndMinute: Int = 0,
    val isSummerTime: Boolean = false, // DST toggle (+1 Hour)
    val takbeerAzanOnly: Boolean = true, // Azan "Takbeer Only"
    val selectedAzanSound: String = "alafasy", // "alafasy", "alafasy_2", "alafasy_3", "alafasy_4", "alafasy_5", "qatami", "takbeer", "makkah", "madinah", "alaqsa", "silent_tone"
    val enabledPrayersJson: String = "Fajr,Sunrise,Dhuhr,Asr,Maghrib,Isha",
    val silentPrayersJson: String = "",
    val selectedTafsirBook: String = "muyassar", // "muyassar", "ibn_kathir", "jalalayn", "saadi", "english_clear_quran"
    val audioPlaybackSpeed: Float = 1.0f,
    val dailyPageGoal: Int = 10, // Default 10 pages/day
    val khatmahTargetDays: Int = 30, // Default 30 days
    val khatmahStartDate: Long = System.currentTimeMillis(),
    val homeGridOrderJson: String = "",
    val enableHaptic: Boolean = true,
    val hapticType: String = "SOFT", // "SOFT" or "HARD"
    val fajrOffsetMins: Int = 0,
    val dhuhrOffsetMins: Int = 0,
    val asrOffsetMins: Int = 0,
    val maghribOffsetMins: Int = 0,
    val ishaOffsetMins: Int = 0,
    val quranScriptType: String = "MADINAH", // "MADINAH", "AMIRI_QURAN", "AMIRI_NASKH", "TAJAWAL"
    val trackedPrayersJson: String = "fajr,dhuhr,asr,maghrib,isha",
    val enablePrayerFollowUpNotification: Boolean = true,
    val prayerFollowUpDelayMinutes: Int = 30,
    val enableAzanOnSilent: Boolean = true
) {
    val prayerOffsetsSummary: String
        get() = "$fajrOffsetMins,$dhuhrOffsetMins,$asrOffsetMins,$maghribOffsetMins,$ishaOffsetMins"

    fun effectiveThemeMode(): String {
        if (enableNightSchedule) {
            val cal = java.util.Calendar.getInstance()
            val nowMins = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val startMins = nightScheduleStartHour * 60 + nightScheduleStartMinute
            val endMins = nightScheduleEndHour * 60 + nightScheduleEndMinute
            val isNightActive = if (startMins < endMins) {
                nowMins in startMins until endMins
            } else {
                nowMins >= startMins || nowMins < endMins
            }
            if (isNightActive) {
                return nightModeTheme
            }
        }
        return themeMode
    }
}

@Entity(tableName = "favorite_surahs")
data class FavoriteSurahEntity(
    @PrimaryKey val surahId: Int
)

@Entity(tableName = "favorite_azkar")
data class FavoriteAzkarEntity(
    @PrimaryKey val azkarId: Int
)

@Entity(tableName = "reading_history")
data class ReadingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageNumber: Int,
    val surahNumber: Int,
    val surahNameArabic: String,
    val surahNameEnglish: String,
    val verseNumber: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "surah_metadata")
data class SurahMetadataEntity(
    @PrimaryKey val id: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val englishTranslation: String,
    val totalVerses: Int,
    val revelationType: String,
    val startPage: Int
)

@Entity(tableName = "quran_verses")
data class QuranVerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahNumber: Int,
    val verseNumber: Int,
    val verseTextArabic: String,
    val verseTextTajweed: String = "",
    val translationEnglish: String = "",
    val pageNumber: Int,
    val juzNumber: Int = 1,
    val hizbQuarter: Int = 1
)

@Entity(
    tableName = "prayer_records",
    indices = [androidx.room.Index(value = ["dateString", "prayerKey"], unique = true)]
)
data class PrayerRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // format "yyyy-MM-dd"
    val prayerKey: String, // "fajr", "dhuhr", "asr", "maghrib", "isha", "duha", "qiyam"
    val isCompleted: Boolean = false,
    val isOnTime: Boolean = true,
    val completedTimestamp: Long? = null
)
