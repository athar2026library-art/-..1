package com.example.data

/**
 * Offline assistant backed only by the adhkar data bundled in this APK.
 *
 * This is a deterministic local retrieval assistant, not a cloud model or a
 * generative LLM. It deliberately quotes stored texts and sources rather than
 * inventing religious wording or references.
 */
class AiRepository {
    private data class Topic(
        val id: String,
        val title: String,
        val keywords: List<String>
    )

    private val topics = listOf(
        Topic("sabah", "أذكار الصباح", listOf("الصباح", "صباح", "صبح", "اصبح")),
        Topic("masaa", "أذكار المساء", listOf("المساء", "مساء", "امسى", "امس")),
        Topic("sleep", "أذكار النوم", listOf("النوم", "نام", "قبل النوم", "النوم")),
        Topic("travel", "أذكار السفر", listOf("السفر", "مسافر", "سفر", "الطريق")),
        Topic("worry", "أذكار الهم والقلق", listOf("الهم", "القلق", "حزين", "حزن", "خائف", "الخوف", "ضيق")),
        Topic("rizq", "أدعية الرزق", listOf("الرزق", "رزق", "المال", "عمل", "الوظيفة")),
        Topic("illness", "أدعية المرض والشفاء", listOf("المرض", "مريض", "شفاء", "اشف", "الصحة")),
        Topic("waking", "ذكر الاستيقاظ", listOf("استيقظ", "الاستيقاظ", "صحيت", "الاستيقاظ")),
        Topic("clothing", "أذكار اللباس", listOf("اللباس", "الثوب", "ملابس", "لبس")),
        Topic("toilet", "أذكار دخول الخلاء والخروج منه", listOf("الخلاء", "الحمام", "المرحاض")),
        Topic("wudu", "أذكار الوضوء", listOf("الوضوء", "توضأت", "وضوء")),
        Topic("home", "أذكار المنزل", listOf("المنزل", "البيت", "الخروج من المنزل")),
        Topic("mosque", "أذكار المسجد", listOf("المسجد", "المساجد")),
        Topic("prayer", "أذكار بعد الصلاة", listOf("الصلاة", "بعد الصلاة", "الصلاه")),
        Topic("food", "أذكار الطعام والشراب", listOf("الطعام", "الأكل", "الشراب", "اكل")),
        Topic("distress", "دعاء الكرب والشدة", listOf("الكرب", "الشدة", "مكروب", "كرب")),
        Topic("weather", "دعاء المطر", listOf("المطر", "المطر")),
        Topic("anger", "ذكر الغضب", listOf("الغضب", "غاضب", "معصب")),
        Topic("fasting", "أذكار الصيام والإفطار", listOf("الصيام", "صائم", "الإفطار", "افطار")),
        Topic("gathering", "أذكار المجالس", listOf("المجلس", "المجالس", "كفارة المجلس")),
        Topic("children", "دعاء تحصين الأولاد", listOf("الأولاد", "الاطفال", "أطفالي", "طفلي")),
        Topic("pain", "دعاء الألم والوجع", listOf("الألم", "الوجع", "يوجع", "وجع", "ألم"))
    )

    private val corpus: List<Zekr> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        (
            AzkarData.all() +
                EverydayAzkarData.items +
                TopicAzkarData.items +
                HisnAlMuslimData.items
            )
            .filter { it.text.isNotBlank() }
            .distinctBy { ArabicText.normalize(it.text) }
    }

    suspend fun ask(message: String): String = answerLocally(message)

    suspend fun explainZekr(zekr: String): String {
        val query = ArabicText.normalize(zekr.trim().take(MAX_INPUT_CHARS))
        if (query.length < 2) return "اكتب نص الذكر الذي تريد معرفة مصدره."

        val match = corpus
            .map { item -> item to relevance(query, item) }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
            ?: return "لم أجد هذا النص ضمن الأذكار المضمّنة في التطبيق؛ لا أريد أن أنسب له مصدرًا غير متحقق منه."

        return buildString {
            appendLine(match.text)
            appendLine()
            if (match.source.isNotBlank()) appendLine("المصدر المذكور في التطبيق: ${match.source}")
            if (match.fadl.isNotBlank()) appendLine("المعنى أو الفضل المسجّل: ${match.fadl}")
            append("هذه معلومات من قاعدة الأذكار المحلية، وليست فتوى.")
        }
    }

    suspend fun suggestZekrForFeeling(feeling: String): String = answerLocally(feeling)

    private fun answerLocally(rawInput: String): String {
        val input = ArabicText.normalize(rawInput.trim().take(MAX_INPUT_CHARS))
        if (input.length < 2) return "اكتب سؤالك أو صف ما تحتاج إليه من ذكر."

        val topic = findTopic(input)
        val matches = if (topic != null) {
            corpus.filter { it.category == topic.id }
                .take(MAX_RESULTS)
                .ifEmpty { rankedMatches(input) }
        } else {
            rankedMatches(input)
        }

        if (matches.isEmpty()) {
            return "لم أجد تطابقًا واضحًا في قاعدة الأذكار المحلية. جرّب السؤال عن أذكار الصباح أو المساء أو النوم أو السفر أو الهم أو المرض، وسأعرض النص والمصدر الموجودين في التطبيق."
        }

        val heading = topic?.title ?: "أقرب الأذكار الموجودة في التطبيق"
        return buildString {
            appendLine("$heading:")
            appendLine()
            matches.forEachIndexed { index, item ->
                appendLine("${index + 1}. ${item.text}")
                appendLine("التكرار: ${item.repeatCount}")
                if (item.source.isNotBlank()) appendLine("المصدر: ${item.source}")
                if (item.fadl.isNotBlank()) appendLine("ملاحظة: ${item.fadl}")
                if (index != matches.lastIndex) appendLine()
            }
            appendLine()
            append("الإجابة مستخرجة محليًا من بيانات التطبيق، دون اتصال بـ Gemini أو بخادم ذكاء اصطناعي. تحقّق من المصدر عند الحاجة، وهذه ليست فتوى.")
        }
    }

    private fun findTopic(input: String): Topic? =
        topics.firstOrNull { topic ->
            topic.keywords.any { keyword -> input.contains(ArabicText.normalize(keyword)) }
        }

    private fun rankedMatches(input: String): List<Zekr> {
        val tokens = input
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 3 && it !in STOP_WORDS }
            .distinct()
        if (tokens.isEmpty()) return emptyList()

        return corpus
            .map { item ->
                val categoryTitle = CategoryDefaults.builtIn
                    .firstOrNull { it.id == item.category }?.title.orEmpty()
                val searchable = ArabicText.normalize(
                    "${item.text} ${item.source} ${item.fadl} $categoryTitle ${item.category}"
                )
                val score = tokens.sumOf { token ->
                    when {
                        searchable.contains(token) && ArabicText.normalize(item.text).contains(token) -> 4
                        searchable.contains(token) -> 2
                        else -> 0
                    }
                }
                item to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(MAX_RESULTS)
            .map { it.first }
    }

    private fun relevance(query: String, item: Zekr): Int {
        val text = ArabicText.normalize(item.text)
        return when {
            text == query -> 100
            text.contains(query) -> 50
            query.contains(text) && text.length > 12 -> 25
            else -> query.split(Regex("\\s+"))
                .filter { it.length >= 3 && text.contains(it) }
                .sumOf { 2 }
        }
    }

    private companion object {
        const val MAX_INPUT_CHARS = 500
        const val MAX_RESULTS = 3
        val STOP_WORDS = setOf(
            "هل", "كيف", "ماذا", "ما", "متى", "اين", "هذا", "هذه",
            "من", "الى", "على", "عن", "عند", "لي", "انا", "اريد",
            "ممكن", "فضلا", "اقترح", "اعطني", "اذكار", "ذكر", "دعاء"
        )
    }
}
