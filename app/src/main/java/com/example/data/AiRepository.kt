package com.example.data

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Runs a bundled Qwen instruction model on this device using LiteRT-LM.
 * No prompt or user message is sent to Gemini or any remote inference service.
 *
 * The bundled adhkar corpus is supplied to the model as grounding context; if
 * model loading fails, the app falls back to deterministic local retrieval.
 */
class AiRepository(context: Context? = null) {
    private val appContext = context?.applicationContext
    private val modelMutex = Mutex()

    @Volatile private var engine: Engine? = null
    @Volatile private var conversation: Conversation? = null

    private data class Topic(
        val id: String,
        val title: String,
        val keywords: List<String>
    )

    private val topics = listOf(
        Topic("sabah", "أذكار الصباح", listOf("الصباح", "صباح", "صبح", "اصبح")),
        Topic("masaa", "أذكار المساء", listOf("المساء", "مساء", "امسى", "امس")),
        Topic("sleep", "أذكار النوم", listOf("النوم", "نام", "قبل النوم")),
        Topic("travel", "أذكار السفر", listOf("السفر", "مسافر", "سفر", "الطريق")),
        Topic("worry", "أذكار الهم والقلق", listOf("الهم", "القلق", "حزين", "حزن", "خائف", "الخوف", "ضيق")),
        Topic("rizq", "أدعية الرزق", listOf("الرزق", "رزق", "المال", "عمل", "الوظيفة")),
        Topic("illness", "أدعية المرض والشفاء", listOf("المرض", "مريض", "شفاء", "اشف", "الصحة")),
        Topic("waking", "ذكر الاستيقاظ", listOf("استيقظ", "الاستيقاظ", "صحيت")),
        Topic("clothing", "أذكار اللباس", listOf("اللباس", "الثوب", "ملابس", "لبس")),
        Topic("toilet", "أذكار دخول الخلاء والخروج منه", listOf("الخلاء", "الحمام", "المرحاض")),
        Topic("wudu", "أذكار الوضوء", listOf("الوضوء", "توضأت", "وضوء")),
        Topic("home", "أذكار المنزل", listOf("المنزل", "البيت", "الخروج من المنزل")),
        Topic("mosque", "أذكار المسجد", listOf("المسجد", "المساجد")),
        Topic("prayer", "أذكار بعد الصلاة", listOf("الصلاة", "بعد الصلاة", "الصلاه")),
        Topic("food", "أذكار الطعام والشراب", listOf("الطعام", "الأكل", "الشراب", "اكل")),
        Topic("distress", "دعاء الكرب والشدة", listOf("الكرب", "الشدة", "مكروب", "كرب")),
        Topic("weather", "دعاء المطر", listOf("المطر")),
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

    suspend fun ask(message: String): String {
        val clean = message.trim().take(MAX_INPUT_CHARS)
        if (clean.length < 2) return "اكتب سؤالك أو صف ما تحتاج إليه من ذكر."
        if (appContext == null) return answerLocally(clean)

        return try {
            generateWithBundledModel(clean)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            val fallback = answerLocally(clean)
            "تعذر تشغيل النموذج المحلي على هذا الجهاز، لذلك عرضت أقرب نتيجة من قاعدة الأذكار المضمّنة:\n\n$fallback"
        }
    }

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

    suspend fun suggestZekrForFeeling(feeling: String): String = ask(feeling)

    private suspend fun generateWithBundledModel(input: String): String = modelMutex.withLock {
        val activeConversation = conversation ?: createConversationLocked()
        val context = relevantLocalContext(input)
        val prompt = """
            سؤال المستخدم:
            $input

            نصوص الأذكار المحلية ذات الصلة:
            $context

            أجب بالعربية الواضحة والمختصرة. عند اقتراح ذكر، انقل نصه ومصدره من السياق أعلاه فقط.
            لا تخترع حديثًا أو مصدرًا أو فضلًا، ولا تقدّم فتوى. إذا لم تتضمن البيانات جوابًا موثوقًا،
            قل بوضوح إن قاعدة التطبيق لا تحتوي على جواب كافٍ. اختم بعبارة: (إجابة آلية وليست فتوى).
        """.trimIndent()

        val messages = activeConversation.sendMessageAsync(prompt).toList()
        messages.lastOrNull()?.text?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: answerLocally(input)
    }

    private suspend fun createConversationLocked(): Conversation {
        val context = appContext ?: error("Application context is required for local model inference")
        val modelFile = withContext(Dispatchers.IO) { ensureModelFile(context) }
        val newEngine = withContext(Dispatchers.IO) {
            Engine(
                EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU()
                )
            ).also { it.initialize() }
        }
        val newConversation = newEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(SYSTEM_INSTRUCTION),
                samplerConfig = SamplerConfig(topK = 20, topP = 0.9, temperature = 0.25),
                maxOutputToken = 512
            )
        )
        engine = newEngine
        conversation = newConversation
        return newConversation
    }

    private fun ensureModelFile(context: Context): File {
        val model = File(context.filesDir, "local-ai/Qwen2_0.5B_Instruct.litertlm")
        if (model.isFile && model.length() > MIN_MODEL_BYTES) return model
        model.parentFile?.mkdirs()
        val temporary = File(model.parentFile, model.name + ".copying")
        temporary.delete()
        context.assets.open(MODEL_ASSET).buffered().use { input ->
            temporary.outputStream().buffered().use { output -> input.copyTo(output) }
        }
        if (temporary.length() < MIN_MODEL_BYTES) {
            temporary.delete()
            error("Bundled local AI model is missing or incomplete")
        }
        if (model.exists()) model.delete()
        if (!temporary.renameTo(model)) error("Could not prepare local AI model file")
        return model
    }

    private fun relevantLocalContext(input: String): String =
        selectMatches(input).joinToString("\n\n") { item ->
            buildString {
                appendLine("الذكر: ${item.text}")
                if (item.source.isNotBlank()) appendLine("المصدر: ${item.source}")
                if (item.fadl.isNotBlank()) appendLine("الملاحظة المسجلة: ${item.fadl}")
                append("التكرار: ${item.repeatCount}")
            }
        }.ifBlank { "لا توجد نتائج محلية مطابقة بوضوح." }

    private fun answerLocally(rawInput: String): String {
        val input = ArabicText.normalize(rawInput.trim().take(MAX_INPUT_CHARS))
        if (input.length < 2) return "اكتب سؤالك أو صف ما تحتاج إليه من ذكر."
        val topic = findTopic(input)
        val matches = selectMatches(input, topic)
        if (matches.isEmpty()) {
            return "لم أجد تطابقًا واضحًا في قاعدة الأذكار المحلية. جرّب السؤال عن أذكار الصباح أو المساء أو النوم أو السفر أو الهم أو المرض."
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
            append("الإجابة مستخرجة محليًا من بيانات التطبيق، دون اتصال بالإنترنت. تحقّق من المصدر عند الحاجة، وهذه ليست فتوى.")
        }
    }

    private fun selectMatches(input: String, topic: Topic? = findTopic(ArabicText.normalize(input))): List<Zekr> {
        if (topic != null) {
            val inTopic = corpus.filter { it.category == topic.id }.take(MAX_RESULTS)
            if (inTopic.isNotEmpty()) return inTopic
        }
        return rankedMatches(input)
    }

    private fun findTopic(input: String): Topic? =
        topics.firstOrNull { topic ->
            topic.keywords.any { keyword -> input.contains(ArabicText.normalize(keyword)) }
        }

    private fun rankedMatches(input: String): List<Zekr> {
        val tokens = ArabicText.normalize(input)
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
        const val MODEL_ASSET = "Qwen2_0.5B_Instruct.litertlm"
        const val MIN_MODEL_BYTES = 500_000_000L
        const val SYSTEM_INSTRUCTION =
            "أنت مساعد عربي محلي داخل تطبيق أذكار. اعتمد أولًا على السياق المحلي المرفق، " +
                "ولا تخترع آيات أو أحاديث أو مصادر أو فضائل. إذا لم تجد دليلًا في السياق فقل ذلك بوضوح. " +
                "لا تصدر فتاوى ولا تدّعِ أنك تحققت من مصدر غير مذكور."
        val STOP_WORDS = setOf(
            "هل", "كيف", "ماذا", "ما", "متى", "اين", "هذا", "هذه",
            "من", "الى", "على", "عن", "عند", "لي", "انا", "اريد",
            "ممكن", "فضلا", "اقترح", "اعطني", "اذكار", "ذكر", "دعاء"
        )
    }
}
