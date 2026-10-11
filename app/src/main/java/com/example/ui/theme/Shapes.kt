package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** أنصاف الأقطار: 8 · 12 · 20 · 28 · 36. الأزرار حبّة كاملة. */
val BaqiyatShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * شكل المحراب: قوس مدبب في الأعلى وجانبان مستقيمان وقاعدة بزوايا مدوّرة.
 * ارتفاع القوس ثابت نسبةً للعرض، فيبقى شكله متزناً مهما طال النص داخله.
 * يُستعمل لبطاقة الذكر فقط.
 */
class MihrabShape(private val baseRadius: Float = 28f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val r = with(density) { baseRadius.dp.toPx() }.coerceAtMost(min(w, h) / 4f)
        val arch = min(h * 0.55f, w * 0.66f)
        val shoulder = arch * 0.4234f
        val path = Path().apply {
            moveTo(0f, h - r)
            lineTo(0f, shoulder)
            cubicTo(0f, arch * 0.2162f, w * 0.2808f, arch * 0.0991f, w * 0.5f, 0f)
            cubicTo(w * 0.7192f, arch * 0.0991f, w, arch * 0.2162f, w, shoulder)
            lineTo(w, h - r)
            quadraticBezierTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticBezierTo(0f, h, 0f, h - r)
            close()
        }
        return Outline.Generic(path)
    }
}
