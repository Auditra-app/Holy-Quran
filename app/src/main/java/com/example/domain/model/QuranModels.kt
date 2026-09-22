package com.example.domain.model

enum class AppLanguage(val code: String, val displayName: String) {
    ARABIC("ar", "العربية"),
    ENGLISH("en", "English")
}

enum class AppThemeMode(val displayName: String) {
    SYSTEM("Auto System"),
    LIGHT("Light Mode"),
    DARK("Night Mode")
}

data class Surah(
    val id: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val englishMeaning: String,
    val versesCount: Int,
    val startPage: Int,
    val revelationType: String, // Meccan or Medinan
    val isFavorite: Boolean = false
)

data class Juz(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val startPage: Int,
    val startSurahName: String,
    val startVerseNumber: Int
)

data class Verse(
    val id: Int,
    val surahId: Int,
    val surahNameArabic: String,
    val surahNameEnglish: String,
    val verseNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int,
    val textUthmani: String,
    val translationEnglish: String = "",
    val tafsirShort: String = ""
)

data class Bookmark(
    val id: Long = 0,
    val pageNumber: Int,
    val surahName: String,
    val verseNumber: Int? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class ReadingProgress(
    val lastPage: Int = 1,
    val lastSurahName: String = "الفاتحة",
    val lastVerseNumber: Int = 1,
    val readingDurationMinutes: Long = 12,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)

data class DailyVerse(
    val id: Int,
    val textArabic: String,
    val surahNameArabic: String,
    val surahNameEnglish: String,
    val verseNumber: Int,
    val pageNumber: Int,
    val translationEnglish: String = ""
)

data class PrayerTime(
    val nameArabic: String,
    val nameEnglish: String,
    val timeFormatted: String,
    val isNext: Boolean = false
)

data class TafsirItem(
    val surahId: Int,
    val verseNumber: Int,
    val verseText: String,
    val bookName: String,
    val tafsirContent: String
)

data class WordMeaning(
    val arabicWord: String,
    val arabicMeaning: String,
    val rootWord: String,
    val englishMeaning: String = "",
    val transliteration: String = ""
)

data class HadithItem(
    val id: Int,
    val collectionName: String,
    val chapterName: String,
    val hadithNumber: String,
    val arabicText: String,
    val englishText: String,
    val narrator: String,
    val isBookmarked: Boolean = false
)

data class Reciter(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val style: String
)

enum class GroupKhatmaJuzStatus {
    UNASSIGNED,
    BOOKED,
    COMPLETED
}

data class GroupKhatmaJuz(
    val juzNumber: Int,
    val status: GroupKhatmaJuzStatus = GroupKhatmaJuzStatus.UNASSIGNED,
    val assignedUserName: String = "",
    val completedTimestamp: Long = 0L
)

data class GroupKhatma(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val title: String = "ختمة قرآنية جماعية",
    val creatorName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val targetDeadlineTimestamp: Long = 0L,
    val juzList: List<GroupKhatmaJuz> = (1..30).map { GroupKhatmaJuz(juzNumber = it) }
)

data class HadithCategory(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val iconEmoji: String,
    val startId: Int,
    val endId: Int
)

data class AzkarCategory(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val iconEmoji: String
)

data class AzkarItem(
    val id: Int,
    val categoryId: String,
    val text: String,
    val count: Int = 1,
    val virtue: String = "",
    val reference: String = ""
)

enum class TrackedPrayerType(
    val key: String,
    val nameArabic: String,
    val nameEnglish: String,
    val iconEmoji: String,
    val isObligatory: Boolean
) {
    FAJR("fajr", "الفجر", "Fajr", "🌅", true),
    DHUHR("dhuhr", "الظهر", "Dhuhr", "☀️", true),
    ASR("asr", "العصر", "Asr", "🌤️", true),
    MAGHRIB("maghrib", "المغرب", "Maghrib", "🌆", true),
    ISHA("isha", "العشاء", "Isha", "🌙", true),
    DUHA("duha", "صلاة الضحى", "Duha", "✨", false),
    QIYAM("qiyam", "قيام الليل والوتر", "Qiyam & Witr", "⭐", false);

    companion object {
        fun fromKey(key: String): TrackedPrayerType? {
            return entries.firstOrNull { it.key.equals(key.trim(), ignoreCase = true) }
        }
    }
}

data class PrayerStatusItem(
    val type: TrackedPrayerType,
    val isCompleted: Boolean = false,
    val isOnTime: Boolean = true,
    val completedTimestamp: Long? = null
)

data class DayPrayerTrackerSummary(
    val dateString: String, // "yyyy-MM-dd"
    val dayNameAr: String,
    val dayNameEn: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val prayers: List<PrayerStatusItem>,
    val completionCount: Int,
    val totalTrackedCount: Int
) {
    val completionPercentage: Float
        get() = if (totalTrackedCount > 0) completionCount.toFloat() / totalTrackedCount else 0f
}

data class PrayerTrackerKPIs(
    val dailyCompleted: Int = 0,
    val dailyTotal: Int = 5,
    val weeklyCompleted: Int = 0,
    val weeklyTotal: Int = 35,
    val monthlyCompleted: Int = 0,
    val monthlyTotal: Int = 150,
    val currentStreakDays: Int = 0,
    val onTimeRatePercent: Int = 100,
    val prayerBreakdown: Map<String, Int> = emptyMap(), // prayerKey -> completion rate %
    val bestDayNameAr: String = "الجمعة"
) {
    val dailyPercentage: Float
        get() = if (dailyTotal > 0) (dailyCompleted.toFloat() / dailyTotal).coerceIn(0f, 1f) else 0f

    val weeklyPercentage: Float
        get() = if (weeklyTotal > 0) (weeklyCompleted.toFloat() / weeklyTotal).coerceIn(0f, 1f) else 0f

    val monthlyPercentage: Float
        get() = if (monthlyTotal > 0) (monthlyCompleted.toFloat() / monthlyTotal).coerceIn(0f, 1f) else 0f
}

