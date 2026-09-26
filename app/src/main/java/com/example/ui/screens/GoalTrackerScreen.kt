package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.QuranDataProvider
import com.example.domain.model.Juz
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGold
import com.example.ui.viewmodel.QuranViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalTrackerScreen(
    viewModel: QuranViewModel,
    onNavigateToPage: (Int) -> Unit,
    onBack: () -> Unit
) {
    val readingProgress by viewModel.readingProgress.collectAsState()
    val readingHistory by viewModel.readingHistory.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    var showResetDialog by remember { mutableStateOf(false) }
    var showDivisionsExplanationDialog by remember { mutableStateOf(false) }

    // Pages read today from reading history
    val todayPagesRead = remember(readingHistory) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        readingHistory
            .filter { it.timestamp >= todayStart }
            .map { it.pageNumber }
            .distinct()
            .size
    }

    val dailyGoal = userSettings.dailyPageGoal
    val dailyProgressFraction = (todayPagesRead.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)

    val currentLastPage = readingProgress.lastPage
    val totalPages = 604
    val overallProgressFraction = (currentLastPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)
    val remainingPages = totalPages - currentLastPage

    val daysElapsed = remember(userSettings.khatmahStartDate) {
        val diff = System.currentTimeMillis() - userSettings.khatmahStartDate
        (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
    }

    // Estimated completion date based on daily goal
    val estimatedDaysRemaining = ceil(remainingPages.toDouble() / dailyGoal.toDouble()).toInt()
    val estimatedFinishDateFormatted = remember(estimatedDaysRemaining) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, estimatedDaysRemaining)
        val sdf = SimpleDateFormat("dd MMMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH)
        sdf.format(cal.time)
    }

    val animatedDailyProgress by animateFloatAsState(
        targetValue = dailyProgressFraction,
        animationSpec = tween(600)
    )

    val animatedOverallProgress by animateFloatAsState(
        targetValue = overallProgressFraction,
        animationSpec = tween(600)
    )

    val juzList: List<Juz> = remember { QuranDataProvider.JUZ_LIST }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "متابعة الختمة والهدف اليومي" else "Khatmah & Daily Goal Tracker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("goal_tracker_back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset Khatmah",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TODAY'S DAILY GOAL CARD
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (dailyProgressFraction >= 1f)
                        EmeraldPrimary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.5.dp, if (dailyProgressFraction >= 1f) EmeraldPrimary else GoldAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
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
                            Icon(
                                imageVector = if (dailyProgressFraction >= 1f) Icons.Default.CheckCircle else Icons.Outlined.Flag,
                                contentDescription = null,
                                tint = if (dailyProgressFraction >= 1f) EmeraldPrimary else GoldAccent
                            )
                            Text(
                                text = if (isArabic) "هدف القراءة اليومي" else "Today's Reading Goal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (dailyProgressFraction >= 1f) EmeraldPrimary else GoldAccent,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "${(animatedDailyProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Ring & Counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                            CircularProgressIndicator(
                                progress = { animatedDailyProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = if (dailyProgressFraction >= 1f) EmeraldPrimary else GoldAccent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 10.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$todayPagesRead / $dailyGoal",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isArabic) "صفحة" else "Pages",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (dailyProgressFraction >= 1f)
                                    (if (isArabic) "🎉 ماشاء الله! أكملت هدف اليوم بنجاح." else "🎉 Excellent! Today's goal achieved.")
                                else
                                    (if (isArabic) "باقي ${(dailyGoal - todayPagesRead).coerceAtLeast(0)} صفحة لتحقيق هدف اليوم." else "${(dailyGoal - todayPagesRead).coerceAtLeast(0)} pages left for today's goal."),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (dailyProgressFraction >= 1f) EmeraldPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Button(
                                onClick = {
                                    viewModel.jumpToPage(currentLastPage)
                                    onNavigateToPage(currentLastPage)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "مواصلة القراءة" else "Continue Reading")
                            }
                        }
                    }
                }
            }

            // 2. OVERALL KHATMAH PROGRESS CARD
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.EmojiEvents, contentDescription = null, tint = IslamicGold)
                            Text(
                                text = if (isArabic) "تقدم ختمة القرآن الكريم" else "Overall Quran Khatmah",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${"%.1f".format(animatedOverallProgress * 100)}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { animatedOverallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape),
                        color = EmeraldPrimary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Breakdown Grid Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBadge(
                            label = if (isArabic) "الصفحات المكتملة" else "Pages Done",
                            value = "$currentLastPage / 604",
                            icon = Icons.Default.Description
                        )
                        MetricBadge(
                            label = if (isArabic) "الأجزاء المكتملة" else "Juz Completed",
                            value = "${currentLastPage / 20} / 30",
                            icon = Icons.Default.Grid4x4
                        )
                        MetricBadge(
                            label = if (isArabic) "الصفحات المتبقية" else "Remaining",
                            value = "$remainingPages",
                            icon = Icons.Default.HourglassEmpty
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // Estimated Finish Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "الموعد المتوقع لإتمام الختمة:" else "Estimated Khatmah Finish Date:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = estimatedFinishDateFormatted,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }

                        Text(
                            text = if (isArabic) "خلال $estimatedDaysRemaining يوم" else "In $estimatedDaysRemaining days",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3. GOAL CUSTOMIZATION CONTROLS
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "تخصيص الأهداف والتخطيط" else "Goal & Pace Customization",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        IconButton(
                            onClick = { showDivisionsExplanationDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = if (isArabic) "شرح تقسيمات المصحف" else "Quran Divisions Info",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isArabic) "الهدف اليومي (عدد الصفحات): $dailyGoal صفحة" else "Daily Page Goal: $dailyGoal pages/day",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Daily Page Goal Presets Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            2 to if (isArabic) "2 صفحة (نحو ربع حزب)" else "2 pages (~Rub' Hizb)",
                            5 to if (isArabic) "5 صفحات (نصف حزب / ربع جزء)" else "5 pages (Half Hizb / Quarter Juz)",
                            10 to if (isArabic) "10 صفحات (حزب كامل)" else "10 pages (1 Hizb)",
                            20 to if (isArabic) "20 صفحة (جزء كامل)" else "20 pages (1 Juz)",
                            30 to if (isArabic) "30 صفحة (جزء ونصف)" else "30 pages (1.5 Juz)"
                        )

                        presets.forEach { (count, label) ->
                            FilterChip(
                                selected = dailyGoal == count,
                                onClick = { viewModel.updateDailyPageGoal(count) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Slider for Custom Daily Pages Goal
                    Slider(
                        value = dailyGoal.toFloat(),
                        onValueChange = { viewModel.updateDailyPageGoal(it.toInt()) },
                        valueRange = 1f..60f,
                        steps = 59,
                        colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isArabic) "مدة الختمة المستهدفة: ${userSettings.khatmahTargetDays} يوم" else "Target Khatmah Duration: ${userSettings.khatmahTargetDays} days",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val durationPresets = listOf(
                            15 to if (isArabic) "15 يوم" else "15 Days",
                            30 to if (isArabic) "30 يوم (شهر)" else "30 Days (1 Mo)",
                            60 to if (isArabic) "60 يوم (شهرين)" else "60 Days",
                            90 to if (isArabic) "90 يوم" else "90 Days",
                            365 to if (isArabic) "سنة كاملة" else "1 Year"
                        )

                        durationPresets.forEach { (days, label) ->
                            FilterChip(
                                selected = userSettings.khatmahTargetDays == days,
                                onClick = { viewModel.updateKhatmahTargetDays(days) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            // 4. VISUAL JUZ COMPLETION CHECKLIST GRID
            Text(
                text = if (isArabic) "خريطة الأجزاء الثلاثين (30 جزء)" else "30 Juz Progress Grid",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Grid of 30 Juz
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                juzList.chunked(3).forEach { rowJuz ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowJuz.forEach { juz ->
                            val juzEndPage = if (juz.number < 30) {
                                juzList.firstOrNull { it.number == juz.number + 1 }?.startPage?.minus(1) ?: 604
                            } else 604

                            val isCompleted = currentLastPage >= juzEndPage
                            val isActive = currentLastPage >= juz.startPage && currentLastPage < juzEndPage

                            JuzProgressCard(
                                juz = juz,
                                isCompleted = isCompleted,
                                isActive = isActive,
                                isArabic = isArabic,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.jumpToPage(juz.startPage)
                                    onNavigateToPage(juz.startPage)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Reset Khatmah Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(if (isArabic) "بدء ختمة جديدة؟" else "Start New Khatmah?") },
            text = {
                Text(
                    if (isArabic)
                        "هل أنت متأكد من رغبتك في إعادة ضبط تقدم الختمة والبدء من الصفحة الأولى؟ سيتم الاحتفاظ بسجل قراءتك السابق."
                    else
                        "Are you sure you want to reset your Khatmah progress and start from Page 1? Your previous reading history will be preserved."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetKhatmahProgress()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isArabic) "إعادة ضبط الختمة" else "Reset Khatmah", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Quran Divisions Explanation Dialog
    if (showDivisionsExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showDivisionsExplanationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (isArabic) "دليل تقسيمات المصحف الشريف" else "Quran Divisions Guide",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isArabic)
                            "يعتمد التخطيط على طبعة مصحف المدينة النبوية (604 صفحات):"
                        else
                            "Based on the standard Madinah Mushaf (604 pages):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    val items = if (isArabic) listOf(
                        "• القرآن كاملاً" to "30 جزءاً (60 حزباً)",
                        "• الجزء الواحد" to "حزبان ≈ 20 صفحة",
                        "• الحزب الواحد" to "نصف جزء ≈ 10 صفحات",
                        "• نصف الحزب" to "ربع جزء ≈ 5 صفحات",
                        "• ربع الحزب (۞)" to "≈ 2.5 صفحة"
                    ) else listOf(
                        "• Whole Quran" to "30 Ajza' (60 Ahzab)",
                        "• 1 Juz'" to "2 Ahzab ≈ 20 pages",
                        "• 1 Hizb" to "Half Juz ≈ 10 pages",
                        "• Half Hizb" to "Quarter Juz ≈ 5 pages",
                        "• Rub' al-Hizb (۞)" to "≈ 2.5 pages"
                    )

                    items.forEach { (term, desc) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = term,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDivisionsExplanationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (isArabic) "حسناً، فهمت" else "Got it")
                }
            }
        )
    }
}

@Composable
fun MetricBadge(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun JuzProgressCard(
    juz: Juz,
    isCompleted: Boolean,
    isActive: Boolean,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> EmeraldPrimary.copy(alpha = 0.15f)
                isActive -> GoldAccent.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isActive || isCompleted) 1.5.dp else 1.dp,
            color = when {
                isCompleted -> EmeraldPrimary
                isActive -> GoldAccent
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${juz.number}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isCompleted) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Completed", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                } else if (isActive) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Active", tint = GoldAccent, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isArabic) juz.nameArabic else juz.nameEnglish,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = if (isArabic) "ص ${juz.startPage}" else "P. ${juz.startPage}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
