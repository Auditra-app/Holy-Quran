package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.QuranDataProvider
import com.example.data.provider.TafsirProvider
import com.example.domain.model.Reciter
import com.example.domain.model.Surah
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuranViewModel
import com.example.util.HijriCalendarHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyVerseScreen(
    viewModel: QuranViewModel,
    onNavigateToPage: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dailyVerse = viewModel.dailyVerse
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    // Recitation & Audio State
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
    val currentPlayingVerse by viewModel.currentPlayingVerse.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    var showReciterDialog by remember { mutableStateOf(false) }

    // Surah Details
    val surahs = remember { QuranDataProvider.SURAHS }
    val currentSurah: Surah = remember(dailyVerse) {
        surahs.firstOrNull {
            it.nameArabic == dailyVerse.surahNameArabic ||
            it.nameEnglish.equals(dailyVerse.surahNameEnglish, ignoreCase = true)
        } ?: QuranDataProvider.getSurahByPage(dailyVerse.pageNumber)
    }

    val juz = remember(dailyVerse.pageNumber) {
        QuranDataProvider.getJuzByPage(dailyVerse.pageNumber)
    }

    // Tafsir Book Selection
    var selectedBookId by remember { mutableStateOf("muyassar") }
    val tafsirDetail = remember(currentSurah.id, dailyVerse.verseNumber, selectedBookId) {
        TafsirProvider.getTafsirForAyah(currentSurah.id, dailyVerse.verseNumber, selectedBookId)
    }

    // English translation fallback
    val englishTranslation = remember(dailyVerse, tafsirDetail) {
        dailyVerse.translationEnglish.ifBlank {
            tafsirDetail.verseTranslationEn.ifBlank {
                "In the name of Allah, the Entirely Merciful, the Especially Merciful."
            }
        }
    }

    val isThisVersePlaying = isPlayingAudio &&
            currentPlayingVerse?.first == currentSurah.id &&
            currentPlayingVerse?.second == dailyVerse.verseNumber

    // Formatted Dates
    val hijriDateStr = remember(isArabic) {
        HijriCalendarHelper.getFormattedHijriDate(isArabic)
    }
    val gregorianDateStr = remember(isArabic) {
        try {
            val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
            val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", locale)
            sdf.format(Date())
        } catch (e: Exception) {
            ""
        }
    }

    val quranScriptFont = remember(userSettings.quranScriptType) {
        QuranScript.fromId(userSettings.quranScriptType).getFontFamily()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isArabic) "آية اليوم المباركة" else "Daily Verse",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = hijriDateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("daily_verse_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "﴿ ${dailyVerse.textArabic} ﴾\n[سورة ${dailyVerse.surahNameArabic} - آية ${dailyVerse.verseNumber}]\n\n" +
                                    if (englishTranslation.isNotBlank()) "\"$englishTranslation\"\n\n" else "" +
                                    "تفسير ميسر: ${tafsirDetail.commentaryText.take(180)}...\n\nتطبيق القرآن الكريم والأذكار"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, if (isArabic) "مشاركة آية اليوم" else "Share Daily Verse"))
                        },
                        modifier = Modifier.testTag("share_daily_verse_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = IslamicGold
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.addCurrentPageBookmark(note = "آية اليوم: ${dailyVerse.surahNameArabic} (${dailyVerse.verseNumber})")
                            Toast.makeText(
                                context,
                                if (isArabic) "تم حفظ الآية في العلامات المرجعية 🔖" else "Saved to Bookmarks 🔖",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag("bookmark_daily_verse_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = IslamicGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Daily Spiritual Header Banner
            item {
                SpiritualGreetingCard(
                    hijriDate = hijriDateStr,
                    gregorianDate = gregorianDateStr,
                    isArabic = isArabic
                )
            }

            // 2. The Main Hero Quranic Verse Card
            item {
                HeroVerseCard(
                    dailyVerse = dailyVerse,
                    currentSurah = currentSurah,
                    englishTranslation = englishTranslation,
                    quranScriptFont = quranScriptFont,
                    isPlaying = isThisVersePlaying,
                    selectedReciter = selectedReciter,
                    isArabic = isArabic,
                    onTogglePlay = {
                        if (isThisVersePlaying) {
                            viewModel.stopAudioPlayback()
                        } else {
                            viewModel.playAyahAudio(
                                com.example.domain.model.Verse(
                                    id = currentSurah.id * 1000 + dailyVerse.verseNumber,
                                    surahId = currentSurah.id,
                                    surahNameArabic = currentSurah.nameArabic,
                                    surahNameEnglish = currentSurah.nameEnglish,
                                    verseNumber = dailyVerse.verseNumber,
                                    pageNumber = dailyVerse.pageNumber,
                                    juzNumber = juz.number,
                                    textUthmani = dailyVerse.textArabic,
                                    translationEnglish = englishTranslation,
                                    tafsirShort = tafsirDetail.commentaryText
                                )
                            )
                        }
                    },
                    onChangeReciter = { showReciterDialog = true },
                    onNavigateToMushaf = {
                        viewModel.jumpToPage(dailyVerse.pageNumber)
                        onNavigateToPage(dailyVerse.pageNumber)
                    },
                    onBookmark = {
                        viewModel.addCurrentPageBookmark(note = "آية اليوم: ${dailyVerse.surahNameArabic} (${dailyVerse.verseNumber})")
                        Toast.makeText(
                            context,
                            if (isArabic) "تم حفظ الآية في العلامات المرجعية 🔖" else "Saved to Bookmarks 🔖",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onCopyText = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(
                            "Quran Verse",
                            "﴿ ${dailyVerse.textArabic} ﴾ [سورة ${dailyVerse.surahNameArabic} - آية ${dailyVerse.verseNumber}]"
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            if (isArabic) "تم نسخ نص الآية المباركة بنجاح 📋" else "Verse copied to clipboard 📋",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            // 3. Surah Context & Sacred Metrics
            item {
                SurahContextMetricsCard(
                    surah = currentSurah,
                    dailyVerse = dailyVerse,
                    juzNumber = juz.number,
                    isArabic = isArabic
                )
            }

            // 4. Multi-Book Tafsir Explorer
            item {
                TafsirSectionCard(
                    tafsirDetail = tafsirDetail,
                    selectedBookId = selectedBookId,
                    onSelectBook = { selectedBookId = it },
                    isArabic = isArabic
                )
            }

            // 5. Spiritual Reflection & Practical Deed of the Day
            item {
                SpiritualReflectionsCard(
                    tafsirDetail = tafsirDetail,
                    surahName = currentSurah.nameArabic,
                    isArabic = isArabic
                )
            }

            // 6. Action Button to Full Surah in Mushaf
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            viewModel.jumpToPage(dailyVerse.pageNumber)
                            onNavigateToPage(dailyVerse.pageNumber)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = IslamicGold,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isArabic) "قراءة سورة ${currentSurah.nameArabic} كاملة" else "Read Surah ${currentSurah.nameEnglish}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isArabic) "صفحة ${toArabicDigits(dailyVerse.pageNumber)} في المصحف الشريف" else "Page ${dailyVerse.pageNumber} in Mushaf",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = IslamicGold
                        )
                    }
                }
            }
        }
    }

    // Reciter Selection Dialog
    if (showReciterDialog) {
        AlertDialog(
            onDismissRequest = { showReciterDialog = false },
            title = {
                Text(
                    text = if (isArabic) "اختر القارئ الصوتي" else "Select Reciter",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    viewModel.reciters.forEach { reciter ->
                        val isSelected = reciter.id == selectedReciter.id
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) IslamicGold else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setReciter(reciter)
                                    showReciterDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isArabic) reciter.nameArabic else reciter.nameEnglish,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReciterDialog = false }) {
                    Text(if (isArabic) "إغلاق" else "Close")
                }
            }
        )
    }
}

// 1. Spiritual Date & Greeting Card
@Composable
private fun SpiritualGreetingCard(
    hijriDate: String,
    gregorianDate: String,
    isArabic: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = IslamicGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = if (isArabic) "نفحات قرآنية متجددة" else "Daily Quranic Inspiration",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (gregorianDate.isNotBlank()) {
                        Text(
                            text = gregorianDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldPrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = if (isArabic) "آية وتدبر" else "Verse & Reflection",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// 2. Hero Quranic Verse Card
@Composable
private fun HeroVerseCard(
    dailyVerse: com.example.domain.model.DailyVerse,
    currentSurah: Surah,
    englishTranslation: String,
    quranScriptFont: androidx.compose.ui.text.font.FontFamily,
    isPlaying: Boolean,
    selectedReciter: Reciter,
    isArabic: Boolean,
    onTogglePlay: () -> Unit,
    onChangeReciter: () -> Unit,
    onNavigateToMushaf: () -> Unit,
    onBookmark: () -> Unit,
    onCopyText: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F5132),
                            Color(0xFF0B3C26),
                            Color(0xFF062315)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top Verse Header Ornament
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "سورة ${dailyVerse.surahNameArabic}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = IslamicGold.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (isArabic) "الآية ${toArabicDigits(dailyVerse.verseNumber)}" else "Ayah ${dailyVerse.verseNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Central Holy Verse Calligraphy
                Text(
                    text = "﴿ ${dailyVerse.textArabic} ﴾",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 25.sp,
                        lineHeight = 46.sp,
                        fontFamily = quranScriptFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )

                // English Translation Subtitle
                if (englishTranslation.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"$englishTranslation\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                // Interactive Recitation Player Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledIconButton(
                                onClick = onTogglePlay,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = GoldAccent,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("play_daily_verse_audio_btn")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause Recitation" else "Play Recitation",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isPlaying) {
                                        if (isArabic) "جارٍ الاستماع للتلاوة 🔊" else "Playing Recitation 🔊"
                                    } else {
                                        if (isArabic) "استماع للآية المباركة" else "Listen to Recitation"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isArabic) selectedReciter.nameArabic else selectedReciter.nameEnglish,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldAccent
                                )
                            }
                        }

                        TextButton(
                            onClick = onChangeReciter,
                            colors = ButtonDefaults.textButtonColors(contentColor = GoldAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "تغيير القارئ" else "Change",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onCopyText,
                        modifier = Modifier.testTag("copy_daily_verse_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = GoldAccent
                        )
                    }

                    IconButton(
                        onClick = onBookmark,
                        modifier = Modifier.testTag("bookmark_daily_verse_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmark",
                            tint = GoldAccent
                        )
                    }

                    Button(
                        onClick = onNavigateToMushaf,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("read_daily_verse_page_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "فتح في المصحف" else "Open in Mushaf",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 3. Surah Context & Sacred Metrics Card
@Composable
private fun SurahContextMetricsCard(
    surah: Surah,
    dailyVerse: com.example.domain.model.DailyVerse,
    juzNumber: Int,
    isArabic: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isArabic) "بيانات ومعلومات السورة" else "Surah & Page Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "سورة ${surah.nameArabic} (${surah.nameEnglish})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = IslamicGold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricItemTile(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "النزول" else "Type",
                    value = if (surah.revelationType.equals("Meccan", ignoreCase = true)) {
                        if (isArabic) "مكية 🕋" else "Meccan 🕋"
                    } else {
                        if (isArabic) "مدنية 🕌" else "Medinan 🕌"
                    }
                )

                MetricItemTile(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "ترتيب السورة" else "Surah No.",
                    value = if (isArabic) "رقم ${toArabicDigits(surah.id)}" else "#${surah.id}"
                )

                MetricItemTile(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "صفحة المصحف" else "Page",
                    value = if (isArabic) "ص ${toArabicDigits(dailyVerse.pageNumber)}" else "P. ${dailyVerse.pageNumber}"
                )

                MetricItemTile(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "الجزء" else "Juz",
                    value = if (isArabic) "جزء ${toArabicDigits(juzNumber)}" else "Juz $juzNumber"
                )
            }
        }
    }
}

@Composable
private fun MetricItemTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 4. Multi-Book Tafsir Explorer Card
@Composable
private fun TafsirSectionCard(
    tafsirDetail: com.example.data.provider.AyahTafsirDetail,
    selectedBookId: String,
    onSelectBook: (String) -> Unit,
    isArabic: Boolean
) {
    val tafsirBooks = remember { TafsirProvider.TAFSIR_BOOKS }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isArabic) "التفسير والبيان القرآني" else "Quranic Commentary (Tafsir)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IslamicGold.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = tafsirDetail.tafsirBookNameAr,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Tafsir Book Selection Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tafsirBooks) { book ->
                    val isSelected = book.id == selectedBookId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectBook(book.id) },
                        label = {
                            Text(
                                text = if (isArabic) book.nameArabic.replace("تفسير ", "").replace(" (تيسير الكريم الرحمن)", "") else book.nameEnglish,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            // Tafsir Commentary Text Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tafsirDetail.commentaryText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 26.sp,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// 5. Spiritual Reflection & Practical Deed Card
@Composable
private fun SpiritualReflectionsCard(
    tafsirDetail: com.example.data.provider.AyahTafsirDetail,
    surahName: String,
    isArabic: Boolean
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = IslamicGold,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isArabic) "إشراقات وتدبرات الآية الكريمة" else "Spiritual Insights & Reflections",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            tafsirDetail.keyLessons.forEachIndexed { index, lesson ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = IslamicGold.copy(alpha = 0.18f),
                        modifier = Modifier
                            .size(26.dp)
                            .padding(top = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isArabic) toArabicDigits(index + 1) else "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = IslamicGold
                            )
                        }
                    }

                    Text(
                        text = lesson,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Daily Suggested Good Deed & Supplication
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = EmeraldPrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolunteerActivism,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = if (isArabic) "عمل اليوم المستحب 🌱" else "Today's Good Deed 🌱",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = if (isArabic) {
                                "تدبر هذه الآية في صلواتك، واذكر بها أحب الناس إليك لنيل أجر التذكير بالله والعمل الصالح."
                            } else {
                                "Reflect upon this verse in your prayers and share its wisdom with your loved ones."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
