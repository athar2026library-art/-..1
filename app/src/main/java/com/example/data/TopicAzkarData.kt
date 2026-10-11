package com.example.data

/**
 * Small offline topic index. Every entry keeps its source beside the text,
 * so search results can show the reference without a network request.
 */
object TopicAzkarData {
    val items = listOf(
        Zekr(
            id = 101,
            text = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْجُبْنِ وَالْبُخْلِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
            source = "صحيح البخاري (6369)",
            fadl = "دعاء للاستعاذة من الهم والحزن والعجز والكسل.",
            repeatCount = 1,
            category = "worry"
        ),
        Zekr(
            id = 102,
            text = "أَذْهِبِ الْبَأْسَ رَبَّ النَّاسِ، وَاشْفِ أَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا",
            source = "صحيح البخاري (5743)، وصحيح مسلم (2191a)",
            fadl = "دعاء نبوي ورد في رقية المريض.",
            repeatCount = 1,
            category = "illness"
        ),
        Zekr(
            id = 103,
            text = "رَبِّ إِنِّي لِمَا أَنْزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ",
            source = "القرآن الكريم، سورة القصص (28:24)",
            fadl = "دعاء موسى عليه السلام بطلب الخير من الله.",
            repeatCount = 1,
            category = "rizq"
        ),
        Zekr(
            id = 104,
            text = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            source = "صحيح البخاري (6312)",
            fadl = "يقال عند الاستيقاظ من النوم.",
            repeatCount = 1,
            category = "waking"
        )
    )
}
