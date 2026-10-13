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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.QuranDataProvider
import com.example.domain.model.Juz
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGold
import com.example.ui.viewmodel.QuranViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.TajawalFontFamily
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
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    var showResetDialog by remember { mutableStateOf(false) }
    var showDivisionsExplanationDialog by remember { mutableStateOf(false) }

    // Dedicated Wird Parameters (Matching reference app)
    val wirdStartPage = userSettings.khatmahCurrentPage.coerceIn(1, 604)
    val wirdDailyPages = userSettings.khatmahDailyGoalPages.coerceAtLeast(1)
    val wirdEndPage = (wirdStartPage + wirdDailyPages - 1).coerceAtMost(604)

    val totalPages = 604
    val khatmahRemainingPages = (totalPages - wirdStartPage).coerceAtLeast(0)
    val khatmahProgressFraction = (wirdStartPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)

    val animatedKhatmahProgress by animateFloatAsState(
        targetValue = khatmahProgressFraction,
        animationSpec = tween(600)
    )

    // Estimated completion date based on daily goal
    val estimatedDaysRemaining = ceil(khatmahRemainingPages.toDouble() / wirdDailyPages.toDouble()).toInt().coerceAtLeast(1)
    val estimatedFinishDateFormatted = remember(estimatedDaysRemaining) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, estimatedDaysRemaining)
        val sdf = SimpleDateFormat("dd MMMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH)
        sdf.format(cal.time)
    }

    val totalWirds = remember(wirdDailyPages) { ceil(604.0 / wirdDailyPages.toDouble()).toInt() }
    val completedWirds = userSettings.khatmahCompletedWirdsCount
    val remainingWirds = (totalWirds - completedWirds).coerceAtLeast(0)
    val khatmahProgressPercent = ((wirdStartPage.toFloat() / 604f) * 100f).toInt().coerceIn(0, 100)

    val startSurah = remember(wirdStartPage) { QuranDataProvider.getSurahByPage(wirdStartPage) }
    val startJuz = remember(wirdStartPage) { QuranDataProvider.getJuzByPage(wirdStartPage) }
    val startVerses = remember(wirdStartPage) { QuranDataProvider.getVersesForPage(wirdStartPage) }
    val firstVerse = startVerses.firstOrNull()

    val endSurah = remember(wirdEndPage) { QuranDataProvider.getSurahByPage(wirdEndPage) }
    val endVerses = remember(wirdEndPage) { QuranDataProvider.getVersesForPage(wirdEndPage) }
    val lastVerse = endVerses.lastOrNull()

    val juzList: List<Juz> = remember { QuranDataProvider.JUZ_LIST }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "الورد الحالي والختمة" else "Daily Wird & Khatmah",
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
            // =========================================================================
            // 0. DEDICATED "الورد الحالي" CARD (Matching Screenshot 3 from 'ختمة')
            // =========================================================================
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row: Juz title & "من قوله تعالى"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) startJuz.nameArabic else "Juz ${startJuz.number}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = EmeraldPrimary
                        )
                        Text(
                            text = if (isArabic) "من قوله تعالى" else "From the Words of Allah",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = TajawalFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Calligraphy Snippet
                    val snippetText = remember(firstVerse) {
                        firstVerse?.textUthmani?.let { raw ->
                            val clean = raw.replace(Regex("[﴿﴾0-9٠-٩]"), "").trim()
                            val words = clean.split(" ")
                            words.take(8).joinToString(" ")
                        } ?: (if (isArabic) "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ" else "In the name of Allah")
                    }
                    Text(
                        text = snippetText,
                        style = TextStyle(
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        color = if (isArabic) MaterialTheme.colorScheme.onSurface else EmeraldPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Start Position
                    val resumePage = remember(wirdStartPage, wirdDailyPages) {
                        viewModel.getWirdResumePage(wirdStartPage, wirdDailyPages)
                    }
                    val isResuming = resumePage > wirdStartPage

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "سورة ${startSurah.nameArabic} - آية ${toArabicDigits(firstVerse?.verseNumber ?: 1)}" else "Surah ${startSurah.nameEnglish} - Ayah ${firstVerse?.verseNumber ?: 1}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                            color = EmeraldPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isResuming) (if (isArabic) "استئناف: ص ${toArabicDigits(resumePage)}" else "Resume: p. $resumePage") else (if (isArabic) "صفحة ${toArabicDigits(wirdStartPage)}" else "Page $wirdStartPage"),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // End Position
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "إلى سورة ${endSurah.nameArabic} - آية ${toArabicDigits(lastVerse?.verseNumber ?: 1)}" else "To Surah ${endSurah.nameEnglish} - Ayah ${lastVerse?.verseNumber ?: 1}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                            color = EmeraldPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isArabic) "صفحة ${toArabicDigits(wirdEndPage)}" else "Page $wirdEndPage",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Action Buttons Row: Yellow ("أتممت القراءة >") & Green ("تابع قراءة الورد")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Yellow Button (Mark Done)
                        Button(
                            onClick = {
                                viewModel.completeWirdDirectly()
                                Toast.makeText(context, if (isArabic) "تقبل الله طاعتكم! تم إتمام الورد وحفظ تقدم الختمة." else "Wird marked as completed!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFACC15),
                                contentColor = Color(0xFF78350F)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("khatmah_done_button")
                        ) {
                            Text(
                                text = if (isArabic) "أتممت القراءة >" else "Done >",
                                style = TextStyle(
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Green Button (Read / Resume Wird)
                        Button(
                            onClick = {
                                viewModel.startWirdSession(wirdStartPage, wirdDailyPages)
                                onNavigateToPage(resumePage)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("khatmah_read_wird_button")
                        ) {
                            Icon(imageVector = Icons.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isResuming) (if (isArabic) "استئناف الورد (ص ${toArabicDigits(resumePage)})" else "Resume (p. $resumePage)") else (if (isArabic) "تابع قراءة الورد" else "Read Wird"),
                                style = TextStyle(
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // CURRENT KHATMAH PROGRESS & STATS (Dedicated Khatmah space)
            // =========================================================================
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "الختمة الحالية" else "Current Khatmah",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$khatmahProgressPercent%",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                            color = EmeraldPrimary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedKhatmahProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = EmeraldPrimary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isArabic) "الأوراد السابقة : ${toArabicDigits(completedWirds)}" else "Previous Wirds: $completedWirds",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isArabic) "الأوراد القادمة : ${toArabicDigits(remainingWirds)}" else "Upcoming Wirds: $remainingWirds",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // =========================================================================
            // DAILY TARGET PAGES ADJUSTMENT CHIPS (Restored Quranic Division Presets)
            // =========================================================================
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "الهدف اليومي لعدد الصفحات:" else "Daily Target Pages:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isArabic) "${toArabicDigits(wirdDailyPages)} صفحات / يوم" else "$wirdDailyPages pages/day",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Rich Quranic Division Presets Chips
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
                            val isSelected = (wirdDailyPages == count)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setKhatmahDailyGoalPages(count)
                                    viewModel.updateDailyPageGoal(count)
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = TextStyle(
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Slider for Custom Daily Pages Goal (1 to 60 pages)
                    Slider(
                        value = wirdDailyPages.toFloat(),
                        onValueChange = {
                            val newPages = it.toInt().coerceIn(1, 60)
                            viewModel.setKhatmahDailyGoalPages(newPages)
                            viewModel.updateDailyPageGoal(newPages)
                        },
                        valueRange = 1f..60f,
                        steps = 59,
                        colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary)
                    )
                }
            }

            // =========================================================================
            // KHATMAH PACE PLANNING & TARGET DURATION
            // =========================================================================
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
                            text = if (isArabic) "تخطيط الختمة والموعد المتوقع" else "Khatmah Planning & Estimate",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Estimated Finish Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "الموعد المتوقع لإتمام الختمة:" else "Estimated Completion Date:",
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

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isArabic) "مدة الختمة المستهدفة: ${userSettings.khatmahTargetDays} يوم" else "Target Duration: ${userSettings.khatmahTargetDays} days",
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

                            val isCompleted = wirdStartPage > juzEndPage
                            val isActive = wirdStartPage >= juz.startPage && wirdStartPage <= juzEndPage

                            JuzProgressCard(
                                juz = juz,
                                isCompleted = isCompleted,
                                isActive = isActive,
                                isArabic = isArabic,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.startWirdSession(juz.startPage, wirdDailyPages)
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
