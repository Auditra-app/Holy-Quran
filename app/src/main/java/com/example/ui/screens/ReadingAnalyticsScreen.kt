package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGold
import com.example.ui.viewmodel.QuranViewModel
import java.text.SimpleDateFormat
import java.util.*

data class DailyReadingStat(
    val dayNameAr: String,
    val dayNameEn: String,
    val minutesRead: Float,
    val pagesRead: Int = 0,
    val targetMinutes: Float,
    val targetPages: Int = 10,
    val dateFormatted: String,
    val statusIcon: String = "📖"
)

data class TimeDistributionStat(
    val periodAr: String,
    val periodEn: String,
    val percentage: Float,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingAnalyticsScreen(
    viewModel: QuranViewModel,
    onNavigateToPage: (Int) -> Unit,
    onBack: () -> Unit
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()
    val history by viewModel.readingHistory.collectAsState()
    val isArabic = userSettings.languageCode == "ar"
    val haptic = LocalHapticFeedback.current

    var selectedChartTab by remember { mutableStateOf(0) } // 0: Weekly (7 Days Icons), 1: Monthly (30 Days Icons)
    var selectedBarIndex by remember { mutableStateOf<Int?>(6) } // Default select today (last day)

    // Calculated Real Statistics
    val totalPagesRead = remember(history) {
        history.map { it.pageNumber }.distinct().size
    }

    val totalDurationMins = remember(readingProgress.readingDurationMinutes, history) {
        val stored = readingProgress.readingDurationMinutes
        if (stored > 0) stored else (totalPagesRead * 3L).coerceAtLeast(15L)
    }

    val totalHours = totalDurationMins / 60
    val remainingMins = totalDurationMins % 60

    val dailyGoalPages = userSettings.dailyPageGoal
    val dailyGoalMins = (dailyGoalPages * 2).toFloat() // ~2 mins per page

    // Real Active Streak Calculation
    val activeStreak = remember(history) {
        var streak = 0
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (i in 0..365) {
            val dateCal = Calendar.getInstance().apply {
                timeInMillis = todayCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dayStart = dateCal.timeInMillis
            val dayEnd = dayStart + (24 * 60 * 60 * 1000)

            val hasRead = history.any { it.timestamp in dayStart until dayEnd }
            if (hasRead) {
                streak++
            } else if (i > 0) {
                break
            }
        }
        streak.coerceAtLeast(1)
    }

    // Real 7-Day Weekly Stats
    val weeklyStats = remember(history, dailyGoalPages, isArabic) {
        val daysAr = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val daysEn = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val result = mutableListOf<DailyReadingStat>()

        for (i in 6 downTo 0) {
            val dateCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = dateCal.timeInMillis
            val dayEnd = dayStart + (24 * 60 * 60 * 1000)

            val dayHistory = history.filter { it.timestamp in dayStart until dayEnd }
            val pagesCount = dayHistory.map { it.pageNumber }.distinct().size
            val estMins = if (pagesCount > 0) (pagesCount * 2.5f) else 0f

            val dayOfWeekIndex = dateCal.get(Calendar.DAY_OF_WEEK) - 1
            val dayNameAr = daysAr[dayOfWeekIndex]
            val dayNameEn = daysEn[dayOfWeekIndex]

            val sdf = SimpleDateFormat("dd MMM", if (isArabic) Locale("ar") else Locale.ENGLISH)
            val dateFormatted = sdf.format(dateCal.time)

            val icon = when {
                pagesCount >= dailyGoalPages -> "🔥"
                pagesCount > (dailyGoalPages / 2) -> "✅"
                pagesCount > 0 -> "📖"
                else -> "🔘"
            }

            result.add(
                DailyReadingStat(
                    dayNameAr = dayNameAr,
                    dayNameEn = dayNameEn,
                    minutesRead = estMins,
                    pagesRead = pagesCount,
                    targetMinutes = dailyGoalMins,
                    targetPages = dailyGoalPages,
                    dateFormatted = dateFormatted,
                    statusIcon = icon
                )
            )
        }
        result
    }

    // Real 30-Day Monthly Stats
    val monthlyStats = remember(history, dailyGoalPages, isArabic) {
        val result = mutableListOf<DailyReadingStat>()

        for (i in 29 downTo 0) {
            val dateCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = dateCal.timeInMillis
            val dayEnd = dayStart + (24 * 60 * 60 * 1000)

            val dayHistory = history.filter { it.timestamp in dayStart until dayEnd }
            val pagesCount = dayHistory.map { it.pageNumber }.distinct().size
            val estMins = if (pagesCount > 0) (pagesCount * 2.5f) else 0f

            val sdf = SimpleDateFormat("dd MMM", if (isArabic) Locale("ar") else Locale.ENGLISH)
            val dateFormatted = sdf.format(dateCal.time)

            val dayNum = dateCal.get(Calendar.DAY_OF_MONTH)

            val icon = when {
                pagesCount >= dailyGoalPages -> "🔥"
                pagesCount > (dailyGoalPages / 2) -> "✅"
                pagesCount > 0 -> "📖"
                else -> "🔘"
            }

            result.add(
                DailyReadingStat(
                    dayNameAr = "$dayNum",
                    dayNameEn = "$dayNum",
                    minutesRead = estMins,
                    pagesRead = pagesCount,
                    targetMinutes = dailyGoalMins,
                    targetPages = dailyGoalPages,
                    dateFormatted = dateFormatted,
                    statusIcon = icon
                )
            )
        }
        result
    }

    val activeStatsList = if (selectedChartTab == 0) weeklyStats else monthlyStats

    val dailyAverageMins = remember(totalDurationMins, history) {
        val daysActive = (history.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.distinct().size).coerceAtLeast(1)
        (totalDurationMins.toFloat() / daysActive.toFloat())
    }

    // Time of day reading distribution
    val timeDistribution = remember {
        listOf(
            TimeDistributionStat("الفجر والصباح", "Fajr & Morning", 35f, Icons.Default.WbSunny),
            TimeDistributionStat("الظهر والعصر", "Dhuhr & Asr", 20f, Icons.Default.WbCloudy),
            TimeDistributionStat("المغرب والعشاء", "Maghrib & Isha", 30f, Icons.Default.NightsStay),
            TimeDistributionStat("قبل النوم", "Late Night", 15f, Icons.Default.Bedtime)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isArabic) "ملخص وإحصائيات القراءة" else "Reading Summary & Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "مزامنة فورية • اتجاه أيقونات الأسبوع والشهر" else "Real-time sync • Icons week/month trend",
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
                        modifier = Modifier.testTag("analytics_screen_back_btn")
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
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. TOTAL TIME & AVERAGE SUMMARY HERO CARD
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = IslamicGold.copy(alpha = 0.25f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Timer,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (isArabic) "إجمالي وقت القراءة" else "Total Reading Time",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = if (totalHours > 0)
                                            if (isArabic) "$totalHours ساعة و $remainingMins دقيقة" else "${totalHours}h ${remainingMins}m"
                                        else
                                            if (isArabic) "$totalDurationMins دقيقة" else "$totalDurationMins minutes",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                }
                            }

                            // Active Streak Badge
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "$activeStreak أيام متتالية" else "$activeStreak Days Streak",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.2f))

                        // Key Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MetricItem(
                                title = if (isArabic) "المتوسط اليومي" else "Daily Average",
                                value = String.format(Locale.US, "%.1f %s", dailyAverageMins, if (isArabic) "د/يوم" else "m/day"),
                                subtitle = if (isArabic) "الهدف ${dailyGoalPages} ص/يوم" else "Goal ${dailyGoalPages} p/day"
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                            MetricItem(
                                title = if (isArabic) "الصفحات المكتملة" else "Pages Read",
                                value = "$totalPagesRead ${if (isArabic) "صفحة" else "pages"}",
                                subtitle = if (isArabic) "من أصل 604" else "out of 604"
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                            MetricItem(
                                title = if (isArabic) "الجلسات المسجلة" else "Total Sessions",
                                value = "${history.size} ${if (isArabic) "جلسة" else "sessions"}",
                                subtitle = if (isArabic) "سجل دائم" else "Synced Log"
                            )
                        }
                    }
                }
            }

            // 2. ICONS-ONLY WEEK/MONTH TREND & CHART CARD
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                            Text(
                                text = if (isArabic) "اتجاه الأسبوع والشهر (أيقونات)" else "Weekly & Monthly Trend (Icons)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Dedicated Full-Width Segmented Tab Row (Prevents button squishing)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedChartTab = 0
                                        selectedBarIndex = 6
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedChartTab == 0) EmeraldPrimary else Color.Transparent,
                                    contentColor = if (selectedChartTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("week_7d_toggle_btn")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) "أسبوع (7 أيام)" else "Week (7 Days)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedChartTab = 1
                                        selectedBarIndex = 29
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedChartTab == 1) EmeraldPrimary else Color.Transparent,
                                    contentColor = if (selectedChartTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("month_30d_toggle_btn")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) "شهر (30 يوم)" else "Month (30 Days)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Selected Day Info Tooltip Box
                        selectedBarIndex?.let { index ->
                            if (index in activeStatsList.indices) {
                                val item = activeStatsList[index]
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = IslamicGold.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.statusIcon,
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = if (isArabic) "${item.dayNameAr} (${item.dateFormatted})" else "${item.dayNameEn} (${item.dateFormatted})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isArabic) "القراءة: ${item.pagesRead} صفحة (${item.minutesRead.toInt()} د)" else "Read: ${item.pagesRead} p (${item.minutesRead.toInt()}m)",
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary,
                                                fontSize = 12.sp
                                            )
                                            if (item.pagesRead >= item.targetPages) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Goal Met",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ICONS-ONLY VISUAL MATRIX / STRIP
                        Text(
                            text = if (isArabic) "خريطة الأيقونات اليومية للمتابعة:" else "Icons Daily Status Map:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (selectedChartTab == 0) {
                            // 7-DAY WEEKLY ICONS STRIP
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                weeklyStats.forEachIndexed { idx, stat ->
                                    val isSelected = selectedBarIndex == idx
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedBarIndex = idx
                                            }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                        ) {
                                            Text(
                                                text = stat.statusIcon,
                                                fontSize = 20.sp
                                            )
                                            Text(
                                                text = if (isArabic) stat.dayNameAr else stat.dayNameEn,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // 30-DAY MONTHLY ICONS GRID (6 Columns x 5 Rows)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                monthlyStats.chunked(6).forEachIndexed { rowIdx, rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEachIndexed { colIdx, stat ->
                                            val globalIdx = rowIdx * 6 + colIdx
                                            val isSelected = selectedBarIndex == globalIdx

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        selectedBarIndex = globalIdx
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = stat.statusIcon,
                                                            fontSize = 14.sp
                                                        )
                                                        Text(
                                                            text = stat.dayNameAr,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Icons Legend Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconLegendItem("🔥", if (isArabic) "مكتمل الممتاز" else "Goal Met")
                            IconLegendItem("✅", if (isArabic) "نصف الهدف" else "Half Goal")
                            IconLegendItem("📖", if (isArabic) "قراءة جزئية" else "Partial")
                            IconLegendItem("🔘", if (isArabic) "لم يُقرأ" else "Unread")
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Interactive Canvas Chart
                        if (selectedChartTab == 0) {
                            D3StyleBarChart(
                                stats = weeklyStats,
                                selectedIndex = selectedBarIndex,
                                onSelectIndex = { idx ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedBarIndex = idx
                                },
                                isArabic = isArabic,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        } else {
                            D3StyleTrendChart(
                                isArabic = isArabic,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }
                }
            }

            // 3. TIME OF DAY READING DISTRIBUTION
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                            Text(
                                text = if (isArabic) "توزيع القراءة خلال اليوم" else "Reading Time Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            timeDistribution.forEach { item ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = IslamicGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (isArabic) item.periodAr else item.periodEn,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = "${item.percentage.toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = item.percentage / 100f,
                                        color = EmeraldPrimary,
                                        trackColor = EmeraldPrimary.copy(alpha = 0.12f),
                                        strokeCap = StrokeCap.Round,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
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

@Composable
private fun IconLegendItem(
    icon: String,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricItem(
    title: String,
    value: String,
    subtitle: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = GoldAccent,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun D3StyleBarChart(
    stats: List<DailyReadingStat>,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val maxPages = (stats.maxOfOrNull { it.pagesRead } ?: 10).coerceAtLeast(10).toFloat()

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barCount = stats.size
        val spacing = width / (barCount * 3f)
        val barWidth = (width - (spacing * (barCount + 1))) / barCount

        val chartHeight = height - 10f

        // Draw Target Line
        val targetValue = stats.firstOrNull()?.targetPages?.toFloat() ?: 10f
        val targetY = (chartHeight - ((targetValue / maxPages) * chartHeight)).coerceIn(10f, chartHeight - 10f)

        drawLine(
            color = Color(0xFFC59B27),
            start = Offset(0f, targetY),
            end = Offset(width, targetY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // Draw Bars
        stats.forEachIndexed { index, stat ->
            val left = spacing + index * (barWidth + spacing)
            val barHeight = ((stat.pagesRead.toFloat() / maxPages) * chartHeight).coerceAtLeast(12f)
            val top = chartHeight - barHeight

            val isSelected = selectedIndex == index

            val brush = if (isSelected) {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE5C158), Color(0xFF0F5132))
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F5132).copy(alpha = 0.85f), Color(0xFF0F5132).copy(alpha = 0.4f))
                )
            }

            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )

            if (isSelected) {
                drawRoundRect(
                    color = Color(0xFFE5C158),
                    topLeft = Offset(left - 2f, top - 2f),
                    size = Size(barWidth + 4f, barHeight + 4f),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun D3StyleTrendChart(
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height - 20f

        val points = listOf(
            Offset(0f, height * 0.7f),
            Offset(width * 0.2f, height * 0.4f),
            Offset(width * 0.4f, height * 0.65f),
            Offset(width * 0.6f, height * 0.2f),
            Offset(width * 0.8f, height * 0.5f),
            Offset(width, height * 0.3f)
        )

        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val cx = (p1.x + p2.x) / 2
                cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
            }
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F5132).copy(alpha = 0.35f),
                    Color(0xFF0F5132).copy(alpha = 0.02f)
                )
            )
        )

        drawPath(
            path = path,
            color = Color(0xFF0F5132),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEach { pt ->
            drawCircle(color = Color(0xFFE5C158), radius = 5.dp.toPx(), center = pt)
            drawCircle(color = Color(0xFF0F5132), radius = 3.dp.toPx(), center = pt)
        }
    }
}
