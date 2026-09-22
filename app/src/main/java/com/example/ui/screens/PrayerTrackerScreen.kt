package com.example.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PrayerStatusItem
import com.example.domain.model.TrackedPrayerType
import com.example.notification.NotificationHelper
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.viewmodel.QuranViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTrackerScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onNavigateToPrayerTimes: () -> Unit = {}
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    val selectedDate by viewModel.selectedTrackerDate.collectAsState()
    val weeklySummaries by viewModel.weeklyTrackerSummaries.collectAsState()
    val kpis by viewModel.prayerTrackerKPIs.collectAsState()
    val prayerTimes = viewModel.getPrayerTimes()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val todayDateStr = remember { viewModel.getTodayDateString() }
    val isViewingToday = (selectedDate == todayDateStr)

    val selectedDaySummary = remember(weeklySummaries, selectedDate) {
        weeklySummaries.firstOrNull { it.dateString == selectedDate }
            ?: weeklySummaries.firstOrNull { it.isToday }
            ?: weeklySummaries.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isArabic) "متابع الصلوات اليومية" else "Prayers Tracker",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic) "المحافظة على الصلاة في وقتها" else "Daily, Weekly & Monthly Progress",
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = IslamicGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("prayer_tracker_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToPrayerTimes,
                        modifier = Modifier.testTag("prayer_tracker_times_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = if (isArabic) "مواقيت الصلاة" else "Prayer Times",
                            tint = EmeraldPrimary
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("prayer_tracker_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = if (isArabic) "إعدادات المتابعة والتذكير" else "Tracker Settings",
                            tint = IslamicGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hadith & Motivation Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prayer_tracker_hadith_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = IslamicGold.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
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
                                text = if (isArabic) "فضل الصلاة على وقتها" else "Virtue of Prayer on Time",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = IslamicGold
                            )
                        }

                        Text(
                            text = if (isArabic) {
                                "«سُئِلَ رسولُ اللَّهِ ﷺ: أيُّ العَمَلِ أحَبُّ إلى اللَّهِ؟ قالَ: الصَّلاةُ علَى وقْتِها»"
                            } else {
                                "The Prophet ﷺ was asked: 'Which deed is most beloved to Allah?' He replied: 'Prayer on its appointed time.'"
                            },
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isArabic) "صحيح البخاري ومسلم • خير الأعمال الصلاة على وقتها" else "Sahih Al-Bukhari & Muslim",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. KPI Cards Row (Daily, Weekly, Monthly)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isArabic) "مؤشرات الأداء والإنجاز" else "Performance & Progress",
                        fontFamily = TajawalFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Daily KPI Card
                        KPISmallCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "اليوم" else "Today",
                            value = "${kpis.dailyCompleted}/${kpis.dailyTotal}",
                            percentage = kpis.dailyPercentage,
                            subtitle = if (isArabic) "صلوات مؤداة" else "Completed",
                            accentColor = EmeraldPrimary,
                            tag = "kpi_daily"
                        )

                        // Weekly KPI Card
                        KPISmallCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "الأسبوع" else "Weekly",
                            value = "${(kpis.weeklyPercentage * 100).toInt()}%",
                            percentage = kpis.weeklyPercentage,
                            subtitle = "${kpis.weeklyCompleted}/${kpis.weeklyTotal} " + (if (isArabic) "صلاة" else "prayers"),
                            accentColor = IslamicGold,
                            tag = "kpi_weekly"
                        )

                        // Monthly KPI Card
                        KPISmallCard(
                            modifier = Modifier.weight(1f),
                            title = if (isArabic) "الشهر" else "Monthly",
                            value = "${(kpis.monthlyPercentage * 100).toInt()}%",
                            percentage = kpis.monthlyPercentage,
                            subtitle = "${kpis.monthlyCompleted} " + (if (isArabic) "صلاة" else "total"),
                            accentColor = Color(0xFF26A69A),
                            tag = "kpi_monthly"
                        )
                    }

                    // Streak and On-time Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🔥", fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = "${kpis.currentStreakDays} " + (if (isArabic) "أيام متتالية" else "Days Streak"),
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isArabic) "المحافظة على الصلوات" else "Consistent Streak",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "⏱️", fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = "${kpis.onTimeRatePercent}% " + (if (isArabic) "في وقتها" else "On Time"),
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isArabic) "الأداء في الوقت المحدد" else "Punctuality Rate",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Weekly Interactive Grid Tracker (Flipped: Vertical Days, Horizontal Prayers)
            item {
                val headerPrayers = remember(weeklySummaries) {
                    weeklySummaries.firstOrNull()?.prayers?.map { it.type } ?: emptyList()
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prayer_tracker_weekly_matrix"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isArabic) "الجدول الأسبوعي للمتابعة" else "Weekly Prayer Schedule",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (!isViewingToday) {
                                TextButton(
                                    onClick = { viewModel.setSelectedTrackerDate(todayDateStr) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "العودة لليوم" else "Go to Today",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 12.sp,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Horizontal Day Selection Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weeklySummaries.forEach { daySummary ->
                                val isSelected = (daySummary.dateString == selectedDate)
                                DayPillItem(
                                    daySummary = daySummary,
                                    isSelected = isSelected,
                                    isArabic = isArabic,
                                    onClick = {
                                        viewModel.setSelectedTrackerDate(daySummary.dateString)
                                    }
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        Text(
                            text = if (isArabic) "انقر على أي مربع لتسجيل الصلاة أو إلغائها مباشرة:" else "Tap any box below to toggle status directly:",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Flipped Table Layout (Vertical Week Days, Horizontal Prayers)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // 1. Table Header Row (Prayers across top with FULL clear names)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Day column header
                                    Text(
                                        text = if (isArabic) "اليوم" else "Day",
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(64.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    // Prayer Columns Headers with full clear names
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        headerPrayers.forEach { prayerType ->
                                            val displayName = if (isArabic) {
                                                when (prayerType) {
                                                    com.example.domain.model.TrackedPrayerType.DUHA -> "الضحى"
                                                    com.example.domain.model.TrackedPrayerType.QIYAM -> "القيام"
                                                    else -> prayerType.nameArabic
                                                }
                                            } else {
                                                prayerType.nameEnglish
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = prayerType.iconEmoji,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = displayName,
                                                    fontFamily = TajawalFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = if (headerPrayers.size > 5) 9.5.sp else 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    // Count / Progress Column Header
                                    Text(
                                        text = if (isArabic) "الإنجاز" else "Total",
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(48.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // 2. Vertical Days Rows
                            weeklySummaries.forEach { daySummary ->
                                val isSelected = (daySummary.dateString == selectedDate)
                                val isToday = daySummary.isToday
                                val allCompleted = daySummary.completionCount == daySummary.totalTrackedCount && daySummary.totalTrackedCount > 0

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        isSelected -> EmeraldPrimary.copy(alpha = 0.12f)
                                        isToday -> IslamicGold.copy(alpha = 0.10f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else if (isToday) 1.2.dp else 0.6.dp,
                                        color = when {
                                            isSelected -> EmeraldPrimary
                                            isToday -> IslamicGold
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Day Name & Date Column (Clickable to select day)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .width(64.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { viewModel.setSelectedTrackerDate(daySummary.dateString) }
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isArabic) daySummary.dayNameAr else daySummary.dayNameEn.take(3),
                                                fontFamily = TajawalFontFamily,
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.5.sp,
                                                color = if (isToday) IslamicGold else if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = "${daySummary.dayOfMonth}",
                                                    fontFamily = TajawalFontFamily,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (isToday) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = IslamicGold
                                                    ) {
                                                        Text(
                                                            text = if (isArabic) "اليوم" else "Today",
                                                            fontFamily = TajawalFontFamily,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0F382C),
                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Checkboxes for each prayer along this day's row
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            daySummary.prayers.forEach { prayerItem ->
                                                val isDone = prayerItem.isCompleted
                                                Surface(
                                                    shape = RoundedCornerShape(9.dp),
                                                    color = if (isDone) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    border = BorderStroke(
                                                        width = 1.dp,
                                                        color = if (isDone) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                                    ),
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clickable {
                                                            viewModel.togglePrayerStatus(
                                                                dateString = daySummary.dateString,
                                                                prayerKey = prayerItem.type.key,
                                                                isOnTime = true,
                                                                context = context
                                                            )
                                                        }
                                                        .testTag("weekly_toggle_${daySummary.dateString}_${prayerItem.type.key}")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        if (isDone) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(19.dp)
                                                            )
                                                        } else {
                                                            Text(
                                                                text = prayerItem.type.iconEmoji,
                                                                fontSize = 12.sp,
                                                                modifier = Modifier.alpha(0.65f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Total completion badge on right
                                        Box(
                                            modifier = Modifier.width(48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(7.dp),
                                                color = if (allCompleted) IslamicGold else EmeraldPrimary.copy(alpha = 0.12f),
                                                border = if (allCompleted) BorderStroke(0.8.dp, IslamicGold) else null
                                            ) {
                                                Text(
                                                    text = if (allCompleted) "${daySummary.completionCount}/${daySummary.totalTrackedCount} ★" else "${daySummary.completionCount}/${daySummary.totalTrackedCount}",
                                                    fontFamily = TajawalFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = if (allCompleted) Color(0xFF0F382C) else EmeraldPrimary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    textAlign = TextAlign.Center
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

            // 4. Selected Day Detailed Prayer Checklist Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prayer_tracker_today_checklist"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header of selected day
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isViewingToday) (if (isArabic) "صلوات اليوم 🕌" else "Today's Prayers")
                                        else (selectedDaySummary?.let { if (isArabic) it.dayNameAr else it.dayNameEn } ?: selectedDate),
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isViewingToday) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (isArabic) "اليوم الحالي" else "Today",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = selectedDate,
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Quick "Mark All Done" or "Reset" button
                            val allDone = (selectedDaySummary?.completionCount == selectedDaySummary?.totalTrackedCount && (selectedDaySummary?.totalTrackedCount ?: 0) > 0)
                            TextButton(
                                onClick = {
                                    selectedDaySummary?.prayers?.forEach { item ->
                                        viewModel.setPrayerStatus(
                                            dateString = selectedDate,
                                            prayerKey = item.type.key,
                                            isCompleted = !allDone,
                                            isOnTime = !allDone,
                                            context = context
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (allDone) Icons.Default.Refresh else Icons.Default.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (allDone) MaterialTheme.colorScheme.error else EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (allDone) (if (isArabic) "إلغاء الكل" else "Reset All")
                                    else (if (isArabic) "تحديد الكل" else "Check All"),
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allDone) MaterialTheme.colorScheme.error else EmeraldPrimary
                                )
                            }
                        }

                        // Progress Bar for Selected Day
                        val dayProgress = selectedDaySummary?.completionPercentage ?: 0f
                        val animatedProgress by animateFloatAsState(targetValue = dayProgress, label = "dayProgress")

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isArabic) "نسبة الإنجاز اليومي" else "Daily Completion",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(dayProgress * 100).toInt()}% (${selectedDaySummary?.completionCount ?: 0}/${selectedDaySummary?.totalTrackedCount ?: 5})",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (dayProgress >= 1f) IslamicGold else EmeraldPrimary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (dayProgress >= 1f) IslamicGold else EmeraldPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // List of prayers for the selected day
                        val prayersList = selectedDaySummary?.prayers ?: emptyList()
                        if (prayersList.isEmpty()) {
                            Text(
                                text = if (isArabic) "لا توجد صلوات محددة للمتابعة. اضغط على أيقونة الإعدادات لإضافة صلوات." else "No prayers selected. Tap settings to configure.",
                                fontFamily = TajawalFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                prayersList.forEach { prayerItem ->
                                    val timing = prayerTimes.firstOrNull { it.nameEnglish.equals(prayerItem.type.nameEnglish, ignoreCase = true) }?.timeFormatted ?: ""
                                    PrayerItemRow(
                                        item = prayerItem,
                                        timeFormatted = timing,
                                        isArabic = isArabic,
                                        onToggle = {
                                            viewModel.togglePrayerStatus(
                                                dateString = selectedDate,
                                                prayerKey = prayerItem.type.key,
                                                isOnTime = true,
                                                context = context
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Per-Prayer Breakdown (Monthly Consistency)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prayer_tracker_breakdown_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = IslamicGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isArabic) "معدل الالتزام لكل صلاة (خلال 30 يوماً)" else "Consistency Per Prayer (30 Days)",
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val breakdown = kpis.prayerBreakdown
                        val standardKeys = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            standardKeys.forEach { key ->
                                val type = TrackedPrayerType.fromKey(key) ?: return@forEach
                                val percent = breakdown[key] ?: 0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "${type.iconEmoji} " + (if (isArabic) type.nameArabic else type.nameEnglish),
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(90.dp)
                                    )

                                    LinearProgressIndicator(
                                        progress = { percent / 100f },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (percent >= 90) EmeraldPrimary else if (percent >= 60) IslamicGold else MaterialTheme.colorScheme.error,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Text(
                                        text = "$percent%",
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings Dialog / Sheet
    if (showSettingsDialog) {
        PrayerTrackerSettingsDialog(
            userSettings = userSettings,
            isArabic = isArabic,
            onDismiss = { showSettingsDialog = false },
            onSaveTrackedPrayers = { list ->
                viewModel.updateTrackedPrayers(list, context)
            },
            onSaveFollowUpSettings = { enabled, delayMins ->
                viewModel.updatePrayerFollowUpSettings(enabled, delayMins, context)
            },
            onTestNotification = {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val builder = NotificationHelper.buildPrayerFollowUpNotification(
                    context = context,
                    prayerKey = "dhuhr",
                    prayerNameAr = "الظهر",
                    prayerNameEn = "Dhuhr",
                    dateString = todayDateStr,
                    notificationId = 99991
                )
                notificationManager?.notify(99991, builder.build())
                Toast.makeText(
                    context,
                    if (isArabic) "تم إرسال إشعار تجريبي للمتابعة: هل صليت؟ 🕌" else "Test follow-up notification sent!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
private fun KPISmallCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    percentage: Float,
    subtitle: String,
    accentColor: Color,
    tag: String
) {
    Surface(
        modifier = modifier.testTag(tag),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontFamily = TajawalFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontFamily = TajawalFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = accentColor
            )
            LinearProgressIndicator(
                progress = { percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = subtitle,
                fontFamily = TajawalFontFamily,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayPillItem(
    daySummary: com.example.domain.model.DayPrayerTrackerSummary,
    isSelected: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) EmeraldPrimary else if (daySummary.isToday) IslamicGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (isSelected || daySummary.isToday) 1.5.dp else 0.5.dp,
            color = if (isSelected) EmeraldPrimary else if (daySummary.isToday) IslamicGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("day_pill_${daySummary.dateString}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isArabic) daySummary.dayNameAr else daySummary.dayNameEn.take(3),
                fontFamily = TajawalFontFamily,
                fontWeight = if (isSelected || daySummary.isToday) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${daySummary.dayOfMonth}",
                fontFamily = TajawalFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) Color.White else if (daySummary.isToday) IslamicGold else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${daySummary.completionCount}/${daySummary.totalTrackedCount}",
                fontFamily = TajawalFontFamily,
                fontSize = 10.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrayerItemRow(
    item: PrayerStatusItem,
    timeFormatted: String,
    isArabic: Boolean,
    onToggle: () -> Unit
) {
    val isDone = item.isCompleted

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDone) EmeraldPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            width = if (isDone) 1.5.dp else 1.dp,
            color = if (isDone) EmeraldPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("prayer_row_${item.type.key}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Checkbox icon button
                Surface(
                    shape = CircleShape,
                    color = if (isDone) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (isDone) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = item.type.iconEmoji, fontSize = 16.sp)
                        Text(
                            text = if (isArabic) item.type.nameArabic else item.type.nameEnglish,
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!item.type.isObligatory) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = IslamicGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isArabic) "سنة" else "Sunnah",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF795548),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (timeFormatted.isNotEmpty()) {
                        Text(
                            text = (if (isArabic) "وقت الأذان: " else "Time: ") + timeFormatted,
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Status Tag
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDone) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (isDone) {
                        if (isArabic) "تمت الصلاة 🤲" else "Completed ✅"
                    } else {
                        if (isArabic) "لم تسجل بعد ⏳" else "Pending"
                    },
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isDone) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerTrackerSettingsDialog(
    userSettings: com.example.data.local.UserSettingsEntity,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSaveTrackedPrayers: (List<String>) -> Unit,
    onSaveFollowUpSettings: (Boolean, Int) -> Unit,
    onTestNotification: () -> Unit
) {
    var followUpEnabled by remember(userSettings) {
        mutableStateOf(userSettings.enablePrayerFollowUpNotification)
    }
    var delayMinutes by remember(userSettings) {
        mutableIntStateOf(userSettings.prayerFollowUpDelayMinutes)
    }

    val currentTrackedList = remember(userSettings) {
        userSettings.trackedPrayersJson.split(",").map { it.trim().lowercase() }.toMutableStateList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = IslamicGold
                )
                Text(
                    text = if (isArabic) "إعدادات متابع الصلوات والتذكير" else "Tracker & Reminder Settings",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Follow-Up Notification Switch
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "إشعار المتابعة التفاعلي (هل صليت؟)" else "Interactive Follow-Up Alert",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isArabic) "تنبيه بعد وقت الأذان لتسجيل أداء الصلاة بنقرة واحدة." else "Prompt 30 mins after prayer to record attendance.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = followUpEnabled,
                                onCheckedChange = { followUpEnabled = it },
                                modifier = Modifier.testTag("toggle_followup_notification_switch")
                            )
                        }

                        if (followUpEnabled) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Text(
                                text = if (isArabic) "وقت التنبيه بعد الأذان:" else "Delay after prayer:",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(15, 30, 45, 60).forEach { mins ->
                                    val isSel = (delayMinutes == mins)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { delayMinutes = mins }
                                    ) {
                                        Text(
                                            text = "$mins " + (if (isArabic) "د" else "m"),
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Tracked Prayers Selection
                Text(
                    text = if (isArabic) "الصلوات التي ترغب في متابعتها:" else "Select Prayers to Track:",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                TrackedPrayerType.entries.forEach { prayerType ->
                    val isChecked = currentTrackedList.contains(prayerType.key)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isChecked) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (isChecked) EmeraldPrimary else Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) {
                                    if (currentTrackedList.size > 1) {
                                        currentTrackedList.remove(prayerType.key)
                                    }
                                } else {
                                    currentTrackedList.add(prayerType.key)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = prayerType.iconEmoji, fontSize = 16.sp)
                                Text(
                                    text = if (isArabic) prayerType.nameArabic else prayerType.nameEnglish,
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!currentTrackedList.contains(prayerType.key)) {
                                            currentTrackedList.add(prayerType.key)
                                        }
                                    } else {
                                        if (currentTrackedList.size > 1) {
                                            currentTrackedList.remove(prayerType.key)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // 3. Test Notification Button
                OutlinedButton(
                    onClick = onTestNotification,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = IslamicGold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "تجربة إشعار (هل صليت؟)" else "Send Test Notification",
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveTrackedPrayers(currentTrackedList.toList())
                    onSaveFollowUpSettings(followUpEnabled, delayMinutes)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(
                    text = if (isArabic) "حفظ التغييرات" else "Save",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isArabic) "إلغاء" else "Cancel",
                    fontFamily = TajawalFontFamily
                )
            }
        }
    )
}
