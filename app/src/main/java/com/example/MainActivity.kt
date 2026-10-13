package com.example

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.notification.AzanPlaybackService
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.domain.model.AppLanguage
import com.example.ui.theme.HolyQuranTheme
import com.example.ui.viewmodel.QuranViewModel

data class NavTarget(
    val eventId: Long = System.currentTimeMillis(),
    val route: String,
    val azkarCategory: String = "morning",
    val azkarQuickRead: Boolean = false,
    val prayerName: String? = null,
    val readerPage: Int? = null,
    val khatmaCode: String? = null
)

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val navTargetState = mutableStateOf<NavTarget?>(null)

    private fun parseNavTarget(intent: android.content.Intent?): NavTarget? {
        if (intent == null) return null
        val uriData = intent.data
        val host = uriData?.host
        val isAppHost = host == "holyquranapp.web.app" || host == "holy-quran-app-live.web.app" || host == "quranapp.link" || uriData?.scheme == "holyquran"
        val pathCode = if (uriData?.path?.startsWith("/khatma/") == true) {
            uriData.path?.substringAfter("/khatma/")?.trim()
        } else if (uriData?.scheme == "holyquran" && uriData.host == "khatma" && !uriData.path.isNullOrBlank()) {
            uriData.path?.trim('/')?.trim()
        } else null
        val khatmaCodeParam = uriData?.getQueryParameter("code")
            ?: uriData?.getQueryParameter("data")
            ?: pathCode
            ?: (if (uriData?.scheme == "holyquran" && host != null && host.length in 4..16 && !host.contains(".") && host != "khatma") host else null)

        val appLinkRoute = if (isAppHost && (!khatmaCodeParam.isNullOrBlank() || uriData?.path?.startsWith("/khatma") == true || uriData?.host == "khatma")) {
            "group_khatma"
        } else null

        val actionRoute = when (intent.action) {
            "com.example.ACTION_NAVIGATE_PRAYER_TIMES" -> "prayer_times"
            "com.example.ACTION_NAVIGATE_PRAYER_TRACKER" -> "prayer_tracker"
            "com.example.ACTION_NAVIGATE_AZKAR",
            "com.example.ACTION_NAVIGATE_AZKAR_MORNING",
            "com.example.ACTION_NAVIGATE_AZKAR_EVENING",
            "com.example.ACTION_NAVIGATE_AZKAR_TASBIH" -> "azkar"
            "com.example.ACTION_NAVIGATE_DAILY_VERSE" -> "daily_verse"
            "com.example.ACTION_NAVIGATE_READING_ANALYTICS" -> "reading_analytics"
            "com.example.ACTION_NAVIGATE_READER" -> "reader"
            else -> null
        }

        val uriRoute = when {
            intent.data?.host in listOf("navigate", "notification", "widget", "service") -> {
                val path = intent.data?.path?.removePrefix("/")
                when (path) {
                    "azan_playback" -> "prayer_times"
                    "reader_reminder" -> "reader"
                    else -> path
                }
            }
            else -> null
        }

        val route = intent.getStringExtra("navigate_to")
            ?: appLinkRoute
            ?: actionRoute
            ?: uriRoute
        if (route.isNullOrBlank()) return null

        val actionCategory = when (intent.action) {
            "com.example.ACTION_NAVIGATE_AZKAR_MORNING" -> "morning"
            "com.example.ACTION_NAVIGATE_AZKAR_EVENING" -> "evening"
            "com.example.ACTION_NAVIGATE_AZKAR_TASBIH" -> "daily"
            else -> null
        }

        val azkarCategory = intent.getStringExtra("azkar_category")
            ?: actionCategory
            ?: intent.data?.getQueryParameter("category")
            ?: "morning"

        val actionQuickRead = when (intent.action) {
            "com.example.ACTION_NAVIGATE_AZKAR_MORNING",
            "com.example.ACTION_NAVIGATE_AZKAR_EVENING" -> true
            else -> false
        }

        val azkarQuickRead = intent.getBooleanExtra("azkar_quick_read", false)
            || actionQuickRead
            || intent.data?.getQueryParameter("quick_read") == "true"

        val prayerName = intent.getStringExtra("prayer_name_ar")
            ?: intent.getStringExtra("prayer_name")
            ?: intent.data?.getQueryParameter("prayer")

        val readerPage = intent.getIntExtra("reader_page", -1).takeIf { it > 0 }
            ?: intent.data?.getQueryParameter("page")?.toIntOrNull()

        val khatmaCode = khatmaCodeParam ?: intent.getStringExtra("khatma_code")

        return NavTarget(
            eventId = System.currentTimeMillis(),
            route = route,
            azkarCategory = azkarCategory,
            azkarQuickRead = azkarQuickRead,
            prayerName = prayerName,
            readerPage = readerPage,
            khatmaCode = khatmaCode
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityStartTime = System.currentTimeMillis()
        Log.d(TAG, " MainActivity.onCreate: Starting activity lifecycle initialization")

        try {
            enableEdgeToEdge()
            configureLockScreenDisplay()
            val target = parseNavTarget(intent)
            navTargetState.value = target
            Log.d(TAG, " MainActivity.onCreate: Parsed initial nav target: $target")

            setContent {
                HolyQuranApp(navTarget = navTargetState.value)
            }
            Log.d(TAG, " MainActivity.onCreate: setContent executed successfully in ${System.currentTimeMillis() - activityStartTime}ms")
        } catch (e: Throwable) {
            Log.e(TAG, " Fatal error in MainActivity.onCreate during setup", e)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = parseNavTarget(intent)
        if (target != null) {
            Log.d(TAG, " MainActivity.onNewIntent: Received nav target $target")
            navTargetState.value = target
            configureLockScreenDisplay()
        }
    }

    private fun configureLockScreenDisplay() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error configuring lock screen flags", e)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, " MainActivity.onStart: Activity becoming visible on thread [${Thread.currentThread().name}]")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, " MainActivity.onResume: Activity in foreground and interactive on thread [${Thread.currentThread().name}]")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, " MainActivity.onPause: Activity pausing")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, " MainActivity.onStop: Activity no longer visible")
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            if (AzanPlaybackService.isAzanPlaying.value) {
                when (keyCode) {
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEvent.KEYCODE_VOLUME_MUTE,
                    KeyEvent.KEYCODE_HEADSETHOOK,
                    KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_STOP,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        Log.d(TAG, "Hardware key ($keyCode) dispatched during Azan playback -> Muting Azan")
                        AzanPlaybackService.stop(this)
                        Toast.makeText(this, "تم كتم صوت الأذان 🔇", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (AzanPlaybackService.isAzanPlaying.value) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_MUTE,
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_STOP,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    Log.d(TAG, "Hardware key ($keyCode) pressed during Azan playback -> Muting Azan")
                    AzanPlaybackService.stop(this)
                    Toast.makeText(this, "تم كتم صوت الأذان 🔇", Toast.LENGTH_SHORT).show()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, " MainActivity.onDestroy: Activity destroying")
    }
}

@Composable
fun HolyQuranApp(
    navTarget: NavTarget? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: QuranViewModel = viewModel()
    val userSettings by viewModel.userSettings.collectAsState()

    var currentAzkarCategory by remember { mutableStateOf(navTarget?.azkarCategory ?: "morning") }
    var currentAzkarQuickRead by remember { mutableStateOf(navTarget?.azkarQuickRead ?: false) }
    var pendingUpdateInfo by remember { mutableStateOf<com.example.data.provider.AppUpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        Log.d("HolyQuranApp", " Composition Root: HolyQuranApp mounted. Checking for remote updates...")
        com.example.data.provider.AppUpdateManager.checkForUpdate { updateInfo ->
            if (updateInfo != null && updateInfo.isUpdateAvailable) {
                pendingUpdateInfo = updateInfo
            }
        }
    }

    HolyQuranTheme(themeMode = userSettings.effectiveThemeMode()) {
        val layoutDirection = if (userSettings.languageCode == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            val navController = rememberNavController()

            pendingUpdateInfo?.let { updateInfo ->
                AlertDialog(
                    onDismissRequest = {
                        if (!updateInfo.forceUpdate) {
                            pendingUpdateInfo = null
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                            contentDescription = null,
                            tint = com.example.ui.theme.EmeraldPrimary
                        )
                    },
                    title = {
                        Text(
                            text = if (userSettings.languageCode == "ar") "يتوفر تحديث جديد لـ تطبيق القرآن الكريم 🚀" else "New App Update Available 🚀",
                            fontFamily = com.example.ui.theme.TajawalFontFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (userSettings.languageCode == "ar") "الإصدار الحالي: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})\nالإصدار الجديد: v${updateInfo.latestVersionName} (Build ${updateInfo.latestVersionCode})" else "Current: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})\nNew: v${updateInfo.latestVersionName} (Build ${updateInfo.latestVersionCode})",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = updateInfo.getLocalizedNotes(userSettings.languageCode.startsWith("ar", ignoreCase = true)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (userSettings.languageCode == "ar") "اضغط على زر 'تحديث الآن' لتنزيل وتثبيت التحديث مباشرة." else "Click 'Update Now' to download and install directly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                com.example.data.provider.AppUpdateManager.startApkDownload(context, updateInfo.apkDownloadUrl)
                                if (!updateInfo.forceUpdate) {
                                    pendingUpdateInfo = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.EmeraldPrimary)
                        ) {
                            Text(if (userSettings.languageCode == "ar") "تحديث الآن 🚀" else "Update Now 🚀")
                        }
                    },
                    dismissButton = {
                        if (!updateInfo.forceUpdate) {
                            TextButton(onClick = { pendingUpdateInfo = null }) {
                                Text(if (userSettings.languageCode == "ar") "تذكيري لاحقاً" else "Remind Later")
                            }
                        }
                    }
                )
            }

            LaunchedEffect(navTarget?.eventId) {
                val target = navTarget ?: return@LaunchedEffect
                if (target.route.isNotBlank() && target.route != "splash") {
                    currentAzkarCategory = target.azkarCategory
                    currentAzkarQuickRead = target.azkarQuickRead

                    if (target.readerPage != null && target.readerPage > 0) {
                        viewModel.jumpToPage(target.readerPage)
                    }

                    try {
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute != null && currentRoute != "splash") {
                            if (currentRoute != target.route) {
                                navController.navigate(target.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e("HolyQuranApp", "Error dynamically navigating to ${target.route}", e)
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("splash") {
                    SplashScreen(
                        onSplashFinished = {
                            val destination = if (navTarget != null && navTarget.route.isNotBlank() && navTarget.route != "splash") {
                                navTarget.route
                            } else {
                                "home"
                            }
                            Log.d("HolyQuranApp", " Transition: Splash finished, navigating to '$destination'")
                            try {
                                if (navController.currentDestination?.route == "splash") {
                                    navController.navigate(destination) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                    Log.d("HolyQuranApp", " Transition: Navigated from splash to '$destination'")
                                } else {
                                    Log.w("HolyQuranApp", " Transition Skipped: Current destination is '${navController.currentDestination?.route}', not 'splash'")
                                }
                            } catch (e: Throwable) {
                                Log.e("HolyQuranApp", " Error navigating from splash to $destination", e)
                                try {
                                    navController.navigate("home") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } catch (ex: Throwable) {
                                    Log.e("HolyQuranApp", " Fallback home navigation failed", ex)
                                }
                            }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigate = { route ->
                            try {
                                if (route == "reader") {
                                    viewModel.exitWirdSession()
                                }
                                navController.navigate(route)
                            } catch (e: Throwable) {
                                Log.e("HolyQuranApp", "Navigation error to $route", e)
                            }
                        }
                    )
                }
                composable("reader") {
                    QuranReaderScreen(
                        viewModel = viewModel,
                        onBack = {
                            try {
                                viewModel.exitWirdSession()
                                navController.popBackStack()
                            } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        },
                        onNavigateToTafsir = { _, _ ->
                            try { navController.navigate("tafsir") } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate tafsir error", e) }
                        }
                    )
                }
                composable("surahs") {
                    SurahListScreen(
                        viewModel = viewModel,
                        onNavigateToPage = { page ->
                            try {
                                viewModel.exitWirdSession(revertToFreeReading = false)
                                viewModel.jumpToPage(page)
                                navController.navigate("reader") {
                                    popUpTo("home") { saveState = false }
                                    launchSingleTop = true
                                }
                            } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate reader error", e) }
                        },
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("juz") {
                    JuzListScreen(
                        viewModel = viewModel,
                        onNavigateToPage = { page ->
                            try {
                                viewModel.exitWirdSession(revertToFreeReading = false)
                                viewModel.jumpToPage(page)
                                navController.navigate("reader") {
                                    popUpTo("home") { saveState = false }
                                    launchSingleTop = true
                                }
                            } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate reader error", e) }
                        },
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("bookmarks") {
                    BookmarkScreen(
                        viewModel = viewModel,
                        onNavigateToPage = {
                            try {
                                viewModel.exitWirdSession(revertToFreeReading = false)
                                navController.navigate("reader") {
                                    popUpTo("home") { saveState = false }
                                    launchSingleTop = true
                                }
                            } catch (e: Throwable) {
                                Log.e("HolyQuranApp", "navigate reader error", e)
                            }
                        },
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("history") {
                    ReadingHistoryScreen(
                        viewModel = viewModel,
                        onNavigateToPage = {
                            try {
                                viewModel.exitWirdSession(revertToFreeReading = false)
                                navController.navigate("reader") {
                                    popUpTo("home") { saveState = false }
                                    launchSingleTop = true
                                }
                            } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate reader error", e) }
                        },
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("goal_tracker") {
                    GoalTrackerScreen(
                        viewModel = viewModel,
                        onNavigateToPage = { page ->
                            try {
                                viewModel.jumpToPage(page)
                                navController.navigate("reader") {
                                    popUpTo("home") { saveState = false }
                                    launchSingleTop = true
                                }
                            } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate reader error", e) }
                        },
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("reading_analytics") {
                    ReadingAnalyticsScreen(
                        viewModel = viewModel,
                        onNavigateToPage = {
                            try {
                                viewModel.exitWirdSession(revertToFreeReading = false)
                                navController.navigate("reader") {
                                    popUpTo("home") { saveState = false }
                                    launchSingleTop = true
                                }
                            } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate reader error", e) }
                        },
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("search") {
                    SearchScreen(
                        viewModel = viewModel,
                        onNavigateToPage = { page ->
                            try {
                                viewModel.exitWirdSession(revertToFreeReading = false)
                                viewModel.jumpToPage(page)
                                navController.navigate("reader") {
                                    popUpTo("home") { saveState = false }
                                    launchSingleTop = true
                                }
                            } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate reader error", e) }
                        },
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("daily_verse") {
                    DailyVerseScreen(
                        viewModel = viewModel,
                        onNavigateToPage = {
                            try {
                                viewModel.exitWirdSession(revertToFreeReading = false)
                                navController.navigate("reader") {
                                    popUpTo("home") { saveState = false }
                                    launchSingleTop = true
                                }
                            } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate reader error", e) }
                        },
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("prayer_times") {
                    PrayerTimesScreen(
                        viewModel = viewModel,
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        },
                        onNavigateToQibla = {
                            try { navController.navigate("qibla") } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate qibla error", e) }
                        },
                        onNavigateToEditOffsets = {
                            try { navController.navigate("edit_prayer_times") } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate edit_prayer_times error", e) }
                        },
                        onNavigateToTracker = {
                            try { navController.navigate("prayer_tracker") } catch (e: Throwable) { Log.e("HolyQuranApp", "navigate prayer_tracker error", e) }
                        }
                    )
                }
                composable("prayer_tracker") {
                    PrayerTrackerScreen(
                        viewModel = viewModel,
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("edit_prayer_times") {
                    com.example.ui.screens.EditPrayerTimesScreen(
                        viewModel = viewModel,
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("qibla") {
                    QiblaScreen(
                        viewModel = viewModel,
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("hadith") {
                    HadithScreen(
                        viewModel = viewModel,
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("azkar") {
                    com.example.ui.screens.AzkarScreen(
                        viewModel = viewModel,
                        initialCategory = currentAzkarCategory,
                        initialQuickRead = currentAzkarQuickRead,
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("tafsir") {
                    TafsirScreen(
                        viewModel = viewModel,
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigate = { route ->
                            try { navController.navigate(route) } catch (e: Throwable) { Log.e("HolyQuranApp", "Navigation error to $route", e) }
                        },
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("donation") {
                    DonationScreen(
                        viewModel = viewModel,
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("features") {
                    AppFeaturesScreen(
                        viewModel = viewModel,
                        onBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        }
                    )
                }
                composable("group_khatma") {
                    val currentLang = if (userSettings.languageCode == "ar") AppLanguage.ARABIC else AppLanguage.ENGLISH
                    GroupKhatmaScreen(
                        currentLanguage = currentLang,
                        initialKhatmaCode = navTarget?.khatmaCode,
                        onNavigateBack = {
                            try { navController.popBackStack() } catch (e: Throwable) { Log.e("HolyQuranApp", "popBackStack error", e) }
                        },
                        onReadJuzClick = { juzNum ->
                            try {
                                val juzList = viewModel.getJuzList()
                                val startPage = juzList.firstOrNull { it.number == juzNum }?.startPage ?: 1
                                viewModel.jumpToPage(startPage)
                                navController.navigate("reader")
                            } catch (e: Throwable) {
                                Log.e("HolyQuranApp", "navigate reader error", e)
                            }
                        }
                    )
                }
            }
        }
    }
}

