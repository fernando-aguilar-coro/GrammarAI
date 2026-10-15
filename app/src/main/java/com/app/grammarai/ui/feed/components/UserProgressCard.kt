package com.app.grammarai.ui.feed.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.data.model.Lesson
import com.app.grammarai.ui.theme.BorderDivider
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.DarkSurfaceElevated
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.NeonEmerald
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun UserProgressCard(
    streakDays: Int,
    completedLessonsCount: Int,
    totalLessonsCount: Int,
    totalExercisesCompleted: Int,
    userXp: Int,
    modifier: Modifier = Modifier,
    weeklyCompletedLessonsCount: Int = completedLessonsCount,
    lessonGoalTarget: Int = 5,
    completedLessons: List<Lesson> = emptyList(),
    onStackClick: (() -> Unit)? = null
) {
    // Up to a maximum of 2 cards stacked behind so it doesn't break the layout
    val stackCount = completedLessonsCount.coerceIn(0, 2)
    val card2OffsetY by animateDpAsState(
        targetValue = if (stackCount >= 2) (-10).dp else 0.dp,
        label = "card2Offset"
    )
    val card1OffsetY by animateDpAsState(
        targetValue = if (stackCount >= 1) (-5).dp else 0.dp,
        label = "card1Offset"
    )

    val rawGoalFraction = if (lessonGoalTarget > 0) {
        weeklyCompletedLessonsCount.coerceAtLeast(0).toFloat() / lessonGoalTarget.toFloat()
    } else 0f
    val goalPercentage = (rawGoalFraction * 100).toInt()
    val progressFraction = rawGoalFraction.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "goalProgress"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Backmost Card Layer 2 (when at least 2 completed lessons)
        if (stackCount >= 2) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = card2OffsetY)
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable(enabled = onStackClick != null) { onStackClick?.invoke() }
            )
        }

        // Middle Card Layer 1 (when at least 1 completed lesson)
        if (stackCount >= 1) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = card1OffsetY)
                    .padding(horizontal = 7.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .clickable(enabled = onStackClick != null) { onStackClick?.invoke() }
            )
        }

        // Front Card: Main Interactive Dashboard Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Goal Header: "Objetivo de lecciones completadas: X/Y"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.feed_completed_lessons_goal_title,
                            weeklyCompletedLessonsCount,
                            lessonGoalTarget
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "$goalPercentage%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (goalPercentage >= 100) NeonEmerald else NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Thin, sleek progress bar directly below the goal title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(NeonCyan, NeonEmerald)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabular Metrics Row (Linear / Raycast style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricStatItem(
                    label = stringResource(R.string.feed_stat_goal),
                    value = "$weeklyCompletedLessonsCount/$lessonGoalTarget"
                )
                MetricDivider()
                MetricStatItem(
                    label = stringResource(R.string.feed_stat_curriculum),
                    value = "$completedLessonsCount/$totalLessonsCount"
                )
                MetricDivider()
                MetricStatItem(
                    label = stringResource(R.string.feed_stat_solved),
                    value = "$totalExercisesCompleted"
                )
                MetricDivider()
                MetricStatItem(
                    label = stringResource(R.string.feed_stat_xp),
                    value = "$userXp"
                )
            }

            // If user has mastered lessons accumulated in the deck, show subtle deck strip
            if (completedLessonsCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface.copy(alpha = 0.6f))
                        .clickable(enabled = onStackClick != null) { onStackClick?.invoke() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.feed_completed_deck_badge, completedLessonsCount),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "View deck →",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricStatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(22.dp)
            .background(BorderDivider)
    )
}
