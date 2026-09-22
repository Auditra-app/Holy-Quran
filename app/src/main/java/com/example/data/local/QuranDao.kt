package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY dateCreated DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksOnce(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("SELECT * FROM bookmarks WHERE pageNumber = :page LIMIT 1")
    suspend fun getBookmarkByPage(page: Int): BookmarkEntity?

    // Reading Progress
    @Query("SELECT * FROM reading_progress WHERE id = 1")
    fun getReadingProgress(): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE id = 1")
    suspend fun getReadingProgressOnce(): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadingProgress(progress: ReadingProgressEntity)

    // User Settings
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getUserSettingsOnce(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSettings(settings: UserSettingsEntity)

    // Favorite Surahs
    @Query("SELECT surahId FROM favorite_surahs")
    fun getFavoriteSurahIds(): Flow<List<Int>>

    @Query("SELECT surahId FROM favorite_surahs")
    suspend fun getFavoriteSurahIdsOnce(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteSurah(fav: FavoriteSurahEntity)

    @Query("DELETE FROM favorite_surahs WHERE surahId = :surahId")
    suspend fun removeFavoriteSurah(surahId: Int)

    // Favorite Azkar
    @Query("SELECT azkarId FROM favorite_azkar")
    fun getFavoriteAzkarIds(): Flow<List<Int>>

    @Query("SELECT azkarId FROM favorite_azkar")
    suspend fun getFavoriteAzkarIdsOnce(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteAzkar(fav: FavoriteAzkarEntity)

    @Query("DELETE FROM favorite_azkar WHERE azkarId = :azkarId")
    suspend fun removeFavoriteAzkar(azkarId: Int)

    // Reading History
    @Query("SELECT * FROM reading_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllReadingHistory(): Flow<List<ReadingHistoryEntity>>

    @Query("SELECT * FROM reading_history")
    suspend fun getAllReadingHistoryOnce(): List<ReadingHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingHistory(history: ReadingHistoryEntity)

    @Query("DELETE FROM reading_history WHERE id = :id")
    suspend fun deleteReadingHistoryById(id: Long)

    @Query("DELETE FROM reading_history")
    suspend fun clearAllReadingHistory()

    // Quranic Text & Surah Metadata Queries
    @Query("SELECT * FROM surah_metadata ORDER BY id ASC")
    fun getAllSurahMetadata(): Flow<List<SurahMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahMetadata(surahs: List<SurahMetadataEntity>)

    @Query("SELECT * FROM quran_verses WHERE pageNumber = :pageNumber ORDER BY surahNumber ASC, verseNumber ASC")
    fun getVersesByPage(pageNumber: Int): Flow<List<QuranVerseEntity>>

    @Query("SELECT * FROM quran_verses WHERE surahNumber = :surahNumber ORDER BY verseNumber ASC")
    fun getVersesBySurah(surahNumber: Int): Flow<List<QuranVerseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<QuranVerseEntity>)

    // Prayer Tracker Records
    @Query("SELECT * FROM prayer_records WHERE dateString = :dateString")
    fun getPrayerRecordsForDate(dateString: String): Flow<List<PrayerRecordEntity>>

    @Query("SELECT * FROM prayer_records WHERE dateString = :dateString")
    suspend fun getPrayerRecordsForDateOnce(dateString: String): List<PrayerRecordEntity>

    @Query("SELECT * FROM prayer_records WHERE dateString BETWEEN :startDate AND :endDate ORDER BY dateString ASC")
    fun getPrayerRecordsBetween(startDate: String, endDate: String): Flow<List<PrayerRecordEntity>>

    @Query("SELECT * FROM prayer_records WHERE dateString BETWEEN :startDate AND :endDate ORDER BY dateString ASC")
    suspend fun getPrayerRecordsBetweenOnce(startDate: String, endDate: String): List<PrayerRecordEntity>

    @Query("SELECT * FROM prayer_records ORDER BY dateString DESC")
    fun getAllPrayerRecords(): Flow<List<PrayerRecordEntity>>

    @Query("SELECT * FROM prayer_records ORDER BY dateString DESC")
    suspend fun getAllPrayerRecordsOnce(): List<PrayerRecordEntity>

    @Query("SELECT * FROM prayer_records WHERE dateString = :dateString AND prayerKey = :prayerKey LIMIT 1")
    suspend fun getPrayerRecord(dateString: String, prayerKey: String): PrayerRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerRecord(record: PrayerRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerRecords(records: List<PrayerRecordEntity>)

    @Query("DELETE FROM prayer_records WHERE dateString = :dateString AND prayerKey = :prayerKey")
    suspend fun deletePrayerRecord(dateString: String, prayerKey: String)
}
