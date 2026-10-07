package com.example.ui.theme

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import java.util.Calendar

/** هل الحركة مفعّلة في النظام؟ (إعداد «تقليل الحركة» يضبط مقياس الأنيميشن على صفر). */
@Composable
fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

/** الساعة الحالية، تُحدَّث كل دقيقة. */
@Composable
fun rememberHour(): Int {
    var hour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }
    return hour
}

/** الوضع الحالي حسب التفضيل والساعة؛ يتبدل أثناء فتح التطبيق. */
@Composable
fun rememberTimeMode(isDark: Boolean): TimeMode = resolveTimeMode(isDark, rememberHour())

@Composable
private fun Color.animatedTo(spec: AnimationSpec<Color>): Color =
    animateColorAsState(this, spec, label = "baqiyatPalette").value

@Composable
private fun animatedPalette(target: BaqiyatPalette): BaqiyatPalette {
    val spec: AnimationSpec<Color> = if (rememberMotionEnabled()) tween(500) else snap()
    return target.copy(
        background = target.background.animatedTo(spec),
        backgroundDeep = target.backgroundDeep.animatedTo(spec),
        glass = target.glass.animatedTo(spec),
        glassHighlight = target.glassHighlight.animatedTo(spec),
        ink = target.ink.animatedTo(spec),
        muted = target.muted.animatedTo(spec),
        primary = target.primary.animatedTo(spec),
        onPrimary = target.onPrimary.animatedTo(spec),
        accent = target.accent.animatedTo(spec),
        line = target.line.animatedTo(spec),
        mint = target.mint.animatedTo(spec),
        onMint = target.onMint.animatedTo(spec),
        buttonFrom = target.buttonFrom.animatedTo(spec),
        buttonTo = target.buttonTo.animatedTo(spec),
        onButton = target.onButton.animatedTo(spec),
        glow = target.glow.animatedTo(spec)
    )
}

private fun BaqiyatPalette.toColorScheme(): ColorScheme {
    val base = if (isLight) lightColorScheme() else darkColorScheme()
    val surfaceSolid = glass.compositeOver(background)
    val surfaceTint = ink.copy(alpha = 0.06f).compositeOver(surfaceSolid)
    val mintSoft = mint.copy(alpha = if (isLight) 1f else 0.18f).compositeOver(background)
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = mintSoft,
        onPrimaryContainer = ink,
        secondary = accent,
        onSecondary = onPrimary,
        secondaryContainer = mintSoft,
        onSecondaryContainer = ink,
        tertiary = accent,
        onTertiary = onPrimary,
        tertiaryContainer = mintSoft,
        onTertiaryContainer = ink,
        background = background,
        onBackground = ink,
        surface = surfaceSolid,
        onSurface = ink,
        surfaceVariant = surfaceTint,
        onSurfaceVariant = muted,
        surfaceContainerLowest = surfaceSolid,
        surfaceContainerLow = surfaceSolid,
        surfaceContainer = surfaceSolid,
        surfaceContainerHigh = surfaceTint,
        surfaceContainerHighest = surfaceTint,
        outline = line.copy(alpha = 1f),
        outlineVariant = line
    )
}

@Composable
fun BaqiyatTheme(mode: TimeMode, content: @Composable () -> Unit) {
    val palette = animatedPalette(paletteFor(mode))
    val scheme = remember(palette) { palette.toColorScheme() }
    CompositionLocalProvider(
        LocalBaqiyat provides palette,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = BaqiyatShapes,
            content = content
        )
    }
}

/** للتوافق مع الاستدعاءات القديمة (الاختبارات والمعاينات). */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    isFajrMode: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val mode = when {
        isFajrMode -> TimeMode.FAJR
        darkTheme -> TimeMode.NIGHT
        else -> TimeMode.DAY
    }
    BaqiyatTheme(mode = mode, content = content)
}
