package com.example.data

/**
 * أدوات بحث عربي: تتجاهل التشكيل والتطويل، وتوحّد صور الألف والياء والهاء.
 * هكذا يجد «الله» نصاً مكتوباً «اللَّهِ»، ويجد «اللهم» نصاً مكتوباً «اللَّهُمَّ».
 */
object ArabicText {
    private fun isMark(c: Char) =
        c in '\u064B'..'\u065F' || c == '\u0670' || c == '\u0640' || c in '\u06D6'..'\u06ED'

    private fun foldChar(c: Char): Char = when (c) {
        '\u0623', '\u0625', '\u0622', '\u0671' -> '\u0627' // أ إ آ ٱ → ا
        '\u0649' -> '\u064A'                                   // ى → ي
        '\u0629' -> '\u0647'                                   // ة → ه
        '\u0624' -> '\u0648'                                   // ؤ → و
        '\u0626' -> '\u064A'                                   // ئ → ي
        else -> c.lowercaseChar()
    }

    /** النص الموحّد مع خريطة من موضع كل حرف فيه إلى موضعه في النص الأصلي. */
    class Folded(val text: String, val map: IntArray)

    fun fold(s: String): Folded {
        val sb = StringBuilder(s.length)
        val map = IntArray(s.length)
        var n = 0
        for (i in s.indices) {
            val c = s[i]
            if (isMark(c)) continue
            sb.append(foldChar(c))
            map[n++] = i
        }
        return Folded(sb.toString(), map.copyOf(n))
    }

    fun normalize(s: String): String = fold(s.trim()).text

    /** مواضع تطابق الاستعلام في النص الأصلي (نهاية كل مدى تشمل حركات الحرف الأخير). */
    fun matches(text: String, query: String): List<IntRange> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        val folded = fold(text)
        val out = ArrayList<IntRange>()
        var from = 0
        while (true) {
            val idx = folded.text.indexOf(q, from)
            if (idx < 0) break
            val start = folded.map[idx]
            var end = folded.map[idx + q.length - 1] + 1
            while (end < text.length && isMark(text[end])) end++
            out.add(start until end)
            from = idx + q.length
        }
        return out
    }
}
