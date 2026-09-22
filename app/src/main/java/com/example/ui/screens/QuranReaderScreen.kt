package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.provider.QuranDataProvider
import com.example.data.provider.QuranPageFetcher
import com.example.data.provider.QuranWordItem
import com.example.data.provider.TafsirProvider
import com.example.data.provider.AsbabAlNuzulProvider
import com.example.data.provider.AsbabNuzulItem
import com.example.data.audio.AyahRepeatMode
import com.example.domain.model.Verse
import com.example.domain.model.WordMeaning
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.QuranScript
import com.example.ui.theme.ScheherazadeFontFamily
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.viewmodel.QuranViewModel
import com.example.util.TajweedHelper
import kotlinx.coroutines.launch

fun sanitizeUthmaniVerseText(text: String): String {
    return text
        .replace("\u06DD", " ")
        .replace("[\uFD3E\uFD3F﴿﴾\\[\\]()]".toRegex(), " ")
        .replace("[0-9٠-٩]+".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()
}

fun cleanUthmaniText(text: String): String {
    return sanitizeUthmaniVerseText(text)
}

fun toArabicDigits(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { if (it in '0'..'9') arabicDigits[it - '0'] else it }.joinToString("")
}

// Comprehensive Arabic text normalizer for accurate matching across diacritics, hamzas, dagger alefs, and Quranic marks
fun normalizeArabicWord(rawWord: String): String {
    return rawWord
        .replace("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06DC\\u06DF-\\u06E8\\u06EA-\\u06ED،.؛؟﴿﴾\\[\\]()0-9٠-٩]".toRegex(), "")
        .replace("ٱ", "ا")
        .replace("[إأآ]", "ا")
        .replace("ى", "ي")
        .replace("ؤ", "و")
        .replace("ئ", "ي")
        .trim()
}

// Strip common Quranic / Arabic prefixes (e.g., Al-, Wa-, Fa-, Bi-, La-) for root vocabulary matching
fun stripArabicPrefixes(word: String): String {
    var result = word
    if (result.startsWith("وبال") || result.startsWith("فبال")) result = result.substring(4)
    else if (result.startsWith("بال") || result.startsWith("كال") || result.startsWith("فال") || result.startsWith("وال")) result = result.substring(3)
    else if (result.startsWith("ولل") || result.startsWith("فلل")) result = result.substring(3)
    else if (result.startsWith("لل") || result.startsWith("ال")) result = result.substring(2)
    else if (result.length > 3 && (result.startsWith("و") || result.startsWith("ف") || result.startsWith("ب") || result.startsWith("ل") || result.startsWith("ك"))) {
        result = result.substring(1)
    }
    return result
}

// Check if a word is any format of the Divine Name (Allah / Lillah / Billah / Wallah / Tallah / Allahumma, etc.)
private fun isDivineName(rawWord: String): Boolean {
    val clean = normalizeArabicWord(rawWord)
    if (clean.isEmpty()) return false

    // Direct matches for common forms
    if (clean == "الله" || clean == "اللهم" || clean == "لله") return true

    // Strip Quranic prefixes: و, ف, ب, ل, أ, ت, ول, فل, وت, أب, أف, أو
    val stripped = clean.replace("^([وفبلتأ]|ول|فل|وت|أب|أف|أو)+".toRegex(), "")
    return stripped == "الله" || stripped == "اللهم" || stripped == "لله"
}

// Check if a word is a highlighted vocabulary word
private fun isVocabularyWord(rawWord: String, vocabularyList: List<WordMeaning>): Boolean {
    val normWord = normalizeArabicWord(rawWord)
    val strippedWord = stripArabicPrefixes(normWord)
    if (normWord.length < 3 && strippedWord.length < 3) return false

    return vocabularyList.any { item ->
        val normItem = normalizeArabicWord(item.arabicWord)
        val strippedItem = stripArabicPrefixes(normItem)
        normWord == normItem ||
        (strippedWord.isNotEmpty() && strippedWord == strippedItem) ||
        (normItem.length >= 3 && (normWord.startsWith(normItem) || normItem.startsWith(normWord))) ||
        (strippedItem.length >= 3 && (strippedWord.startsWith(strippedItem) || strippedItem.startsWith(strippedWord)))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuranReaderScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onNavigateToTafsir: ((Int, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentPage by viewModel.currentPage.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val selectedVerse by viewModel.selectedVerseForDetail.collectAsState()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
    val isBufferingAudio by viewModel.isBufferingAudio.collectAsState()
    val currentPlayingVerse by viewModel.currentPlayingVerse.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    val isArabic = userSettings.languageCode == "ar"
    val currentTheme = userSettings.effectiveThemeMode()
    val isDarkMode = currentTheme == "DARK" || currentTheme == "SEPIA" || (currentTheme == "SYSTEM" && androidx.compose.foundation.isSystemInDarkTheme())
    
    // Keep Screen Awake while reading Quran
    DisposableEffect(userSettings.keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (userSettings.keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    val haptic = LocalHapticFeedback.current

    val isPageBookmarked = remember(bookmarks, currentPage) {
        bookmarks.any { it.pageNumber == currentPage }
    }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showSurahPickerSheet by remember { mutableStateOf(false) }
    var showReciterDialog by remember { mutableStateOf(false) }
    var showPageTafseerSheet by remember { mutableStateOf(false) }
    var showScriptPickerSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showThemeSubmenu by remember { mutableStateOf(false) }
    var activeClickedWord by remember { mutableStateOf<Pair<String, Verse>?>(null) }
    var isMushafPageView by remember { mutableStateOf(true) }
    var isImmersiveReading by remember { mutableStateOf(true) }
    var mushafPageScale by rememberSaveable { mutableFloatStateOf(1.05f) }

    val pagerState = rememberPagerState(
        initialPage = (currentPage - 1).coerceIn(0, 603),
        pageCount = { 604 }
    )

    LaunchedEffect(pagerState.currentPage) {
        val targetPage = pagerState.currentPage + 1
        if (targetPage != currentPage) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.jumpToPage(targetPage)
        }
    }

    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage - 1) {
            val pageDiff = kotlin.math.abs(pagerState.currentPage - (currentPage - 1))
            if (pageDiff <= 1) {
                pagerState.animateScrollToPage(currentPage - 1)
            } else {
                pagerState.scrollToPage(currentPage - 1)
            }
        }
    }

    val surah = remember(currentPage) { QuranDataProvider.getSurahByPage(currentPage) }
    val juz = remember(currentPage) { QuranDataProvider.getJuzByPage(currentPage) }
    val currentVerses by viewModel.currentVerses.collectAsState()
    val vocabularyList = remember { QuranDataProvider.WORD_MEANINGS }

    val pageBgColor = if (isDarkMode) Color(0xFF131A27) else Color(0xFFEFE6D8)

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = !isImmersiveReading,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color(0xFF0F5132),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSurahPickerSheet = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("reader_top_title_surah_picker")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IslamicGold.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = "Reading Quran Icon",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Text(
                                    text = if (isArabic) surah.nameArabic else surah.nameEnglish,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Surah",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = if (isArabic) "الجزء ${toArabicDigits(juz.number)} • صفحة ${toArabicDigits(currentPage)}" else "Juz ${juz.number} • Page $currentPage",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White.copy(alpha = 0.95f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .clickable { onBack() }
                            .testTag("reader_back_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        // Quick Bookmark Toggle Button Chip
                        Surface(
                            shape = CircleShape,
                            color = if (isPageBookmarked) Color(0xFFFEE2E2) else Color.White.copy(alpha = 0.15f),
                            border = if (isPageBookmarked) BorderStroke(1.5.dp, Color(0xFFDC2626)) else null,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    if (isPageBookmarked) {
                                        val existing = bookmarks.filter { it.pageNumber == currentPage }
                                        existing.forEach { viewModel.deleteBookmark(it.id) }
                                        Toast.makeText(context, if (isArabic) "تم إزالة العلامة المرجعية" else "Bookmark Removed", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addCurrentPageBookmark()
                                        Toast.makeText(context, if (isArabic) "تم حفظ الصفحة بالمفضلة" else "Page Bookmarked", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .testTag("reader_bookmark_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isPageBookmarked) Color(0xFFDC2626) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Toggle Mushaf Page / Text View Button
                        Surface(
                            shape = CircleShape,
                            color = if (isMushafPageView) IslamicGold else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    isMushafPageView = !isMushafPageView
                                    Toast.makeText(
                                        context,
                                        if (isMushafPageView) (if (isArabic) "نمط صفحات المصحف المصورة (مع التكبير)" else "15-Line Mushaf Page View")
                                        else (if (isArabic) "نمط القراءة النصية والتجويد" else "Text & Tajweed View"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .testTag("reader_mushaf_view_toggle")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isMushafPageView) Icons.Default.AutoStories else Icons.Default.TextFields,
                                    contentDescription = "Toggle Page/Text View",
                                    tint = if (isMushafPageView) Color(0xFF1B5E55) else Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Tafseer Quick Button Chip
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { showPageTafseerSheet = true }
                                .testTag("reader_tafseer_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Tafseer & Word Meaning",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Quick Dark Mode Toggle Button Chip
                        val isCurrentlyDark = currentTheme == "DARK"
                        Surface(
                            shape = CircleShape,
                            color = if (isCurrentlyDark) GoldAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
                            border = if (isCurrentlyDark) BorderStroke(1.5.dp, IslamicGold) else null,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    val nextTheme = if (isCurrentlyDark) "LIGHT" else "DARK"
                                    viewModel.updateThemeMode(nextTheme)
                                    Toast.makeText(
                                        context,
                                        if (nextTheme == "DARK") (if (isArabic) "تم تفعيل الوضع الليلي الداكن 🌙" else "True Dark Mode Activated 🌙")
                                        else (if (isArabic) "تم تفعيل المظهر النهاري ☀️" else "Light Mode Activated ☀️"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .testTag("reader_quick_dark_mode_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isCurrentlyDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Quick Dark Mode Toggle",
                                    tint = if (isCurrentlyDark) GoldAccent else Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // More Options Menu (3 Dots Overflow)
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { showOptionsMenu = true }
                                    .testTag("reader_more_options_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = {
                                    showOptionsMenu = false
                                    showThemeSubmenu = false
                                },
                                modifier = Modifier.background(
                                    if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
                                )
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isArabic) (if (isMushafPageView) "التبديل إلى نمط النص والتجويد" else "التبديل إلى نمط مصحف المدينة (15 سطراً)")
                                            else (if (isMushafPageView) "Switch to Text View" else "Switch to 15-Line Mushaf View"),
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isMushafPageView) Icons.Default.TextFields else Icons.Default.AutoStories,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showThemeSubmenu = false
                                        isMushafPageView = !isMushafPageView
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isArabic) "فهرس السور والأجزاء" else "Surah & Juz Index",
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showThemeSubmenu = false
                                        showSurahPickerSheet = true
                                    }
                                )


                                // Theme Submenu Accordion / Sub-Items
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isArabic) "المظهر والسمات" else "Theme & Mode",
                                                fontFamily = TajawalFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = if (showThemeSubmenu) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (currentTheme) {
                                                "LIGHT" -> Icons.Default.LightMode
                                                "MUSHAF" -> Icons.Default.MenuBook
                                                "SEPIA" -> Icons.Default.NightsStay
                                                "DARK" -> Icons.Default.DarkMode
                                                else -> Icons.Default.Palette
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
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
                                                        .padding(start = 12.dp),
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
                                                showOptionsMenu = false
                                                showThemeSubmenu = false
                                            }
                                        )
                                    }
                                }

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isArabic) "رسم ونوع خط المصحف" else "Quran Script & Style",
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showThemeSubmenu = false
                                        showScriptPickerSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isArabic) "اختيار القارئ" else "Select Reciter",
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showThemeSubmenu = false
                                        showReciterDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isImmersiveReading,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                // Sleek Mushaf Bottom Navigation Bar matching theme design
                Surface(
                    tonalElevation = 6.dp,
                    color = if (isDarkMode) MaterialTheme.colorScheme.surface else Color(0xFF0F5132)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play Audio Button & Reciter Switcher
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { viewModel.toggleAudioPlayback() }
                                    .testTag("reader_play_pause_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isBufferingAudio) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Reciter Switcher Dropdown
                            var showBottomReciterMenu by remember { mutableStateOf(false) }
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable { showBottomReciterMenu = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = if (isArabic) selectedReciter.nameArabic.split(" ").take(2).joinToString(" ") else selectedReciter.nameEnglish.split(" ").take(2).joinToString(" "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }

                                DropdownMenu(
                                    expanded = showBottomReciterMenu,
                                    onDismissRequest = { showBottomReciterMenu = false }
                                ) {
                                    viewModel.reciters.forEach { reciter ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isArabic) reciter.nameArabic else reciter.nameEnglish,
                                                    fontWeight = if (reciter.id == selectedReciter.id) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (reciter.id == selectedReciter.id) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                viewModel.setReciter(reciter)
                                                showBottomReciterMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Progress Dots Bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeDotIndex = (currentPage % 12)
                            repeat(12) { i ->
                                Box(
                                    modifier = Modifier
                                        .size(if (i == activeDotIndex) 6.dp else 4.dp)
                                        .clip(CircleShape)
                                        .background(if (i == activeDotIndex) IslamicGold else Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }

                        // Page Number Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IslamicGold,
                            modifier = Modifier
                                .clickable { showJumpDialog = true }
                                .testTag("reader_page_badge")
                        ) {
                            Text(
                                text = if (isArabic) toArabicDigits(currentPage) else currentPage.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF1B5E55),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isImmersiveReading) PaddingValues(0.dp) else innerPadding)
                .background(pageBgColor)
        ) {
            // Elegant View Selector Ribbon (15-Line Mushaf Page vs Formatted Text & Tajweed)
            AnimatedVisibility(
                visible = !isImmersiveReading,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = if (isDarkMode) Color(0xFF131B2A) else Color(0xFFF4ECE0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE5DDD0),
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFD4C7B5)),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Mushaf Page Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isMushafPageView) IslamicGold else Color.Transparent,
                                modifier = Modifier
                                    .clickable {
                                        isMushafPageView = true
                                        isImmersiveReading = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    .testTag("tab_mushaf_page_view")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = if (isMushafPageView) Color(0xFF1B5E55) else if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF4B5563),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "صفحة المصحف (15 سطراً)" else "15-Line Mushaf Page",
                                        style = TextStyle(
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = if (isMushafPageView) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isMushafPageView) Color(0xFF1B5E55) else if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF4B5563)
                                    )
                                }
                            }

                            // Formatted Text & Tajweed Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (!isMushafPageView) IslamicGold else Color.Transparent,
                                modifier = Modifier
                                    .clickable {
                                        isMushafPageView = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    .testTag("tab_text_tajweed_view")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TextFields,
                                        contentDescription = null,
                                        tint = if (!isMushafPageView) Color(0xFF1B5E55) else if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF4B5563),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "النص والتجويد" else "Text & Tajweed",
                                        style = TextStyle(
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = if (!isMushafPageView) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (!isMushafPageView) Color(0xFF1B5E55) else if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF4B5563)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                val pageNum = pageIndex + 1
                val pageVerses = remember(pageNum, currentVerses, currentPage) {
                    if (pageNum == currentPage && currentVerses.isNotEmpty()) currentVerses else QuranDataProvider.getVersesForPage(pageNum)
                }

                if (isMushafPageView) {
                    GoldenMushafPageView(
                        pageNum = pageNum,
                        pageVerses = pageVerses,
                        isDarkMode = isDarkMode,
                        isArabic = isArabic,
                        isPlayingAudio = isPlayingAudio,
                        currentPlayingVerse = currentPlayingVerse,
                        isImmersive = isImmersiveReading,
                        onToggleImmersive = { isImmersiveReading = !isImmersiveReading },
                        onVerseClick = { verse -> viewModel.setSelectedVerseForDetail(verse) },
                        onWordMeaningClick = { word, verse -> activeClickedWord = Pair(word, verse) },
                        onOpenPageTafsir = { showPageTafseerSheet = true },
                        onToggleBookmark = {
                            val isThisPageBookmarked = bookmarks.any { it.pageNumber == pageNum }
                            if (isThisPageBookmarked) {
                                val existing = bookmarks.filter { it.pageNumber == pageNum }
                                existing.forEach { viewModel.deleteBookmark(it.id) }
                                Toast.makeText(context, if (isArabic) "تم إزالة العلامة المرجعية" else "Bookmark Removed", Toast.LENGTH_SHORT).show()
                            } else {
                                val surahForThisPage = QuranDataProvider.getSurahByPage(pageNum)
                                viewModel.addBookmarkForPage(pageNum, if (isArabic) "سورة ${surahForThisPage.nameArabic} - صفحة $pageNum" else "Surah ${surahForThisPage.nameEnglish} - Page $pageNum")
                                Toast.makeText(context, if (isArabic) "تم حفظ الصفحة بالمفضلة" else "Page Bookmarked", Toast.LENGTH_SHORT).show()
                            }
                        },
                        isBookmarked = bookmarks.any { it.pageNumber == pageNum },
                        onPlayPageAudio = {
                            val surahForThisPage = QuranDataProvider.getSurahByPage(pageNum)
                            if (isPlayingAudio) {
                                viewModel.stopAudioPlayback()
                            } else {
                                viewModel.playSpecificSurahAudio(surahForThisPage.id)
                            }
                        },
                        onNavigateBack = onBack,
                        onSwitchToTextView = { isMushafPageView = false },
                        onOpenSurahPicker = { showSurahPickerSheet = true },
                        onOpenJumpDialog = { showJumpDialog = true },
                        onPreviousPage = {
                            if (pageNum > 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pageIndex - 1)
                                }
                            }
                        },
                        onNextPage = {
                            if (pageNum < 604) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pageIndex + 1)
                                }
                            }
                        },
                        onToggleDarkMode = {
                            val newMode = if (isDarkMode) "LIGHT" else "DARK"
                            viewModel.updateThemeMode(newMode)
                            Toast.makeText(
                                context,
                                if (isDarkMode) (if (isArabic) "تم التبديل إلى الوضع النهاري ☀️" else "Switched to Light Mode ☀️")
                                else (if (isArabic) "تم التبديل إلى الوضع الليلي 🌙" else "Switched to Dark Mode 🌙"),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        pageScale = mushafPageScale,
                        onPageScaleChange = { newScale -> mushafPageScale = newScale }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Header Surah Title Banner if top of Surah
                        val surahOnPage = QuranDataProvider.getSurahByPage(pageNum)
                        if (surahOnPage.startPage == pageNum) {
                            item {
                                OrnateSurahHeaderBanner(
                                    surah = surahOnPage,
                                    isDarkMode = isDarkMode,
                                    isArabic = isArabic
                                )
                            }
                        }

                        if (!isArabic && userSettings.showEnglishTranslation) {
                        // 100% Pure English Quran Page View
                        item {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                ParchmentPaperBackground(
                                    isDarkMode = isDarkMode,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFEADBCE)), RoundedCornerShape(12.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoStories,
                                                    contentDescription = null,
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "Surah ${surahOnPage.nameEnglish} • Page $pageNum",
                                                    style = TextStyle(
                                                        fontFamily = TajawalFontFamily,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = IslamicGold.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "Sahih Intl",
                                                    style = TextStyle(
                                                        fontFamily = TajawalFontFamily,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = if (isDarkMode) GoldAccent else Color(0xFF92400E),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = if (isDarkMode) Color(0xFF334155) else Color(0xFFEADBCE))

                                        pageVerses.forEach { verse ->
                                            val isVersePlaying = isPlayingAudio &&
                                                    currentPlayingVerse?.first == verse.surahId &&
                                                    currentPlayingVerse?.second == verse.verseNumber

                                            val verseEnText = verse.translationEnglish.ifBlank {
                                                TafsirProvider.getTafsirForAyah(verse.surahId, verse.verseNumber, "muyassar").verseTranslationEn.ifBlank {
                                                    "Surah ${verse.surahNameEnglish} - Verse ${verse.verseNumber}"
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isVersePlaying) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                                border = if (isVersePlaying) BorderStroke(1.dp, EmeraldPrimary) else null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.setSelectedVerseForDetail(verse) }
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = if (isVersePlaying) GoldAccent else EmeraldPrimary,
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = "${verse.verseNumber}",
                                                                    fontSize = 12.sp,
                                                                    fontFamily = TajawalFontFamily,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = "Surah ${verse.surahNameEnglish} • Verse ${verse.verseNumber}",
                                                            style = TextStyle(
                                                                fontFamily = TajawalFontFamily,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            ),
                                                            color = if (isDarkMode) GoldAccent else Color(0xFF0D5C3A)
                                                        )
                                                    }

                                                    Text(
                                                        text = verseEnText,
                                                        style = TextStyle(
                                                            fontFamily = TajawalFontFamily,
                                                            fontSize = (userSettings.fontSizeSp * 0.95f).sp,
                                                            lineHeight = (userSettings.fontSizeSp * 1.5f).sp,
                                                            fontWeight = FontWeight.Normal,
                                                            textAlign = TextAlign.Start,
                                                            textDirection = TextDirection.Ltr
                                                        ),
                                                        color = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // High-Definition Crisp Mushaf Page Layout for Arabic Mode
                        item {
                            ParchmentPaperBackground(
                                isDarkMode = isDarkMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFEADBCE)), RoundedCornerShape(12.dp))
                            ) {
                                val animatedFocusGold by animateColorAsState(
                                    targetValue = if (isDarkMode) Color(0xFFF59E0B) else Color(0xFFD97706),
                                    animationSpec = tween(350),
                                    label = "animatedFocusGold"
                                )

                                val currentScript = QuranScript.fromId(userSettings.quranScriptType)
                                val currentFontFamily = currentScript.getFontFamily()

                                val fontMultiplier = when (currentScript) {
                                    QuranScript.MADINAH -> 1.45f
                                    QuranScript.AMIRI_QURAN -> 1.48f
                                    QuranScript.AMIRI_NASKH -> 1.38f
                                    QuranScript.TAJAWAL -> 1.25f
                                }
                                val lineHeightMultiplier = when (currentScript) {
                                    QuranScript.MADINAH -> 2.65f
                                    QuranScript.AMIRI_QURAN -> 2.70f
                                    QuranScript.AMIRI_NASKH -> 2.45f
                                    QuranScript.TAJAWAL -> 2.25f
                                }

                                val softHighlightBg = if (isDarkMode) Color(0xFF854D0E).copy(alpha = 0.40f) else Color(0xFFFEF3C7)

                                val annotatedPageString = remember(pageVerses, currentPlayingVerse, isDarkMode, userSettings, vocabularyList, isPlayingAudio, activeClickedWord, animatedFocusGold, currentScript) {
                                    buildAnnotatedString {
                                        pageVerses.forEach { verse ->
                                            val isVersePlaying = isPlayingAudio &&
                                                    currentPlayingVerse?.first == verse.surahId &&
                                                    currentPlayingVerse?.second == verse.verseNumber

                                            val verseText = sanitizeUthmaniVerseText(verse.textUthmani)
                                            val words = verseText.split(" ").filter { it.isNotBlank() }

                                            words.forEach { rawWord ->
                                                val isRed = isDivineName(rawWord)
                                                val isSelected = activeClickedWord != null &&
                                                        activeClickedWord?.first == rawWord &&
                                                        activeClickedWord?.second?.surahId == verse.surahId &&
                                                        activeClickedWord?.second?.verseNumber == verse.verseNumber

                                                val tajweedColor = if (userSettings.enableTajweed && !isVersePlaying && !isSelected && !isRed) {
                                                    TajweedHelper.getTajweedColorForWord(rawWord, isDarkMode)
                                                } else null

                                                val finalColor = when {
                                                    isVersePlaying -> if (isDarkMode) Color(0xFFFFD54F) else Color(0xFF065F46)
                                                    isSelected -> animatedFocusGold
                                                    isRed -> if (isDarkMode) Color(0xFFFF8A80) else Color(0xFFDC2626)
                                                    tajweedColor != null -> tajweedColor
                                                    isDarkMode -> Color(0xFFFDE68A)
                                                    else -> Color(0xFF1A1A1A)
                                                }

                                                val wordStart = length
                                                append(rawWord)
                                                val wordEnd = length

                                                addStringAnnotation(
                                                    tag = "WORD",
                                                    annotation = "$rawWord|${verse.surahId}|${verse.verseNumber}",
                                                    start = wordStart,
                                                    end = wordEnd
                                                )

                                                addStyle(
                                                    style = SpanStyle(
                                                        fontFamily = currentFontFamily,
                                                        color = finalColor,
                                                        background = if (isSelected || isVersePlaying) softHighlightBg else Color.Transparent,
                                                        fontWeight = if (isRed || isVersePlaying || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None
                                                    ),
                                                    start = wordStart,
                                                    end = wordEnd
                                                )

                                                append(" ")
                                            }

                                            val verseStart = length
                                            val badgeText = "﴿${toArabicDigits(verse.verseNumber)}﴾"
                                            append(badgeText)
                                            val verseEnd = length

                                            addStringAnnotation(
                                                tag = "VERSE",
                                                annotation = "${verse.surahId}|${verse.verseNumber}",
                                                start = verseStart,
                                                end = verseEnd
                                            )

                                            addStyle(
                                                style = SpanStyle(
                                                    fontFamily = currentFontFamily,
                                                    color = if (isVersePlaying) Color(0xFFF59E0B) else if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF1B5E55),
                                                    background = if (isVersePlaying) softHighlightBg else Color.Transparent,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                start = verseStart,
                                                end = verseEnd
                                            )

                                            append(" ")
                                        }
                                    }
                                }

                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 20.dp)
                                ) {
                                    Text(
                                        text = annotatedPageString,
                                        style = TextStyle(
                                            fontFamily = currentFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (userSettings.fontSizeSp * fontMultiplier).sp,
                                            lineHeight = (userSettings.fontSizeSp * lineHeightMultiplier).sp,
                                            textAlign = TextAlign.Center,
                                            textDirection = TextDirection.ContentOrRtl,
                                            platformStyle = PlatformTextStyle(includeFontPadding = true)
                                        ),
                                        onTextLayout = { layoutResult = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(annotatedPageString) {
                                                detectTapGestures { tapOffset ->
                                                    try {
                                                        layoutResult?.let { layout ->
                                                            if (layout.lineCount <= 0) return@detectTapGestures

                                                            val offset = try {
                                                                layout.getOffsetForPosition(tapOffset).coerceIn(0, (annotatedPageString.length - 1).coerceAtLeast(0))
                                                            } catch (e: Exception) {
                                                                0
                                                            }

                                                            val allVerseAnnos = annotatedPageString.getStringAnnotations("VERSE", 0, annotatedPageString.length)
                                                            val exactVerseAnno = allVerseAnnos.firstOrNull { offset >= it.start && offset <= it.end }
                                                            if (exactVerseAnno != null) {
                                                                val parts = exactVerseAnno.item.split("|")
                                                                if (parts.size >= 2) {
                                                                    val surahId = parts[0].toIntOrNull() ?: 1
                                                                    val verseNum = parts[1].toIntOrNull() ?: 1
                                                                    val verse = pageVerses.firstOrNull { v -> v.surahId == surahId && v.verseNumber == verseNum }
                                                                    if (verse != null) {
                                                                        viewModel.setSelectedVerseForDetail(verse)
                                                                        return@detectTapGestures
                                                                    }
                                                                }
                                                            }

                                                            val allWordAnnos = annotatedPageString.getStringAnnotations("WORD", 0, annotatedPageString.length)
                                                            var targetWordAnno = allWordAnnos.firstOrNull { offset >= it.start && offset < it.end }

                                                            if (targetWordAnno == null && offset > 0) {
                                                                targetWordAnno = allWordAnnos.firstOrNull { (offset - 1) >= it.start && (offset - 1) < it.end }
                                                            }

                                                            if (targetWordAnno != null) {
                                                                val parts = targetWordAnno.item.split("|")
                                                                if (parts.size >= 3) {
                                                                    val rawWord = parts[0]
                                                                    val surahId = parts[1].toIntOrNull() ?: 1
                                                                    val verseNum = parts[2].toIntOrNull() ?: 1
                                                                    val verse = pageVerses.firstOrNull { v -> v.surahId == surahId && v.verseNumber == verseNum }
                                                                    if (verse != null) {
                                                                        activeClickedWord = Pair(rawWord, verse)
                                                                        return@detectTapGestures
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Throwable) {
                                                        android.util.Log.e("QuranReaderScreen", "Error in tap gesture", e)
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                        }

                        // Optional English Verses Translation Card in Arabic Mode
                        if (userSettings.showEnglishTranslation) {
                            item {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
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
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Translate,
                                                        contentDescription = null,
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "English Verses Translation",
                                                        style = TextStyle(
                                                            fontFamily = TajawalFontFamily,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = "Page $pageNum",
                                                    style = TextStyle(
                                                        fontFamily = TajawalFontFamily,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = IslamicGold
                                                )
                                            }

                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                            pageVerses.forEach { verse ->
                                                val verseEnTranslation = verse.translationEnglish.ifBlank {
                                                    TafsirProvider.getTafsirForAyah(verse.surahId, verse.verseNumber, "muyassar").verseTranslationEn.ifBlank {
                                                        "Surah ${verse.surahNameEnglish} - Verse ${verse.verseNumber}"
                                                    }
                                                }
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = EmeraldPrimary.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "[${verse.surahId}:${verse.verseNumber}]",
                                                                fontSize = 11.sp,
                                                                fontFamily = TajawalFontFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                color = EmeraldPrimary,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = "Surah ${verse.surahNameEnglish}",
                                                            fontSize = 12.sp,
                                                            fontFamily = TajawalFontFamily,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = verseEnTranslation,
                                                        style = TextStyle(
                                                            fontFamily = TajawalFontFamily,
                                                            fontSize = 14.sp,
                                                            lineHeight = 22.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            textAlign = TextAlign.Start,
                                                            textDirection = TextDirection.Ltr
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
        }
    }

    // Popup Card when ANY Word is Clicked (Interactive Word Meaning & Verse Tafseer)
    activeClickedWord?.let { (word, verse) ->
        val meaning = remember(word, verse) {
            QuranDataProvider.lookupWordMeaning(word, verse)
        }

        val liveWordItemState = remember(word, verse) {
            mutableStateOf<QuranWordItem?>(null)
        }
        var isWordFetching by remember(word, verse) { mutableStateOf(false) }

        LaunchedEffect(word, verse) {
            val normClicked = QuranDataProvider.normalizeArabic(word)
            val cachedWords = QuranPageFetcher.getCachedWordsForVerse(verse.surahId, verse.verseNumber)
            val matched = cachedWords?.firstOrNull { QuranDataProvider.normalizeArabic(it.textUthmani) == normClicked }
            if (matched != null) {
                liveWordItemState.value = matched
            } else {
                isWordFetching = true
                val fetchedList = QuranPageFetcher.fetchWordsForAyah(verse.surahId, verse.verseNumber)
                val m = fetchedList.firstOrNull { QuranDataProvider.normalizeArabic(it.textUthmani) == normClicked }
                if (m != null) {
                    liveWordItemState.value = m
                }
                isWordFetching = false
            }
        }

        var liveTafsirText by remember(verse.surahId, verse.verseNumber) {
            mutableStateOf(
                TafsirProvider.getCachedTafsir(verse.surahId, verse.verseNumber, "muyassar")
                    ?: verse.tafsirShort.ifBlank {
                        TafsirProvider.getTafsirForAyah(verse.surahId, verse.verseNumber, "muyassar").commentaryText
                    }
            )
        }
        var isTafsirFetching by remember(verse.surahId, verse.verseNumber) { mutableStateOf(false) }

        LaunchedEffect(verse.surahId, verse.verseNumber) {
            val cached = TafsirProvider.getCachedTafsir(verse.surahId, verse.verseNumber, "muyassar")
            if (!cached.isNullOrBlank()) {
                liveTafsirText = cached
            } else {
                isTafsirFetching = true
                val fetched = TafsirProvider.fetchAyahTafsirLive(verse.surahId, verse.verseNumber, "muyassar")
                if (!fetched.isNullOrBlank()) {
                    liveTafsirText = fetched
                }
                isTafsirFetching = false
            }
        }

        AlertDialog(
            onDismissRequest = { activeClickedWord = null },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.setSelectedVerseForDetail(verse)
                    onNavigateToTafsir?.invoke(verse.surahId, verse.verseNumber)
                    activeClickedWord = null 
                }) {
                    Text(if (isArabic) "عرض التفسير الكامل للآية" else "View Full Verse Tafsir", style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold), color = EmeraldPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeClickedWord = null }) {
                    Text(if (isArabic) "إغلاق" else "Close", style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily))
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isArabic) "الكلمة: $word" else "Word: $word",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = QuranScript.fromId(userSettings.quranScriptType).getFontFamily(),
                                fontSize = 24.sp
                            ),
                            color = if (isDivineName(word)) Color(0xFFD32F2F) else EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { viewModel.playWordAudio(word) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "استماع للكلمة",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IslamicGold.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isArabic) "آية ${toArabicDigits(verse.verseNumber)}" else "Verse ${verse.verseNumber}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = IslamicGold
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Recitation & Audio Options Panel
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "خيارات تلاوة الصوت والاستماع:" else "Recitation & Audio Options:",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.playWordAudio(word) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isArabic) "نطق الكلمة" else "Pronounce Word", style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold))
                                }

                                Button(
                                    onClick = { viewModel.playAyahAudio(verse) },
                                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isArabic) "تلاوة الآية" else "Recite Verse", style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    if (isDivineName(word)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE),
                            border = BorderStroke(1.dp, Color(0xFFEF5350)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isArabic) "اسم الجلالة «الله»: خالق السماوات والأرض، الإله الحق المستحق للعبادة وحده لا شريك له." else "Name of Majesty 'Allah': Creator of the heavens and earth, the true God worthy of worship alone.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        val arMeaning = meaning.arabicMeaning.ifBlank {
                            "لفظ قرآني كريم في سياق سورة ${verse.surahNameArabic} (الآية ${verse.verseNumber})."
                        }
                        val rootVal = meaning.rootWord.ifBlank { if (isArabic) "---" else "N/A" }
                        val currentWordItem = liveWordItemState.value
                        val engVal = if (currentWordItem != null && currentWordItem.translationEn.isNotBlank()) currentWordItem.translationEn else meaning.englishMeaning

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFDFBF7),
                            border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isArabic) "معنى الكلمة: $arMeaning" else "Word Meaning: ${if (engVal.isNotBlank()) engVal else arMeaning}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = if (isArabic) "جذر الكلمة: [ $rootVal ]" else "Word Root: [ $rootVal ]",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.SemiBold),
                                    color = IslamicGold
                                )
                                if (engVal.isNotBlank() && isArabic) {
                                    Text(
                                        text = "English Meaning: $engVal",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (currentWordItem != null && currentWordItem.transliteration.isNotBlank()) {
                                    Text(
                                        text = "Transliteration: ${currentWordItem.transliteration}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "التفسير الميسر للآية ${toArabicDigits(verse.verseNumber)}:" else "Tafsir Muyassar for Verse ${verse.verseNumber}:",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                            color = IslamicGold
                        )
                        if (isTafsirFetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = IslamicGold,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Text(
                        text = liveTafsirText.ifBlank { "تفسير الآية ${verse.verseNumber} من سورة ${verse.surahNameArabic}." },
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily, lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    // Jump To Page Dialog
    if (showJumpDialog) {
        var pageInput by remember { mutableStateOf(currentPage.toString()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                    Text(
                        text = if (isArabic) "الانتقال السريع للصفحة" else "Quick Jump to Page",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isArabic) "الصفحة الحالية: ${toArabicDigits(currentPage)} من ٦٠٤" else "Current Page: $currentPage of 604",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            pageInput = clean
                        },
                        label = { Text(if (isArabic) "رقم الصفحة (1 - 604)" else "Page Number (1 - 604)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = IslamicGold)
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("jump_page_input")
                    )

                    // Quick Page Step Presets
                    Text(
                        text = if (isArabic) "خطوات سريعة:" else "Quick Steps:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-50, -10, +10, +50).forEach { delta ->
                            val target = (currentPage + delta).coerceIn(1, 604)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        pageInput = target.toString()
                                    }
                            ) {
                                Text(
                                    text = if (delta > 0) "+$delta" else "$delta",
                                    style = TextStyle(
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = pageInput.toIntOrNull()?.coerceIn(1, 604) ?: currentPage
                        viewModel.jumpToPage(target)
                        showJumpDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("jump_page_confirm_btn")
                ) {
                    Text(
                        text = if (isArabic) "انتقال الآن" else "Jump Now",
                        style = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text(
                        text = if (isArabic) "إلغاء" else "Cancel",
                        style = TextStyle(fontFamily = TajawalFontFamily)
                    )
                }
            }
        )
    }

    // Jump To Surah Overlay Menu Sheet
    if (showSurahPickerSheet) {
        var searchQuery by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("ALL") }

        val filteredSurahs = remember(searchQuery, selectedFilter) {
            QuranDataProvider.SURAHS.filter { s ->
                val matchesQuery = searchQuery.isBlank() ||
                        s.nameArabic.contains(searchQuery, ignoreCase = true) ||
                        s.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                        s.id.toString() == searchQuery.trim() ||
                        s.englishMeaning.contains(searchQuery, ignoreCase = true)

                val matchesFilter = when (selectedFilter) {
                    "MECCAN" -> s.revelationType == "Meccan"
                    "MEDINAN" -> s.revelationType == "Medinan"
                    else -> true
                }

                matchesQuery && matchesFilter
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showSurahPickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IslamicGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FormatListNumbered,
                                    contentDescription = null,
                                    tint = IslamicGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isArabic) "فهرس السور - الانتقال السريع" else "Jump to Surah",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = if (isArabic) "اختر أياً من 114 سورة للانتقال المباشر إليها" else "Select from 114 Surahs",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = TajawalFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = { showSurahPickerSheet = false },
                        modifier = Modifier.testTag("close_surah_picker_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isArabic) "ابحث باسم السورة أو رقمها (مثال: يس، 36)..." else "Search Surah name or number...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily)
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("surah_picker_search_input")
                )

                // Quick Filter Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text(if (isArabic) "جميع السور (114)" else "All (114)", fontFamily = TajawalFontFamily) },
                            leadingIcon = if (selectedFilter == "ALL") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "MECCAN",
                            onClick = { selectedFilter = "MECCAN" },
                            label = { Text(if (isArabic) "مكية 🕋" else "Meccan 🕋", fontFamily = TajawalFontFamily) },
                            leadingIcon = if (selectedFilter == "MECCAN") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "MEDINAN",
                            onClick = { selectedFilter = "MEDINAN" },
                            label = { Text(if (isArabic) "مدنية 🕌" else "Medinan 🕌", fontFamily = TajawalFontFamily) },
                            leadingIcon = if (selectedFilter == "MEDINAN") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }

                    val popularSurahs = listOf(
                        Pair(1, if (isArabic) "الفاتحة" else "Al-Fatihah"),
                        Pair(18, if (isArabic) "الكهف" else "Al-Kahf"),
                        Pair(36, if (isArabic) "يس" else "Yasin"),
                        Pair(55, if (isArabic) "الرحمن" else "Ar-Rahman"),
                        Pair(56, if (isArabic) "الواقعة" else "Al-Waqi'ah"),
                        Pair(67, if (isArabic) "الملك" else "Al-Mulk")
                    )

                    items(popularSurahs) { (surahNum, labelStr) ->
                        val targetS = QuranDataProvider.SURAHS.firstOrNull { it.id == surahNum }
                        if (targetS != null) {
                            SuggestionChip(
                                onClick = {
                                    viewModel.jumpToPage(targetS.startPage)
                                    showSurahPickerSheet = false
                                },
                                label = { Text("⚡ $labelStr", fontFamily = TajawalFontFamily, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                // Surah List
                if (filteredSurahs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "لم يتم العثور على سورة تطابق بحثك" else "No Surah found matching search",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = TajawalFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredSurahs, key = { it.id }) { item ->
                            val isCurrentSurah = item.id == surah.id
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isCurrentSurah) {
                                    if (isDarkMode) Color(0x3310B981) else Color(0xFFF0FDF4)
                                } else if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    width = if (isCurrentSurah) 1.5.dp else 1.dp,
                                    color = if (isCurrentSurah) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.jumpToPage(item.startPage)
                                        showSurahPickerSheet = false
                                    }
                                    .testTag("surah_picker_item_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Surah Number Badge Frame (Fixed 44.dp for vertical alignment)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isCurrentSurah) EmeraldPrimary else IslamicGold.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, if (isCurrentSurah) EmeraldPrimary else IslamicGold.copy(alpha = 0.5f)),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = toArabicDigits(item.id),
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = if (isCurrentSurah) Color.White else if (isDarkMode) IslamicGold else Color(0xFF1B5E55)
                                                    )
                                                )
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isArabic) "سورة ${item.nameArabic}" else "Surah ${item.nameEnglish}",
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp,
                                                        color = if (isCurrentSurah) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )

                                                if (isCurrentSurah) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = EmeraldPrimary
                                                    ) {
                                                        Text(
                                                            text = if (isArabic) "الحالية" else "Current",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontFamily = TajawalFontFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp,
                                                                color = Color.White
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "${item.nameEnglish} • ${item.englishMeaning}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = TajawalFontFamily,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (item.revelationType == "Meccan") Color(0xFFFEF3C7) else Color(0xFFE0E7FF)
                                                ) {
                                                    Text(
                                                        text = if (isArabic) {
                                                            if (item.revelationType == "Meccan") "مكية 🕋" else "مدنية 🕌"
                                                        } else item.revelationType,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = TajawalFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp,
                                                            color = if (item.revelationType == "Meccan") Color(0xFF92400E) else Color(0xFF3730A3)
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                Text(
                                                    text = if (isArabic)
                                                        "${toArabicDigits(item.versesCount)} آية • صفحة ${toArabicDigits(item.startPage)}"
                                                    else
                                                        "${item.versesCount} Verses • Page ${item.startPage}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = TajawalFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Jump to Surah",
                                        tint = if (isCurrentSurah) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // Page Level Tafseer & Word Meanings Sheet
    if (showPageTafseerSheet) {
        var pageVersesForTafseer by remember(currentPage) {
            val matching = currentVerses.filter { it.pageNumber == currentPage }
            val initial = if (matching.isNotEmpty()) matching else (QuranPageFetcher.getCachedPage(currentPage) ?: QuranDataProvider.getVersesForPage(currentPage))
            mutableStateOf(initial)
        }
        var isPageTafseerLoading by remember(currentPage) {
            mutableStateOf(pageVersesForTafseer.isEmpty())
        }

        LaunchedEffect(currentPage) {
            if (pageVersesForTafseer.isEmpty()) {
                isPageTafseerLoading = true
                val fetched = QuranPageFetcher.fetchAndCachePage(currentPage)
                if (!fetched.isNullOrEmpty()) {
                    pageVersesForTafseer = fetched
                } else {
                    pageVersesForTafseer = QuranDataProvider.getVersesForPage(currentPage)
                }
                isPageTafseerLoading = false
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showPageTafseerSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = EmeraldPrimary)
                        Text(
                            text = if (isArabic) "تفسير صفحة ${toArabicDigits(currentPage)} - آية بآية" else "Page $currentPage Tafsir - Verse by Verse",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showPageTafseerSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                if (isPageTafseerLoading && pageVersesForTafseer.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = IslamicGold)
                            Text(
                                text = if (isArabic) "جاري تحميل آيات وتفسير الصفحة..." else "Loading page verses and Tafsir...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pageVersesForTafseer) { verse ->
                            InteractiveVerseCard(
                                verse = verse,
                                viewModel = viewModel,
                                onBookmark = {
                                    viewModel.addCurrentPageBookmark(note = "آية ${verse.verseNumber}")
                                    Toast.makeText(context, "تم حفظ العلامة", Toast.LENGTH_SHORT).show()
                                },
                                onNavigateToTafsir = { surahId, verseNum ->
                                    showPageTafseerSheet = false
                                    viewModel.setSelectedVerseForDetail(verse)
                                    onNavigateToTafsir?.invoke(surahId, verseNum)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Quran Script & Calligraphy Selection Modal Bottom Sheet
    if (showScriptPickerSheet) {
        val scriptSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showScriptPickerSheet = false },
            sheetState = scriptSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                            color = IslamicGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = IslamicGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isArabic) "رسم ونوع خط صفحات المصحف" else "Quran Script & Mushaf Style",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = if (isArabic) "اختر خطك المفضل لعرض المصحف الكريم" else "Choose your preferred Quranic font style",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = TajawalFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = { showScriptPickerSheet = false },
                        modifier = Modifier.testTag("close_script_picker_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                val selectedScript = QuranScript.fromId(userSettings.quranScriptType)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuranScript.entries.forEach { script ->
                        val isSelected = script == selectedScript
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateQuranScript(script.id)
                                    Toast.makeText(
                                        context,
                                        if (isArabic) "تم تفعيل ${script.titleArabic}" else "${script.titleEnglish} activated",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .testTag("reader_script_option_${script.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.updateQuranScript(script.id)
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                                        )
                                        Column {
                                            Text(
                                                text = if (isArabic) script.titleArabic else script.titleEnglish,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = TajawalFontFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isArabic) script.subtitleArabic else script.subtitleEnglish,
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                                                color = IslamicGold
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldPrimary
                                        ) {
                                            Text(
                                                text = if (isArabic) "مفعّل" else "Active",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (isArabic) script.descriptionArabic else script.descriptionEnglish,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = TajawalFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                // Live Verse Sample in this Exact Font
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFDF9),
                                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = script.sampleVerse,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = script.getFontFamily(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 21.sp,
                                            lineHeight = 40.sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Single Verse Detail Bottom Sheet with Audio Recitation Controllers
    selectedVerse?.let { verse ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.setSelectedVerseForDetail(null) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InteractiveVerseCard(
                    verse = verse,
                    viewModel = viewModel,
                    onBookmark = {
                        viewModel.addCurrentPageBookmark(note = "آية ${verse.verseNumber}")
                        Toast.makeText(context, "تم حفظ العلامة", Toast.LENGTH_SHORT).show()
                        viewModel.setSelectedVerseForDetail(null)
                    },
                    onNavigateToTafsir = onNavigateToTafsir
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveVerseCard(
    verse: Verse,
    viewModel: QuranViewModel,
    onBookmark: () -> Unit,
    onNavigateToTafsir: ((Int, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
    val isBufferingAudio by viewModel.isBufferingAudio.collectAsState()
    val currentPlayingVerse by viewModel.currentPlayingVerse.collectAsState()
    val currentPlayingSurah by viewModel.currentPlayingSurah.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"
    val selectedReciter by viewModel.selectedReciter.collectAsState()

    val isVerseBookmarked = remember(bookmarks, verse) {
        bookmarks.any { it.pageNumber == verse.pageNumber && (it.verseNumber == verse.verseNumber || it.note.contains("${verse.verseNumber}")) }
    }

    val isThisVersePlaying = currentPlayingVerse?.first == verse.surahId && currentPlayingVerse?.second == verse.verseNumber && isPlayingAudio
    val isThisSurahPlaying = currentPlayingSurah == verse.surahId && isPlayingAudio
    val currentTheme = userSettings.effectiveThemeMode()
    val isDarkMode = currentTheme == "DARK" || currentTheme == "SEPIA" || (currentTheme == "SYSTEM" && androidx.compose.foundation.isSystemInDarkTheme())

    val cleanText = cleanUthmaniText(verse.textUthmani)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFDF9),
        border = BorderStroke(
            width = if (isThisVersePlaying) 2.dp else 1.dp,
            color = if (isThisVersePlaying) MaterialTheme.colorScheme.primary else if (isDarkMode) Color(0xFF334155) else Color(0xFFE2D6C3)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Navigation & Share/Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isArabic) "الآية ${toArabicDigits(verse.verseNumber)} • سورة ${verse.surahNameArabic}" else "Verse ${verse.verseNumber} • Surah ${verse.surahNameEnglish}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateToPreviousVerse() }) {
                        Icon(
                            imageVector = Icons.Default.NavigateBefore,
                            contentDescription = "Prev Verse",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.navigateToNextVerse() }) {
                        Icon(
                            imageVector = Icons.Default.NavigateNext,
                            contentDescription = "Next Verse",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "$cleanText ﴿${toArabicDigits(verse.verseNumber)}﴾\n\n[سورة ${verse.surahNameArabic} - آية ${verse.verseNumber}]")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Verse"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(onClick = {
                        if (isVerseBookmarked) {
                            val existing = bookmarks.filter { it.pageNumber == verse.pageNumber && (it.verseNumber == verse.verseNumber || it.note.contains("${verse.verseNumber}")) }
                            existing.forEach { viewModel.deleteBookmark(it.id) }
                            Toast.makeText(context, if (isArabic) "تم إزالة العلامة المرجعية" else "Bookmark removed", Toast.LENGTH_SHORT).show()
                        } else {
                            onBookmark()
                        }
                    }) {
                        Icon(
                            imageVector = if (isVerseBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isVerseBookmarked) Color(0xFFDC2626) else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Verse Text Rendering with Luxury Parchment Frame & Amiri Quran Calligraphy
            ParchmentPaperBackground(
                isDarkMode = isDarkMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        border = BorderStroke(
                            width = if (isThisVersePlaying) 2.dp else 1.dp,
                            color = if (isThisVersePlaying) Color(0xFFF59E0B) else IslamicGold.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Subtle Top Ornamental Badge for Uthmani Script
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = "✤ ",
                            style = TextStyle(fontFamily = TajawalFontFamily, fontSize = 10.sp),
                            color = IslamicGold
                        )
                        Text(
                            text = if (isArabic) "الرَّسْمُ العُثْمَانِيُّ • الآيَةُ ${toArabicDigits(verse.verseNumber)}" else "Uthmani Script • Verse ${verse.verseNumber}",
                            style = TextStyle(
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isDarkMode) IslamicGold else Color(0xFF1B5E55)
                        )
                        Text(
                            text = " ✤",
                            style = TextStyle(fontFamily = TajawalFontFamily, fontSize = 10.sp),
                            color = IslamicGold
                        )
                    }

                    val formattedVerseText = remember(cleanText, userSettings.enableTajweed, isDarkMode, isThisVersePlaying) {
                        if (userSettings.enableTajweed && !isThisVersePlaying) {
                            TajweedHelper.buildTajweedAnnotatedString(
                                text = cleanText,
                                enableTajweed = true,
                                baseColor = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF0F172A),
                                isDarkMode = isDarkMode
                            )
                        } else {
                            buildAnnotatedString { append(cleanText) }
                        }
                    }

                    val selectedScript = QuranScript.fromId(userSettings.quranScriptType)
                    val scriptFontFamily = selectedScript.getFontFamily()

                    val finalAyahAnnotated = remember(formattedVerseText, verse.verseNumber, isDarkMode, isThisVersePlaying, selectedScript) {
                        buildAnnotatedString {
                            append(formattedVerseText)
                            append("  ")
                            withStyle(
                                SpanStyle(
                                    color = if (isThisVersePlaying) Color(0xFFF59E0B) else if (isDarkMode) Color(0xFFFDE68A) else IslamicGold,
                                    fontFamily = scriptFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("﴿${toArabicDigits(verse.verseNumber)}﴾")
                            }
                        }
                    }

                    Text(
                        text = finalAyahAnnotated,
                        style = TextStyle(
                            fontFamily = scriptFontFamily,
                            fontSize = (userSettings.fontSizeSp * 1.15f).coerceIn(22f, 34f).sp,
                            lineHeight = (userSettings.fontSizeSp * 1.85f).coerceIn(38f, 62f).sp,
                            textAlign = TextAlign.Center,
                            textDirection = TextDirection.ContentOrRtl
                        ),
                        color = if (isThisVersePlaying) {
                            if (isDarkMode) Color(0xFFFFD54F) else Color(0xFF92400E)
                        } else if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF1A202C),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // English Translation Box in InteractiveVerseCard
            if (userSettings.showEnglishTranslation) {
                val verseEnTrans = verse.translationEnglish.ifBlank {
                    TafsirProvider.getTafsirForAyah(verse.surahId, verse.verseNumber, "muyassar").verseTranslationEn.ifBlank {
                        "Surah ${verse.surahNameEnglish} - Verse ${verse.verseNumber}"
                    }
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "English Translation:",
                                style = TextStyle(
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = EmeraldPrimary
                            )
                            Text(
                                text = verseEnTrans,
                                style = TextStyle(
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Start,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Audio Recitation Controller Panel (استماع للآية + اختيار القارئ + تكرار + استماع للسورة)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reciter Switcher Dropdown Bar
                    var showVerseReciterMenu by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showVerseReciterMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (isArabic) "القارئ: ${selectedReciter.nameArabic}" else "Reciter: ${selectedReciter.nameEnglish}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose Reciter", tint = EmeraldPrimary)
                            }
                        }

                        DropdownMenu(
                            expanded = showVerseReciterMenu,
                            onDismissRequest = { showVerseReciterMenu = false }
                        ) {
                            viewModel.reciters.forEach { reciter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isArabic) reciter.nameArabic else reciter.nameEnglish,
                                            fontWeight = if (reciter.id == selectedReciter.id) FontWeight.Bold else FontWeight.Normal,
                                            color = if (reciter.id == selectedReciter.id) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.setReciter(reciter)
                                        showVerseReciterMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Pause Ayah Button
                        Button(
                            onClick = {
                                if (isThisVersePlaying) {
                                    viewModel.stopAudioPlayback()
                                } else {
                                    viewModel.playAyahAudio(verse)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isThisVersePlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                contentColor = if (isThisVersePlaying) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isBufferingAudio && isThisVersePlaying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "جاري التحميل..." else "Loading...", style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily))
                            } else {
                                Icon(
                                    imageVector = if (isThisVersePlaying) Icons.Default.Pause else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isThisVersePlaying) (if (isArabic) "إيقاف القراءة" else "Stop Audio") else (if (isArabic) "استماع للآية 🔊" else "Recite Ayah 🔊"),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Play Full Surah Button
                        if (isThisSurahPlaying) {
                            Button(
                                onClick = { viewModel.stopAudioPlayback() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isBufferingAudio) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause Surah", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (isArabic) "إيقاف السورة ⏸" else "Stop Surah ⏸",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.playSpecificSurahAudio(verse.surahId) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArabic) "السورة كاملة 🎧" else "Full Surah 🎧",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Loop / Repeat Mode Controls for Memorization (تكرار الآية)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تكرار القراءة للتحفيظ:" else "Repeat Recitation:",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AyahRepeatMode.entries.forEach { mode ->
                                val isSelected = repeatMode == mode
                                Surface(
                                    selected = isSelected,
                                    onClick = { viewModel.setAyahRepeatMode(mode) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    border = if (isSelected) BorderStroke(1.dp, IslamicGold) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) mode.labelAr else mode.labelEn,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Tafseer Section Specific To THIS Verse
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "تفسير الآية ${toArabicDigits(verse.verseNumber)} من سورة ${verse.surahNameArabic}:" else "Tafsir of Verse ${verse.verseNumber} - Surah ${verse.surahNameEnglish}:",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                    color = IslamicGold
                )

                var selectedBookId by remember(verse.surahId, verse.verseNumber) { mutableStateOf("muyassar") }

                // Tafsir Book Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TafsirProvider.TAFSIR_BOOKS) { book ->
                        val isSelected = selectedBookId == book.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedBookId = book.id },
                            label = {
                                Text(
                                    text = if (isArabic) book.nameArabic else book.nameEnglish,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IslamicGold,
                                selectedLabelColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                val tafsirDetail = remember(verse.surahId, verse.verseNumber, selectedBookId) {
                    TafsirProvider.getTafsirForAyah(verse.surahId, verse.verseNumber, selectedBookId)
                }

                var liveCommentaryText by remember(verse.surahId, verse.verseNumber, selectedBookId) {
                    mutableStateOf(tafsirDetail.commentaryText)
                }
                var isCommentaryLoading by remember(verse.surahId, verse.verseNumber, selectedBookId) {
                    mutableStateOf(false)
                }

                LaunchedEffect(verse.surahId, verse.verseNumber, selectedBookId) {
                    val cached = TafsirProvider.getCachedTafsir(verse.surahId, verse.verseNumber, selectedBookId)
                    if (!cached.isNullOrBlank()) {
                        liveCommentaryText = cached
                    } else {
                        isCommentaryLoading = true
                        val fetched = TafsirProvider.fetchAyahTafsirLive(verse.surahId, verse.verseNumber, selectedBookId)
                        if (!fetched.isNullOrBlank()) {
                            liveCommentaryText = fetched
                        }
                        isCommentaryLoading = false
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFFFFBF5),
                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isCommentaryLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = IslamicGold, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "جاري تحديث التفسير..." else "Updating commentary...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                                    color = IslamicGold
                                )
                            }
                        }

                        Text(
                            text = liveCommentaryText.ifBlank { tafsirDetail.commentaryText },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = TajawalFontFamily,
                                fontSize = 15.sp,
                                lineHeight = 25.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (tafsirDetail.keyLessons.isNotEmpty()) {
                            HorizontalDivider(color = IslamicGold.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = if (isArabic) "💡 من هدايات الآية وفوائدها:" else "💡 Key Verse Lessons & Guidance:",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary
                            )
                            tafsirDetail.keyLessons.forEach { lesson ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("•", style = MaterialTheme.typography.bodySmall, color = IslamicGold)
                                    Text(
                                        text = lesson,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Word Meanings for this Verse Section
                var showVerseWords by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().clickable { showVerseWords = !showVerseWords }
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isArabic) "معاني مفردات الآية (اضغط للتفصيل)" else "Verse Word Meanings (Click to Expand)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                    color = EmeraldPrimary
                                )
                            }
                            Icon(
                                imageVector = if (showVerseWords) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        }

                        if (showVerseWords) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            val cleanWords = remember(verse.textUthmani) {
                                verse.textUthmani.split(" ")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() && !it.startsWith("﴿") && !it.endsWith("﴾") }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                cleanWords.forEach { w ->
                                    val wordMeaning = QuranDataProvider.lookupWordMeaning(w, verse)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = w,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = QuranScript.fromId(userSettings.quranScriptType).getFontFamily(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            ),
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = if (isArabic) wordMeaning.arabicMeaning else wordMeaning.englishMeaning.ifBlank { wordMeaning.arabicMeaning },
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Asbab Al-Nuzul (Reason for Revelation) Section for this Ayah
                val asbabNuzul = remember(verse.surahId, verse.verseNumber) {
                    AsbabAlNuzulProvider.getAsbabNuzul(verse.surahId, verse.verseNumber)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkMode) Color(0xFF1E1B18) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "📜 سبب نزول الآية الكريمة:" else "📜 Reason for Revelation:",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                color = if (isDarkMode) Color(0xFFFCD34D) else Color(0xFF92400E)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = IslamicGold.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = asbabNuzul.contextType,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily, fontSize = 10.sp),
                                    color = if (isDarkMode) Color(0xFFFCD34D) else Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isArabic) asbabNuzul.reasonArabic else asbabNuzul.reasonEnglish.ifBlank { asbabNuzul.reasonArabic },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = TajawalFontFamily,
                                fontSize = 14.sp,
                                lineHeight = 23.sp
                            ),
                            color = if (isDarkMode) Color(0xFFFDF6B2) else Color(0xFF451A03)
                        )

                        Text(
                            text = "المصدر: ${asbabNuzul.sourceBook}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily, fontSize = 10.sp),
                            color = if (isDarkMode) Color(0xFFD97706) else Color(0xFF78350F)
                        )
                    }
                }

                // Button to open Full Tafsir Screen
                Button(
                    onClick = {
                        viewModel.setSelectedVerseForDetail(null)
                        onNavigateToTafsir?.invoke(verse.surahId, verse.verseNumber)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_full_tafsir_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "عرض في صفحة التفسير والعلوم الكاملة 📖" else "View in Full Tafsir & Sciences Page 📖",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun ParchmentPaperBackground(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val baseColor = if (isDarkMode) Color(0xFF1B2232) else Color(0xFFFBF6EE)
    val edgeVignetteColor = if (isDarkMode) Color(0xFF101522) else Color(0xFFEADBCE)
    val borderGold = if (isDarkMode) IslamicGold.copy(alpha = 0.45f) else Color(0xFFC5A059).copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .drawBehind {
                // 1. Base Solid Parchment Fill
                drawRect(color = baseColor)

                // 2. Soft Radial Vignette for authentic aged parchment depth
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            edgeVignetteColor.copy(alpha = 0.7f)
                        ),
                        center = size.center,
                        radius = size.maxDimension * 0.75f
                    )
                )

                // 3. Subtle Organic Papyrus Grain / Texture Fiber Dots
                val grainColor = if (isDarkMode) Color.White.copy(alpha = 0.018f) else Color(0xFF8B6B38).copy(alpha = 0.035f)
                val random = java.util.Random(10042L)
                for (i in 0..220) {
                    val x = random.nextFloat() * size.width
                    val y = random.nextFloat() * size.height
                    val radiusPx = 0.7f + random.nextFloat() * 1.6f
                    drawCircle(
                        color = grainColor,
                        radius = radiusPx,
                        center = Offset(x, y)
                    )
                }

                // 4. Double Gold Framing Lines (Classical Printed Mushaf Margin Frame)
                val marginOuter = 7.dp.toPx()
                val marginInner = 11.dp.toPx()
                drawRect(
                    color = borderGold,
                    topLeft = Offset(marginOuter, marginOuter),
                    size = Size(size.width - marginOuter * 2, size.height - marginOuter * 2),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawRect(
                    color = borderGold.copy(alpha = 0.35f),
                    topLeft = Offset(marginInner, marginInner),
                    size = Size(size.width - marginInner * 2, size.height - marginInner * 2),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            },
        content = content
    )
}

@Composable
fun OrnateSurahHeaderBanner(
    surah: com.example.domain.model.Surah,
    isDarkMode: Boolean,
    isArabic: Boolean = true,
    modifier: Modifier = Modifier
) {
    val frameBorderColor = if (isDarkMode) IslamicGold else Color(0xFFC5A059)
    val titleTextColor = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF1B5E55)
    val subtitleTextColor = if (isDarkMode) Color(0xFFFDF0CD) else Color(0xFF2D3748)
    val badgeBgColor = if (isDarkMode) Color(0xFF252D3F) else Color(0xFFF3E8D8)

    val revTypeStr = if (isArabic) {
        if (surah.revelationType.contains("Meccan", ignoreCase = true)) "مكّية" else "مدنيّة"
    } else {
        if (surah.revelationType.contains("Meccan", ignoreCase = true)) "Meccan" else "Medinan"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkMode) {
                        listOf(Color(0xFF1F2937), Color(0xFF111827))
                    } else {
                        listOf(Color(0xFFFFFDF9), Color(0xFFF7EFE2))
                    }
                )
            )
            .border(
                border = BorderStroke(1.5.dp, frameBorderColor.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                // Draw Islamic Ornamental Frame Corner Nodes & Accent Lines
                val goldPx = frameBorderColor.copy(alpha = 0.5f)
                val strokePx = 1.dp.toPx()

                // Corner Diamonds
                val cornerOffset = 10.dp.toPx()
                val diamondSize = 4.dp.toPx()

                val corners = listOf(
                    Offset(cornerOffset, cornerOffset),
                    Offset(size.width - cornerOffset, cornerOffset),
                    Offset(cornerOffset, size.height - cornerOffset),
                    Offset(size.width - cornerOffset, size.height - cornerOffset)
                )

                corners.forEach { pt ->
                    drawCircle(color = frameBorderColor, radius = diamondSize, center = pt)
                }

                // Inner Decorative Border Frame Line
                val innerMargin = 6.dp.toPx()
                drawRect(
                    color = goldPx,
                    topLeft = Offset(innerMargin, innerMargin),
                    size = Size(size.width - innerMargin * 2, size.height - innerMargin * 2),
                    style = Stroke(width = strokePx)
                )
            }
            .padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Surah Badges (Revelation place & Verses count & Order)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Revelation Badge (Meccan/Medinan)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = badgeBgColor,
                border = BorderStroke(1.dp, frameBorderColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕌 $revTypeStr",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = subtitleTextColor
                    )
                }
            }

            // Order Number Badge
            Surface(
                shape = CircleShape,
                color = frameBorderColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, frameBorderColor.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier.size(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${surah.id}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = titleTextColor
                    )
                }
            }

            // Verse Count Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = badgeBgColor,
                border = BorderStroke(1.dp, frameBorderColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "آياتها ${toArabicDigits(surah.versesCount)}" else "${surah.versesCount} Verses",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = subtitleTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center Surah Calligraphy Title with Side Arabesque Ornaments
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "﴿",
                style = TextStyle(
                    fontFamily = TajawalFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = frameBorderColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isArabic) "سُورَةُ ${surah.nameArabic}" else "Surah ${surah.nameEnglish}",
                style = TextStyle(
                    fontFamily = AmiriQuranFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    textDirection = if (isArabic) TextDirection.Rtl else TextDirection.Ltr
                ),
                color = titleTextColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "﴾",
                style = TextStyle(
                    fontFamily = TajawalFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = frameBorderColor
            )
        }

        // Basmala (if applicable for this Surah)
        if (surah.id != 1 && surah.id != 9) {
            Spacer(modifier = Modifier.height(12.dp))
            OrnateBasmalaBanner(isDarkMode = isDarkMode)
        }
    }
}

@Composable
fun OrnateBasmalaBanner(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val goldColor = if (isDarkMode) IslamicGold else Color(0xFFB8860B)
    val basmalaTextColor = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF1A202C)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkMode) Color(0xFF182232) else Color(0xFFFFFBF2),
        border = BorderStroke(1.dp, goldColor.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✤ ──────",
                style = TextStyle(
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = goldColor.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                style = TextStyle(
                    fontFamily = AmiriQuranFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    textDirection = TextDirection.Rtl
                ),
                color = basmalaTextColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "────── ✤",
                style = TextStyle(
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = goldColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun GoldenMushafPageView(
    pageNum: Int,
    pageVerses: List<Verse>,
    isDarkMode: Boolean,
    isArabic: Boolean,
    isPlayingAudio: Boolean,
    currentPlayingVerse: Pair<Int, Int>?,
    isImmersive: Boolean,
    onToggleImmersive: () -> Unit,
    onVerseClick: (Verse) -> Unit,
    onWordMeaningClick: (String, Verse) -> Unit,
    onOpenPageTafsir: () -> Unit,
    onToggleBookmark: () -> Unit = {},
    isBookmarked: Boolean = false,
    onPlayPageAudio: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onSwitchToTextView: () -> Unit = {},
    onOpenSurahPicker: () -> Unit = {},
    onOpenJumpDialog: () -> Unit = {},
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onToggleDarkMode: () -> Unit = {},
    pageScale: Float = 1.05f,
    onPageScaleChange: (Float) -> Unit = {},
    onOpenScaleDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedAyahOnPage by remember { mutableStateOf<Verse?>(null) }
    var showAyatQuickBar by remember { mutableStateOf(false) }
    var isFillWidth by remember { mutableStateOf(true) }
    var isFullscreenControlsExpanded by remember { mutableStateOf(true) }
    // Night font mode: "WHITE" (default for dark theme: crisp white font), "SEPIA" (warm golden sepia font), "ORIGINAL" (original paper)
    var nightFontColorMode by remember { mutableStateOf("WHITE") }

    // Default zoomed view (1.05f) as requested: full width comfortable reading without manual zoom needed
    val effectiveScale = 1.05f

    val imageUrl = remember(pageNum) { QuranPageFetcher.getPageImageUrl(pageNum) }
    val surahOnPage = remember(pageNum) { QuranDataProvider.getSurahByPage(pageNum) }
    val juzForPage = remember(pageNum) { QuranDataProvider.getJuzByPage(pageNum) }

    // White Inversion Matrix for Dark Mode: turns white paper into dark night canvas & black calligraphy into crisp white font (#FFFFFF)
    val whiteDarkColorMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -1.0f,  0.0f,  0.0f, 0.0f, 255f,
                 0.0f, -1.0f,  0.0f, 0.0f, 255f,
                 0.0f,  0.0f, -1.0f, 0.0f, 255f,
                 0.0f,  0.0f,   0.0f, 1.0f,   0f
            )
        )
    }

    // Sepia Inversion Matrix for Dark Mode: turns white paper into dark night canvas & black calligraphy into warm golden sepia (#FDE68A)
    val sepiaDarkColorMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -1.0f,  0.0f,  0.0f, 0.0f, 253f,
                 0.0f, -0.92f,  0.0f, 0.0f, 230f,
                 0.0f,  0.0f, -0.65f, 0.0f, 138f,
                 0.0f,  0.0f,   0.0f, 1.0f,   0f
            )
        )
    }

    val isDarkParchment = isDarkMode && nightFontColorMode != "ORIGINAL"

    // Animated padding between standard mode and full screen mode for smooth visual transition
    // A subtle right margin is explicitly preserved (5.dp) so that on physical devices the text and ornate border do not touch the screen edge.
    val animatedStartPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isImmersive) 2.dp else 4.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "startPadding"
    )
    val animatedEndPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isImmersive) 2.dp else 4.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "endPadding"
    )
    val animatedTopPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isImmersive) 1.dp else 8.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "topPadding"
    )
    val animatedBottomPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isImmersive) 1.dp else 4.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "bottomPadding"
    )

    // Gesture handling:
    // - Default zoomed view (1.05f) is active by default; manual zoom functionality is removed per user request.
    // - Natural reading swipe direction:
    //   * Swiping LEFT (totalDragX < -swipeThreshold): flips to PREVIOUS page (onPreviousPage())
    //   * Swiping RIGHT (totalDragX > swipeThreshold): flips to NEXT page (onNextPage())
    // - Single tap and double tap toggle full screen controls cleanly.
    val gestureModifier = Modifier
        .pointerInput(pageNum) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                var hasFlippedPage = false
                var totalDragX = 0f
                var totalDragY = 0f

                do {
                    val event = awaitPointerEvent()
                    val canceled = event.changes.any { it.isConsumed }
                    if (canceled) break

                    val activePointers = event.changes.filter { it.pressed }
                    if (activePointers.size == 1) {
                        val pan = event.calculatePan()
                        totalDragX += pan.x
                        totalDragY += pan.y

                        val swipeThreshold = 40.dp.toPx()
                        val isHorizontalSwipe = kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY) * 1.2f

                        if (!hasFlippedPage && isHorizontalSwipe) {
                            // Swiping LEFT flips to PREVIOUS PAGE (as reading Arabic Quran left-to-right)
                            if (totalDragX < -swipeThreshold) {
                                hasFlippedPage = true
                                if (pageNum > 1) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPreviousPage()
                                } else {
                                    Toast.makeText(context, if (isArabic) "أول صفحة في المصحف الشريف" else "First page of the Quran", Toast.LENGTH_SHORT).show()
                                }
                            }
                            // Swiping RIGHT flips to NEXT PAGE
                            else if (totalDragX > swipeThreshold) {
                                hasFlippedPage = true
                                if (pageNum < 604) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNextPage()
                                } else {
                                    Toast.makeText(context, if (isArabic) "آخر صفحة في المصحف الشريف" else "Last page of the Quran", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            }
        }
        .pointerInput(pageNum) {
            detectTapGestures(
                onTap = {
                    if (isImmersive) {
                        isFullscreenControlsExpanded = !isFullscreenControlsExpanded
                    }
                },
                onDoubleTap = {
                    if (isImmersive) {
                        isFullscreenControlsExpanded = !isFullscreenControlsExpanded
                    }
                }
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkParchment) Color(0xFF0F172A) else Color(0xFFFFFBF2))
            .let { if (isImmersive) it.statusBarsPadding().navigationBarsPadding() else it }
    ) {
        // Main Box containing the Quran Page image and floating controls
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            // Fullscreen Quran Page High-Res Image (Exact Golden Quran 15-Line Mushaf Layout)
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                colorFilter = if (isDarkMode) {
                    when (nightFontColorMode) {
                        "WHITE" -> ColorFilter.colorMatrix(whiteDarkColorMatrix)
                        "SEPIA" -> ColorFilter.colorMatrix(sepiaDarkColorMatrix)
                        else -> null
                    }
                } else null,
                contentScale = ContentScale.Fit,
                contentDescription = "Mushaf Page $pageNum",
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = EmeraldPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = if (isArabic) "جارٍ تحميل صفحة المصحف الذهبي عالية الدقة..." else "Loading HD Golden Mushaf Page...",
                                style = TextStyle(
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isDarkMode) Color.LightGray else Color.DarkGray
                            )
                        }
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (isArabic) "صفحة المصحف رقم ${toArabicDigits(pageNum)}" else "Mushaf Page $pageNum",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = animatedStartPadding,
                        end = animatedEndPadding,
                        top = animatedTopPadding,
                        bottom = animatedBottomPadding
                    )
                    .then(gestureModifier)
                    .graphicsLayer {
                        scaleX = effectiveScale
                        scaleY = effectiveScale
                    }
            )

            // Floating Top Fullscreen Bar (Page Number, Bookmark, Juz Number, Surah, Exit Fullscreen)
            androidx.compose.animation.AnimatedVisibility(
                visible = isImmersive,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFBF2)).copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.6f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bookmark & Juz & Page Number Group
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Interactive Bookmark Button
                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isBookmarked) IslamicGold else (if (isDarkMode) Color.LightGray else Color.Gray),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Juz' Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = IslamicGold.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = if (isArabic) "الجزء ${toArabicDigits(juzForPage.number)}" else "Juz ${juzForPage.number}",
                                    style = TextStyle(
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            // Page Number Badge (Clickable to jump quickly to any page)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldPrimary.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { onOpenJumpDialog() }
                                    .testTag("fullscreen_page_badge_jump")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Quick Jump",
                                        tint = if (isDarkMode) Color(0xFF34D399) else Color(0xFF065F46),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "صفحة ${toArabicDigits(pageNum)}" else "Page $pageNum",
                                        style = TextStyle(
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF065F46)
                                    )
                                }
                            }
                        }

                        // Surah Name, Dark Mode & Exit Fullscreen Button Group
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Surah title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenSurahPicker() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "سورة ${surahOnPage.nameArabic}" else surahOnPage.nameEnglish,
                                    style = TextStyle(
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF1B5E55)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = IslamicGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Dark Mode Toggle Icon Button
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2D6C3).copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onToggleDarkMode() }
                                    .testTag("fullscreen_dark_mode_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                                        tint = if (isDarkMode) IslamicGold else Color(0xFF1B5E55),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            // Exit Fullscreen Button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2D6C3).copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .clickable { onToggleImmersive() }
                                    .testTag("fullscreen_exit_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = "Exit Fullscreen",
                                        tint = if (isDarkMode) IslamicGold else Color(0xFF1B5E55),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Exit Fullscreen",
                                        tint = if (isDarkMode) Color.White else Color(0xFF1B5E55),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Bottom Action Overlay in Fullscreen Mode (Play audio, Page Tafsir, Ayat list with Tafsir/Recite triggers)
            androidx.compose.animation.AnimatedVisibility(
                visible = isImmersive,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 10.dp, start = 12.dp, end = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Fullscreen Ayat Quick Bar (when toggled on)
                    AnimatedVisibility(
                        visible = showAyatQuickBar && pageVerses.isNotEmpty() && isFullscreenControlsExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)).copy(alpha = 0.96f),
                            border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "آيات الصفحة (اضغط للاستماع أو عرض التفسير والمعاني):" else "Page Ayat (Tap to play, view Tafsir & Meanings):",
                                        style = TextStyle(
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF0D5C3A),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    IconButton(
                                        onClick = { showAyatQuickBar = false },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(pageVerses) { verse ->
                                        val isSelected = selectedAyahOnPage?.id == verse.id
                                        val isVersePlaying = isPlayingAudio &&
                                                currentPlayingVerse?.first == verse.surahId &&
                                                currentPlayingVerse?.second == verse.verseNumber

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = when {
                                                isVersePlaying -> EmeraldPrimary
                                                isSelected -> IslamicGold
                                                else -> if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected || isVersePlaying) Color.Transparent else if (isDarkMode) Color(0xFF475569) else Color(0xFFCBD5E1)
                                            ),
                                            modifier = Modifier.clickable {
                                                selectedAyahOnPage = if (selectedAyahOnPage?.id == verse.id) null else verse
                                                onVerseClick(verse)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (isVersePlaying) {
                                                    Icon(
                                                        imageVector = Icons.Default.VolumeUp,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "الآية ${toArabicDigits(verse.verseNumber)}",
                                                    style = TextStyle(
                                                        fontFamily = TajawalFontFamily,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = when {
                                                        isVersePlaying || isSelected -> Color.White
                                                        else -> if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF0F172A)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Bottom Controls Capsule
                    if (isFullscreenControlsExpanded) {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFBF2)).copy(alpha = 0.96f),
                            border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.6f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Page Quick Button
                                IconButton(
                                    onClick = onPreviousPage,
                                    enabled = pageNum > 1,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("fullscreen_bottom_prev_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Previous Page",
                                        tint = if (pageNum > 1) (if (isDarkMode) IslamicGold else EmeraldPrimary) else Color.Gray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Play Audio Button
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isPlayingAudio) MaterialTheme.colorScheme.error.copy(alpha = 0.9f) else EmeraldPrimary,
                                    modifier = Modifier.clickable { onPlayPageAudio() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.VolumeUp,
                                            contentDescription = "Audio Recitation",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isPlayingAudio) (if (isArabic) "إيقاف" else "Stop") else (if (isArabic) "تلاوة" else "Play"),
                                            style = TextStyle(
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }

                                // Ayat Quick Selector Button
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (showAyatQuickBar) EmeraldPrimary else (if (isDarkMode) Color(0xFF334155) else Color(0xFFE5DDD0)),
                                    modifier = Modifier.clickable { showAyatQuickBar = !showAyatQuickBar }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatListNumbered,
                                            contentDescription = "Ayat",
                                            tint = if (showAyatQuickBar) Color.White else (if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF1B5E55)),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isArabic) "آيات" else "Ayat",
                                            style = TextStyle(
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (showAyatQuickBar) Color.White else (if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF1B5E55))
                                        )
                                    }
                                }

                                // Page Tafseer Button
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = IslamicGold.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.7f)),
                                    modifier = Modifier.clickable { onOpenPageTafsir() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = "Tafsir",
                                            tint = if (isDarkMode) IslamicGold else Color(0xFF92400E),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (isArabic) "تفسير" else "Tafsir",
                                            style = TextStyle(
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isDarkMode) IslamicGold else Color(0xFF92400E)
                                        )
                                    }
                                }

                                // Next Page Quick Button
                                IconButton(
                                    onClick = onNextPage,
                                    enabled = pageNum < 604,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("fullscreen_bottom_next_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Next Page",
                                        tint = if (pageNum < 604) (if (isDarkMode) IslamicGold else EmeraldPrimary) else Color.Gray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Minimize / Collapse Floating Bar
                                IconButton(
                                    onClick = { isFullscreenControlsExpanded = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Collapse Controls",
                                        tint = if (isDarkMode) Color.LightGray else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Collapsed Quick-Pill to Re-open Fullscreen Controls with Quick Prev/Next Navigation
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFBF2)).copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.6f)),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = onPreviousPage,
                                    enabled = pageNum > 1,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Previous Page",
                                        tint = if (pageNum > 1) IslamicGold else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable { isFullscreenControlsExpanded = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Show Tools",
                                        tint = IslamicGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "أدوات" else "Tools",
                                        style = TextStyle(
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isDarkMode) Color.White else Color(0xFF1B5E55)
                                    )
                                }

                                IconButton(
                                    onClick = onNextPage,
                                    enabled = pageNum < 604,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Next Page",
                                        tint = if (pageNum < 604) IslamicGold else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls Bar (Visible in normal reading mode, positioned below the Quran page)
        androidx.compose.animation.AnimatedVisibility(
            visible = !isImmersive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 6.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Expandable Ayat Quick Bar
                AnimatedVisibility(visible = showAyatQuickBar && pageVerses.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = if (isArabic) "آيات الصفحة (اضغط لعرض التفسير وسبب النزول والمعاني):" else "Page Ayat (Tap for Tafsir, Asbab Al-Nuzul & Meanings):",
                                style = TextStyle(
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF0D5C3A),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(pageVerses) { verse ->
                                    val isSelected = selectedAyahOnPage?.id == verse.id
                                    val isVersePlaying = isPlayingAudio &&
                                            currentPlayingVerse?.first == verse.surahId &&
                                            currentPlayingVerse?.second == verse.verseNumber

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            isVersePlaying -> EmeraldPrimary
                                            isSelected -> IslamicGold
                                            else -> if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected || isVersePlaying) Color.Transparent else if (isDarkMode) Color(0xFF475569) else Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier.clickable {
                                            selectedAyahOnPage = if (selectedAyahOnPage?.id == verse.id) null else verse
                                            onVerseClick(verse)
                                        }
                                    ) {
                                        Text(
                                            text = "الآية ${toArabicDigits(verse.verseNumber)}",
                                            style = TextStyle(
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = when {
                                                isVersePlaying || isSelected -> Color.White
                                                else -> if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF0F172A)
                                            },
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Control Capsule (Matching standard design)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFBF2)).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Page Button
                        IconButton(
                            onClick = onPreviousPage,
                            enabled = pageNum > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Previous Page",
                                tint = if (pageNum > 1) (if (isDarkMode) Color.White else Color(0xFF1B5E55)) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Full Width / Fullscreen Button ("ملء العرض")
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = IslamicGold.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.7f)),
                            modifier = Modifier.clickable { onToggleImmersive() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = null,
                                    tint = if (isDarkMode) IslamicGold else Color(0xFF1B5E55),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isArabic) "ملء العرض" else "Full Width",
                                    style = TextStyle(
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isDarkMode) IslamicGold else Color(0xFF1B5E55)
                                )
                            }
                        }

                        // Ayat List Toggle Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (showAyatQuickBar) EmeraldPrimary else (if (isDarkMode) Color(0xFF334155) else Color(0xFFE5DDD0)),
                            modifier = Modifier.clickable { showAyatQuickBar = !showAyatQuickBar }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListNumbered,
                                    contentDescription = null,
                                    tint = if (showAyatQuickBar) Color.White else (if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF1B5E55)),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isArabic) "آيات" else "Ayat",
                                    style = TextStyle(
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (showAyatQuickBar) Color.White else (if (isDarkMode) Color(0xFFFDE68A) else Color(0xFF1B5E55))
                                )
                            }
                        }

                        // Page Tafseer Button (Highlighted in soft gold)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = IslamicGold.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.6f)),
                            modifier = Modifier.clickable { onOpenPageTafsir() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = if (isDarkMode) IslamicGold else Color(0xFF92400E),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isArabic) "تفسير الصفحة" else "Page Tafsir",
                                    style = TextStyle(
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isDarkMode) IslamicGold else Color(0xFF92400E)
                                )
                            }
                        }

                        // Next Page Button
                        IconButton(
                            onClick = onNextPage,
                            enabled = pageNum < 604,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Next Page",
                                tint = if (pageNum < 604) (if (isDarkMode) Color.White else Color(0xFF1B5E55)) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableMushafPageView(
    pageNum: Int,
    pageVerses: List<Verse>,
    isDarkMode: Boolean,
    isArabic: Boolean,
    isPlayingAudio: Boolean,
    currentPlayingVerse: Pair<Int, Int>?,
    onVerseClick: (Verse) -> Unit,
    onWordMeaningClick: (String, Verse) -> Unit,
    onOpenPageTafsir: () -> Unit,
    onOpenJumpDialog: () -> Unit = {},
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {}
) {
    var isImmersive by remember { mutableStateOf(false) }
    var localScale by remember { mutableFloatStateOf(1.0f) }
    GoldenMushafPageView(
        pageNum = pageNum,
        pageVerses = pageVerses,
        isDarkMode = isDarkMode,
        isArabic = isArabic,
        isPlayingAudio = isPlayingAudio,
        currentPlayingVerse = currentPlayingVerse,
        isImmersive = isImmersive,
        onToggleImmersive = { isImmersive = !isImmersive },
        onVerseClick = onVerseClick,
        onWordMeaningClick = onWordMeaningClick,
        onOpenPageTafsir = onOpenPageTafsir,
        onOpenJumpDialog = onOpenJumpDialog,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        pageScale = localScale,
        onPageScaleChange = { localScale = it }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafPageScaleDialog(
    currentScale: Float,
    isArabic: Boolean,
    isDarkMode: Boolean,
    onScaleChange: (Float) -> Unit,
    onRestoreDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val percentage = (currentScale * 100).toInt()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFBF2),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkMode) IslamicGold.copy(alpha = 0.6f) else EmeraldPrimary.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
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
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = IslamicGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isArabic) "حجم صفحة المصحف والخط" else "Mushaf Page & Font Size",
                        style = TextStyle(
                            fontFamily = TajawalFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDarkMode) Color.White else Color(0xFF1B5E55)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isDarkMode) Color.LightGray else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current percentage badge with descriptive label
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFEDE5D8).copy(alpha = 0.7f),
                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$percentage%",
                        style = TextStyle(
                            fontFamily = TajawalFontFamily,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = IslamicGold
                    )
                    Text(
                        text = when {
                            currentScale <= 1.02f -> if (isArabic) "الحجم الطبيعي القياسي (الصفحة كاملة 100%)" else "Standard Full Page View (100%)"
                            currentScale <= 1.10f -> if (isArabic) "ملائم ومتناسق (خط أكبر مع ظهور الآيات كاملة)" else "Optimal Fit (Bigger Font, All Lines Visible)"
                            currentScale <= 1.25f -> if (isArabic) "خط كبير وواضح للقراءة المريحة" else "Large & Clear for Comfortable Reading"
                            else -> if (isArabic) "تكبير عالي لتدقيق تفاصيل الخط ورسم المصحف" else "High Magnification"
                        },
                        style = TextStyle(
                            fontFamily = TajawalFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isDarkMode) Color.White.copy(alpha = 0.85f) else Color(0xFF1B5E55)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Slider with Steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Minus Stepper
                IconButton(
                    onClick = {
                        val newScale = (currentScale - 0.05f).coerceIn(0.85f, 2.20f)
                        onScaleChange(newScale)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = "Zoom Out",
                        tint = if (isDarkMode) IslamicGold else Color(0xFF1B5E55),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Slider
                Slider(
                    value = currentScale,
                    onValueChange = { onScaleChange(it) },
                    valueRange = 0.85f..2.20f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = IslamicGold,
                        activeTrackColor = IslamicGold,
                        inactiveTrackColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2D6C3)
                    )
                )

                // Plus Stepper
                IconButton(
                    onClick = {
                        val newScale = (currentScale + 0.05f).coerceIn(0.85f, 2.20f)
                        onScaleChange(newScale)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "Zoom In",
                        tint = if (isDarkMode) IslamicGold else Color(0xFF1B5E55),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Preset Chips (100% normal, 106% fit, 115% large, 125% zoom)
            Text(
                text = if (isArabic) "خيارات التكبير السريعة:" else "Quick Scale Presets:",
                style = TextStyle(
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isDarkMode) Color.LightGray else Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    1.00f to (if (isArabic) "100% عادي" else "100% Normal"),
                    1.06f to (if (isArabic) "106% ملائم" else "106% Fit"),
                    1.15f to (if (isArabic) "115% كبير" else "115% Large"),
                    1.25f to (if (isArabic) "125% مكبّر" else "125% Zoom")
                )

                presets.forEach { (presetScale, label) ->
                    val isSelected = Math.abs(currentScale - presetScale) < 0.04f
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) IslamicGold else (if (isDarkMode) Color(0xFF334155) else Color(0xFFE5DDD0)),
                        border = if (isSelected) null else BorderStroke(1.dp, IslamicGold.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onScaleChange(presetScale) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) Color(0xFF0F172A) else (if (isDarkMode) Color.White else Color(0xFF1E293B)),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Restore Button
            Button(
                onClick = {
                    onRestoreDefault()
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentScale > 1.03f) IslamicGold else (if (isDarkMode) Color(0xFF334155) else Color(0xFFEDE5D8)),
                    contentColor = if (currentScale > 1.03f) Color(0xFF0F172A) else (if (isDarkMode) Color.White else Color(0xFF1E293B))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("restore_default_scale_btn")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = "Restore Default",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isArabic) "استعادة الحجم الافتراضي (100%)" else "Restore Default Size (100%)",
                        style = TextStyle(
                            fontFamily = TajawalFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

