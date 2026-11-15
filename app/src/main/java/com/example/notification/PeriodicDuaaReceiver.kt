package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class PeriodicDuaaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PeriodicDuaaReceiver", "Periodic Duaa Alarm received: ${intent.action}")
        val pendingResult = goAsync()

        val appContext = context.applicationContext
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HolyQuran::PeriodicDuaaWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(45_000L) // 45s safety timeout
        }

        try {
            PeriodicDuaaManager.onAlarmFired(appContext) {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (e: Exception) {
                    Log.w("PeriodicDuaaReceiver", "Error releasing wakelock", e)
                }
                try {
                    pendingResult.finish()
                } catch (e: Exception) {
                    Log.w("PeriodicDuaaReceiver", "Error finishing pending result", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PeriodicDuaaReceiver", "Error processing periodic duaa alarm", e)
            try {
                if (wakeLock?.isHeld == true) wakeLock.release()
            } catch (ex: Exception) {}
            try {
                pendingResult.finish()
            } catch (ex: Exception) {}
        }
    }
}
