package com.app.grammarai.ui.practice.celebration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.NeonEmerald
import com.app.grammarai.ui.theme.NeonOrange
import com.app.grammarai.ui.theme.NeonPurple
import kotlin.random.Random

private data class ConfettiParticle(
    val x: Float,
    val initialY: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val angleOffset: Float
)

@Composable
fun CelebrationConfettiCanvas(modifier: Modifier = Modifier) {
    val particles = remember {
        val colors = listOf(NeonCyan, AccentPrimary, NeonEmerald, NeonOrange, NeonPurple, Color(0xFFFFD700))
        List(36) {
            ConfettiParticle(
                x = Random.nextFloat(),
                initialY = Random.nextFloat() * -0.5f,
                speed = 0.25f + Random.nextFloat() * 0.45f,
                size = 6f + Random.nextFloat() * 8f,
                color = colors[it % colors.size],
                angleOffset = Random.nextFloat() * 360f
            )
        }
    }

    val particleTransition = rememberInfiniteTransition(label = "particles")
    val particleProgress by particleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleFall"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            val currentY = ((p.initialY + particleProgress * p.speed) % 1.2f) * height
            val currentX = (p.x * width) + (kotlin.math.sin((particleProgress * 4f + p.angleOffset).toDouble()) * 20f).toFloat()

            drawCircle(
                color = p.color.copy(alpha = 0.85f),
                radius = p.size,
                center = Offset(currentX, currentY)
            )
        }
    }
}
