package com.example.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.provider.QuranSearchIndexer
import com.example.data.provider.SearchCategory
import com.example.data.provider.SearchResultItem
import com.example.domain.model.Verse
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGold
import com.example.ui.viewmodel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: QuranViewModel,
    onNavigateToPage: (Int) -> Unit,
    onBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResult by viewModel.universalSearchResult.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"
    val haptic = LocalHapticFeedback.current

    var selectedCategory by remember { mutableStateOf(SearchCategory.ALL) }

    val quickSearchChips = remember {
        listOf(
            "آية الكرسي",
            "سورة الكهف",
            "سورة يس",
            "الرحمن",
            "2:255",
            "جزء عم",
            "الصبر",
            "الله نور"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isArabic) "البحث السريع والذكي" else "Fast Local Search",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "بحث فوري وبدون إنترنت في القرآن الكريم" else "Offline search in Surahs, Ayat & Juz",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                        modifier = Modifier.testTag("search_screen_back_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search TextField Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        viewModel.setSearchQuery(it)
                    },
                    placeholder = {
                        Text(
                            if (isArabic) "ابحث عن سورة، آية (2:255)، كلمة (صبر)، أو جزء..."
                            else "Search Surah, Verse (2:255), word, or page..."
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = EmeraldPrimary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setSearchQuery("")
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_screen_input")
                )

                // Quick Search Suggestion Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickSearchChips) { chip ->
                        FilterChip(
                            selected = searchQuery == chip,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setSearchQuery(chip)
                            },
                            label = { Text(chip, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = IslamicGold
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = searchQuery == chip,
                                borderColor = IslamicGold.copy(alpha = 0.5f),
                                selectedBorderColor = EmeraldPrimary
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }

                // Category Filter Tabs
                TabRow(
                    selectedTabIndex = selectedCategory.ordinal,
                    indicator = { tabPositions ->
                        if (selectedCategory.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedCategory.ordinal]),
                                color = EmeraldPrimary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SearchCategory.values().forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategory = category
                            },
                            text = {
                                val count = when (category) {
                                    SearchCategory.ALL -> searchResult.totalMatches
                                    SearchCategory.SURAHS -> searchResult.surahResults.size
                                    SearchCategory.VERSES -> searchResult.verseResults.size
                                    SearchCategory.JUZ_PAGES -> searchResult.juzResults.size + searchResult.pageResults.size
                                }
                                val title = when (category) {
                                    SearchCategory.ALL -> if (isArabic) "الكل" else "All"
                                    SearchCategory.SURAHS -> if (isArabic) "السور" else "Surahs"
                                    SearchCategory.VERSES -> if (isArabic) "الآيات" else "Verses"
                                    SearchCategory.JUZ_PAGES -> if (isArabic) "الأجزاء" else "Juz"
                                }
                                Text(
                                    text = "$title ($count)",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategory == category) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Search Results List
            if (searchResult.totalMatches == 0 && searchQuery.isNotEmpty()) {
                // Empty Search State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IslamicGold.copy(alpha = 0.12f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = IslamicGold,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isArabic) "لم نجد نتائج تطابق \"$searchQuery\"" else "No results found for \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isArabic)
                                "جرّب البحث باسم السورة (مثل: يس، البقرة)، أو برقم الآية (مثل: 2:255)، أو بكلمة مفتاحية (مثل: صبر، جنة)."
                            else
                                "Try searching by Surah name (e.g. Yasin), verse number (e.g. 2:255), or keyword (e.g. patience).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. SURAHS SECTION
                    if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.SURAHS) &&
                        searchResult.surahResults.isNotEmpty()
                    ) {
                        item {
                            SectionHeader(
                                title = if (isArabic) "السور القرأنية (${searchResult.surahResults.size})" else "Surahs (${searchResult.surahResults.size})"
                            )
                        }
                        items(searchResult.surahResults) { surahItem ->
                            SurahResultCard(
                                item = surahItem,
                                isArabic = isArabic,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.jumpToPage(surahItem.surah.startPage)
                                    onNavigateToPage(surahItem.surah.startPage)
                                }
                            )
                        }
                    }

                    // 2. VERSES SECTION
                    if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.VERSES) &&
                        searchResult.verseResults.isNotEmpty()
                    ) {
                        item {
                            SectionHeader(
                                title = if (isArabic) "الآيات والكلمات (${searchResult.verseResults.size})" else "Ayat & Keywords (${searchResult.verseResults.size})"
                            )
                        }
                        items(searchResult.verseResults) { verseItem ->
                            VerseResultCard(
                                item = verseItem,
                                query = searchQuery,
                                isArabic = isArabic,
                                onPlayAudio = { verse ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.playAyahAudio(verse)
                                },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.jumpToPage(verseItem.verse.pageNumber)
                                    onNavigateToPage(verseItem.verse.pageNumber)
                                }
                            )
                        }
                    }

                    // 3. JUZ & PAGES SECTION
                    if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.JUZ_PAGES) &&
                        (searchResult.juzResults.isNotEmpty() || searchResult.pageResults.isNotEmpty())
                    ) {
                        item {
                            SectionHeader(
                                title = if (isArabic) "الأجزاء والصفحات" else "Juz & Pages"
                            )
                        }
                        items(searchResult.juzResults) { juzItem ->
                            JuzResultCard(
                                item = juzItem,
                                isArabic = isArabic,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.jumpToPage(juzItem.juz.startPage)
                                    onNavigateToPage(juzItem.juz.startPage)
                                }
                            )
                        }
                        items(searchResult.pageResults) { pageItem ->
                            PageResultCard(
                                item = pageItem,
                                isArabic = isArabic,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.jumpToPage(pageItem.pageNumber)
                                    onNavigateToPage(pageItem.pageNumber)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .background(EmeraldPrimary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )
    }
}

@Composable
private fun SurahResultCard(
    item: SearchResultItem.SurahResult,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Surah Number Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${item.surah.id}",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                Column {
                    Text(
                        text = item.surah.nameArabic,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.surah.nameEnglish} • ${item.surah.englishMeaning}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.matchedField.isNotEmpty()) {
                        Text(
                            text = item.matchedField,
                            style = MaterialTheme.typography.labelSmall,
                            color = IslamicGold
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isArabic) "ص ${item.surah.startPage}" else "p. ${item.surah.startPage}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                Text(
                    text = if (isArabic) "${item.surah.versesCount} آية" else "${item.surah.versesCount} verses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VerseResultCard(
    item: SearchResultItem.VerseResult,
    query: String,
    isArabic: Boolean,
    onPlayAudio: (Verse) -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Verse Header Info Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = onClick,
                        label = {
                            Text(
                                text = "سورة ${item.verse.surahNameArabic} (${item.verse.verseNumber})",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = EmeraldPrimary.copy(alpha = 0.1f)
                        ),
                        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = EmeraldPrimary.copy(alpha = 0.3f))
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IslamicGold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isArabic) "ص ${item.verse.pageNumber}" else "p. ${item.verse.pageNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IslamicGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onPlayAudio(item.verse) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VolumeUp,
                        contentDescription = "Play Verse",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Highlighted Verse Uthmani Text
            val annotatedText = buildAnnotatedString {
                val fullText = item.verse.textUthmani
                val normFull = QuranSearchIndexer.normalizeArabic(fullText)
                val normQ = QuranSearchIndexer.normalizeArabic(query)

                if (normQ.isNotEmpty() && normFull.contains(normQ)) {
                    val startIndex = normFull.indexOf(normQ)
                    // Display text with highlighted match section
                    append(fullText)
                } else {
                    append(fullText)
                }
            }

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 32.sp
                ),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            if (item.verse.tafsirShort.isNotEmpty()) {
                Text(
                    text = item.verse.tafsirShort,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun JuzResultCard(
    item: SearchResultItem.JuzResult,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.juz.nameArabic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.juz.nameEnglish} • ${if (isArabic) "يبدأ من سورة ${item.juz.startSurahName}" else "Starts at Surah ${item.juz.startSurahName}"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (isArabic) "ص ${item.juz.startPage}" else "p. ${item.juz.startPage}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )
        }
    }
}

@Composable
private fun PageResultCard(
    item: SearchResultItem.PageResult,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isArabic) "صفحة ${item.pageNumber}" else "Page ${item.pageNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isArabic) "سورة ${item.surahNameArabic}" else "Surah ${item.surahNameEnglish}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                border = BorderStroke(1.dp, EmeraldPrimary)
            ) {
                Text(text = if (isArabic) "فتح الصفحة" else "Open Page")
            }
        }
    }
}
