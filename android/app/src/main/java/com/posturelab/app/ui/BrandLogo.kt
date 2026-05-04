package com.posturelab.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun BrandLogo(sizeDp: Int) {
    Canvas(modifier = Modifier.size(sizeDp.dp, (sizeDp * 0.4).dp)) {
        val sx = size.width / 200f
        val sy = size.height / 80f
        val light = Color(0xFF3FA9D6)
        val dark = Color(0xFF1B4F8C)
        val stroke = Stroke(width = 6f * sx, cap = StrokeCap.Round)

        val p1 = Path().apply {
            moveTo(14f * sx, 50f * sy); quadraticBezierTo(14f * sx, 22f * sy, 38f * sx, 22f * sy)
            quadraticBezierTo(60f * sx, 22f * sy, 60f * sx, 44f * sy)
            quadraticBezierTo(60f * sx, 58f * sy, 46f * sx, 58f * sy)
        }
        val p2 = Path().apply {
            moveTo(30f * sx, 30f * sy); quadraticBezierTo(54f * sx, 30f * sy, 54f * sx, 52f * sy)
            quadraticBezierTo(54f * sx, 70f * sy, 36f * sx, 70f * sy)
            quadraticBezierTo(18f * sx, 70f * sy, 18f * sx, 54f * sy)
        }
        drawPath(p1, color = light, style = stroke)
        drawPath(p2, color = dark, style = stroke)
    }
}

object _BrandLogoUnused {
    val anchor = Offset.Zero
}
