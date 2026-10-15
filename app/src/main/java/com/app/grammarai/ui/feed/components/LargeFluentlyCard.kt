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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.data.model.Lesson
import com.app.grammarai.data.model.LessonStatus
import com.app.grammarai.ui.common.animation.tactilePress
import com.app.grammarai.ui.common.components.GrammarChip
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary
import com.app.grammarai.ui.theme.WarningAmber

@Composable
fun LargeFluentlyCard(
    lesson: Lesson,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (lesson.isLocked) {
        LockedFluentlyCard(
            lesson = lesson,
            onClick = onClick,
            onToggleComplete = onToggleComplete,
            modifier = modifier
        )
        return
    }

    val isMastered = lesson.status == LessonStatus.MASTERED
    val cardAlpha = if (isMastered) 0.60f else 1.0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(
                1.dp,
                if (isMastered) BorderSubtle else BorderSubtle.copy(alpha = 0.85f),
                RoundedCornerShape(16.dp)
            )
            .tactilePress(onClick = onClick)
            .padding(18.dp)
    ) {
        // Top Meta Row: Module number & Status / CEFR badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GrammarChip(
                text = stringResource(
                    R.string.feed_lesson_number,
                    lesson.moduleTwoDigits,
                    lesson.cefr.ifBlank { "B1" }
                ),
                textColor = AccentPrimary,
                containerColor = DarkSurface,
                borderColor = BorderSubtle,
                fontSize = 11.sp,
                shapeRadius = 6.dp
            )

            if (isMastered) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (lesson.score > 0) {
                        val scoreColor = when {
                            lesson.score >= 80 -> SuccessEmerald
                            lesson.score >= 60 -> WarningAmber
                            else -> Color(0xFFF43F5E)
                        }
                        GrammarChip(
                            text = "${lesson.score}%",
                            textColor = scoreColor,
                            containerColor = scoreColor.copy(alpha = 0.12f),
                            borderColor = scoreColor.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            shapeRadius = 6.dp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SuccessEmerald.copy(alpha = 0.12f))
                            .border(1.dp, SuccessEmerald.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .clickable { onToggleComplete() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Completada - Tocar para desmarcar",
                            tint = SuccessEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = stringResource(R.string.feed_status_mastered),
                            color = SuccessEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${lesson.durationMinutes} min",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { onToggleComplete() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Marcar como completada",
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Completar",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title
        Text(
            text = lesson.title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Summary
        Text(
            text = lesson.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(18.dp))

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
                    text = if (lesson.exercises.isNotEmpty()) {
                        stringResource(R.string.feed_card_practice_items, lesson.exercises.size)
                    } else {
                        stringResource(R.string.feed_card_video_guide)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isMastered) DarkSurface else AccentPrimary.copy(alpha = 0.12f))
                    .border(
                        1.dp,
                        if (isMastered) BorderSubtle else AccentPrimary.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isMastered) stringResource(R.string.feed_btn_review) else stringResource(R.string.feed_btn_open),
                    color = if (isMastered) TextSecondary else AccentPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = if (isMastered) TextSecondary else AccentPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
