package com.app.grammarai.ui.lesson.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.ui.common.components.GrammarChip
import com.app.grammarai.ui.common.components.GrammarIconButton
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary
import com.app.grammarai.ui.theme.WarningAmber

@Composable
fun LessonTopBar(
    title: String,
    onBackClick: () -> Unit,
    isCompleted: Boolean = false,
    score: Int = 0,
    onToggleComplete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GrammarIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onBackClick,
            contentDescription = stringResource(R.string.nav_back),
            size = 38.dp,
            iconSize = 18.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        if (onToggleComplete != null) {
            Spacer(modifier = Modifier.width(8.dp))
            if (isCompleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (score > 0) {
                        val scoreVal = score.coerceIn(0, 100)
                        val scoreColor = when {
                            scoreVal >= 80 -> SuccessEmerald
                            scoreVal >= 60 -> WarningAmber
                            else -> Color(0xFFF43F5E)
                        }
                        GrammarChip(
                            text = "$scoreVal%",
                            textColor = scoreColor,
                            containerColor = scoreColor.copy(alpha = 0.12f),
                            borderColor = scoreColor.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            shapeRadius = 6.dp
                        )
                    }

                    GrammarChip(
                        text = stringResource(R.string.lesson_status_completed),
                        icon = Icons.Outlined.Check,
                        iconTint = SuccessEmerald,
                        textColor = SuccessEmerald,
                        containerColor = SuccessEmerald.copy(alpha = 0.12f),
                        borderColor = SuccessEmerald.copy(alpha = 0.3f),
                        fontSize = 11.sp,
                        shapeRadius = 6.dp,
                        onClick = onToggleComplete
                    )
                }
            } else {
                GrammarChip(
                    text = stringResource(R.string.lesson_action_complete),
                    icon = Icons.Outlined.Check,
                    iconTint = TextSecondary,
                    textColor = TextSecondary,
                    containerColor = DarkSurfaceCard,
                    borderColor = BorderSubtle,
                    fontSize = 11.sp,
                    shapeRadius = 6.dp,
                    onClick = onToggleComplete
                )
            }
        }
    }
}
