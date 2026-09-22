package com.example.util

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

object HijriCalendarHelper {

    private val ARABIC_HIJRI_MONTHS = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    private val ENGLISH_HIJRI_MONTHS = listOf(
        "Muḥarram", "Ṣafar", "Rabīʿ al-Awwal", "Rabīʿ al-Thānī",
        "Jumādá al-Ūlá", "Jumādá al-Ākhirah", "Rajab", "Shaʿbān",
        "Ramaḍān", "Shawwāl", "Dhū al-Qaʿdah", "Dhū al-Ḥijjah"
    )

    fun getFormattedHijriDate(isArabic: Boolean = true): String {
        return try {
            val hijrahDate = HijrahDate.now()
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val monthIdx = (hijrahDate.get(ChronoField.MONTH_OF_YEAR) - 1).coerceIn(0, 11)
            val year = hijrahDate.get(ChronoField.YEAR)

            if (isArabic) {
                val arabicDigits = toArabicDigits(day)
                val arabicYear = toArabicDigits(year)
                val monthName = ARABIC_HIJRI_MONTHS[monthIdx]
                "$arabicDigits $monthName $arabicYear هـ"
            } else {
                val monthName = ENGLISH_HIJRI_MONTHS[monthIdx]
                "$day $monthName $year AH"
            }
        } catch (e: Exception) {
            if (isArabic) "١٩ صفر ١٤٤٨ هـ" else "19 Safar 1448 AH"
        }
    }

    private fun toArabicDigits(number: Int): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { char ->
            if (char.isDigit()) arabicDigits[char - '0'] else char
        }.joinToString("")
    }
}
