package com.app.grammarai.ui.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun GoogleGLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.18f

        // Blue right/center bar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(w * 0.5f, h * 0.5f),
            end = Offset(w, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Arc (Google 4-colors)
        drawArc(
            color = Color(0xFFEA4335), // Red
            startAngle = 210f,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFFFBBC05), // Yellow
            startAngle = 130f,
            sweepAngle = 80f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF34A853), // Green
            startAngle = 30f,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
