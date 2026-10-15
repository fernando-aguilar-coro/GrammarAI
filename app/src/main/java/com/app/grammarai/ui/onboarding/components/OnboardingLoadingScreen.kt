package com.app.grammarai.ui.onboarding.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkBackground
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.DarkSurfaceCardElevated
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun OnboardingLoadingScreen(
    isFinalizing: Boolean = false,
    onSkip: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing AI orb / emblem
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Subtle glow halo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(AccentPrimary.copy(alpha = glowAlpha * 0.35f))
                )
                // Minimal card core
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceCardElevated)
                        .border(1.dp, BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFinalizing) "🚀" else "⚡",
                        fontSize = 32.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(
                    if (isFinalizing) R.string.onboarding_finalizing_title else R.string.onboarding_loading_title
                ),
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    if (isFinalizing) R.string.onboarding_finalizing_subtitle else R.string.onboarding_loading_subtitle
                ),
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Loading indicator pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AccentPrimary,
                    strokeWidth = 2.dp
                )
            }

            if (!isFinalizing) {
                Spacer(modifier = Modifier.height(20.dp))
                GrammarButton(
                    text = stringResource(R.string.action_skip),
                    onClick = onSkip,
                    style = GrammarButtonStyle.GHOST
                )
            }
        }
    }
}
