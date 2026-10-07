package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * أوقات اليوم الأربعة. النهار فاتح، والباقي داكن بدرجات مختلفة.
 * المصدر المرجعي للقيم: design/tokens.json
 */
enum class TimeMode { DAY, FAJR, MAGHRIB, NIGHT }

/**
 * يحدد الوضع حسب تفضيل المستخدم والساعة.
 * الوضع الفاتح يبقى نهاراً دائماً. الوضع الداكن يتبدل بين فجر ومغرب وليل.
 * (ستُستبدل الساعات الثابتة بمواقيت الصلاة في المرحلة 7)
 */
fun resolveTimeMode(isDark: Boolean, hour: Int): TimeMode = when {
    !isDark -> TimeMode.DAY
    hour in 3..6 -> TimeMode.FAJR
    hour in 17..18 -> TimeMode.MAGHRIB
    else -> TimeMode.NIGHT
}

@Immutable
data class BaqiyatPalette(
    val isLight: Boolean,
    val background: Color,
    val backgroundDeep: Color,
    /** سطح زجاجي شبه شفاف يوضع فوق الخلفية. */
    val glass: Color,
    val glassHighlight: Color,
    val ink: Color,
    val muted: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val line: Color,
    val mint: Color,
    val onMint: Color,
    val buttonFrom: Color,
    val buttonTo: Color,
    val onButton: Color,
    val glow: Color,
    val success: Color,
    val warning: Color
)

val DayPalette = BaqiyatPalette(
    isLight = true,
    background = Color(0xFFF1F3E8),
    backgroundDeep = Color(0xFFE4EAD8),
    glass = Color(0xB8FFFFFF),
    glassHighlight = Color(0xCCFFFFFF),
    ink = Color(0xFF16362A),
    muted = Color(0xFF4F6A5D),
    primary = Color(0xFF1B4B3A),
    onPrimary = Color(0xFFF8F6EA),
    accent = Color(0xFFB28A22),
    line = Color(0x291B4B3A),
    mint = Color(0xFFDCE7D3),
    onMint = Color(0xFF16362A),
    buttonFrom = Color(0xFF1B4B3A),
    buttonTo = Color(0xFF2F6B52),
    onButton = Color(0xFFF8F6EA),
    glow = Color(0x47B28A22),
    success = Color(0xFF2E7D32),
    warning = Color(0xFFB26A00)
)

val FajrPalette = BaqiyatPalette(
    isLight = false,
    background = Color(0xFF0D2A30),
    backgroundDeep = Color(0xFF081C21),
    glass = Color(0x12FFFFFF),
    glassHighlight = Color(0x2EFFFFFF),
    ink = Color(0xFFEAF2EE),
    muted = Color(0xFFA9C5C0),
    primary = Color(0xFF8FD3C2),
    onPrimary = Color(0xFF0D2A30),
    accent = Color(0xFFE2C968),
    line = Color(0x3D8FD3C2),
    mint = Color(0xFFB9DDD3),
    onMint = Color(0xFF0D2A30),
    buttonFrom = Color(0xFF8FD3C2),
    buttonTo = Color(0xFF5FB5A3),
    onButton = Color(0xFF0D2A30),
    glow = Color(0x38E2C968),
    success = Color(0xFF8FD3A0),
    warning = Color(0xFFF0B45A)
)

val MaghribPalette = BaqiyatPalette(
    isLight = false,
    background = Color(0xFF262A1B),
    backgroundDeep = Color(0xFF1A1C12),
    glass = Color(0x12FFF4D6),
    glassHighlight = Color(0x2EFFF4D6),
    ink = Color(0xFFF7ECD3),
    muted = Color(0xFFCDBF9C),
    primary = Color(0xFFEDB84F),
    onPrimary = Color(0xFF2A220F),
    accent = Color(0xFFF0C96A),
    line = Color(0x42F0C96A),
    mint = Color(0xFFE6D8A8),
    onMint = Color(0xFF2A220F),
    buttonFrom = Color(0xFFF0C96A),
    buttonTo = Color(0xFFD49A33),
    onButton = Color(0xFF2A220F),
    glow = Color(0x42EDB84F),
    success = Color(0xFFA6D58A),
    warning = Color(0xFFF2A65A)
)

val NightPalette = BaqiyatPalette(
    isLight = false,
    background = Color(0xFF0F2920),
    backgroundDeep = Color(0xFF081A13),
    glass = Color(0x12FFFFFF),
    glassHighlight = Color(0x2EFFFFFF),
    ink = Color(0xFFF4F2E7),
    muted = Color(0xFFB4C7B9),
    primary = Color(0xFFE2C968),
    onPrimary = Color(0xFF1A2A1F),
    accent = Color(0xFFE2C968),
    line = Color(0x38E2C968),
    mint = Color(0xFFB9D4B6),
    onMint = Color(0xFF1A2A1F),
    buttonFrom = Color(0xFFE2C968),
    buttonTo = Color(0xFFC9A63D),
    onButton = Color(0xFF1A2A1F),
    glow = Color(0x33E2C968),
    success = Color(0xFF9AD59A),
    warning = Color(0xFFF0B45A)
)

fun paletteFor(mode: TimeMode): BaqiyatPalette = when (mode) {
    TimeMode.DAY -> DayPalette
    TimeMode.FAJR -> FajrPalette
    TimeMode.MAGHRIB -> MaghribPalette
    TimeMode.NIGHT -> NightPalette
}

val LocalBaqiyat = staticCompositionLocalOf { NightPalette }

/** وصول مختصر: `Baqiyat.colors.accent` */
object Baqiyat {
    val colors: BaqiyatPalette
        @androidx.compose.runtime.Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalBaqiyat.current
}
