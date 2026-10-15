package com.app.grammarai.ui.common.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Applies a subtle tactile spring squeeze on press with an automatic micro-haptic tick.
 */
fun Modifier.tactilePress(
    scaleDown: Float = 0.975f,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            haptic.tick()
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tactile_press_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}

/**
 * Oscillates the element horizontally with a damped sine wave whenever [trigger] updates.
 * Ideal for polite, non-punitive wrong answer and mismatch feedback.
 */
fun Modifier.shakeEffect(
    trigger: Any?,
    shakeOffsetDp: Dp = 4.dp
): Modifier = composed {
    val density = LocalDensity.current
    val shakeOffsetPx = with(density) { shakeOffsetDp.toPx() }
    val animOffset = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger != null) {
            animOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 260
                    0f at 0
                    (-shakeOffsetPx) at 50
                    shakeOffsetPx at 100
                    (-shakeOffsetPx * 0.5f) at 160
                    (shakeOffsetPx * 0.5f) at 210
                    0f at 260
                }
            )
        }
    }

    this.graphicsLayer {
        translationX = animOffset.value
    }
}

/**
 * Momentarily pops the element upward and scales slightly on success.
 */
fun Modifier.popBounceEffect(
    trigger: Any?,
    bounceHeightDp: Dp = 5.dp
): Modifier = composed {
    val density = LocalDensity.current
    val bounceHeightPx = with(density) { bounceHeightDp.toPx() }
    val animOffset = remember { Animatable(0f) }
    val animScale = remember { Animatable(1f) }

    LaunchedEffect(trigger) {
        if (trigger != null) {
            coroutineScope {
                launch {
                    animOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 280
                            0f at 0
                            (-bounceHeightPx) at 100
                            0f at 280
                        }
                    )
                }
                launch {
                    animScale.animateTo(
                        targetValue = 1f,
                        animationSpec = keyframes {
                            durationMillis = 280
                            1f at 0
                            1.04f at 100
                            1f at 280
                        }
                    )
                }
            }
        }
    }

    this.graphicsLayer {
        translationY = animOffset.value
        scaleX = animScale.value
        scaleY = animScale.value
    }
}

/**
 * Creates an ambient, continuously moving border gradient for hero cards
 * that gently breathes and shifts over [durationMillis].
 */
@Composable
fun rememberBreathingBorderBrush(
    colors: List<Color>,
    durationMillis: Int = 3600
): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing_border_transition")
    val offsetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathing_border_offset"
    )

    return Brush.linearGradient(
        colors = colors,
        start = Offset(offsetProgress, 0f),
        end = Offset(offsetProgress + 1000f, 1000f)
    )
}
