package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.QuranDatabase
import com.example.data.provider.QuranDataProvider
import com.example.data.provider.TafsirProvider
import com.example.ui.viewmodel.QuranViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppStartupVerificationTest {

    @Test
    fun testQuranDataProvider() {
        val surahs = QuranDataProvider.SURAHS
        assertNotNull(surahs)
        println("Surahs count: ${surahs.size}")
        
        val verses = QuranDataProvider.getVersesForPage(1)
        println("Page 1 verses count: ${verses.size}")
        
        val dailyVerse = QuranDataProvider.getDailyVerseForToday()
        println("Daily verse: ${dailyVerse.surahNameArabic} ayah ${dailyVerse.verseNumber}")
    }

    @Test
    fun testTafsirProvider() {
        val tafsir = TafsirProvider.getTafsirForAyah(1, 1, "muyassar")
        assertNotNull(tafsir)
        println("Tafsir 1:1: ${tafsir.commentaryText.take(50)}")
    }

    @Test
    fun testQuranDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = QuranDatabase.getDatabase(context)
        assertNotNull(db)
        println("Database initialized successfully")
    }

    @Test
    fun testQuranViewModelCreation() {
        val app = ApplicationProvider.getApplicationContext<HolyQuranApplication>()
        val vm = QuranViewModel(app)
        assertNotNull(vm)
        println("QuranViewModel created: ${vm.userSettings.value.selectedAzanSound}")
    }

    @Test
    fun testMainActivityLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                println("MainActivity launched successfully: $activity")
            }
        }
    }
}
