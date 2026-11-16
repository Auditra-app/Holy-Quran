package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PeriodicDuaaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PeriodicDuaaReceiver", "Periodic Duaa Alarm received: ${intent.action}")

        val appContext = context.applicationContext
        try {
            // 1. Reschedule next periodic alarm
            PeriodicDuaaManager.scheduleNext(appContext)

            // 2. If enabled and not in quiet hours, launch foreground playback service
            if (PeriodicDuaaManager.isEnabled(appContext)) {
                if (!PeriodicDuaaManager.isQuietHoursNow(appContext)) {
                    Log.d("PeriodicDuaaReceiver", "Starting PeriodicDuaaService for full audio playback")
                    PeriodicDuaaService.start(appContext, isTest = false)
                } else {
                    Log.d("PeriodicDuaaReceiver", "Quiet hours active. Skipping playback.")
                }
            }
        } catch (e: Exception) {
            Log.e("PeriodicDuaaReceiver", "Error handling periodic duaa alarm", e)
        }
    }
}
