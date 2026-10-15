package com.app.grammarai.ui.practice.celebration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.data.model.SkillMetrics
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.radar.components.PentagonRadarChart
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkBackground
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun LessonCompletedCelebrationOverlay(
    lessonTitle: String,
    score: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    completedLessonsCount: Int = 0,
    targetLessonsCount: Int = 5,
    skillMetrics: SkillMetrics = SkillMetrics(),
    streakDays: Int = 1,
    durationSeconds: Int = 10
) {
    var secondsRemaining by remember { mutableIntStateOf(durationSeconds) }
    val progress = remember { Animatable(1f) }
    val scaleAnim = remember { Animatable(0.4f) }
    val contentAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(300))
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
        )
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = durationSeconds * 1000, easing = LinearEasing)
        )
        onDismiss()
    }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.95f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        CelebrationConfettiCanvas()

        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    alpha = contentAlpha.value
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                }
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CelebrationBadge(pulseScale = pulseScale)

            Spacer(modifier = Modifier.height(16.dp))

            CelebrationTitleSection(lessonTitle = lessonTitle)

            Spacer(modifier = Modifier.height(18.dp))

            CelebrationStatsRow(
                score = score,
                completedLessons = completedLessonsCount,
                targetLessons = targetLessonsCount,
                streakDays = streakDays
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-Skill Spider/Radar Chart Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface.copy(alpha = 0.5f))
                    .border(1.dp, BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.radar_title).uppercase(),
                        color = AccentPrimary,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = skillMetrics.overallCefr.ifBlank { "B1" },
                        color = SuccessEmerald,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                PentagonRadarChart(
                    metrics = skillMetrics,
                    height = 200.dp,
                    canvasSize = 125.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            GrammarButton(
                text = stringResource(R.string.action_continue),
                onClick = onDismiss,
                style = GrammarButtonStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AccentPrimary,
                trackColor = BorderSubtle.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.celebration_dismiss_hint, secondsRemaining.coerceAtLeast(1)),
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
