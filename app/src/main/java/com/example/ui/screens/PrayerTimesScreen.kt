package com.example.ui.screens

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.PrayerTime
import com.example.notification.AzanPlaybackService
import com.example.notification.PrayerNotificationScheduler
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGold
import com.example.ui.viewmodel.QuranViewModel
import com.example.util.LocationHelper
import com.example.widget.PrayerTimesWidgetProvider
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onNavigateToQibla: () -> Unit = {},
    onNavigateToEditOffsets: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {}
) {
    val context = LocalContext.current
    val prayerTimes = viewModel.getPrayerTimes()
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    var showMethodDialog by remember { mutableStateOf(false) }
    var showMadhhabDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showAzanModeDialog by remember { mutableStateOf(false) }
    var showTestNotificationDialog by remember { mutableStateOf(false) }

    // App theme colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryDarkColor = EmeraldPrimaryDark
    val goldColor = IslamicGold
    val goldAccent = GoldAccent
    val bgColor = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surface
    val headerBg = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Live countdown timer state
    var countdownText by remember { mutableStateOf("00:00:00") }
    var nextPrayerName by remember { mutableStateOf("الصلاة القادمة") }
    var nextPrayerArabicName by remember { mutableStateOf("الصلاة القادمة") }
    var nextPrayerFormattedTime by remember { mutableStateOf("") }

    // Live active Azan playback state from service
    val isServiceAzanPlaying by AzanPlaybackService.isAzanPlaying.collectAsState()
    val servicePrayerName by AzanPlaybackService.currentPrayerName.collectAsState()

    // Location Refresh State
    var isRefreshingLocation by remember { mutableStateOf(false) }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (granted) {
            isRefreshingLocation = true
            LocationHelper.getCurrentLocation(context) { lat, lng, cityName ->
                viewModel.setUserGPSLocation(lat, lng, cityName, context)
                isRefreshingLocation = false
                Toast.makeText(context, "تم تحديث الموقع بنجاح: $cityName", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "إذن تحديد الموقع غير مفعّل", Toast.LENGTH_SHORT).show()
        }
    }

    fun refreshLocation() {
        if (!LocationHelper.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        isRefreshingLocation = true
        LocationHelper.getCurrentLocation(context) { lat, lng, cityName ->
            viewModel.setUserGPSLocation(lat, lng, cityName, context)
            isRefreshingLocation = false
            Toast.makeText(context, "تم تحديث الموقع بنجاح: $cityName", Toast.LENGTH_SHORT).show()
        }
    }

    // Media Player state for testing test Azan sound
    var isPlayingTestSound by remember { mutableStateOf(false) }
    var playingSoundCode by remember { mutableStateOf<String?>(null) }
    var testMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun toggleTestAzanSound(targetSoundCode: String? = null) {
        try {
            if (isServiceAzanPlaying) {
                AzanPlaybackService.stop(context)
                if (targetSoundCode == null) {
                    Toast.makeText(context, "تم كتم صوت الأذان 🔇", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            val soundToPlay = targetSoundCode ?: userSettings.selectedAzanSound

            if (isPlayingTestSound && (playingSoundCode == soundToPlay && targetSoundCode == null)) {
                try {
                    testMediaPlayer?.stop()
                    testMediaPlayer?.release()
                } catch (e: Exception) {}
                testMediaPlayer = null
                isPlayingTestSound = false
                playingSoundCode = null
                Toast.makeText(context, "تم إيقاف صوت الآذان", Toast.LENGTH_SHORT).show()
            } else if (isPlayingTestSound && playingSoundCode == soundToPlay && targetSoundCode != null) {
                try {
                    testMediaPlayer?.stop()
                    testMediaPlayer?.release()
                } catch (e: Exception) {}
                testMediaPlayer = null
                isPlayingTestSound = false
                playingSoundCode = null
                Toast.makeText(context, "تم إيقاف المعاينة", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    testMediaPlayer?.stop()
                    testMediaPlayer?.release()
                } catch (e: Exception) {}
                testMediaPlayer = null
                isPlayingTestSound = false
                playingSoundCode = null

                if (soundToPlay == "silent_tone" || soundToPlay == "silent") {
                    Toast.makeText(context, "🔕 الوضع الصامت (بدون صوت أذان)", Toast.LENGTH_SHORT).show()
                    return
                }

                val soundResId = when (soundToPlay) {
                    "alafasy_2" -> R.raw.alafasy_2
                    "alafasy_3" -> R.raw.alafasy_3
                    "alafasy_4" -> R.raw.alafasy_4
                    "alafasy_5" -> R.raw.alafasy_5
                    "alafasy" -> R.raw.alafasy
                    "qatami" -> R.raw.qatami
                    "makkah" -> R.raw.makkah
                    "madinah" -> R.raw.madinah
                    "alaqsa" -> R.raw.alaqsa
                    "takbeer" -> R.raw.takbeer
                    else -> R.raw.alafasy
                }

                val soundLabel = when (soundToPlay) {
                    "alafasy" -> "أذان بصوت الشيخ مشاري العفاسي (1)"
                    "alafasy_2" -> "أذان بصوت الشيخ مشاري العفاسي (2)"
                    "alafasy_3" -> "أذان بصوت الشيخ مشاري العفاسي (فجر 1)"
                    "alafasy_4" -> "أذان بصوت الشيخ مشاري العفاسي (4)"
                    "alafasy_5" -> "أذان بصوت الشيخ مشاري العفاسي (فجر 2)"
                    "qatami" -> "أذان بصوت الشيخ ناصر القطامي"
                    "makkah" -> "أذان الحرم المكي الشريف"
                    "madinah" -> "أذان المسجد النبوي الشريف"
                    "alaqsa" -> "أذان المسجد الأقصى المبارك"
                    "takbeer" -> "تكبيرات الآذان"
                    else -> "أذان بصوت الشيخ مشاري العفاسي (1)"
                }

                var mp: MediaPlayer? = null

                // 1. Standard Android raw creation
                try {
                    mp = MediaPlayer.create(context, soundResId)
                } catch (e: Exception) {
                    mp = null
                }

                // 2. Fallback via Uri
                if (mp == null) {
                    try {
                        val soundUri = Uri.parse("android.resource://${context.packageName}/$soundResId")
                        mp = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build()
                            )
                            setDataSource(context, soundUri)
                            prepare()
                        }
                    } catch (e: Exception) {
                        mp = null
                    }
                }

                // 3. Fallback via openRawResourceFd
                if (mp == null) {
                    try {
                        val afd = context.resources.openRawResourceFd(soundResId)
                        if (afd != null) {
                            mp = MediaPlayer().apply {
                                setAudioAttributes(
                                    AudioAttributes.Builder()
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .build()
                                )
                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                prepare()
                            }
                            afd.close()
                        }
                    } catch (e: Exception) {
                        mp = null
                    }
                }

                if (mp != null) {
                    testMediaPlayer = mp
                    mp.setVolume(1.0f, 1.0f)
                    mp.setOnErrorListener { player, what, extra ->
                        isPlayingTestSound = false
                        playingSoundCode = null
                        try {
                            player.release()
                        } catch (e: Exception) {}
                        testMediaPlayer = null
                        true
                    }
                    mp.setOnCompletionListener { player ->
                        isPlayingTestSound = false
                        playingSoundCode = null
                        try {
                            player.release()
                        } catch (e: Exception) {}
                        testMediaPlayer = null
                    }
                    mp.start()
                    isPlayingTestSound = true
                    playingSoundCode = soundToPlay
                    Toast.makeText(context, "🔊 $soundLabel", Toast.LENGTH_SHORT).show()
                } else {
                    // 4. Fallback via RingtoneManager
                    var ringtonePlayed = false
                    try {
                        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        if (ringtoneUri != null) {
                            val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
                            if (ringtone != null) {
                                ringtone.play()
                                ringtonePlayed = true
                                isPlayingTestSound = true
                                playingSoundCode = soundToPlay
                                Toast.makeText(context, "🔊 جاري تشغيل صوت التنبيه...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 5. Ultimate Fallback via ToneGenerator
                    if (!ringtonePlayed) {
                        try {
                            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000)
                            isPlayingTestSound = true
                            playingSoundCode = soundToPlay
                            Toast.makeText(context, "🔊 جاري تشغيل نغمة التنبيه...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "تعذر تشغيل الصوت", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPlayingTestSound = false
            playingSoundCode = null
            Toast.makeText(context, "خطأ أثناء تشغيل الصوت: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            testMediaPlayer?.stop()
            testMediaPlayer?.release()
            testMediaPlayer = null
        }
    }

    val enabledPrayers = remember(userSettings.enabledPrayersJson) {
        userSettings.enabledPrayersJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    val silentPrayers = remember(userSettings.silentPrayersJson) {
        userSettings.silentPrayersJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun isPrayerEnabled(prayerNameEn: String): Boolean {
        return enabledPrayers.any { it.equals(prayerNameEn, ignoreCase = true) }
    }

    fun isPrayerAzanEnabled(prayerNameEn: String): Boolean {
        return !silentPrayers.any { it.equals(prayerNameEn, ignoreCase = true) }
    }

    fun togglePrayerNotif(prayerNameEn: String, isEnabled: Boolean) {
        val updatedList = enabledPrayers.filter { !it.equals(prayerNameEn, ignoreCase = true) }.toMutableList()
        if (isEnabled) {
            updatedList.add(prayerNameEn)
        }
        viewModel.updatePrayerAlerts(
            enabledPrayersJson = updatedList.joinToString(","),
            silentPrayersJson = userSettings.silentPrayersJson,
            context = context
        )
    }

    fun togglePrayerAzanSound(prayerNameEn: String, isAzanEnabled: Boolean, prayerNameAr: String) {
        val updatedSilentList = silentPrayers.filter { !it.equals(prayerNameEn, ignoreCase = true) }.toMutableList()
        if (!isAzanEnabled) {
            updatedSilentList.add(prayerNameEn)
        }
        viewModel.updatePrayerAlerts(
            enabledPrayersJson = userSettings.enabledPrayersJson,
            silentPrayersJson = updatedSilentList.joinToString(","),
            context = context
        )
        val msg = if (isAzanEnabled) "🔊 تم تفعيل صوت الأذان لصلاة $prayerNameAr" else "🔕 تم إيقاف صوت الأذان (تنبيه صامت) لصلاة $prayerNameAr"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        if (notifGranted) {
            PrayerNotificationScheduler.schedulePrayerNotifications(context, viewModel.getPrayerTimes())
        }
    }

    LaunchedEffect(prayerTimes) {
        if (LocationHelper.hasNotificationPermission(context)) {
            PrayerNotificationScheduler.schedulePrayerNotifications(context, prayerTimes)
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
        PrayerTimesWidgetProvider.sendUpdateBroadcast(context)
    }

    // Calculate countdown timer to next prayer
    LaunchedEffect(prayerTimes) {
        while (true) {
            val now = Calendar.getInstance()
            val currentMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            var targetPrayer: PrayerTime? = null
            for (pt in prayerTimes) {
                if (pt.nameEnglish == "Sunrise") continue
                val parts = pt.timeFormatted.replace("[^0-9:]".toRegex(), "").split(":")
                if (parts.size >= 2) {
                    var h = parts[0].toIntOrNull() ?: 0
                    val m = parts[1].toIntOrNull() ?: 0
                    val isPm = pt.timeFormatted.contains("PM") || pt.timeFormatted.contains("م")
                    val isAm = pt.timeFormatted.contains("AM") || pt.timeFormatted.contains("ص")
                    if (isPm && h < 12) h += 12
                    else if (isAm && h == 12) h = 0

                    val pMins = h * 60 + m
                    if (pMins > currentMins) {
                        targetPrayer = pt
                        break
                    }
                }
            }

            if (targetPrayer == null && prayerTimes.isNotEmpty()) {
                targetPrayer = prayerTimes.firstOrNull { it.nameEnglish == "Fajr" } ?: prayerTimes.first()
            }

            if (targetPrayer != null) {
                nextPrayerName = targetPrayer.nameEnglish
                nextPrayerArabicName = targetPrayer.nameArabic
                nextPrayerFormattedTime = targetPrayer.timeFormatted

                val parts = targetPrayer.timeFormatted.replace("[^0-9:]".toRegex(), "").split(":")
                if (parts.size >= 2) {
                    var h = parts[0].toIntOrNull() ?: 0
                    val m = parts[1].toIntOrNull() ?: 0
                    val isPm = targetPrayer.timeFormatted.contains("PM") || targetPrayer.timeFormatted.contains("م")
                    val isAm = targetPrayer.timeFormatted.contains("AM") || targetPrayer.timeFormatted.contains("ص")
                    if (isPm && h < 12) h += 12
                    else if (isAm && h == 12) h = 0

                    val targetCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, h)
                        set(Calendar.MINUTE, m)
                        set(Calendar.SECOND, 0)
                    }
                    if (targetCal.before(now)) {
                        targetCal.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    val diffMs = targetCal.timeInMillis - now.timeInMillis
                    if (diffMs > 0) {
                        val hrs = diffMs / (1000 * 60 * 60)
                        val mins = (diffMs % (1000 * 60 * 60)) / (1000 * 60)
                        val secs = (diffMs % (1000 * 60)) / 1000
                        countdownText = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
                    } else {
                        countdownText = "00:00:00"
                    }
                }
            }

            delay(1000L)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isArabic) "مواقيت الصلاة" else "Prayer Times",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("prayer_times_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { refreshLocation() },
                            modifier = Modifier.testTag("refresh_location_topbar_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "تحديث الموقع",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(
                            onClick = onNavigateToTracker,
                            modifier = Modifier.testTag("prayer_times_tracker_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = if (isArabic) "متابع الصلوات" else "Tracker",
                                tint = GoldAccent
                            )
                        }
                        IconButton(
                            onClick = onNavigateToQibla,
                            modifier = Modifier.testTag("qibla_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "اتجاه القبلة",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryColor,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            containerColor = bgColor
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(bgColor),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // --- ACTIVE AZAN PLAYBACK BANNER (IF AZAN IS CURRENTLY PLAYING) ---
                if (isServiceAzanPlaying) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3D2F)),
                            border = BorderStroke(1.5.dp, goldColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = goldAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "🕌 يرفع الآن أذان ${servicePrayerName ?: "الصلاة"}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "اضغط الزر أو أزرار الصوت لكتم الأذان",
                                            fontSize = 11.sp,
                                            color = goldAccent
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        AzanPlaybackService.stop(context)
                                        Toast.makeText(context, "تم كتم صوت الأذان 🔇", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD32F2F),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("كتم الأذان", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // --- HERO BANNER CARD (ELEGANT UPCOMING PRAYER HIGHLIGHT) ---
                item {
                    val locData = LocationHelper.getSavedLocationAndSettings(context)
                    val timeParts = countdownText.split(":")
                    val hoursStr = timeParts.getOrNull(0) ?: "00"
                    val minsStr = timeParts.getOrNull(1) ?: "00"
                    val secsStr = timeParts.getOrNull(2) ?: "00"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = primaryColor),
                        border = BorderStroke(1.5.dp, goldColor.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(primaryColor, primaryDarkColor)
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Top Header Row inside Hero Banner: Location & Next Prayer Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Location Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .clickable { refreshLocation() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("location_refresh_badge")
                                    ) {
                                        if (isRefreshingLocation) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = goldColor,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "تحديث الموقع",
                                                tint = goldColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = locData.cityName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "تحديث",
                                            tint = goldAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Next Prayer Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(goldColor.copy(alpha = 0.25f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = goldAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "الصلاة القادمة",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = goldAccent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Upcoming Prayer Title & Scheduled Time
                                Text(
                                    text = "صلاة $nextPrayerArabicName",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )

                                if (nextPrayerFormattedTime.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "الساعة $nextPrayerFormattedTime",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Countdown Subtitle
                                Text(
                                    text = "المتبقي على أذان الصلاة:",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Styled Clock Blocks (Hours : Minutes : Seconds)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ClockBlock(value = hoursStr, label = "ساعة", goldAccent = goldAccent)
                                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = goldAccent)
                                    ClockBlock(value = minsStr, label = "دقيقة", goldAccent = goldAccent)
                                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = goldAccent)
                                    ClockBlock(value = secsStr, label = "ثانية", goldAccent = goldAccent)
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Action Buttons Row (Qibla, Test Sound, Test Notification)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                                ) {
                                    OutlinedButton(
                                        onClick = onNavigateToQibla,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, goldColor),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = goldColor),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("hero_qibla_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CompassCalibration,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "القبلة",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = { toggleTestAzanSound() },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = goldColor,
                                            contentColor = Color(0xFF1E3A2B)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("hero_test_sound_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingTestSound) Icons.Default.Stop else Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isPlayingTestSound) "إيقاف" else "الآذان",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { showTestNotificationDialog = true },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, goldColor.copy(alpha = 0.85f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = goldColor),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("hero_test_notif_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "تجربة الإشعار",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- SECTION TITLE: جدول مواقيت الصلاة ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مواقيت الصلاة اليوم",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = SimpleDateFormat("EEEE، d MMMM", Locale("ar")).format(Date()),
                            fontSize = 14.sp,
                            color = onSurfaceVariantColor
                        )
                    }
                }

                // --- PRAYER TIMES CARDS (INCLUDING SUNRISE WITH NOTIFICATION TOGGLE) ---
                items(prayerTimes.size) { index ->
                    val prayer = prayerTimes[index]
                    val isNext = prayer.nameEnglish.equals(nextPrayerName, ignoreCase = true)
                    val isNotifEnabled = isPrayerEnabled(prayer.nameEnglish)
                    val isAzanSoundOn = isPrayerAzanEnabled(prayer.nameEnglish)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNext) Color(0xFFEEF7F2) else cardBg
                        ),
                        border = if (isNext) BorderStroke(1.5.dp, goldColor) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 4.dp else 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Prayer Icon, Arabic Name, and Formatted Time
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (isNext) primaryColor else headerBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (prayer.nameEnglish.lowercase()) {
                                        "fajr" -> Icons.Default.WbTwilight
                                        "sunrise" -> Icons.Default.WbSunny
                                        "dhuhr" -> Icons.Default.LightMode
                                        "asr" -> Icons.Default.WbSunny
                                        "maghrib" -> Icons.Default.NightsStay
                                        else -> Icons.Default.Bedtime
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isNext) goldColor else primaryColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.Start) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = prayer.nameArabic,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNext) primaryColor else onSurfaceColor
                                        )
                                        if (isNext) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = primaryColor.copy(alpha = 0.15f),
                                                contentColor = primaryDarkColor
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp),
                                                        tint = primaryDarkColor
                                                    )
                                                    Text(
                                                        text = "الصلاة القادمة",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = primaryDarkColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = prayer.timeFormatted,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isNext) primaryDarkColor else onSurfaceVariantColor
                                    )
                                }
                            }

                            // Notification & Azan Sound Toggle Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isNotifEnabled) {
                                    IconButton(
                                        onClick = {
                                            togglePrayerAzanSound(prayer.nameEnglish, !isAzanSoundOn, prayer.nameArabic)
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("azan_sound_toggle_${prayer.nameEnglish}")
                                    ) {
                                        Icon(
                                            imageVector = if (isAzanSoundOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                            contentDescription = if (isAzanSoundOn) "صوت الأذان مفعل" else "تنبيه صامت (بدون أذان)",
                                            tint = if (isAzanSoundOn) primaryColor else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Switch(
                                    checked = isNotifEnabled,
                                    onCheckedChange = { togglePrayerNotif(prayer.nameEnglish, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = primaryColor,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color.LightGray
                                    ),
                                    modifier = Modifier.testTag("notif_switch_${prayer.nameEnglish}")
                                )
                            }
                        }
                    }
                }

                // --- SECTION HEADER: إدارة إعدادات الصلاة ---
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBg)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "إدارة إعدادات الصلاة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = onSurfaceColor,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                }

                // 1. طريقة الحساب (Calculation Method)
                item {
                    val methodLabel = when (userSettings.calculationMethod) {
                        "EGYPT" -> "الهيئة المصرية العامة للمساحة"
                        "MAKKAH" -> "جامعة أم القرى - مكة المكرمة"
                        "MWL" -> "رابطة العالم الإسلامي"
                        "ISNA" -> "الجمعية الإسلامية لأمريكا الشمالية"
                        "KARACHI" -> "جامعة العلوم الإسلامية بكراتشي"
                        else -> "الهيئة المصرية العامة للمساحة"
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardBg)
                                .clickable { showMethodDialog = true }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "طريقة الحساب",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = methodLabel,
                                    fontSize = 15.sp,
                                    color = onSurfaceVariantColor
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = onSurfaceVariantColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    }
                }

                // 2. المذهب الفقهي (Juristic Method / Asr)
                item {
                    val madhhabLabel = if (userSettings.madhhab == "Hanafi") "المذهب الحنفي" else "جعفري/شافعي/مالكي/حنبلي"

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardBg)
                                .clickable { showMadhhabDialog = true }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "المذهب الفقهي",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = madhhabLabel,
                                    fontSize = 15.sp,
                                    color = onSurfaceVariantColor
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = onSurfaceVariantColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    }
                }

                // 3. تعديل أوقات الصلاة (Adjust Prayer Times Offsets)
                item {
                    val offsetsSummary = "${userSettings.fajrOffsetMins},${userSettings.dhuhrOffsetMins},${userSettings.asrOffsetMins},${userSettings.maghribOffsetMins},${userSettings.ishaOffsetMins}"

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardBg)
                                .clickable { onNavigateToEditOffsets() }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تعديل أوقات الصلاة",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = offsetsSummary,
                                    fontSize = 15.sp,
                                    color = onSurfaceVariantColor
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = onSurfaceVariantColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    }
                }

                // 4. صوت تنبيه الآذان (Adhan Sound)
                item {
                    val soundLabel = when (userSettings.selectedAzanSound) {
                        "alafasy" -> "أذان بصوت الشيخ مشاري العفاسي (1)"
                        "alafasy_2" -> "أذان بصوت الشيخ مشاري العفاسي (2)"
                        "alafasy_3" -> "أذان بصوت الشيخ مشاري العفاسي (فجر 1)"
                        "alafasy_4" -> "أذان بصوت الشيخ مشاري العفاسي (4)"
                        "alafasy_5" -> "أذان بصوت الشيخ مشاري العفاسي (فجر 2)"
                        "qatami" -> "أذان بصوت الشيخ ناصر القطامي"
                        "makkah" -> "أذان الحرم المكي الشريف"
                        "madinah" -> "أذان المسجد النبوي الشريف"
                        "alaqsa" -> "أذان المسجد الأقصى المبارك"
                        "takbeer" -> "تكبيرات الآذان"
                        "silent_tone" -> "تنبيه هادئ (بدون أذان)"
                        else -> "أذان بصوت الشيخ مشاري العفاسي (1)"
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardBg)
                                .clickable { showSoundDialog = true }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "صوت تنبيه الآذان",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = soundLabel,
                                    fontSize = 15.sp,
                                    color = onSurfaceVariantColor
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = onSurfaceVariantColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    }
                }

                // 5. نمط تشغيل الأذان (Azan Mode: Full vs Takbeer Only)
                item {
                    val azanModeLabel = if (userSettings.takbeerAzanOnly) "التكبيرات فقط (~30 ثانية)" else "أذان كامل"
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardBg)
                                .clickable { showAzanModeDialog = true }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "نمط تشغيل الأذان",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                Text(
                                    text = if (userSettings.takbeerAzanOnly) "تشغيل تكبيرات الأذان فقط لجميع الأصوات" else "تشغيل الأذان كاملاً حتى نهايته",
                                    fontSize = 12.sp,
                                    color = onSurfaceVariantColor
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = azanModeLabel,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (userSettings.takbeerAzanOnly) IslamicGold else primaryColor
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = onSurfaceVariantColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    }
                }

                // 6. اختبار إشعار وأذان الصلاة (Test Prayer Notifications)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardBg)
                                .clickable { showTestNotificationDialog = true }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "اختبار إشعار وأذان الصلاة",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                    Text(
                                        text = "إرسال إشعار فوري وتجربة صوت الأذان في الخلفية",
                                        fontSize = 12.sp,
                                        color = onSurfaceVariantColor
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = onSurfaceVariantColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    }
                }

                // Button: إعادة ضبط الإعدادات (Reset Settings)
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.resetPrayerSettings(context)
                                Toast.makeText(context, "تمت إعادة ضبط إعدادات الصلاة بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "إعادة ضبط الإعدادات",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                }
            }
        }

        // Dialog: Calculation Method Selection
        if (showMethodDialog) {
            val methods = listOf(
                "EGYPT" to "الهيئة المصرية العامة للمساحة",
                "MAKKAH" to "جامعة أم القرى - مكة المكرمة",
                "MWL" to "رابطة العالم الإسلامي",
                "ISNA" to "الجمعية الإسلامية لأمريكا الشمالية",
                "KARACHI" to "جامعة العلوم الإسلامية بكراتشي"
            )
            AlertDialog(
                onDismissRequest = { showMethodDialog = false },
                title = { Text("طريقة حساب مواقيت الصلاة", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        methods.forEach { (code, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updatePrayerMethod(code, context)
                                        showMethodDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                if (code == userSettings.calculationMethod) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = primaryColor)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMethodDialog = false }) {
                        Text("إلغاء", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Dialog: Madhhab Selection
        if (showMadhhabDialog) {
            val madhhabs = listOf(
                "Shafi" to "جعفري/شافعي/مالكي/حنبلي (الجمهور)",
                "Hanafi" to "المذهب الحنفي (تأخير صلاة العصر)"
            )
            AlertDialog(
                onDismissRequest = { showMadhhabDialog = false },
                title = { Text("المذهب الفقهي لحساب صلاة العصر", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        madhhabs.forEach { (code, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateMadhhab(code, context)
                                        showMadhhabDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                if (code == userSettings.madhhab) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = primaryColor)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMadhhabDialog = false }) {
                        Text("إلغاء", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Dialog: Sound Selection with Test Playback Feature
        if (showSoundDialog) {
            val sounds = listOf(
                "alafasy" to "أذان بصوت الشيخ مشاري العفاسي (1) 🎙️",
                "alafasy_2" to "أذان بصوت الشيخ مشاري العفاسي (2) 🎙️",
                "alafasy_3" to "أذان بصوت الشيخ مشاري العفاسي (فجر 1) 🎙️",
                "alafasy_4" to "أذان بصوت الشيخ مشاري العفاسي (4) 🎙️",
                "alafasy_5" to "أذان بصوت الشيخ مشاري العفاسي (فجر 2) 🎙️",
                "qatami" to "أذان بصوت الشيخ ناصر القطامي 🎙️",
                "makkah" to "أذان الحرم المكي الشريف 🕋",
                "madinah" to "أذان المسجد النبوي الشريف 🕌",
                "alaqsa" to "أذان المسجد الأقصى المبارك 🕌",
                "takbeer" to "تكبيرات الآذان 🔊",
                "silent_tone" to "تنبيه صامت (بدون أذان) 🔕"
            )
            AlertDialog(
                onDismissRequest = {
                    if (isPlayingTestSound) toggleTestAzanSound()
                    showSoundDialog = false
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("صوت تنبيه الآذان", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { toggleTestAzanSound() }) {
                            Icon(
                                imageVector = if (isPlayingTestSound) Icons.Default.StopCircle else Icons.Default.PlayCircleFilled,
                                contentDescription = "اختبار الصوت",
                                tint = primaryColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Segmented Control for Full Azan vs Takbeer Only
                        Card(
                            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🕌 نمط تشغيل الأذان (ينطبق على جميع الأصوات):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Full Azan Button
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (!userSettings.takbeerAzanOnly) primaryColor else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.updateTakbeerAzanSetting(false)
                                                Toast.makeText(context, "تم ضبط تشغيل الأذان كاملاً", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "🕌 أذان كامل",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (!userSettings.takbeerAzanOnly) Color.White else primaryColor
                                            )
                                        }
                                    }

                                    // Takbeer Only Button
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (userSettings.takbeerAzanOnly) primaryColor else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.updateTakbeerAzanSetting(true)
                                                Toast.makeText(context, "تم ضبط التكبيرات فقط لجميع أصوات الأذان", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "🔊 التكبيرات فقط",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (userSettings.takbeerAzanOnly) Color.White else primaryColor
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (userSettings.takbeerAzanOnly)
                                        "• يتم تشغيل تكبيرات الأذان فقط (~30 ثانية) لكافة أصوات المشايخ والحرمين"
                                    else
                                        "• يتم تشغيل الأذان كاملاً حتى نهايته بصوت الشيخ أو المسجد المختار",
                                    fontSize = 11.sp,
                                    color = onSurfaceVariantColor,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Preview Sound Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleTestAzanSound() }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingTestSound) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                                Text(
                                    text = if (isPlayingTestSound) "جاري تشغيل المعاينة (اضغط للإيقاف)..." else "اضغط هنا لمعاينة الصوت المختار",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        sounds.forEach { (code, label) ->
                            val isSelected = userSettings.selectedAzanSound == code
                            val isThisRowPlaying = isPlayingTestSound && (playingSoundCode == code || (playingSoundCode == null && isSelected))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        viewModel.updateSelectedAzanSound(code)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateSelectedAzanSound(code)
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                    )
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                                if (code != "silent_tone") {
                                    IconButton(
                                        onClick = {
                                            toggleTestAzanSound(code)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isThisRowPlaying) Icons.Default.StopCircle else Icons.Default.PlayCircleFilled,
                                            contentDescription = "معاينة",
                                            tint = if (isThisRowPlaying) primaryColor else onSurfaceVariantColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeMute,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "💡 يمكنك كتم صوت الأذان في أي وقت بالضغط على أزرار الصوت (رفع/خفض) أو زر الطاقة أو من الإشعار.\n💡 خيار (التكبيرات فقط) ينطبق على كافة أصوات المشايخ والحرمين.",
                                    fontSize = 12.sp,
                                    color = onSurfaceVariantColor,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isPlayingTestSound) toggleTestAzanSound()
                            showSoundDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Dialog: Azan Playback Mode Selection (Full vs Takbeer Only)
        if (showAzanModeDialog) {
            AlertDialog(
                onDismissRequest = { showAzanModeDialog = false },
                title = {
                    Text(
                        text = "نمط تشغيل الأذان",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = primaryColor
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "اختر مدة ونمط رفع صوت الأذان عند حلول وقت الصلاة لجميع الأصوات:",
                            fontSize = 13.sp,
                            color = onSurfaceVariantColor
                        )

                        // Option 1: Full Azan
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (!userSettings.takbeerAzanOnly) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (!userSettings.takbeerAzanOnly) 2.dp else 1.dp,
                                    color = if (!userSettings.takbeerAzanOnly) primaryColor else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.updateTakbeerAzanSetting(false)
                                    Toast.makeText(context, "تم اختيار: أذان كامل", Toast.LENGTH_SHORT).show()
                                    showAzanModeDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = !userSettings.takbeerAzanOnly,
                                    onClick = {
                                        viewModel.updateTakbeerAzanSetting(false)
                                        Toast.makeText(context, "تم اختيار: أذان كامل", Toast.LENGTH_SHORT).show()
                                        showAzanModeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🕌 أذان كامل (Full Azan)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = primaryColor
                                    )
                                    Text(
                                        text = "تشغيل الأذان كاملاً حتى نهايته بصوت الشيخ أو المسجد المختار",
                                        fontSize = 12.sp,
                                        color = onSurfaceVariantColor
                                    )
                                }
                            }
                        }

                        // Option 2: Takbeer Only
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (userSettings.takbeerAzanOnly) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (userSettings.takbeerAzanOnly) 2.dp else 1.dp,
                                    color = if (userSettings.takbeerAzanOnly) primaryColor else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.updateTakbeerAzanSetting(true)
                                    Toast.makeText(context, "تم اختيار: التكبيرات فقط", Toast.LENGTH_SHORT).show()
                                    showAzanModeDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = userSettings.takbeerAzanOnly,
                                    onClick = {
                                        viewModel.updateTakbeerAzanSetting(true)
                                        Toast.makeText(context, "تم اختيار: التكبيرات فقط", Toast.LENGTH_SHORT).show()
                                        showAzanModeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🔊 التكبيرات فقط (Takbeer Only)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = primaryColor
                                    )
                                    Text(
                                        text = "تشغيل تكبيرات الأذان فقط (~30 ثانية) لجميع الأصوات والمشايخ",
                                        fontSize = 12.sp,
                                        color = onSurfaceVariantColor
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAzanModeDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("تم", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Dialog: Test Prayer Notification & Azan Sound
        if (showTestNotificationDialog) {
            val currentSoundLabel = when (userSettings.selectedAzanSound) {
                "alafasy" -> "الشيخ مشاري العفاسي (1)"
                "alafasy_2" -> "الشيخ مشاري العفاسي (2)"
                "alafasy_3" -> "الشيخ مشاري العفاسي (فجر 1)"
                "alafasy_4" -> "الشيخ مشاري العفاسي (4)"
                "alafasy_5" -> "الشيخ مشاري العفاسي (فجر 2)"
                "qatami" -> "الشيخ ناصر القطامي"
                "makkah" -> "أذان الحرم المكي الشريف"
                "madinah" -> "أذان المسجد النبوي الشريف"
                "alaqsa" -> "أذان المسجد الأقصى المبارك"
                "takbeer" -> "تكبيرات الآذان"
                "silent_tone", "silent" -> "الوضع الصامت (بدون صوت)"
                else -> "الشيخ مشاري العفاسي (1)"
            }

            AlertDialog(
                onDismissRequest = { showTestNotificationDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "اختبار إشعار وأذان الصلاة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = primaryColor
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "يمكنك اختبار وصول إشعار دخول وقت الصلاة وسماع صوت الأذان المختار فوراً أو بمهلة 5 ثوانٍ لتجربته خارج التطبيق.",
                            fontSize = 13.sp,
                            color = onSurfaceVariantColor,
                            lineHeight = 18.sp
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "🔊 الصوت المختار: $currentSoundLabel",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                 Text(
                                    text = "⏱️ نمط التشغيل: ${if (userSettings.takbeerAzanOnly) "التكبيرات فقط (~30 ثانية)" else "أذان كامل"}",
                                    fontSize = 12.sp,
                                    color = onSurfaceVariantColor
                                )
                                Text(
                                    text = "🔕 مستوى الصوت: يلتزم بمستوى صوت الهاتف ويكتم عند الصامت",
                                    fontSize = 12.sp,
                                    color = onSurfaceVariantColor
                                )
                            }
                        }

                        // Button 1: Instant Test
                        Button(
                            onClick = {
                                showTestNotificationDialog = false
                                PrayerNotificationScheduler.scheduleTestNotification(context, delaySeconds = 0)
                                Toast.makeText(context, "تم إرسال إشعار وأذان التجربة الآن 🕌", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختبار فوري (الآن)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Button 2: 5-second delayed test
                        OutlinedButton(
                            onClick = {
                                showTestNotificationDialog = false
                                PrayerNotificationScheduler.scheduleTestNotification(context, delaySeconds = 5)
                                Toast.makeText(context, "⏳ سيصلك إشعار وأذان الصلاة بعد 5 ثوانٍ، يمكنك الخروج من التطبيق لتجربته في الخلفية", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, primaryColor)
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختبار بعد 5 ثوانٍ (لتجربة الخلفية)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTestNotificationDialog = false }) {
                        Text("إلغاء", color = onSurfaceVariantColor)
                    }
                }
            )
        }
    }
}

@Composable
private fun ClockBlock(value: String, label: String, goldAccent: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(58.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.25f))
                .border(1.dp, goldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = goldAccent,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium
        )
    }
}
