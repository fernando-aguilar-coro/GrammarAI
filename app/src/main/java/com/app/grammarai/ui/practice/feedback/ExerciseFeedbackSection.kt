package com.app.grammarai.ui.practice.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.DarkBackground
import com.app.grammarai.ui.theme.DarkSurfaceElevated
import com.app.grammarai.ui.theme.ErrorCrimson
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun ExerciseFeedbackSection(
    isAnswerChecked: Boolean,
    isCorrect: Boolean,
    isOverriddenByAi: Boolean,
    isEvaluatingWithAi: Boolean,
    aiExplanation: String?,
    exercise: GeneratedExercise?,
    onRequestAiEvaluation: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isAnswerChecked,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isCorrect) SuccessEmerald else ErrorCrimson),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Outlined.Check else Icons.Outlined.Close,
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }

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
            }

            if (!isCorrect) {
                if (exercise?.type == com.app.grammarai.data.model.ExerciseType.SELECT_PAIRS && exercise.pairs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pares correctos:",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    exercise.pairs.forEach { pair ->
                        Text(
                            text = "• ${pair.left}  ➔  ${pair.right}",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                        )
                    }
                } else if (exercise?.correctAnswer.orEmpty().isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${stringResource(R.string.drill_correct_answer_label)}: ${exercise?.correctAnswer}",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            val explanation = exercise?.explanation.orEmpty()
            val isExplanationUseful = explanation.isNotBlank() && !explanation.equals(exercise?.prompt, ignoreCase = true)
            val aiExp = aiExplanation.orEmpty()

            if (aiExp.isNotBlank() || isExplanationUseful) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (aiExp.isNotBlank()) aiExp else explanation,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
                )
            }

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
