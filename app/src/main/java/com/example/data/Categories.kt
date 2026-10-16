package com.example.data

/** تصنيف أذكار. التصنيفات الأساسية ومناسبات الذكر اليومية مضمّنة، والإضافات الإدارية تأتي من Firestore. */
data class AzkarCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconKey: String,
    val order: Int = 0,
    val builtIn: Boolean = false
)

object CategoryDefaults {
    val builtIn = listOf(
        AzkarCategory("sabah", "الصباح", "ابدأ يومك بنور", "sun", order = 0, builtIn = true),
        AzkarCategory("masaa", "المساء", "اختم يومك بسكينة", "moon", order = 1, builtIn = true),
        AzkarCategory("sleep", "النوم", "طمأنينة قبل النوم", "bed", order = 2, builtIn = true),
        AzkarCategory("travel", "السفر", "حفظ وأمان", "car", order = 3, builtIn = true),
        AzkarCategory("worry", "الهم والقلق", "أذكار عند الضيق والخوف", "heart", order = 4, builtIn = true),
        AzkarCategory("rizq", "الرزق", "دعاء بسعة الخير", "food", order = 5, builtIn = true),
        AzkarCategory("illness", "المرض والشفاء", "دعاء للمريض", "heart", order = 6, builtIn = true),
        AzkarCategory("waking", "الاستيقاظ", "ذكر بداية اليوم", "sun", order = 7, builtIn = true),
        AzkarCategory("clothing", "الملابس", "أذكار اللباس", "book", order = 8, builtIn = true),
        AzkarCategory("toilet", "دخول الخلاء والخروج منه", "أذكار قضاء الحاجة", "home", order = 9, builtIn = true),
        AzkarCategory("wudu", "الوضوء", "الذكر بعد الوضوء", "shield", order = 10, builtIn = true),
        AzkarCategory("home", "المنزل", "الذكر عند الدخول والخروج", "home", order = 11, builtIn = true),
        AzkarCategory("mosque", "المسجد", "أذكار الدخول والخروج", "mosque", order = 12, builtIn = true),
        AzkarCategory("prayer", "بعد الصلاة", "أذكار ما بعد السلام", "book", order = 13, builtIn = true),
        AzkarCategory("food", "الطعام والشراب", "الذكر قبل الطعام وبعده", "food", order = 14, builtIn = true),
        AzkarCategory("distress", "الكرب والشدة", "أدعية عند الكرب", "heart", order = 15, builtIn = true),
        AzkarCategory("weather", "المطر", "أدعية المطر", "rain", order = 16, builtIn = true),
        AzkarCategory("anger", "الغضب", "ما يقال عند الغضب", "heart", order = 17, builtIn = true),
        AzkarCategory("fasting", "الصيام والإفطار", "ذكر الإفطار", "moon", order = 18, builtIn = true),
        AzkarCategory("gathering", "المجالس والآداب", "كفارة المجلس", "star", order = 19, builtIn = true),
        AzkarCategory("children", "تحصين الأولاد", "دعاء التعويذ", "shield", order = 20, builtIn = true),
        AzkarCategory("pain", "الألم والوجع", "دعاء عند ألم الجسد", "heart", order = 21, builtIn = true),
    )

    private val builtInIds = builtIn.map { it.id }.toSet()
    private val reserved = builtInIds + setOf("favorites", "hisn")

    fun isBuiltIn(id: String) = id in builtInIds || id == "hisn" || HisnAlMuslimData.isChapterId(id)

    /** تصنيف ديناميكي يمكن فتحه من «أقسام الأذكار» (لا المحجوزة ولا المخفية). */
    fun isDynamicId(id: String) = id !in reserved && !id.startsWith("wird_") && !id.startsWith("hisn_") && id.isNotBlank()

    /** الأساسية أولاً ثم الديناميكية مرتبة. تُتجاهل أي وثيقة تتعارض معرّفاتها مع المحجوزة. */
    fun merge(remote: List<AzkarCategory>): List<AzkarCategory> =
        builtIn + remote.filter { isDynamicId(it.id) && it.title.isNotBlank() }
            .distinctBy { it.id }
            .sortedBy { it.order }
}
