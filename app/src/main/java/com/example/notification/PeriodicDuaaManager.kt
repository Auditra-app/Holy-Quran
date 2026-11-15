package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

object PeriodicDuaaManager {

    private const val TAG = "PeriodicDuaaManager"
    private const val PREFS_NAME = "periodic_duaa_prefs"
    private const val CHANNEL_ID = "periodic_duaa_channel"
    private const val NOTIFICATION_ID = 9501
    const val ACTION_PERIODIC_DUAA_ALARM = "com.example.ACTION_PERIODIC_DUAA_ALARM"

    // Authentic Sheikh Voice Keys
    const val VOICE_ALAFASY = "alafasy"
    const val VOICE_ABDULBASIT = "abdulbasit"
    const val VOICE_GHAMDI = "ghamdi"
    const val VOICE_MUAIQLY = "muaiqly"

    data class ReciterVoiceOption(
        val id: String,
        val nameArabic: String,
        val nameEnglish: String
    )

    val VOICE_OPTIONS = listOf(
        ReciterVoiceOption(VOICE_ALAFASY, "الشيخ مشاري العفاسي", "Mishary Al-Afasy"),
        ReciterVoiceOption(VOICE_ABDULBASIT, "الشيخ عبد الباسط عبد الصمد", "Abdul Basit"),
        ReciterVoiceOption(VOICE_GHAMDI, "الشيخ سعد الغامدي", "Saad Al-Ghamdi"),
        ReciterVoiceOption(VOICE_MUAIQLY, "الشيخ ماهر المعيقلي", "Maher Al-Muaiqly")
    )

    data class AuthenticDuaaItem(
        val title: String,
        val text: String,
        val surahNumber: Int = 0,
        val verseNumber: Int = 0,
        val specialUrls: Map<String, String> = emptyMap()
    )

    // Curated Authentic Independent Supplications & Tasbeeh with Devotional/Duaa Cadence
    val AUTHENTIC_DUAA_LIST = listOf(
        AuthenticDuaaItem(
            title = "تسبيح وتحميد مبارك",
            text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            specialUrls = mapOf(
                VOICE_ALAFASY to "https://archive.org/download/azkar-alafasy/sobhan-allah-mla-albr.mp3"
            ),
            surahNumber = 1,
            verseNumber = 2
        ),
        AuthenticDuaaItem(
            title = "الحمد لله ملء ما خلق",
            text = "الْحَمْدُ لِلَّهِ عَدَدَ مَا خَلَقَ، وَالْحَمْدُ لِلَّهِ مِلْءَ مَا خَلَقَ",
            specialUrls = mapOf(
                VOICE_ALAFASY to "https://archive.org/download/azkar-alafasy/alhamdu-lellah-adad-ma-khalq.mp3"
            ),
            surahNumber = 1,
            verseNumber = 2
        ),
        AuthenticDuaaItem(
            title = "التكبير والتحميد والتسبيح",
            text = "اللَّهُ أَكْبَرُ كَبِيرًا وَالْحَمْدُ لِلَّهِ كَثِيرًا وَسُبْحَانَ اللَّهِ بُكْرَةً وَأَصِيلًا",
            specialUrls = mapOf(
                VOICE_ALAFASY to "https://archive.org/download/azkar-alafasy/allahu-akbaru-kabira.mp3"
            ),
            surahNumber = 17,
            verseNumber = 111
        ),
        AuthenticDuaaItem(
            title = "دعاء الاستعاذة النبوي الخاشع",
            text = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عَذَابِ جَهَنَّمَ، وَمِنْ عَذَابِ الْقَبْرِ، وَمِنْ فِتْنَةِ الْمَحْيَا وَالْمَمَاتِ",
            specialUrls = mapOf(
                VOICE_ALAFASY to "https://archive.org/download/azkar-alafasy/doaa-after-tashahud.mp3"
            ),
            surahNumber = 23,
            verseNumber = 97
        ),
        AuthenticDuaaItem(
            title = "دعاء المغفرة والرحمة (الصديق)",
            text = "اللَّهُمَّ إِنِّي ظَلَمْتُ نَفْسِي ظُلْمًا كَثِيرًا، وَلَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ فَاغْفِرْ لِي مَغْفِرَةً مِنْ عِنْدِكَ",
            specialUrls = mapOf(
                VOICE_ALAFASY to "https://archive.org/download/azkar-alafasy/doaa-after-tashahud-2.mp3"
            ),
            surahNumber = 21,
            verseNumber = 87
        ),
        AuthenticDuaaItem(
            title = "دعاء الإعانة على الذكر والشكر",
            text = "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
            specialUrls = mapOf(
                VOICE_ALAFASY to "https://archive.org/download/azkar-alafasy/doaa-after-tashahud-3.mp3"
            ),
            surahNumber = 27,
            verseNumber = 19
        ),
        AuthenticDuaaItem(
            title = "دعاء الحساب اليسير",
            text = "اللَّهُمَّ حَاسِبْنِي حِسَابًا يَسِيرًا",
            specialUrls = mapOf(
                VOICE_ALAFASY to "https://archive.org/download/azkar-alafasy/doaa-after-tashahud-4.mp3"
            ),
            surahNumber = 84,
            verseNumber = 8
        ),
        AuthenticDuaaItem(
            title = "الاستغفار والسلام",
            text = "أَسْتَغْفِرُ اللَّهَ، اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالإِكْرَامِ",
            specialUrls = mapOf(
                VOICE_ALAFASY to "https://archive.org/download/azkar-alafasy/azkar-baed-al-salah.mp3"
            ),
            surahNumber = 4,
            verseNumber = 106
        ),
        AuthenticDuaaItem(
            title = "تهليل التوحيد والفضل",
            text = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            specialUrls = mapOf(
                VOICE_ALAFASY to "https://archive.org/download/azkar-alafasy/azkar-baed-al-salah-2.mp3"
            ),
            surahNumber = 3,
            verseNumber = 18
        ),
        AuthenticDuaaItem(
            title = "دعاء خيري الدنيا والآخرة",
            text = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            surahNumber = 2,
            verseNumber = 201
        ),
        AuthenticDuaaItem(
            title = "دعاء الهداية والرحمة",
            text = "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً إِنَّكَ أَنتَ الْوَهَّابُ",
            surahNumber = 3,
            verseNumber = 8
        ),
        AuthenticDuaaItem(
            title = "دعاء بر الوالدين",
            text = "رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            surahNumber = 17,
            verseNumber = 24
        ),
        AuthenticDuaaItem(
            title = "الصلاة على النبي ﷺ",
            text = "إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ يَا أَيُّهَا الَّذِينَ آمَنُوا صَلُّوا عَلَيْهِ وَسَلِّمُوا تَسْلِيمًا",
            surahNumber = 33,
            verseNumber = 56
        )
    )

    private var activeMediaPlayer: MediaPlayer? = null

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("is_enabled", false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_enabled", enabled).apply()
        if (enabled) {
            scheduleNext(context)
        } else {
            cancel(context)
        }
    }

    fun getVoiceSource(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val source = prefs.getString("voice_source", VOICE_ALAFASY) ?: VOICE_ALAFASY
        return if (source == "man_voice" || source == "man_tts") VOICE_ALAFASY else source
    }

    fun setVoiceSource(context: Context, source: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("voice_source", source).apply()
    }

    fun getVoiceDisplayName(voiceSource: String): String {
        return when (voiceSource) {
            VOICE_ABDULBASIT -> "الشيخ عبد الباسط عبد الصمد"
            VOICE_GHAMDI -> "الشيخ سعد الغامدي"
            VOICE_MUAIQLY -> "الشيخ ماهر المعيقلي"
            else -> "الشيخ مشاري راشد العفاسي"
        }
    }

    fun getAudioUrlForItem(item: AuthenticDuaaItem, voiceSource: String): String {
        if (item.specialUrls.containsKey(voiceSource)) {
            return item.specialUrls[voiceSource]!!
        }
        val sId = if (item.surahNumber > 0) item.surahNumber else 1
        val vId = if (item.verseNumber > 0) item.verseNumber else 2
        val surahStr = sId.toString().padStart(3, '0')
        val verseStr = vId.toString().padStart(3, '0')
        return when (voiceSource) {
            VOICE_ABDULBASIT -> "https://everyayah.com/data/Abdul_Basit_Murattal_192kbps/$surahStr$verseStr.mp3"
            VOICE_GHAMDI -> "https://everyayah.com/data/Ghamadi_40kbps/$surahStr$verseStr.mp3"
            VOICE_MUAIQLY -> "https://everyayah.com/data/MaherAlMuaiqly128kbps/$surahStr$verseStr.mp3"
            else -> "https://everyayah.com/data/Alafasy_128kbps/$surahStr$verseStr.mp3"
        }
    }

    fun getIntervalMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("interval_minutes", 30)
    }

    fun setIntervalMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("interval_minutes", minutes).apply()
        if (isEnabled(context)) {
            scheduleNext(context)
        }
    }

    fun isQuietHoursEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("quiet_hours_enabled", true)
    }

    fun setQuietHoursEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("quiet_hours_enabled", enabled).apply()
    }

    fun isQuietHoursNow(context: Context): Boolean {
        if (!isQuietHoursEnabled(context)) return false
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val nowMins = hour * 60 + minute

        val startMins = 23 * 60
        val endMins = 6 * 60 + 30
        return nowMins >= startMins || nowMins < endMins
    }

    fun scheduleNext(context: Context) {
        try {
            if (!isEnabled(context)) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intervalMins = getIntervalMinutes(context).coerceAtLeast(5)
            val triggerAtMs = System.currentTimeMillis() + (intervalMins * 60 * 1000L)

            val intent = Intent(context, PeriodicDuaaReceiver::class.java).apply {
                action = ACTION_PERIODIC_DUAA_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9502,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
            Log.d(TAG, "Scheduled next periodic duaa in $intervalMins minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling periodic duaa", e)
        }
    }

    fun cancel(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PeriodicDuaaReceiver::class.java).apply {
                action = ACTION_PERIODIC_DUAA_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9502,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled periodic duaa alarms")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling periodic duaa", e)
        }
    }

    fun onAlarmFired(context: Context, onCompleted: (() -> Unit)? = null) {
        try {
            scheduleNext(context)

            if (!isEnabled(context)) {
                onCompleted?.invoke()
                return
            }

            if (isQuietHoursNow(context)) {
                Log.d(TAG, "Quiet hours active. Skipping voice playback.")
                onCompleted?.invoke()
                return
            }

            playNextDuaa(context, isTest = false, onCompleted = onCompleted)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing onAlarmFired", e)
            onCompleted?.invoke()
        }
    }

    fun testVoiceNow(context: Context, onCompleted: (() -> Unit)? = null) {
        playNextDuaa(context, isTest = true, onCompleted = onCompleted)
    }

    private fun playNextDuaa(context: Context, isTest: Boolean = false, onCompleted: (() -> Unit)? = null) {
        val voiceSource = getVoiceSource(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val lastIdx = prefs.getInt("last_authentic_duaa_idx", -1)
        val nextIdx = (lastIdx + 1) % AUTHENTIC_DUAA_LIST.size
        prefs.edit().putInt("last_authentic_duaa_idx", nextIdx).apply()

        val item = AUTHENTIC_DUAA_LIST[nextIdx]
        val voiceDisplayName = getVoiceDisplayName(voiceSource)
        val audioUrl = getAudioUrlForItem(item, voiceSource)

        showDuaaNotification(context, item.text, voiceDisplayName)
        playAuthenticAudio(context, audioUrl, onCompleted = onCompleted)
    }

    private fun showDuaaNotification(context: Context, duaaText: String, reciterSubtext: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "التذكير بالأدعية والأذكار",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تذكير صوتي دوري بالأدعية النبوية والقرآنية بأصوات شيوخ معروفين"
                    setSound(null, null)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "azkar")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                9503,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_quran)
                .setContentTitle("🌸 تذكير بالأدعية والأذكار")
                .setContentText(duaaText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(duaaText))
                .setSubText(reciterSubtext)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing duaa notification", e)
        }
    }

    private fun playAuthenticAudio(
        context: Context,
        audioUrl: String,
        onCompleted: (() -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        if (AzanAudioPlayer.isDeviceSilentOrMuted(appContext)) {
            Log.d(TAG, "Device is silent or muted. Skipping voice playback.")
            onCompleted?.invoke()
            return
        }

        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        var focusRequest: AudioFocusRequest? = null
        val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { }

        val mediaVol = try { audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0 } catch (e: Exception) { 0 }
        val notifVol = try { audioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 0 } catch (e: Exception) { 0 }

        // Use active mobile media volume, falling back to notification volume if media is muted
        val (usageType, streamType) = if (mediaVol > 0 || notifVol == 0) {
            Pair(AudioAttributes.USAGE_MEDIA, AudioManager.STREAM_MUSIC)
        } else {
            Pair(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, AudioManager.STREAM_NOTIFICATION)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(usageType)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                audioFocusChangeListener,
                streamType,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }

        var isFinished = false
        fun cleanup() {
            if (isFinished) return
            isFinished = true
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager?.abandonAudioFocusRequest(focusRequest)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager?.abandonAudioFocus(audioFocusChangeListener)
                }
                activeMediaPlayer?.release()
                activeMediaPlayer = null
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up MediaPlayer", e)
            }
            onCompleted?.invoke()
        }

        fun playFallback() {
            try {
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer.create(appContext, R.raw.takbeer)?.apply {
                    setAudioAttributes(audioAttributes)
                    setVolume(1.0f, 1.0f)
                    setOnCompletionListener { cleanup() }
                    setOnErrorListener { _, _, _ -> cleanup(); true }
                    start()
                }
                if (activeMediaPlayer == null) {
                    cleanup()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback audio failed", e)
                cleanup()
            }
        }

        try {
            activeMediaPlayer?.release()
            activeMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setVolume(1.0f, 1.0f)
                setWakeMode(appContext, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(audioUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnCompletionListener {
                    cleanup()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Remote duaa audio failed (what=$what, extra=$extra), attempting local fallback")
                    playFallback()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaPlayer for authentic duaa $audioUrl, using fallback", e)
            playFallback()
        }
    }
}
