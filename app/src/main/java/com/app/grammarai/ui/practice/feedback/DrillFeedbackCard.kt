package com.app.grammarai.ui.practice.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.DarkSurfaceElevated
import com.app.grammarai.ui.theme.ErrorCrimson
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun DrillFeedbackCard(
    isAnswerChecked: Boolean,
    isCorrect: Boolean,
    correctAnswer: String,
    explanation: String,
    aiExplanation: String?,
    isGeneratingExplanation: Boolean = false,
    isOverriddenByAi: Boolean = false,
    isEvaluatingWithAi: Boolean = false,
    onRequestAiEvaluation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = isAnswerChecked, modifier = modifier) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (isCorrect) SuccessEmerald.copy(alpha = 0.6f) else ErrorCrimson.copy(alpha = 0.6f),
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) SuccessEmerald.copy(alpha = 0.1f) else ErrorCrimson.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isOverriddenByAi) {
                            stringResource(R.string.drill_ai_override_badge)
                        } else if (isCorrect) {
                            stringResource(R.string.drill_feedback_correct)
                        } else {
                            stringResource(R.string.drill_feedback_incorrect)
                        },
                        color = if (isCorrect) SuccessEmerald else ErrorCrimson,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    if (!isCorrect && correctAnswer.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${stringResource(R.string.drill_correct_answer_label)} “$correctAnswer”",
                            color = AccentPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isCorrect) {
                        if (isGeneratingExplanation) {
                            Text(
                                text = stringResource(R.string.drill_eval_analyzing),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        } else if (!aiExplanation.isNullOrBlank()) {
                            Text(
                                text = aiExplanation,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp)
                            )
                        } else if (explanation.isNotBlank()) {
                            Text(
                                text = explanation,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp)
                            )
                        }
                    } else if (explanation.isNotBlank()) {
                        Text(
                            text = explanation,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp)
                        )
                    }

                    if (onRequestAiEvaluation != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (isEvaluatingWithAi) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceElevated)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentPrimary
                                )
                                Text(
                                    text = stringResource(R.string.drill_ai_explaining),
                                    color = AccentPrimary,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        } else {
                            GrammarButton(
                                text = if (isOverriddenByAi) {
                                    stringResource(R.string.drill_ai_override_badge)
                                } else {
                                    stringResource(R.string.drill_ai_explain_button)
                                },
                                onClick = onRequestAiEvaluation,
                                style = if (isOverriddenByAi) GrammarButtonStyle.SUCCESS else GrammarButtonStyle.OUTLINED,
                                leadingIcon = Icons.Default.AutoAwesome,
                                height = 38.dp,
                                modifier = Modifier.fillMaxWidth(0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}

