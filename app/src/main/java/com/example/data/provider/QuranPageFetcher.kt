package com.example.data.provider

import com.example.domain.model.Verse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

data class QuranWordItem(
    val position: Int,
    val textUthmani: String,
    val translationEn: String,
    val transliteration: String,
    val audioUrl: String?
)

object QuranPageFetcher {

    private val pageCache = ConcurrentHashMap<Int, List<Verse>>()
    private val wordsByVerseCache = ConcurrentHashMap<Int, List<QuranWordItem>>()

    fun getCachedPage(page: Int): List<Verse>? = pageCache[page]

    fun getCachedWordsForVerse(surahId: Int, verseNum: Int): List<QuranWordItem>? {
        val key = surahId * 10000 + verseNum
        return wordsByVerseCache[key]
    }

    fun getPageImageUrl(page: Int): String {
        val pageStr = page.toString().padStart(3, '0')
        return "https://files.quran.app/hafs/madani/width_1260/page$pageStr.png"
    }

    fun getSvgPageUrl(page: Int): String {
        val pageStr = page.toString().padStart(3, '0')
        return "https://raw.githubusercontent.com/itqandev/mushaf-svg/main/pages/page_$pageStr.svg"
    }

    fun fetchAndCachePage(page: Int): List<Verse>? {
        if (pageCache.containsKey(page)) {
            return pageCache[page]
        }
        return try {
            val url = URL("https://api.alquran.cloud/v1/page/$page/quran-uthmani")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            if (conn.responseCode == 200) {
                val stream = conn.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val json = JSONObject(sb.toString())
                val data = json.getJSONObject("data")
                val ayahs = data.getJSONArray("ayahs")
                val verseList = mutableListOf<Verse>()

                val englishTranslationsMap = fetchEnglishTranslationsForPage(page)
                val tafsirMap = fetchTafsirMapForPage(page)
                val jalalaynMap = fetchJalalaynMapForPage(page)

                for (i in 0 until ayahs.length()) {
                    val ayahObj = ayahs.getJSONObject(i)
                    val surahObj = ayahObj.getJSONObject("surah")
                    val surahId = surahObj.getInt("number")
                    val surahNameAr = surahObj.getString("name")
                        .replace("سُورَةُ ", "")
                        .replace("سورة ", "")
                        .trim()
                    val surahNameEn = surahObj.optString("englishName", "")
                    val verseNum = ayahObj.getInt("numberInSurah")
                    val juzNum = ayahObj.optInt("juz", 1)
                    val rawText = ayahObj.getString("text")

                    var uthmaniText = rawText.trim()
                    if (uthmaniText.startsWith("\uFEFF")) {
                        uthmaniText = uthmaniText.substring(1).trim()
                    }

                    if (verseNum == 1 && surahId != 1 && surahId != 9) {
                        val bismillahPrefixes = listOf(
                            "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
                        )
                        for (prefix in bismillahPrefixes) {
                            if (uthmaniText.startsWith(prefix)) {
                                uthmaniText = uthmaniText.removePrefix(prefix).trim()
                                break
                            }
                        }
                    }

                    val verseKey = surahId * 10000 + verseNum
                    val realEnTranslation = englishTranslationsMap[verseKey]
                        ?: TafsirProvider.getTafsirForAyah(surahId, verseNum, "muyassar").verseTranslationEn
                        .ifBlank { "Surah $surahNameEn, Verse $verseNum" }

                    val pageTafsir = tafsirMap[verseKey]
                    if (!pageTafsir.isNullOrBlank()) {
                        TafsirProvider.cacheTafsir(surahId, verseNum, "muyassar", pageTafsir)
                    }

                    val finalTafsir = pageTafsir ?: TafsirProvider.getTafsirForAyah(surahId, verseNum, "muyassar").commentaryText

                    verseList.add(
                        Verse(
                            id = verseKey,
                            surahId = surahId,
                            surahNameArabic = surahNameAr,
                            surahNameEnglish = surahNameEn,
                            verseNumber = verseNum,
                            pageNumber = page,
                            juzNumber = juzNum,
                            textUthmani = uthmaniText,
                            translationEnglish = realEnTranslation,
                            tafsirShort = finalTafsir
                        )
                    )
                }

                if (verseList.isNotEmpty()) {
                    pageCache[page] = verseList
                }
                verseList
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchTafsirMapForPage(page: Int): Map<Int, String> {
        return try {
            val url = URL("https://api.alquran.cloud/v1/page/$page/ar.muyassar")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            if (conn.responseCode == 200) {
                val stream = conn.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val json = JSONObject(sb.toString())
                val data = json.getJSONObject("data")
                val ayahs = data.getJSONArray("ayahs")
                val resultMap = mutableMapOf<Int, String>()

                for (i in 0 until ayahs.length()) {
                    val item = ayahs.getJSONObject(i)
                    val surahObj = item.getJSONObject("surah")
                    val surahId = surahObj.getInt("number")
                    val verseNum = item.getInt("numberInSurah")
                    val tafsirText = item.optString("text", "").trim()
                    if (tafsirText.isNotBlank()) {
                        val key = surahId * 10000 + verseNum
                        resultMap[key] = tafsirText
                        TafsirProvider.cacheTafsir(surahId, verseNum, "muyassar", tafsirText)
                    }
                }
                resultMap
            } else {
                emptyMap()
            }
        } catch (e: Throwable) {
            emptyMap()
        }
    }

    private fun fetchJalalaynMapForPage(page: Int): Map<Int, String> {
        return try {
            val url = URL("https://api.alquran.cloud/v1/page/$page/ar.jalalayn")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            if (conn.responseCode == 200) {
                val stream = conn.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val json = JSONObject(sb.toString())
                val data = json.getJSONObject("data")
                val ayahs = data.getJSONArray("ayahs")
                val resultMap = mutableMapOf<Int, String>()

                for (i in 0 until ayahs.length()) {
                    val item = ayahs.getJSONObject(i)
                    val surahObj = item.getJSONObject("surah")
                    val surahId = surahObj.getInt("number")
                    val verseNum = item.getInt("numberInSurah")
                    val tafsirText = item.optString("text", "").trim()
                    if (tafsirText.isNotBlank()) {
                        val key = surahId * 10000 + verseNum
                        resultMap[key] = tafsirText
                        TafsirProvider.cacheTafsir(surahId, verseNum, "jalalayn", tafsirText)
                    }
                }
                resultMap
            } else {
                emptyMap()
            }
        } catch (e: Throwable) {
            emptyMap()
        }
    }

    suspend fun fetchWordsForAyah(surahId: Int, verseNum: Int): List<QuranWordItem> = withContext(Dispatchers.IO) {
        val key = surahId * 10000 + verseNum
        wordsByVerseCache[key]?.let { return@withContext it }
        try {
            val url = URL("https://api.quran.com/api/v4/verses/by_key/${surahId}:${verseNum}?words=true&word_fields=text_uthmani,translation,transliteration")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val stream = conn.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val json = JSONObject(sb.toString())
                val verseObj = json.getJSONObject("verse")
                val wordsArray = verseObj.getJSONArray("words")
                val list = mutableListOf<QuranWordItem>()
                for (i in 0 until wordsArray.length()) {
                    val w = wordsArray.getJSONObject(i)
                    val pos = w.optInt("position", i + 1)
                    val text = w.optString("text_uthmani", w.optString("text", ""))
                    val transObj = w.optJSONObject("translation")
                    val transEn = transObj?.optString("text", "") ?: ""
                    val translitObj = w.optJSONObject("transliteration")
                    val translit = translitObj?.optString("text", "") ?: ""
                    val audio = w.optString("audio_url", null)
                    val fullAudio = if (!audio.isNullOrBlank() && !audio.startsWith("http")) {
                        "https://audio.qurancdn.com/$audio"
                    } else audio

                    list.add(QuranWordItem(pos, text, transEn, translit, fullAudio))
                }
                if (list.isNotEmpty()) {
                    wordsByVerseCache[key] = list
                }
                return@withContext list
            }
        } catch (e: Exception) {
            // Fallback
        }
        emptyList()
    }

    suspend fun fetchWordsForPage(page: Int): Map<Int, List<QuranWordItem>> = withContext(Dispatchers.IO) {
        val resultMap = mutableMapOf<Int, List<QuranWordItem>>()
        try {
            val url = URL("https://api.quran.com/api/v4/verses/by_page/$page?words=true&word_fields=text_uthmani,translation,transliteration")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            if (conn.responseCode == 200) {
                val stream = conn.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val json = JSONObject(sb.toString())
                val versesArray = json.getJSONArray("verses")
                for (v in 0 until versesArray.length()) {
                    val verseObj = versesArray.getJSONObject(v)
                    val verseKeyStr = verseObj.optString("verse_key", "")
                    val parts = verseKeyStr.split(":")
                    if (parts.size == 2) {
                        val sId = parts[0].toIntOrNull() ?: 1
                        val vNum = parts[1].toIntOrNull() ?: 1
                        val key = sId * 10000 + vNum
                        val wordsArray = verseObj.getJSONArray("words")
                        val list = mutableListOf<QuranWordItem>()
                        for (i in 0 until wordsArray.length()) {
                            val w = wordsArray.getJSONObject(i)
                            val pos = w.optInt("position", i + 1)
                            val text = w.optString("text_uthmani", w.optString("text", ""))
                            val transObj = w.optJSONObject("translation")
                            val transEn = transObj?.optString("text", "") ?: ""
                            val translitObj = w.optJSONObject("transliteration")
                            val translit = translitObj?.optString("text", "") ?: ""
                            val audio = w.optString("audio_url", null)
                            val fullAudio = if (!audio.isNullOrBlank() && !audio.startsWith("http")) {
                                "https://audio.qurancdn.com/$audio"
                            } else audio
                            list.add(QuranWordItem(pos, text, transEn, translit, fullAudio))
                        }
                        resultMap[key] = list
                        wordsByVerseCache[key] = list
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
        resultMap
    }

    private fun fetchEnglishTranslationsForPage(page: Int): Map<Int, String> {
        return try {
            val url = URL("https://api.alquran.cloud/v1/page/$page/en.sahih")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            if (conn.responseCode == 200) {
                val stream = conn.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val json = JSONObject(sb.toString())
                val data = json.getJSONObject("data")
                val ayahs = data.getJSONArray("ayahs")
                val resultMap = mutableMapOf<Int, String>()

                for (i in 0 until ayahs.length()) {
                    val item = ayahs.getJSONObject(i)
                    val surahObj = item.getJSONObject("surah")
                    val surahId = surahObj.getInt("number")
                    val verseNum = item.getInt("numberInSurah")
                    val textEn = item.optString("text", "")
                    if (textEn.isNotBlank()) {
                        val key = surahId * 10000 + verseNum
                        resultMap[key] = textEn
                    }
                }
                resultMap
            } else {
                emptyMap()
            }
        } catch (e: Throwable) {
            emptyMap()
        }
    }
}
