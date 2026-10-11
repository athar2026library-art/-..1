package com.example.data

/** Offline topic index for searches that describe a need rather than quote a dua. */
object TopicAzkarData {
    val items = listOf(
        Zekr(
            101,
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْجُبْنِ وَالْبُخْلِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ.",
            "صحيح البخاري",
            "دعاء ثابت للاستعاذة من الهم والحزن وما يتصل بهما.",
            1,
            "worry"
        ),
        Zekr(
            102,
            "أَذْهِبِ الْبَأْسَ رَبَّ النَّاسِ، اشْفِ وَأَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا.",
            "صحيح البخاري وصحيح مسلم",
            "دعاء نبوي يُقال عند عيادة المريض والدعاء له بالشفاء.",
            1,
            "illness"
        ),
        Zekr(
            103,
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا.",
            "سنن ابن ماجه 925",
            "دعاء مأثور يجمع سؤال العلم النافع والرزق الطيب والعمل المتقبل.",
            1,
            "rizq"
        ),
        Zekr(
            104,
            "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ.",
            "صحيح البخاري",
            "يقال عند الاستيقاظ من النوم.",
            1,
            "waking"
        )
    )

    private val aliases = mapOf(
        "sabah" to listOf("صباح", "الصباح", "صبح", "فجر"),
        "masaa" to listOf("مساء", "المساء", "عصر", "مغرب"),
        "sleep" to listOf("نوم", "النوم", "فراش", "أرق", "قبل النوم"),
        "travel" to listOf("سفر", "السفر", "مسافر", "طريق", "سيارة", "رحلة"),
        "worry" to listOf("هم", "الهم", "قلق", "خوف", "حزن", "كرب", "ضيق", "توتر", "فزع"),
        "rizq" to listOf("رزق", "الرزق", "معيشة", "عمل", "بركة", "مال"),
        "illness" to listOf("مرض", "المرض", "مريض", "شفاء", "صحة", "وجع"),
        "waking" to listOf("استيقاظ", "استيقظ", "الاستيقاظ", "صحوة", "صحوت", "بعد النوم")
    )

    val topicOnlyCategories: Set<String> = setOf("worry", "rizq", "illness", "waking")

    fun categoriesForQuery(query: String): Set<String> {
        val normalized = ArabicText.normalize(query)
        if (normalized.isBlank()) return emptySet()

        return aliases.filterValues { terms ->
            terms.any { term ->
                val normalizedTerm = ArabicText.normalize(term)
                normalizedTerm.isNotEmpty() && normalized.contains(normalizedTerm)
            }
        }.keys
    }
}
