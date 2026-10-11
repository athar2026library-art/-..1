package com.example.data

/**
 * Small offline topic index. Texts and source labels are kept together so search
 * results never detach a dua from its stated source.
 */
object TopicAzkarData {
    val items = listOf(
        Zekr(
            id = 101,
            text = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
            source = "صحيح البخاري",
            repeatCount = 1,
            category = "sleep"
        ),
        Zekr(
            id = 102,
            text = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            source = "صحيح البخاري",
            repeatCount = 1,
            category = "waking"
        ),
        Zekr(
            id = 103,
            text = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ ۝ وَإِنَّا إِلَى رَبِّنَا لَمُنْقَلِبُونَ",
            source = "القرآن الكريم، سورة الزخرف 13-14؛ ويقال في دعاء السفر",
            repeatCount = 1,
            category = "travel"
        ),
        Zekr(
            id = 104,
            text = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْجُبْنِ وَالْبُخْلِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
            source = "صحيح البخاري",
            repeatCount = 1,
            category = "worry"
        ),
        Zekr(
            id = 105,
            text = "أَذْهِبِ الْبَأْسَ رَبَّ النَّاسِ، اشْفِ وَأَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا",
            source = "صحيح البخاري وصحيح مسلم؛ دعاء للمريض",
            repeatCount = 1,
            category = "illness"
        ),
        Zekr(
            id = 106,
            text = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا",
            source = "سنن ابن ماجه 925؛ صححه الألباني",
            repeatCount = 1,
            category = "rizq"
        )
    )
}
