package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.AsbabAlNuzulProvider
import com.example.data.provider.AsbabNuzulItem
import com.example.data.provider.QuranDataProvider
import com.example.data.provider.TafsirProvider
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.QuranScript
import com.example.ui.theme.ScheherazadeFontFamily
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.viewmodel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    val verseForDetail by viewModel.selectedVerseForDetail.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Ayah Tafsir, 1: Asbab Al-Nuzul, 2: Vocabulary Dictionary

    // Tafsir Explorer State
    var selectedSurahNum by remember { mutableIntStateOf(verseForDetail?.surahId ?: 1) }
    var selectedAyahNum by remember { mutableIntStateOf(verseForDetail?.verseNumber ?: 1) }
    var selectedBookId by remember { mutableStateOf("muyassar") }

    LaunchedEffect(verseForDetail) {
        verseForDetail?.let {
            selectedSurahNum = it.surahId
            selectedAyahNum = it.verseNumber
        }
    }

    val surahs = QuranDataProvider.SURAHS
    val currentSurah = remember(selectedSurahNum) {
        surahs.firstOrNull { it.id == selectedSurahNum } ?: surahs.first()
    }

    val tafsirDetail = remember(selectedSurahNum, selectedAyahNum, selectedBookId) {
        TafsirProvider.getTafsirForAyah(selectedSurahNum, selectedAyahNum, selectedBookId)
    }

    val currentAsbab = remember(selectedSurahNum, selectedAyahNum) {
        AsbabAlNuzulProvider.getAsbabNuzul(selectedSurahNum, selectedAyahNum)
    }

    // Dropdown menus state
    var surahExpanded by remember { mutableStateOf(false) }
    var ayahExpanded by remember { mutableStateOf(false) }

    // Asbab search query
    var asbabSearchQuery by remember { mutableStateOf("") }
    val allKnownAsbab = remember { AsbabAlNuzulProvider.getAllKnownAsbab() }
    val filteredAsbabList = remember(asbabSearchQuery) {
        if (asbabSearchQuery.isBlank()) {
            allKnownAsbab
        } else {
            allKnownAsbab.filter {
                it.surahNameAr.contains(asbabSearchQuery, ignoreCase = true) ||
                it.reasonArabic.contains(asbabSearchQuery, ignoreCase = true) ||
                it.verseTextAr.contains(asbabSearchQuery, ignoreCase = true) ||
                it.surahNumber.toString() == asbabSearchQuery.trim()
            }
        }
    }

    // Dictionary Search Query
    var dictionaryQuery by remember { mutableStateOf("") }
    val wordMeanings = QuranDataProvider.WORD_MEANINGS
    val filteredWords = remember(dictionaryQuery) {
        if (dictionaryQuery.isBlank()) {
            wordMeanings
        } else {
            wordMeanings.filter {
                it.arabicWord.contains(dictionaryQuery, ignoreCase = true) ||
                it.englishMeaning.contains(dictionaryQuery, ignoreCase = true) ||
                it.transliteration.contains(dictionaryQuery, ignoreCase = true) ||
                it.rootWord.contains(dictionaryQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "التفسير وعلوم القرآن" else "Tafsir & Quranic Sciences",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tafsir_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 3-Tab Selector Row (Tafsir, Asbab Al-Nuzul, Vocabulary)
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Text(
                            text = if (isArabic) "تفسير الآيات" else "Ayah Tafsir",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Text(
                            text = if (isArabic) "أسباب النزول" else "Asbab Al-Nuzul",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Text(
                            text = if (isArabic) "معجم الكلمات" else "Vocabulary",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            when (activeTab) {
                0 -> {
                    // AYAH TAFSIR EXPLORER
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Surah & Verse Dropdown Selectors
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = if (isArabic) "اختر السورة والآية للتفسير" else "Select Surah & Ayah",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Surah Dropdown Box
                                        Box(modifier = Modifier.weight(1.8f)) {
                                            OutlinedButton(
                                                onClick = { surahExpanded = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = "${currentSurah.id}. ${currentSurah.nameArabic}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Verse Dropdown Box
                                        Box(modifier = Modifier.weight(1.2f)) {
                                            OutlinedButton(
                                                onClick = { ayahExpanded = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = if (isArabic) "الآية $selectedAyahNum" else "Ayah $selectedAyahNum",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Tafsir Book Selector Chips
                                    Text(
                                        text = if (isArabic) "كتاب التفسير:" else "Tafsir Book:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(TafsirProvider.TAFSIR_BOOKS) { book ->
                                            val isSelected = book.id == selectedBookId
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedBookId = book.id },
                                                label = {
                                                    Text(
                                                        text = if (isArabic) book.nameArabic else book.nameEnglish,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = EmeraldPrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Main Tafsir Card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Header with Surah & Ayah Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = EmeraldPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (isArabic) "سورة ${tafsirDetail.surahNameAr} - الآية ${tafsirDetail.verseNumber}" else "Surah ${tafsirDetail.surahNameEn} - Verse ${tafsirDetail.verseNumber}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily),
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = IslamicGold.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (isArabic) tafsirDetail.tafsirBookNameAr else (TafsirProvider.TAFSIR_BOOKS.firstOrNull { it.id == tafsirDetail.tafsirBookId }?.nameEnglish ?: tafsirDetail.tafsirBookNameAr),
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                                                color = IslamicGold,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Arabic Verse Box
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val quranScriptFont = QuranScript.fromId(userSettings.quranScriptType).getFontFamily()
                                            Text(
                                                text = tafsirDetail.verseTextAr,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontSize = 24.sp,
                                                    fontFamily = quranScriptFont,
                                                    lineHeight = 44.sp
                                                ),
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            if (tafsirDetail.verseTranslationEn.isNotEmpty()) {
                                                Text(
                                                    text = tafsirDetail.verseTranslationEn,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                    // Commentary Body
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (isArabic) "التفسير والشرح المفصل:" else "Detailed Commentary:",
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )

                                        Text(
                                            text = tafsirDetail.commentaryText,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = TajawalFontFamily,
                                                lineHeight = 28.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Key Lessons Section
                                    if (tafsirDetail.keyLessons.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = IslamicGold.copy(alpha = 0.1f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = if (isArabic) "من فوائد وهدايات الآية المباركة:" else "Key Spiritual Takeaways:",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily),
                                                    fontWeight = FontWeight.Bold,
                                                    color = IslamicGold
                                                )

                                                tafsirDetail.keyLessons.forEach { lesson ->
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Text("•", color = IslamicGold, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = lesson,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Asbab Al-Nuzul Card preview for this Ayah
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (AsbabAlNuzulProvider.hasSpecificReason(selectedSurahNum, selectedAyahNum)) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(18.dp))
                                                    Text(
                                                        text = if (isArabic) "سبب نزول الآية الكريمة:" else "Reason for Revelation (Asbab Al-Nuzul):",
                                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily),
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF92400E)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = IslamicGold.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = currentAsbab.contextType,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily, fontSize = 10.sp),
                                                        color = Color(0xFF92400E),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = if (isArabic) currentAsbab.reasonArabic else currentAsbab.reasonEnglish.ifBlank { currentAsbab.reasonArabic },
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = TajawalFontFamily,
                                                    lineHeight = 24.sp
                                                ),
                                                color = Color(0xFF451A03)
                                            )

                                            Text(
                                                text = "المصدر: ${currentAsbab.sourceBook}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily, fontSize = 10.sp),
                                                color = Color(0xFF78350F)
                                            )
                                        }
                                    }

                                    // Action Buttons (Copy, Share)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val textToCopy = "${tafsirDetail.verseTextAr}\n\n[تفسير سورة ${tafsirDetail.surahNameAr} - آية ${tafsirDetail.verseNumber}]\n${tafsirDetail.commentaryText}\n\n[سبب النزول]\n${currentAsbab.reasonArabic}"
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Tafsir", textToCopy))
                                                Toast.makeText(context, if (isArabic) "تم نسخ التفسير وسبب النزول" else "Tafsir copied", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isArabic) "نسخ التفسير وسبب النزول" else "Copy Details")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // ASBAB AL-NUZUL (REASONS FOR REVELATION) DEDICATED TAB
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                                        Text(
                                            text = if (isArabic) "أسباب نزول القرآن الكريم" else "Reasons of Quranic Revelation",
                                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = if (isArabic) "معرفة الحوادث والوقائع التي نزلت فيها الآيات، مستندة إلى كتب الواحدي والسيوطي وابن كثير." else "Explore the authentic historical events and causes behind the revelation of Quranic verses.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )

                                    OutlinedTextField(
                                        value = asbabSearchQuery,
                                        onValueChange = { asbabSearchQuery = it },
                                        placeholder = { Text(if (isArabic) "ابحث عن سورة، آية، أو حادثة نزول..." else "Search Surah, Ayah, or event...", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily)) },
                                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        items(filteredAsbabList) { item ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
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
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = EmeraldPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (isArabic) "سورة ${item.surahNameAr} - الآية ${item.verseNumber}" else "Surah ${item.surahNameEn} - Verse ${item.verseNumber}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily),
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = IslamicGold.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = item.contextType,
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                                                color = Color(0xFF92400E),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // Verse Text Box
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.verseTextAr,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = TajawalFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }

                                    // Reason text
                                    Text(
                                        text = if (isArabic) item.reasonArabic else item.reasonEnglish.ifBlank { item.reasonArabic },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = TajawalFontFamily,
                                            lineHeight = 26.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "المصدر: ${item.sourceBook}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily, fontSize = 10.sp),
                                            color = IslamicGold
                                        )

                                        IconButton(
                                            onClick = {
                                                val textToCopy = "سورة ${item.surahNameAr} (الآية ${item.verseNumber}):\n${item.verseTextAr}\n\n[سبب النزول]:\n${item.reasonArabic}\n\n(المصدر: ${item.sourceBook})"
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Asbab Nuzul", textToCopy))
                                                Toast.makeText(context, if (isArabic) "تم نسخ سبب النزول" else "Reason of revelation copied", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // DICTIONARY & VOCABULARY TAB
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (isArabic) "معجم مفردات وغريب القرآن الكريم" else "Quranic Word-by-Word Dictionary",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (isArabic) "البحث في مفردات الكلمات وتراكيبها والجذور اللغوية في القرآن." else "Search vocabulary, roots, and linguistic meanings.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )

                                    OutlinedTextField(
                                        value = dictionaryQuery,
                                        onValueChange = { dictionaryQuery = it },
                                        placeholder = { Text(if (isArabic) "ابحث عن كلمة، جذر، أو معنى..." else "Search word, root, or meaning...", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily)) },
                                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("tafsir_search_input")
                                    )
                                }
                            }
                        }

                        items(filteredWords) { word ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = word.arabicWord,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 22.sp,
                                                fontFamily = TajawalFontFamily
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${word.transliteration} • الجذر: [ ${word.rootWord} ]",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                            color = IslamicGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = word.englishMeaning,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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

    if (surahExpanded) {
        AlertDialog(
            onDismissRequest = { surahExpanded = false },
            title = {
                Text(
                    text = if (isArabic) "اختر السورة" else "Select Surah",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(modifier = Modifier.height(350.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(surahs) { surah ->
                            val isSelected = surah.id == selectedSurahNum
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSurahNum = surah.id
                                        selectedAyahNum = 1
                                        surahExpanded = false
                                    }
                            ) {
                                Text(
                                    text = "${surah.id}. ${surah.nameArabic} (${surah.nameEnglish})",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { surahExpanded = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily))
                }
            }
        )
    }

    if (ayahExpanded) {
        AlertDialog(
            onDismissRequest = { ayahExpanded = false },
            title = {
                Text(
                    text = if (isArabic) "اختر رقم الآية" else "Select Ayah Number",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(modifier = Modifier.height(350.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items((1..currentSurah.versesCount).toList()) { ayahNum ->
                            val isSelected = ayahNum == selectedAyahNum
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAyahNum = ayahNum
                                        ayahExpanded = false
                                    }
                            ) {
                                Text(
                                    text = if (isArabic) "الآية $ayahNum" else "Ayah $ayahNum",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { ayahExpanded = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily))
                }
            }
        )
    }
}
