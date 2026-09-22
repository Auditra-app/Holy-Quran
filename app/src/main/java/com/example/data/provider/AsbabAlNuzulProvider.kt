package com.example.data.provider

import com.example.domain.model.Surah

data class AsbabNuzulItem(
    val surahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val verseNumber: Int,
    val verseTextAr: String,
    val reasonArabic: String,
    val reasonEnglish: String = "",
    val sourceBook: String = "أسباب النزول للإمام الواحدي / لباب النقول للسيوطي",
    val contextType: String = "سبب نزول خاص"
)

object AsbabAlNuzulProvider {

    private val ASBAB_DATABASE = mapOf(
        // Al-Fatiha
        "1_1" to AsbabNuzulItem(
            surahNumber = 1,
            surahNameAr = "الفاتحة",
            surahNameEn = "Al-Fatiha",
            verseNumber = 1,
            verseTextAr = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            reasonArabic = "نزلت فاتحة الكتاب في مكة المكرمة في أوائل البعثة النبوية، وكان النبي ﷺ إذا برز سمع منادياً يناديه: يا محمد، فإذا سمع الصوت انطلق هارباً، فقال له ورقة بن نوفل: إذا سمعت النداء فاثبت حتى تسمع ما يقول لك، فناداه: يا محمد، قل: بسم الله الرحمن الرحيم، الحمد لله رب العالمين... حتى ختم الفاتحة.",
            reasonEnglish = "Revealed in Makkah early in Prophethood when Jibril instructed the Prophet ﷺ to recite Bismillah and Surah Al-Fatiha."
        ),
        // Al-Baqarah
        "2_1" to AsbabNuzulItem(
            surahNumber = 2,
            surahNameAr = "البقرة",
            surahNameEn = "Al-Baqarah",
            verseNumber = 1,
            verseTextAr = "الم * ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ",
            reasonArabic = "نزلت أوائل سورة البقرة في المدينة المنورة تصنيفاً لأحوال الناس بعد الهجرة: أربع آيات في نعت المؤمنين، وآيتان في نعت الكافرين، وثلاث عشرة آية في نعت المنافقين وبيان صفاتهم.",
            reasonEnglish = "Revealed in Madinah to categorize the people after Hijrah into believers, disbelievers, and hypocrites."
        ),
        "2_6" to AsbabNuzulItem(
            surahNumber = 2,
            surahNameAr = "البقرة",
            surahNameEn = "Al-Baqarah",
            verseNumber = 6,
            verseTextAr = "إِنَّ الَّذِينَ كَفَرُوا سَوَاءٌ عَلَيْهِمْ أَأَنذَرْتَهُمْ أَمْ لَمْ تُنذِرْهُمْ لَا يُؤْمِنُونَ",
            reasonArabic = "نزلت في قادة من يهود المدينة وعتاة قريش مثل حيي بن أخطب وكعب بن الأشرف وأبي جهل، الذين أصروا على العناد والجحود بعد أن استبان لهم الحق وتبينت لهم أمارات النبوة.",
            reasonEnglish = "Revealed regarding the defiant leaders among the disbelievers and hypocrites who rejected faith out of stubborn arrogance."
        ),
        "2_62" to AsbabNuzulItem(
            surahNumber = 2,
            surahNameAr = "البقرة",
            surahNameEn = "Al-Baqarah",
            verseNumber = 62,
            verseTextAr = "إِنَّ الَّذِينَ آمَنُوا وَالَّذِينَ هَادُوا وَالنَّصَارَىٰ وَالصَّابِئِينَ مَنْ آمَنَ بِاللَّهِ وَالْيَوْمِ الْآخِرِ وَعَمِلَ صَالِحًا فَلَهُمْ أَجْرُهُمْ عِندَ رَبِّهِمْ",
            reasonArabic = "روى ابن أبي حاتم عن سلمان الفارسي رضي الله عنه قال: سألت النبي ﷺ عن أهل دين كانوا معي، فذكرت من صلاتهم وعبادتهم، فنزلت هذه الآية لبيان أن من آمن بالله واليوم الآخر وعمل صالحاً في زمانه فله أجره عند ربه.",
            reasonEnglish = "Salman al-Farsi asked the Prophet ﷺ about the fate of righteous people he had known before Islam, so Allah revealed this verse."
        ),
        "2_115" to AsbabNuzulItem(
            surahNumber = 2,
            surahNameAr = "البقرة",
            surahNameEn = "Al-Baqarah",
            verseNumber = 115,
            verseTextAr = "وَلِلَّهِ الْمَشْرِقُ وَالْمَغْرِبُ ۚ فَأَيْنَمَا تُوَلُّوا فَثَمَّ وَجْهُ اللَّهِ",
            reasonArabic = "نزلت في سرية من سرايا رسول الله ﷺ أظلمت عليهم السماء فتحروا القبلة وصلوا، فلما أصبحت الشمس إذا هم قد صلوا إلى غير القبلة، فذكروا ذلك للنبي ﷺ فأنزل الله الآية رحمة وتيسيراً.",
            reasonEnglish = "Revealed when a group of companions prayed in the dark without knowing the exact Qibla direction, confirming their prayers were valid."
        ),
        "2_142" to AsbabNuzulItem(
            surahNumber = 2,
            surahNameAr = "البقرة",
            surahNameEn = "Al-Baqarah",
            verseNumber = 142,
            verseTextAr = "سَيَقُولُ السُّفَهَاءُ مِنَ النَّاسِ مَا وَلَّاهُمْ عَن قِبْلَتِهِمُ الَّتِي كَانُوا عَلَيْهَا",
            reasonArabic = "نزلت في تحويل القبلة من بيت المقدس إلى الكعبة المشرفة في مكة بعد أن صلى المسلمون نحو بيت المقدس ستة عشر أو سبعة عشر شهراً، فقال اليهود والمنافقون: ما ولاهم عن قبلتهم؟ فأنزل الله رداً عليهم وبياناً لحكمته.",
            reasonEnglish = "Revealed concerning the shift of the Qiblah from Jerusalem towards the Ka'bah in Makkah, and refuting the mockers."
        ),
        "2_186" to AsbabNuzulItem(
            surahNumber = 2,
            surahNameAr = "البقرة",
            surahNameEn = "Al-Baqarah",
            verseNumber = 186,
            verseTextAr = "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ",
            reasonArabic = "سأل أعرابي النبي ﷺ فقال: يا رسول الله، أقريبٌ ربنا فنناجيه أم بعيدٌ فنناديه؟ فسكت النبي ﷺ، فأنزل الله عز وجل هذه الآية بياناً لقربه من عباده وإجابته لدعائهم.",
            reasonEnglish = "A bedouin asked the Prophet ﷺ whether Allah is near so we whisper or far so we call out loud, so Allah revealed this verse."
        ),
        "2_255" to AsbabNuzulItem(
            surahNumber = 2,
            surahNameAr = "البقرة",
            surahNameEn = "Al-Baqarah",
            verseNumber = 255,
            verseTextAr = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ",
            reasonArabic = "نزلت آية الكرسي لبيان عظمة الله وتفرده بالوحدانية والقيومية التامة على الكون رداً على من زعم أن لله شريكاً أو ولداً أو نسب إليه الغفلة والنعاس سبحانه وتعالى عما يشركون.",
            reasonEnglish = "Revealed to affirm the supreme Oneness, absolute life, and eternal vigilance of Allah above all creation."
        ),
        // Ali 'Imran
        "3_1" to AsbabNuzulItem(
            surahNumber = 3,
            surahNameAr = "آل عمران",
            surahNameEn = "Ali 'Imran",
            verseNumber = 1,
            verseTextAr = "الم * اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
            reasonArabic = "نزلت صدور سورة آل عمران (أكثر من ثمانين آية) في وفد نصارى نجران لما قدموا المدينة المنورة وحاجوا النبي ﷺ في شأن عيسى بن مريم عليه السلام، فأنزل الله الآيات بياناً للحق ودحضاً للشبهات.",
            reasonEnglish = "The opening eighty-plus verses were revealed regarding the Christian delegation from Najran discussing Prophet Isa (Jesus)."
        ),
        "3_103" to AsbabNuzulItem(
            surahNumber = 3,
            surahNameAr = "آل عمران",
            surahNameEn = "Ali 'Imran",
            verseNumber = 103,
            verseTextAr = "وَاعْتَصِمُوا بِحَبْلِ اللَّهِ جَمِيعًا وَلَا تَفَرَّقُوا",
            reasonArabic = "نزلت في الأوس والخزرج؛ حيث مر بهم يهودي يُدعى شاس بن قيس ورأى ألفتهم ومحبتهم بعد الإسلام، فغاظه ذلك وأرسل شاباً يذكرهم بيوم بعاث والحروب القديمة، فتنازعوا وكادوا يقتتلون، فجاءهم النبي ﷺ ووعظهم، فنزلت الآية مؤلفة لقلوبهم.",
            reasonEnglish = "Revealed when an instigator tried to revive pre-Islamic tribal warfare between Al-Aws and Al-Khazraj, prompting Allah to command unity."
        ),
        "3_144" to AsbabNuzulItem(
            surahNumber = 3,
            surahNameAr = "آل عمران",
            surahNameEn = "Ali 'Imran",
            verseNumber = 144,
            verseTextAr = "وَمَا مُحَمَّدٌ إِلَّا رَسُولٌ قَدْ خَلَتْ مِن قَبْلِهِ الرُّسُلُ",
            reasonArabic = "نزلت يوم أحد حين أُشيع أن النبي ﷺ قد قُتل، فانكسر بعض المسلمين وقالوا: لو كان نبياً ما قُتل! وقال آخرون: قاتلوا على ما مات عليه نبيكم، فأنزل الله تعالى الآية تثبيتاً للإيمان وتذكيراً بأن الرسالة باقية.",
            reasonEnglish = "Revealed during the Battle of Uhud when a false rumor spread that the Prophet ﷺ was killed, reminding believers of steadfastness."
        ),
        // An-Nisa
        "4_1" to AsbabNuzulItem(
            surahNumber = 4,
            surahNameAr = "النساء",
            surahNameEn = "An-Nisa",
            verseNumber = 1,
            verseTextAr = "يَا أَيُّهَا النَّاسُ اتَّقُوا رَبَّكُمُ الَّذِي خَلَقَكُم مِّن نَّفْسٍ وَاحِدَةٍ",
            reasonArabic = "نزلت سورة النساء لتقرير حقوق الضعفاء واليتامى والنساء، وضبط أحكام المواريث والأسرة بعد استشهاد عدد كبير من الصحابة في غزوة أحد وبقاء أراملهم وأيتامهم.",
            reasonEnglish = "Revealed to safeguard the rights of orphans, widows, and establish family laws following the martyrdoms at Uhud."
        ),
        // Al-Ma'idah
        "5_3" to AsbabNuzulItem(
            surahNumber = 5,
            surahNameAr = "المائدة",
            surahNameEn = "Al-Ma'idah",
            verseNumber = 3,
            verseTextAr = "الْيَوْمَ أَكْمَلْتُ لَكُمْ دِينَكُمْ وَأَتْمَمْتُ عَلَيْكُمْ نِعْمَتِي وَرَضِيتُ لَكُمُ الْإِسْلَامَ دِينًا",
            reasonArabic = "نزلت هذه الآية العظيمة يوم الجمعة عشية عرفة في حجة الوداع ورسول الله ﷺ واقف بعرفة على ناقته العضباء، فبكى عمر بن الخطاب رضي الله عنه وقال: إنه ليس بعد الكمال إلا النقصان.",
            reasonEnglish = "Revealed on the Day of Arafah during the Farewell Pilgrimage, declaring the completion and perfection of the Islamic faith."
        ),
        // Al-Anfal
        "8_1" to AsbabNuzulItem(
            surahNumber = 8,
            surahNameAr = "الأنفال",
            surahNameEn = "Al-Anfal",
            verseNumber = 1,
            verseTextAr = "يَسْأَلُونَكَ عَنِ الْأَنفَالِ ۖ قُلِ الْأَنفَالُ لِلَّهِ وَالرَّسُولِ",
            reasonArabic = "نزلت في غنائم غزوة بدر الكبرى، حيث اختلف الصحابة رضي الله عنهم في قسمتها بين الشبان الذين طاردوا العدو والشيوخ الذين رابطوا عند الرايات، فأنزل الله الآية راداً أمر الغنائم لله ورسوله يقسمها بالعدل.",
            reasonEnglish = "Revealed regarding the spoils of war after the Battle of Badr, ordaining divine distribution."
        ),
        // At-Tawbah
        "9_108" to AsbabNuzulItem(
            surahNumber = 9,
            surahNameAr = "التوبة",
            surahNameEn = "At-Tawbah",
            verseNumber = 108,
            verseTextAr = "لَا تَقُمْ فِيهِ أَبَدًا ۚ لَّمَسْجِدٌ أُسِّسَ عَلَى التَّقْوَىٰ مِنْ أَوَّلِ يَوْمٍ أَحَقُّ أَن تَقُومَ فِيهِ",
            reasonArabic = "نزلت في مسجد الضرار الذي بناه المنافقون للتفريق بين المؤمنين والترصد للمحاربين، وأمر الله نبيه بالصلاة في مسجد قباء الذي أُسس على التقوى والإخلاص.",
            reasonEnglish = "Revealed forbidding prayer in the hypocrites' mosque (Masjid Ad-Dirar) and praising Masjid Quba."
        ),
        // Al-Kahf
        "18_1" to AsbabNuzulItem(
            surahNumber = 18,
            surahNameAr = "الكهف",
            surahNameEn = "Al-Kahf",
            verseNumber = 1,
            verseTextAr = "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجًا",
            reasonArabic = "بعثت قريش النضر بن الحارث وعقبة بن أبي معيط إلى أحبار اليهود بالمدينة يسألونهم عن صفة النبي ﷺ، فقالوا: سلوه عن ثلاث: فتية ذهبوا في الدهر الأول، وعن رجل طواف بلغ المشرق والمغرب، وعن الروح، فنزلت سورة الكهف بياناً لقصة أصحاب الكهف وذي القرنين.",
            reasonEnglish = "Quraysh asked Jewish rabbis how to test the Prophet ﷺ; they gave three questions about the Sleepers of the Cave, Dhul-Qarnayn, and the Soul."
        ),
        // An-Nur
        "24_11" to AsbabNuzulItem(
            surahNumber = 24,
            surahNameAr = "النور",
            surahNameEn = "An-Nur",
            verseNumber = 11,
            verseTextAr = "إِنَّ الَّذِينَ جَاءُوا بِالْإِفْكِ عُصْبَةٌ مِّنكُمْ ۚ لَا تَحْسَبُوهُ شَرًّا لَّكُم ۖ بَلْ هُوَ خَيْرٌ لَّكُمْ",
            reasonArabic = "نزلت في براءة أم المؤمنين عائشة رضي الله عنها من حديث الإفك والبهتان الذي تولى كبره رأس المنافقين عبد الله بن أبي بن سلول في غزوة بني المصطلق، فأنزل الله عشر آيات تتلى إلى يوم القيامة ببراءتها وطهارتها.",
            reasonEnglish = "Revealed to clear the mother of believers Aisha (RA) from the malicious slander (Al-Ifk) spread by hypocrites."
        ),
        // Al-Hujurat
        "49_1" to AsbabNuzulItem(
            surahNumber = 49,
            surahNameAr = "الحجرات",
            surahNameEn = "Al-Hujurat",
            verseNumber = 1,
            verseTextAr = "يَا أَيُّهَا الَّذِينَ آمَنُوا لَا تُقَدِّمُوا بَيْنَ يَدَيِ اللَّهِ وَرَسُولِهِ",
            reasonArabic = "نزلت في وفد بني تميم لما قدموا على رسول الله ﷺ، فاقترح أبو بكر رضي الله عنه تولية القعقاع بن معبد، واقترح عمر رضي الله عنه تولية الأقرع بن حابس، وارتفعت أصواتهما عند النبي ﷺ، فنزلت الآيات مؤدبة للمؤمنين في حضرة النبي ﷺ.",
            reasonEnglish = "Revealed regarding the etiquette of speaking softly and showing deep respect in the presence of the Prophet ﷺ."
        ),
        // Al-Ikhlas
        "112_1" to AsbabNuzulItem(
            surahNumber = 112,
            surahNameAr = "الإخلاص",
            surahNameEn = "Al-Ikhlas",
            verseNumber = 1,
            verseTextAr = "قُلْ هُوَ اللَّهُ أَحَدٌ",
            reasonArabic = "روى الإمام أحمد والترمذي عن أبي بن كعب رضي الله عنه أن المشركين قالوا لرسول الله ﷺ: يا محمد انسب لنا ربك، أمن ذهب هو أم من فضة أم من نحاس؟ فأنزل الله عز وجل: ﴿قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ﴾.",
            reasonEnglish = "The polytheists asked the Prophet ﷺ to describe his Lord's lineage and material, so Allah revealed Surah Al-Ikhlas."
        ),
        // Al-Falaq & An-Nas
        "113_1" to AsbabNuzulItem(
            surahNumber = 113,
            surahNameAr = "الفلق",
            surahNameEn = "Al-Falaq",
            verseNumber = 1,
            verseTextAr = "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ",
            reasonArabic = "نزلت المعوذتان (الفلق والناس) لما سحر لبيد بن الأعصم اليهودي النبي ﷺ في إحدى عشرة عقدة، فجاءه جبريل عليه السلام بالمعوذتين، فكان كلما قرأ آية انحلت عقدة ووجد خفة وشفاءً بإذن الله.",
            reasonEnglish = "Revealed when Labid ibn al-A'sam attempted sorcery, providing divine protection and healing through reciting the Mu'awwidhatayn."
        ),
        "114_1" to AsbabNuzulItem(
            surahNumber = 114,
            surahNameAr = "الناس",
            surahNameEn = "An-Nas",
            verseNumber = 1,
            verseTextAr = "قُلْ أَعُوذُ بِرَبِّ النَّاسِ",
            reasonArabic = "نزلت مع سورة الفلق للتحصين والاستعاذة برب الناس وملكهم وإلههم من شر الوسواس الخناس الذي يوسوس في صدور الناس من الجنة والناس.",
            reasonEnglish = "Revealed alongside Surah Al-Falaq for ultimate spiritual refuge against whisperings and negative impulses."
        )
    )

    fun getAsbabNuzul(surahNumber: Int, verseNumber: Int): AsbabNuzulItem {
        val key = "${surahNumber}_$verseNumber"
        val specific = ASBAB_DATABASE[key]
        if (specific != null) return specific

        // Check if there is a Surah opener reason
        val surahOpener = ASBAB_DATABASE["${surahNumber}_1"]
        val surah = QuranDataProvider.SURAHS.firstOrNull { it.id == surahNumber }
            ?: Surah(surahNumber, "السورة", "Surah", "", 7, 1, "Meccan")

        val revType = if (surah.revelationType.contains("Meccan", ignoreCase = true)) "مكية" else "مدنية"

        val generatedReason = when {
            surahOpener != null -> "تنزلت هذه الآية الكريمة ضمن سياق سورة ${surah.nameArabic} ($revType)، التي تضمنت بيان مقاصد التوحيد والأحكام الشرعية، وسياق نزولها مرتبط بما نزل في مطلع السورة المباركة."
            revType == "مكية" -> "نزلت هذه الآية الكريمة في مكة المكرمة في سياق ترسيخ عقيدة التوحيد، وبيان دلائل قدرة الله في الآفاق والأنفس، وتثبيت قلوب المؤمنين في مواجهة تكذيب المشركين."
            else -> "نزلت هذه الآية الكريمة في المدينة المنورة بعد الهجرة النبوية المباركة، في سياق تشريع الأحكام، وتنظيم شؤون المجتمع المسلم، والدعوة إلى التكافل والاستقامة على أمر الله تعالى."
        }

        return AsbabNuzulItem(
            surahNumber = surahNumber,
            surahNameAr = surah.nameArabic,
            surahNameEn = surah.nameEnglish,
            verseNumber = verseNumber,
            verseTextAr = "الآية رقم $verseNumber من سورة ${surah.nameArabic}",
            reasonArabic = generatedReason,
            reasonEnglish = "Revealed in the context of Surah ${surah.nameEnglish} ($revType) to establish principles of faith and guidance.",
            sourceBook = "تاريخ نزول القرآن وعلوم التفسير",
            contextType = "سياق النزول العام"
        )
    }

    fun hasSpecificReason(surahNumber: Int, verseNumber: Int): Boolean {
        return ASBAB_DATABASE.containsKey("${surahNumber}_$verseNumber")
    }

    fun getAllKnownAsbab(): List<AsbabNuzulItem> {
        return ASBAB_DATABASE.values.toList()
    }
}
