package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 19,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE user_settings ADD COLUMN khatmahCurrentPage INTEGER NOT NULL DEFAULT 1")
                } catch (_: Throwable) {}
                try {
                    db.execSQL("ALTER TABLE user_settings ADD COLUMN khatmahDailyGoalPages INTEGER NOT NULL DEFAULT 5")
                } catch (_: Throwable) {}
                try {
                    db.execSQL("ALTER TABLE user_settings ADD COLUMN khatmahCompletedWirdsCount INTEGER NOT NULL DEFAULT 0")
                } catch (_: Throwable) {}
                try {
                    db.execSQL("ALTER TABLE user_settings ADD COLUMN khatmahLastCompletedDate INTEGER NOT NULL DEFAULT 0")
                } catch (_: Throwable) {}
            }
        }

        fun getDatabase(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "holy_quran_db"
                )
                .addMigrations(MIGRATION_18_19)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
