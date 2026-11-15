package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.AzkarDataProvider
import com.example.domain.model.AzkarCategory
import com.example.domain.model.AzkarItem
import com.example.notification.PeriodicDuaaManager
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.ScheherazadeFontFamily
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.viewmodel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(
    viewModel: QuranViewModel,
    initialCategory: String = "morning",
    initialQuickRead: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val userSettings by viewModel.userSettings.collectAsState()
    val favoriteAzkarIds by viewModel.favoriteAzkarIds.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    var selectedCategoryId by remember { mutableStateOf(initialCategory) }
    var searchQuery by remember { mutableStateOf("") }
    var showQuickReadSheet by remember { mutableStateOf(initialQuickRead) }
    var showPeriodicDuaaDialog by remember { mutableStateOf(false) }
    var selectedVirtueZikr by remember { mutableStateOf<AzkarItem?>(null) }

    LaunchedEffect(initialCategory) {
        if (initialCategory.isNotBlank()) {
            selectedCategoryId = initialCategory
        }
    }

    LaunchedEffect(initialQuickRead) {
        if (initialQuickRead) {
            showQuickReadSheet = true
        }
    }

    // Map to track current counts for each zikr item ID
    val countsMap = remember { mutableStateMapOf<Int, Int>() }

    val categories = remember { AzkarDataProvider.CATEGORIES }
    val allAzkar = remember { AzkarDataProvider.AZKAR_COLLECTION }

    val filteredAzkar = remember(selectedCategoryId, searchQuery, allAzkar, favoriteAzkarIds) {
        allAzkar.filter { zikr ->
            val matchesCategory = when (selectedCategoryId) {
                "all" -> true
                "favorites" -> favoriteAzkarIds.contains(zikr.id)
                else -> zikr.categoryId == selectedCategoryId
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                zikr.text.contains(searchQuery, ignoreCase = true) ||
                zikr.virtue.contains(searchQuery, ignoreCase = true) ||
                zikr.reference.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isArabic) "الأذكار والأدعية 🤲" else "Azkar & Supplications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("azkar_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPeriodicDuaaDialog = true },
                        modifier = Modifier.testTag("periodic_duaa_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = if (isArabic) "الأدعية الدورية" else "Periodic Duaa",
                            tint = IslamicGold
                        )
                    }
                    IconButton(
                        onClick = { showQuickReadSheet = true },
                        modifier = Modifier.testTag("quick_read_azkar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Quick Read",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Category Filter Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == "all",
                        onClick = { selectedCategoryId = "all" },
                        label = { Text(if (isArabic) "الكل ✨" else "All ✨", fontFamily = TajawalFontFamily) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategoryId == "favorites",
                        onClick = { selectedCategoryId = "favorites" },
                        label = { Text(if (isArabic) "المفضلة ❤️" else "Favorites ❤️", fontFamily = TajawalFontFamily) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("azkar_favorites_chip")
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(cat.iconEmoji, fontSize = 14.sp)
                                Text(if (isArabic) cat.nameArabic else cat.nameEnglish, fontFamily = TajawalFontFamily)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Search Bar & Quick Read Launch Banner
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isArabic) "البحث في الأذكار والأدعية..." else "Search Azkar...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null) } }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("azkar_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Read Card Banner
                Surface(
                    onClick = { showQuickReadSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldPrimary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = if (isArabic) "وضع القراءة السريعة للأذكار ⚡" else "Quick Read Mode ⚡",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = if (isArabic) "تصفح أذكار الصباح والمساء بأسلوب متتابع مريح" else "Swipe & read morning & evening adhkar seamlessly",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = TajawalFontFamily
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = EmeraldPrimary)
                    }
                }
            }

            // Recited Audio Banner for Morning & Evening Azkar (Sheikh Mishary Al-Afasy)
            if (selectedCategoryId == "morning" || selectedCategoryId == "evening") {
                val isMorning = selectedCategoryId == "morning"
                AzkarAudioPlayerBanner(
                    isMorning = isMorning,
                    viewModel = viewModel,
                    isArabic = isArabic,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Azkar Items List
            if (filteredAzkar.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedCategoryId == "favorites") Icons.Default.FavoriteBorder else Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (selectedCategoryId == "favorites") {
                                if (isArabic) "لم تقم بإضافة أي أذكار للمفضلة بعد 🤍" else "No favorite Azkar added yet"
                            } else {
                                if (isArabic) "لا توجد أذكار مطابقة للبحث" else "No Azkar found"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = TajawalFontFamily
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAzkar, key = { it.id }) { zikr ->
                        val currentCount = countsMap[zikr.id] ?: 0
                        val isCompleted = currentCount >= zikr.count
                        val isFav = favoriteAzkarIds.contains(zikr.id)

                        AzkarCardItem(
                            zikr = zikr,
                            currentCount = currentCount,
                            isCompleted = isCompleted,
                            isArabic = isArabic,
                            isFavorite = isFav,
                            onToggleFavorite = { viewModel.toggleFavoriteAzkar(zikr.id, !isFav) },
                            onShowVirtue = { selectedVirtueZikr = zikr },
                            onCountIncrement = {
                                if (currentCount < zikr.count) {
                                    val nextCount = currentCount + 1
                                    countsMap[zikr.id] = nextCount
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (nextCount == zikr.count) {
                                        Toast.makeText(context, if (isArabic) "تمت قراءة الذكر بنجاح! جزاك الله خيراً 🌿" else "Completed! May Allah reward you.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onReset = { countsMap[zikr.id] = 0 },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Azkar", "${zikr.text}\n\n[المصدر: ${zikr.reference}]")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, if (isArabic) "تم نسخ الذكر بنجاح" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "ذكر من تطبيق القرآن الكريم والأذكار")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "✨ ${zikr.text}\n\n" +
                                                (if (zikr.virtue.isNotEmpty()) "🍃 فضل الذكر: ${zikr.virtue}\n" else "") +
                                                "📖 المصدر: ${zikr.reference}\n\n" +
                                                "تم المشاركة من تطبيق القرآن الكريم حصن المسلم 🕌"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, if (isArabic) "مشاركة الذكر" else "Share Zikr"))
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Quick Reader Sheet
    if (showQuickReadSheet) {
        val sheetAzkar = remember(selectedCategoryId, allAzkar) {
            val cat = if (selectedCategoryId == "all" || selectedCategoryId == "daily") "morning" else selectedCategoryId
            allAzkar.filter { it.categoryId == cat }
        }

        ModalBottomSheet(
            onDismissRequest = { showQuickReadSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            val isMorningQuick = selectedCategoryId == "morning" || (!selectedCategoryId.contains("evening"))
            QuickReadSheetContent(
                azkarList = sheetAzkar,
                isArabic = isArabic,
                countsMap = countsMap,
                viewModel = viewModel,
                isMorning = isMorningQuick,
                onClose = { showQuickReadSheet = false },
                onShowVirtue = { zikr -> selectedVirtueZikr = zikr },
                onCountIncrement = { id, maxCount ->
                    val current = countsMap[id] ?: 0
                    if (current < maxCount) {
                        countsMap[id] = current + 1
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            )
        }
    }

    // Modal Sheet for Prophetic Virtue & Reward (فضل وثواب الذكر)
    selectedVirtueZikr?.let { zikr ->
        AzkarVirtueBottomSheet(
            zikr = zikr,
            isArabic = isArabic,
            onDismiss = { selectedVirtueZikr = null }
        )
    }

    if (showPeriodicDuaaDialog) {
        PeriodicDuaaSettingsDialog(
            isArabic = isArabic,
            onDismiss = { showPeriodicDuaaDialog = false }
        )
    }
}

@Composable
fun AzkarCardItem(
    zikr: AzkarItem,
    currentCount: Int,
    isCompleted: Boolean,
    isArabic: Boolean,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onShowVirtue: () -> Unit = {},
    onCountIncrement: () -> Unit,
    onReset: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val cardBgColor = if (isCompleted) {
        EmeraldPrimary.copy(alpha = 0.035f).compositeOver(MaterialTheme.colorScheme.surface)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardBorderColor = if (isCompleted) {
        EmeraldPrimary.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBgColor,
        border = BorderStroke(1.dp, cardBorderColor),
        shadowElevation = if (isCompleted) 1.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("azkar_item_card_${zikr.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Category Badge + Reference / Share / Copy / Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = zikr.reference.ifEmpty { "حصن المسلم" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = TajawalFontFamily,
                        color = EmeraldPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Toggle Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("favorite_azkar_btn_${zikr.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onCopy, modifier = Modifier.size(38.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(38.dp)) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                    }
                }
            }

            // Main Zikr Arabic Text
            Text(
                text = zikr.text,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                fontFamily = TajawalFontFamily,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Dedicated Prophetic Virtue & Reward Button (فضل وثواب الذكر النبوي)
            if (zikr.virtue.isNotEmpty()) {
                Surface(
                    onClick = onShowVirtue,
                    shape = RoundedCornerShape(12.dp),
                    color = IslamicGold.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("azkar_virtue_btn_${zikr.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = IslamicGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = if (isArabic) "فضل وثواب هذا الذكر ✨" else "Virtue & Reward ✨",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = EmeraldPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isArabic) "كما ورد عن النبي ﷺ (اضغط للمشاهدة)" else "As taught by Prophet Muhammad ﷺ",
                                    fontSize = 10.5.sp,
                                    fontFamily = TajawalFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = IslamicGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Show Virtue",
                                    tint = IslamicGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Footer Counter Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                ) {
                    if (currentCount > 0) {
                        IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(
                        text = if (isCompleted) (if (isArabic) "مكتمل ✓" else "Done ✓") else (if (isArabic) "التكرار: $currentCount / ${zikr.count}" else "Repeat: $currentCount / ${zikr.count}"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        color = if (isCompleted) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Interactive Counter Button or Completed Badge
                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = if (isArabic) "تم بحمد الله" else "Completed",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onCountIncrement,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 42.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(18.dp))
                            Text(
                                text = "${zikr.count - currentCount}",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickReadSheetContent(
    azkarList: List<AzkarItem>,
    isArabic: Boolean,
    countsMap: Map<Int, Int>,
    viewModel: QuranViewModel? = null,
    isMorning: Boolean = true,
    onClose: () -> Unit,
    onShowVirtue: (AzkarItem) -> Unit = {},
    onCountIncrement: (id: Int, maxCount: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    text = if (isArabic) "القراءة السريعة والأوردة المباركة 📖" else "Quick Adhkar Reader",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TajawalFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(38.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prominent Button & Audio Player for Sheikh Mishary Al-Afasy inside Quick Azkar View
        if (viewModel != null) {
            AzkarAudioPlayerBanner(
                isMorning = isMorning,
                viewModel = viewModel,
                isArabic = isArabic,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(azkarList, key = { it.id }) { zikr ->
                val current = countsMap[zikr.id] ?: 0
                val completed = current >= zikr.count

                val itemBgColor = if (completed) {
                    EmeraldPrimary.copy(alpha = 0.035f).compositeOver(MaterialTheme.colorScheme.surface)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
                val itemBorderColor = if (completed) {
                    EmeraldPrimary.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                }

                Surface(
                    onClick = { onCountIncrement(zikr.id, zikr.count) },
                    shape = RoundedCornerShape(16.dp),
                    color = itemBgColor,
                    border = BorderStroke(1.dp, itemBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = zikr.text,
                            fontSize = 17.sp,
                            lineHeight = 26.sp,
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (zikr.virtue.isNotEmpty()) {
                            Surface(
                                onClick = { onShowVirtue(zikr) },
                                shape = RoundedCornerShape(8.dp),
                                color = IslamicGold.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, IslamicGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = if (isArabic) "فضل وثواب الذكر: ${zikr.virtue.take(60)}..." else "Virtue: ${zikr.virtue.take(50)}...",
                                        fontSize = 11.sp,
                                        fontFamily = TajawalFontFamily,
                                        color = EmeraldPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = zikr.reference,
                                fontSize = 11.sp,
                                fontFamily = TajawalFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )

                            Surface(
                                onClick = { onCountIncrement(zikr.id, zikr.count) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (completed) EmeraldPrimary.copy(alpha = 0.12f) else IslamicGold,
                                border = if (completed) BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f)) else null,
                                modifier = Modifier.defaultMinSize(minWidth = 72.dp, minHeight = 36.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (completed) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = if (completed) (if (isArabic) "تم" else "Done") else "$current / ${zikr.count}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = TajawalFontFamily,
                                            color = if (completed) EmeraldPrimary else Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            softWrap = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarVirtueBottomSheet(
    zikr: AzkarItem,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = IslamicGold.copy(alpha = 0.18f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = IslamicGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = if (isArabic) "فضل وثواب الذكر النبوي 🌟" else "Virtue & Prophetic Reward",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TajawalFontFamily,
                            color = EmeraldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isArabic) "كما ورد عن سيدنا رسول الله ﷺ" else "As taught by Prophet Muhammad ﷺ",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = TajawalFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Zikr Preview Text Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EmeraldPrimary.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isArabic) "نص الذكر المبارك:" else "The Blessed Zikr:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontFamily = TajawalFontFamily
                    )
                    Text(
                        text = zikr.text,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TajawalFontFamily,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Prophetic Virtue & Reward Detailed Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = IslamicGold.copy(alpha = 0.1f),
                border = BorderStroke(1.5.dp, IslamicGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IslamicGold
                        ) {
                            Text(
                                text = if (isArabic) "🌿 الفضل والثواب كما قاله النبي ﷺ" else "🌿 Prophetic Reward & Virtue",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = TajawalFontFamily,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = zikr.virtue.ifEmpty {
                            if (isArabic) "ذكر نبوي مبارك عظيم الأجر والبركة في حفظ المسلم وتقريبه إلى الله تعالى."
                            else "A blessed remembrance of great reward and divine protection."
                        },
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        fontFamily = TajawalFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = IslamicGold.copy(alpha = 0.3f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isArabic) "المصدر والتوثيق: ${zikr.reference.ifEmpty { "صحيح السنة النبوية" }}" else "Reference: ${zikr.reference}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = TajawalFontFamily,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            // Quick Actions: Copy and Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val textToCopy = "✨ فضل وثواب هذا الذكر كما قاله رسول الله ﷺ:\n\n${zikr.text}\n\n🍃 الفضل: ${zikr.virtue}\n📖 المصدر: ${zikr.reference}"
                        clipboard.setPrimaryClip(ClipData.newPlainText("Azkar Virtue", textToCopy))
                        Toast.makeText(context, if (isArabic) "تم نسخ فضل الذكر بنجاح ✨" else "Virtue copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "نسخ الفضل" else "Copy Virtue", color = EmeraldPrimary, fontFamily = TajawalFontFamily, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "فضل وثواب الذكر من السنة النبوية")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "✨ ${zikr.text}\n\n" +
                                "🍃 فضل وثواب هذا الذكر كما قاله رسول الله ﷺ:\n${zikr.virtue}\n\n" +
                                "📖 المصدر: ${zikr.reference}\n\n" +
                                "تطبيق القرآن الكريم وحصن المسلم 🕌"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, if (isArabic) "مشاركة فضل الذكر" else "Share Virtue"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "مشاركة الثواب" else "Share", color = Color.White, fontFamily = TajawalFontFamily, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

// -------------------------------------------------------------
// RECITATIONS AUDIO BANNER (SHEIKH MISHARY AL-AFASY)
// -------------------------------------------------------------
@Composable
private fun AzkarAudioPlayerBanner(
    isMorning: Boolean,
    viewModel: QuranViewModel,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isPlayingAudio.collectAsState()
    val isBuffering by viewModel.isBufferingAudio.collectAsState()
    val activeTitle by viewModel.audioActiveTitle.collectAsState()
    val progressFraction by viewModel.audioSessionProgressFraction.collectAsState()
    val elapsedMs by viewModel.audioSessionElapsedMs.collectAsState()
    val totalMs by viewModel.audioSessionTotalMs.collectAsState()

    val targetTitle = if (isMorning) "أذكار الصباح بصوت الشيخ مشاري العفاسي" else "أذكار المساء بصوت الشيخ مشاري العفاسي"
    val isThisTrackActive = activeTitle == targetTitle

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.08f),
                            IslamicGold.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isThisTrackActive && isBuffering) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isThisTrackActive && isPlaying) Icons.Default.GraphicEq else Icons.Default.Headphones,
                                contentDescription = null,
                                tint = IslamicGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = targetTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isArabic) "الشيخ مشاري راشد العفاسي • تسجيل متقن" else "Sheikh Mishary Al-Afasy • Studio Audio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldPrimary.copy(alpha = 0.15f),
                    contentColor = EmeraldPrimary
                ) {
                    Text(
                        text = if (isThisTrackActive && isPlaying) "جاري الاستماع" else "تلاوة صوتية",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // If active and playing/paused: show seek slider and controls
            if (isThisTrackActive) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = progressFraction,
                        onValueChange = { viewModel.seekAudioSessionFraction(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = EmeraldPrimary.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val elapsedSecs = (elapsedMs / 1000).toInt()
                        val totalSecs = (totalMs / 1000).toInt()
                        Text(
                            text = String.format("%02d:%02d", elapsedSecs / 60, elapsedSecs % 60),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (totalSecs > 0) String.format("%02d:%02d", totalSecs / 60, totalSecs % 60) else "--:--",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newFrac = (progressFraction - 0.03f).coerceAtLeast(0f)
                        viewModel.seekAudioSessionFraction(newFrac)
                    }) {
                        Icon(imageVector = Icons.Default.Replay10, contentDescription = "Rewind 10s")
                    }

                    FilledTonalIconButton(
                        onClick = { viewModel.toggleAudioPlayback() },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(onClick = {
                        val newFrac = (progressFraction + 0.03f).coerceAtMost(1f)
                        viewModel.seekAudioSessionFraction(newFrac)
                    }) {
                        Icon(imageVector = Icons.Default.Forward10, contentDescription = "Forward 10s")
                    }

                    IconButton(onClick = { viewModel.stopAudioPlayback() }) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                // If not currently playing, show prominent one-tap play button
                Button(
                    onClick = { viewModel.playAzkarAudio(isMorning) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "استماع للأذكار بصوت الشيخ مشاري العفاسي" else "Listen to Audio by Mishary Al-Afasy",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PERIODIC DUAA SETTINGS DIALOG (التذكير الصوتي بالأدعية والأذكار)
// -------------------------------------------------------------
@Composable
private fun PeriodicDuaaSettingsDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(PeriodicDuaaManager.isEnabled(context)) }
    var voiceSource by remember { mutableStateOf(PeriodicDuaaManager.getVoiceSource(context)) }
    var intervalMinutes by remember { mutableIntStateOf(PeriodicDuaaManager.getIntervalMinutes(context)) }
    var quietHoursEnabled by remember { mutableStateOf(PeriodicDuaaManager.isQuietHoursEnabled(context)) }
    var isTestingVoice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = if (isArabic) "التذكير الصوتي بالأدعية" else "Periodic Voice Supplications",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    fontFamily = TajawalFontFamily
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isArabic)
                        "تشغيل أدعية وأذكار صوتية مباركة تلقائياً كل فترة (مثل الصلاة على النبي ﷺ والتسبيح والاستغفار)."
                    else
                        "Automatically play voice supplications and dhikr at chosen intervals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Master Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isArabic) "تفعيل التذكير الصوتي" else "Enable Voice Reminders",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            PeriodicDuaaManager.setEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPrimary,
                            checkedTrackColor = EmeraldPrimary.copy(alpha = 0.3f)
                        )
                    )
                }

                if (isEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = if (isArabic) "صوت التذكير بالأدعية (أصوات شيوخ معروفين):" else "Supplication Voice (Renowned Sheikhs):",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PeriodicDuaaManager.VOICE_OPTIONS) { opt ->
                            val isSel = voiceSource == opt.id
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    voiceSource = opt.id
                                    PeriodicDuaaManager.setVoiceSource(context, opt.id)
                                },
                                label = {
                                    Text(
                                        text = if (isArabic) opt.nameArabic else opt.nameEnglish,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = if (isArabic) "التكرار كل:" else "Repeat Every:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Interval options
                    val intervals = listOf(15, 30, 45, 60, 120)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(intervals) { mins ->
                            val isSel = mins == intervalMinutes
                            val label = when (mins) {
                                15 -> if (isArabic) "١٥ دقيقة" else "15m"
                                30 -> if (isArabic) "٣٠ دقيقة" else "30m"
                                45 -> if (isArabic) "٤٥ دقيقة" else "45m"
                                60 -> if (isArabic) "ساعة" else "1h"
                                else -> if (isArabic) "ساعتان" else "2h"
                            }
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    intervalMinutes = mins
                                    PeriodicDuaaManager.setIntervalMinutes(context, mins)
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Quiet Hours toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "ساعات الهدوء ليلاً" else "Quiet Hours (Night)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isArabic) "إيقاف الصوت من ١١:٠٠ م إلى ٦:٣٠ ص" else "Pause voice between 11 PM and 6:30 AM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = quietHoursEnabled,
                            onCheckedChange = {
                                quietHoursEnabled = it
                                PeriodicDuaaManager.setQuietHoursEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldPrimary,
                                checkedTrackColor = EmeraldPrimary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Test Voice Button
                    OutlinedButton(
                        onClick = {
                            isTestingVoice = true
                            PeriodicDuaaManager.testVoiceNow(context) {
                                isTestingVoice = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTestingVoice) (if (isArabic) "جاري تشغيل الصوت..." else "Playing voice...") else (if (isArabic) "🔊 تجربة الصوت الآن" else "Test Voice Now"),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isArabic) "تم" else "Done",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }
    )
}
