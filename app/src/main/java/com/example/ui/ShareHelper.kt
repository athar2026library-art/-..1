package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Builds a simple branded share card (green/gold) and opens the system share sheet.
 */
object ShareHelper {

    fun shareZekrAsImage(context: Context, text: String, source: String = "") {
        val bitmap = createCardBitmap(text, source)
        val cache = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(cache, "zekr_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الذكر"))
    }

    private fun createCardBitmap(text: String, source: String): Bitmap {
        val width = 1080
        val padding = 72f
        val brandColor = Color.parseColor("#294b38")
        val gold = Color.parseColor("#d9a85c")
        val paper = Color.parseColor("#f7faf7")

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandColor
            textSize = 48f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val layoutWidth = (width - padding * 2).toInt()
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, layoutWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(8f, 1.15f)
            .setIncludePad(true)
            .build()

        val height = (padding * 2 + layout.height + 160).toInt().coerceAtLeast(800)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(paper)

        // Top gold accent bar
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold }
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, barPaint)

        // Card border
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#e1e9e2")
        }
        canvas.drawRoundRect(
            RectF(24f, 24f, width - 24f, height - 24f),
            32f, 32f, border
        )

        // Brand title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandColor
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("الباقيات", width / 2f, padding, titlePaint)

        canvas.save()
        canvas.translate(padding, padding + 40f)
        layout.draw(canvas)
        canvas.restore()

        if (source.isNotBlank()) {
            val srcPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#718077")
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(source, width / 2f, height - padding / 2, srcPaint)
        }

        return bmp
    }
}
