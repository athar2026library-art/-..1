package com.example.data

/** ورد مخصص: اسم وقائمة معرّفات أذكار بترتيب اختيار المستخدم. */
data class CustomWird(val id: String, val name: String, val ids: List<Int>)

/** ترميز بسيط بالسطور والتبويب (بلا مكتبات): id \t name \t 1,2,3 */
object CustomWirds {
    const val MAX_NAME = 40

    fun cleanName(raw: String): String =
        raw.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim().take(MAX_NAME)

    fun encode(list: List<CustomWird>): String =
        list.joinToString("\n") { "${it.id}\t${cleanName(it.name)}\t${it.ids.joinToString(",")}" }

    fun decode(raw: String?): List<CustomWird> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split('\n').mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size != 3 || parts[0].isBlank() || parts[1].isBlank()) return@mapNotNull null
            CustomWird(
                id = parts[0],
                name = parts[1],
                ids = parts[2].split(',').mapNotNull { it.trim().toIntOrNull() }
            )
        }
    }

    fun newId(): String = "w" + System.currentTimeMillis().toString(36)

    fun upsert(list: List<CustomWird>, wird: CustomWird): List<CustomWird> =
        if (list.any { it.id == wird.id }) list.map { if (it.id == wird.id) wird else it } else list + wird

    /** يحوّل المعرّفات إلى أذكار بالترتيب المحفوظ، ويتجاوز المعرّفات التي لم تعد موجودة. */
    fun resolve(wird: CustomWird, all: List<Zekr>): List<Zekr> {
        val byId = all.associateBy { it.id }
        return wird.ids.mapNotNull { byId[it] }
    }
}
