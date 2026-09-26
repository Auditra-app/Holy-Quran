package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.notification.PrayerNotificationScheduler
import com.example.notification.PrayerFollowUpScheduler
import com.example.notification.NotificationHelper
import com.example.util.LocationHelper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.ScheherazadeFontFamily
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.QuranScript
import com.example.ui.viewmodel.QuranViewModel
import com.example.util.TajweedHelper

private data class ThemeOption(
    val modeKey: String,
    val modeTitle: String,
    val bgColor: Color
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: QuranViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    var fontSize by remember(userSettings) { mutableFloatStateOf(userSettings.fontSizeSp) }
    var dailyVerseNotif by remember(userSettings) { mutableStateOf(userSettings.enableDailyVerseNotification) }
    var readingReminderNotif by remember(userSettings) { mutableStateOf(userSettings.enableReadingReminderNotification) }
    var inactivityNotif by remember(userSettings) { mutableStateOf(userSettings.enableInactivityNotification) }

    var enableAzkarNotif by remember(userSettings) { mutableStateOf(userSettings.enableAzkarNotification) }
    var enableMorningAzkar by remember(userSettings) { mutableStateOf(userSettings.enableMorningAzkar) }
    var enableEveningAzkar by remember(userSettings) { mutableStateOf(userSettings.enableEveningAzkar) }
    var morningHour by remember(userSettings) { mutableIntStateOf(userSettings.morningAzkarHour) }
    var morningMinute by remember(userSettings) { mutableIntStateOf(userSettings.morningAzkarMinute) }
    var eveningHour by remember(userSettings) { mutableIntStateOf(userSettings.eveningAzkarHour) }
    var eveningMinute by remember(userSettings) { mutableIntStateOf(userSettings.eveningAzkarMinute) }

    var showMorningTimePicker by remember { mutableStateOf(false) }
    var showEveningTimePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var reminderHour by remember(userSettings) { mutableIntStateOf(userSettings.notificationTimeHour) }
    var reminderMinute by remember(userSettings) { mutableIntStateOf(userSettings.notificationTimeMinute) }

    var showExportBackupDialog by remember { mutableStateOf(false) }
    var showImportBackupDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var updateDialogInfo by remember { mutableStateOf<com.example.data.provider.AppUpdateInfo?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(exportedJsonText.toByteArray())
                }
                Toast.makeText(
                    context,
                    if (isArabic) "تم حفظ ملف النسخة الاحتياطية بنجاح!" else "Backup file saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isArabic) "خطأ أثناء حفظ الملف: ${e.localizedMessage}" else "Error saving file: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.use { isStream ->
                    isStream.bufferedReader().use { reader -> reader.readText() }
                }
                if (!content.isNullOrBlank()) {
                    viewModel.importBackupData(content) { success ->
                        if (success) {
                            Toast.makeText(
                                context,
                                if (isArabic) "تم استعادة البيانات بنجاح!" else "Backup restored successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                if (isArabic) "تنسيق النسخة الاحتياطية غير صالح" else "Invalid backup file format",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isArabic) "فشل قراءة الملف: ${e.localizedMessage}" else "Failed to read file: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "الإعدادات" else "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Settings Section
            item {
                Text(
                    text = if (isArabic) "عام" else "General",
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Language
            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = EmeraldPrimary)
                            Text(text = if (isArabic) "لغة التطبيق" else "App Language", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = userSettings.languageCode == "ar",
                                onClick = { viewModel.updateLanguage("ar") },
                                label = { Text("العربية") }
                            )
                            FilterChip(
                                selected = userSettings.languageCode == "en",
                                onClick = { viewModel.updateLanguage("en") },
                                label = { Text("English") }
                            )
                        }
                    }
                }
            }

            // Theme Mode Selection
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = EmeraldPrimary)
                            Column {
                                Text(
                                    text = if (isArabic) "مظهر التطبيق والمصحف" else "App & Mushaf Theme",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isArabic) "اختر لون الخلفية والمظهر المفضل للقراءة" else "Choose your preferred reading color theme",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val themes = listOf(
                            ThemeOption("MUSHAF", if (isArabic) "ورقي" else "Mushaf", Color(0xFFFAF3E0)),
                            ThemeOption("SEPIA", if (isArabic) "سيبيا" else "Sepia", Color(0xFF38271C)),
                            ThemeOption("LIGHT", if (isArabic) "نهار" else "Light", Color(0xFFFFFFFF)),
                            ThemeOption("DARK", if (isArabic) "داكن" else "Dark", Color(0xFF000000)),
                            ThemeOption("SYSTEM", if (isArabic) "تلقائي" else "Auto", Color(0xFF757575))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            themes.forEach { theme ->
                                val isSelected = userSettings.themeMode == theme.modeKey
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = theme.bgColor,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) IslamicGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.updateThemeMode(theme.modeKey) }
                                        .testTag("theme_option_${theme.modeKey}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (theme.modeKey == "DARK" || theme.modeKey == "SEPIA") GoldAccent else EmeraldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            if (isSelected) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = theme.modeTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (theme.modeKey == "DARK" || theme.modeKey == "SEPIA") Color.White else Color.Black,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Night Mode & Auto-Schedule Configuration Card
            item {
                var showStartTimePicker by remember { mutableStateOf(false) }
                var showEndTimePicker by remember { mutableStateOf(false) }

                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title & Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = IslamicGold
                                )
                                Column {
                                    Text(
                                        text = if (isArabic) "وضع القراءة الليلي وإراحة العين" else "Night Mode & Eye Protection",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (isArabic) "تطبيق نسق سيبيا دافئ أو داكن لتقليل إجهاد العين أثناء التلاوة الليلة" else "Apply warm sepia or dark theme to reduce eye strain during late-night recitation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Preferred Night Theme Style Selection
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "نسق الوضع الليلي المفضل:" else "Preferred Night Theme Style:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val isSepiaSelected = userSettings.nightModeTheme == "SEPIA"
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF38271C),
                                    border = BorderStroke(
                                        width = if (isSepiaSelected) 2.5.dp else 1.dp,
                                        color = if (isSepiaSelected) IslamicGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.updateNightModeSettings(nightModeTheme = "SEPIA") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSepiaSelected,
                                            onClick = { viewModel.updateNightModeSettings(nightModeTheme = "SEPIA") },
                                            colors = RadioButtonDefaults.colors(selectedColor = IslamicGold)
                                        )
                                        Column {
                                            Text(
                                                text = if (isArabic) "سيبيا دافئ" else "Warm Sepia",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF5EBE0)
                                            )
                                            Text(
                                                text = if (isArabic) "مقاوم للضوء الأزرق" else "Eye-Safe Amber",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFE2D0BE)
                                            )
                                        }
                                    }
                                }

                                val isDarkSelected = userSettings.nightModeTheme == "DARK"
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF000000),
                                    border = BorderStroke(
                                        width = if (isDarkSelected) 2.5.dp else 1.dp,
                                        color = if (isDarkSelected) IslamicGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.updateNightModeSettings(nightModeTheme = "DARK") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = isDarkSelected,
                                            onClick = { viewModel.updateNightModeSettings(nightModeTheme = "DARK") },
                                            colors = RadioButtonDefaults.colors(selectedColor = IslamicGold)
                                        )
                                        Column {
                                            Text(
                                                text = if (isArabic) "ليلي داكن" else "OLED Dark",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (isArabic) "تباين عالي للظلام" else "Deep Black",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFC2D4C8)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Schedule Auto-Toggle Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "التفعيل التلقائي بالجدول الزمني" else "Schedule Auto-Toggle",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isArabic) "التبديل التلقائي للوضع الليلي يومياً في أوقات محددة" else "Automatically activate night mode during scheduled hours",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = userSettings.enableNightSchedule,
                                onCheckedChange = { viewModel.updateNightModeSettings(enableNightSchedule = it) },
                                modifier = Modifier.testTag("night_schedule_switch")
                            )
                        }

                        if (userSettings.enableNightSchedule) {
                            // Active Schedule Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val startStr = String.format("%02d:%02d", userSettings.nightScheduleStartHour, userSettings.nightScheduleStartMinute)
                                    val endStr = String.format("%02d:%02d", userSettings.nightScheduleEndHour, userSettings.nightScheduleEndMinute)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Start Time Button
                                        OutlinedButton(
                                            onClick = { showStartTimePicker = true },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, IslamicGold),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = if (isArabic) "وقت البداية" else "Start Time",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = startStr,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldPrimary
                                                )
                                            }
                                        }

                                        Text(
                                            text = " ➔ ",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = IslamicGold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )

                                        // End Time Button
                                        OutlinedButton(
                                            onClick = { showEndTimePicker = true },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, IslamicGold),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = if (isArabic) "وقت الانتهاء" else "End Time",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = endStr,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldPrimary
                                                )
                                            }
                                        }
                                    }

                                    // Presets
                                    Text(
                                        text = if (isArabic) "جداول سريعة جاهزة:" else "Quick Schedule Presets:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilterChip(
                                            selected = userSettings.nightScheduleStartHour == 20 && userSettings.nightScheduleEndHour == 6,
                                            onClick = {
                                                viewModel.updateNightModeSettings(startHour = 20, startMinute = 0, endHour = 6, endMinute = 0)
                                            },
                                            label = { Text(if (isArabic) "8 م - 6 ص" else "8 PM - 6 AM", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = userSettings.nightScheduleStartHour == 22 && userSettings.nightScheduleEndHour == 5,
                                            onClick = {
                                                viewModel.updateNightModeSettings(startHour = 22, startMinute = 0, endHour = 5, endMinute = 0)
                                            },
                                            label = { Text(if (isArabic) "10 م - 5 ص" else "10 PM - 5 AM", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = userSettings.nightScheduleStartHour == 19 && userSettings.nightScheduleEndHour == 6,
                                            onClick = {
                                                viewModel.updateNightModeSettings(startHour = 19, startMinute = 0, endHour = 6, endMinute = 0)
                                            },
                                            label = { Text(if (isArabic) "المغرب للفجر" else "7 PM - 6 AM", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Active Indicator
                        val isNightActiveNow = userSettings.effectiveThemeMode() == "SEPIA" || userSettings.effectiveThemeMode() == "DARK"
                        if (isNightActiveNow) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = IslamicGold.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = IslamicGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "الوضع الليلي نشط حالياً على واجهة القراءة (${if (userSettings.effectiveThemeMode() == "SEPIA") "سيبيا دافئ" else "ليلي داكن"})"
                                        else "Night mode is active right now (${if (userSettings.effectiveThemeMode() == "SEPIA") "Warm Sepia" else "OLED Dark"})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                if (showStartTimePicker) {
                    ScheduleTimePickerDialog(
                        title = if (isArabic) "اختر وقت بداية الوضع الليلي" else "Select Night Mode Start Time",
                        initialHour = userSettings.nightScheduleStartHour,
                        initialMinute = userSettings.nightScheduleStartMinute,
                        isArabic = isArabic,
                        onDismiss = { showStartTimePicker = false },
                        onTimeSelected = { hour, minute ->
                            viewModel.updateNightModeSettings(startHour = hour, startMinute = minute)
                            showStartTimePicker = false
                        }
                    )
                }

                if (showEndTimePicker) {
                    ScheduleTimePickerDialog(
                        title = if (isArabic) "اختر وقت انتهاء الوضع الليلي" else "Select Night Mode End Time",
                        initialHour = userSettings.nightScheduleEndHour,
                        initialMinute = userSettings.nightScheduleEndMinute,
                        isArabic = isArabic,
                        onDismiss = { showEndTimePicker = false },
                        onTimeSelected = { hour, minute ->
                            viewModel.updateNightModeSettings(endHour = hour, endMinute = minute)
                            showEndTimePicker = false
                        }
                    )
                }
            }

            // Show English Translation Toggle
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Translate, contentDescription = null, tint = EmeraldPrimary)
                            Column {
                                Text(
                                    text = if (isArabic) "إظهار الترجمة الإنجليزية" else "Show English Translation",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isArabic) "عند إيقافها، تظهر صفحة القرآن بنص عثماني خالص كالمصحف" else "When disabled, shows clean Uthmani text like Madinah Mushaf",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = userSettings.showEnglishTranslation,
                            onCheckedChange = { viewModel.toggleEnglishTranslation() },
                            modifier = Modifier.testTag("toggle_english_translation_switch")
                        )
                    }
                }
            }

            // Keep Screen Awake Toggle Card
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = EmeraldPrimary)
                            Column {
                                Text(
                                    text = if (isArabic) "إبقاء الشاشة مضاءة" else "Keep Screen Awake",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isArabic) "منع إيقاف الشاشة التلقائي أثناء قراءة وتلاوة القرآن الكريم" else "Prevent display timeout while reading or listening to the Quran",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = userSettings.keepScreenOn,
                            onCheckedChange = { viewModel.updateKeepScreenOn(it) },
                            modifier = Modifier.testTag("keep_screen_on_switch")
                        )
                    }
                }
            }

            // Tajweed Color Coding Toggle Card
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = EmeraldPrimary)
                                Column {
                                    Text(
                                        text = if (isArabic) "تلوين أحكام التجويد" else "Tajweed Color Coding",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (isArabic) "تظليل الحروف وأحكام التلاوة (المدود، القلقلة، الغنة، الإخفاء) بألوان مخصصة" else "Highlight letters according to Tajweed rules for correct pronunciation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = userSettings.enableTajweed,
                                onCheckedChange = { viewModel.updateTajweedSetting(it) },
                                modifier = Modifier.testTag("tajweed_color_switch")
                            )
                        }

                        if (userSettings.enableTajweed) {
                            // Live Tajweed Sample Verse Preview Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "معاينة أحكام التجويد الملونة:" else "Tajweed Color Coding Preview:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    val sampleVerse = "قُلْ أَعُوذُ بِرَبِّ ٱلْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ"
                                    val isDarkMode = userSettings.themeMode == "DARK"
                                    val annotatedSample = remember(userSettings.enableTajweed, isDarkMode) {
                                        TajweedHelper.buildTajweedAnnotatedString(
                                            text = sampleVerse,
                                            enableTajweed = true,
                                            baseColor = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                            isDarkMode = isDarkMode
                                        )
                                    }

                                    Text(
                                        text = annotatedSample,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = QuranScript.fromId(userSettings.quranScriptType).getFontFamily(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            lineHeight = 44.sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    // Tajweed Rules Key / Legend
                                    Text(
                                        text = if (isArabic) "دليل ألوان التجويد:" else "Tajweed Color Key:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        TajweedLegendBadge(color = TajweedHelper.ColorMadd, label = if (isArabic) "مدود" else "Madd", modifier = Modifier.weight(1f))
                                        TajweedLegendBadge(color = TajweedHelper.ColorGhunnah, label = if (isArabic) "غنة" else "Ghunnah", modifier = Modifier.weight(1f))
                                        TajweedLegendBadge(color = TajweedHelper.ColorQalqalah, label = if (isArabic) "قلقلة" else "Qalqalah", modifier = Modifier.weight(1f))
                                        TajweedLegendBadge(color = TajweedHelper.ColorIkhfa, label = if (isArabic) "إخفاء" else "Ikhfa", modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quran Mushaf Script & Calligraphy Selection Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_script_selection_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = IslamicGold.copy(alpha = 0.15f),
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
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (isArabic) "اختر رسم ونوع الخط المفضل لصفحات المصحف الشريف" else "Choose your preferred Quranic script for reader pages",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        val selectedScript = QuranScript.fromId(userSettings.quranScriptType)

                        // Script List Cards
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuranScript.entries.forEach { script ->
                                val isSelected = script == selectedScript
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updateQuranScript(script.id) }
                                        .testTag("quran_script_option_${script.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                                    onClick = { viewModel.updateQuranScript(script.id) },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = EmeraldPrimary
                                                    )
                                                )
                                                Column {
                                                    Text(
                                                        text = if (isArabic) script.titleArabic else script.titleEnglish,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = if (isArabic) script.subtitleArabic else script.subtitleEnglish,
                                                        style = MaterialTheme.typography.labelSmall,
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
                                                        text = if (isArabic) "مفعّل حالياً" else "Active",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = if (isArabic) script.descriptionArabic else script.descriptionEnglish,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Sample Verse rendered in this EXACT font
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                            border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = script.sampleVerse,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontFamily = script.getFontFamily(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp,
                                                    lineHeight = 38.sp,
                                                    textAlign = TextAlign.Center
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Font Size & Uthmani Script Scaling
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FormatSize, contentDescription = null, tint = EmeraldPrimary)
                                Column {
                                    Text(
                                        text = if (isArabic) "حجم الخط والنص العثماني" else "Uthmani Script Font Size",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (isArabic) "تكبير وتصغير آيات المصحف والقراءة بسهولة" else "Scale Quran Uthmani text for optimal reading and accessibility",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Live Uthmani Preview Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "معاينة مباشرة للخط المختار:" else "Live Selected Script Preview:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldPrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${fontSize.toInt()} sp",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ۝ ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = QuranScript.fromId(userSettings.quranScriptType).getFontFamily(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (fontSize * 1.15f).sp,
                                        lineHeight = (fontSize * 2.2f).sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }

                        // Slider with Stepper Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    val newSize = (fontSize - 1f).coerceAtLeast(16f)
                                    fontSize = newSize
                                    viewModel.updateFontSize(newSize)
                                },
                                enabled = fontSize > 16f,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("decrease_font_size_btn"),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Font Size", modifier = Modifier.size(18.dp))
                            }

                            Slider(
                                value = fontSize,
                                onValueChange = { fontSize = it },
                                onValueChangeFinished = { viewModel.updateFontSize(fontSize) },
                                valueRange = 16f..42f,
                                steps = 25,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("font_size_slider")
                            )

                            FilledIconButton(
                                onClick = {
                                    val newSize = (fontSize + 1f).coerceAtMost(42f)
                                    fontSize = newSize
                                    viewModel.updateFontSize(newSize)
                                },
                                enabled = fontSize < 42f,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("increase_font_size_btn"),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Font Size", modifier = Modifier.size(18.dp))
                            }
                        }

                        // Quick Accessibility Presets
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isArabic) "حجوم سريعة متوافقة مع إمكانية الوصول:" else "Quick Accessibility Presets:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val presets = listOf(
                                18f to if (isArabic) "صغير" else "Small",
                                22f to if (isArabic) "عادي" else "Default",
                                28f to if (isArabic) "كبير" else "Large",
                                34f to if (isArabic) "كبير جداً" else "X-Large",
                                40f to if (isArabic) "أقصى حجم" else "Maximum"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                presets.forEach { (presetVal, label) ->
                                    val isSelected = fontSize.toInt() == presetVal.toInt()
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            fontSize = presetVal
                                            viewModel.updateFontSize(presetVal)
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("preset_font_${presetVal.toInt()}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notifications Section
            item {
                Text(
                    text = if (isArabic) "التنبيهات والإشعارات" else "Notifications & Reminders",
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isArabic) "إشعار آية اليوم" else "Daily Verse Notification", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = dailyVerseNotif,
                                onCheckedChange = {
                                    dailyVerseNotif = it
                                    viewModel.updateNotificationSetting(dailyVerseNotif, readingReminderNotif, inactivityNotif, context)
                                }
                            )
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isArabic) "تذكير القراءة اليومي" else "Daily Reading Reminder", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = readingReminderNotif,
                                onCheckedChange = {
                                    readingReminderNotif = it
                                    viewModel.updateNotificationSetting(dailyVerseNotif, readingReminderNotif, inactivityNotif, context)
                                }
                            )
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isArabic) "وقت التذكير اليومي" else "Daily Reminder Time", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = String.format(java.util.Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { showReminderTimePicker = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "تغيير" else "Change", fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = if (isArabic) "تذكير «هل صليت؟» (متابعة الصلاة)" else "Salat Reminder («Have you prayed?»)",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isArabic)
                                        "إشعار تفاعلي بعد الصلاة لتسجيل أدائها (مثل: هل صليت العصر؟) - مستقل عن صوت الأذان"
                                    else
                                        "Interactive reminder after prayer time to log attendance (e.g. Have you prayed Asr?) - separate from Azan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = userSettings.enablePrayerFollowUpNotification,
                                onCheckedChange = { enabled ->
                                    viewModel.updatePrayerFollowUpSettings(
                                        enabled = enabled,
                                        delayMinutes = userSettings.prayerFollowUpDelayMinutes,
                                        context = context
                                    )
                                    Toast.makeText(
                                        context,
                                        if (isArabic) {
                                            if (enabled) "تم تفعيل تذكير متابعة الصلاة (هل صليت؟) 🕌"
                                            else "تم تعطيل تذكير متابعة الصلاة (هل صليت؟) 🔕"
                                        } else {
                                            if (enabled) "Salat follow-up reminder enabled 🕌"
                                            else "Salat follow-up reminder disabled 🔕"
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.testTag("salat_followup_reminder_switch")
                            )
                        }
                        if (userSettings.enablePrayerFollowUpNotification) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "توقيت التذكير بعد وقت الصلاة:" else "Reminder delay after prayer:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val delayOptions = listOf(15, 30, 45, 60)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    delayOptions.forEach { mins ->
                                        val isSelected = userSettings.prayerFollowUpDelayMinutes == mins
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.updatePrayerFollowUpSettings(
                                                    enabled = true,
                                                    delayMinutes = mins,
                                                    context = context
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = if (isArabic) "$mins د" else "${mins}m",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier.height(28.dp)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isArabic) "التوقيت الصيفي (+1 ساعة)" else "Summer Time / DST (+1 Hour)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (userSettings.isSummerTime) (if (isArabic) "مُفعل (+1 ساعة)" else "Enabled (+1h)") else (if (isArabic) "غير مفعل (توقيت عادي)" else "Disabled"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = userSettings.isSummerTime,
                                onCheckedChange = {
                                    viewModel.updateSummerTimeSetting(it)
                                }
                            )
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    com.example.notification.DailyVerseScheduler.scheduleTestDailyVerseNotification(context, delaySeconds = 2)
                                    Toast.makeText(context, if (isArabic) "سيصلك إشعار آية اليوم خلال ثانيتين..." else "Daily Verse notification in 2s...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isArabic) "اختبار آية اليوم 📖" else "Test Verse 📖",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    com.example.notification.ReadingReminderScheduler.scheduleTestReadingReminder(context, delaySeconds = 2)
                                    Toast.makeText(context, if (isArabic) "سيصلك تذكير القراءة خلال ثانيتين..." else "Reading reminder in 2s...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isArabic) "اختبار القراءة 📖" else "Test Reading 📖",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    PrayerNotificationScheduler.scheduleTestNotification(context, delaySeconds = 2)
                                    Toast.makeText(context, if (isArabic) "سيصلك إشعار الصلاة (الأذان) خلال ثانيتين..." else "Prayer Azan notification in 2s...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isArabic) "اختبار الأذان 🕌" else "Test Azan 🕌",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                                    val builder = NotificationHelper.buildPrayerFollowUpNotification(
                                        context = context,
                                        prayerKey = "asr",
                                        prayerNameAr = "العصر",
                                        prayerNameEn = "Asr",
                                        dateString = todayStr,
                                        notificationId = 99992
                                    )
                                    notificationManager?.notify(99992, builder.build())
                                    Toast.makeText(
                                        context,
                                        if (isArabic) "تم إرسال إشعار تجريبي: هل صليت صلاة العصر؟ 🕌" else "Test reminder sent: Have you prayed Asr? 🕌",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircleOutline, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isArabic) "اختبار: هل صليت؟ 🤲" else "Test: Did you pray? 🤲",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IslamicGold
                                )
                            }
                        }
                    }
                }
            }

            // Dedicated Azkar Reminders Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("azkar_reminders_card")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        if (isArabic) "تذكير أذكار الصباح والمساء 🤲" else "Azkar Reminders 🤲",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        if (isArabic) "تنبيهات يومية مخصصة لأذكار الصباح والمساء" else "Custom daily alerts for morning & evening Azkar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = enableAzkarNotif,
                                onCheckedChange = { checked ->
                                    enableAzkarNotif = checked
                                    viewModel.updateAzkarNotificationSetting(
                                        enableAzkar = enableAzkarNotif,
                                        enableMorning = enableMorningAzkar,
                                        enableEvening = enableEveningAzkar,
                                        morningHour = morningHour,
                                        morningMinute = morningMinute,
                                        eveningHour = eveningHour,
                                        eveningMinute = eveningMinute,
                                        context = context
                                    )
                                },
                                modifier = Modifier.testTag("azkar_master_switch")
                            )
                        }

                        if (enableAzkarNotif) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // Morning Azkar Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = enableMorningAzkar,
                                        onCheckedChange = { checked ->
                                            enableMorningAzkar = checked
                                            viewModel.updateAzkarNotificationSetting(
                                                enableAzkar = enableAzkarNotif,
                                                enableMorning = enableMorningAzkar,
                                                enableEvening = enableEveningAzkar,
                                                morningHour = morningHour,
                                                morningMinute = morningMinute,
                                                eveningHour = eveningHour,
                                                eveningMinute = eveningMinute,
                                                context = context
                                            )
                                        },
                                        modifier = Modifier.testTag("morning_azkar_switch")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isArabic) "أذكار الصباح ☀️" else "Morning Azkar ☀️",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Surface(
                                    onClick = { showMorningTimePicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                                    modifier = Modifier.testTag("morning_time_picker")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = formatTime12H(morningHour, morningMinute, isArabic),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = EmeraldPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Evening Azkar Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = enableEveningAzkar,
                                        onCheckedChange = { checked ->
                                            enableEveningAzkar = checked
                                            viewModel.updateAzkarNotificationSetting(
                                                enableAzkar = enableAzkarNotif,
                                                enableMorning = enableMorningAzkar,
                                                enableEvening = enableEveningAzkar,
                                                morningHour = morningHour,
                                                morningMinute = morningMinute,
                                                eveningHour = eveningHour,
                                                eveningMinute = eveningMinute,
                                                context = context
                                            )
                                        },
                                        modifier = Modifier.testTag("evening_azkar_switch")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isArabic) "أذكار المساء 🌙" else "Evening Azkar 🌙",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Surface(
                                    onClick = { showEveningTimePicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                                    modifier = Modifier.testTag("evening_time_picker")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = formatTime12H(eveningHour, eveningMinute, isArabic),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = EmeraldPrimary
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // Test Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        com.example.notification.AzkarNotificationScheduler.scheduleTestAzkarNotification(context, isMorning = true, delaySeconds = 2)
                                        Toast.makeText(context, if (isArabic) "سيصلك إشعار أذكار الصباح التجريبي خلال ثانيتين..." else "Test Morning Azkar notification in 2 seconds...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (isArabic) "اختبار أذكار الصباح ☀️" else "Test Morning Azkar ☀️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                                OutlinedButton(
                                    onClick = {
                                        com.example.notification.AzkarNotificationScheduler.scheduleTestAzkarNotification(context, isMorning = false, delaySeconds = 2)
                                        Toast.makeText(context, if (isArabic) "سيصلك إشعار أذكار المساء التجريبي خلال ثانيتين..." else "Test Evening Azkar notification in 2 seconds...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (isArabic) "اختبار أذكار المساء 🌙" else "Test Evening Azkar 🌙", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // System Permissions & Notifications Section
            item {
                Text(
                    text = if (isArabic) "صلاحيات الإشعارات والموقع" else "Notification & Location Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                var hasLocationPerm by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }
                var hasNotifPerm by remember { mutableStateOf(LocationHelper.hasNotificationPermission(context)) }

                val settingsPermLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    hasLocationPerm = LocationHelper.hasLocationPermission(context)
                    hasNotifPerm = LocationHelper.hasNotificationPermission(context)
                    if (hasLocationPerm) {
                        LocationHelper.getCurrentLocation(context) { lat, lng, cityName ->
                            viewModel.setUserGPSLocation(lat, lng, cityName)
                        }
                    }
                }

                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Location Permission Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = EmeraldPrimary)
                                Column {
                                    Text(if (isArabic) "صلاحية تحديد الموقع (GPS)" else "Location Permission (GPS)", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (hasLocationPerm) (if (isArabic) "مُفعّلة (حساب دقيق للمواقيت)" else "Granted")
                                        else (if (isArabic) "غير مفعلة (يستخدم الموقع الافتراضي)" else "Not Granted"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasLocationPerm) EmeraldPrimary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (hasLocationPerm) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Granted", tint = EmeraldPrimary)
                            } else {
                                Button(
                                    onClick = {
                                        settingsPermLauncher.launch(
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(if (isArabic) "تفعيل" else "Grant")
                                }
                            }
                        }

                        HorizontalDivider()

                        // Notification Permission Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = EmeraldPrimary)
                                Column {
                                    Text(if (isArabic) "إشعارات مواقيت الصلاة والتنبيهات" else "Prayer Time Notifications", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (hasNotifPerm) (if (isArabic) "مُفعّلة (تنبيه عند كل صلاة)" else "Granted")
                                        else (if (isArabic) "غير مفعلة (لن تصلك إشعارات المواقيت)" else "Not Granted (No prayer time alerts)"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasNotifPerm) EmeraldPrimary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (hasNotifPerm) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Granted", tint = EmeraldPrimary)
                            } else {
                                Button(
                                    onClick = {
                                        val list = mutableListOf<String>()
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            list.add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        settingsPermLauncher.launch(list.toTypedArray())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(if (isArabic) "تفعيل" else "Grant")
                                }
                            }
                        }

                        HorizontalDivider()

                        // System Settings Button
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, if (isArabic) "افتح إعدادات الهاتف -> التطبيقات -> القرآن الكريم -> الصلاحيات" else "Open Phone Settings -> Apps -> Holy Quran -> Permissions", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "فتح إعدادات النظام للتطبيق" else "Open System App Settings")
                        }
                    }
                }
            }

            // Data Backup & Restore Section
            item {
                Text(
                    text = if (isArabic) "النسخ الاحتياطي واستعادة البيانات" else "Data Backup & Restore",
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = EmeraldPrimary)
                            Column {
                                Text(
                                    text = if (isArabic) "نسخ واستعادة كافة بيانات التطبيق" else "Backup & Restore All App Data",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isArabic)
                                        "احفظ العلامات المرجعية، تقدم القراءة، السور المفضلة، وإعدادات الأذان والتنبيهات في ملف أو كود نصي واسترجعها بسهولة"
                                    else
                                        "Save your bookmarks, reading progress, favorite surahs, and settings to a file or JSON text and restore them anytime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.exportBackupData { json ->
                                        exportedJsonText = json
                                        val fileName = "Quran_Backup_${java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())}.json"
                                        createDocumentLauncher.launch(fileName)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "تصدير ملف" else "Export File")
                            }

                            Button(
                                onClick = {
                                    openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGold)
                            ) {
                                Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "استعادة ملف" else "Import File")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.exportBackupData { json ->
                                        exportedJsonText = json
                                        showExportBackupDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "نسخ النص" else "Copy Text")
                            }

                            OutlinedButton(
                                onClick = {
                                    showImportBackupDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "لصق واستعادة" else "Paste & Import")
                            }
                        }
                    }
                }
            }

            // Support & About Section
            item {
                Text(
                    text = if (isArabic) "حول المطور والتطوير والتواصل" else "About Developer & Feedback",
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text(if (isArabic) "مميزات التطبيق الشاملة" else "App Features Showcase", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(if (isArabic) "استعرض كافة مزايا وإمكانيات تطبيق المصحف الشريف" else "Explore all features and tools of the Quran app") },
                            leadingContent = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = IslamicGold) },
                            trailingContent = { Icon(imageVector = if (isArabic) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            modifier = Modifier.clickable { onNavigate("features") }.testTag("settings_features_item")
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(if (isArabic) "مشاركة التطبيق مع الأهل والأصدقاء" else "Share App with Family & Friends", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(if (isArabic) "شارك الأجر وكونوا سبباً في نشر المصحف الشريف" else "Share the blessing and spread the Holy Quran") },
                            leadingContent = { Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary) },
                            trailingContent = { Icon(imageVector = if (isArabic) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            modifier = Modifier.clickable {
                                try {
                                    val shareText = if (isArabic) {
                                        "الْقُرْآنُ الْكَرِيمُ - تطبيق المصحف الشريف الشامل (بدون إعلانات • صدقة جارية)\n\n" +
                                        "دعواتكم الصادقة للمطور علي إبراهيم الشيخ ووالديه وأهل بيته بالرحمة والمغفرة.\n\n" +
                                        "رابط التطبيق:\nhttps://holy-quran-app-live.web.app"
                                    } else {
                                        "The Holy Quran App - Complete Offline Quran & Prayer Times (100% Ad-Free)\n\n" +
                                        "Dedicated as Sadaqah Jariyah by Ali Ibrahim Elsheikh for his parents and household.\n\n" +
                                        "Open app:\nhttps://holy-quran-app-live.web.app"
                                    }
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, if (isArabic) "تطبيق القرآن الكريم" else "The Holy Quran App")
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, if (isArabic) "مشاركة التطبيق" else "Share App"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, if (isArabic) "تعذر فتح المشاركة" else "Cannot share", Toast.LENGTH_SHORT).show()
                                }
                            }.testTag("settings_share_item")
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(if (isArabic) "عن التطبيق والمطور (صدقة جارية)" else "About App & Developer (Sadaqah Jariyah)", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(if (isArabic) "علي إبراهيم الشيخ • صدقة جارية لكل من ساهم في إكمال هذا التطبيق ومن ساهم في نشره" else "Ali Ibrahim Elsheikh • Sadaqah Jariyah for all who contributed and shared") },
                            leadingContent = { Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = IslamicGold) },
                            trailingContent = { Icon(imageVector = if (isArabic) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            modifier = Modifier.clickable { onNavigate("donation") }.testTag("settings_donation_item")
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(if (isArabic) "إصدار التطبيق والتحقق من التحديثات" else "App Version & Check Updates", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(if (isArabic) "الإصدار الحالي: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE}) • انقر للتحقق من وجود تحديث جديد" else "Current Version: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE}) • Tap to check for updates") },
                            leadingContent = { Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = EmeraldPrimary) },
                            trailingContent = {
                                Button(
                                    onClick = {
                                        Toast.makeText(context, if (isArabic) "جاري التحقق من التحديثات..." else "Checking for updates...", Toast.LENGTH_SHORT).show()
                                        com.example.data.provider.AppUpdateManager.checkForUpdate { info ->
                                            if (info != null && info.isUpdateAvailable) {
                                                updateDialogInfo = info
                                            } else {
                                                Toast.makeText(context, if (isArabic) "تطبيق القرآن الكريم مُحدث لأحدث إصدار رسمي (v${com.example.BuildConfig.VERSION_NAME}) ✅" else "Holy Quran App is up to date (v${com.example.BuildConfig.VERSION_NAME}) ✅", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Text(if (isArabic) "تحقق الآن 🚀" else "Check Now 🚀")
                                }
                            },
                            modifier = Modifier.clickable {
                                Toast.makeText(context, if (isArabic) "جاري التحقق من التحديثات..." else "Checking for updates...", Toast.LENGTH_SHORT).show()
                                com.example.data.provider.AppUpdateManager.checkForUpdate { info ->
                                    if (info != null && info.isUpdateAvailable) {
                                        updateDialogInfo = info
                                    } else {
                                        Toast.makeText(context, if (isArabic) "تطبيق القرآن الكريم مُحدث لأحدث إصدار رسمي (v${com.example.BuildConfig.VERSION_NAME}) ✅" else "Holy Quran App is up to date (v${com.example.BuildConfig.VERSION_NAME}) ✅", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    updateDialogInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateDialogInfo = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = EmeraldPrimary
                )
            },
            title = {
                Text(
                    text = if (isArabic) "يتوفر تحديث جديد لـ تطبيق القرآن الكريم 🚀" else "New App Update Available 🚀",
                    fontFamily = TajawalFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "الإصدار الحالي: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})\nالإصدار الجديد: v${info.latestVersionName} (Build ${info.latestVersionCode})" else "Current: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})\nNew: v${info.latestVersionName} (Build ${info.latestVersionCode})",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = info.getLocalizedNotes(isArabic),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (isArabic) "اضغط على زر 'تحديث الآن' لتنزيل وتثبيت التحديث مباشرة." else "Click 'Update Now' to download and install directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.example.data.provider.AppUpdateManager.startApkDownload(context, info.apkDownloadUrl)
                        updateDialogInfo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (isArabic) "تحديث الآن 🚀" else "Update Now 🚀")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateDialogInfo = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    if (showExportBackupDialog) {
        ExportBackupDialog(
            isArabic = isArabic,
            jsonText = exportedJsonText,
            onDismiss = { showExportBackupDialog = false }
        )
    }

    if (showImportBackupDialog) {
        ImportBackupDialog(
            isArabic = isArabic,
            onImport = { json ->
                viewModel.importBackupData(json) { success ->
                    if (success) {
                        Toast.makeText(
                            context,
                            if (isArabic) "تم استعادة البيانات بنجاح!" else "Backup restored successfully!",
                            Toast.LENGTH_LONG
                        ).show()
                        showImportBackupDialog = false
                    } else {
                        Toast.makeText(
                            context,
                            if (isArabic) "تنسيق الكود غير صحيح" else "Invalid JSON backup text",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismiss = { showImportBackupDialog = false }
        )
    }

    if (showMorningTimePicker) {
        ScheduleTimePickerDialog(
            title = if (isArabic) "وقت أذكار الصباح ☀️" else "Morning Azkar Time ☀️",
            initialHour = morningHour,
            initialMinute = morningMinute,
            isArabic = isArabic,
            onDismiss = { showMorningTimePicker = false },
            onTimeSelected = { h, m ->
                morningHour = h
                morningMinute = m
                showMorningTimePicker = false
                viewModel.updateAzkarNotificationSetting(
                    enableAzkar = enableAzkarNotif,
                    enableMorning = enableMorningAzkar,
                    enableEvening = enableEveningAzkar,
                    morningHour = morningHour,
                    morningMinute = morningMinute,
                    eveningHour = eveningHour,
                    eveningMinute = eveningMinute,
                    context = context
                )
            }
        )
    }

    if (showEveningTimePicker) {
        ScheduleTimePickerDialog(
            title = if (isArabic) "وقت أذكار المساء 🌙" else "Evening Azkar Time 🌙",
            initialHour = eveningHour,
            initialMinute = eveningMinute,
            isArabic = isArabic,
            onDismiss = { showEveningTimePicker = false },
            onTimeSelected = { h, m ->
                eveningHour = h
                eveningMinute = m
                showEveningTimePicker = false
                viewModel.updateAzkarNotificationSetting(
                    enableAzkar = enableAzkarNotif,
                    enableMorning = enableMorningAzkar,
                    enableEvening = enableEveningAzkar,
                    morningHour = morningHour,
                    morningMinute = morningMinute,
                    eveningHour = eveningHour,
                    eveningMinute = eveningMinute,
                    context = context
                )
            }
        )
    }

    if (showReminderTimePicker) {
        ScheduleTimePickerDialog(
            title = if (isArabic) "وقت التذكير اليومي 📖" else "Daily Reminder Time 📖",
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            isArabic = isArabic,
            onDismiss = { showReminderTimePicker = false },
            onTimeSelected = { h, m ->
                reminderHour = h
                reminderMinute = m
                showReminderTimePicker = false
                viewModel.updateNotificationTime(h, m, context)
            }
        )
    }
}

private fun formatTime12H(hour: Int, minute: Int, isArabic: Boolean): String {
    val period = if (hour >= 12) (if (isArabic) "م" else "PM") else (if (isArabic) "ص" else "AM")
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val minStr = String.format(java.util.Locale.US, "%02d", minute)
    return "$hour12:$minStr $period"
}

@Composable
private fun TajweedLegendBadge(color: Color, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScheduleTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = String.format("%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isArabic) "الساعة" else "Hour", style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { hour = (hour - 1 + 24) % 24 }) {
                                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = String.format("%02d", hour), style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { hour = (hour + 1) % 24 }) {
                                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isArabic) "الدقيقة" else "Minute", style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { minute = (minute - 15 + 60) % 60 }) {
                                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = String.format("%02d", minute), style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { minute = (minute + 15) % 60 }) {
                                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onTimeSelected(hour, minute) }) {
                Text(if (isArabic) "تأكيد" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}
