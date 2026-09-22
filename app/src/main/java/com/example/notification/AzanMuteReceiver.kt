package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class AzanMuteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("AzanMuteReceiver", "Azan mute broadcast received")
        try {
            AzanAudioPlayer.stop()
            AzanPlaybackService.stop(context)
            Toast.makeText(context, "تم كتم صوت الأذان 🔇", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("AzanMuteReceiver", "Error stopping Azan via MuteReceiver", e)
        }
    }

    companion object {
        const val ACTION_MUTE_AZAN = "com.example.notification.ACTION_MUTE_AZAN"
    }
}
