package com.example.ui

enum class WirdSlot { SABAH, MASAA, SLEEP, FREE }

/** اقتراح «وردك الآن». category = null يعني: افتح المسبحة. */
data class WirdSuggestion(
    val slot: WirdSlot,
    val category: String?,
    val done: Boolean,
    val title: String,
    val subtitle: String
)

/**
 * يقترح الورد المناسب للساعة.
 * (الساعات ثابتة مؤقتاً، وتُستبدل بمواقيت الصلاة في المرحلة 7)
 */
fun suggestWird(hour: Int, sabahDone: Boolean, masaaDone: Boolean, sleepDone: Boolean = false): WirdSuggestion = when {
    hour in 3..11 -> sabah(sabahDone)
    hour in 12..14 -> if (!sabahDone) {
        WirdSuggestion(WirdSlot.SABAH, "sabah", false, "أذكار الصباح", "ما زال بإمكانك إتمامها، والله يقبل منك.")
    } else {
        WirdSuggestion(WirdSlot.FREE, null, false, "وقت للتسبيح", "سبّح بما تيسّر، وتابع عدّك في المسبحة.")
    }
    hour in 15..20 -> if (masaaDone) {
        WirdSuggestion(WirdSlot.MASAA, "masaa", true, "أذكار المساء", "أتممت ورد المساء اليوم، تقبّل الله.")
    } else {
        WirdSuggestion(WirdSlot.MASAA, "masaa", false, "أذكار المساء", "اختم يومك بسكينة، دقائق قليلة تكفي.")
    }
    sleepDone -> WirdSuggestion(WirdSlot.SLEEP, "sleep", true, "أذكار النوم", "أتممت أذكار النوم، نم قرير العين.")
    else -> WirdSuggestion(WirdSlot.SLEEP, "sleep", false, "أذكار النوم", "طمأنينة قبل النوم.")
}

private fun sabah(done: Boolean) = if (done) {
    WirdSuggestion(WirdSlot.SABAH, "sabah", true, "أذكار الصباح", "أتممت ورد الصباح اليوم، تقبّل الله.")
} else {
    WirdSuggestion(WirdSlot.SABAH, "sabah", false, "أذكار الصباح", "ابدأ يومك بنور، دقائق قليلة تكفي.")
}
