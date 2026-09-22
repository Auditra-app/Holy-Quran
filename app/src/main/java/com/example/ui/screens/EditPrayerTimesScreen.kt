package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.viewmodel.QuranViewModel

data class PrayerAdjustmentItem(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val currentOffset: Int
)

data class CalculationMethodInfo(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val description: String
)

val GOLDEN_CALCULATION_METHODS = listOf(
    CalculationMethodInfo("UMM_AL_QURA", "أم القرى - مكة المكرمة", "Umm Al-Qura University, Makkah", "المملكة العربية السعودية والخليج العربي"),
    CalculationMethodInfo("EGYPT", "الهيئة المصرية العامة للمساحة", "Egyptian General Survey Authority", "مصر، إفريقيا، والشرق الأوسط"),
    CalculationMethodInfo("MWL", "رابطة العالم الإسلامي", "Muslim World League", "أوروبا، الشرق الأقصى، وأجزاء من أمريكا"),
    CalculationMethodInfo("KARACHI", "جامعة العلوم الإسلامية بكراتشي", "University of Islamic Sciences, Karachi", "باكستان، الهند، بنغلاديش، وأفغانستان"),
    CalculationMethodInfo("ISNA", "الجمعية الإسلامية لأمريكا الشمالية", "Islamic Society of North America (ISNA)", "الولايات المتحدة الأمريكية وكندا"),
    CalculationMethodInfo("TEHRAN", "معهد الجيوفيزياء بجامعة طهران", "Institute of Geophysics, Tehran", "إيران وبعض دول الجوار"),
    CalculationMethodInfo("FRANCE", "مجلس الديانة الإسلامية بفرنسا (UOIF)", "Union of Islamic Organizations of France", "فرنسا وغرب أوروبا"),
    CalculationMethodInfo("DUBAI", "دائرة الشؤون الإسلامية بدبي", "Dubai Islamic Affairs", "دولة الإمارات العربية المتحدة"),
    CalculationMethodInfo("SINGAPORE", "المجلس الإسلامي بسنغافورة (MUIS)", "Majlis Ugama Islam Singapura", "سنغافورة وماليزيا")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPrayerTimesScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    var fajrOffset by remember(userSettings.fajrOffsetMins) { mutableIntStateOf(userSettings.fajrOffsetMins) }
    var dhuhrOffset by remember(userSettings.dhuhrOffsetMins) { mutableIntStateOf(userSettings.dhuhrOffsetMins) }
    var asrOffset by remember(userSettings.asrOffsetMins) { mutableIntStateOf(userSettings.asrOffsetMins) }
    var maghribOffset by remember(userSettings.maghribOffsetMins) { mutableIntStateOf(userSettings.maghribOffsetMins) }
    var ishaOffset by remember(userSettings.ishaOffsetMins) { mutableIntStateOf(userSettings.ishaOffsetMins) }

    var selectedMethod by remember(userSettings.calculationMethod) { mutableStateOf(userSettings.calculationMethod) }
    var selectedMadhhab by remember(userSettings.madhhab) { mutableStateOf(userSettings.madhhab) }
    var isSummerTime by remember(userSettings.isSummerTime) { mutableStateOf(userSettings.isSummerTime) }
    var takbeerOnly by remember(userSettings.takbeerAzanOnly) { mutableStateOf(userSettings.takbeerAzanOnly) }
    var selectedAzanSound by remember(userSettings.selectedAzanSound) { mutableStateOf(userSettings.selectedAzanSound) }

    var showMethodDialog by remember { mutableStateOf(false) }
    var showMadhhabDialog by remember { mutableStateOf(false) }
    var showAzanSoundDialog by remember { mutableStateOf(false) }
    var activeOffsetPickerPrayer by remember { mutableStateOf<String?>(null) }

    val prayerList = listOf(
        PrayerAdjustmentItem("fajr", "الفجر", "Fajr", fajrOffset),
        PrayerAdjustmentItem("dhuhr", "الظهر", "Dhuhr", dhuhrOffset),
        PrayerAdjustmentItem("asr", "العصر", "Asr", asrOffset),
        PrayerAdjustmentItem("maghrib", "المغرب", "Maghrib", maghribOffset),
        PrayerAdjustmentItem("isha", "العشاء", "Isha", ishaOffset)
    )

    fun saveOffsets(f: Int, d: Int, a: Int, m: Int, i: Int) {
        fajrOffset = f
        dhuhrOffset = d
        asrOffset = a
        maghribOffset = m
        ishaOffset = i
        viewModel.updatePrayerOffsets(
            fajr = f,
            dhuhr = d,
            asr = a,
            maghrib = m,
            isha = i,
            context = context
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val currentMethodInfo = remember(selectedMethod) {
        GOLDEN_CALCULATION_METHODS.firstOrNull { it.id.equals(selectedMethod, ignoreCase = true) }
            ?: GOLDEN_CALCULATION_METHODS.first()
    }

    CompositionLocalProvider(LocalLayoutDirection provides (if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isArabic) "ضبط وتعديل مواعيد الصلاة" else "Prayer Times Adjustment",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = TajawalFontFamily,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("edit_prayer_times_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                fajrOffset = 0
                                dhuhrOffset = 0
                                asrOffset = 0
                                maghribOffset = 0
                                ishaOffset = 0
                                selectedMethod = "EGYPT"
                                selectedMadhhab = "Shafi"
                                isSummerTime = false
                                viewModel.resetPrayerSettings(context)
                                Toast.makeText(context, if (isArabic) "تمت إعادة ضبط المواعيد إلى الوضع الافتراضي" else "Reset to default settings", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = if (isArabic) "إعادة ضبط" else "Reset",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryColor,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            containerColor = bgColor
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(bgColor),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Info Banner
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isArabic) "طريقة المصحف الذهبي لضبط المواقيت" else "Golden Quran Prayer Adjustment",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isArabic) "يمكنك اختيار طريقة الحساب الفلكية المعتمدة لمدينتك، والمذهب الفقهي، وتعديل الدقائق يدوياً لكل صلاة." else "Select official calculation method, juristic madhhab, and adjust minutes for exact local mosque match.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                // Section 1: Calculation Method & Juristic Madhhab
                item {
                    Text(
                        text = if (isArabic) "طريقة الحساب والمذهب الفقهي" else "Calculation Method & Madhhab",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Method row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMethodDialog = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "طريقة حساب المواقيت" else "Calculation Method",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = onSurfaceColor
                                    )
                                    Text(
                                        text = if (isArabic) currentMethodInfo.nameArabic else currentMethodInfo.nameEnglish,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = currentMethodInfo.description,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                        color = onSurfaceVariantColor
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = onSurfaceVariantColor
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Madhhab row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMadhhabDialog = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "طريقة حساب صلاة العصر (المذهب الفقهي)" else "Asr Juristic Calculation",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = onSurfaceColor
                                    )
                                    Text(
                                        text = if (selectedMadhhab == "Hanafi") {
                                            if (isArabic) "المذهب الحنفي (ظل الشيء مثليه)" else "Hanafi (Shadow twice the length)"
                                        } else {
                                            if (isArabic) "الجمهور: الشافعي، المالكي، الحنبلي (ظل الشيء مثله)" else "Standard: Shafi'i, Maliki, Hanbali (Shadow equal length)"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = onSurfaceVariantColor
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Daylight Saving Time (DST)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "التوقيت الصيفي (+1 ساعة)" else "Daylight Saving Time (+1 hr)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = onSurfaceColor
                                    )
                                    Text(
                                        text = if (isArabic) "إضافة ساعة واحدة تلقائياً لمواقيت الصلاة" else "Adds 1 hour to calculated prayer times",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                        color = onSurfaceVariantColor
                                    )
                                }
                                Switch(
                                    checked = isSummerTime,
                                    onCheckedChange = { checked ->
                                        isSummerTime = checked
                                        viewModel.updateCalculationSettings(
                                            method = selectedMethod,
                                            isSummer = checked,
                                            context = context
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Section 2: Manual Minute Adjustments
                item {
                    Text(
                        text = if (isArabic) "تعديل دقائق الصلوات يدويًا (التقديم والتأخير)" else "Manual Prayer Minute Adjustments",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                items(prayerList) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Prayer Name
                            Column {
                                Text(
                                    text = if (isArabic) item.nameArabic else item.nameEnglish,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TajawalFontFamily,
                                    color = primaryColor
                                )
                                Text(
                                    text = when {
                                        item.currentOffset > 0 -> if (isArabic) "تقديم +${item.currentOffset} دقيقة" else "+${item.currentOffset} mins"
                                        item.currentOffset < 0 -> if (isArabic) "تأخير ${item.currentOffset} دقيقة" else "${item.currentOffset} mins"
                                        else -> if (isArabic) "حسب التوقيت الفلكي الدقيق" else "Exact astronomical time"
                                    },
                                    fontSize = 12.sp,
                                    fontFamily = TajawalFontFamily,
                                    color = if (item.currentOffset != 0) IslamicGold else onSurfaceVariantColor
                                )
                            }

                            // Stepper Controls (- / value / +)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Decrease Button (-)
                                FilledIconButton(
                                    onClick = {
                                        val newOffset = (item.currentOffset - 1).coerceIn(-30, 30)
                                        when (item.id) {
                                            "fajr" -> saveOffsets(newOffset, dhuhrOffset, asrOffset, maghribOffset, ishaOffset)
                                            "dhuhr" -> saveOffsets(fajrOffset, newOffset, asrOffset, maghribOffset, ishaOffset)
                                            "asr" -> saveOffsets(fajrOffset, dhuhrOffset, newOffset, maghribOffset, ishaOffset)
                                            "maghrib" -> saveOffsets(fajrOffset, dhuhrOffset, asrOffset, newOffset, ishaOffset)
                                            "isha" -> saveOffsets(fajrOffset, dhuhrOffset, asrOffset, maghribOffset, newOffset)
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = onSurfaceColor
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                                }

                                // Offset Value Badge (Clickable to open dialog)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (item.currentOffset != 0) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (item.currentOffset != 0) EmeraldPrimary else Color.Transparent),
                                    modifier = Modifier
                                        .clickable { activeOffsetPickerPrayer = item.id }
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "${if (item.currentOffset > 0) "+" else ""}${item.currentOffset} د",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (item.currentOffset != 0) EmeraldPrimary else onSurfaceColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                // Increase Button (+)
                                FilledIconButton(
                                    onClick = {
                                        val newOffset = (item.currentOffset + 1).coerceIn(-30, 30)
                                        when (item.id) {
                                            "fajr" -> saveOffsets(newOffset, dhuhrOffset, asrOffset, maghribOffset, ishaOffset)
                                            "dhuhr" -> saveOffsets(fajrOffset, newOffset, asrOffset, maghribOffset, ishaOffset)
                                            "asr" -> saveOffsets(fajrOffset, dhuhrOffset, newOffset, maghribOffset, ishaOffset)
                                            "maghrib" -> saveOffsets(fajrOffset, dhuhrOffset, asrOffset, newOffset, ishaOffset)
                                            "isha" -> saveOffsets(fajrOffset, dhuhrOffset, asrOffset, maghribOffset, newOffset)
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Section 3: Azan Sound and Alerts
                item {
                    Text(
                        text = if (isArabic) "التنبيهات وصوت الأذان" else "Azan & Alerts Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Azan Sound Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAzanSoundDialog = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "صوت الأذان المختار" else "Selected Azan Sound",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = onSurfaceColor
                                    )
                                    Text(
                                        text = when (selectedAzanSound) {
                                            "alafasy_2" -> if (isArabic) "الشيخ مشاري راشد العفاسي (2)" else "Sheikh Mishary Alafasy (2)"
                                            "alafasy_3" -> if (isArabic) "الشيخ مشاري راشد العفاسي (فجر 1)" else "Sheikh Mishary Alafasy (Fajr 1)"
                                            "alafasy_4" -> if (isArabic) "الشيخ مشاري راشد العفاسي (4)" else "Sheikh Mishary Alafasy (4)"
                                            "alafasy_5" -> if (isArabic) "الشيخ مشاري راشد العفاسي (فجر 2)" else "Sheikh Mishary Alafasy (Fajr 2)"
                                            "qatami" -> if (isArabic) "الشيخ ناصر القطامي" else "Sheikh Nasser Al-Qatami"
                                            "makkah" -> if (isArabic) "أذان الحرم المكي الشريف" else "Makkah Haram Azan"
                                            "madinah" -> if (isArabic) "أذان المسجد النبوي الشريف" else "Madinah Nabawi Azan"
                                            "alaqsa" -> if (isArabic) "أذان المسجد الأقصى المبارك" else "Al-Aqsa Mosque Azan"
                                            "takbeer" -> if (isArabic) "تكبيرات فقط" else "Takbeer Only"
                                            "silent_tone" -> if (isArabic) "تنبيه هادئ (بدون أذان)" else "Gentle Chime"
                                            else -> if (isArabic) "الشيخ مشاري راشد العفاسي (1)" else "Sheikh Mishary Alafasy (1)"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = onSurfaceVariantColor
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Takbeer Only Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "التكبيرات فقط (الله أكبر الله أكبر)" else "Takbeer Only",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = TajawalFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = onSurfaceColor
                                    )
                                    Text(
                                        text = if (isArabic) "اقتصار التنبيه على تكبيرات الأذان الأولى" else "Play only the initial takbeers for notifications",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                        color = onSurfaceVariantColor
                                    )
                                }
                                Switch(
                                    checked = takbeerOnly,
                                    onCheckedChange = { checked ->
                                        takbeerOnly = checked
                                        viewModel.updateAzanSettings(
                                            takbeerOnly = checked,
                                            sound = selectedAzanSound
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Select Calculation Method
    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            title = {
                Text(
                    text = if (isArabic) "اختر طريقة حساب المواقيت" else "Select Calculation Method",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(modifier = Modifier.height(380.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GOLDEN_CALCULATION_METHODS) { method ->
                            val isSelected = method.id.equals(selectedMethod, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, EmeraldPrimary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMethod = method.id
                                        viewModel.updateCalculationSettings(
                                            method = method.id,
                                            isSummer = isSummerTime,
                                            context = context
                                        )
                                        showMethodDialog = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (isArabic) method.nameArabic else method.nameEnglish,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) EmeraldPrimary else onSurfaceColor
                                    )
                                    Text(
                                        text = method.description,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                        color = onSurfaceVariantColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMethodDialog = false }) {
                    Text(if (isArabic) "إغلاق" else "Close", style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily))
                }
            }
        )
    }

    // Dialog: Select Madhhab
    if (showMadhhabDialog) {
        AlertDialog(
            onDismissRequest = { showMadhhabDialog = false },
            title = {
                Text(
                    text = if (isArabic) "طريقة حساب صلاة العصر" else "Asr Juristic Calculation",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMadhhab == "Shafi") EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        border = if (selectedMadhhab == "Shafi") BorderStroke(1.dp, EmeraldPrimary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMadhhab = "Shafi"
                                viewModel.updateMadhhab("Shafi")
                                showMadhhabDialog = false
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isArabic) "الجمهور (الشافعي، المالكي، الحنبلي)" else "Standard (Shafi'i, Maliki, Hanbali)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMadhhab == "Shafi") EmeraldPrimary else onSurfaceColor
                            )
                            Text(
                                text = if (isArabic) "يدخل وقت العصر إذا صار ظل كل شيء مثله" else "Asr starts when the shadow of an object equals its length",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                color = onSurfaceVariantColor
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMadhhab == "Hanafi") EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        border = if (selectedMadhhab == "Hanafi") BorderStroke(1.dp, EmeraldPrimary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMadhhab = "Hanafi"
                                viewModel.updateMadhhab("Hanafi")
                                showMadhhabDialog = false
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isArabic) "المذهب الحنفي" else "Hanafi School",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMadhhab == "Hanafi") EmeraldPrimary else onSurfaceColor
                            )
                            Text(
                                text = if (isArabic) "يدخل وقت العصر إذا صار ظل كل شيء مثليه" else "Asr starts when the shadow of an object is twice its length",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = TajawalFontFamily),
                                color = onSurfaceVariantColor
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMadhhabDialog = false }) {
                    Text(if (isArabic) "إغلاق" else "Close", style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily))
                }
            }
        )
    }

    // Dialog: Select Azan Sound
    if (showAzanSoundDialog) {
        val azanSounds = listOf(
            "alafasy" to (if (isArabic) "الشيخ مشاري راشد العفاسي (1)" else "Sheikh Mishary Alafasy (1)"),
            "alafasy_2" to (if (isArabic) "الشيخ مشاري راشد العفاسي (2)" else "Sheikh Mishary Alafasy (2)"),
            "alafasy_3" to (if (isArabic) "الشيخ مشاري راشد العفاسي (فجر 1)" else "Sheikh Mishary Alafasy (Fajr 1)"),
            "alafasy_4" to (if (isArabic) "الشيخ مشاري راشد العفاسي (4)" else "Sheikh Mishary Alafasy (4)"),
            "alafasy_5" to (if (isArabic) "الشيخ مشاري راشد العفاسي (فجر 2)" else "Sheikh Mishary Alafasy (Fajr 2)"),
            "qatami" to (if (isArabic) "الشيخ ناصر القطامي" else "Sheikh Nasser Al-Qatami"),
            "makkah" to (if (isArabic) "أذان الحرم المكي الشريف" else "Makkah Haram Azan"),
            "madinah" to (if (isArabic) "أذان المسجد النبوي الشريف" else "Madinah Nabawi Azan"),
            "alaqsa" to (if (isArabic) "أذان المسجد الأقصى المبارك" else "Al-Aqsa Mosque Azan"),
            "takbeer" to (if (isArabic) "تكبيرات فقط" else "Takbeer Only"),
            "silent_tone" to (if (isArabic) "تنبيه هادئ (بدون أذان)" else "Gentle Chime")
        )

        AlertDialog(
            onDismissRequest = { showAzanSoundDialog = false },
            title = {
                Text(
                    text = if (isArabic) "اختر صوت الأذان والتنبيه" else "Select Azan Voice",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(azanSounds) { (soundId, soundName) ->
                        val isSelected = selectedAzanSound == soundId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, EmeraldPrimary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAzanSound = soundId
                                    viewModel.updateAzanSettings(
                                        takbeerOnly = takbeerOnly,
                                        sound = soundId
                                    )
                                    showAzanSoundDialog = false
                                }
                        ) {
                            Text(
                                text = soundName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) EmeraldPrimary else onSurfaceColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAzanSoundDialog = false }) {
                    Text(if (isArabic) "إغلاق" else "Close", style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily))
                }
            }
        )
    }

    // Dialog: Quick Offset Picker (-30 to +30)
    if (activeOffsetPickerPrayer != null) {
        val prayerId = activeOffsetPickerPrayer!!
        val prayerName = when (prayerId) {
            "fajr" -> if (isArabic) "الفجر" else "Fajr"
            "dhuhr" -> if (isArabic) "الظهر" else "Dhuhr"
            "asr" -> if (isArabic) "العصر" else "Asr"
            "maghrib" -> if (isArabic) "المغرب" else "Maghrib"
            else -> if (isArabic) "العشاء" else "Isha"
        }
        val currentVal = when (prayerId) {
            "fajr" -> fajrOffset
            "dhuhr" -> dhuhrOffset
            "asr" -> asrOffset
            "maghrib" -> maghribOffset
            else -> ishaOffset
        }

        AlertDialog(
            onDismissRequest = { activeOffsetPickerPrayer = null },
            title = {
                Text(
                    text = if (isArabic) "تعديل دقائق صلاة $prayerName" else "Adjust $prayerName Minutes",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = TajawalFontFamily),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(modifier = Modifier.height(300.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items((-30..30).toList()) { mins ->
                            val isSelected = mins == currentVal
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (prayerId) {
                                            "fajr" -> saveOffsets(mins, dhuhrOffset, asrOffset, maghribOffset, ishaOffset)
                                            "dhuhr" -> saveOffsets(fajrOffset, mins, asrOffset, maghribOffset, ishaOffset)
                                            "asr" -> saveOffsets(fajrOffset, dhuhrOffset, mins, maghribOffset, ishaOffset)
                                            "maghrib" -> saveOffsets(fajrOffset, dhuhrOffset, asrOffset, mins, ishaOffset)
                                            "isha" -> saveOffsets(fajrOffset, dhuhrOffset, asrOffset, maghribOffset, mins)
                                        }
                                        activeOffsetPickerPrayer = null
                                    }
                            ) {
                                Text(
                                    text = when {
                                        mins > 0 -> if (isArabic) "+$mins دقيقة (تقديم)" else "+$mins mins"
                                        mins < 0 -> if (isArabic) "$mins دقيقة (تأخير)" else "$mins mins"
                                        else -> if (isArabic) "0 دقيقة (التوقيت الفلكي الأصلي)" else "0 mins (Standard)"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TajawalFontFamily),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldPrimary else onSurfaceColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeOffsetPickerPrayer = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", style = MaterialTheme.typography.labelLarge.copy(fontFamily = TajawalFontFamily))
                }
            }
        )
    }
}
