package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    private const val TAG = "NotificationHelper"

    const val CHANNEL_PRAYER_ALERT = "PRAYER_NOTIFICATION_CHANNEL_V12"
    const val CHANNEL_PRAYER_SILENT = "SILENT_PRAYER_NOTIFICATION_CHANNEL_V12"
    const val CHANNEL_DAILY_VERSE = "DAILY_VERSE_NOTIFICATION_CHANNEL"
    const val CHANNEL_AZKAR_REMINDER = "AZKAR_REMINDER_NOTIFICATION_CHANNEL"
    const val CHANNEL_READING_REMINDER = "READING_REMINDER_NOTIFICATION_CHANNEL"
    const val CHANNEL_PRAYER_FOLLOWUP = "PRAYER_FOLLOWUP_NOTIFICATION_CHANNEL"

    fun getAzanChannelId(selectedAzanSound: String, isSilent: Boolean): String {
        return if (isSilent || selectedAzanSound == "silent_tone" || selectedAzanSound == "silent") {
            CHANNEL_PRAYER_SILENT
        } else {
            CHANNEL_PRAYER_ALERT
        }
    }

    fun getAzanSoundResId(selectedAzanSound: String): Int {
        return when (selectedAzanSound) {
            "alafasy_2" -> R.raw.alafasy_2
            "alafasy_3" -> R.raw.alafasy_3
            "alafasy_4" -> R.raw.alafasy_4
            "alafasy_5" -> R.raw.alafasy_5
            "qatami" -> R.raw.qatami
            "makkah" -> R.raw.makkah
            "madinah" -> R.raw.madinah
            "alaqsa" -> R.raw.alaqsa
            "takbeer" -> R.raw.takbeer
            else -> R.raw.alafasy
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        try {
            // 1. High-Priority Prayer Notification Channel (Visual Alert & Heads-up with Vibration; audio is managed by AzanPlaybackService)
            val prayerAlertChannel = NotificationChannel(
                CHANNEL_PRAYER_ALERT,
                "تنبيهات مواقيت الصلاة (Prayer Times Alerts)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات دخول وقت الصلاة المباركة والأذان"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setSound(null, null)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Silent Prayer Channel
            val silentChannel = NotificationChannel(
                CHANNEL_PRAYER_SILENT,
                "تنبيه صامت (Silent Prayer Alerts)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات صامتة عند دخول وقت الصلاة بدون صوت"
                enableVibration(false)
                vibrationPattern = null
                setSound(null, null)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. Daily Verse Channel
            val dailyVerseChannel = NotificationChannel(
                CHANNEL_DAILY_VERSE,
                "آية اليوم والتذكير اليومي المبارك",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات يومية مباركة بآيات القرآن الكريم وتفسيرها"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 4. Azkar Reminder Channel
            val azkarChannel = NotificationChannel(
                CHANNEL_AZKAR_REMINDER,
                "أذكار الصباح والمساء (Azkar Reminders)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات يومية بأذكار الصباح والمساء للتحصين وذكر الله"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 5. Quran Reading Reminder Channel
            val readingReminderChannel = NotificationChannel(
                CHANNEL_READING_REMINDER,
                "تذكير ورد القراءة (Quran Reading Reminder)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات وتذكير بالورد اليومي لقراءة القرآن الكريم"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 6. Prayer Tracker Follow-Up Notification Channel
            val followUpChannel = NotificationChannel(
                CHANNEL_PRAYER_FOLLOWUP,
                "متابعة أداء الصلوات (Prayer Tracker Follow-up)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعار تفاعلي للمتابعة بعد الصلاة: هل صليت؟ لتسجيل أداء الصلوات"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(prayerAlertChannel)
            notificationManager.createNotificationChannel(silentChannel)
            notificationManager.createNotificationChannel(dailyVerseChannel)
            notificationManager.createNotificationChannel(azkarChannel)
            notificationManager.createNotificationChannel(readingReminderChannel)
            notificationManager.createNotificationChannel(followUpChannel)

            Log.d(TAG, "Notification channels successfully created and registered.")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channels", e)
        }
    }

    @Volatile
    private var cachedAppLogoBitmap: android.graphics.Bitmap? = null

    fun clearBitmapCache() {
        // Null the reference cleanly to let GC reclaim memory without forcing native ashmem pin/unpin cycles
        cachedAppLogoBitmap = null
    }

    fun getAppLogoBitmap(context: Context): android.graphics.Bitmap? {
        cachedAppLogoBitmap?.let {
            if (!it.isRecycled) return it
        }

        return try {
            val size = 192 // 192x192 px for notification largeIcon
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            // 1. Draw Deep Emerald Background Circle (#06291E)
            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#06291E")
                style = android.graphics.Paint.Style.FILL
            }
            val radius = size / 2f
            canvas.drawCircle(radius, radius, radius - 4f, bgPaint)

            // 2. Draw Islamic Gold Outer Border (#D4AF37)
            val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#D4AF37")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawCircle(radius, radius, radius - 4f, borderPaint)

            // 3. Render Holy Quran Foreground Icon in Center
            val fgDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
                ?: androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)

            if (fgDrawable != null) {
                val inset = (size * 0.10f).toInt()
                fgDrawable.setBounds(inset, inset, size - inset, size - inset)
                fgDrawable.draw(canvas)
            }

            cachedAppLogoBitmap = bitmap
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create app logo bitmap for notifications", e)
            null
        }
    }

    fun buildPrayerNotification(
        context: Context,
        prayerNameAr: String,
        prayerTimeFormatted: String,
        isSilent: Boolean = false,
        selectedAzanSound: String = "alafasy"
    ): NotificationCompat.Builder {
        val channelId = getAzanChannelId(selectedAzanSound, isSilent)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_PRAYER_TIMES"
            data = Uri.parse("holyquran://notification/prayer_times?prayer=${Uri.encode(prayerNameAr)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "prayer_times")
            putExtra("prayer_name_ar", prayerNameAr)
        }

        val prayerReqCode = when {
            prayerNameAr.contains("فجر") -> 6001
            prayerNameAr.contains("شروق") -> 6002
            prayerNameAr.contains("ظهر") -> 6003
            prayerNameAr.contains("عصر") -> 6004
            prayerNameAr.contains("مغرب") -> 6005
            prayerNameAr.contains("عشاء") -> 6006
            else -> 6099
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerReqCode,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Open Reader / Mushaf
        val readerIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_READER"
            data = Uri.parse("holyquran://notification/reader_from_prayer")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "reader")
        }
        val readerPendingIntent = PendingIntent.getActivity(
            context,
            6092,
            readerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Stop / Mute Azan
        val stopIntent = Intent(context, AzanMuteReceiver::class.java).apply {
            action = AzanMuteReceiver.ACTION_MUTE_AZAN
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🕌 حان الآن موعد صلاة $prayerNameAr"
        val timeSuffix = if (prayerTimeFormatted.isNotBlank()) " ($prayerTimeFormatted)" else ""
        val contentText = "الله أكبر • حان الآن موعد صلاة $prayerNameAr$timeSuffix"

        val appLogoBitmap = getAppLogoBitmap(context)

        // Full Screen Intent to wake lockscreen and display heads-up prayer dialog/screen
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_PRAYER_TIMES"
            data = Uri.parse("holyquran://notification/prayer_fullscreen?prayer=${Uri.encode(prayerNameAr)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("navigate_to", "prayer_times")
            putExtra("prayer_name_ar", prayerNameAr)
            putExtra("from_prayer_alarm", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            prayerReqCode + 500,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_quran)
            .apply {
                if (appLogoBitmap != null) setLargeIcon(appLogoBitmap)
            }
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText("مواقيت الصلاة")
            .setPriority(if (isSilent) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_MAX)
            .setCategory(if (isSilent) NotificationCompat.CATEGORY_STATUS else NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .apply {
                if (!isSilent) {
                    setFullScreenIntent(fullScreenPendingIntent, true)
                }
            }
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText("الله أكبر، الله أكبر • حان الآن موعد صلاة $prayerNameAr$timeSuffix\n\nحي على الصلاة، حي على الفلاح 🤲")
            )
            .addAction(
                R.drawable.ic_stat_quran,
                "📖 المصحف",
                readerPendingIntent
            )

        if (!isSilent) {
            builder.addAction(
                R.drawable.ic_stat_quran,
                "⏹️ كتم الأذان",
                stopPendingIntent
            )
        }

        return builder
    }

    fun buildDailyVerseNotification(
        context: Context,
        ayaText: String,
        surahAndVerseRef: String,
        isArabic: Boolean,
        slotIndex: Int = 0
    ): NotificationCompat.Builder {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_DAILY_VERSE"
            data = Uri.parse("holyquran://notification/daily_verse?slot=$slotIndex")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "daily_verse")
            putExtra("daily_verse_slot", slotIndex)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            6201 + (slotIndex % 10),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isArabic) {
            when (slotIndex) {
                0 -> "🌅 آية للتفكر - الصباح"
                1 -> "☀️ آية للتفكر - الظهيرة"
                2 -> "🌆 آية للتفكر - المساء"
                else -> "🌙 آية للتفكر - الليل"
            }
        } else {
            "📖 Quran Verse Reminder"
        }
        val formattedAya = "﴿ $ayaText ﴾"

        val appLogoBitmap = getAppLogoBitmap(context)

        return NotificationCompat.Builder(context, CHANNEL_DAILY_VERSE)
            .setSmallIcon(R.drawable.ic_stat_quran)
            .apply {
                if (appLogoBitmap != null) setLargeIcon(appLogoBitmap)
            }
            .setContentTitle(title)
            .setContentText(formattedAya)
            .setSubText(surahAndVerseRef)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText("$formattedAya\n\n[$surahAndVerseRef]")
            )
    }

    fun buildAzkarNotification(
        context: Context,
        isMorning: Boolean,
        isArabic: Boolean
    ): NotificationCompat.Builder {
        val categoryKey = if (isMorning) "morning" else "evening"
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_AZKAR"
            data = Uri.parse("holyquran://notification/azkar?category=$categoryKey&quick_read=true")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "azkar")
            putExtra("azkar_category", categoryKey)
            putExtra("azkar_quick_read", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (isMorning) 6101 else 6102,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isMorning) {
            if (isArabic) "☀️ أذكار الصباح المباركة - تحصين يومك" else "☀️ Morning Adhkar - Daily Fortress"
        } else {
            if (isArabic) "🌙 أذكار المساء المباركة - حفظك ورعايتك" else "🌙 Evening Adhkar - Night Protection"
        }

        val zikrText = if (isMorning) {
            "أَصْبَحْنَا وَأَصْبَحَ المُلْكُ لِلَّهِ، وَالحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ...\n\n«اللَّهُمَّ بك أصبحت وبك أمسينا وبك نحيا وبك نموت وإليك النشور»"
        } else {
            "أَمْسَيْنَا وَأَمْسَى المُلْكُ لِلَّهِ، وَالحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ...\n\n«أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ»"
        }

        val appLogoBitmap = getAppLogoBitmap(context)

        return NotificationCompat.Builder(context, CHANNEL_AZKAR_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_quran)
            .apply {
                if (appLogoBitmap != null) setLargeIcon(appLogoBitmap)
            }
            .setContentTitle(title)
            .setContentText(zikrText.replace("\n\n", " "))
            .setSubText(if (isMorning) "أذكار الصباح" else "أذكار المساء")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText("$zikrText\n\nاضغط لقراءة الأذكار كاملة بالعداد والمشاركة 🤲")
            )
    }

    fun buildReadingReminderNotification(
        context: Context,
        lastPage: Int,
        lastSurah: String,
        isArabic: Boolean
    ): NotificationCompat.Builder {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_READER"
            data = Uri.parse("holyquran://notification/reader_reminder?page=$lastPage")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "reader")
            putExtra("reader_page", lastPage)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            6301,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isArabic) {
            "📖 تذكير بالورد اليومي لقراءة القرآن"
        } else {
            "📖 Daily Quran Reading Reminder"
        }

        val bodyText = if (isArabic) {
            "لا تنسَ وردك اليومي من القرآن الكريم! واصل القراءة من سورة $lastSurah (صفحة $lastPage) 🤲"
        } else {
            "Don't forget your daily Quran reading portion! Continue from Surah $lastSurah (Page $lastPage) 🤲"
        }

        val appLogoBitmap = getAppLogoBitmap(context)

        return NotificationCompat.Builder(context, CHANNEL_READING_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_quran)
            .apply {
                if (appLogoBitmap != null) setLargeIcon(appLogoBitmap)
            }
            .setContentTitle(title)
            .setContentText(bodyText)
            .setSubText(if (isArabic) "ورد القراءة اليومي" else "Daily Reading Portion")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText("$bodyText\n\nاضغط الآن لفتح المصحف ومتابعة القراءة.")
            )
    }

    fun buildPrayerFollowUpNotification(
        context: Context,
        prayerKey: String,
        prayerNameAr: String,
        prayerNameEn: String,
        dateString: String,
        notificationId: Int
    ): NotificationCompat.Builder {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_PRAYER_TRACKER"
            data = Uri.parse("holyquran://notification/prayer_tracker?date=$dateString")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "prayer_tracker")
            putExtra("tracker_date", dateString)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            9100 + (notificationId % 100),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // YES Action Intent
        val yesIntent = Intent(context, PrayerFollowUpReceiver::class.java).apply {
            action = PrayerFollowUpReceiver.ACTION_PRAYER_CONFIRM_YES
            putExtra(PrayerFollowUpReceiver.EXTRA_PRAYER_KEY, prayerKey)
            putExtra(PrayerFollowUpReceiver.EXTRA_PRAYER_NAME_AR, prayerNameAr)
            putExtra(PrayerFollowUpReceiver.EXTRA_DATE_STRING, dateString)
            putExtra(PrayerFollowUpReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val yesPendingIntent = PendingIntent.getBroadcast(
            context,
            9200 + (notificationId % 100),
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // NO Action Intent
        val noIntent = Intent(context, PrayerFollowUpReceiver::class.java).apply {
            action = PrayerFollowUpReceiver.ACTION_PRAYER_CONFIRM_NO
            putExtra(PrayerFollowUpReceiver.EXTRA_PRAYER_KEY, prayerKey)
            putExtra(PrayerFollowUpReceiver.EXTRA_PRAYER_NAME_AR, prayerNameAr)
            putExtra(PrayerFollowUpReceiver.EXTRA_DATE_STRING, dateString)
            putExtra(PrayerFollowUpReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val noPendingIntent = PendingIntent.getBroadcast(
            context,
            9300 + (notificationId % 100),
            noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appLogoBitmap = getAppLogoBitmap(context)

        return NotificationCompat.Builder(context, CHANNEL_PRAYER_FOLLOWUP)
            .setSmallIcon(R.drawable.ic_stat_quran)
            .apply {
                if (appLogoBitmap != null) setLargeIcon(appLogoBitmap)
            }
            .setContentTitle("🕌 هل صليت صلاة $prayerNameAr؟")
            .setContentText("اضغط نعم لتسجيل الصلاة في متابع الصلوات لنيل الأجر والمحافظة عليها في وقتها.")
            .setSubText("متابع الصلوات")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_stat_quran,
                "✅ نعم، صليت",
                yesPendingIntent
            )
            .addAction(
                R.drawable.ic_stat_quran,
                "⏳ ليس بعد",
                noPendingIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("🕌 هل صليت صلاة $prayerNameAr؟")
                    .bigText("مضى وقت على دخول أذان صلاة $prayerNameAr. هل أديت الفريضة؟\n\nاضغط «نعم» لتثبيتها في سجل صلواتك أو «ليس بعد» للتذكير بأدائها في وقتها.")
            )
    }

    fun buildPrayerMotivationNotification(
        context: Context,
        prayerKey: String,
        prayerNameAr: String,
        dateString: String
    ): NotificationCompat.Builder {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_NAVIGATE_PRAYER_TRACKER"
            data = Uri.parse("holyquran://notification/prayer_tracker?date=$dateString")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "prayer_tracker")
            putExtra("tracker_date", dateString)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            9400,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appLogoBitmap = getAppLogoBitmap(context)

        return NotificationCompat.Builder(context, CHANNEL_PRAYER_FOLLOWUP)
            .setSmallIcon(R.drawable.ic_stat_quran)
            .apply {
                if (appLogoBitmap != null) setLargeIcon(appLogoBitmap)
            }
            .setContentTitle("✨ خير الأعمال.. الصلاة على وقتها")
            .setContentText("أحب الأعمال إلى الله الصلاة على وقتها. بادر لأداء صلاة $prayerNameAr الآن 🕋")
            .setSubText("متابع الصلوات")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("✨ خير الأعمال.. الصلاة على وقتها")
                    .bigText("«سُئِلَ النبيُّ ﷺ: أيُّ العَمَلِ أحَبُّ إلى اللَّهِ؟ قالَ: الصَّلاةُ علَى وقْتِها»\n\nبارك الله فيك، لا تؤخر صلاة $prayerNameAr وبادر لأدائها الآن 🕋.")
            )
    }
}
