package com.app.grammarai.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.data.model.Lesson
import com.app.grammarai.ui.common.animation.rememberBreathingBorderBrush
import com.app.grammarai.ui.common.animation.tactilePress
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.AccentSlate
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.DarkSurfaceElevated
import com.app.grammarai.ui.theme.NeonAmber
import com.app.grammarai.ui.theme.NeonEmerald
import com.app.grammarai.ui.theme.NeonViolet
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun SituationalVocabularyCard(
    lesson: Lesson,
    themeTitle: String = "Cafetería",
    partIndex: Int = 1,
    totalParts: Int = 3,
    isAllCompleted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardGradient = if (isAllCompleted) {
        Brush.verticalGradient(
            colors = listOf(
                DarkSurfaceCard,
                DarkSurface
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                DarkSurfaceCard,
                DarkSurfaceElevated
            )
        )
    }

    val borderModifier = if (isAllCompleted) {
        Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
    } else {
        val borderGradient = rememberBreathingBorderBrush(
            colors = listOf(
                AccentSlate.copy(alpha = 0.6f),
                AccentPrimary.copy(alpha = 0.5f),
                AccentSlate.copy(alpha = 0.6f)
            ),
            durationMillis = 3500
        )
        Modifier.border(1.dp, borderGradient, RoundedCornerShape(16.dp))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isAllCompleted) Modifier.alpha(0.75f) else Modifier)
            .clip(RoundedCornerShape(16.dp))
            .background(cardGradient)
            .then(borderModifier)
            .tactilePress(onClick = onClick)
            .padding(18.dp)
    ) {
        // Top Meta Row: Situational Level Badge & Linear Stepper Pips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Compact Shiny Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isAllCompleted) NeonEmerald.copy(alpha = 0.15f) else NeonViolet.copy(alpha = 0.22f))
                    .border(1.dp, if (isAllCompleted) NeonEmerald.copy(alpha = 0.4f) else NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isAllCompleted) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isAllCompleted) NeonEmerald else NeonAmber,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (isAllCompleted) "TEMA COMPLETADO" else "${themeTitle.uppercase()} • PARTE $partIndex/$totalParts",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isAllCompleted) NeonEmerald else NeonAmber
                )
            }

            // Right: Stepper Progress Pips (● ○ ○) + Duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (step in 1..totalParts) {
                        val isFilled = isAllCompleted || step < partIndex
                        val isCurrent = !isAllCompleted && step == partIndex
                        val pipSize by androidx.compose.animation.core.animateDpAsState(
                            targetValue = if (isCurrent) 9.dp else 7.dp,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.6f,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                            ),
                            label = "pip_size_$step"
                        )
                        Box(
                            modifier = Modifier
                                .size(pipSize)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isAllCompleted -> NeonEmerald
                                        isFilled -> NeonAmber
                                        isCurrent -> NeonAmber
                                        else -> Color(0xFF3B2D54)
                                    }
                                )
                                .then(
                                    if (isCurrent) Modifier.border(1.dp, Color.White, CircleShape) else Modifier
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "${lesson.durationMinutes} min",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title
        Text(
            text = lesson.title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 23.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Summary
        if (lesson.summary.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = lesson.summary,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom utility row: items count & CTA button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircleOutline,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${lesson.exercises.size} ejercicios rápidos",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }

            // Practice CTA button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAllCompleted) DarkSurface else NeonViolet.copy(alpha = 0.25f))
                    .border(
                        1.dp,
                        if (isAllCompleted) BorderSubtle else NeonViolet.copy(alpha = 0.6f),
                        RoundedCornerShape(8.dp)
                    )
                    .tactilePress(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isAllCompleted) "Repasar" else "Empezar",
                    color = if (isAllCompleted) TextSecondary else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = if (isAllCompleted) TextSecondary else Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
