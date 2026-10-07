package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Baqiyat
import com.example.ui.theme.MihrabShape
import com.example.ui.theme.rememberMotionEnabled

// ───────────────────────── الحركة ─────────────────────────

/** انكماش خفيف عند الضغط (ربيعي). يُعطَّل عند تفعيل «تقليل الحركة». */
fun Modifier.pressScale(source: MutableInteractionSource, pressedScale: Float = 0.975f): Modifier = composed {
    val motion = rememberMotionEnabled()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ───────────────────────── الخلفية ─────────────────────────

/**
 * خلفية التطبيق: تدرج رأسي، وهج علوي، ونقش هندسي (نجمة من مربعين) بشفافية منخفضة جداً.
 * تُوضع مرة واحدة في جذر التطبيق، وتجعل الشاشات شفافة فوقها.
 */
@Composable
fun BaqiyatBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val p = Baqiyat.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.background, p.backgroundDeep)))
            .drawWithCache {
                val tile = 76.dp.toPx()
                val r = tile * 0.30f
                val d = r * 1.4142f
                val path = Path()
                val cols = (size.width / tile).toInt() + 2
                val rows = (size.height / tile).toInt() + 2
                for (i in 0 until cols) {
                    for (j in 0 until rows) {
                        val cx = i * tile
                        val cy = j * tile
                        path.moveTo(cx - r, cy - r)
                        path.lineTo(cx + r, cy - r)
                        path.lineTo(cx + r, cy + r)
                        path.lineTo(cx - r, cy + r)
                        path.close()
                        path.moveTo(cx, cy - d)
                        path.lineTo(cx + d, cy)
                        path.lineTo(cx, cy + d)
                        path.lineTo(cx - d, cy)
                        path.close()
                    }
                }
                val center = Offset(size.width / 2f, 0f)
                val glowRadius = size.width * 0.95f
                val glow = Brush.radialGradient(listOf(p.glow, Color.Transparent), center = center, radius = glowRadius)
                val stroke = Stroke(width = 1.dp.toPx())
                val patternColor = p.accent.copy(alpha = if (p.isLight) 0.07f else 0.05f)
                onDrawBehind {
                    drawCircle(brush = glow, radius = glowRadius, center = center)
                    drawPath(path, color = patternColor, style = stroke)
                }
            },
        content = content
    )
}

// ───────────────────────── البطاقات ─────────────────────────

/** بطاقة زجاجية: سطح شبه شفاف مع حد متدرج (أفتح من الأعلى). */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = Baqiyat.colors
    val source = remember { MutableInteractionSource() }
    val base = if (onClick != null) modifier.pressScale(source) else modifier
    Column(
        modifier = base
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(p.glass.copy(alpha = (p.glass.alpha + 0.05f).coerceAtMost(1f)), p.glass))
            )
            .border(1.dp, Brush.verticalGradient(listOf(p.glassHighlight, p.line)), shape)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = source,
                    indication = null,
                    onClickLabel = clickLabel,
                    role = Role.Button,
                    onClick = onClick
                ) else Modifier
            )
            .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

/** بطاقة المحراب: لبطاقة الذكر فقط. المحتوى يضع الهوامش بنفسه (يترك أعلى القوس فارغاً). */
@Composable
fun MihrabCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = Baqiyat.colors
    val shape = remember { MihrabShape() }
    val source = remember { MutableInteractionSource() }
    val base = if (onClick != null) modifier.pressScale(source, 0.985f) else modifier
    Column(
        modifier = base
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(p.glass.copy(alpha = (p.glass.alpha + 0.06f).coerceAtMost(1f)), p.glass))
            )
            .border(1.5.dp, Brush.verticalGradient(listOf(p.accent, p.accent.copy(alpha = 0.35f))), shape)
            .drawBehind {
                // قوس داخلي رفيع يعطي عمق المحراب
                val inset = 14.dp.toPx()
                val inner = MihrabShape(20f).createOutline(
                    Size(size.width - inset * 2, size.height - inset * 2), layoutDirection, this
                )
                translate(inset, inset) {
                    drawOutline(inner, color = p.line, style = Stroke(width = 1.dp.toPx()))
                }
            }
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = source,
                    indication = null,
                    onClickLabel = clickLabel,
                    role = Role.Button,
                    onClick = onClick
                ) else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

// ───────────────────────── الأزرار ─────────────────────────

enum class BaqiyatButtonStyle { Primary, Glass, Text }

@Composable
fun BaqiyatButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BaqiyatButtonStyle = BaqiyatButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp
) {
    val p = Baqiyat.colors
    val shape = RoundedCornerShape(50)
    val source = remember { MutableInteractionSource() }
    val contentColor = when (style) {
        BaqiyatButtonStyle.Primary -> p.onButton
        BaqiyatButtonStyle.Glass -> p.ink
        BaqiyatButtonStyle.Text -> p.primary
    }
    val look = when (style) {
        BaqiyatButtonStyle.Primary -> Modifier
            .background(Brush.linearGradient(listOf(p.buttonFrom, p.buttonTo)))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)), shape)
        BaqiyatButtonStyle.Glass -> Modifier
            .background(p.glass)
            .border(1.dp, p.accent, shape)
        BaqiyatButtonStyle.Text -> Modifier
    }
    Row(
        modifier = modifier
            .heightIn(min = height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .pressScale(source, 0.97f)
            .clip(shape)
            .then(look)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 28.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center)
    }
}

@Composable
fun BaqiyatChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val p = Baqiyat.colors
    val shape = RoundedCornerShape(50)
    val bg = if (selected) p.mint.copy(alpha = if (p.isLight) 1f else 0.22f) else p.glass
    val borderColor = if (selected) p.accent else p.line
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.55f }
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = p.ink, style = MaterialTheme.typography.labelLarge)
    }
}

// ───────────────────────── الحلقة ─────────────────────────

/** حلقة تقدم. تتحرك دائماً حتى مع «تقليل الحركة» لأنها تنقل معلومة (العدّاد). */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    stroke: Dp = 5.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val p = Baqiyat.colors
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "ring"
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2f
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(
                color = p.line,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = w)
            )
            if (animated > 0.001f) {
                drawArc(
                    brush = Brush.linearGradient(listOf(p.buttonFrom, p.accent)),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = w, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

// ───────────────────────── الشريط السفلي العائم ─────────────────────────

data class BaqiyatNavItem(val label: String, val icon: ImageVector)

/** شريط تنقل عائم بمؤشر يتحرك بين العناصر. */
@Composable
fun FloatingNavBar(
    items: List<BaqiyatNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = Baqiyat.colors
    val barShape = RoundedCornerShape(50)
    val solid = p.glass.compositeOver(p.background)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val itemWidth = (maxWidth - 12.dp) / items.size
        val indicatorX by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "navIndicator"
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = if (p.isLight) 14.dp else 0.dp,
                    shape = barShape,
                    ambientColor = p.primary.copy(alpha = 0.2f),
                    spotColor = p.primary.copy(alpha = 0.25f)
                )
                .clip(barShape)
                .background(solid)
                .border(1.dp, Brush.verticalGradient(listOf(p.glassHighlight, p.line)), barShape)
                .padding(6.dp)
        ) {
            Box(
                Modifier
                    .offset(x = indicatorX)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(barShape)
                    .background(Brush.linearGradient(listOf(p.buttonFrom, p.buttonTo)))
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(if (selected) p.onButton else p.muted, label = "navTint")
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(barShape)
                            .selectable(
                                selected = selected,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelect(index) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                        Text(
                            item.label,
                            color = tint,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
