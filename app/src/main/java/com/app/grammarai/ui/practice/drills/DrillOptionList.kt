package com.app.grammarai.ui.practice.drills

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.ui.common.animation.popBounceEffect
import com.app.grammarai.ui.common.animation.shakeEffect
import com.app.grammarai.ui.common.animation.tactilePress
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkSurface
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.ErrorCrimson
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextMuted
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun DrillOptionList(
    options: List<String>,
    selectedOption: String?,
    correctAnswer: String,
    isAnswerChecked: Boolean,
    onSelectOption: (String) -> Unit,
    onSpeakOption: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val labels = listOf("A", "B", "C", "D", "E", "F")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedOption == option
            val isCorrectAnswer = option.equals(correctAnswer, ignoreCase = true)
            val optionLabel = labels.getOrElse(index) { "${index + 1}" }

            val borderColor by animateColorAsState(
                targetValue = when {
                    isAnswerChecked && isCorrectAnswer -> SuccessEmerald
                    isAnswerChecked && isSelected && !isCorrectAnswer -> ErrorCrimson
                    isSelected -> AccentPrimary
                    else -> BorderSubtle
                },
                label = "option_border_color"
            )

            val backgroundColor by animateColorAsState(
                targetValue = when {
                    isAnswerChecked && isCorrectAnswer -> SuccessEmerald.copy(alpha = 0.10f)
                    isAnswerChecked && isSelected && !isCorrectAnswer -> ErrorCrimson.copy(alpha = 0.10f)
                    isSelected -> AccentPrimary.copy(alpha = 0.08f)
                    else -> DarkSurfaceCard
                },
                label = "option_bg_color"
            )

            val isWrongSelected = isAnswerChecked && isSelected && !isCorrectAnswer
            val isCheckedCorrect = isAnswerChecked && isCorrectAnswer

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isWrongSelected) {
                            Modifier.shakeEffect(trigger = isAnswerChecked)
                        } else if (isCheckedCorrect) {
                            Modifier.popBounceEffect(trigger = isAnswerChecked)
                        } else {
                            Modifier
                        }
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .tactilePress(
                        enabled = !isAnswerChecked,
                        onClick = { onSelectOption(option) }
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option Index Badge (A, B, C, D)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isAnswerChecked && isCorrectAnswer -> SuccessEmerald.copy(alpha = 0.15f)
                                    isAnswerChecked && isSelected && !isCorrectAnswer -> ErrorCrimson.copy(alpha = 0.15f)
                                    isSelected -> AccentPrimary.copy(alpha = 0.15f)
                                    else -> DarkSurface
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isAnswerChecked && isCorrectAnswer -> SuccessEmerald
                                    isAnswerChecked && isSelected && !isCorrectAnswer -> ErrorCrimson
                                    isSelected -> AccentPrimary
                                    else -> BorderSubtle
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAnswerChecked && isCorrectAnswer) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Correct",
                                tint = SuccessEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (isAnswerChecked && isSelected && !isCorrectAnswer) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Incorrect",
                                tint = ErrorCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = optionLabel,
                                color = if (isSelected) AccentPrimary else TextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = option,
                        color = when {
                            isAnswerChecked && isCorrectAnswer -> TextPrimary
                            isAnswerChecked && isSelected && !isCorrectAnswer -> Color(0xFFEF4444)
                            isSelected -> TextPrimary
                            else -> TextSecondary
                        },
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { onSpeakOption(option) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak option",
                            tint = if (isSelected) AccentPrimary else TextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
