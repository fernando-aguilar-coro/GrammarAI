package com.app.grammarai.ui.common.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.common.animation.confirm
import com.app.grammarai.ui.common.animation.tick
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkBackground
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.ErrorCrimson
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

enum class GrammarButtonStyle {
    PRIMARY,
    OUTLINED,
    GHOST,
    SUCCESS,
    DANGER
}

/**
 * Standardized, high-performance button component for GrammarAI.
 * Features:
 * - 12.dp unified corner radius.
 * - Tactile spring-squeeze press animation.
 * - Haptic click tick on press.
 * - Optional leading/trailing icons and loading state.
 */
@Composable
fun GrammarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GrammarButtonStyle = GrammarButtonStyle.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    height: Dp = 50.dp,
    shapeRadius: Dp = 12.dp
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(shapeRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && enabled && !isLoading) {
            haptic.tick()
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "grammar_button_scale"
    )

    val contentColor = when (style) {
        GrammarButtonStyle.PRIMARY -> DarkBackground
        GrammarButtonStyle.SUCCESS -> DarkBackground
        GrammarButtonStyle.DANGER -> Color.White
        GrammarButtonStyle.OUTLINED -> if (enabled) TextPrimary else TextMuted
        GrammarButtonStyle.GHOST -> if (enabled) TextSecondary else TextMuted
    }

    val containerColor = when (style) {
        GrammarButtonStyle.PRIMARY -> AccentPrimary
        GrammarButtonStyle.SUCCESS -> SuccessEmerald
        GrammarButtonStyle.DANGER -> ErrorCrimson
        GrammarButtonStyle.OUTLINED -> DarkSurfaceCard
        GrammarButtonStyle.GHOST -> Color.Transparent
    }

    val disabledContainerColor = when (style) {
        GrammarButtonStyle.PRIMARY,
        GrammarButtonStyle.SUCCESS,
        GrammarButtonStyle.DANGER -> DarkSurfaceCard
        GrammarButtonStyle.OUTLINED -> DarkSurfaceCard.copy(alpha = 0.5f)
        GrammarButtonStyle.GHOST -> Color.Transparent
    }

    val borderStroke = when (style) {
        GrammarButtonStyle.OUTLINED -> BorderStroke(1.dp, if (enabled) BorderSubtle else BorderSubtle.copy(alpha = 0.4f))
        else -> null
    }

    val handleClick = {
        if (enabled && !isLoading) {
            haptic.confirm()
            onClick()
        }
    }

    val buttonModifier = modifier
        .height(height)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }

    val content = @Composable {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingContent != null) {
                    leadingContent()
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = text,
                    color = if (enabled) contentColor else TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.2.sp
                )

                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingContent()
                } else if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    val defaultPadding = if (style == GrammarButtonStyle.GHOST) {
        androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    } else {
        androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    }

    if (style == GrammarButtonStyle.OUTLINED) {
        OutlinedButton(
            onClick = handleClick,
            enabled = enabled && !isLoading,
            interactionSource = interactionSource,
            shape = shape,
            border = borderStroke,
            contentPadding = defaultPadding,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = containerColor,
                disabledContainerColor = disabledContainerColor
            ),
            modifier = buttonModifier
        ) {
            content()
        }
    } else if (style == GrammarButtonStyle.GHOST) {
        TextButton(
            onClick = handleClick,
            enabled = enabled && !isLoading,
            interactionSource = interactionSource,
            shape = shape,
            contentPadding = defaultPadding,
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            modifier = buttonModifier
        ) {
            content()
        }
    } else {
        Button(
            onClick = handleClick,
            enabled = enabled && !isLoading,
            interactionSource = interactionSource,
            shape = shape,
            contentPadding = defaultPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                disabledContainerColor = disabledContainerColor
            ),
            modifier = buttonModifier
        ) {
            content()
        }
    }
}
