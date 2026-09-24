package com.example.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {

    private const val TAG = "AnalyticsHelper"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
            Log.d(TAG, "Firebase Analytics initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Firebase Analytics: ${e.message}")
        }
    }

    fun logScreenView(screenName: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            Log.d(TAG, "Logged screen_view: $screenName")
        } catch (e: Exception) {
            Log.w(TAG, "Error logging screen view $screenName: ${e.message}")
        }
    }

    fun logQuranReading(surahNumber: Int, surahName: String, pageNumber: Int) {
        try {
            val bundle = Bundle().apply {
                putInt("surah_number", surahNumber)
                putString("surah_name", surahName)
                putInt("page_number", pageNumber)
            }
            firebaseAnalytics?.logEvent("quran_read_page", bundle)
            Log.d(TAG, "Logged quran_read_page: $surahName (P.$pageNumber)")
        } catch (e: Exception) {
            Log.w(TAG, "Error logging Quran reading event: ${e.message}")
        }
    }

    fun logKhatmaEvent(action: String, khatmaId: String) {
        try {
            val bundle = Bundle().apply {
                putString("action_type", action) // e.g. "created", "joined", "completed"
                putString("khatma_id", khatmaId)
            }
            firebaseAnalytics?.logEvent("group_khatma_event", bundle)
            Log.d(TAG, "Logged group_khatma_event: $action ($khatmaId)")
        } catch (e: Exception) {
            Log.w(TAG, "Error logging Khatma event $action: ${e.message}")
        }
    }

    fun logPrayerTracker(prayerName: String, isCompleted: Boolean) {
        try {
            val bundle = Bundle().apply {
                putString("prayer_name", prayerName)
                putBoolean("is_completed", isCompleted)
            }
            firebaseAnalytics?.logEvent("prayer_tracked", bundle)
            Log.d(TAG, "Logged prayer_tracked: $prayerName (done=$isCompleted)")
        } catch (e: Exception) {
            Log.w(TAG, "Error logging Prayer tracker event: ${e.message}")
        }
    }

    fun logCustomEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics?.logEvent(eventName, params)
            Log.d(TAG, "Logged custom_event: $eventName")
        } catch (e: Exception) {
            Log.w(TAG, "Error logging event $eventName: ${e.message}")
        }
    }
}
