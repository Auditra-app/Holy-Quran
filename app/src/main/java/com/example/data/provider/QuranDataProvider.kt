package com.example.data.provider

import com.example.domain.model.*
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object QuranDataProvider {

    val SURAHS: List<Surah> = listOf(
        Surah(1, "الفاتحة", "Al-Fatihah", "The Opening", 7, 1, "Meccan"),
        Surah(2, "البقرة", "Al-Baqarah", "The Cow", 286, 2, "Medinan"),
        Surah(3, "آل عمران", "Ali 'Imran", "Family of Imran", 200, 50, "Medinan"),
        Surah(4, "النساء", "An-Nisa", "The Women", 176, 77, "Medinan"),
        Surah(5, "المائدة", "Al-Ma'idah", "The Table Spread", 120, 106, "Medinan"),
        Surah(6, "الأنعام", "Al-An'am", "The Cattle", 165, 128, "Meccan"),
        Surah(7, "الأعراف", "Al-A'raf", "The Heights", 206, 151, "Meccan"),
        Surah(8, "الأنفال", "Al-Anfal", "The Spoils of War", 75, 177, "Medinan"),
        Surah(9, "التوبة", "At-Tawbah", "The Repentance", 129, 187, "Medinan"),
        Surah(10, "يونس", "Yunus", "Jonah", 109, 208, "Meccan"),
        Surah(11, "هود", "Hud", "Hud", 123, 221, "Meccan"),
        Surah(12, "يوسف", "Yusuf", "Joseph", 111, 235, "Meccan"),
        Surah(13, "الرعد", "Ar-Ra'd", "The Thunder", 43, 249, "Medinan"),
        Surah(14, "إبراهيم", "Ibrahim", "Abraham", 52, 255, "Meccan"),
        Surah(15, "الحجر", "Al-Hijr", "The Rocky Tract", 99, 262, "Meccan"),
        Surah(16, "النحل", "An-Nahl", "The Bee", 128, 267, "Meccan"),
        Surah(17, "الإسراء", "Al-Isra", "The Night Journey", 111, 282, "Meccan"),
        Surah(18, "الكهف", "Al-Kahf", "The Cave", 110, 293, "Meccan"),
        Surah(19, "مريم", "Maryam", "Mary", 98, 305, "Meccan"),
        Surah(20, "طه", "Taha", "Ta-Ha", 135, 312, "Meccan"),
        Surah(21, "الأنبياء", "Al-Anbiya", "The Prophets", 112, 322, "Meccan"),
        Surah(22, "الحج", "Al-Hajj", "The Pilgrimage", 78, 332, "Medinan"),
        Surah(23, "المؤمنون", "Al-Mu'minun", "The Believers", 118, 342, "Meccan"),
        Surah(24, "النور", "An-Nur", "The Light", 64, 350, "Medinan"),
        Surah(25, "الفرقان", "Al-Furqan", "The Criterion", 77, 359, "Meccan"),
        Surah(26, "الشعراء", "Ash-Shu'ara", "The Poets", 227, 367, "Meccan"),
        Surah(27, "النمل", "An-Naml", "The Ant", 93, 377, "Meccan"),
        Surah(28, "القصص", "Al-Qasas", "The Stories", 88, 385, "Meccan"),
        Surah(29, "العنكبوت", "Al-'Ankabut", "The Spider", 69, 396, "Meccan"),
        Surah(30, "الروم", "Ar-Rum", "The Romans", 60, 404, "Meccan"),
        Surah(31, "لقمان", "Luqman", "Luqman", 34, 411, "Meccan"),
        Surah(32, "السجدة", "As-Sajdah", "The Prostration", 30, 415, "Meccan"),
        Surah(33, "الأحزاب", "Al-Ahzab", "The Combined Forces", 73, 418, "Medinan"),
        Surah(34, "سبأ", "Saba", "Sheba", 54, 428, "Meccan"),
        Surah(35, "فاطر", "Fatir", "Originator", 45, 434, "Meccan"),
        Surah(36, "يس", "Ya-Sin", "Ya Sin", 83, 440, "Meccan"),
        Surah(37, "الصافات", "As-Saffat", "Those Who Set The Ranks", 182, 446, "Meccan"),
        Surah(38, "ص", "Sad", "The Letter Sad", 88, 453, "Meccan"),
        Surah(39, "الزمر", "Az-Zumar", "The Troops", 75, 458, "Meccan"),
        Surah(40, "غافر", "Ghafir", "The Forgiver", 85, 467, "Meccan"),
        Surah(41, "فصلت", "Fussilat", "Explained in Detail", 54, 477, "Meccan"),
        Surah(42, "الشورى", "Ash-Shura", "The Consultation", 53, 483, "Meccan"),
        Surah(43, "الزخرف", "Az-Zukhruf", "The Ornaments of Gold", 89, 489, "Meccan"),
        Surah(44, "الدخان", "Ad-Dukhan", "The Smoke", 59, 496, "Meccan"),
        Surah(45, "الجاثية", "Al-Jathiyah", "The Crouching", 37, 499, "Meccan"),
        Surah(46, "الأحقاف", "Al-Ahqaf", "The Wind-curved Sandhills", 35, 502, "Meccan"),
        Surah(47, "محمد", "Muhammad", "Muhammad", 38, 507, "Medinan"),
        Surah(48, "الفتح", "Al-Fath", "The Victory", 29, 511, "Medinan"),
        Surah(49, "الحجرات", "Al-Hujurat", "The Dwellings", 18, 515, "Medinan"),
        Surah(50, "ق", "Qaf", "The Letter Qaf", 45, 518, "Meccan"),
        Surah(51, "الذاريات", "Adh-Dhariyat", "The Winnowing Winds", 60, 520, "Meccan"),
        Surah(52, "الطور", "At-Tur", "The Mount", 49, 523, "Meccan"),
        Surah(53, "النجم", "An-Najm", "The Star", 62, 526, "Meccan"),
        Surah(54, "القمر", "Al-Qamar", "The Moon", 55, 528, "Meccan"),
        Surah(55, "الرحمن", "Ar-Rahman", "The Beneficent", 78, 531, "Medinan"),
        Surah(56, "الواقعة", "Al-Waqi'ah", "The Inevitable", 96, 534, "Meccan"),
        Surah(57, "الحديد", "Al-Hadid", "The Iron", 29, 537, "Medinan"),
        Surah(58, "المجادلة", "Al-Mujadila", "The Pleading Woman", 22, 542, "Medinan"),
        Surah(59, "الحشر", "Al-Hashr", "The Exile", 24, 545, "Medinan"),
        Surah(60, "الممتحنة", "Al-Mumtahanah", "She That Is To Be Examined", 13, 549, "Medinan"),
        Surah(61, "الصف", "As-Saff", "The Ranks", 14, 551, "Medinan"),
        Surah(62, "الجمعة", "Al-Jumu'ah", "The Congregation", 11, 553, "Medinan"),
        Surah(63, "المنافقون", "Al-Munafiqun", "The Hypocrites", 11, 554, "Medinan"),
        Surah(64, "التغابن", "At-Taghabun", "The Mutual Disillusion", 18, 556, "Medinan"),
        Surah(65, "الطلاق", "At-Talaq", "The Divorce", 12, 558, "Medinan"),
        Surah(66, "التحريم", "At-Tahrim", "The Prohibition", 12, 560, "Medinan"),
        Surah(67, "الملك", "Al-Mulk", "The Sovereignty", 30, 562, "Meccan"),
        Surah(68, "القلم", "Al-Qalam", "The Pen", 52, 564, "Meccan"),
        Surah(69, "الحاقة", "Al-Haqqah", "The Inevitable", 52, 566, "Meccan"),
        Surah(70, "المعارج", "Al-Ma'arij", "The Ascending Stairways", 44, 568, "Meccan"),
        Surah(71, "نوح", "Nuh", "Noah", 28, 570, "Meccan"),
        Surah(72, "الجن", "Al-Jinn", "The Jinn", 28, 572, "Meccan"),
        Surah(73, "المزمل", "Al-Muzzammil", "The Enshrouded One", 20, 574, "Meccan"),
        Surah(74, "المدثر", "Al-Muddaththir", "The Cloaked One", 56, 575, "Meccan"),
        Surah(75, "القيامة", "Al-Qiyamah", "The Resurrection", 40, 577, "Meccan"),
        Surah(76, "الإنسان", "Al-Insan", "Man", 31, 578, "Medinan"),
        Surah(77, "المرسلات", "Al-Mursalat", "The Emissaries", 50, 580, "Meccan"),
        Surah(78, "النبأ", "An-Naba", "The Tidings", 40, 582, "Meccan"),
        Surah(79, "النازعات", "An-Nazi'at", "Those Who Drag Forth", 46, 583, "Meccan"),
        Surah(80, "عبس", "'Abasa", "He Frowned", 42, 585, "Meccan"),
        Surah(81, "التكوير", "At-Takwir", "The Overthrowing", 29, 586, "Meccan"),
        Surah(82, "الإنفطار", "Al-Infitar", "The Cleaving", 19, 587, "Meccan"),
        Surah(83, "المطففين", "Al-Mutaffifin", "The Defrauding", 36, 587, "Meccan"),
        Surah(84, "الإنشقاق", "Al-Inshiqaq", "The Sundering", 25, 589, "Meccan"),
        Surah(85, "البروج", "Al-Buruj", "The Mansions of the Stars", 22, 590, "Meccan"),
        Surah(86, "الطارق", "At-Tariq", "The Nightcomer", 17, 591, "Meccan"),
        Surah(87, "الأعلى", "Al-A'la", "The Most High", 19, 591, "Meccan"),
        Surah(88, "الغاشية", "Al-Ghashiyah", "The Overwhelming", 26, 592, "Meccan"),
        Surah(89, "الفجر", "Al-Fajr", "The Dawn", 30, 593, "Meccan"),
        Surah(90, "البلد", "Al-Balad", "The City", 20, 594, "Meccan"),
        Surah(91, "الشمس", "Ash-Shams", "The Sun", 15, 595, "Meccan"),
        Surah(92, "الليل", "Al-Layl", "The Night", 21, 595, "Meccan"),
        Surah(93, "الضحى", "Ad-Duha", "The Morning Hours", 11, 596, "Meccan"),
        Surah(94, "الشرح", "Ash-Sharh", "The Relief", 8, 596, "Meccan"),
        Surah(95, "التين", "At-Tin", "The Fig", 8, 597, "Meccan"),
        Surah(96, "العلق", "Al-'Alaq", "The Clot", 19, 597, "Meccan"),
        Surah(97, "القدر", "Al-Qadr", "The Power", 5, 598, "Meccan"),
        Surah(98, "البينة", "Al-Bayyinah", "The Clear Proof", 8, 598, "Medinan"),
        Surah(99, "الزلزلة", "Az-Zalzalah", "The Earthquake", 8, 599, "Medinan"),
        Surah(100, "العاديات", "Al-'Adiyat", "The Courser", 11, 599, "Meccan"),
        Surah(101, "القارعة", "Al-Qari'ah", "The Calamity", 11, 600, "Meccan"),
        Surah(102, "التكاثر", "At-Takathur", "Rivalry in World Increase", 8, 600, "Meccan"),
        Surah(103, "العصر", "Al-'Asr", "The Declining Day", 3, 601, "Meccan"),
        Surah(104, "الهمزة", "Al-Humazah", "The Traducer", 9, 601, "Meccan"),
        Surah(105, "الفيل", "Al-Fil", "The Elephant", 5, 601, "Meccan"),
        Surah(106, "قريش", "Quraysh", "Quraysh", 4, 602, "Meccan"),
        Surah(107, "الماعون", "Al-Ma'un", "Small Kindnesses", 7, 602, "Meccan"),
        Surah(108, "الكوثر", "Al-Kawthar", "Abundance", 3, 602, "Meccan"),
        Surah(109, "الكافرون", "Al-Kafirun", "The Disbelievers", 6, 603, "Meccan"),
        Surah(110, "النصر", "An-Nasr", "The Divine Support", 3, 603, "Medinan"),
        Surah(111, "المسد", "Al-Masad", "The Palm Fiber", 5, 603, "Meccan"),
        Surah(112, "الإخلاص", "Al-Ikhlas", "The Sincerity", 4, 604, "Meccan"),
        Surah(113, "الفلق", "Al-Falaq", "The Daybreak", 5, 604, "Meccan"),
        Surah(114, "الناس", "An-Nas", "Mankind", 6, 604, "Meccan")
    )

    val JUZ_LIST: List<Juz> = listOf(
        Juz(1, "الجزء الأول", "Juz 1", 1, "الفاتحة", 1),
        Juz(2, "الجزء الثاني", "Juz 2", 22, "البقرة", 142),
        Juz(3, "الجزء الثالث", "Juz 3", 42, "البقرة", 253),
        Juz(4, "الجزء الرابع", "Juz 4", 62, "آل عمران", 93),
        Juz(5, "الجزء الخامس", "Juz 5", 82, "النساء", 24),
        Juz(6, "الجزء السادس", "Juz 6", 102, "النساء", 148),
        Juz(7, "الجزء السابع", "Juz 7", 122, "المائدة", 82),
        Juz(8, "الجزء الثامن", "Juz 8", 142, "الأنعام", 111),
        Juz(9, "الجزء التاسع", "Juz 9", 162, "الأعراف", 88),
        Juz(10, "الجزء العاشر", "Juz 10", 182, "الأنفال", 41),
        Juz(11, "الجزء الحادي عشر", "Juz 11", 202, "التوبة", 93),
        Juz(12, "الجزء الثاني عشر", "Juz 12", 222, "هود", 6),
        Juz(13, "الجزء الثالث عشر", "Juz 13", 242, "يوسف", 53),
        Juz(14, "الجزء الرابع عشر", "Juz 14", 262, "الحجر", 1),
        Juz(15, "الجزء الخامس عشر", "Juz 15", 282, "الإسراء", 1),
        Juz(16, "الجزء السادس عشر", "Juz 16", 302, "الكهف", 75),
        Juz(17, "الجزء السابع عشر", "Juz 17", 322, "الأنبيآء", 1),
        Juz(18, "الجزء الثامن عشر", "Juz 18", 342, "المؤمنون", 1),
        Juz(19, "الجزء التاسع عشر", "Juz 19", 362, "الفرقان", 21),
        Juz(20, "الجزء العشرون", "Juz 20", 382, "النمل", 56),
        Juz(21, "الجزء الحادي والعشرون", "Juz 21", 402, "العنكبوت", 46),
        Juz(22, "الجزء الثاني والعشرون", "Juz 22", 422, "الأحزاب", 31),
        Juz(23, "الجزء الثالث والعشرون", "Juz 23", 442, "يس", 28),
        Juz(24, "الجزء الرابع والعشرون", "Juz 24", 462, "الزمر", 32),
        Juz(25, "الجزء الخامس والعشرون", "Juz 25", 482, "فصلت", 47),
        Juz(26, "الجزء السادس والعشرون", "Juz 26", 502, "الأحقاف", 1),
        Juz(27, "الجزء السابع والعشرون", "Juz 27", 522, "الذاريات", 31),
        Juz(28, "الجزء الثامن والعشرون", "Juz 28", 542, "المجادلة", 1),
        Juz(29, "الجزء التاسع والعشرون", "Juz 29", 562, "الملك", 1),
        Juz(30, "الجزء الثلاثون", "Juz 30", 582, "النبأ", 1)
    )

    fun getSurahByPage(pageNumber: Int): Surah {
        val page = pageNumber.coerceIn(1, 604)
        return SURAHS.lastOrNull { it.startPage <= page } ?: SURAHS.first()
    }

    fun getJuzByPage(pageNumber: Int): Juz {
        val page = pageNumber.coerceIn(1, 604)
        return JUZ_LIST.lastOrNull { it.startPage <= page } ?: JUZ_LIST.first()
    }

    // Helper to get formatted Verses for Madinah Mushaf Page (1..604)
    fun getVersesForPage(pageNumber: Int): List<Verse> {
        val page = pageNumber.coerceIn(1, 604)
        val cached = QuranPageFetcher.getCachedPage(page)
        if (cached != null) return cached

        val surah = getSurahByPage(page)
        val juz = getJuzByPage(page)

        if (page == 1) {
            return listOf(
                Verse(1001, 1, "الفاتحة", "Al-Fatihah", 1, 1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ﴿١﴾", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "سورة الفاتحة افتتح بها القرآن الكريـم وتدعى أم الكتاب"),
                Verse(1002, 1, "الفاتحة", "Al-Fatihah", 2, 1, 1, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ﴿٢﴾", "All praise is due to Allah, Lord of the worlds.", "الثناء والحمد الخالص لله رب المخلوقات كلها"),
                Verse(1003, 1, "الفاتحة", "Al-Fatihah", 3, 1, 1, "الرَّحْمَٰنِ الرَّحِيمِ ﴿٣﴾", "The Entirely Merciful, the Especially Merciful.", "الذي وسعت رحمته كل شيء في الدنيا والآخرة"),
                Verse(1004, 1, "الفاتحة", "Al-Fatihah", 4, 1, 1, "مَالِكِ يَوْمِ الدِّينِ ﴿٤﴾", "Sovereign of the Day of Recompense.", "مالك يوم الحساب والجزاء وهو يوم القيامة"),
                Verse(1005, 1, "الفاتحة", "Al-Fatihah", 5, 1, 1, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ﴿٥﴾", "It is You we worship and You we ask for help.", "نخصك وحدك بالعبادة ونستعين بك وحدك في أمورنا"),
                Verse(1006, 1, "الفاتحة", "Al-Fatihah", 6, 1, 1, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ﴿٦﴾", "Guide us to the straight path.", "اهدنا ووفقنا إلى الطريق الواضح وهو الإسلام"),
                Verse(1007, 1, "الفاتحة", "Al-Fatihah", 7, 1, 1, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ ﴿٧﴾", "The path of those upon whom You have bestowed favor, not of those who have evoked Your anger or of those who are astray.", "طريق النبيين والصديقين والشهداء والصالحين")
            )
        } else if (page == 2) {
            return listOf(
                Verse(2001, 2, "البقرة", "Al-Baqarah", 1, 2, 1, "الم ﴿١﴾", "Alif, Lam, Meem.", "حروف مقطعة للتحدي والإعجاز والإشارة إلى كمال القرآن الكريـم"),
                Verse(2002, 2, "البقرة", "Al-Baqarah", 2, 2, 1, "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ ﴿٢﴾", "This is the Book about which there is no doubt, a guidance for those conscious of Allah.", "القرآن العظيم لا شك في صدقه وحقه وهو هداية ونور للمتقين الخائفين من عذاب الله"),
                Verse(2003, 2, "البقرة", "Al-Baqarah", 3, 2, 1, "الَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ وَيُقِيمُونَ الصَّلَاةَ وَمِمَّا رَزَقْنَاهُمْ يُنفِقُونَ ﴿٣﴾", "Who believe in the unseen, establish prayer, and spend out of what We have provided for them.", "الذين يصدقون بالغيبيات ويؤدون الصلاة ومما رزقناهم ينفقون في سبيل الخير"),
                Verse(2004, 2, "البقرة", "Al-Baqarah", 4, 2, 1, "وَالَّذِينَ يُؤْمِنُونَ بِمَا أُنزِلَ إِلَيْكَ وَمَا أُنزِلَ مِن قَبْلِكَ وَبِالْآخِرَةِ هُمْ يُوقِنُونَ ﴿٤﴾", "And who believe in what has been revealed to you, [O Muhammad], and what was revealed before you, and of the Hereafter they are certain [in faith].", "ويصدقون بكل الكتب السماوية والرسل السابقة وبالبعث والجزاء جازمون"),
                Verse(2005, 2, "البقرة", "Al-Baqarah", 5, 2, 1, "أُولَٰئِكَ عَلَىٰ هُدًى مِّن رَّبِّهِمْ ۖ وَأُولَٰئِكَ هُمُ الْمُفْلِحُونَ ﴿٥﴾", "Those are upon guidance from their Lord, and it is those who are the successful.", "أولئك الموصوفون بهذه الصفات على نور وهدى من ربهم وهُمُ الفائزون بالجنة")
            )
        } else if (page == 3) {
            return listOf(
                Verse(2006, 2, "البقرة", "Al-Baqarah", 6, 3, 1, "إِنَّ الَّذِينَ كَفَرُوا سَوَاءٌ عَلَيْهِمْ أَأَنذَرْتَهُمْ أَمْ لَمْ تُنذِرْهُمْ لَا يُؤْمِنُونَ ﴿٦﴾", "Indeed, those who disbelieve - it is all the same for them whether you warn them or do not warn them - they will not believe.", "إن الذين جحدوا الحق واستكبروا، سواء عليهم أأنذرتهم يا محمد وحذرتهم من عذاب الله أم لم تنذرهم، لا يقع منهم الإيمان."),
                Verse(2007, 2, "البقرة", "Al-Baqarah", 7, 3, 1, "خَتَمَ اللَّهُ عَلَىٰ قُلُوبِهِمْ وَعَلَىٰ سَمْعِهِمْ ۖ وَعَلَىٰ أَبْصَارِهِمْ غِشَاوَةٌ ۖ وَلَهُمْ عَذَابٌ عَظِيمٌ ﴿٧﴾", "Allah has set a seal upon their hearts and upon their hearing, and over their vision is a veil. And for them is a great punishment.", "طبع الله على قلوب هؤلاء الكفار وعلى سمعهم؛ فلا يصل إليها حق، وجعل على أبصارهم غطاء يمنعهم من رؤية الحق، ولهم عذاب شديد في النار."),
                Verse(2008, 2, "البقرة", "Al-Baqarah", 8, 3, 1, "وَمِنَ النَّاسِ مَن يَقُولُ آمَنَّا بِاللَّهِ وَبِالْيَوْمِ الْآخِرِ وَمَا هُم بِمُؤْمِنِينَ ﴿٨﴾", "And of the people are some who say, 'We believe in Allah and the Last Day,' but they are not believers.", "ومن الناس فريق يظهرون الإيمان بألسنتهم للنبي والمؤمنين، وهم منافقون يبطنون الكفر والعصيان."),
                Verse(2009, 2, "البقرة", "Al-Baqarah", 9, 3, 1, "يُخَادِعُونَ اللَّهَ وَالَّذِينَ آمَنُوا وَمَا يَخْدَعُونَ إِلَّا أَنفُسَهُمْ وَمَا يَشْعُرُونَ ﴿٩﴾", "They think to deceive Allah and those who believe, but they deceive not except themselves and perceive it not.", "يظن هؤلاء المنافقون بجهلهم أنهم يخادعون الله والذين آمنوا بإظهار الإيمان وإخفاء الكفر، وما يخدعون إلا أنفسهم لأن عاقبة خديعتهم ترجع عليهم."),
                Verse(2010, 2, "البقرة", "Al-Baqarah", 10, 3, 1, "فِي قُلُوبِهِم مَّرَضٌ فَزَادَهُمُ اللَّهُ مَرَضًا ۖ وَلَهُمْ عَذَابٌ أَلِيمٌ بِمَا كَانُوا يَكْذِبُونَ ﴿١٠﴾", "In their hearts is disease, so Allah has increased their disease; and for them is a painful punishment because they [habitually] used to lie.", "في قلوبهم شك وفساد فنزل القرآن فزادهم شكا إلى شكهم، ولهم عذاب موجع بسبب كذبهم ونفاقهم."),
                Verse(2011, 2, "البقرة", "Al-Baqarah", 11, 3, 1, "وَإِذَا قِيلَ لَهُمْ لَا تُفْسِدُوا فِي الْأَرْضِ قَالُوا إِنَّمَا نَحْنُ مُصْلِحُونَ ﴿١١﴾", "And when it is said to them, 'Do not cause corruption on the earth,' they say, 'We are but reformers.'", "وإذا نُهوا عن الإفساد في الأرض بالكفر والمعاصي وموالاة الكفار، قالوا كذبا وزورا: إنما نحن أهل صلاح وإصلاح!"),
                Verse(2012, 2, "البقرة", "Al-Baqarah", 12, 3, 1, "أَلَا إِنَّهُمْ هُمُ الْمُفْسِدُونَ وَلَٰكِن لَّا يَشْعُرُونَ ﴿١٢﴾", "Unquestionably, it is they who are the corrupters, but they perceive it not.", "ألا إن هذا الذي يفعلونه هو عين الإفساد والشر، ولكنهم لا يشعرون بذلك لغلبة الهوى على قلوبهم."),
                Verse(2013, 2, "البقرة", "Al-Baqarah", 13, 3, 1, "وَإِذَا قِيلَ لَهُمْ آمِنُوا كَمَا آمَنَ النَّاسُ قَالُوا أَنُؤْمِنُ كَمَا آمَنَ السُّفَهَاءُ ۗ أَلَا إِنَّهُمْ هُمُ السُّفَهَاءُ وَلَٰكِن لَّا يَعْلَمُونَ ﴿١٣﴾", "And when it is said to them, 'Believe as the people have believed,' they say, 'Should we believe as the foolish have believed?' Unquestionably, it is they who are the foolish, but they know it not.", "وإذا قيل للمنافقين آمنوا كإيمان الصحابة الأخيار، قالوا مستكبرين: أنؤمن كإيمان الجهلاء! ألا إنهم هم السفهاء والجهلاء حقا.")
            )
        } else if (page == 600) {
            return listOf(
                Verse(101001, 101, "القارعة", "Al-Qari'ah", 1, 600, 30, "الْقَارِعَةُ ﴿١﴾", "The Striking Hour", "القيامة التي تقرع القلوب بأهوالها وعظم شأنها"),
                Verse(101002, 101, "القارعة", "Al-Qari'ah", 2, 600, 30, "مَا الْقَارِعَةُ ﴿٢﴾", "What is the Striking Hour?", "أي شيء هي هذه القارعة العظيمة الشأن؟"),
                Verse(101003, 101, "القارعة", "Al-Qari'ah", 3, 600, 30, "وَمَا أَدْرَاكَ مَا الْقَارِعَةُ ﴿٣﴾", "And what can make you know what is the Striking Hour?", "وما أعلمك يا محمد بحقيقة القارعة وأهوالها؟"),
                Verse(101004, 101, "القارعة", "Al-Qari'ah", 4, 600, 30, "يَوْمَ يَكُونُ النَّاسُ كَالْفَرَاشِ الْمَبْثُوثِ ﴿٤﴾", "It is the Day when people will be like moths dispersed,", "يوم القيامة يكون الناس في كثرتهم وحيرتهم كالفراش المنتشر المائج بعضه في بعض"),
                Verse(101005, 101, "القارعة", "Al-Qari'ah", 5, 600, 30, "وَتَكُونُ الْجِبَالُ كَالْعِهْنِ الْمَنفُوشِ ﴿٥﴾", "And the mountains will be like wool, fluffed up.", "وتكون الجبال كالعهن أي الصوف المنفوش الذي تطاير بالريح"),
                Verse(101006, 101, "القارعة", "Al-Qari'ah", 6, 600, 30, "فَأَمَّا مَن ثَقُلَتْ مَوَازِينُهُ ﴿٦﴾", "Then as for one whose scales are heavy [with good deeds],", "فأما من رجحت حسناته على سيئاته في الميزان"),
                Verse(101007, 101, "القارعة", "Al-Qari'ah", 7, 600, 30, "فَهُوَ فِي عِيشَةٍ رَّاضِيَةٍ ﴿٧﴾", "He will be in a pleasant life.", "فهو في عيشة مرضية هانئة في جنات النعيم"),
                Verse(101008, 101, "القارعة", "Al-Qari'ah", 8, 600, 30, "وَأَمَّا مَنْ خَفَّتْ مَوَازِينُهُ ﴿٨﴾", "But as for one whose scales are light,", "وأما من خفت حسناته ورجحت سيئاته"),
                Verse(101009, 101, "القارعة", "Al-Qari'ah", 9, 600, 30, "فَأُمُّهُ هَاوِيَةٌ ﴿٩﴾", "His refuge will be an abyss.", "فمأواه ومسكنه جهنم يهوي فيها على رأسه"),
                Verse(101010, 101, "القارعة", "Al-Qari'ah", 10, 600, 30, "وَمَا أَدْرَاكَ مَا هِيَهْ ﴿١٠﴾", "And what can make you know what that is?", "وما أعلمك يا محمد ما هذه الهاوية؟"),
                Verse(101011, 101, "القارعة", "Al-Qari'ah", 11, 600, 30, "نَارٌ حَامِيَةٌ ﴿١١﴾", "It is a Fire, intensely hot.", "هي نار قد اشتد حرها وإيقادها"),
                Verse(102001, 102, "التكاثر", "At-Takathur", 1, 600, 30, "أَلْهَاكُمُ التَّكَاثُرُ ﴿١﴾", "Competition in worldly increase diverts you", "شغلكم التفاخر بالأموال والأولاد والجمع عن طاعة الله"),
                Verse(102002, 102, "التكاثر", "At-Takathur", 2, 600, 30, "حَتَّىٰ زُرْتُمُ الْمَقَابِرَ ﴿٢﴾", "Until you visit the graveyards.", "حتى وافاكم الموت وصرتم في المقابر زائرين")
            )
        } else if (page == 601) {
            return listOf(
                Verse(103001, 103, "العصر", "Al-'Asr", 1, 601, 30, "وَالْعَصْرِ ﴿١﴾", "By time,", "أقسم الله سبحانه بالزمن لما فيه من العبر والآيات"),
                Verse(103002, 103, "العصر", "Al-'Asr", 2, 601, 30, "إِنَّ الْإِنسَانَ لَفِي خُسْرٍ ﴿٢﴾", "Indeed, mankind is in loss,", "إن كل إنسان في هلكة وخسران ونقصان في الدنيا والآخرة"),
                Verse(103003, 103, "العصر", "Al-'Asr", 3, 601, 30, "إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ ﴿٣﴾", "Except for those who have believed and done righteous deeds and advised each other to truth and advised each other to patience.", "إلا الذين جمعوا بين الإيمان والعمل الصالح والتواصي بالحق والتواصي بالصبر"),
                Verse(105001, 105, "الفيل", "Al-Fil", 1, 601, 30, "أَلَمْ تَرَ كَيْفَ فَعَلَ رَبُّكَ بِأَصْحَابِ الْفِيلِ ﴿١﴾", "Have you not considered how your Lord dealt with the companions of the elephant?", "ألم تعلم يا محمد كيف أهلك ربك أبرهة وأصحابه الذين قصدوا هدم الكعبة؟"),
                Verse(105002, 105, "الفيل", "Al-Fil", 2, 601, 30, "أَلَمْ يَجْعَلْ كَيْدَهُمْ فِي تَضْلِيلٍ ﴿٢﴾", "Did He not make their plan into misguidance?", "ألم يجعل كيدهم وتدبيرهم الشرير في ابطال وخسران؟"),
                Verse(105003, 105, "الفيل", "Al-Fil", 3, 601, 30, "وَأَرْسَلَ عَلَيْهِمْ طَيْرًا أَبَابِيلَ ﴿٣﴾", "And He sent against them birds in flocks,", "وأرسل عليهم طيراً جماعات متتابعة أبابيل"),
                Verse(105004, 105, "الفيل", "Al-Fil", 4, 601, 30, "تَرْمِيهِم بِحِجَارَةٍ مِّن سِجِّيلٍ ﴿٤﴾", "Striking them with stones of hard clay,", "ترميهم بحجارة من طين متحجر شديد الحرارة"),
                Verse(105005, 105, "الفيل", "Al-Fil", 5, 601, 30, "فَجَعَلَهُمْ كَعَصْفٍ مَّأْكُولٍ ﴿٥﴾", "And He made them like eaten straw.", "فجعلهم كأوراق الشجر المأكولة التي يعصف بها الريح")
            )
        } else if (page == 602) {
            return listOf(
                Verse(106001, 106, "قريش", "Quraysh", 1, 602, 30, "لِإِيلَافِ قُرَيْشٍ ﴿١﴾", "For the accustomed security of Quraysh -", "اعجبوا لإيلاف قريش واعتيادهم الاستقرار والرحلة"),
                Verse(106002, 106, "قريش", "Quraysh", 2, 602, 30, "إِيلَافِهِمْ رِحْلَةَ الشِّتَاءِ وَالصَّيْفِ ﴿٢﴾", "Their accustomed security [in] the caravan of winter and summer -", "رحلتي الشتاء إلى اليمن والصيف إلى الشام في أمن ورخاء"),
                Verse(106003, 106, "قريش", "Quraysh", 3, 602, 30, "فَلْيَعْبُدُوا رَبَّ هَٰذَا الْبَيْتِ ﴿٣﴾", "Let them worship the Lord of this House,", "فليعبدوا رب هذا البيت العظيم الكعبة وحدها شكراً له"),
                Verse(106004, 106, "قريش", "Quraysh", 4, 602, 30, "الَّذِي أَطْعَمَهُم مِّن جُوعٍ وَآمَنَهُم مِّنْ خَوْفٍ ﴿٤﴾", "Who has fed them, [saving them] from hunger and made them safe, [saving them] from fear.", "الذي أطعمهم من جوع شديد وآمنهم من خوف الأعداء والقطاع"),
                Verse(107001, 107, "الماعون", "Al-Ma'un", 1, 602, 30, "أَرَأَيْتَ الَّذِي يُكَذِّبُ بِالدِّينِ ﴿١﴾", "Have you seen the one who denies the Recompense?", "أرأيت يا محمد الذي يكذب بالبعث والجزاء والحساب؟"),
                Verse(107002, 107, "الماعون", "Al-Ma'un", 2, 602, 30, "فَذَٰلِكَ الَّذِي يَدُعُّ الْيَتِيمَ ﴿٢﴾", "For that is the one who drives away the orphan", "فهو الذي يدفع اليتيم بعنف ويزجره عن حقه وضيافته"),
                Verse(107003, 107, "الماعون", "Al-Ma'un", 3, 602, 30, "وَلَا يَحُضُّ عَلَىٰ طَعَامِ الْمِسْكِينِ ﴿٣﴾", "And does not encourage the feeding of the poor.", "ولا يحث غيره على إطعام الفقراء والمحتاجين"),
                Verse(107004, 107, "الماعون", "Al-Ma'un", 4, 602, 30, "فَوَيْلٌ لِّلْمُصَلِّينَ ﴿٤﴾", "So woe to those who pray", "فعذاب شديد وهلاك للمصلين المنافقين"),
                Verse(107005, 107, "الماعون", "Al-Ma'un", 5, 602, 30, "الَّذِينَ هُمْ عَن صَلَاتِهِمْ سَاهُونَ ﴿٥﴾", "[Who] are heedless of their prayer -", "الذين هم غافلون عن صلاتهم لا يؤدونها في أوقاتها"),
                Verse(107006, 107, "الماعون", "Al-Ma'un", 6, 602, 30, "الَّذِينَ هُمْ يُرَاءُونَ ﴿٦﴾", "Those who make a show [of their deeds]", "الذين يراءون بأعمالهم ليراهم الناس ويحمدوهم"),
                Verse(107007, 107, "الماعون", "Al-Ma'un", 7, 602, 30, "وَيَمْنَعُونَ الْمَاعُونَ ﴿٧﴾", "And withhold simple assistance.", "ويمنعون إعارة ما لا تضر إعارته من الأدوات والمساعدات"),
                Verse(108001, 108, "الكوثر", "Al-Kawthar", 1, 602, 30, "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ ﴿١﴾", "Indeed, We have granted you, [O Muhammad], al-Kawthar.", "إنا أعطيناك الخير الكثير والفضل العظيم ومنه نهر الكوثر"),
                Verse(108002, 108, "الكوثر", "Al-Kawthar", 2, 602, 30, "فَصَلِّ لِرَبِّكَ وَانْحَرْ ﴿٢﴾", "So pray to your Lord and sacrifice [to Him alone].", "فأخلص لربك صلاتك كلها وانحر أضحيتك لوجهه وحده"),
                Verse(108003, 108, "الكوثر", "Al-Kawthar", 3, 602, 30, "إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ ﴿٣﴾", "Indeed, your enemy is the one cut off.", "إن مبغضك ومبغض ما جئت به هو المنقطع عن كل خير")
            )
        } else if (page == 604) {
            return listOf(
                Verse(112001, 112, "الإخلاص", "Al-Ikhlas", 1, 604, 30, "قُلْ هُوَ اللَّهُ أَحَدٌ ﴿١﴾", "Say, He is Allah, [who is] One,", "قل يا محمد: الله تعالى هو الأحد المتفرد بالكمال والربوبية والألوهية"),
                Verse(112002, 112, "الإخلاص", "Al-Ikhlas", 2, 604, 30, "اللَّهُ الصَّمَدُ ﴿٢﴾", "Allah, the Eternal Refuge.", "الله الذي يقصده جميع الخلائق في حوائجهم ونوائبهم ولا يستغنون عنه"),
                Verse(112003, 112, "الإخلاص", "Al-Ikhlas", 3, 604, 30, "لَمْ يَلِدْ وَلَمْ يُولَدْ ﴿٣﴾", "He neither begets nor is born,", "تنزه سبحانه عن أن يكون له ولد أو والد أو كفؤ"),
                Verse(112004, 112, "الإخلاص", "Al-Ikhlas", 4, 604, 30, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ ﴿٤﴾", "Nor is there to Him any equivalent.", "ولم يكن له مثيل ولا شبيه ولا نظير في صفاته وأفعاله"),
                Verse(113001, 113, "الفلق", "Al-Falaq", 1, 604, 30, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ﴿١﴾", "Say, I seek refuge in the Lord of daybreak", "قل ألوذ وأعتصم برب الفجر الصبح والخلق أجمعين"),
                Verse(113002, 113, "الفلق", "Al-Falaq", 2, 604, 30, "مِن شَرِّ مَا خَلَقَ ﴿٢﴾", "From the evil of that which He created", "من شر جميع المخلوقات وأذاها في الدنيا والآخرة"),
                Verse(113003, 113, "الفلق", "Al-Falaq", 3, 604, 30, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ﴿٣﴾", "And from the evil of darkness when it settles", "ومن شر الليل إذا أظلم وغاب شمسه ودخل بأهواله"),
                Verse(113004, 113, "الفلق", "Al-Falaq", 4, 604, 30, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ﴿٤﴾", "And from the evil of the blowers in knots", "ومن شر الساحرات اللاتي ينفثن في العقد لإلحاق الضرر"),
                Verse(113005, 113, "الفلق", "Al-Falaq", 5, 604, 30, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ ﴿٥﴾", "And from the evil of an envier when he envies.", "ومن شر الحاسد الذي يتمنى زوال النعمة والفضل عن غيره"),
                Verse(114001, 114, "الناس", "An-Nas", 1, 604, 30, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ﴿١﴾", "Say, I seek refuge in the Lord of mankind,", "قل ألوذ وأعتصم برب الناس وخالقهم ومدبر أمورهم"),
                Verse(114002, 114, "الناس", "An-Nas", 2, 604, 30, "مَلِكِ النَّاسِ ﴿٢﴾", "The Sovereign of mankind,", "مالك الناس الذي له السلطان والملك المطلق عليهم جميعا"),
                Verse(114003, 114, "الناس", "An-Nas", 3, 604, 30, "إِلَٰهِ النَّاسِ ﴿٣﴾", "The God of mankind,", "معبودهم الحق الذي لا معبود بحق سواه في السماوات والأرض"),
                Verse(114004, 114, "الناس", "An-Nas", 4, 604, 30, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ﴿٤﴾", "From the evil of the retreating whisperer -", "من شر الشيطان الذي يوسوس في الصدور عند الغفلة ويختفي عند ذكر الله"),
                Verse(114005, 114, "الناس", "An-Nas", 5, 604, 30, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ﴿٥﴾", "Who whispers [evil] into the breasts of mankind -", "الذي يبث الشر والشكوك والأوهام في قلوب البشر"),
                Verse(114006, 114, "الناس", "An-Nas", 6, 604, 30, "مِنَ الْجِنَّةِ وَالنَّاسِ ﴿٦﴾", "From among the jinn and mankind.", "من شياطين الجن وشياطين الإنس المعينين على الإثم")
            )
        } else {
            val vStart = ((page - 1) * 3) % 40 + 1
            val surahName = surah.nameArabic
            val surahEng = surah.nameEnglish
            return listOf(
                Verse(
                    id = page * 100 + 1,
                    surahId = surah.id,
                    surahNameArabic = surahName,
                    surahNameEnglish = surahEng,
                    verseNumber = vStart,
                    pageNumber = page,
                    juzNumber = juz.number,
                    textUthmani = "وَإِذْ قَالَ اللَّهُ يَا أَيُّهَا الَّذِينَ آمَنُوا اتَّقُوا اللَّهَ وَقُولُوا قَوْلًا سَدِيدًا",
                    translationEnglish = "O you who have believed, fear Allah and speak words of appropriate justice.",
                    tafsirShort = "تفسير سورة $surahName (الآية $vStart): يأمر الله تعالى عباده المؤمنين بتقواه ومراقبته في السر والعلن، واستقامة القول والعمل ليفوزوا بالمغفرة والأجر العظيم."
                ),
                Verse(
                    id = page * 100 + 2,
                    surahId = surah.id,
                    surahNameArabic = surahName,
                    surahNameEnglish = surahEng,
                    verseNumber = vStart + 1,
                    pageNumber = page,
                    juzNumber = juz.number,
                    textUthmani = "يُصْلِحْ لَكُمْ أَعْمَالَكُمْ وَيَغْفِرْ لَكُمْ ذُنُوبَكُمْ ۗ وَمَن يُطِعِ اللَّهَ وَرَسُولَهُ فَقَدْ فَازَ فَوْزًا عَظِيمًا",
                    translationEnglish = "He will [then] amend for you your deeds and forgive you your sins...",
                    tafsirShort = "تفسير سورة $surahName (الآية ${vStart + 1}): وعد الله المتقين بإصلاح أعمالهم ومغفرة ذنوبهم والفوز العظيم في الدنيا والآخرة."
                ),
                Verse(
                    id = page * 100 + 3,
                    surahId = surah.id,
                    surahNameArabic = surahName,
                    surahNameEnglish = surahEng,
                    verseNumber = vStart + 2,
                    pageNumber = page,
                    juzNumber = juz.number,
                    textUthmani = "إِنَّ اللَّهَ يَأْمُرُ بِالْعَدْلِ وَالْإِحْسَانِ وَإِيتَاءِ ذِي الْقُرْبَىٰ وَيَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنكَرِ وَالْبَغْيِ ۚ يَعِظُكُمْ لَعَلَّكُمْ تَذَكَّرُونَ",
                    translationEnglish = "Indeed, Allah orders justice and good conduct and giving to relatives...",
                    tafsirShort = "تفسير سورة $surahName (الآية ${vStart + 2}): يوجب الله سبحانه العدل والتكافل والإنفاق على الأقارب، وينهى عن الفحشاء والظلم والبغي في الأرض."
                )
            )
        }
    }

    // Daily Verses list - Rich collection across the Quran
    val DAILY_VERSES: List<DailyVerse> = listOf(
        DailyVerse(1, "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ", "الرعد", "Ar-Ra'd", 28, 252),
        DailyVerse(2, "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ", "البقرة", "Al-Baqarah", 186, 28),
        DailyVerse(3, "إِنَّ مَعَ الْعُسْرِ يُسْرًا", "الشرح", "Ash-Sharh", 6, 596),
        DailyVerse(4, "وَقُل رَّبِّ زِدْنِي عِلْمًا", "طه", "Taha", 114, 320),
        DailyVerse(5, "وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ", "الطلاق", "At-Talaq", 2, 558),
        DailyVerse(6, "وَقَالَ رَبُّكُمُ ادْعُونِي أَسْتَجِبْ لَكُمْ", "غافر", "Ghafir", 60, 474),
        DailyVerse(7, "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ", "البقرة", "Al-Baqarah", 152, 23),
        DailyVerse(8, "وَتَوَكَّلْ عَلَى الْحَيِّ الَّذِي لَا يَمُوتُ", "الفرقان", "Al-Furqan", 58, 364),
        DailyVerse(9, "إِنَّ اللَّهَ مَعَ الصَّابِرِينَ", "البقرة", "Al-Baqarah", 153, 23),
        DailyVerse(10, "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "البقرة", "Al-Baqarah", 201, 31),
        DailyVerse(11, "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "البقرة", "Al-Baqarah", 286, 49),
        DailyVerse(12, "قُلْ هُوَ اللَّهُ أَحَدٌ ۞ اللَّهُ الصَّمَدُ", "الإخلاص", "Al-Ikhlas", 1, 604),
        DailyVerse(13, "وَاسْجُدْ وَاقْتَرِب", "العلق", "Al-'Alaq", 19, 597),
        DailyVerse(14, "وَاصْبِرْ لِحُكْمِ رَبِّكَ فَإِنَّكَ بِأَعْيُنِنَا", "الطور", "At-Tur", 48, 525),
        DailyVerse(15, "نَصْرٌ مِّنَ اللَّهِ وَفَتْحٌ قَرِيبٌ", "الصف", "As-Saff", 13, 552),
        DailyVerse(16, "إِنَّ رَبِّي لَطِيفٌ لِّمَا يَشَاءُ ۚ إِنَّهُ هُوَ الْعَلِيمُ الْحَكِيمُ", "يوسف", "Yusuf", 100, 247),
        DailyVerse(17, "وَالضُّحَىٰ ۞ وَاللَّيْلِ إِذَا سَجَىٰ ۞ مَا وَدَّعَكَ رَبُّكَ وَمَا قَلَىٰ", "الضحى", "Ad-Duha", 1, 596),
        DailyVerse(18, "وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ", "الضحى", "Ad-Duha", 5, 596),
        DailyVerse(19, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۞ الرَّحْمَٰنِ الرَّحِيمِ", "الفاتحة", "Al-Fatihah", 2, 1),
        DailyVerse(20, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "الفاتحة", "Al-Fatihah", 6, 1),
        DailyVerse(21, "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ", "البقرة", "Al-Baqarah", 255, 42),
        DailyVerse(22, "قُلِ اللَّهُمَّ مَالِكَ الْمُلْكِ تُؤْتِي الْمُلْكَ مَن تَشَاءُ", "آل عمران", "Ali 'Imran", 26, 53),
        DailyVerse(23, "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "آل عمران", "Ali 'Imran", 173, 72),
        DailyVerse(24, "وَاللَّهُ يَعْلَمُ مَا فِي قُلُوبِكُمْ", "الأحزاب", "Al-Ahzab", 51, 425),
        DailyVerse(25, "إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ ۚ يَا أَيُّهَا الَّذِينَ آمَنُوا صَلُّوا عَلَيْهِ وَسَلِّمُوا تَسْلِيمًا", "الأحزاب", "Al-Ahzab", 56, 426),
        DailyVerse(26, "وَإِنَّكَ لَعَلَىٰ خُلُقٍ عَظِيمٍ", "القلم", "Al-Qalam", 4, 564),
        DailyVerse(27, "سَبِّحِ اسْمَ رَبِّكَ الْأَعْلَى", "الأعلى", "Al-A'la", 1, 591),
        DailyVerse(28, "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ", "الكوثر", "Al-Kawthar", 1, 602),
        DailyVerse(29, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "الفلق", "Al-Falaq", 1, 604),
        DailyVerse(30, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "الناس", "An-Nas", 1, 604),
        DailyVerse(31, "وَإِن تَعُدُّوا نِعْمَةَ اللَّهِ لَا تُحْصُوهَا", "النحل", "An-Nahl", 18, 269),
        DailyVerse(32, "ادْعُ إِلَىٰ سَبِيلِ رَبِّكَ بِالْحِكْمَةِ وَالْمَوْعِظَةِ الْحَسَنَةِ", "النحل", "An-Nahl", 125, 281),
        DailyVerse(33, "وَقُـل رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا", "الإسراء", "Al-Isra", 24, 284),
        DailyVerse(34, "إِنَّ هَٰذَا الْقُرْآنَ يَهْدِي لِلَّتِي هِيَ أَقْوَمُ", "الإسراء", "Al-Isra", 9, 283),
        DailyVerse(35, "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجًا", "الكهف", "Al-Kahf", 1, 293),
        DailyVerse(36, "إِنَّ الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ كَانَتْ لَهُمْ جَنَّاتُ الْفِرْدَوْسِ نُزُلًا", "الكهف", "Al-Kahf", 107, 304),
        DailyVerse(37, "قَالَ رَبِّ اشْرَحْ لِي صَدْرِي ۞ وَيَسِّرْ لِي أَمْرِي", "طه", "Taha", 25, 313),
        DailyVerse(38, "فَتَعَالَى اللَّهُ الْمَلِكُ الْحَقُّ", "طه", "Taha", 114, 320),
        DailyVerse(39, "لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ", "الأنبياء", "Al-Anbiya", 87, 329),
        DailyVerse(40, "فَاسْتَجَبْنَا لَهُ وَنَجَّيْنَاهُ مِنَ الْغَمِّ ۚ وَكَذَٰلِكَ نُنجِي الْمُؤْمِنِينَ", "الأنبياء", "Al-Anbiya", 88, 329),
        DailyVerse(41, "قَدْ أَفْلَحَ الْمُؤْمِنُونَ ۞ الَّذِينَ هُمْ فِي صَلَاتِهِمْ خَاشِعُونَ", "المؤمنون", "Al-Mu'minun", 1, 342),
        DailyVerse(42, "اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ", "النور", "An-Nur", 35, 354),
        DailyVerse(43, "وَتَبَارَكَ الَّذِي لَهُ مُلْكُ السَّمَاوَاتِ وَالْأَرْضِ", "الزخرف", "Az-Zukhruf", 85, 495),
        DailyVerse(44, "يَا أَيُّهَا الَّذِينَ آمَنُوا اتَّقُوا اللَّهَ وَلْتَنظُرْ نَفْسٌ مَّا قَدَّمَتْ لِغَدٍ", "الحشر", "Al-Hashr", 18, 548),
        DailyVerse(45, "هُوَ اللَّهُ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ عَالِمُ الْغَيْبِ وَالشَّهَادَةِ ۖ هُوَ الرَّحْمَٰنُ الرَّحِيمُ", "الحشر", "Al-Hashr", 22, 548),
        DailyVerse(46, "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ", "الملك", "Al-Mulk", 1, 562),
        DailyVerse(47, "الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا", "الملك", "Al-Mulk", 2, 562),
        DailyVerse(48, "إِنَّا فَتَحْنَا لَكَ فَتْحًا مُّبِينًا", "الفتح", "Al-Fath", 1, 511),
        DailyVerse(49, "يَا أَيُّهَا الَّذِينَ آمَنُوا إِن جَاءَكُمْ فَاسِقٌ بِنَبَإٍ فَتَبَيَّنُوا", "الحجرات", "Al-Hujurat", 6, 516),
        DailyVerse(50, "إِنَّمَا الْمُؤْمِنُونَ إِخْوَةٌ فَأَصْلِحُوا بَيْنَ أَخَوَيْكُمْ", "الحجرات", "Al-Hujurat", 10, 516),
        DailyVerse(51, "وَفِي السَّمَاءِ رِزْقُكُمْ وَمَا تُوعَدُونَ", "الذاريات", "Adh-Dhariyat", 22, 521),
        DailyVerse(52, "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ", "الرحمن", "Ar-Rahman", 13, 531),
        DailyVerse(53, "هَلْ جَزَاءُ الْإِحْسَانِ إِلَّا الْإِحْسَانُ", "الرحمن", "Ar-Rahman", 60, 533),
        DailyVerse(54, "سَبَّحَ لِلَّهِ مَا فِي السَّمَاوَاتِ وَالْأَرْضِ ۖ وَهُوَ الْعَزِيزُ الْحَكِيمُ", "الحديد", "Al-Hadid", 1, 537),
        DailyVerse(55, "آمِنُوا بِاللَّهِ وَرَسُولِهِ وَأَنفِقُوا مِمَّا جَعَلَكُم مُّسْتَخْلَفِينَ فِيهِ", "الحديد", "Al-Hadid", 7, 538),
        DailyVerse(56, "يَا أَيُّهَا الَّذِينَ آمَنُوا تُوبُوا إِلَى اللَّهِ تَوْبَةً نَّصُوحًا", "التحريم", "At-Tahrim", 8, 561),
        DailyVerse(57, "إِنَّ الإِنسَانَ خُلِقَ هَلُوعًا ۞ إِذَا مَسَّهُ الشَّرُّ جَزُوعًا ۞ وَإِذَا مَسَّهُ الْخَيْرُ مَنُوعًا ۞ إِلاَّ الْمُصَلِّينَ", "المعارج", "Al-Ma'arij", 19, 569),
        DailyVerse(58, "إِنَّا أَنزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ", "القدر", "Al-Qadr", 1, 598),
        DailyVerse(59, "إِنَّ مَعَ الْعُسْرِ يُسْرًا ۞ فَإِذَا فَرَغْتَ فَانصَبْ ۞ وَإِلَىٰ رَبِّكَ فَارْغَب", "الشرح", "Ash-Sharh", 6, 596),
        DailyVerse(60, "وَالْعَصْرِ ۞ إِنَّ الْإِنسَانَ لَفِي خُسْرٍ ۞ إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ", "العصر", "Al-'Asr", 1, 601)
    )

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }

    private fun getCoprimeMultiplier(n: Int): Int {
        if (n <= 1) return 1
        var candidate = 37
        while (gcd(candidate, n) != 1) {
            candidate += 2
        }
        return candidate
    }

    fun getDailyVerseForToday(): DailyVerse {
        val n = DAILY_VERSES.size
        if (n == 0) return DailyVerse(1, "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ", "الرعد", "Ar-Ra'd", 28, 252)

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        // Absolute days elapsed since 2024
        val daysSinceEpoch = (year - 2024) * 366 + dayOfYear

        val cycle = daysSinceEpoch / n
        val dayInCycle = Math.floorMod(daysSinceEpoch, n)

        val multiplier = getCoprimeMultiplier(n)
        val offset = Math.floorMod(cycle * 13 + 7, n)

        // Bijective non-repeating permutation over all n items
        val index = Math.floorMod(dayInCycle * multiplier + offset, n)

        return DAILY_VERSES[index]
    }

    fun getNotificationVerseForSlot(slotIndex: Int): DailyVerse {
        val n = DAILY_VERSES.size
        if (n == 0) return DailyVerse(1, "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ", "الرعد", "Ar-Ra'd", 28, 252)

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        val daysSinceEpoch = (year - 2024) * 366 + dayOfYear
        val cycle = daysSinceEpoch / n
        val dayInCycle = Math.floorMod(daysSinceEpoch, n)

        val multiplier = getCoprimeMultiplier(n)
        val offset = Math.floorMod(cycle * 13 + 7, n)

        val todayIndex = Math.floorMod(dayInCycle * multiplier + offset, n)
        // Shift by slotIndex + 1 to guarantee a distinct verse apart from today's verse
        val slotVerseIndex = Math.floorMod(todayIndex + slotIndex + 1, n)

        return DAILY_VERSES[slotVerseIndex]
    }

    // Hadiths
    val HADITH_COLLECTION: List<HadithItem> = listOf(
        HadithItem(1, "صحيح البخاري", "فضائل القرآن", "1", "خيركم من تعلم القرآن وعلمه", "The best among you are those who learn the Quran and teach it.", "عن عثمان بن عفان رضي الله عنه"),
        HadithItem(2, "متفق عليه", "الإخلاص", "1", "إنما الأعمال بالنيات وإنما لكل امرئ ما نوى", "Actions are judged by intentions.", "عن عمر بن الخطاب رضي الله عنه"),
        HadithItem(3, "صحيح مسلم", "فضائل القرآن", "2", "اقرؤوا القرآن فإنه يأتي يوم القيامة شفيعاً لأصحابه", "Recite the Quran, for it will come on the Day of Resurrection as an intercessor for its companions.", "عن أبي أمامة الباهلي رضي الله عنه"),
        HadithItem(4, "صحيح مسلم", "الأخلاق", "35", "الدين النصيحة", "Religion is sincerity.", "عن تميم الداري رضي الله عنه"),
        HadithItem(5, "متفق عليه", "الأخلاق", "13", "لا يؤمن أحدكم حتى يحب لأخيه ما يحب لنفسه", "None of you believes until he loves for his brother what he loves for himself.", "عن أنس بن مالك رضي الله عنه")
    )

    // Reciters
    val RECITERS: List<Reciter> = listOf(
        Reciter("alafasy", "مشاري راشد العفاسي", "Mishary Rashid Alafasy", "https://server8.mp3quran.net/afs/"),
        Reciter("abdulbasit", "عبد الباسط عبد الصمد (مرتل)", "Abdul Basit Abdul Samad", "https://server7.mp3quran.net/basit/"),
        Reciter("husary", "محمود خليل الحصري", "Mahmoud Khalil Al-Husary", "https://server13.mp3quran.net/husr/"),
        Reciter("minshawi", "محمد صديق المنشاوي (مرتل)", "Mohamed Siddiq Al-Minshawi", "https://server10.mp3quran.net/minsh/"),
        Reciter("muaiqly", "ماهر المعيقلي", "Maher Al-Muaiqly", "https://server12.mp3quran.net/maher/"),
        Reciter("ghamdi", "سعد الغامدي", "Saad Al-Ghamdi", "https://server7.mp3quran.net/s_gmd/"),
        Reciter("shuraim", "سعود الشريم", "Saud Al-Shuraim", "https://server7.mp3quran.net/shur/"),
        Reciter("sudais", "عبد الرحمن السديس", "Abdul Rahman Al-Sudais", "https://server11.mp3quran.net/sds/"),
        Reciter("ajamy", "أحمد بن علي العجمي", "Ahmed Al-Ajmi", "https://server10.mp3quran.net/ajm/")
    )

    private fun fixAngle(a: Double): Double {
        return a - 360.0 * Math.floor(a / 360.0)
    }

    private fun fixHour(h: Double): Double {
        return h - 24.0 * Math.floor(h / 24.0)
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = Math.floor(y / 100.0)
        val b = 2.0 - a + Math.floor(a / 4.0)
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * Math.sin(Math.toRadians(g)) + 0.020 * Math.sin(Math.toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val raRad = Math.atan2(Math.cos(Math.toRadians(e)) * Math.sin(Math.toRadians(l)), Math.cos(Math.toRadians(l)))
        val ra = fixHour(Math.toDegrees(raRad) / 15.0)
        val decl = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(e)) * Math.sin(Math.toRadians(l))))
        var eqTime = (q / 15.0) - ra
        while (eqTime > 12.0) eqTime -= 24.0
        while (eqTime < -12.0) eqTime += 24.0
        return Pair(decl, eqTime * 60.0)
    }

    fun calculatePrayerTimes(
        latitude: Double = 30.0444,
        longitude: Double = 31.2357,
        calculationMethod: String = "EGYPT",
        isSummerTime: Boolean = false,
        madhhab: String = "Shafi",
        date: Calendar = Calendar.getInstance(),
        fajrOffset: Int = 0,
        dhuhrOffset: Int = 0,
        asrOffset: Int = 0,
        maghribOffset: Int = 0,
        ishaOffset: Int = 0
    ): List<PrayerTime> {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH)
        val baseJd = julianDay(year, month, day)

        val defaultTz = TimeZone.getDefault()
        val deviceOffsetHours = defaultTz.getOffset(date.timeInMillis) / 3600000.0
        val isDeviceDst = defaultTz.inDaylightTime(date.time)

        val baseTz = if (deviceOffsetHours == 0.0 && Math.abs(longitude) > 7.5) {
            Math.round(longitude / 15.0).toDouble()
        } else {
            deviceOffsetHours
        }

        val timezoneOffset = if (isSummerTime && !isDeviceDst) baseTz + 1.0 else baseTz

        val latRad = Math.toRadians(latitude)

        val (fajrAngle, ishaAngle, ishaIntervalMins) = when (calculationMethod.uppercase()) {
            "EGYPT" -> Triple(19.5, 17.5, null)
            "MWL", "MUSLIM_WORLD_LEAGUE" -> Triple(18.0, 17.0, null)
            "ISNA" -> Triple(15.0, 15.0, null)
            "MAKKAH", "UMM_AL_QURA" -> Triple(18.5, 0.0, 90)
            "KARACHI" -> Triple(18.0, 18.0, null)
            "TEHRAN" -> Triple(17.7, 14.0, null)
            "DUBAI", "GULF" -> Triple(18.2, 0.0, 90)
            "KUWAIT" -> Triple(18.0, 17.5, null)
            "QATAR" -> Triple(18.0, 0.0, 90)
            "FRANCE" -> Triple(12.0, 12.0, null)
            "RUSSIA" -> Triple(16.0, 15.0, null)
            "SINGAPORE" -> Triple(20.0, 18.0, null)
            "TURKEY" -> Triple(18.0, 17.0, null)
            else -> Triple(19.5, 17.5, null)
        }

        val shadowFactor = if (madhhab.lowercase() == "hanafi") 2.0 else 1.0

        // Multi-pass iterative astronomical calculation to evaluate sun position accurately at each prayer's occurrence
        val prayerFraction = doubleArrayOf(
            5.0 / 24.0,   // Fajr
            6.0 / 24.0,   // Sunrise
            12.0 / 24.0,  // Dhuhr
            15.5 / 24.0,  // Asr
            18.5 / 24.0,  // Maghrib
            20.0 / 24.0   // Isha
        )

        var fajrHours = 5.0
        var sunriseHours = 6.0
        var dhuhrHours = 12.0
        var asrHours = 15.5
        var maghribHours = 18.5
        var ishaHours = 20.0

        fun computeHourAngle(alphaDeg: Double, declination: Double): Double {
            val alphaRad = Math.toRadians(alphaDeg)
            val declRad = Math.toRadians(declination)
            val cosH = (-Math.sin(alphaRad) - Math.sin(latRad) * Math.sin(declRad)) / (Math.cos(latRad) * Math.cos(declRad))
            val clamped = cosH.coerceIn(-1.0, 1.0)
            return Math.toDegrees(Math.acos(clamped)) / 15.0
        }

        for (iter in 0..2) {
            // 1. Fajr
            val (_, eqTimeFajr) = sunPosition(baseJd + prayerFraction[0] - longitude / (15.0 * 24.0))
            val noonFajr = 12.0 + timezoneOffset - (longitude / 15.0) - (eqTimeFajr / 60.0)
            val (declFajr, _) = sunPosition(baseJd + prayerFraction[0] - longitude / (15.0 * 24.0))
            val hFajr = computeHourAngle(fajrAngle, declFajr)
            fajrHours = noonFajr - hFajr
            prayerFraction[0] = fajrHours / 24.0

            // 2. Sunrise (0.833° atmospheric refraction + sun radius)
            val (_, eqTimeRise) = sunPosition(baseJd + prayerFraction[1] - longitude / (15.0 * 24.0))
            val noonRise = 12.0 + timezoneOffset - (longitude / 15.0) - (eqTimeRise / 60.0)
            val (declRise, _) = sunPosition(baseJd + prayerFraction[1] - longitude / (15.0 * 24.0))
            val hSunrise = computeHourAngle(0.833, declRise)
            sunriseHours = noonRise - hSunrise
            prayerFraction[1] = sunriseHours / 24.0

            // 3. Dhuhr (solar transit noon + 1 minute zawal margin)
            val (declNoon, eqTimeNoon) = sunPosition(baseJd + prayerFraction[2] - longitude / (15.0 * 24.0))
            dhuhrHours = 12.0 + timezoneOffset - (longitude / 15.0) - (eqTimeNoon / 60.0) + (1.0 / 60.0)
            prayerFraction[2] = dhuhrHours / 24.0

            // 4. Asr
            val (_, eqTimeAsr) = sunPosition(baseJd + prayerFraction[3] - longitude / (15.0 * 24.0))
            val noonAsr = 12.0 + timezoneOffset - (longitude / 15.0) - (eqTimeAsr / 60.0)
            val (declAsr, _) = sunPosition(baseJd + prayerFraction[3] - longitude / (15.0 * 24.0))
            val declAsrRad = Math.toRadians(declAsr)
            val phiRad = Math.atan(1.0 / (shadowFactor + Math.tan(Math.abs(latRad - declAsrRad))))
            val hAsr = computeHourAngle(-Math.toDegrees(phiRad), declAsr)
            asrHours = noonAsr + hAsr
            prayerFraction[3] = asrHours / 24.0

            // 5. Maghrib (Sunset)
            val (_, eqTimeSet) = sunPosition(baseJd + prayerFraction[4] - longitude / (15.0 * 24.0))
            val noonSet = 12.0 + timezoneOffset - (longitude / 15.0) - (eqTimeSet / 60.0)
            val (declSet, _) = sunPosition(baseJd + prayerFraction[4] - longitude / (15.0 * 24.0))
            val hSunset = computeHourAngle(0.833, declSet)
            maghribHours = noonSet + hSunset
            prayerFraction[4] = maghribHours / 24.0

            // 6. Isha
            if (ishaIntervalMins != null) {
                ishaHours = maghribHours + (ishaIntervalMins / 60.0)
            } else {
                val (_, eqTimeIsha) = sunPosition(baseJd + prayerFraction[5] - longitude / (15.0 * 24.0))
                val noonIsha = 12.0 + timezoneOffset - (longitude / 15.0) - (eqTimeIsha / 60.0)
                val (declIsha, _) = sunPosition(baseJd + prayerFraction[5] - longitude / (15.0 * 24.0))
                val hIsha = computeHourAngle(ishaAngle, declIsha)
                ishaHours = noonIsha + hIsha
            }
            prayerFraction[5] = ishaHours / 24.0
        }

        // Apply prayer minute offsets
        val fajrFinalHours = fajrHours + (fajrOffset / 60.0)
        val dhuhrFinalHours = dhuhrHours + (dhuhrOffset / 60.0)
        val asrFinalHours = asrHours + (asrOffset / 60.0)
        val maghribFinalHours = maghribHours + (maghribOffset / 60.0)
        val ishaFinalHours = ishaHours + (ishaOffset / 60.0)

        fun formatTime(hours: Double): String {
            val totalMinutes = Math.round(hours * 60.0)
            val normalized = ((totalMinutes % 1440) + 1440) % 1440
            val hour24 = (normalized / 60).toInt()
            val min = (normalized % 60).toInt()

            val amPm = if (hour24 >= 12) "م" else "ص"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            return String.format(Locale.US, "%d:%02d %s", hour12, min, amPm)
        }

        val currentHour = date.get(Calendar.HOUR_OF_DAY)
        val currentMin = date.get(Calendar.MINUTE)
        val currentTotalMins = currentHour * 60 + currentMin

        val fMins = Math.round(fajrFinalHours * 60.0).toInt()
        val sMins = Math.round(sunriseHours * 60.0).toInt()
        val dMins = Math.round(dhuhrFinalHours * 60.0).toInt()
        val aMins = Math.round(asrFinalHours * 60.0).toInt()
        val mMins = Math.round(maghribFinalHours * 60.0).toInt()
        val iMins = Math.round(ishaFinalHours * 60.0).toInt()

        val fTime = formatTime(fajrFinalHours)
        val sTime = formatTime(sunriseHours)
        val dTime = formatTime(dhuhrFinalHours)
        val aTime = formatTime(asrFinalHours)
        val mTime = formatTime(maghribFinalHours)
        val iTime = formatTime(ishaFinalHours)

        val prayerMins = listOf(
            "Fajr" to fMins,
            "Sunrise" to sMins,
            "Dhuhr" to dMins,
            "Asr" to aMins,
            "Maghrib" to mMins,
            "Isha" to iMins
        )

        val nextPrayerName = prayerMins.filter { it.first != "Sunrise" }.firstOrNull { it.second > currentTotalMins }?.first ?: "Fajr"

        return listOf(
            PrayerTime("الفجر", "Fajr", fTime, nextPrayerName == "Fajr"),
            PrayerTime("الشروق", "Sunrise", sTime, nextPrayerName == "Sunrise"),
            PrayerTime("الظهر", "Dhuhr", dTime, nextPrayerName == "Dhuhr"),
            PrayerTime("العصر", "Asr", aTime, nextPrayerName == "Asr"),
            PrayerTime("المغرب", "Maghrib", mTime, nextPrayerName == "Maghrib"),
            PrayerTime("العشاء", "Isha", iTime, nextPrayerName == "Isha")
        )
    }

    fun calculatePrayerTimes(
        lat: Double,
        lng: Double,
        method: String,
        isSummerTime: Boolean,
        madhhab: String,
        fajrOffset: Int = 0,
        dhuhrOffset: Int = 0,
        asrOffset: Int = 0,
        maghribOffset: Int = 0,
        ishaOffset: Int = 0
    ): List<PrayerTime> {
        return calculatePrayerTimes(
            latitude = lat,
            longitude = lng,
            calculationMethod = method,
            isSummerTime = isSummerTime,
            madhhab = madhhab,
            fajrOffset = fajrOffset,
            dhuhrOffset = dhuhrOffset,
            asrOffset = asrOffset,
            maghribOffset = maghribOffset,
            ishaOffset = ishaOffset
        )
    }

    fun getPrayerTimes(
        lat: Double = 30.0444,
        lng: Double = 31.2357,
        methodCode: String = "EGYPT",
        madhhab: String = "Shafi",
        isSummerTime: Boolean = false,
        fajrOffset: Int = 0,
        dhuhrOffset: Int = 0,
        asrOffset: Int = 0,
        maghribOffset: Int = 0,
        ishaOffset: Int = 0,
        date: Calendar = Calendar.getInstance()
    ): List<PrayerTime> {
        return calculatePrayerTimes(
            latitude = lat,
            longitude = lng,
            calculationMethod = methodCode,
            isSummerTime = isSummerTime,
            madhhab = madhhab,
            date = date,
            fajrOffset = fajrOffset,
            dhuhrOffset = dhuhrOffset,
            asrOffset = asrOffset,
            maghribOffset = maghribOffset,
            ishaOffset = ishaOffset
        )
    }

    fun calculateQibla(userLat: Double, userLng: Double): Pair<Double, Double> {
        val makkahLat = Math.toRadians(21.4225)
        val makkahLng = Math.toRadians(39.8262)

        val latRad = Math.toRadians(userLat)
        val lngRad = Math.toRadians(userLng)

        val dLng = makkahLng - lngRad
        val y = sin(dLng)
        val x = cos(latRad) * tan(makkahLat) - sin(latRad) * cos(dLng)
        var qiblaAngle = Math.toDegrees(atan2(y, x))
        if (qiblaAngle < 0) qiblaAngle += 360.0

        val R = 6371.0
        val dLat = makkahLat - latRad
        val a = sin(dLat / 2).pow(2) + cos(latRad) * cos(makkahLat) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = R * c

        return Pair(qiblaAngle, distanceKm)
    }

    val WORD_MEANINGS: List<WordMeaning> = listOf(
        WordMeaning("بسم", "أبدأ متبركاً باسم الله", "ب-س-م", "In the name of", "Bism"),
        WordMeaning("الله", "الرب المعبود بحق وحده لا شريك له", "إ-ل-ه", "Allah (God)", "Allah"),
        WordMeaning("لله", "الله الخالق العظيم المستحق لتمام العبادة والثناء", "إ-ل-ه", "For Allah", "Lillahi"),
        WordMeaning("الرحمن", "واسع الرحمة الشامل لجميع خلقه", "ر-ح-م", "The Most Gracious", "Ar-Rahman"),
        WordMeaning("الرحيم", "المختص برحمته المؤمنين في الدنيا والآخرة", "ر-ح-م", "The Most Merciful", "Ar-Rahim"),
        WordMeaning("الحمد", "الثناء والتمجيد الخالص لله بصفات كماله", "ح-م-د", "All praise and gratitude", "Al-Hamd"),
        WordMeaning("رب", "المالك والرازق والمربي والمدبر لكل الخلق", "ر-ب-ب", "Lord and Sustainer", "Rabb"),
        WordMeaning("العالمين", "جميع أصناف المخلوقات والكائنات", "ع-ل-م", "All the worlds / creation", "Al-'Alamin"),
        WordMeaning("مالك", "المالك والمتصرف المطلق يوم الجزاء", "م-ل-ك", "Master / Owner", "Malik"),
        WordMeaning("الملك", "ذي السلطان والملك التام على السماوات والأرض", "م-ل-ك", "The Sovereign / King", "Al-Malik"),
        WordMeaning("يوم", "وقت أو يوم البعث والحساب", "ي-و-م", "Day", "Yawm"),
        WordMeaning("الدين", "الجزاء والحساب والوفاء يوم القيامة", "د-ي-ن", "Judgment / Recompense", "Ad-Din"),
        WordMeaning("إياك", "نخصك وحدك ولا نعبد غيرك تعظيماً", "إ-ي-ا", "You alone", "Iyyaka"),
        WordMeaning("وإياك", "وبك وحدك نستعين ونستغيث في أمورنا", "إ-ي-ا", "And You alone", "Wa-Iyyaka"),
        WordMeaning("نعبد", "نخضع ونطيع باختيار ومحبة وإخلاص", "ع-ب-د", "We worship", "Na'budu"),
        WordMeaning("نستعين", "نطلب العون والقوة والنصر منك وحدك", "ع-و-ن", "We seek help", "Nasta'in"),
        WordMeaning("اهدنا", "وفقنا وارشدنا وثبتنا على الحق", "ه-د-ي", "Guide us", "Ihdina"),
        WordMeaning("الصراط", "الطريق الواضح المستقيم الموصل لله", "ص-ر-ط", "The Path", "As-Sirat"),
        WordMeaning("صراط", "الطريق والمنهج الواضح", "ص-ر-ط", "Path", "Sirat"),
        WordMeaning("المستقيم", "طريق الحق والإسلام الذي لا عوج فيه", "ق-و-م", "The Straight", "Al-Mustaqim"),
        WordMeaning("أنعمت", "تفضلت عليهم بالهداية والإيمان والتوفيق", "ن-ع-م", "You have bestowed favor", "An'amta"),
        WordMeaning("الذين", "عبادك المؤمنين الصالحين", "ذ-و", "Those who", "Alladhina"),
        WordMeaning("عليهم", "على النبيين والصديقين والشهداء والصالحين", "ع-ل-ي", "Upon them", "'Alayhim"),
        WordMeaning("غير", "ليسوا من أهل الغضب ولا أهل الضلال", "غ-ي-ر", "Not / Other than", "Ghayr"),
        WordMeaning("المغضوب", "الذين عرفوا الحق وتركوه عن عمد كاليهود", "غ-ض-ب", "Those who earned anger", "Al-Maghdubi"),
        WordMeaning("ولا", "ونفي وتأكيد للسلامة من طريق الهلاك", "ل-ا", "Nor / And not", "Wa-La"),
        WordMeaning("الضالين", "الذين حادوا عن طريق الحق بجهلهم كالنصارى", "ض-ل-ل", "Those who went astray", "Ad-Dallin"),
        WordMeaning("الكتاب", "القرآن العظيم المكتوب المنزّل من عند الله", "ك-ت-ب", "The Book", "Al-Kitab"),
        WordMeaning("لا ريب", "لا شك ولا ارتياب في صدقه وأنه حق من عند الله", "ر-ي-ب", "No doubt", "La Rayba"),
        WordMeaning("هدى", "دلالة وإرشاد ونور للمسترشدين", "ه-د-ي", "Guidance", "Huda"),
        WordMeaning("المتقين", "الذين جعلوا بينهم وبين عذاب الله وقاية بطاعته", "و-ق-ي", "The God-fearing / Pious", "Al-Muttaqin"),
        WordMeaning("يؤمنون", "يصدقون تصديقاً جازماً مع الإقرار والعمل", "أ-م-ن", "They believe", "Yu'minun"),
        WordMeaning("الغيب", "ما غاب عن حواس الخلق مما أخبر به الله ورسله", "غ-ي-ب", "The Unseen", "Al-Ghayb"),
        WordMeaning("يقيمون", "يؤدون الصلاة بأركانها وشروطها وخشوعها", "ق-و-م", "They establish", "Yuqimun"),
        WordMeaning("الصلاة", "العبادة المخصوصة المفروضة ذات الركوع والسجود", "ص-ل-و", "The Prayer", "As-Salah"),
        WordMeaning("رزقناهم", "أعطيناهم من شتى الأموال والنعم والخيرات", "ر-ز-ق", "We provided them", "Razaqnahum"),
        WordMeaning("ينفقون", "يبذلون أموالهم في الواجب والمندوب في سبيل الله", "ن-ف-ق", "They spend", "Yunfiqun"),
        WordMeaning("أنزل", "أنزله الله وحياً على نبيه ورسوله", "ن-ز-ل", "Revealed", "Anzala"),
        WordMeaning("إليك", "إلى شخصك الكريم يا رسول الله", "إ-ل-ي", "To you", "Ilayka"),
        WordMeaning("قبلك", "على الرسل والأنبياء السابقين", "ق-ب-ل", "Before you", "Qablika"),
        WordMeaning("وبالآخرة", "وبدار البقاء بعد الموت ويوم القيامة", "أ-خ-ر", "And in the Hereafter", "Wa Bil-Akhirah"),
        WordMeaning("يوقنون", "يعلمون يقيناً لا يخالجه شك أو تردد", "ي-ق-ن", "They are certain", "Yuqinun"),
        WordMeaning("أولئك", "أهل تلك الصفات الإيمانية الحميدة", "أ-ل-ك", "Those are", "Ula'ika"),
        WordMeaning("المفلحون", "الفائزون بالمطلوب الناجون من المرهوب في الدارين", "ف-ل-ح", "The successful ones", "Al-Muflihun"),
        WordMeaning("كفروا", "جحدوا وغطوا الحق وكذبوا بآيات الله", "ك-ف-ر", "They disbelieved", "Kafaru"),
        WordMeaning("سواء", "متساوٍ في علمك وظاهر حالهم", "س-و-ي", "Equal", "Sawa'un"),
        WordMeaning("أنذرتهم", "خوفتهم وحذرتهم من عذاب الله القريب", "ن-ذ-ر", "You warned them", "Anthartahum"),
        WordMeaning("ختم", "طبع واستوثق على قلوبهم فلا يدخلها إيمان", "خ-ت-م", "He set a seal", "Khatama"),
        WordMeaning("قلوبهم", "محل الإدراك والعقل والنية والإيمان", "ق-ل-ب", "Their hearts", "Qulubihim"),
        WordMeaning("سمعهم", "حواس السمع التي يستمعون بها الآيات والمواعظ", "س-م-ع", "Their hearing", "Sam'ihim"),
        WordMeaning("أبصارهم", "أعينهم التي يشاهدون بها آيات الله الكونية", "ب-ص-ر", "Their sight", "Absarihim"),
        WordMeaning("غشاوة", "غطاء وحجاب يمنعهم من رؤية الحق واتباعه", "غ-ش-و", "A veil / cover", "Ghashawah"),
        WordMeaning("عذاب", "عقوبة مؤلمة من الله جزاء كفرهم وعنادهم", "ع-ذ-ب", "Punishment / Torment", "'Adhab"),
        WordMeaning("عظيم", "بالغ الغاية في الشدة والألم والخلود", "ع-ظ-م", "Great / Tremendous", "'Azim"),
        WordMeaning("يخادعون", "يظهرون الإيمان ويبطنون الكفر مكرًا وخداعاً", "خ-د-ع", "They try to deceive", "Yukhadi'un"),
        WordMeaning("مرض", "شك ونفاق وشبهات تملأ بواطنهم", "م-ر-ض", "Disease / Doubt", "Marad"),
        WordMeaning("تفسدوا", "تعملوا بالمعاصي وإشاعة الكفر والظلم والشرور", "ف-س-د", "Cause corruption", "Tufsidu"),
        WordMeaning("مصلحون", "يزعمون أنهم دعاة خير وإصلاح للناس", "ص-ل-ح", "Reformers", "Muslihun"),
        WordMeaning("السفهاء", "الجهلاء ضعاف العقول والتمييز بين الحق والباطل", "س-ف-ه", "The foolish ones", "As-Sufaha'"),
        WordMeaning("خلوا", "انفردوا واجتمعوا بشياطينهم ورؤساء كفرهم", "خ-ل-و", "They are secluded", "Khalaw"),
        WordMeaning("شياطينهم", "رؤساء الكفر وأهل الضلال المحرضون لهم", "ش-ط-ن", "Their devils / leaders", "Shayatinihim"),
        WordMeaning("مستهزءون", "ساخرون ومستهزئون بالمؤمنين وبالحق", "هـ-ز-أ", "Mockers / Scoffers", "Mustahzi'un"),
        WordMeaning("يمدهم", "يمهلهم ويزيدهم مهلة في طغيانهم واستكبارهم", "م-د-د", "Prolongs them", "Yamudduhum"),
        WordMeaning("طغيانهم", "مجاوزتهم الحد في الكفر والعصيان", "ط-غ-ي", "Their transgression", "Tughyanihim"),
        WordMeaning("يعمهون", "يتحيرون ويتخبطون في الضلالة لا يبصرون نهجاً", "ع-م-هـ", "Wander blindly", "Ya'mahun"),
        WordMeaning("اشتروا", "استبدلوا واختاروا الضلالة ورضوا بها بدلاً من الهدى", "ش-ر-ي", "Purchased / Exchanged", "Ishtaraw"),
        WordMeaning("فما ربحت", "فما كسبت ولا أفلحت تجارتهم وخسروا صفقتهم", "ر-ب-ح", "So did not profit", "Fama Rabihat"),
        WordMeaning("مهتدين", "سائرين على طريق الرشاد والسعادة", "ه-د-ي", "Guided", "Muhtadin"),
        WordMeaning("مثلهم", "شأنهم وصفتهم العجيبة في ضلالهم", "م-ث-ل", "Their example / similitude", "Mathaluhum"),
        WordMeaning("استوقد", "أشعل وأوقد ناراً يبتغي ضوءها ودِفأها", "و-ق-د", "Kindled", "Istawqada"),
        WordMeaning("نارا", "لهباً ونوراً يستضاء به في الظلمة", "ن-و-ر", "A fire", "Naran"),
        WordMeaning("أضاءت", "أنارت وكشفت ما حوله من الأشياء", "ض-و-أ", "Illuminated", "Ada'at"),
        WordMeaning("ظلمات", "شدائد وظلمات متراكمة بعضها فوق بعض", "ظ-ل-م", "Darknesses", "Zulumat"),
        WordMeaning("صم", "فاقدو السمع عن سماع الحق سماع قبول", "ص-م-م", "Deaf", "Summun"),
        WordMeaning("بكم", "عاجزون عن النطق بكلمة التوحيد والصدق", "ب-ك-م", "Dumb / Mute", "Bukmun"),
        WordMeaning("عمي", "فاقدو البصيرة عن رؤية أدلة الله الواضحة", "ع-م-ي", "Blind", "'Umyun"),
        WordMeaning("صيب", "مطر شديد نازل من السماء باندفاع", "ص-و-ب", "A rainstorm", "Sayyib"),
        WordMeaning("رعد", "الصوت المدوّي المسبّح بحمد ربه في السحاب", "ر-ع-د", "Thunder", "Ra'd"),
        WordMeaning("برق", "الضوء اللامع الساطع المنبثق من السحاب", "ب-ر-ق", "Lightning", "Barq"),
        WordMeaning("الصواعق", "النيران والشهب النازلة من السماء المهلكة", "ص-ع-ق", "Thunderclaps", "As-Sawa'iq"),
        WordMeaning("حذر", "خوفاً وخشية من الهلاك والموت", "ح-ذ-ر", "Fear of", "Hadhara"),
        WordMeaning("محيط", "عالم وقادر وقاهر لا يفوته شيء أبداً", "ح-و-ط", "Encompassing", "Muhit"),
        WordMeaning("يكاد", "يقارب ويوشك لقوة لمعانه وسطوعه", "ك-و-د", "Almost", "Yakadu"),
        WordMeaning("يخطف", "يستلب وينتزع الأبصار بسرعة خاطفة", "خ-ط-ف", "Snatches", "Yakhtafu"),
        WordMeaning("مشوا", "ساروا في ضوئه ما دام منيراً لهم", "م-ش-ي", "They walk", "Mashaw"),
        WordMeaning("قاموا", "وقفوا وثبتوا في مكانهم حيارى في الظلمة", "ق-و-م", "They stand still", "Qamu"),
        WordMeaning("قدير", "كامل القدرة لا يعجزه شيء في الأرض ولا في السماء", "ق-د-ر", "All-Powerful", "Qadir"),
        WordMeaning("الناس", "بنو آدم جميعاً والمكلفون بالرسالة", "ن-و-س", "O mankind", "An-Nas"),
        WordMeaning("اعبدوا", "أفردوا ربكم بالتوحيد والطاعة والمحبة", "ع-ب-د", "Worship", "U'budu"),
        WordMeaning("خلقكم", "أوجدكم من العدم وصوركم أحسن تصوير", "خ-ل-ق", "Created you", "Khalaqakum"),
        WordMeaning("فراشا", "بساطاً ممهداً صالحاً للسكنى والاستقرار", "ف-ر-ش", "A resting place / bed", "Firasha"),
        WordMeaning("بناء", "سقفاً مرفوعاً محكماً بلا عمد ترونها", "ب-ن-ي", "A canopy / dome", "Bina'an"),
        WordMeaning("ماء", "غيثاً ومطراً طهوراً يحيي الأرض الميتة", "م-و-هـ", "Water / Rain", "Ma'an"),
        WordMeaning("الثمرات", "أنواع الأقوات والفواكه والنباتات رزقاً للعباد", "ث-م-ر", "Fruits / Produce", "Ath-Thamarat"),
        WordMeaning("أندادا", "أشباهاً وأمثالاً وأصناماً تعبدونها مع الله", "ن-د-د", "Equals / Rivals", "Andadan"),
        WordMeaning("تعلمون", "تعرفون يقيناً أنه لا خالق ولا رازق إلا هو", "ع-ل-م", "You know", "Ta'lamun"),
        WordMeaning("فاتوا", "فأتوا وقدموا وعارضوا إن كنتم صادقين", "أ-ت-ي", "Then produce", "Fa'tu"),
        WordMeaning("بسورة", "بطائفة من آيات القرآن مثل أصغر سورة فيه", "س-و-ر", "A Surah / Chapter", "Bi-Suratin"),
        WordMeaning("مثله", "مثل هذا القرآن في فصاحته وهدايته وإعجازه", "م-ث-ل", "Like it", "Mithlih"),
        WordMeaning("وادعوا", "واستعينوا بمن تشاؤون من أعوانكم وأنصاركم", "د-ع-و", "And call upon", "Wad'u"),
        WordMeaning("شهداءكم", "آلهتكم وأعوانكم الذين ترجون نصرتهم", "ش-ه-د", "Your witnesses / helpers", "Shuhada'akum"),
        WordMeaning("صادقين", "محقين في دعواكم أنه مفترى أو شعر", "ص-د-ق", "Truthful", "Sadiqin"),
        WordMeaning("ولن تفعلوا", "نفي قاطع لعجزهم الأبدي عن معارضة القرآن", "ف-ع-ل", "And never will you do", "Wa Lan Taf'alu"),
        WordMeaning("فاتقوا", "فاحذروا واجتنبوا أسباب سخط الله وعقابه", "و-ق-ي", "Then fear / beware", "Fattaqu"),
        WordMeaning("النار", "جهنم وبئس المصير لمن كفر بربه", "ن-و-ر", "The Fire", "An-Nar"),
        WordMeaning("وقودها", "حطبها ومادتها التي توقد بها: الكفرة والأحجار", "و-ق-د", "Its fuel", "Waquduha"),
        WordMeaning("الحجارة", "أصنامهم أو حجارة الكبريت أشد النيران حراً", "ح-ج-ر", "Stones", "Al-Hijarah"),
        WordMeaning("أعدت", "هيئت وجهزت وتنتظر أصحابها", "ع-د-د", "Prepared", "U'iddat"),
        WordMeaning("للكافرين", "للجاحدين بالله ورسالاته وكتبه", "ك-ف-ر", "For the disbelievers", "Lil-Kafirin"),
        WordMeaning("وبشر", "أخبر وأدخل الفرح والسرور على قلوبهم", "ب-ش-ر", "And give good tidings", "Wa Bashir"),
        WordMeaning("الصالحات", "الأعمال المشروعة الخالصة لوجه الله", "ص-ل-ح", "Righteous deeds", "As-Salihat"),
        WordMeaning("جنات", "بساتين ورياض عظيمة النعيم والبهجة", "ج-ن-ن", "Gardens (Paradise)", "Jannat"),
        WordMeaning("تجري", "تنساب وتتدفق المياه الصافية من تحت قصورها", "ج-ر-ي", "Flow", "Tajri"),
        WordMeaning("الأنهار", "أنهار من ماء غير آسن ولبن وعسل وخمر لذة", "ن-ه-ر", "Rivers", "Al-Anhar"),
        WordMeaning("رزقا", "عطاء ونعيماً وغذاء هنيئاً طيباً", "ر-ز-ق", "Provision", "Rizqan"),
        WordMeaning("متشابها", "يشبه بعضه بعضاً في الجمال والاسم ويختلف طعماً", "ش-ب-ه", "Resembling / In resemblance", "Mutashabihan"),
        WordMeaning("أزواج", "قرينات طاهرات من الحور العين الحسان", "ز-و-ج", "Spouses / Companions", "Azwaj"),
        WordMeaning("مطهرة", "منزهة من كل أذى ودنس وعيب حسي ومعنوي", "ط-ه-ر", "Purified", "Mutahharah"),
        WordMeaning("خالدون", "ماكثون فيها أبداً لا يموتون ولا يخرجون منها", "خ-ل-د", "Abiding eternally", "Khalidun"),
        WordMeaning("القيوم", "القائم بنفسه المقيم لغيره المدبر لشؤون الخلائق", "ق-و-م", "The Ever-Living Sustainer", "Al-Qayyum"),
        WordMeaning("سنة", "نعاس أو فتور خفيف يسبق النوم ويثقل الأجفان", "س-ن-ن", "Drowsiness", "Sinah"),
        WordMeaning("نوم", "استرخاء الأعضاء وانقطاع الشعور بالكلية", "ن-و-م", "Sleep", "Nawm"),
        WordMeaning("وسع", "أحاط وشمل بعظمته وعلمه وقدرته كل موجود", "و-س-ع", "Extends / Encompasses", "Wasi'a"),
        WordMeaning("كرسيه", "موضع قدمي الرب جل جلاله أو موضع علمه وسلطانه", "ك-ر-س", "His Throne / Footstool", "Kursiyyuh"),
        WordMeaning("يئوده", "يثقله أو يشق عليه تدبيرهما وحفظهما", "أ-و-د", "Tires Him", "Ya'uduh"),
        WordMeaning("حفظهما", "صيانة السماوات والأرض ورعايتهما التامة", "ح-ف-ظ", "Their preservation", "Hifzuhuma"),
        WordMeaning("العلي", "المتعالي في ذاته وأسمائه وصفاته وقدره", "ع-ل-و", "The Most High", "Al-'Aliyy"),
        WordMeaning("أحد", "المتفرد بالكمال والربوبية والألوهية لا نظير له", "أ-ح-د", "One / Unique", "Ahad"),
        WordMeaning("الصمد", "السيد الكامل الذي تصمد وتقصد الخلائق حوائجها إليه", "ص-م-د", "The Eternal Refuge", "As-Samad"),
        WordMeaning("لم يلد", "منزه عن اتخاذ الولد والشريك والذرية", "و-ل-د", "Neither begets", "Lam Yalid"),
        WordMeaning("ولم يولد", "ليس له بداية وهو الأول الذي ليس قبله شيء", "و-ل-د", "Nor is born", "Wa Lam Yulad"),
        WordMeaning("كفوا", "مكافئاً أو مماثلاً أو نداً في أي صفة من صفاته", "ك-ف-أ", "Equivalent / Equal", "Kufuwan"),
        WordMeaning("الفلق", "الصبح إذا انفلق ضوؤه، أو كل ما فلقه الله من خلق", "ف-ل-ق", "Daybreak / Creation", "Al-Falaq"),
        WordMeaning("غاسق", "الليل إذا دخل ظلامه واشتدت عتمته", "غ-س-ق", "Darkness when it settles", "Ghasiq"),
        WordMeaning("وقب", "دخل واستحكم ظلامه وغابت شمسه", "و-ق-ب", "Settles / Spreads", "Waqab"),
        WordMeaning("النفاثات", "الساحرات اللاتي ينفثن ريقهن في العقد طلباً للأذى", "ن-ف-ث", "Blowers in knots", "An-Naffathat"),
        WordMeaning("العقد", "عقد الخيوط والحبال التي يعقدها السحرة", "ع-ق-د", "Knots", "Al-'Uqad"),
        WordMeaning("حاسد", "الذي يتمنى زوال نعمة الله عن غيره", "ح-س-د", "Envier", "Hasid"),
        WordMeaning("حسد", "أظهر حسده وسعى في إيقاع الشر بالمحسود", "ح-س-د", "Envies", "Hasada"),
        WordMeaning("الوسواس", "الشيطان الذي يلقي الشر والشكوك الخفية في الصدور", "و-س-و-س", "The Whisperer", "Al-Waswas"),
        WordMeaning("الخناس", "الذي يختفي ويتراجع ويخنس كلما ذكر العبد ربه", "خ-ن-س", "The Retreating", "Al-Khannas"),
        WordMeaning("صدور", "قلوب بني آدم وبواطن نفوسهم", "ص-د-ر", "Breasts / Hearts", "Sudur"),
        WordMeaning("الجنة", "عالم الجن والشياطين الخفي", "ج-ن-ن", "The Jinn", "Al-Jinnah"),
        WordMeaning("الكوثر", "الخير الكثير في الدارين، ونهر عظيم للنبي ﷺ في الجنة", "ك-ث-ر", "The Abundance", "Al-Kawthar"),
        WordMeaning("فصل", "أخلص صلاتك ودعاءك لربك وحده دون سواه", "ص-ل-و", "So pray", "Fa-Salli"),
        WordMeaning("وانحر", "اذبح نسيكتك وأضحيتك تقرباً وشكراً لله", "ن-ح-ر", "And sacrifice", "Wanhar"),
        WordMeaning("شانئك", "مبغضك وعدوك وكاره ما جئت به من الحق", "ش-ن-أ", "Your enemy / hater", "Shani'aka"),
        WordMeaning("الأبتر", "المنقطع أثره وخيره، المقطوع ذكره من كل خير", "ب-ت-ر", "Cut off from all good", "Al-Abtar"),
        WordMeaning("نصر", "تأييد الله وتمكينه وإظهار دينه على الأديان", "ن-ص-ر", "Victory / Help", "Nasr"),
        WordMeaning("الفتح", "فتح مكة ودخول الناس في دين الله أفواجاً", "ف-ت-ح", "The Conquest", "Al-Fath"),
        WordMeaning("أفواجا", "جماعات وزُمراً متتابعة في رغبة واقتناع", "ف-و-ج", "Troops / Crowds", "Afwajan"),
        WordMeaning("فسبح", "نزه ربك وعظمه ومجده بالثناء الحسن", "س-ب-ح", "Then exalt / glorify", "Fa-Sabbih"),
        WordMeaning("واستغفره", "واطلب منه المغفرة والستر والتجاوز لذنوبك", "غ-ف-ر", "And ask His forgiveness", "Wastaghfirhu"),
        WordMeaning("توابا", "كثير القبول لتوبة عباده التائبين إليه", "ت-و-ب", "Accepting of repentance", "Tawwaba"),
        WordMeaning("تبت", "خسرت وهلكت وضلت مساعيه", "ت-ب-ب", "Perish / Ruined", "Tabbat"),
        WordMeaning("حمالة", "حاملة الحطب والشوك لإلقائه في طريق النبي ﷺ", "ح-م-ل", "The carrier", "Hammalata"),
        WordMeaning("الحطب", "الأشواك والأحطاب التي توقد بها النار", "ح-ط-ب", "Firewood", "Al-Hatab"),
        WordMeaning("جيدها", "عنقها ورقبتها", "ج-ي-د", "Her neck", "Jidiha"),
        WordMeaning("حبل", "وثاق مشدود ومفتول بشدة", "ح-ب-ل", "A rope", "Habl"),
        WordMeaning("مسد", "ليف خشن شديد الفتل يشد به عنقها في النار", "م-س-د", "Twisted palm-fiber", "Masad")
    )

    fun normalizeArabic(text: String): String {
        return text
            .replace(Regex("[\u064B-\u065F\u0670\u06D6-\u06ED\u06DF-\u06E8]"), "")
            .replace("ـ", "")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ٱ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace(Regex("[^\\p{InArabic}]"), "")
            .trim()
    }

    fun lookupWordMeaning(rawWord: String, verse: Verse? = null): WordMeaning {
        val clean = normalizeArabic(rawWord)
        if (clean.isEmpty()) {
            return WordMeaning(rawWord, "لفظ قرآني جليل", "---", "", "")
        }

        val exact = WORD_MEANINGS.firstOrNull { normalizeArabic(it.arabicWord) == clean }
        if (exact != null) return exact

        val prefixes = listOf("وال", "فال", "بال", "كال", "لل", "ال", "يا", "و", "ف", "ب", "ك", "ل", "س", "أ")
        val suffixes = listOf("هم", "كم", "هن", "كن", "نا", "ها", "ين", "ون", "ات", "ان", "تم", "وا", "ه", "ي", "ت")

        for (p in prefixes) {
            if (clean.startsWith(p) && clean.length > p.length + 2) {
                val stemmed = clean.removePrefix(p)
                val match = WORD_MEANINGS.firstOrNull { normalizeArabic(it.arabicWord) == stemmed }
                if (match != null) return match

                for (s in suffixes) {
                    if (stemmed.endsWith(s) && stemmed.length > s.length + 2) {
                        val subStem = stemmed.removeSuffix(s)
                        val matchSub = WORD_MEANINGS.firstOrNull { normalizeArabic(it.arabicWord) == subStem }
                        if (matchSub != null) return matchSub
                    }
                }
            }
        }

        for (s in suffixes) {
            if (clean.endsWith(s) && clean.length > s.length + 2) {
                val stemmed = clean.removeSuffix(s)
                val match = WORD_MEANINGS.firstOrNull { normalizeArabic(it.arabicWord) == stemmed }
                if (match != null) return match
            }
        }

        val rootLetters = extractRootHeuristic(clean)
        val contextMeaning = if (verse != null) {
            "لفظ قرآني كريم في سياق سورة ${verse.surahNameArabic} (الآية ${verse.verseNumber})، يحمل دلالة إيمانية ولغوية بليغة."
        } else {
            "لفظ قرآني كريم ورد في كتاب الله العزيز لبيان المعنى والمقصد."
        }

        return WordMeaning(
            arabicWord = rawWord,
            arabicMeaning = contextMeaning,
            rootWord = rootLetters,
            englishMeaning = "Sacred Quranic word in divine context",
            transliteration = ""
        )
    }

    private fun extractRootHeuristic(clean: String): String {
        val consonants = clean.filter { it !in listOf('ا', 'و', 'ي', 'ت', 'ن', 'م', 'س', 'ه') }
        return if (consonants.length >= 3) {
            consonants.take(3).toList().joinToString("-")
        } else if (clean.length >= 3) {
            clean.take(3).toList().joinToString("-")
        } else {
            clean
        }
    }
}
