package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Google Tajawal Arabic Font Family (UI & Modern Arabic)
val TajawalFontFamily: FontFamily = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold)
)

// Amiri Font Family for Classical Arabic & Naskh
val AmiriFontFamily: FontFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)

// Amiri Quran Specialized Font Family
val AmiriQuranFontFamily: FontFamily = FontFamily(
    Font(R.font.amiri_quran, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)

// Scheherazade / Madinah Font Family for Quran Verses
val ScheherazadeFontFamily: FontFamily = FontFamily(
    Font(R.font.scheherazade_regular, FontWeight.Normal),
    Font(R.font.scheherazade_bold, FontWeight.Bold)
)

enum class QuranScript(
    val id: String,
    val titleArabic: String,
    val titleEnglish: String,
    val subtitleArabic: String,
    val subtitleEnglish: String,
    val descriptionArabic: String,
    val descriptionEnglish: String,
    val sampleVerse: String = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ۝ ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ"
) {
    MADINAH(
        id = "MADINAH",
        titleArabic = "مصحف المدينة المنورة (الرسم العثماني)",
        titleEnglish = "Madinah Mushaf (Uthmani Script)",
        subtitleArabic = "الرسم العثماني المعتمد لمجمع الملك فهد",
        subtitleEnglish = "King Fahd Complex Uthmani Script",
        descriptionArabic = "خط النسخ العثماني المتقن المعتمد في طباعة مصاحف الحرمين الشريفين والمدينة المنورة.",
        descriptionEnglish = "Authentic King Fahd Quran Complex Madinah script with precise Uthmani markings."
    ),
    AMIRI_QURAN(
        id = "AMIRI_QURAN",
        titleArabic = "المصحف الأميري الشريف",
        titleEnglish = "Amiri Quranic Script",
        subtitleArabic = "رسم المصحف الأميري التاريخي الموثق",
        subtitleEnglish = "Classical Egyptian Amiri Typeface",
        descriptionArabic = "الخط الأميري القرآني العريق المتميز بجمال الحروف ورسوخ الرسم القرآني الكلاسيكي.",
        descriptionEnglish = "Renowned classical Arabic typeface modeled after historic high-quality Quran prints."
    ),
    AMIRI_NASKH(
        id = "AMIRI_NASKH",
        titleArabic = "خط النسخ العربي الأصيل",
        titleEnglish = "Amiri Classical Naskh",
        subtitleArabic = "خط نسخ واضح ومريح للعين",
        subtitleEnglish = "Traditional Clear Naskh Script",
        descriptionArabic = "رسم نسخي عربي سلس ومألوف، يتميز بالوضوح التام للتشكيل وعلامات الوقف.",
        descriptionEnglish = "Clear, comfortable traditional Naskh styling with crisp diacritic markings."
    ),
    TAJAWAL(
        id = "TAJAWAL",
        titleArabic = "خط تجوال الحديث الميسر",
        titleEnglish = "Tajawal Modern Font",
        subtitleArabic = "خط مبسط وعصري للقراءة",
        subtitleEnglish = "Simplified Modern Typography",
        descriptionArabic = "خط تجوال عالي التباين والوضوح مصمم لأعلى درجات المقروئية والراحة البصرية.",
        descriptionEnglish = "Modern clean Tajawal font engineered for optimal digital screen readability and accessibility."
    );

    fun getFontFamily(): FontFamily {
        return when (this) {
            MADINAH -> ScheherazadeFontFamily
            AMIRI_QURAN -> AmiriQuranFontFamily
            AMIRI_NASKH -> AmiriFontFamily
            TAJAWAL -> TajawalFontFamily
        }
    }

    companion object {
        fun fromId(id: String?): QuranScript {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MADINAH
        }
    }
}

fun createAppTypography(): Typography {
    val tajawal = TajawalFontFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 42.sp,
            letterSpacing = 0.sp
        ),
        displayMedium = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = tajawal,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

val Typography: Typography
    get() = createAppTypography()


