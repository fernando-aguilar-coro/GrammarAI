package com.app.grammarai.ui.radar.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.data.model.SkillMetrics
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.NeonEmerald
import com.app.grammarai.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * 5-Axis Pentagon Radar Chart widget matching the reference geometry and colors.
 */
@Composable
fun PentagonRadarChart(
    metrics: SkillMetrics,
    modifier: Modifier = Modifier,
    height: Dp = 310.dp,
    canvasSize: Dp = 190.dp
) {
    val isCompact = canvasSize < 170.dp
    val titleFontSize = if (isCompact) 10.sp else 12.sp
    val scoreFontSize = if (isCompact) 13.sp else 17.sp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        // Pentagon Canvas (Grid & Data Polygon)
        Canvas(modifier = Modifier.size(canvasSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f

            val angles = listOf(
                -Math.PI / 2.0,                  // Pronunciation (Top)
                -Math.PI / 2.0 + 2 * Math.PI / 5, // Fluency (Right)
                -Math.PI / 2.0 + 4 * Math.PI / 5, // Intonation (Bottom-Right)
                -Math.PI / 2.0 + 6 * Math.PI / 5, // Grammar (Bottom-Left)
                -Math.PI / 2.0 + 8 * Math.PI / 5  // Vocabulary (Left)
            )

            // Draw concentric pentagon web guide rings (4 levels: 25%, 50%, 75%, 100%)
            val ringFractions = listOf(0.25f, 0.50f, 0.75f, 1.0f)
            for (fraction in ringFractions) {
                val r = maxRadius * fraction
                val ringPath = Path()
                for (i in angles.indices) {
                    val angle = angles[i]
                    val x = center.x + (r * cos(angle)).toFloat()
                    val y = center.y + (r * sin(angle)).toFloat()
                    if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                }
                ringPath.close()
                drawPath(
                    path = ringPath,
                    color = Color(0x33382D6E),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Draw 5 spoke lines from center to outer vertices
            for (angle in angles) {
                val end = Offset(
                    x = center.x + (maxRadius * cos(angle)).toFloat(),
                    y = center.y + (maxRadius * sin(angle)).toFloat()
                )
                drawLine(
                    color = Color(0x33382D6E),
                    start = center,
                    end = end,
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Dynamic polygon scale based on maxScore milestone (minimum 300 pts baseline)
            val targetScale = maxOf(300f, metrics.maxScore().toFloat())
            val values = listOf(
                metrics.productive / targetScale,
                metrics.pragmatics / targetScale,
                metrics.receptive / targetScale,
                metrics.structure / targetScale,
                metrics.lexicon / targetScale
            )

            val dataPath = Path()
            for (i in angles.indices) {
                val r = maxRadius * values[i].coerceIn(0.08f, 1.0f)
                val x = center.x + (r * cos(angles[i])).toFloat()
                val y = center.y + (r * sin(angles[i])).toFloat()
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            // Fill with translucent turquoise/emerald gradient
            drawPath(
                path = dataPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x7700E5FF), Color(0x4410B981)),
                    center = center,
                    radius = maxRadius
                ),
                style = Fill
            )

            // Outline polygon stroke
            drawPath(
                path = dataPath,
                brush = Brush.linearGradient(
                    colors = listOf(NeonCyan, NeonEmerald)
                ),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Labels overlay positioned around the radar (5 pillars)
        RadarAxisMetricLabel(
            title = stringResource(R.string.radar_skill_productive),
            score = metrics.productive,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = if (isCompact) 2.dp else 6.dp),
            titleSize = titleFontSize,
            scoreSize = scoreFontSize
        )

        RadarAxisMetricLabel(
            title = stringResource(R.string.radar_skill_pragmatics),
            score = metrics.pragmatics,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = if (isCompact) 2.dp else 4.dp),
            titleSize = titleFontSize,
            scoreSize = scoreFontSize
        )

        RadarAxisMetricLabel(
            title = stringResource(R.string.radar_skill_receptive),
            score = metrics.receptive,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = if (isCompact) 12.dp else 24.dp, bottom = if (isCompact) 2.dp else 4.dp),
            titleSize = titleFontSize,
            scoreSize = scoreFontSize
        )

        RadarAxisMetricLabel(
            title = stringResource(R.string.radar_skill_structure),
            score = metrics.structure,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = if (isCompact) 12.dp else 24.dp, bottom = if (isCompact) 2.dp else 4.dp),
            titleSize = titleFontSize,
            scoreSize = scoreFontSize
        )

        RadarAxisMetricLabel(
            title = stringResource(R.string.radar_skill_lexicon),
            score = metrics.lexicon,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = if (isCompact) 2.dp else 4.dp),
            titleSize = titleFontSize,
            scoreSize = scoreFontSize
        )
    }
}

@Composable
private fun RadarAxisMetricLabel(
    title: String,
    score: Int,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 12.sp,
    scoreSize: TextUnit = 17.sp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = titleSize,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "$score pts",
            color = if (score >= 150) NeonEmerald else NeonCyan,
            fontSize = scoreSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
