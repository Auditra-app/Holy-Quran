package com.example.data.di

import android.content.Context
import com.example.data.local.QuranDao
import com.example.data.local.QuranDatabase
import com.example.data.repository.QuranRepository

/**
 * DataModule serves as the central dependency provider for Room Database schema access,
 * DAOs, and the Repository layer. It manages offline caching for Quranic text,
 * Surah metadata, user bookmarks, and reading history.
 */
object DataModule {

    @Volatile
    private var database: QuranDatabase? = null

    @Volatile
    private var repository: QuranRepository? = null

    /**
     * Returns the singleton instance of QuranDatabase.
     */
    fun provideQuranDatabase(context: Context): QuranDatabase {
        return database ?: synchronized(this) {
            database ?: QuranDatabase.getDatabase(context).also { database = it }
        }
    }

    /**
     * Returns the QuranDao instance.
     */
    fun provideQuranDao(context: Context): QuranDao {
        return provideQuranDatabase(context).quranDao()
    }

    /**
     * Returns the singleton instance of QuranRepository.
     */
    fun provideQuranRepository(context: Context): QuranRepository {
        return repository ?: synchronized(this) {
            repository ?: QuranRepository(provideQuranDao(context)).also { repository = it }
        }
    }
}
