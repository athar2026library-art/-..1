package com.example.ui

import com.example.data.UserProgress
import com.example.ui.theme.AccentTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
 * منطق «رحلتي»: سلسلة الالتزام مع أيام العفو، الشارات، الخريطة الحرارية، الملخص الأسبوعي.
 * كله دوال نقية بلا اعتماد على أندرويد كي تُختبر على الجهاز المضيف.
 */

private val DATE_RE = Regex("^\\d{4}-\\d{2}-\\d{2}$")
private fun fmt() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun todayKey(): String = fmt().format(java.util.Date())

fun shiftKey(key: String, days: Int): String {
    val cal = Calendar.getInstance()
    cal.time = fmt().parse(key)!!
    cal.set(Calendar.HOUR_OF_DAY, 12) // يتجنب مشاكل منتصف الليل عند تغيير التوقيت الصيفي
    cal.add(Calendar.DAY_OF_YEAR, days)
    return fmt().format(cal.time)
}

fun UserProgress.wirdCount(): Int = listOf(completedSabah, completedMasaa, completedSleep).count { it }
fun UserProgress.isActive(): Boolean = wirdCount() > 0 || totalTasbeeh > 0

// ───────────── السلسلة وأيام العفو ─────────────

/** كل 7 أيام التزام تكسب يوم عفو (حتى يومين) يحمي السلسلة من يوم فائت. */
const val GRACE_EVERY = 7
const val MAX_GRACE = 2

data class StreakInfo(val current: Int, val longest: Int, val graceTokens: Int, val graceUsed: Int)

/**
 * يحاكي الأيام من أقدم صف إلى اليوم:
 * يوم نشط ← تزيد السلسلة، وعند مضاعفات 7 يُكسب يوم عفو.
 * يوم فائت ← يُستهلك يوم عفو إن وُجد (السلسلة تبقى ولا تزيد)، وإلا تنقطع.
 * اليوم الحالي لا يُحاسَب إن لم يُنجَز بعد.
 */
fun computeStreak(progress: List<UserProgress>, today: String = todayKey()): StreakInfo {
    val rows = progress.filter { DATE_RE.matches(it.date) }
    val first = rows.minOfOrNull { it.date } ?: return StreakInfo(0, 0, 0, 0)
    if (first > today) return StreakInfo(0, 0, 0, 0)
    val byDate = rows.associateBy { it.date }
    var day = first
    var streak = 0
    var longest = 0
    var tokens = 0
    var used = 0
    while (true) {
        if (byDate[day]?.isActive() == true) {
            streak++
            if (streak % GRACE_EVERY == 0) tokens = minOf(tokens + 1, MAX_GRACE)
            longest = maxOf(longest, streak)
        } else if (day != today) {
            if (streak > 0 && tokens > 0) {
                tokens--
                used++
            } else {
                streak = 0
                used = 0
            }
        }
        if (day == today) break
        day = shiftKey(day, 1)
    }
    return StreakInfo(streak, longest, tokens, used)
}

// ───────────── الشارات ─────────────

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val progress: Float
)

fun computeBadges(all: List<UserProgress>, info: StreakInfo): List<Badge> {
    val tasbeeh = all.sumOf { it.totalTasbeeh }
    val perfect = all.count { it.completedSabah && it.completedMasaa && it.completedSleep }
    fun badge(id: String, title: String, desc: String, value: Int, target: Int) =
        Badge(id, title, desc, value >= target, (value / target.toFloat()).coerceIn(0f, 1f))
    return listOf(
        badge("first", "الخطوة الأولى", "أول يوم التزام", all.count { it.isActive() }, 1),
        badge("week", "أسبوع ثابت", "سلسلة 7 أيام", info.longest, 7),
        badge("month", "شهر من النور", "سلسلة 30 يوماً", info.longest, 30),
        badge("sabah10", "صباح ثابت", "10 أيام أذكار صباح", all.count { it.completedSabah }, 10),
        badge("masaa10", "مساء ثابت", "10 أيام أذكار مساء", all.count { it.completedMasaa }, 10),
        badge("sleep10", "نوم بسكينة", "10 أيام أذكار نوم", all.count { it.completedSleep }, 10),
        badge("perfect7", "ورد كامل", "7 أيام أكملت فيها الصباح والمساء والنوم", perfect, 7),
        badge("tasbih1k", "ألف تسبيحة", "إجمالي 1000 تسبيحة", tasbeeh, 1000),
        badge("tasbih10k", "سيد الذكر", "إجمالي 10000 تسبيحة", tasbeeh, 10000)
    )
}

fun unlockedAccents(badges: List<Badge>): Set<AccentTheme> {
    val open = badges.filter { it.unlocked }.map { it.id }.toSet()
    return AccentTheme.entries.filter { it.unlockBadge == null || it.unlockBadge in open }.toSet()
}

// ───────────── الخريطة الحرارية ─────────────

/** 0 = لا شيء، 1 = تسبيح فقط أو ورد واحد، 2 = وردان، 3 = ثلاثة أوراد. */
fun heatLevel(p: UserProgress?): Int {
    if (p == null) return 0
    val w = p.wirdCount()
    return if (w == 0 && p.totalTasbeeh > 0) 1 else w.coerceAtMost(3)
}

/**
 * شبكة أسابيع بترتيب الأعمدة: الفهرس = عمود*7 + صف، والصف 0 = السبت.
 * آخر خلية غير فارغة هي اليوم؛ ما بعدها null.
 */
fun heatmapDates(today: String, weeks: Int = 12): List<String?> {
    val cal = Calendar.getInstance().apply { time = fmt().parse(today)!! }
    val dowFromSaturday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7
    val total = (weeks - 1) * 7 + dowFromSaturday + 1
    return List(weeks * 7) { i -> if (i < total) shiftKey(today, i - (total - 1)) else null }
}

fun dayDetails(key: String, p: UserProgress?): String {
    fun mark(b: Boolean?) = if (b == true) "✓" else "✗"
    return "$key — الصباح ${mark(p?.completedSabah)}  المساء ${mark(p?.completedMasaa)}  النوم ${mark(p?.completedSleep)}  ·  ${p?.totalTasbeeh ?: 0} تسبيحة"
}

// ───────────── الملخص الأسبوعي ─────────────

data class WeeklySummary(
    val activeDays: Int,
    val tasbeeh: Int,
    val wirds: Int,
    val perfectDays: Int,
    /** أيام أُنجز فيها الصباح والمساء معاً (هدف الأسبوع). */
    val fullDays: Int
)

fun weeklySummary(all: List<UserProgress>, today: String = todayKey()): WeeklySummary {
    val keys = (0..6).map { shiftKey(today, -it) }.toSet()
    val rows = all.filter { it.date in keys }
    return WeeklySummary(
        activeDays = rows.count { it.isActive() },
        tasbeeh = rows.sumOf { it.totalTasbeeh },
        wirds = rows.sumOf { it.wirdCount() },
        perfectDays = rows.count { it.wirdCount() == 3 },
        fullDays = rows.count { it.completedSabah && it.completedMasaa }
    )
}

fun summaryText(s: WeeklySummary, streak: Int): String = buildString {
    append("هذا الأسبوع في الباقيات\n\n")
    append("${s.activeDays} أيام التزام من 7\n")
    append("${s.wirds} ورداً مكتملاً\n")
    append("${s.tasbeeh} تسبيحة")
    if (streak > 0) append("\nسلسلة $streak يوماً")
}
