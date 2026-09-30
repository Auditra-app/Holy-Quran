package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.viewmodel.QuranViewModel

data class FeatureItem(
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val icon: ImageVector,
    val categoryAr: String,
    val categoryEn: String,
    val badgeAr: String? = null,
    val badgeEn: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFeaturesScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val isArabic = userSettings.languageCode == "ar"

    val featuresList = remember {
        listOf(
            FeatureItem(
                titleAr = "المصحف الشريف كاملاً (604 صفحة)",
                titleEn = "Full Holy Quran (604 Pages)",
                descAr = "عرض الصفحات بالرسم العثماني المطابق لمصحف المدينة المنورة مع علامات التجويد وحفظ الصفحة التلقائي.",
                descEn = "Authentic Uthmani script matching the Madinah Mushaf with auto-save and Tajweed guidance.",
                icon = Icons.Default.MenuBook,
                categoryAr = "القرآن الكريم",
                categoryEn = "Holy Quran",
                badgeAr = "أساسي",
                badgeEn = "Core"
            ),
            FeatureItem(
                titleAr = "التلاوة والاستماع لأشهر القراء",
                titleEn = "Audio Recitations by Top Qaris",
                descAr = "استمع للقرآن بصوت الحصري، عبد الباسط، المنشاوي، العفاسي وغيرهم مع المتابعة كلمة بكلمة.",
                descEn = "Listen to world-renowned reciters with verse-by-verse sync and word-by-word pronunciation.",
                icon = Icons.Default.VolumeUp,
                categoryAr = "الصوتيات",
                categoryEn = "Audio",
                badgeAr = "صوتي",
                badgeEn = "Audio"
            ),
            FeatureItem(
                titleAr = "التفسير الميسر والتراجم العالمية",
                titleEn = "Tafsir & World Translations",
                descAr = "عرض التفسير الميسر وابن كثير مع ترجمة المعاني للإنجليزية والفرنسية ولغات أخرى.",
                descEn = "In-depth Tafsir Al-Muyassar, Ibn Kathir, and multi-language translations.",
                icon = Icons.Default.AutoStories,
                categoryAr = "التفسير والدراسات",
                categoryEn = "Tafsir & Study"
            ),
            FeatureItem(
                titleAr = "مواقيت الصلاة والتنبيهات المباركة",
                titleEn = "Precise Prayer Times & Alerts",
                descAr = "حساب دقيق لمواقيت الصلاة بحسب موقعك مع تنبيهات مخصصة عند دخول وقت الصلاة.",
                descEn = "Astronomically accurate prayer times based on location with customizable prayer notifications.",
                icon = Icons.Default.AccessTime,
                categoryAr = "العبادات",
                categoryEn = "Worship",
                badgeAr = "دقيق",
                badgeEn = "Accurate"
            ),
            FeatureItem(
                titleAr = "اتجاه القبلة الشريفة",
                titleEn = "Digital Qibla Compass",
                descAr = "بوصلة رقمية تفاعلية فائقة الدقة لتحديد اتجاه الكعبة المشرفة من أي مكان بالعالم.",
                descEn = "Interactive High-Precision Compass pointing directly to the Holy Kaaba.",
                icon = Icons.Default.Explore,
                categoryAr = "العبادات",
                categoryEn = "Worship"
            ),
            FeatureItem(
                titleAr = "الأذكار والمسبحة الرقمية الذكية",
                titleEn = "Azkar & Smart Digital Tasbeeh",
                descAr = "أذكار الصباح والمساء والصلاة مع مسبحة ذكية مدعومة بالإحصائيات والعداد.",
                descEn = "Morning, Evening & Prayer Azkar with digital counter and statistics.",
                icon = Icons.Default.TouchApp,
                categoryAr = "الأذكار",
                categoryEn = "Azkar"
            ),
            FeatureItem(
                titleAr = "آية اليوم ومتابعة الورد والختمة",
                titleEn = "Daily Verse & Habit Tracker",
                descAr = "إشعارات يومية متجددة لتدبر القرآن مع تحديد الأهداف ومتابعة ختمات المصحف الشريف.",
                descEn = "Daily contemplation notifications, reading goals, and Khatmah completion progress.",
                icon = Icons.Default.EventAvailable,
                categoryAr = "التطوير",
                categoryEn = "Habits"
            ),
            FeatureItem(
                titleAr = "الختمة الجماعية المشتركة",
                titleEn = "Group Collaborative Khatma",
                descAr = "إنشاء ومشاركة ختمات قرآنية جماعية عبر روابط وأكواد. لحجز الأجزاء ومتابعة من أتم قراءته بين الأهل والأصدقاء.",
                descEn = "Create and share collaborative group Khatmas via codes/links. Reserve Ajzaa and track live progress among family and friends.",
                icon = Icons.Default.Groups,
                categoryAr = "مشاركة الأجر",
                categoryEn = "Community",
                badgeAr = "جديد ✨",
                badgeEn = "New ✨"
            ),
            FeatureItem(
                titleAr = "البحث الفوري الشامل",
                titleEn = "Instant Smart Search",
                descAr = "بحث فوري بالنص أو الكلمة أو السورة أو الجزء مع نتائج فائقة السرعة.",
                descEn = "Search instantly by verse text, surah name, juz number, or page.",
                icon = Icons.Default.Search,
                categoryAr = "الأدوات",
                categoryEn = "Tools"
            ),
            FeatureItem(
                titleAr = "تطبيق بدون إعلانات • صدقة جارية",
                titleEn = "100% Ad-Free • Sadaqah Jariyah",
                descAr = "تطبيق خالص لوجه الله تعالى بدون أي إعلانات، صدقة جارية عن المطور علي إبراهيم الشيخ ووالديه وأهل بيته، ولكل من ساهم في إكمال هذا التطبيق ومن ساهم في نشره.",
                descEn = "Completely free & ad-free app, built as an ongoing charity (Sadaqah Jariyah) by Ali Ibrahim Elsheikh for his parents & household, and for everyone who contributed to completing and sharing this app.",
                icon = Icons.Default.Favorite,
                categoryAr = "إهداء",
                categoryEn = "Pledge",
                badgeAr = "مجاني 100%",
                badgeEn = "Free"
            )
        )
    }

    fun shareApp() {
        try {
            val shareText = if (isArabic) {
                "الْقُرْآنُ الْكَرِيمُ - تطبيق المصحف الشريف الشامل (بدون إعلانات • صدقة جارية)\n\n" +
                "رابط التطبيق:\nhttps://holyquranapp.web.app"
            } else {
                "The Holy Quran App - Complete Offline Quran, Prayer Times & Azkar (100% Ad-Free • Sadaqah Jariyah)\n\n" +
                "Download/Open here:\nhttps://holyquranapp.web.app"
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, if (isArabic) "تطبيق القرآن الكريم الشامل" else "The Holy Quran App")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, if (isArabic) "مشاركة التطبيق عبر" else "Share App via"))
        } catch (e: Exception) {
            Toast.makeText(context, if (isArabic) "تعذر فتح نافذة المشاركة" else "Cannot open share window", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "مميزات التطبيق الشاملة" else "App Features",
                        fontFamily = TajawalFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("features_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { shareApp() }, modifier = Modifier.testTag("features_share_btn")) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share App", tint = EmeraldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // Hero Banner Header
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F382C),
                                        Color(0xFF1E5242)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(1.dp, IslamicGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(IslamicGold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = IslamicGold,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "دليلك لمميزات تطبيق المصحف" else "Comprehensive Guide to App Features",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IslamicGold
                                    )
                                    Text(
                                        text = if (isArabic) "مصمم بعناية ليجمع كل ما تحتاجه في يومك الإيماني" else "Crafted with love for your daily spiritual journey",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { shareApp() },
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("hero_share_app_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF0F382C))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "مشاركة التطبيق مع الأهل والأصدقاء 🤲" else "Share App with Family & Friends 🤲",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F382C)
                                )
                            }
                        }
                    }
                }
            }

            // Feature List Items
            items(featuresList) { feature ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) feature.titleAr else feature.titleEn,
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                val badgeText = if (isArabic) feature.badgeAr else feature.badgeEn
                                if (badgeText != null) {
                                    Surface(
                                        color = EmeraldPrimary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = TajawalFontFamily),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (isArabic) feature.descAr else feature.descEn,
                                fontFamily = TajawalFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // Sadaqah Jariyah Pledge Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldPrimary.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) "صدقة جارية • إهداء وتضرع" else "Sadaqah Jariyah",
                            fontFamily = TajawalFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic)
                                "هذا التطبيق مجاني بالكامل وبدون إعلانات، نلتمس منكم الدعاء بظهر الغيب للمطور علي إبراهيم الشيخ ووالديه وأهل بيته."
                            else
                                "This app is completely free & ad-free, please remember Ali Ibrahim Elsheikh, his parents & household in your prayers.",
                            fontFamily = TajawalFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
