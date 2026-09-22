package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object TajweedHelper {

    // Standard Tajweed Rule Color Palette
    val ColorMadd = Color(0xFFDC2626)       // Red for Madd (Prolongation / Elongation)
    val ColorGhunnah = Color(0xFF059669)    // Green for Ghunnah (Noon & Meem Muschaddadah)
    val ColorQalqalah = Color(0xFF2563EB)   // Blue for Qalqalah (Echoing consonants: ق ط ب ج د)
    val ColorIkhfa = Color(0xFF7C3AED)      // Purple for Ikhfa / Idgham / Iqlab
    val ColorWasl = Color(0xFF0D9488)       // Teal/Muted for Hamzat Al-Wasl / Silent letters

    private val qalqalahLetters = setOf('ق', 'ط', 'ب', 'ج', 'د')
    private val ikhfaLetters = setOf('ت', 'ث', 'ج', 'د', 'ذ', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ف', 'ق', 'ك')
    private val idghamLetters = setOf('ي', 'ر', 'م', 'ل', 'و', 'ن')

    /**
     * Determines the primary Tajweed rule color for an entire word.
     * Keeping style spans at the word level guarantees that Arabic character ligatures
     * and Uthmani script shaping remain 100% connected and unbroken.
     */
    fun getTajweedColorForWord(word: String, isDarkMode: Boolean): Color? {
        if (word.isBlank()) return null

        // 1. Madd (Prolongation symbols: ٓ, ۤ, ۦ, ۥ, ٰ)
        if (word.contains('ٓ') || word.contains('ۤ') || word.contains('ۦ') || word.contains('ۥ') || word.contains('ٰ')) {
            return if (isDarkMode) Color(0xFFFF8A80) else ColorMadd
        }

        // 2. Ghunnah (Noon or Meem with Shaddah: نّ or مّ)
        if (word.contains("نّ") || word.contains("مّ")) {
            return if (isDarkMode) Color(0xFF69F0AE) else ColorGhunnah
        }

        // 3. Qalqalah (ق, ط, ب, ج, د with Sukun or at word boundary)
        for (i in word.indices) {
            val char = word[i]
            if (char in qalqalahLetters) {
                val nextChar = if (i + 1 < word.length) word[i + 1] else ' '
                if (nextChar == 'ْ' || nextChar == 'ۡ' || i == word.length - 1) {
                    return if (isDarkMode) Color(0xFF82B1FF) else ColorQalqalah
                }
            }
        }

        // 4. Ikhfa / Idgham / Tanween / Noon Sakinah
        if (word.contains('ً') || word.contains('ٍ') || word.contains('ٌ') || word.contains('ۨ') || word.contains('ۣ')) {
            return if (isDarkMode) Color(0xFFD1C4E9) else ColorIkhfa
        }
        for (i in word.indices) {
            if (word[i] == 'ن') {
                val nextChar = if (i + 1 < word.length) word[i + 1] else ' '
                if (nextChar == 'ْ' || nextChar == 'ۡ' || nextChar in ikhfaLetters || nextChar in idghamLetters) {
                    return if (isDarkMode) Color(0xFFD1C4E9) else ColorIkhfa
                }
            }
        }

        // 5. Hamzat Al-Wasl
        if (word.startsWith('ٱ')) {
            return if (isDarkMode) Color(0xFF80CBC4) else ColorWasl
        }

        return null
    }

    /**
     * Builds an AnnotatedString formatted with Tajweed colors while strictly maintaining
     * word-level text runs to preserve flawless Arabic font ligatures.
     */
    fun buildTajweedAnnotatedString(
        text: String,
        enableTajweed: Boolean = true,
        baseColor: Color = Color.Unspecified,
        isDarkMode: Boolean = false,
        isBold: Boolean = false
    ): AnnotatedString {
        if (!enableTajweed || text.isBlank()) {
            return buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = if (baseColor != Color.Unspecified) baseColor else Color.Unspecified,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
                    )
                ) {
                    append(text)
                }
            }
        }

        return buildAnnotatedString {
            val tokens = text.split(Regex("(?<=\\s)|(?=\\s)"))
            tokens.forEach { token ->
                if (token.isBlank()) {
                    append(token)
                } else {
                    val tajweedColor = getTajweedColorForWord(token, isDarkMode) ?: baseColor
                    withStyle(
                        SpanStyle(
                            color = if (tajweedColor != Color.Unspecified) tajweedColor else Color.Unspecified,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
                        )
                    ) {
                        append(token)
                    }
                }
            }
        }
    }
}

