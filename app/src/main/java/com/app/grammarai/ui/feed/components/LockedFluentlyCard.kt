package com.app.grammarai.ui.feed.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.data.model.Lesson
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary
import kotlin.math.atan2

/**
 * Dedicated Card for Locked Curriculum Lessons.
 * Features corner chains wrapping toward a central high-opacity lock badge,
 * full-contrast typography for the locked state, and a separate one-touch
 * complete button that bypasses card click interception.
 */
@Composable
fun LockedFluentlyCard(
    lesson: Lesson,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        // Top Meta Row: Module number & Status badge + One-touch Complete button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.feed_lesson_number,
                        lesson.moduleTwoDigits,
                        lesson.cefr.ifBlank { "B1" }
                    ),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Locked status badge - 100% full opacity, no dimming
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.feed_status_locked),
                        tint = TextPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = stringResource(R.string.feed_status_locked),
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // One-touch completion action
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

        Spacer(modifier = Modifier.height(14.dp))

        // Title
        Text(
            text = lesson.title,
            color = TextSecondary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Summary
        Text(
            text = lesson.summary,
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Prominent Locked Container with Corner Chains
        ChainedLockContainer(
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

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
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }

            // Locked CTA
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.feed_status_locked),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Visual container displaying metallic diagonal chains anchored to the four corners,
 * leading to a solid, centered lock badge with 100% opacity.
 */
@Composable
fun ChainedLockContainer(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Metallic Chains drawn from all four corners
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            val chainColor = Color(0xFF64748B) // Slate steel
            val chainFill = Color(0xFF1E2230) // Dark iron core
            val rivetColor = Color(0xFF94A3B8) // Bright steel fastener

            val corners = listOf(
                Offset(0f, 0f),       // Top-Left
                Offset(w, 0f),       // Top-Right
                Offset(0f, h),       // Bottom-Left
                Offset(w, h)        // Bottom-Right
            )

            // Draw corner rivets
            corners.forEach { corner ->
                drawCircle(
                    color = rivetColor,
                    radius = 4.dp.toPx(),
                    center = corner
                )
                drawCircle(
                    color = chainFill,
                    radius = 2.dp.toPx(),
                    center = corner
                )
            }

            // Draw interlocking chain links along each diagonal
            val linkWidth = 14.dp.toPx()
            val linkHeight = 7.dp.toPx()
            val stepDist = 12.dp.toPx()
            val stopRadius = 38.dp.toPx() // Stop before overlapping central lock badge

            corners.forEach { corner ->
                val dx = center.x - corner.x
                val dy = center.y - corner.y
                val dist = kotlin.math.hypot(dx, dy)
                val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                var currentDist = 8.dp.toPx()
                var linkToggle = false

                while (currentDist < (dist - stopRadius)) {
                    val progress = currentDist / dist
                    val linkCenter = Offset(
                        corner.x + dx * progress,
                        corner.y + dy * progress
                    )

                    rotate(degrees = angleDeg, pivot = linkCenter) {
                        if (!linkToggle) {
                            // Flat link
                            drawRoundRect(
                                color = chainFill,
                                topLeft = Offset(linkCenter.x - linkWidth / 2, linkCenter.y - linkHeight / 2),
                                size = Size(linkWidth, linkHeight),
                                cornerRadius = CornerRadius(linkHeight / 2, linkHeight / 2)
                            )
                            drawRoundRect(
                                color = chainColor,
                                topLeft = Offset(linkCenter.x - linkWidth / 2, linkCenter.y - linkHeight / 2),
                                size = Size(linkWidth, linkHeight),
                                cornerRadius = CornerRadius(linkHeight / 2, linkHeight / 2),
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        } else {
                            // Interlocking perpendicular ring (narrow profile)
                            val ringWidth = 8.dp.toPx()
                            val ringHeight = 10.dp.toPx()
                            drawRoundRect(
                                color = chainFill,
                                topLeft = Offset(linkCenter.x - ringWidth / 2, linkCenter.y - ringHeight / 2),
                                size = Size(ringWidth, ringHeight),
                                cornerRadius = CornerRadius(ringWidth / 2, ringWidth / 2)
                            )
                            drawRoundRect(
                                color = rivetColor,
                                topLeft = Offset(linkCenter.x - ringWidth / 2, linkCenter.y - ringHeight / 2),
                                size = Size(ringWidth, ringHeight),
                                cornerRadius = CornerRadius(ringWidth / 2, ringWidth / 2),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    linkToggle = !linkToggle
                    currentDist += stepDist
                }
            }
        }

        // Central Lock Badge - elevated over the chains with 100% opacity
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF141722))
                .border(1.5.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceCard)
                    .border(1.dp, Color(0xFF64748B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = stringResource(R.string.feed_status_locked).uppercase(),
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}
