package com.app.grammarai.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.app.grammarai.R
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.ui.practice.celebration.ExerciseCompletedView
import com.app.grammarai.ui.practice.celebration.LessonCompletedCelebrationOverlay
import com.app.grammarai.ui.practice.components.ExerciseBottomActionBar
import com.app.grammarai.ui.practice.components.ExerciseHeaderBar
import com.app.grammarai.ui.practice.drills.ExerciseDynamicContent
import com.app.grammarai.ui.practice.feedback.AiExplanationDialog
import com.app.grammarai.ui.practice.feedback.ExerciseTranslationCard
import com.app.grammarai.ui.theme.DarkBackground
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.TextSecondary

import com.app.grammarai.ui.common.animation.doubleTick
import com.app.grammarai.ui.common.animation.tick

@Composable
fun ExerciseRunnerScreen(
    lessonId: String,
    viewModel: ExerciseRunnerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRadar: (String) -> Unit,
    onNavigateToFeed: () -> Unit = onNavigateBack
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.isAnswerChecked) {
        if (state.isAnswerChecked) {
            if (state.isCorrect) {
                haptic.doubleTick(scope)
            } else {
                haptic.tick()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.processIntent(ExerciseUiIntent.ToggleSpeechRecording)
        } else {
            Toast.makeText(context, context.getString(R.string.permission_mic_required), Toast.LENGTH_SHORT).show()
        }
    }

    val onToggleMicWithPermission: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.processIntent(ExerciseUiIntent.ToggleSpeechRecording)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(lessonId) { viewModel.processIntent(ExerciseUiIntent.LoadLesson(lessonId)) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ExerciseUiEffect.NavigateBack -> onNavigateBack()
                is ExerciseUiEffect.NavigateToRadar -> onNavigateToRadar(effect.lessonId)
                is ExerciseUiEffect.NavigateToFeed -> onNavigateToFeed()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(DarkBackground, Color(0xFF140D38), DarkBackground)))
    ) {
        if (state.isLoadingExercises && state.exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.drill_eval_analyzing), color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else if (state.isCompleted && !state.showCompletionCelebration) {
            ExerciseCompletedView(
                score = state.score,
                lessonId = lessonId,
                totalExercises = state.exercises.size,
                onFinishPractice = { viewModel.processIntent(ExerciseUiIntent.FinishPractice) },
                onNavigateToRadar = onNavigateToRadar
            )
        } else {
            val exercise = state.currentExercise
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                ExerciseHeaderBar(
                    currentIndex = state.currentExerciseIndex,
                    totalCount = state.exercises.size,
                    progressFraction = state.progressFraction,
                    score = state.score,
                    isPlayingAudio = state.isPlayingAudio,
                    isAudioLoading = state.isAudioLoading,
                    consecutiveCorrectCount = state.consecutiveCorrectCount,
                    onBackClick = { viewModel.processIntent(ExerciseUiIntent.Back) },
                    onToggleAudio = { viewModel.processIntent(ExerciseUiIntent.ToggleAudio) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)) {
                    if (exercise != null) {
                        ExerciseDynamicContent(
                            exercise = exercise,
                            state = state,
                            viewModel = viewModel,
                            onToggleMicWithPermission = onToggleMicWithPermission
                        )

                        if (state.isCurrentExerciseTranslatable && state.isTranslationVisible) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ExerciseTranslationCard(
                                visible = state.isTranslationVisible,
                                isTranslating = state.isTranslating,
                                translation = state.currentExerciseTranslation,
                                targetLanguage = "Español",
                                onDismiss = { viewModel.processIntent(ExerciseUiIntent.DismissExerciseTranslation) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                ExerciseBottomActionBar(
                    state = state,
                    onContinueFromConversation = { viewModel.processIntent(ExerciseUiIntent.ContinueFromConversation) },
                    onCheckAnswer = { viewModel.processIntent(ExerciseUiIntent.CheckAnswer) },
                    onNextExercise = { viewModel.processIntent(ExerciseUiIntent.NextExercise) },
                    onGiveUpExercise = { viewModel.processIntent(ExerciseUiIntent.GiveUpExercise) },
                    onRequestAiEvaluation = { viewModel.processIntent(ExerciseUiIntent.RequestAiEvaluation) }
                )
            }
        }

        AiExplanationDialog(
            visible = state.showAiExplanationDialog,
            aiResult = state.aiEvaluationResult,
            isOverriddenByAi = state.isOverriddenByAi,
            onDismiss = { viewModel.processIntent(ExerciseUiIntent.DismissAiExplanationDialog) }
        )

        if (state.showCompletionCelebration) {
            LessonCompletedCelebrationOverlay(
                lessonTitle = state.lesson?.title ?: "",
                score = state.score,
                completedLessonsCount = state.completedLessonsCount,
                targetLessonsCount = state.targetLessonsCount,
                skillMetrics = state.skillMetrics,
                streakDays = state.streakDays,
                onDismiss = { viewModel.processIntent(ExerciseUiIntent.DismissCelebration) }
            )
        }
    }
}
