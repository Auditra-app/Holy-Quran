package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        ReadingProgressEntity::class,
        UserSettingsEntity::class,
        FavoriteSurahEntity::class,
        FavoriteAzkarEntity::class,
        ReadingHistoryEntity::class,
        SurahMetadataEntity::class,
        QuranVerseEntity::class,
        PrayerRecordEntity::class
    ],
    version = 18,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        fun getDatabase(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "holy_quran_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
