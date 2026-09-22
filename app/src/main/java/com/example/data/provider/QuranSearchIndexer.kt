package com.example.data.provider

import com.example.domain.model.Juz
import com.example.domain.model.Surah
import com.example.domain.model.Verse

sealed class SearchResultItem {
    data class SurahResult(
        val surah: Surah,
        val matchedField: String = ""
    ) : SearchResultItem()

    data class VerseResult(
        val verse: Verse,
        val highlightedSnippet: String,
        val matchType: String = "آية" // e.g. "آية", "كلمة مفتاحية", "مطابقة رقمية"
    ) : SearchResultItem()

    data class JuzResult(
        val juz: Juz
    ) : SearchResultItem()

    data class PageResult(
        val pageNumber: Int,
        val surahNameArabic: String,
        val surahNameEnglish: String
    ) : SearchResultItem()
}

enum class SearchCategory(val titleArabic: String, val titleEnglish: String) {
    ALL("الكل", "All"),
    SURAHS("السور", "Surahs"),
    VERSES("الآيات والكلمات", "Verses & Words"),
    JUZ_PAGES("الأجزاء والصفحات", "Juz & Pages")
}

data class UniversalSearchResult(
    val query: String,
    val totalMatches: Int,
    val surahResults: List<SearchResultItem.SurahResult> = emptyList(),
    val verseResults: List<SearchResultItem.VerseResult> = emptyList(),
    val juzResults: List<SearchResultItem.JuzResult> = emptyList(),
    val pageResults: List<SearchResultItem.PageResult> = emptyList()
)

object QuranSearchIndexer {

    // Fast Arabic Text Normalization (strips Tashkeel, normalizes Alifs, Yaa, Taa Marboota)
    fun normalizeArabic(input: String): String {
        if (input.isBlank()) return ""
        var text = input.trim()

        // Remove Arabic Tashkeel / Diacritics (\u064B to \u0652) and Quranic marks
        text = text.replace(Regex("[\u064B-\u0652\u0653\u0670\u06D6-\u06ED\u0610-\u061A]"), "")

        // Normalize Alifs
        text = text.replace(Regex("[أإآٱ]"), "ا")

        // Normalize Yaa / Alif Maqsoora
        text = text.replace('ى', 'ي')

        // Normalize Taa Marboota
        text = text.replace('ة', 'ه')

        // Normalize Hamza
        text = text.replace(Regex("[ؤئ]"), "ء")

        // Clean extra spaces and punctuation
        return text.replace(Regex("[^a-zA-Z0-9\\u0621-\\u064A\\s:]"), "")
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    // Curated local offline indexed database of important & widely searched Quranic Verses
    private val OFFLINE_INDEXED_VERSES: List<Verse> by lazy {
        val list = mutableListOf<Verse>()

        // Surah 1: Al-Fatihah (All 7 Verses)
        list.add(Verse(1001, 1, "الفاتحة", "Al-Fatihah", 1, 1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "سورة الفاتحة افتتح بها القرآن الكريـم وتدعى أم الكتاب"))
        list.add(Verse(1002, 1, "الفاتحة", "Al-Fatihah", 2, 1, 1, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "All praise is due to Allah, Lord of the worlds.", "الثناء والحمد الخالص لله رب المخلوقات كلها"))
        list.add(Verse(1003, 1, "الفاتحة", "Al-Fatihah", 3, 1, 1, "الرَّحْمَٰنِ الرَّحِيمِ", "The Entirely Merciful, the Especially Merciful.", "الذي وسعت رحمته كل شيء في الدنيا والآخرة"))
        list.add(Verse(1004, 1, "الفاتحة", "Al-Fatihah", 4, 1, 1, "مَالِكِ يَوْمِ الدِّينِ", "Sovereign of the Day of Recompense.", "مالك يوم الحساب والجزاء وهو يوم القيامة"))
        list.add(Verse(1005, 1, "الفاتحة", "Al-Fatihah", 5, 1, 1, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help.", "نخصك وحدك بالعبادة ونستعين بك وحدك في أمورنا"))
        list.add(Verse(1006, 1, "الفاتحة", "Al-Fatihah", 6, 1, 1, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Guide us to the straight path.", "اهدنا ووفقنا إلى الطريق الواضح وهو الإسلام"))
        list.add(Verse(1007, 1, "الفاتحة", "Al-Fatihah", 7, 1, 1, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "The path of those upon whom You have bestowed favor...", "طريق النبيين والصديقين والشهداء والصالحين"))

        // Surah 2: Al-Baqarah Famous Verses (e.g. Ayat Al-Kursi, Amanar Rasulu, etc.)
        list.add(Verse(2255, 2, "البقرة", "Al-Baqarah", 255, 42, 3, "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ", "Allah - there is no deity except Him, the Ever-Living, the Sustainer of [all] existence. (Ayat Al-Kursi)", "آية الكرسي أعظم آية في كتاب الله تبارك وتعالى"))
        list.add(Verse(2153, 2, "البقرة", "Al-Baqarah", 153, 23, 2, "يَا أَيُّهَا الَّذِينَ آمَنُوا اسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ ۚ إِنَّ اللَّهَ مَعَ الصَّابِرِينَ", "O you who have believed, seek help through patience and prayer. Indeed, Allah is with the patient.", "الصبر والصلاة أعظم عون للمؤمن في نوازل الدَهر"))
        list.add(Verse(2285, 2, "البقرة", "Al-Baqarah", 285, 49, 3, "آمَنَ الرَّسُولُ بِمَا أُنزِلَ إِلَيْهِ مِن رَّبِّهِ وَالْمُؤْمِنُونَ ۚ كُلٌّ آمَنَ بِاللَّهِ وَمَلَائِكَتِهِ وَكُتُبِهِ وَرُسُلِهِ", "The Messenger has believed in what was revealed to him from his Lord, and [so have] the believers.", "خواتيم سورة البقرة فيها الإيمان والتوكل والمغفرة"))
        list.add(Verse(2286, 2, "البقرة", "Al-Baqarah", 286, 49, 3, "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا ۚ لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ", "Allah does not charge a soul except with that within its capacity.", "رحمة الله بعباده وتخفيف التكاليف الشرعية"))

        // Surah 3: Ali 'Imran
        list.add(Verse(3018, 3, "آل عمران", "Ali 'Imran", 18, 52, 3, "شَهِدَ اللَّهُ أَنَّهُ لَا إِلَٰهَ إِلَّا هُوَ وَالْمَلَائِكَةُ وَأُولُو الْعِلْمِ قَائِمًا بِالْقِسْطِ", "Allah witnesses that there is no deity except Him, and [so do] the angels and those of knowledge.", "شهادة التوحيد العظمى من رب العالمين وملائكته وأوليو العلم"))

        // Surah 18: Al-Kahf
        list.add(Verse(18001, 18, "الكهف", "Al-Kahf", 1, 293, 15, "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجًا", "All praise is due to Allah, who has sent down upon His Servant the Book.", "افتتاح سورة الكهف والثناء على الله لنعمة القرآن الكريم"))
        list.add(Verse(18010, 18, "الكهف", "Al-Kahf", 10, 294, 15, "إِذْ أَوَى الْفِتْيَةُ إِلَى الْكَهْفِ فَقَالُوا رَبَّنَا آتِنَا مِن لَّدُنكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا", "When the youths retreated to the cave and said, 'Our Lord, grant us from Yourself mercy...'", "دعاء فتية الكهف بالرحمة والرشد والأمان"))
        list.add(Verse(18110, 18, "الكهف", "Al-Kahf", 110, 304, 16, "قُلْ إِنَّمَا أَنَا بَشَرٌ مِّثْلُكُمْ يُوحَىٰ إِلَيَّ أَنَّمَا إِلَٰهُكُمْ إِلَٰهٌ وَاحِدٌ", "Say, 'I am only a man like you, to whom has been revealed that your god is one God...'", "خاتمة سورة الكهف والأمر بإخلاص العمل لله"))

        // Surah 20: Taha
        list.add(Verse(20025, 20, "طه", "Taha", 25, 313, 16, "قَالَ رَبِّ اشْرَحْ لِي صَدْرِي ﴿٢٥﴾ وَيَسِّرْ لِي أَمْرِي ﴿٢٦﴾", "He said, 'My Lord, expand for me my breast and ease for me my task...'", "دعاء سيدنا موسى عليه السلام عند إرساله إلى فرعون"))

        // Surah 21: Al-Anbiya
        list.add(Verse(21087, 21, "الأنبياء", "Al-Anbiya", 87, 329, 17, "فَنَادَىٰ فِي الظُّلُمَاتِ أَن لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ", "And he called out within the darknesses, 'There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.'", "دعاء يونس عليه السلام في بطن الحوت (دعوة ذي النون)"))
        list.add(Verse(21030, 21, "الأنبياء", "Al-Anbiya", 30, 324, 17, "وَجَعَلْنَا مِنَ الْمَاءِ كُلَّ شَيْءٍ حَيٍّ ۖ أَفَلَا يُؤْمِنُونَ", "And We made from water every living thing. Then will they not believe?", "الماء أصل الحياة ومصدر كائنات الأرض"))

        // Surah 24: An-Nur
        list.add(Verse(24035, 24, "النور", "An-Nur", 35, 354, 18, "اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ ۚ مَثَلُ نُورِهِ كَمِشْكَاةٍ فِيهَا مِصْبَاحٌ", "Allah is the Light of the heavens and the earth. The example of His light is like a niche...", "آية النور العظيمة في بيان نور الهداية والإيمان"))

        // Surah 36: Yasin
        list.add(Verse(36001, 36, "يس", "Ya-Sin", 1, 440, 22, "يس ﴿١﴾ وَالْقُرْآنِ الْحَكِيمِ ﴿٢﴾ إِنَّكَ لَمِنَ الْمُرْسَلِينَ ﴿٣﴾", "Ya, Seen. By the wise Qur'an. Indeed you are from among the messengers.", "افتتاحية سورة يس المباركة قلب القرآن"))

        // Surah 55: Ar-Rahman
        list.add(Verse(55001, 55, "الرحمن", "Ar-Rahman", 1, 531, 27, "الرَّحْمَٰنُ ﴿١﴾ عَلَّمَ الْقُرْآنَ ﴿٢﴾ خَلَقَ الْإِنسَانَ ﴿٣﴾ عَلَّمَهُ الْبَيَانَ ﴿٤﴾", "The Beneficent. Taught the Qur'an. Created man. Taught him eloquence.", "بيان نعم الرحمن العظيمة على العباد"))

        // Surah 67: Al-Mulk
        list.add(Verse(67001, 67, "الملك", "Al-Mulk", 1, 562, 29, "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ", "Blessed is He in whose hand is dominion, and He is over all things competent.", "سورة الملك المنجية من عذاب القبر"))

        // Surah 94: Ash-Sharh
        list.add(Verse(94005, 94, "الشرح", "Ash-Sharh", 5, 596, 30, "فَإِنَّ مَعَ الْعُسرِ يُسرًا ﴿٥﴾ إِنَّ مَعَ الْعُسْرِ يُسْرًا ﴿٦﴾", "For indeed, with hardship [will be] ease. Indeed, with hardship [will be] ease.", "بشارة الله بالفرج والتيسير بعد الكرب والعسر"))

        // Surah 97: Al-Qadr
        list.add(Verse(97001, 97, "القدر", "Al-Qadr", 1, 598, 30, "إِنَّا أَنزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ ﴿١﴾ وَمَا أَدْرَاكَ مَا لَيْلَةُ الْقَدْرِ ﴿٢﴾", "Indeed, We sent the Qur'an down during the Night of Decree.", "فضل ليلة القدر المباركة خير من ألف شهر"))

        // Surah 112: Al-Ikhlas
        list.add(Verse(112001, 112, "الإخلاص", "Al-Ikhlas", 1, 604, 30, "قُلْ هُوَ اللَّهُ أَحَدٌ ﴿١﴾ اللَّهُ الصَّمَدُ ﴿٢﴾ لَمْ يَلِدْ وَلَمْ يُولَدْ ﴿٣﴾ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ ﴿٤﴾", "Say, 'He is Allah, [who is] One, Allah, the Eternal Refuge...'", "سورة الإخلاص تعدل ثلث القرآن الكريم"))

        // Surah 113: Al-Falaq
        list.add(Verse(113001, 113, "الفلق", "Al-Falaq", 1, 604, 30, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ﴿١﴾ مِن شَرِّ مَا خَلَقَ ﴿٢﴾", "Say, 'I seek refuge in the Lord of daybreak from the evil of that which He created...'", "سورة الفلق والمعوذات للتحصين"))

        // Surah 114: An-Nas
        list.add(Verse(114001, 114, "الناس", "An-Nas", 1, 604, 30, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ﴿١﴾ مَلِكِ النَّاسِ ﴿٢﴾ إِلَٰهِ النَّاسِ ﴿٣﴾", "Say, 'I seek refuge in the Lord of mankind, The Sovereign of mankind...'", "سورة الناس للتحصين والاستعاذة بالله"))

        list
    }

    // Perform Local Universal Search across Surahs, Verses, Juz, Pages, and Numbers
    fun search(query: String): UniversalSearchResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return UniversalSearchResult(
                query = "",
                totalMatches = 0,
                surahResults = QuranDataProvider.SURAHS.map { SearchResultItem.SurahResult(it) }
            )
        }

        val normQuery = normalizeArabic(trimmed)
        val isNumeric = trimmed.all { it.isDigit() }
        val isVerseRef = trimmed.contains(":") && trimmed.split(":").size == 2 && trimmed.split(":")[0].all { it.isDigit() }

        val surahMatches = mutableListOf<SearchResultItem.SurahResult>()
        val verseMatches = mutableListOf<SearchResultItem.VerseResult>()
        val juzMatches = mutableListOf<SearchResultItem.JuzResult>()
        val pageMatches = mutableListOf<SearchResultItem.PageResult>()

        // 1. SURAH MATCHING
        QuranDataProvider.SURAHS.forEach { surah ->
            val normAr = normalizeArabic(surah.nameArabic)
            val normEn = surah.nameEnglish.lowercase()
            val normMeaning = surah.englishMeaning.lowercase()

            if (normAr.contains(normQuery) ||
                normEn.contains(normQuery) ||
                normMeaning.contains(normQuery) ||
                surah.id.toString() == trimmed ||
                (isNumeric && surah.startPage.toString() == trimmed)
            ) {
                val matchField = when {
                    normAr.contains(normQuery) -> "اسم السورة بالعربية"
                    normEn.contains(normQuery) -> "اسم السورة بالإنجليزية"
                    surah.id.toString() == trimmed -> "رقم السورة (${surah.id})"
                    else -> "مطابقة عامة"
                }
                surahMatches.add(SearchResultItem.SurahResult(surah, matchField))
            }
        }

        // 2. VERSE REFERENCE MATCHING (e.g., "2:255" or "36:1")
        if (isVerseRef) {
            val parts = trimmed.split(":")
            val surahNum = parts[0].toIntOrNull() ?: 0
            val verseNum = parts[1].toIntOrNull() ?: 0

            val surah = QuranDataProvider.SURAHS.firstOrNull { it.id == surahNum }
            if (surah != null) {
                val verseMatch = OFFLINE_INDEXED_VERSES.firstOrNull { it.surahId == surahNum && it.verseNumber == verseNum }
                    ?: Verse(
                        id = surahNum * 10000 + verseNum,
                        surahId = surahNum,
                        surahNameArabic = surah.nameArabic,
                        surahNameEnglish = surah.nameEnglish,
                        verseNumber = verseNum,
                        pageNumber = surah.startPage,
                        juzNumber = 1,
                        textUthmani = "سورة ${surah.nameArabic} - الآية $verseNum",
                        translationEnglish = "Surah ${surah.nameEnglish} - Verse $verseNum"
                    )

                verseMatches.add(
                    SearchResultItem.VerseResult(
                        verse = verseMatch,
                        highlightedSnippet = verseMatch.textUthmani,
                        matchType = "مطابقة رقم الآية ($surahNum:$verseNum)"
                    )
                )
            }
        }

        // 3. OFFLINE INDEXED VERSE & KEYWORD TEXT MATCHING
        val allVersesToSearch = mutableListOf<Verse>()
        allVersesToSearch.addAll(OFFLINE_INDEXED_VERSES)

        // Also check cached pages from page fetcher
        for (p in 1..604) {
            val cached = QuranPageFetcher.getCachedPage(p)
            if (cached != null) {
                allVersesToSearch.addAll(cached)
            }
        }

        allVersesToSearch.distinctBy { it.id }.forEach { verse ->
            val normVerseText = normalizeArabic(verse.textUthmani)
            val normTafsir = normalizeArabic(verse.tafsirShort)
            val normEnglish = verse.translationEnglish.lowercase()

            if (normVerseText.contains(normQuery) || normTafsir.contains(normQuery) || normEnglish.contains(normQuery)) {
                val type = when {
                    normVerseText.contains(normQuery) -> "نص القرآن"
                    normTafsir.contains(normQuery) -> "التفسير والموضوع"
                    else -> "الترجمة"
                }

                verseMatches.add(
                    SearchResultItem.VerseResult(
                        verse = verse,
                        highlightedSnippet = verse.textUthmani,
                        matchType = type
                    )
                )
            }
        }

        // 4. JUZ MATCHING
        QuranDataProvider.JUZ_LIST.forEach { juz ->
            val normJuzAr = normalizeArabic(juz.nameArabic)
            val normJuzEn = juz.nameEnglish.lowercase()

            if (normJuzAr.contains(normQuery) ||
                normJuzEn.contains(normQuery) ||
                "جزء ${juz.number}" == trimmed ||
                juz.number.toString() == trimmed
            ) {
                juzMatches.add(SearchResultItem.JuzResult(juz))
            }
        }

        // 5. PAGE NUMBER DIRECT MATCH
        if (isNumeric) {
            val pageNum = trimmed.toIntOrNull() ?: 0
            if (pageNum in 1..604) {
                val surahForPage = QuranDataProvider.getSurahByPage(pageNum)
                pageMatches.add(
                    SearchResultItem.PageResult(
                        pageNumber = pageNum,
                        surahNameArabic = surahForPage.nameArabic,
                        surahNameEnglish = surahForPage.nameEnglish
                    )
                )
            }
        }

        val total = surahMatches.size + verseMatches.size + juzMatches.size + pageMatches.size

        return UniversalSearchResult(
            query = trimmed,
            totalMatches = total,
            surahResults = surahMatches,
            verseResults = verseMatches.distinctBy { it.verse.id },
            juzResults = juzMatches,
            pageResults = pageMatches
        )
    }
}
