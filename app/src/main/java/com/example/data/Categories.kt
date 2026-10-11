package com.example.data

/** تصنيف أذكار. الأربعة الأساسية مضمّنة، والباقي يأتي من Firestore (content/categories/items). */
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
        AzkarCategory("waking", "الاستيقاظ", "ذكر بداية اليوم", "sun", order = 7, builtIn = true)
    )

    private val builtInIds = builtIn.map { it.id }.toSet()
    private val reserved = builtInIds + "favorites"

    fun isBuiltIn(id: String) = id in builtInIds

    /** تصنيف ديناميكي يمكن فتحه من «أقسام الأذكار» (لا المحجوزة ولا المخفية). */
    fun isDynamicId(id: String) = id !in reserved && !id.startsWith("wird_") && id.isNotBlank()

    /** الأساسية أولاً ثم الديناميكية مرتبة. تُتجاهل أي وثيقة تتعارض معرّفاتها مع المحجوزة. */
    fun merge(remote: List<AzkarCategory>): List<AzkarCategory> =
        builtIn + remote.filter { isDynamicId(it.id) && it.title.isNotBlank() }
            .distinctBy { it.id }
            .sortedBy { it.order }
}
