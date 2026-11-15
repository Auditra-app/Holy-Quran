package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.AyahRepeatMode
import com.example.domain.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranAudioListeningScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onNavigateToPage: (Int) -> Unit
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = (userSettings.languageCode.ifBlank { "ar" }).startsWith("ar", ignoreCase = true)

    val isPlaying by viewModel.isPlayingAudio.collectAsState()
    val isBuffering by viewModel.isBufferingAudio.collectAsState()
    val sessionProgressFraction by viewModel.audioSessionProgressFraction.collectAsState()
    val sessionElapsedMs by viewModel.audioSessionElapsedMs.collectAsState()
    val sessionTotalMs by viewModel.audioSessionTotalMs.collectAsState()
    val activeTitle by viewModel.audioActiveTitle.collectAsState()
    val activeSubtitle by viewModel.audioActiveSubtitle.collectAsState()
    val activeScope by viewModel.audioActiveScope.collectAsState()
    val currentQueueIndex by viewModel.audioCurrentQueueIndex.collectAsState()
    val totalQueueCount by viewModel.audioTotalQueueCount.collectAsState()
    val sleepTimerSeconds by viewModel.audioSleepTimerRemainingSeconds.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()

    val surahs = remember { viewModel.getSurahs() }
    val juzList = remember { viewModel.getJuzList() }
    val quartersList = remember { viewModel.getQuartersList() }
    val reciters = remember { viewModel.reciters }

    // User selection state (Defaulting to SURAH as requested)
    var selectedScope by remember { mutableStateOf(AudioListeningScope.SURAH) }
    var selectedSurahId by remember { mutableIntStateOf(1) } // Default Surah Al-Fatihah
    var selectedJuzNumber by remember { mutableIntStateOf(1) }
    var selectedPageNumber by remember { mutableIntStateOf(1) }
    var selectedQuarterId by remember { mutableIntStateOf(1) }

    // Ayah range selection
    var rangeSurahId by remember { mutableIntStateOf(1) }
    var startAyah by remember { mutableIntStateOf(1) }
    var endAyah by remember { mutableIntStateOf(7) }

    // Dialogs
    var showReciterDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }

    val currentSelectedSurah = remember(selectedSurahId, surahs) {
        surahs.firstOrNull { it.id == selectedSurahId } ?: surahs.first()
    }
    val currentRangeSurah = remember(rangeSurahId, surahs) {
        surahs.firstOrNull { it.id == rangeSurahId } ?: surahs.first()
    }

    // Flag indicating user has made an active selection change in the UI
    var userSelectedNewTarget by remember { mutableStateOf(false) }

    val stagedTitle = remember(
        selectedScope,
        selectedSurahId,
        selectedJuzNumber,
        selectedPageNumber,
        selectedQuarterId,
        rangeSurahId,
        startAyah,
        endAyah,
        surahs,
        quartersList,
        isArabic
    ) {
        when (selectedScope) {
            AudioListeningScope.SURAH -> {
                val s = surahs.firstOrNull { it.id == selectedSurahId } ?: surahs.first()
                if (isArabic) "سورة ${s.nameArabic}" else "Surah ${s.nameEnglish}"
            }
            AudioListeningScope.JUZ -> {
                if (isArabic) "الجزء $selectedJuzNumber" else "Juz $selectedJuzNumber"
            }
            AudioListeningScope.QUARTER -> {
                val q = quartersList.firstOrNull { it.id == selectedQuarterId } ?: quartersList.first()
                if (isArabic) "ربع ${q.id}: ۞ ${q.firstWords}..." else "Quarter ${q.id}: ۞ ${q.firstWords}..."
            }
            AudioListeningScope.PAGE -> {
                if (isArabic) "صفحة $selectedPageNumber" else "Page $selectedPageNumber"
            }
            AudioListeningScope.AYAH_RANGE -> {
                val s = surahs.firstOrNull { it.id == rangeSurahId } ?: surahs.first()
                if (isArabic) "سورة ${s.nameArabic} ($startAyah - $endAyah)" else "${s.nameEnglish} ($startAyah - $endAyah)"
            }
        }
    }

    val stagedSubtitle = remember(
        selectedScope,
        selectedSurahId,
        selectedJuzNumber,
        selectedPageNumber,
        selectedQuarterId,
        rangeSurahId,
        startAyah,
        endAyah,
        selectedReciter,
        surahs,
        quartersList,
        isArabic
    ) {
        val reciterName = if (isArabic) selectedReciter.nameArabic else selectedReciter.nameEnglish
        when (selectedScope) {
            AudioListeningScope.SURAH -> {
                val s = surahs.firstOrNull { it.id == selectedSurahId } ?: surahs.first()
                if (isArabic) "$reciterName • تلاوة كاملة (${s.versesCount} آية)" else "$reciterName • Full Surah (${s.versesCount} verses)"
            }
            AudioListeningScope.JUZ -> {
                if (isArabic) "$reciterName • تلاوة الجزء كاملاً" else "$reciterName • Full Juz"
            }
            AudioListeningScope.QUARTER -> {
                val q = quartersList.firstOrNull { it.id == selectedQuarterId } ?: quartersList.first()
                val englishSurahName = surahs.firstOrNull { it.id == q.surahId }?.nameEnglish ?: q.surahNameArabic
                if (isArabic) "$reciterName • سورة ${q.surahNameArabic} (آية ${q.startAyah})" else "$reciterName • Surah $englishSurahName"
            }
            AudioListeningScope.PAGE -> {
                val s = surahs.lastOrNull { it.startPage <= selectedPageNumber } ?: surahs.first()
                if (isArabic) "$reciterName • سورة ${s.nameArabic}" else "$reciterName • Surah ${s.nameEnglish}"
            }
            AudioListeningScope.AYAH_RANGE -> {
                val count = (endAyah - startAyah + 1).coerceAtLeast(1)
                if (isArabic) "$reciterName • تلاوة $count آية" else "$reciterName • $count verses"
            }
        }
    }

    val isAudioPaused = viewModel.isAudioPaused()
    val hasActiveSession = (isPlaying || viewModel.hasActiveAudio()) && !userSelectedNewTarget
    val displayTitle = if (hasActiveSession && activeTitle.isNotBlank()) activeTitle else stagedTitle
    val displaySubtitle = if (hasActiveSession && activeSubtitle.isNotBlank()) activeSubtitle else stagedSubtitle
    val displayScope = if (hasActiveSession) activeScope else selectedScope

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = IslamicGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isArabic) "الاستماع للقرآن الكريم" else "Listen to Holy Quran",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                },
                actions = {
                    if (sleepTimerSeconds != null && sleepTimerSeconds!! > 0) {
                        FilledTonalButton(
                            onClick = { showSleepTimerDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = IslamicGold.copy(alpha = 0.2f),
                                contentColor = IslamicGold
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val mins = sleepTimerSeconds!! / 60
                            val secs = sleepTimerSeconds!! % 60
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        IconButton(onClick = { showSleepTimerDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = if (isArabic) "مؤقت النوم" else "Sleep Timer"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // ==========================================================
            // STEP 1 (TOP): RECITER SELECTION CARD (اختر اسم القارئ)
            // ==========================================================
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReciterDialog = true }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top bar: Label badge on one side, "تغيير القارئ" button on the other
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldPrimary.copy(alpha = 0.12f),
                                contentColor = EmeraldPrimary
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "القارئ الحالي" else "Current Reciter",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Distinct, nicely allocated "تغيير القارئ" pill button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = EmeraldPrimary,
                                contentColor = Color.White,
                                shadowElevation = 1.dp,
                                modifier = Modifier.clickable { showReciterDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        text = if (isArabic) "تغيير القارئ" else "Change Reciter",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Bottom row: Avatar + Reciter Name & Style details + Chevron
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = IslamicGold.copy(alpha = 0.15f),
                                border = BorderStroke(1.5.dp, IslamicGold.copy(alpha = 0.5f)),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = IslamicGold,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) selectedReciter.nameArabic else selectedReciter.nameEnglish,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = AmiriQuranFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isArabic) "برواية حفص عن عاصم • مرتل" else "Hafs 'an 'Asim • Murattal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================================
            // STEP 2: LISTENING MODE SELECTION (طريقة الاستماع)
            // ==========================================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "طريقة الاستماع:" else "Listening Mode:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AudioListeningScope.values()) { scope ->
                            val isSelected = scope == selectedScope
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedScope = scope
                                    userSelectedNewTarget = true
                                },
                                label = {
                                    Text(
                                        text = if (isArabic) scope.titleAr else scope.titleEn,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (scope) {
                                            AudioListeningScope.SURAH -> Icons.Default.FormatListNumbered
                                            AudioListeningScope.JUZ -> Icons.Default.Grid4x4
                                            AudioListeningScope.PAGE -> Icons.AutoMirrored.Filled.MenuBook
                                            AudioListeningScope.QUARTER -> Icons.Default.DonutLarge
                                            AudioListeningScope.AYAH_RANGE -> Icons.Default.LinearScale
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================================
            // STEP 3: SCOPE-SPECIFIC TARGET PICKER (تحديد السورة / الجزء / ...)
            // ==========================================================
            item {
                AnimatedContent(
                    targetState = selectedScope,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ScopePickerAnimation"
                ) { scope ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (scope) {
                                AudioListeningScope.SURAH -> {
                                    SurahSelectorContent(
                                        surahs = surahs,
                                        selectedSurahId = selectedSurahId,
                                        isArabic = isArabic,
                                        onSelectSurah = {
                                            selectedSurahId = it
                                            userSelectedNewTarget = true
                                        },
                                        onPlay = {
                                            userSelectedNewTarget = false
                                            viewModel.playSurahAudioDetailed(selectedSurahId, selectedReciter.id)
                                        }
                                    )
                                }
                                AudioListeningScope.JUZ -> {
                                    JuzSelectorContent(
                                        juzList = juzList,
                                        selectedJuzNumber = selectedJuzNumber,
                                        isArabic = isArabic,
                                        onSelectJuz = {
                                            selectedJuzNumber = it
                                            userSelectedNewTarget = true
                                        },
                                        onPlay = {
                                            userSelectedNewTarget = false
                                            viewModel.playJuzAudioDetailed(selectedJuzNumber, selectedReciter.id)
                                        }
                                    )
                                }
                                AudioListeningScope.PAGE -> {
                                    PageSelectorContent(
                                        selectedPage = selectedPageNumber,
                                        surahs = surahs,
                                        isArabic = isArabic,
                                        onPageChange = {
                                            selectedPageNumber = it
                                            userSelectedNewTarget = true
                                        },
                                        onPlay = {
                                            userSelectedNewTarget = false
                                            viewModel.playPageAudioDetailed(selectedPageNumber, selectedReciter.id)
                                        }
                                    )
                                }
                                AudioListeningScope.QUARTER -> {
                                    QuarterSelectorContent(
                                        quarters = quartersList,
                                        selectedQuarterId = selectedQuarterId,
                                        isArabic = isArabic,
                                        onQuarterSelect = {
                                            selectedQuarterId = it
                                            userSelectedNewTarget = true
                                        },
                                        onPlay = {
                                            userSelectedNewTarget = false
                                            viewModel.playQuarterAudioDetailed(selectedQuarterId, selectedReciter.id)
                                        }
                                    )
                                }
                                AudioListeningScope.AYAH_RANGE -> {
                                    AyahRangeSelectorContent(
                                        surahs = surahs,
                                        selectedSurahId = rangeSurahId,
                                        startAyah = startAyah,
                                        endAyah = endAyah,
                                        isArabic = isArabic,
                                        onSurahChange = { newSurahId ->
                                            rangeSurahId = newSurahId
                                            userSelectedNewTarget = true
                                            val s = surahs.firstOrNull { it.id == newSurahId }
                                            startAyah = 1
                                            endAyah = (s?.versesCount ?: 7).coerceAtMost(10)
                                        },
                                        onStartAyahChange = {
                                            startAyah = it
                                            userSelectedNewTarget = true
                                        },
                                        onEndAyahChange = {
                                            endAyah = it
                                            userSelectedNewTarget = true
                                        },
                                        onPlay = {
                                            userSelectedNewTarget = false
                                            viewModel.playAyahRangeAudioDetailed(
                                                surahId = rangeSurahId,
                                                startAyah = startAyah,
                                                endAyah = endAyah,
                                                reciterId = selectedReciter.id
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================================
            // STEP 4: INTEGRATED PLAYER & CUMULATIVE PROGRESS BAR
            // ==========================================================
            item {
                UnifiedAudioPlayerCard(
                    title = displayTitle,
                    subtitle = displaySubtitle,
                    scope = displayScope,
                    isPlaying = isPlaying,
                    isPaused = isAudioPaused,
                    isBuffering = isBuffering,
                    sessionProgressFraction = sessionProgressFraction,
                    sessionElapsedMs = sessionElapsedMs,
                    sessionTotalMs = sessionTotalMs,
                    currentQueueIndex = currentQueueIndex,
                    totalQueueCount = totalQueueCount,
                    repeatMode = repeatMode,
                    playbackSpeed = playbackSpeed,
                    isArabic = isArabic,
                    onSeekFraction = { viewModel.seekAudioSessionFraction(it) },
                    onPlayPauseToggle = {
                        if (userSelectedNewTarget) {
                            userSelectedNewTarget = false
                            startPlaybackForCurrentSelection(
                                viewModel = viewModel,
                                scope = selectedScope,
                                surahId = selectedSurahId,
                                juzNumber = selectedJuzNumber,
                                pageNumber = selectedPageNumber,
                                quarterId = selectedQuarterId,
                                rangeSurahId = rangeSurahId,
                                startAyah = startAyah,
                                endAyah = endAyah,
                                reciterId = selectedReciter.id
                            )
                        } else if (isPlaying) {
                            viewModel.pauseAudioPlayback()
                        } else if (viewModel.isAudioPaused() || viewModel.hasActiveAudio()) {
                            viewModel.resumeAudioPlayback()
                        } else {
                            userSelectedNewTarget = false
                            startPlaybackForCurrentSelection(
                                viewModel = viewModel,
                                scope = selectedScope,
                                surahId = selectedSurahId,
                                juzNumber = selectedJuzNumber,
                                pageNumber = selectedPageNumber,
                                quarterId = selectedQuarterId,
                                rangeSurahId = rangeSurahId,
                                startAyah = startAyah,
                                endAyah = endAyah,
                                reciterId = selectedReciter.id
                            )
                        }
                    },
                    onStop = { viewModel.stopAudioPlayback() },
                    onPrevious = { viewModel.skipPreviousTrack() },
                    onNext = { viewModel.skipNextTrack() },
                    onRewind10 = { viewModel.rewind10Seconds() },
                    onForward10 = { viewModel.forward10Seconds() },
                    onOpenSpeed = { showSpeedDialog = true },
                    onOpenRepeat = { showRepeatDialog = true },
                    onOpenSleepTimer = { showSleepTimerDialog = true },
                    onNavigateToMushaf = {
                        val targetPage = when (activeScope) {
                            AudioListeningScope.SURAH -> currentSelectedSurah.startPage
                            AudioListeningScope.PAGE -> selectedPageNumber
                            AudioListeningScope.JUZ -> juzList.firstOrNull { it.number == selectedJuzNumber }?.startPage ?: 1
                            AudioListeningScope.QUARTER -> quartersList.firstOrNull { it.id == selectedQuarterId }?.startPage ?: 1
                            AudioListeningScope.AYAH_RANGE -> currentRangeSurah.startPage
                        }
                        onNavigateToPage(targetPage)
                    }
                )
            }
        }
    }

    // Dialogs
    if (showReciterDialog) {
        ReciterSelectionDialog(
            reciters = reciters,
            currentReciter = selectedReciter,
            isArabic = isArabic,
            onDismiss = { showReciterDialog = false },
            onSelect = {
                viewModel.setReciter(it)
                userSelectedNewTarget = true
                showReciterDialog = false
            }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentMinutes = (sleepTimerSeconds ?: 0) / 60,
            isArabic = isArabic,
            onDismiss = { showSleepTimerDialog = false },
            onSetTimer = { minutes ->
                if (minutes > 0) {
                    viewModel.setSleepTimer(minutes)
                } else {
                    viewModel.cancelSleepTimer()
                }
                showSleepTimerDialog = false
            }
        )
    }

    if (showSpeedDialog) {
        SpeedSelectionDialog(
            currentSpeed = playbackSpeed,
            isArabic = isArabic,
            onDismiss = { showSpeedDialog = false },
            onSelectSpeed = { speed ->
                viewModel.setPlaybackSpeed(speed)
                showSpeedDialog = false
            }
        )
    }

    if (showRepeatDialog) {
        RepeatModeSelectionDialog(
            currentMode = repeatMode,
            isArabic = isArabic,
            onDismiss = { showRepeatDialog = false },
            onSelectMode = { mode ->
                viewModel.setAyahRepeatMode(mode)
                showRepeatDialog = false
            }
        )
    }
}

private fun startPlaybackForCurrentSelection(
    viewModel: QuranViewModel,
    scope: AudioListeningScope,
    surahId: Int,
    juzNumber: Int,
    pageNumber: Int,
    quarterId: Int,
    rangeSurahId: Int,
    startAyah: Int,
    endAyah: Int,
    reciterId: String
) {
    when (scope) {
        AudioListeningScope.SURAH -> viewModel.playSurahAudioDetailed(surahId, reciterId)
        AudioListeningScope.JUZ -> viewModel.playJuzAudioDetailed(juzNumber, reciterId)
        AudioListeningScope.PAGE -> viewModel.playPageAudioDetailed(pageNumber, reciterId)
        AudioListeningScope.QUARTER -> viewModel.playQuarterAudioDetailed(quarterId, reciterId)
        AudioListeningScope.AYAH_RANGE -> viewModel.playAyahRangeAudioDetailed(rangeSurahId, startAyah, endAyah, reciterId)
    }
}

// -------------------------------------------------------------
// UNIFIED AUDIO PLAYER CARD WITH CUMULATIVE SESSION PROGRESS
// -------------------------------------------------------------
@Composable
private fun UnifiedAudioPlayerCard(
    title: String,
    subtitle: String,
    scope: AudioListeningScope,
    isPlaying: Boolean,
    isPaused: Boolean,
    isBuffering: Boolean,
    sessionProgressFraction: Float,
    sessionElapsedMs: Long,
    sessionTotalMs: Long,
    currentQueueIndex: Int,
    totalQueueCount: Int,
    repeatMode: AyahRepeatMode,
    playbackSpeed: Float,
    isArabic: Boolean,
    onSeekFraction: (Float) -> Unit,
    onPlayPauseToggle: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenRepeat: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onNavigateToMushaf: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.09f),
                            IslamicGold.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row: Scope Badge, Open in Mushaf button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldPrimary.copy(alpha = 0.15f),
                        contentColor = EmeraldPrimary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when (scope) {
                                    AudioListeningScope.SURAH -> Icons.Default.FormatListNumbered
                                    AudioListeningScope.JUZ -> Icons.Default.Grid4x4
                                    AudioListeningScope.PAGE -> Icons.AutoMirrored.Filled.MenuBook
                                    AudioListeningScope.QUARTER -> Icons.Default.DonutLarge
                                    AudioListeningScope.AYAH_RANGE -> Icons.Default.LinearScale
                                },
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isArabic) scope.titleAr else scope.titleEn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TextButton(
                        onClick = onNavigateToMushaf,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = IslamicGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "عرض في المصحف" else "Open Mushaf",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IslamicGold
                        )
                    }
                }

                // Middle Row: Avatar / Equalizer, Title & Reciter, Progress Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = EmeraldPrimary,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else if (isPlaying) {
                                AnimatedEqualizer(tint = IslamicGold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = IslamicGold,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = AmiriQuranFontFamily
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Session Progress Badge: Ayah X of Y or Full Reading
                        val badgeText = if (isPlaying && totalQueueCount > 1) {
                            val percent = (sessionProgressFraction * 100).toInt()
                            if (isArabic) {
                                "الآية ${currentQueueIndex + 1} من $totalQueueCount • التقدم الإجمالي: $percent%"
                            } else {
                                "Verse ${currentQueueIndex + 1} of $totalQueueCount • Total: $percent%"
                            }
                        } else if (isPlaying) {
                            if (isArabic) "جاري الاستماع الآن" else "Currently Playing"
                        } else if (isPaused) {
                            if (isArabic) "متوقف مؤقتاً • اضغط للمتابعة" else "Paused • Tap to resume"
                        } else {
                            if (isArabic) "جاهز للتشغيل • اضغط لبدء الاستماع" else "Ready to play • Tap to start"
                        }
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ==========================================================
                // FULL SESSION CUMULATIVE PROGRESS BAR (شريط التقدم التراكمي)
                // ==========================================================
                Column(modifier = Modifier.fillMaxWidth()) {
                    var isUserSeeking by remember { mutableStateOf(false) }
                    var seekProgress by remember { mutableFloatStateOf(0f) }

                    val effectiveSliderValue = if (isUserSeeking) seekProgress else sessionProgressFraction

                    Slider(
                        value = effectiveSliderValue.coerceIn(0f, 1f),
                        onValueChange = {
                            isUserSeeking = true
                            seekProgress = it
                        },
                        onValueChangeFinished = {
                            onSeekFraction(seekProgress)
                            isUserSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = IslamicGold,
                            activeTrackColor = IslamicGold,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDurationMs(sessionElapsedMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (sessionTotalMs > 0) formatDurationMs(sessionTotalMs) else "--:--",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Primary Controllers Row: Prev, Rewind 10, Play/Pause, Forward 10, Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = if (isArabic) "السابق" else "Previous",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(onClick = onRewind10) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = if (isArabic) "تراجع" else "Rewind",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary,
                        shadowElevation = 5.dp,
                        modifier = Modifier
                            .size(64.dp)
                            .clickable(enabled = !isBuffering) { onPlayPauseToggle() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onForward10) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = if (isArabic) "تقديم" else "Forward",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = if (isArabic) "التالي" else "Next",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Secondary Row: Speed, Repeat, Stop, Sleep Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed
                    OutlinedButton(
                        onClick = onOpenSpeed,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${playbackSpeed}x", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Repeat
                    OutlinedButton(
                        onClick = onOpenRepeat,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = if (repeatMode == AyahRepeatMode.LOOP) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = null,
                            tint = if (repeatMode != AyahRepeatMode.ONCE) IslamicGold else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) repeatMode.labelAr else repeatMode.labelEn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Sleep Timer
                    OutlinedButton(
                        onClick = onOpenSleepTimer,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isArabic) "مؤقت" else "Timer", fontSize = 12.sp)
                    }

                    // Stop
                    OutlinedButton(
                        onClick = onStop,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isArabic) "إيقاف" else "Stop", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SURAH SELECTOR CONTENT (DEFAULT)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahSelectorContent(
    surahs: List<Surah>,
    selectedSurahId: Int,
    isArabic: Boolean,
    onSelectSurah: (Int) -> Unit,
    onPlay: () -> Unit
) {
    var showPickerSheet by remember { mutableStateOf(false) }
    val currentSurah = surahs.firstOrNull { it.id == selectedSurahId } ?: surahs.first()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isArabic) "اختر السورة:" else "Select Surah:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = IslamicGold.copy(alpha = 0.15f),
                contentColor = IslamicGold
            ) {
                Text(
                    text = if (isArabic) "${currentSurah.versesCount} آية (${if (currentSurah.revelationType == "Meccan") "مكية" else "مدنية"})" else "${currentSurah.versesCount} verses",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Fast, clickable Surah selector triggering searchable bottom sheet
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPickerSheet = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isArabic) "${currentSurah.id}. سورة ${currentSurah.nameArabic}" else "${currentSurah.id}. ${currentSurah.nameEnglish}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = AmiriQuranFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isArabic) "تغيير" else "Change",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Button(
            onClick = onPlay,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isArabic) "تشغيل سورة ${currentSurah.nameArabic} كاملة" else "Play Full Surah ${currentSurah.nameEnglish}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showPickerSheet) {
        SurahPickerModalBottomSheet(
            surahs = surahs,
            selectedSurahId = selectedSurahId,
            isArabic = isArabic,
            onSelectSurah = {
                onSelectSurah(it)
                showPickerSheet = false
            },
            onDismiss = { showPickerSheet = false }
        )
    }
}

// -------------------------------------------------------------
// JUZ SELECTOR CONTENT
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JuzSelectorContent(
    juzList: List<Juz>,
    selectedJuzNumber: Int,
    isArabic: Boolean,
    onSelectJuz: (Int) -> Unit,
    onPlay: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentJuz = juzList.firstOrNull { it.number == selectedJuzNumber } ?: juzList.first()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isArabic) "اختر الجزء (١ - ٣٠):" else "Select Juz' (1 - 30):",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldPrimary.copy(alpha = 0.15f),
                contentColor = EmeraldPrimary
            ) {
                Text(
                    text = if (isArabic) "يبدأ من صفحة ${currentJuz.startPage}" else "Starts at page ${currentJuz.startPage}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = if (isArabic) "${currentJuz.nameArabic} (يبدأ بـ ${currentJuz.startSurahName})" else "${currentJuz.nameEnglish} (Starts with ${currentJuz.startSurahName})",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                juzList.forEach { j ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isArabic) j.nameArabic else j.nameEnglish,
                                    fontWeight = if (j.number == selectedJuzNumber) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "ص ${j.startPage} (${j.startSurahName})",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onSelectJuz(j.number)
                            expanded = false
                        }
                    )
                }
            }
        }

        Button(
            onClick = onPlay,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isArabic) "تشغيل تلاوة ${currentJuz.nameArabic}" else "Play ${currentJuz.nameEnglish}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------
// PAGE SELECTOR CONTENT
// -------------------------------------------------------------
@Composable
private fun PageSelectorContent(
    selectedPage: Int,
    surahs: List<Surah>,
    isArabic: Boolean,
    onPageChange: (Int) -> Unit,
    onPlay: () -> Unit
) {
    val currentSurah = remember(selectedPage, surahs) {
        surahs.lastOrNull { it.startPage <= selectedPage } ?: surahs.first()
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isArabic) "اختر الصفحة (١ - ٦٠٤):" else "Select Page (1 - 604):",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = IslamicGold.copy(alpha = 0.15f),
                contentColor = IslamicGold
            ) {
                Text(
                    text = if (isArabic) "سورة ${currentSurah.nameArabic}" else currentSurah.nameEnglish,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalIconButton(
                onClick = { if (selectedPage > 1) onPageChange(selectedPage - 1) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
            }

            var pageText by remember(selectedPage) { mutableStateOf(selectedPage.toString()) }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "صفحة " else "Page ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldPrimary
                    )
                    BasicTextField(
                        value = pageText,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            pageText = digits
                            val pageNum = digits.toIntOrNull()
                            if (pageNum != null) {
                                onPageChange(pageNum.coerceIn(1, 604))
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = EmeraldPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.width(64.dp)
                    )
                }
            }

            FilledTonalIconButton(
                onClick = { if (selectedPage < 604) onPageChange(selectedPage + 1) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
            }
        }

        Slider(
            value = selectedPage.toFloat(),
            onValueChange = { onPageChange(it.toInt().coerceIn(1, 604)) },
            valueRange = 1f..604f,
            colors = SliderDefaults.colors(
                thumbColor = EmeraldPrimary,
                activeTrackColor = EmeraldPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onPlay,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isArabic) "تشغيل تلاوة صفحة $selectedPage" else "Play Page $selectedPage",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------
// QUARTER SELECTOR CONTENT (أرباع القرآن الـ 240)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuarterSelectorContent(
    quarters: List<QuranQuarter>,
    selectedQuarterId: Int,
    isArabic: Boolean,
    onQuarterSelect: (Int) -> Unit,
    onPlay: () -> Unit
) {
    var selectedJuzFilter by remember { mutableIntStateOf(1) }
    val filteredQuarters = remember(selectedJuzFilter, quarters) {
        quarters.filter { it.juzNumber == selectedJuzFilter }
    }
    val currentQuarter = quarters.firstOrNull { it.id == selectedQuarterId } ?: quarters.first()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isArabic) "اختر الربع (من ٢٤٠ ربعاً):" else "Select Quarter (1 - 240):",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = IslamicGold.copy(alpha = 0.15f),
                contentColor = IslamicGold
            ) {
                Text(
                    text = "الجزء $selectedJuzFilter",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Juz Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items((1..30).toList()) { juz ->
                val isSel = juz == selectedJuzFilter
                FilterChip(
                    selected = isSel,
                    onClick = {
                        selectedJuzFilter = juz
                        quarters.firstOrNull { it.juzNumber == juz }?.let { onQuarterSelect(it.id) }
                    },
                    label = { Text("جزء $juz", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Quarter Selection Cards in that Juz
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            filteredQuarters.forEach { q ->
                val isSelected = q.id == selectedQuarterId
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuarterSelect(q.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) EmeraldPrimary else IslamicGold.copy(alpha = 0.2f),
                                contentColor = if (isSelected) Color.White else IslamicGold,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "${q.id}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column {
                                Text(
                                    text = "۞ ${q.firstWords}...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = AmiriQuranFontFamily
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "سورة ${q.surahNameArabic} - آية ${q.startAyah} (صفحة ${q.startPage})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

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

        Button(
            onClick = onPlay,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isArabic) "تشغيل ربع ${currentQuarter.id}" else "Play Quarter ${currentQuarter.id}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------
// AYAH RANGE SELECTOR CONTENT
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyahRangeSelectorContent(
    surahs: List<Surah>,
    selectedSurahId: Int,
    startAyah: Int,
    endAyah: Int,
    isArabic: Boolean,
    onSurahChange: (Int) -> Unit,
    onStartAyahChange: (Int) -> Unit,
    onEndAyahChange: (Int) -> Unit,
    onPlay: () -> Unit
) {
    var expandedSurah by remember { mutableStateOf(false) }
    val currentSurah = surahs.firstOrNull { it.id == selectedSurahId } ?: surahs.first()
    val totalVerses = currentSurah.versesCount

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = EmeraldPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LinearScale,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = if (isArabic) "تحديد نطاق الآيات من سورة:" else "Select Verses Range:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Fast, clickable Surah selector triggering searchable bottom sheet
        var showRangeSurahPicker by remember { mutableStateOf(false) }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showRangeSurahPicker = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = IslamicGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isArabic) "${currentSurah.id}. سورة ${currentSurah.nameArabic} ($totalVerses آية)" else "${currentSurah.id}. ${currentSurah.nameEnglish} ($totalVerses v)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = AmiriQuranFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isArabic) "تغيير" else "Change",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showRangeSurahPicker) {
            SurahPickerModalBottomSheet(
                surahs = surahs,
                selectedSurahId = selectedSurahId,
                isArabic = isArabic,
                onSelectSurah = {
                    onSurahChange(it)
                    showRangeSurahPicker = false
                },
                onDismiss = { showRangeSurahPicker = false }
            )
        }

        // Range Boxes: Side by Side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernAyahStepperBox(
                label = if (isArabic) "من آية" else "From Ayah",
                value = startAyah,
                minValue = 1,
                maxValue = totalVerses,
                isArabic = isArabic,
                onValueChange = { newStart ->
                    onStartAyahChange(newStart)
                    if (newStart > endAyah) {
                        onEndAyahChange(newStart)
                    }
                },
                modifier = Modifier.weight(1f)
            )

            ModernAyahStepperBox(
                label = if (isArabic) "إلى آية" else "To Ayah",
                value = endAyah,
                minValue = 1,
                maxValue = totalVerses,
                isArabic = isArabic,
                onValueChange = { newEnd ->
                    onEndAyahChange(newEnd)
                    if (newEnd < startAyah) {
                        onStartAyahChange(newEnd)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Summary Info Pill
        val rangeCount = (endAyah - startAyah + 1).coerceAtLeast(1)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = IslamicGold.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = IslamicGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic)
                        "إجمالي الآيات: $rangeCount آية (من آية $startAyah إلى $endAyah)"
                    else
                        "Total: $rangeCount verses (Ayah $startAyah to $endAyah)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Play Button
        Button(
            onClick = onPlay,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isArabic) "تشغيل الآيات المختارة ($startAyah - $endAyah)" else "Play Selected Verses ($startAyah - $endAyah)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ModernAyahStepperBox(
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    isArabic: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Label above
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // Stepper Row: [-] [ Number Field ] [+] (Forced LTR for intuitive decrease on left, increase on right)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Minus / Decrease Button
                    Surface(
                        shape = CircleShape,
                        color = if (value > minValue) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = if (value > minValue) 1.dp else 0.dp,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(enabled = value > minValue) {
                                val newVal = (value - 1).coerceAtLeast(minValue)
                                textValue = newVal.toString()
                                onValueChange(newVal)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = if (value > minValue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Middle Text Field with manual number entry
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .widthIn(min = 52.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        BasicTextField(
                            value = textValue,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }
                                textValue = filtered
                                val num = filtered.toIntOrNull()
                                if (num != null) {
                                    val clamped = num.coerceIn(minValue, maxValue)
                                    onValueChange(clamped)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                    }

                    // Plus / Increase Button
                    Surface(
                        shape = CircleShape,
                        color = if (value < maxValue) EmeraldPrimary else EmeraldPrimary.copy(alpha = 0.4f),
                        shadowElevation = if (value < maxValue) 1.dp else 0.dp,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(enabled = value < maxValue) {
                                val newVal = (value + 1).coerceAtMost(maxValue)
                                textValue = newVal.toString()
                                onValueChange(newVal)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS: RECITERS, SLEEP TIMER, SPEED, REPEAT
// -------------------------------------------------------------
@Composable
private fun ReciterSelectionDialog(
    reciters: List<Reciter>,
    currentReciter: Reciter,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Reciter) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "اختر قارئ القرآن الكريم" else "Select Reciter",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(reciters) { r ->
                    val isSelected = r.id == currentReciter.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(r) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isArabic) r.nameArabic else r.nameEnglish,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isArabic) "مرتل بجودة عالية" else "High Quality Murattal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
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
            TextButton(onClick = onDismiss) {
                Text(text = if (isArabic) "إغلاق" else "Close")
            }
        }
    )
}

@Composable
private fun SleepTimerDialog(
    currentMinutes: Int,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    val options = listOf(
        0 to (if (isArabic) "إيقاف المؤقت ❌" else "Turn Off ❌"),
        15 to (if (isArabic) "١٥ دقيقة" else "15 Minutes"),
        30 to (if (isArabic) "٣٠ دقيقة" else "30 Minutes"),
        45 to (if (isArabic) "٤٥ دقيقة" else "45 Minutes"),
        60 to (if (isArabic) "ساعة واحدة (٦٠ دقيقة)" else "1 Hour (60 Min)"),
        120 to (if (isArabic) "ساعتان (١٢٠ دقيقة)" else "2 Hours (120 Min)")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = IslamicGold)
                Text(
                    text = if (isArabic) "مؤقت النوم والإيقاف التلقائي" else "Sleep Timer",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (mins, label) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (currentMinutes == mins && mins > 0) IslamicGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (currentMinutes == mins && mins > 0) IslamicGold else Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetTimer(mins) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, fontWeight = FontWeight.SemiBold)
                            if (currentMinutes == mins && mins > 0) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
private fun SpeedSelectionDialog(
    currentSpeed: Float,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit
) {
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "سرعة التلاوة" else "Playback Speed",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                speeds.forEach { sp ->
                    val isSel = kotlin.math.abs(currentSpeed - sp) < 0.05f
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (isSel) EmeraldPrimary else Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSpeed(sp) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (sp == 1.0f) "${sp}x (${if (isArabic) "عادية" else "Normal"})" else "${sp}x",
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
private fun RepeatModeSelectionDialog(
    currentMode: AyahRepeatMode,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSelectMode: (AyahRepeatMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "وضع التكرار" else "Repeat Mode",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AyahRepeatMode.values().forEach { mode ->
                    val isSel = mode == currentMode
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) IslamicGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (isSel) IslamicGold else Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) mode.labelAr else mode.labelEn,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) IslamicGold else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

// -------------------------------------------------------------
// VISUALIZER / EQUALIZER ANIMATION
// -------------------------------------------------------------
@Composable
private fun AnimatedEqualizer(tint: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqualizerAnimation")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 22f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bar4"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(30.dp)
    ) {
        Box(modifier = Modifier.width(3.5.dp).height(h1.dp).clip(CircleShape).background(tint))
        Box(modifier = Modifier.width(3.5.dp).height(h2.dp).clip(CircleShape).background(tint))
        Box(modifier = Modifier.width(3.5.dp).height(h3.dp).clip(CircleShape).background(tint))
        Box(modifier = Modifier.width(3.5.dp).height(h4.dp).clip(CircleShape).background(tint))
    }
}

// -------------------------------------------------------------
// TIME FORMATTER HELPER
// -------------------------------------------------------------
private fun formatDurationMs(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        val remMin = minutes % 60
        String.format("%d:%02d:%02d", hours, remMin, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

// -------------------------------------------------------------
// SEARCHABLE SURAH PICKER MODAL BOTTOM SHEET (FAST & VIRTUALIZED)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahPickerModalBottomSheet(
    surahs: List<Surah>,
    selectedSurahId: Int,
    isArabic: Boolean,
    onSelectSurah: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredSurahs = remember(searchQuery, surahs) {
        if (searchQuery.isBlank()) surahs
        else {
            val q = searchQuery.trim().lowercase()
            val normalizedQ = q.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ة", "ه")
            surahs.filter { s ->
                val normNameAr = s.nameArabic.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ة", "ه")
                s.id.toString() == q ||
                normNameAr.contains(normalizedQ) ||
                s.nameEnglish.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "اختر سورة من القرآن الكريم" else "Select Surah",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Real-time Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(if (isArabic) "ابحث باسم السورة أو رقمها (مثال: الكهف، 18)..." else "Search surah name or number...")
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Fast Virtualized LazyColumn (Instant 60fps rendering)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredSurahs, key = { it.id }) { s ->
                    val isSelected = s.id == selectedSurahId
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSurah(s.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) EmeraldPrimary else IslamicGold.copy(alpha = 0.15f),
                                    contentColor = if (isSelected) Color.White else IslamicGold,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${s.id}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = if (isArabic) "سورة ${s.nameArabic}" else s.nameEnglish,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = AmiriQuranFontFamily
                                        ),
                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isArabic) "${s.versesCount} آية • ${if (s.revelationType == "Meccan") "مكية" else "مدنية"}" else "${s.versesCount} verses",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
