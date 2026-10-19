package com.app.grammarai.ui.practice.evaluator

import com.app.grammarai.core.srs.SpacedRepetitionEngine
import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.model.ExerciseAttempt
import com.app.grammarai.data.model.SkillCategory
import com.app.grammarai.data.repository.CurriculumRepository
import com.app.grammarai.data.repository.PracticeHistoryRepository
import com.app.grammarai.data.repository.SupabaseSyncRepository
import com.app.grammarai.ui.practice.ExerciseUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ExerciseFlowCoordinator(
    private val prefs: PreferencesManager,
    private val repo: CurriculumRepository,
    private val historyRepo: PracticeHistoryRepository,
    private val srsEngine: SpacedRepetitionEngine,
    private val supabaseSyncRepo: SupabaseSyncRepository,
    private val scope: CoroutineScope,
    private val stateProvider: () -> ExerciseUiState,
    private val updateState: ((ExerciseUiState) -> ExerciseUiState) -> Unit,
    private val onSpeakExercise: () -> Unit
) {
    fun advanceToExercise(nextIndex: Int) {
        val state = stateProvider()
        if (nextIndex < state.exercises.size) {
            updateState {
                it.copy(
                    currentExerciseIndex = nextIndex,
                    selectedOption = null,
                    reconstructedWords = emptyList(),
                    translationAnswerMode = com.app.grammarai.ui.practice.TranslationAnswerMode.ORDER,
                    translationTextAnswer = "",
                    translationOrderedWords = emptyList(),
                    isAnswerChecked = false,
                    isCorrect = false,
                    isLiveConversationActive = false,
                    isConversationEnded = false,
                    conversationTurns = emptyList(),
                    conversationTurnCount = 0,
                    liveStreamingAiText = "",
                    liveStreamingUserText = "",
                    spokenTranscript = "",
                    pronunciationMatchScore = null,
                    isListening = false,
                    aiExplanation = null,
                    aiEvaluationResult = null,
                    isGeneratingExplanation = false,
                    isEvaluatingWithAi = false,
                    showAiExplanationDialog = false,
                    isOverriddenByAi = false,
                    selectedPairLeftId = null,
                    selectedPairRightId = null,
                    selectedPairLeftText = null,
                    selectedPairRightText = null,
                    matchedLeftIds = emptySet(),
                    matchedRightIds = emptySet(),
                    matchedPairs = emptySet(),
                    pairError = null,
                    isTranslationVisible = false,
                    isTranslating = false
                )
            }
            onSpeakExercise()
        } else {
            finishPractice()
        }
    }

    fun onConversationExerciseFinished() {
        val state = stateProvider()
        val currentEx = state.currentExercise ?: return
        prefs.recordExerciseCompleted(currentEx.category, true, 100)
        val attempt = ExerciseAttempt(
            id = "att_${System.currentTimeMillis()}",
            lessonId = state.lesson?.id ?: "",
            exerciseId = currentEx.id,
            category = currentEx.category,
            wasCorrect = true,
            accuracyScore = 100,
            userAnswer = "Completed dialogue with ${state.conversationTurns.size} turns",
            expectedAnswer = currentEx.prompt
        )
        scope.launch { historyRepo.recordAttempt(attempt) }
        updateState {
            it.copy(
                isConversationEnded = true,
                isLiveConversationActive = false,
                isListening = false,
                score = it.score + 20,
                exerciseResults = it.exerciseResults + (currentEx.id to true)
            )
        }
    }

    fun finishPractice() {
        val state = stateProvider()
        val lessonId = state.lesson?.id ?: ""
        val finalScorePercent = state.calculatedScorePercent
        repo.markLessonCompleted(lessonId, finalScorePercent)
        srsEngine.recordReviewResult(lessonId, finalScorePercent >= 70)
        updateState {
            it.copy(
                isCompleted = true,
                showCompletionCelebration = true,
                streakDays = prefs.streakDays,
                completedLessonsCount = prefs.weeklyLessonsCompletedCount,
                targetLessonsCount = prefs.lessonGoalTarget,
                skillMetrics = prefs.skillMetrics,
                score = finalScorePercent
            )
        }
        scope.launch {
            runCatching { supabaseSyncRepo.syncUserData() }
        }
    }
}
