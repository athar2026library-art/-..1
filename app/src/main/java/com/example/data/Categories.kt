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
        AzkarCategory("sabah", "الصباح", "ابدأ يومك بنور", "sun", builtIn = true),
        AzkarCategory("masaa", "المساء", "اختم يومك بسكينة", "moon", builtIn = true),
        AzkarCategory("sleep", "النوم", "طمأنينة قبل النوم", "bed", builtIn = true),
        AzkarCategory("travel", "السفر", "حفظ وأمان", "car", builtIn = true)
    )

    private val builtInIds = builtIn.map { it.id }.toSet()
    private val topicOnlyBuiltInIds = setOf("worry", "rizq", "illness", "waking")
    private val reserved = builtInIds + topicOnlyBuiltInIds + "favorites"

    /** Search-only built-ins can be opened from results without becoming extra home tiles. */
    fun isBuiltIn(id: String) = id in builtInIds || id in topicOnlyBuiltInIds

    /** تصنيف ديناميكي يمكن فتحه من «أقسام الأذكار» (لا المحجوزة ولا المخفية). */
    fun isDynamicId(id: String) = id !in reserved && !id.startsWith("wird_") && id.isNotBlank()

    /** الأساسية أولاً ثم الديناميكية مرتبة. تُتجاهل أي وثيقة تتعارض معرّفاتها مع المحجوزة. */
    fun merge(remote: List<AzkarCategory>): List<AzkarCategory> =
        builtIn + remote.filter { isDynamicId(it.id) && it.title.isNotBlank() }
            .distinctBy { it.id }
            .sortedBy { it.order }
}
