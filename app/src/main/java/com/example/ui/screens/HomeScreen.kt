package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import com.example.domain.model.AppLanguage
import com.example.data.provider.QuranDataProvider
import com.example.data.provider.TafsirProvider
import com.example.notification.PrayerNotificationScheduler
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.QuranScript
import com.example.ui.theme.ScheherazadeFontFamily
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.viewmodel.QuranViewModel
import com.example.util.LocationHelper

data class HomeGridItem(
    val titleAr: String,
    val titleEn: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: QuranViewModel,
    onNavigate: (String) -> Unit
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()
    val readingHistory by viewModel.readingHistory.collectAsState()
    val isArabic = (userSettings.languageCode.ifBlank { "ar" }).startsWith("ar", ignoreCase = true)

    val todayPagesRead = remember(readingHistory) {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        readingHistory
            .filter { it.timestamp >= todayStart }
            .map { it.pageNumber }
            .distinct()
            .size
    }

    val context = LocalContext.current
    var hasAttemptedPermissionPrompt by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        val locationGranted = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                              (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

        if (locationGranted) {
            LocationHelper.getCurrentLocation(context) { lat, lng, cityName ->
                viewModel.setUserGPSLocation(lat, lng, cityName)
            }
        }
        if (notifGranted) {
            PrayerNotificationScheduler.schedulePrayerNotifications(context, viewModel.getPrayerTimes())
        }
    }

    LaunchedEffect(Unit) {
        val needsNotif = !LocationHelper.hasNotificationPermission(context)
        val needsLoc = !LocationHelper.hasLocationPermission(context)
        if ((needsNotif || needsLoc) && !hasAttemptedPermissionPrompt) {
            hasAttemptedPermissionPrompt = true
            val list = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            list.add(Manifest.permission.ACCESS_FINE_LOCATION)
            list.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            try {
                permissionLauncher.launch(list.toTypedArray())
            } catch (e: Throwable) {
                android.util.Log.e("HomeScreen", "Permission request failed gracefully", e)
            }
        } else if (LocationHelper.hasLocationPermission(context)) {
            try {
                LocationHelper.getCurrentLocation(context) { lat, lng, cityName ->
                    viewModel.setUserGPSLocation(lat, lng, cityName)
                }
            } catch (e: Throwable) {
                android.util.Log.e("HomeScreen", "Location fetch failed gracefully", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        com.example.util.AnalyticsHelper.logScreenView("HomeScreen")
    }

    var isEditLayoutMode by remember { mutableStateOf(false) }
    var showReorderDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showThemeSubmenu by remember { mutableStateOf(false) }
    var showExportBackupDialog by remember { mutableStateOf(false) }
    var showImportBackupDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var pendingUpdateInfo by remember { mutableStateOf<com.example.data.provider.AppUpdateInfo?>(null) }

    val defaultGridItems = remember {
        listOf(
            HomeGridItem("قراءة القرآن", "Read Quran", Icons.AutoMirrored.Filled.MenuBook, "reader", EmeraldPrimary),
            HomeGridItem("ملخص وإحصائيات القراءة", "Reading Summary & Stats", Icons.Default.BarChart, "reading_analytics", IslamicGold),
            HomeGridItem("هدف القراءة والختمة", "Khatmah Goal", Icons.Outlined.EmojiEvents, "goal_tracker", IslamicGold),
            HomeGridItem("الختمة الجماعية", "Group Khatma", Icons.Default.Groups, "group_khatma", EmeraldPrimary),
            HomeGridItem("سجل القراءة", "Reading History", Icons.Default.History, "history", EmeraldPrimary),
            HomeGridItem("السور", "Surahs", Icons.Default.FormatListNumbered, "surahs", EmeraldPrimary),
            HomeGridItem("الأجزاء", "Juz'", Icons.Default.Grid4x4, "juz", EmeraldPrimary),
            HomeGridItem("العلامات المرجعية", "Bookmarks", Icons.Default.Bookmark, "bookmarks", EmeraldPrimary),
            HomeGridItem("آية اليوم", "Daily Verse", Icons.Default.AutoAwesome, "daily_verse", IslamicGold),
            HomeGridItem("مواقيت الصلاة", "Prayer Times", Icons.Default.Schedule, "prayer_times", EmeraldPrimary),
            HomeGridItem("متابع الصلوات", "Prayers Tracker", Icons.Default.FactCheck, "prayer_tracker", IslamicGold),
            HomeGridItem("اتجاه القبلة", "Qibla Direction", Icons.Default.Explore, "qibla", EmeraldPrimary),
            HomeGridItem("الأذكار والأدعية", "Azkar & Duas", Icons.Default.SelfImprovement, "azkar", IslamicGold),
            HomeGridItem("الأحاديث النبوية", "Hadith Collection", Icons.Default.AutoStories, "hadith", EmeraldPrimary),
            HomeGridItem("التفسير والمعاني", "Tafsir", Icons.Default.Translate, "tafsir", EmeraldPrimary),
            HomeGridItem("مميزات التطبيق", "App Features", Icons.Default.AutoAwesome, "features", IslamicGold),
            HomeGridItem("الإعدادات", "Settings", Icons.Default.Settings, "settings", Color(0xFF556B2F))
        )
    }

    val gridItems = remember(userSettings.homeGridOrderJson, defaultGridItems) {
        val savedRoutes = userSettings.homeGridOrderJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (savedRoutes.isEmpty()) {
            defaultGridItems
        } else {
            val orderedList = mutableListOf<HomeGridItem>()
            savedRoutes.forEach { route ->
                defaultGridItems.firstOrNull { it.route == route }?.let { item ->
                    if (orderedList.none { it.route == item.route }) {
                        orderedList.add(item)
                    }
                }
            }
            // Append missing default items
            defaultGridItems.forEach { item ->
                if (orderedList.none { it.route == item.route }) {
                    orderedList.add(item)
                }
            }
            orderedList
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IslamicGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = IslamicGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isArabic) "القرآن الكريم" else "Holy Quran",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                actions = {
                    // Search Action Button
                    IconButton(
                        onClick = { onNavigate("search") },
                        modifier = Modifier.testTag("home_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    // Quick Dark Mode Toggle Button
                    val activeTheme = userSettings.effectiveThemeMode()
                    val isCurrentlyDark = activeTheme == "DARK"
                    IconButton(
                        onClick = {
                            val nextTheme = if (isCurrentlyDark) "LIGHT" else "DARK"
                            viewModel.updateThemeMode(nextTheme)
                            Toast.makeText(
                                context,
                                if (nextTheme == "DARK") (if (isArabic) "تم تفعيل الوضع الليلي الداكن 🌙" else "True Dark Mode Activated 🌙")
                                else (if (isArabic) "تم تفعيل المظهر النهاري ☀️" else "Light Mode Activated ☀️"),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag("home_quick_dark_mode_btn")
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Quick Dark Mode Toggle",
                            tint = if (isCurrentlyDark) IslamicGold else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 3-Dots Action Overflow Menu (Language, Theme, Share, Backup)
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("home_more_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = {
                                showOverflowMenu = false
                                showThemeSubmenu = false
                            }
                        ) {
                            // 1. Language Toggle
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        Text(text = if (isArabic) "تغيير اللغة (English)" else "Switch Language (العربية)")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showThemeSubmenu = false
                                    val nextLang = if (isArabic) "en" else "ar"
                                    viewModel.updateLanguage(nextLang)
                                }
                            )

                            // 2. Theme Mode Submenu
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = when (activeTheme) {
                                                    "LIGHT" -> Icons.Default.LightMode
                                                    "MUSHAF" -> Icons.Default.MenuBook
                                                    "SEPIA" -> Icons.Default.NightsStay
                                                    "DARK" -> Icons.Default.DarkMode
                                                    else -> Icons.Default.BrightnessAuto
                                                },
                                                contentDescription = null,
                                                tint = IslamicGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (isArabic) "المظهر والسمات" else "Theme & Mode",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Icon(
                                            imageVector = if (showThemeSubmenu) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    showThemeSubmenu = !showThemeSubmenu
                                }
                            )

                            if (showThemeSubmenu) {
                                val themeOptions = listOf(
                                    Triple("LIGHT", if (isArabic) "☀️ نهار (أبيض)" else "☀️ Light Mode", Color(0xFFFFFFFF)),
                                    Triple("MUSHAF", if (isArabic) "📜 ورقي (مصحف المدينة)" else "📜 Mushaf Paper Cream", Color(0xFFFAF3E0)),
                                    Triple("SEPIA", if (isArabic) "🍂 سيبيا دافئ (إراحة العين)" else "🍂 Warm Sepia", Color(0xFF38271C)),
                                    Triple("DARK", if (isArabic) "🌙 ليلي داكن (أسود حقيقي)" else "🌙 True Dark OLED", Color(0xFF000000)),
                                    Triple("SYSTEM", if (isArabic) "⚙️ تلقائي (حسب النظام)" else "⚙️ System Default", Color(0xFF757575))
                                )

                                themeOptions.forEach { (modeKey, title, dotColor) ->
                                    val isSelected = userSettings.themeMode == modeKey
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = dotColor,
                                                        border = BorderStroke(1.dp, if (modeKey == "DARK") Color(0xFF555555) else Color(0xFFCCCCCC)),
                                                        modifier = Modifier.size(14.dp)
                                                    ) {}
                                                    Text(
                                                        text = title,
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.updateThemeMode(modeKey)
                                            showOverflowMenu = false
                                            showThemeSubmenu = false
                                        }
                                    )
                                }
                            }

                            // 3. Share App
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        Text(text = if (isArabic) "مشاركة التطبيق" else "Share App")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    try {
                                        val shareText = if (isArabic) {
                                            "الْقُرْآنُ الْكَرِيمُ - تطبيق المصحف الشريف الشامل (بدون إعلانات • صدقة جارية)\n\n" +
                                            "رابط التطبيق:\nhttps://holy-quran-app-live.web.app"
                                        } else {
                                            "The Holy Quran App - Complete Offline Quran, Prayer Times & Azkar (100% Ad-Free • Sadaqah Jariyah)\n\n" +
                                            "Open app:\nhttps://holy-quran-app-live.web.app"
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, if (isArabic) "تطبيق القرآن الكريم" else "The Holy Quran App")
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, if (isArabic) "مشاركة التطبيق" else "Share App"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, if (isArabic) "تعذر فتح المشاركة" else "Cannot share", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // Check for Updates (3-dots menu item)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        Text(text = if (isArabic) "التحقق من التحديثات 🚀" else "Check for Updates 🚀")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    Toast.makeText(context, if (isArabic) "جاري التحقق من التحديثات..." else "Checking for updates...", Toast.LENGTH_SHORT).show()
                                    com.example.data.provider.AppUpdateManager.checkForUpdate { info ->
                                        if (info != null && info.isUpdateAvailable) {
                                            pendingUpdateInfo = info
                                        } else {
                                            Toast.makeText(context, if (isArabic) "التطبيق مُحدّث لأحدث إصدار رسمي (v${com.example.BuildConfig.VERSION_NAME}) ✅" else "Holy Quran App is up to date (v${com.example.BuildConfig.VERSION_NAME}) ✅", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )

                            HorizontalDivider()

                            // 4. Export Backup
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        Text(text = if (isArabic) "تصدير نسخة احتياطية" else "Export Backup")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.exportBackupData { json ->
                                        exportedJsonText = json
                                        showExportBackupDialog = true
                                    }
                                }
                            )

                            // 5. Import Backup
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(18.dp))
                                        Text(text = if (isArabic) "استرداد نسخة احتياطية" else "Import Backup")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showImportBackupDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Continue Reading Banner (متابعة القراءة)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigate("reader") }
                        .testTag("continue_reading_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        EmeraldPrimary,
                                        Color(0xFF0B3C26)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "متابعة القراءة" else "Continue Reading",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = GoldAccent.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${readingProgress.lastPage} / 604",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Surah Name Title
                            val currentReadingSurah = remember(readingProgress.lastPage) {
                                QuranDataProvider.getSurahByPage(readingProgress.lastPage)
                            }
                            Text(
                                text = if (isArabic) "سورة ${currentReadingSurah.nameArabic}" else "Surah ${currentReadingSurah.nameEnglish}",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            // Progress Bar
                            val progressFraction = (readingProgress.lastPage.toFloat() / 604f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = progressFraction,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = GoldAccent,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )

                            // Page Number & Goal / History Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = if (isArabic) "الصفحة ${readingProgress.lastPage}" else "Page ${readingProgress.lastPage}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onNavigate("goal_tracker") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                                        border = BorderStroke(1.dp, GoldAccent),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Outlined.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isArabic) "الهدف" else "Goal",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onNavigate("history") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                                        border = BorderStroke(1.dp, GoldAccent),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isArabic) "السجل" else "History",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Prominent Full-Width "Go to Reading" Button
                            Button(
                                onClick = { onNavigate("reader") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = Color(0xFF0D3B26)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_go_to_quran_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "انتقل للقراءة" else "Continue Reading",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Prayer Times Summary Card (الصلاة القادمة)
            item {
                val prayerTimes = remember(userSettings.calculationMethod, userSettings.isSummerTime) {
                    viewModel.getPrayerTimes()
                }
                val nextPrayer = prayerTimes.firstOrNull { it.isNext }
                    ?: prayerTimes.firstOrNull { it.nameEnglish != "Sunrise" }
                    ?: prayerTimes.firstOrNull()

                val methodLabel = when (userSettings.calculationMethod) {
                    "EGYPT" -> if (isArabic) "الهيئة المصرية" else "Egypt Method"
                    "MAKKAH" -> if (isArabic) "أم القرى (مكة)" else "Makkah Method"
                    "MWL" -> if (isArabic) "رابطة العالم الإسلامي" else "MWL Method"
                    "ISNA" -> if (isArabic) "أمريكا الشمالية" else "ISNA Method"
                    "KARACHI" -> if (isArabic) "جامعة كراتشي" else "Karachi Method"
                    else -> userSettings.calculationMethod
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("prayer_times") }
                        .testTag("prayer_times_home_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isArabic)
                                        "الصلاة القادمة: ${nextPrayer?.nameArabic ?: ""}"
                                    else
                                        "Next Prayer: ${nextPrayer?.nameEnglish ?: ""}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${nextPrayer?.timeFormatted ?: ""} • $methodLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isArabic) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2.5. Prayers Tracker Card (متابع الصلوات اليومية)
            item {
                val weeklySummaries by viewModel.weeklyTrackerSummaries.collectAsState()
                val todayDateStr = remember { viewModel.getTodayDateString() }
                val todaySummary = remember(weeklySummaries) {
                    weeklySummaries.firstOrNull { it.dateString == todayDateStr }
                        ?: weeklySummaries.firstOrNull { it.isToday }
                        ?: weeklySummaries.firstOrNull()
                }
                val completedCount = todaySummary?.completionCount ?: 0
                val totalCount = todaySummary?.totalTrackedCount ?: 5

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("prayer_tracker") }
                        .testTag("prayer_tracker_home_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = IslamicGold.copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FactCheck,
                                            contentDescription = null,
                                            tint = IslamicGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (isArabic) "متابع الصلوات اليومية 🕌" else "Daily Prayers Tracker 🕌",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isArabic) "«خير الأعمال.. الصلاة على وقتها»" else "Keep your prayers on time",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IslamicGold,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (completedCount == totalCount && totalCount > 0) IslamicGold else EmeraldPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$completedCount / $totalCount " + (if (isArabic) "صلوات" else "prayers"),
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (completedCount == totalCount && totalCount > 0) Color(0xFF0D3B26) else EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Quick 5-Prayer check buttons on Home Screen
                        val prayers = todaySummary?.prayers ?: emptyList()
                        if (prayers.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                prayers.take(5).forEach { prayerItem ->
                                    val isDone = prayerItem.isCompleted
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDone) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isDone) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.togglePrayerStatus(
                                                    dateString = todayDateStr,
                                                    prayerKey = prayerItem.type.key,
                                                    isOnTime = true,
                                                    context = context
                                                )
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            if (isDone) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Text(text = prayerItem.type.iconEmoji, fontSize = 13.sp)
                                            }
                                            Text(
                                                text = if (isArabic) prayerItem.type.nameArabic else prayerItem.type.nameEnglish.take(4),
                                                fontFamily = TajawalFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isDone) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Daily Verse Teaser (آية اليوم)
            item {
                val dailyVerse = viewModel.dailyVerse
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("daily_verse") }
                        .testTag("daily_verse_home_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isArabic) "آية اليوم" else "Daily Verse",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val dailyVerseEn = remember(dailyVerse) {
                            dailyVerse.translationEnglish.ifBlank {
                                val surahId = QuranDataProvider.SURAHS.firstOrNull { 
                                    it.nameArabic == dailyVerse.surahNameArabic || it.nameEnglish.equals(dailyVerse.surahNameEnglish, ignoreCase = true) 
                                }?.id ?: 1
                                try {
                                    TafsirProvider.getTafsirForAyah(surahId, dailyVerse.verseNumber, "muyassar").verseTranslationEn.ifBlank {
                                        "In the name of Allah, the Entirely Merciful, the Especially Merciful."
                                    }
                                } catch (e: Throwable) {
                                    "In the name of Allah, the Entirely Merciful, the Especially Merciful."
                                }
                            }
                        }
                        if (!isArabic) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = "\"$dailyVerseEn\"",
                                    style = TextStyle(
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        lineHeight = 26.sp,
                                        textAlign = TextAlign.Start,
                                        textDirection = TextDirection.Ltr
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            val quranScriptFont = QuranScript.fromId(userSettings.quranScriptType).getFontFamily()
                            Text(
                                text = "﴿ ${dailyVerse.textArabic} ﴾",
                                style = TextStyle(
                                    fontFamily = quranScriptFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.Center,
                                    textDirection = TextDirection.Rtl
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isArabic) "سورة ${dailyVerse.surahNameArabic} • الآية ${toArabicDigits(dailyVerse.verseNumber)}" else "Surah ${dailyVerse.surahNameEnglish} • Verse ${dailyVerse.verseNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. Daily Reading Encouragement Row ("ما تم قراءته اليوم: X صفحة")
            item {
                val pageGoal = userSettings.dailyPageGoal.coerceAtLeast(1)
                val progressRatio = (todayPagesRead.toFloat() / pageGoal.toFloat()).coerceIn(0f, 1f)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_reading_encouragement_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = IslamicGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (isArabic) "ما تم قراءته اليوم: $todayPagesRead صفحة" else "Read Today: $todayPagesRead Page(s)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isArabic) "الهدف اليومي: $pageGoal صفحة" else "Daily Goal: $pageGoal page(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldPrimary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "$todayPagesRead / $pageGoal",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (progressRatio >= 1f) IslamicGold else EmeraldPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        val motivationalText = remember(todayPagesRead, pageGoal, isArabic) {
                            if (todayPagesRead == 0) {
                                if (isArabic) "🚀 ابدأ وردك اليومي الآن وتحدّ نفسك لقراءة صفحة واحدة على الأقل!"
                                else "🚀 Start your daily portion now and challenge yourself to read at least 1 page!"
                            } else if (todayPagesRead < pageGoal) {
                                if (isArabic) "🌟 إنجاز ممتاز! واصل القراءة لتتجاوز التحدي وتصل لـ $pageGoal صفحات اليوم."
                                else "🌟 Great progress! Keep reading to reach your daily goal of $pageGoal page(s)."
                            } else {
                                if (isArabic) "🏆 ما شاء الله! أتممت هدفك اليومي بامتياز، واصل القراءة لزيادة الأجر والتصاعد بالختمة!"
                                else "🏆 Masha'Allah! You completed your daily goal with excellence!"
                            }
                        }

                        Text(
                            text = motivationalText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // 5. Khatma Challenge Card (جدول الختمة)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("goal_tracker") }
                        .testTag("khatma_challenge_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.EmojiEvents,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isArabic) "جدول الختمة وتحدي القراءة 🎯" else "Khatma Challenge & Goals 🎯",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isArabic) "تتبع أهدافك والتزامك اليومي بالختمة" else "Track your daily Khatma progress & streak",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isArabic) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Dashboard Grid Options Header with Reorder Mode Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isArabic) "الأقسام والخدمات" else "Features & Tools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        IconButton(
                            onClick = { isEditLayoutMode = !isEditLayoutMode },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("toggle_reorder_mode_btn")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isEditLayoutMode) EmeraldPrimary else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = if (isEditLayoutMode) Icons.Default.Check else Icons.Default.Edit,
                                        contentDescription = if (isEditLayoutMode) (if (isArabic) "حفظ الترتيب" else "Done") else (if (isArabic) "تعديل الترتيب" else "Reorder"),
                                        tint = if (isEditLayoutMode) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (isEditLayoutMode) {
                            Surface(
                                color = IslamicGold,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "تعديل الترتيب" else "Editing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF0F382C),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (isEditLayoutMode) {
                        TextButton(
                            onClick = { viewModel.resetHomeGridOrder() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isArabic) "إعادة ضبط" else "Reset",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Edit Mode Guidance Tip
            if (isEditLayoutMode) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = IslamicGold.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = IslamicGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "خصص ترتيب الصفحة الرئيسية بحسب استخدامك" else "Customize layout based on daily usage",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isArabic) "اضغط على أزرار الأسهم (⬆️/⬇️) لتقديم أو تأخير أي قسم." else "Tap arrow buttons (⬆️/⬇️) on any card to move up or down.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { showReorderDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "قائمة شاملة" else "List View",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Dashboard Grid Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    gridItems.chunked(2).forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEachIndexed { itemInRowIndex, item ->
                                val overallIndex = rowIndex * 2 + itemInRowIndex

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (isEditLayoutMode) 2.dp else 1.dp,
                                        color = if (isEditLayoutMode) IslamicGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    ),
                                    tonalElevation = if (isEditLayoutMode) 4.dp else 2.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(if (isEditLayoutMode) 110.dp else 95.dp)
                                        .clickable {
                                            if (!isEditLayoutMode) {
                                                onNavigate(item.route)
                                            }
                                        }
                                        .testTag("home_grid_${item.route}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        val itemColor = when {
                                            item.color == EmeraldPrimary -> MaterialTheme.colorScheme.primary
                                            item.color == IslamicGold -> MaterialTheme.colorScheme.secondary
                                            else -> item.color
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = itemColor.copy(alpha = 0.15f),
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = item.titleEn,
                                                        tint = itemColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            if (isEditLayoutMode) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    IconButton(
                                                        onClick = {
                                                            if (overallIndex > 0) {
                                                                val mutable = gridItems.toMutableList()
                                                                val temp = mutable[overallIndex]
                                                                mutable[overallIndex] = mutable[overallIndex - 1]
                                                                mutable[overallIndex - 1] = temp
                                                                viewModel.updateHomeGridOrder(mutable.map { it.route })
                                                            }
                                                        },
                                                        enabled = overallIndex > 0,
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowUpward,
                                                            contentDescription = "Move Up",
                                                            tint = if (overallIndex > 0) IslamicGold else Color.Gray,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            if (overallIndex < gridItems.size - 1) {
                                                                val mutable = gridItems.toMutableList()
                                                                val temp = mutable[overallIndex]
                                                                mutable[overallIndex] = mutable[overallIndex + 1]
                                                                mutable[overallIndex + 1] = temp
                                                                viewModel.updateHomeGridOrder(mutable.map { it.route })
                                                            }
                                                        },
                                                        enabled = overallIndex < gridItems.size - 1,
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDownward,
                                                            contentDescription = "Move Down",
                                                            tint = if (overallIndex < gridItems.size - 1) IslamicGold else Color.Gray,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = if (isArabic) item.titleAr else item.titleEn,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Reorder Full Dialog
            if (showReorderDialog) {
                item {
                    HomeReorderDialog(
                        isArabic = isArabic,
                        gridItems = gridItems,
                        onReorder = { newOrder ->
                            viewModel.updateHomeGridOrder(newOrder.map { it.route })
                        },
                        onReset = {
                            viewModel.resetHomeGridOrder()
                        },
                        onDismiss = { showReorderDialog = false }
                    )
                }
            }

            // Developer Support / Donation Footer Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clickable { onNavigate("donation") }
                        .testTag("donation_banner_home")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Support",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "دعم تطوير التطبيق" else "Support App Development",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isArabic) "التطبيق خالي تماماً من الإعلانات ومجاني 100%" else "100% Free with zero ads. Optional developer support.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = if (isArabic) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (showExportBackupDialog) {
            ExportBackupDialog(
                isArabic = isArabic,
                jsonText = exportedJsonText,
                onDismiss = { showExportBackupDialog = false }
            )
        }

        if (showImportBackupDialog) {
            ImportBackupDialog(
                isArabic = isArabic,
                onImport = { json ->
                    viewModel.importBackupData(json) { success ->
                        showImportBackupDialog = false
                        if (success) {
                            Toast.makeText(context, if (isArabic) "تمت استعادة النسخة الاحتياطية بنجاح!" else "Backup restored successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, if (isArabic) "فشل استرداد البيانات! تأكد من صحة الكود." else "Import failed! Please check code.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = { showImportBackupDialog = false }
            )
        }

        pendingUpdateInfo?.let { updateInfo ->
            AlertDialog(
                onDismissRequest = {
                    if (!updateInfo.forceUpdate) {
                        pendingUpdateInfo = null
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                },
                title = {
                    Text(
                        text = if (isArabic) "يتوفر تحديث جديد لـ تطبيق القرآن الكريم 🚀" else "New App Update Available 🚀",
                        fontFamily = TajawalFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isArabic) "الإصدار الحالي: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})\nالإصدار الجديد: v${updateInfo.latestVersionName} (Build ${updateInfo.latestVersionCode})" else "Current: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})\nNew: v${updateInfo.latestVersionName} (Build ${updateInfo.latestVersionCode})",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = updateInfo.getLocalizedNotes(isArabic),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isArabic) "اضغط على زر 'تحديث الآن' لتنزيل وتثبيت التحديث مباشرة." else "Click 'Update Now' to download and install directly.",
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
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(if (isArabic) "تحديث الآن 🚀" else "Update Now 🚀")
                    }
                },
                dismissButton = {
                    if (!updateInfo.forceUpdate) {
                        TextButton(onClick = { pendingUpdateInfo = null }) {
                            Text(if (isArabic) "تذكيري لاحقاً" else "Remind Later")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun HomeReorderDialog(
    isArabic: Boolean,
    gridItems: List<HomeGridItem>,
    onReorder: (List<HomeGridItem>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var items by remember { mutableStateOf(gridItems) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = null,
                    tint = IslamicGold
                )
                Text(
                    text = if (isArabic) "إعادة ترتيب الأقسام" else "Reorder Sections",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isArabic) "حرك الأقسام لأعلى ولأسفل حسب أولوية استخدامك اليومي:" else "Move sections up and down based on daily usage priority:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(items) { index, item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isArabic) item.titleAr else item.titleEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = items.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index - 1]
                                                mutable[index - 1] = temp
                                                items = mutable
                                                onReorder(mutable)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Up",
                                            tint = if (index > 0) EmeraldPrimary else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < items.size - 1) {
                                                val mutable = items.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index + 1]
                                                mutable[index + 1] = temp
                                                items = mutable
                                                onReorder(mutable)
                                            }
                                        },
                                        enabled = index < items.size - 1,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Down",
                                            tint = if (index < items.size - 1) EmeraldPrimary else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(
                    text = if (isArabic) "حفظ وإغلاق" else "Save & Close",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onReset()
                    onDismiss()
                }
            ) {
                Text(
                    text = if (isArabic) "إعادة الترتيب الافتراضي" else "Reset Default",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
fun ExportBackupDialog(
    isArabic: Boolean,
    jsonText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    text = if (isArabic) "تصدير النسخة الاحتياطية" else "Export Backup Data",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isArabic)
                        "تم إنشاء النسخة الاحتياطية بنجاح! تحتوي على العلامات المرجعية وسجل القراءة وإعداداتك. يمكنك نسخ النص التالي للاحتفاظ به."
                    else
                        "Backup created successfully! Contains bookmarks, reading progress, and settings. Copy text below to keep.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    textStyle = MaterialTheme.typography.labelSmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(jsonText))
                    Toast.makeText(context, if (isArabic) "تم نسخ النسخة الاحتياطية إلى الحافظة" else "Copied backup to clipboard", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "نسخ الحافظة" else "Copy Text")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إغلاق" else "Close")
            }
        }
    )
}

@Composable
fun ImportBackupDialog(
    isArabic: Boolean,
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, tint = IslamicGold)
                Text(
                    text = if (isArabic) "استرداد نسخة احتياطية" else "Import Backup Data",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isArabic)
                        "الصق نص النسخة الاحتياطية (JSON) للاسترداد. سيتم استرجاع العلامات المرجعية والتقدم والإعدادات."
                    else
                        "Paste your JSON backup text below to restore bookmarks, history, and settings.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(if (isArabic) "الصق الكود هنا..." else "Paste JSON code here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    textStyle = MaterialTheme.typography.labelSmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onImport(inputText.trim())
                    }
                },
                enabled = inputText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGold)
            ) {
                Text(if (isArabic) "استرداد البيانات" else "Import Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}
