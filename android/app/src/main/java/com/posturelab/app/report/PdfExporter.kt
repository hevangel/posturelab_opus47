package com.posturelab.app.report

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.posturelab.app.ui.UiState
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the posture report into a single Letter-sized PDF page using
 * `PdfDocument` + `Canvas`. The output mimics the styling of the on-screen
 * report (band headers, alternating rows, two-column layout).
 */
object PdfExporter {

    private const val PT_PER_IN = 72f
    private const val PAGE_W = 8.5f * PT_PER_IN
    private const val PAGE_H = 11f * PT_PER_IN
    private val BRAND = Color.rgb(0x1B, 0x4F, 0x8C)
    private val BRAND_LIGHT = Color.rgb(0x3F, 0xA9, 0xD6)
    private val ROW_TINT = Color.rgb(0xE8, 0xF0, 0xF8)

    fun export(ctx: Context, fileName: String, s: UiState): Boolean {
        val front = s.front ?: return false
        val side = s.side ?: return false
        val pdf = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), 1).create()
        val page = pdf.startPage(info)
        drawPage(page.canvas, s, front, side)
        pdf.finishPage(page)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: return false
                ctx.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val out = File(dir, fileName)
                FileOutputStream(out).use { pdf.writeTo(it) }
            }
            true
        } catch (_: Throwable) {
            false
        } finally {
            pdf.close()
        }
    }

    private fun drawPage(c: Canvas, s: UiState,
                         f: com.posturelab.app.analysis.PostureMath.FrontMetrics,
                         sd: com.posturelab.app.analysis.PostureMath.SideMetrics) {
        val pad = 0.4f * PT_PER_IN
        var y = pad

        // Logo (two arcs)
        drawLogo(c, pad, y, height = 32f)
        // Header right text
        val rightX = PAGE_W - pad
        val tinyPaint = paint(11f, BRAND, bold = true).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("PostureLab", rightX, y + 11f, tinyPaint)
        val grayPaint = paint(9f, Color.GRAY).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("Posture Analysis Platform", rightX, y + 22f, grayPaint)
        c.drawText("posturelab.example", rightX, y + 33f, grayPaint)
        y += 40f
        c.drawLine(pad, y, PAGE_W - pad, y, paint(1f, BRAND).apply { strokeWidth = 1f })
        y += 8f

        // Intro
        y = drawWrapped(c, PostureReportText.INTRO, pad, y, PAGE_W - 2f * pad, paint(8f, Color.BLACK))
        y += 6f

        // Two image columns
        val colGap = 8f
        val colW = (PAGE_W - 2f * pad - colGap) / 2f
        val imgH = 240f
        val bandH = 18f

        drawColumn(c, "Your Posture Viewed from the Front", s.frontAnnotated, pad, y, colW, imgH, f.totalShiftIn, f.totalTiltDeg)
        drawColumn(c, "Your Posture Viewed from the Side", s.sideAnnotated, pad + colW + colGap, y, colW, imgH, sd.totalShiftIn, sd.totalTiltDeg)
        y += bandH + imgH + 26f

        // Findings table — two columns of 5/6 rows each
        val frontLines = listOf(
            PostureReportText.headLine(f) to true,
            PostureReportText.shouldersLine(f) to false,
            PostureReportText.ribLine(f) to true,
            PostureReportText.hipsLine(f) to false,
            PostureReportText.qLine(f) to true,
        )
        val sideLines = listOf(
            PostureReportText.headSide(sd) to true,
            PostureReportText.headWeightSide(sd) to false,
            PostureReportText.shouldersSide(sd) to true,
            PostureReportText.hipsSide(sd) to false,
            PostureReportText.kneesSide(sd) to true,
            PostureReportText.headVsAnkle(sd) to false,
        )
        val rowH = 18f
        var fy = y
        for ((line, tint) in frontLines) {
            drawRow(c, line, pad, fy, colW, rowH, tint); fy += rowH
        }
        var sy = y
        for ((line, tint) in sideLines) {
            drawRow(c, line, pad + colW + colGap, sy, colW, rowH, tint); sy += rowH
        }
        y = maxOf(fy, sy) + 8f

        // Disclaimer
        val disclaimer = paint(9f, BRAND, bold = true)
        y = drawWrapped(c, "Any measurable deviation from normal posture causes weakening of the spine as well as increased stress on the nervous system which can adversely affect overall health.",
            pad, y + 4f, PAGE_W - 2f * pad, disclaimer)

        // Footer
        c.drawLine(pad, y + 6f, PAGE_W - pad, y + 6f, paint(1f, Color.LTGRAY).apply { strokeWidth = 0.5f })
        val footP = paint(7f, Color.GRAY)
        c.drawText("Pose detection uses Google ML Kit on-device. This report is automatically generated and is not a medical diagnosis.", pad, y + 18f, footP)
        c.drawText("Report for ${s.name} on ${s.date}.", pad, y + 28f, footP)
    }

    private fun drawColumn(c: Canvas, title: String, bm: Bitmap?, x: Float, y: Float, w: Float, imgH: Float,
                           totalShift: Float, totalTilt: Float) {
        val bandH = 18f
        // Band
        val band = Paint().apply { color = BRAND; isAntiAlias = true }
        c.drawRect(x, y, x + w, y + bandH, band)
        val tp = paint(9f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.CENTER }
        c.drawText(title.uppercase(), x + w / 2f, y + 12f, tp)

        // Image
        val border = Paint().apply { color = BRAND; style = Paint.Style.STROKE; strokeWidth = 0.6f }
        val bg = Paint().apply { color = Color.rgb(245, 247, 250) }
        c.drawRect(x, y + bandH, x + w, y + bandH + imgH, bg)
        if (bm != null) {
            val src = android.graphics.Rect(0, 0, bm.width, bm.height)
            val ratio = bm.width.toFloat() / bm.height
            val targetH = imgH; val targetW = targetH * ratio
            val dx = x + (w - targetW) / 2f; val dy = y + bandH
            val dst = RectF(dx, dy, dx + targetW.coerceAtMost(w), dy + targetH)
            c.drawBitmap(bm, src, dst, null)
        }
        c.drawRect(x, y + bandH, x + w, y + bandH + imgH, border)

        // Stats
        val sy = y + bandH + imgH
        val statH = 22f
        c.drawRect(x, sy, x + w, sy + statH, border)
        c.drawLine(x + w / 2f, sy, x + w / 2f, sy + statH, border)
        val stp = paint(8f, BRAND, bold = true).apply { textAlign = Paint.Align.CENTER }
        val vtp = paint(8f, Color.BLACK).apply { textAlign = Paint.Align.CENTER }
        c.drawText("Total Shift", x + w / 4f, sy + 9f, stp)
        c.drawText("${"%.2f".format(totalShift)} in", x + w / 4f, sy + 19f, vtp)
        c.drawText("Total Tilt", x + 3 * w / 4f, sy + 9f, stp)
        c.drawText("${"%.1f".format(totalTilt)}°", x + 3 * w / 4f, sy + 19f, vtp)
    }

    private fun drawRow(c: Canvas, text: String, x: Float, y: Float, w: Float, h: Float, tint: Boolean) {
        if (tint) {
            c.drawRect(x, y, x + w, y + h, Paint().apply { color = ROW_TINT })
        }
        val tp = paint(8f, Color.BLACK)
        drawWrapped(c, text, x + 4f, y + 11f, w - 8f, tp, lineSpacing = 9f)
    }

    private fun drawWrapped(c: Canvas, text: String, x: Float, y: Float, maxW: Float, p: Paint,
                            lineSpacing: Float = 11f): Float {
        val words = text.split(" ")
        val sb = StringBuilder()
        var cy = y
        for (w in words) {
            val candidate = if (sb.isEmpty()) w else "$sb $w"
            if (p.measureText(candidate) > maxW) {
                c.drawText(sb.toString(), x, cy, p)
                cy += lineSpacing
                sb.clear(); sb.append(w)
            } else {
                if (sb.isNotEmpty()) sb.append(' ')
                sb.append(w)
            }
        }
        if (sb.isNotEmpty()) {
            c.drawText(sb.toString(), x, cy, p)
            cy += lineSpacing
        }
        return cy
    }

    private fun drawLogo(c: Canvas, x: Float, y: Float, height: Float) {
        val sx = height / 80f * 200f / 200f
        val s = height / 80f
        val light = Paint().apply { color = BRAND_LIGHT; style = Paint.Style.STROKE; strokeWidth = 4f * s; strokeCap = Paint.Cap.ROUND; isAntiAlias = true }
        val dark = Paint().apply { color = BRAND; style = Paint.Style.STROKE; strokeWidth = 4f * s; strokeCap = Paint.Cap.ROUND; isAntiAlias = true }
        val p1 = android.graphics.Path().apply {
            moveTo(x + 14f * s, y + 50f * s); quadTo(x + 14f * s, y + 22f * s, x + 38f * s, y + 22f * s)
            quadTo(x + 60f * s, y + 22f * s, x + 60f * s, y + 44f * s)
            quadTo(x + 60f * s, y + 58f * s, x + 46f * s, y + 58f * s)
        }
        val p2 = android.graphics.Path().apply {
            moveTo(x + 30f * s, y + 30f * s); quadTo(x + 54f * s, y + 30f * s, x + 54f * s, y + 52f * s)
            quadTo(x + 54f * s, y + 70f * s, x + 36f * s, y + 70f * s)
            quadTo(x + 18f * s, y + 70f * s, x + 18f * s, y + 54f * s)
        }
        c.drawPath(p1, light)
        c.drawPath(p2, dark)
        // Text "Posture" / "Lab"
        c.drawText("Posture", x + 74f * s, y + 36f * s, paint(18f * s, BRAND, bold = true))
        c.drawText("Lab", x + 74f * s, y + 60f * s, paint(18f * s, BRAND_LIGHT, bold = true))
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false): Paint = Paint().apply {
        textSize = size
        this.color = color
        isAntiAlias = true
        if (bold) typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        else typeface = android.graphics.Typeface.SANS_SERIF
    }
}
