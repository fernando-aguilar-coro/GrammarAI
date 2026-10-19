package com.app.grammarai.ui.practice.evaluator

import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.model.ExerciseAttempt
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.SkillCategory
import com.app.grammarai.data.remote.GeminiApiClient
import com.app.grammarai.data.repository.PracticeHistoryRepository
import com.app.grammarai.ui.practice.ExerciseUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ExerciseAnswerEvaluator(
    private val prefs: PreferencesManager,
    private val historyRepo: PracticeHistoryRepository,
    private val geminiClient: GeminiApiClient,
    private val scope: CoroutineScope,
    private val stateProvider: () -> ExerciseUiState,
    private val updateState: ((ExerciseUiState) -> ExerciseUiState) -> Unit,
    private val onIncorrectAnswer: ((com.app.grammarai.data.model.GeneratedExercise) -> Unit)? = null
) {
    fun checkCurrentAnswer() {
        val state = stateProvider()
        val currentEx = state.currentExercise ?: return
        if (state.isAnswerChecked) return

        val isCorrect = ExerciseEvaluator.isAnswerCorrect(
            exercise = currentEx,
            selectedOption = state.selectedOption,
            reconstructedWords = state.reconstructedWords,
            spokenTranscript = state.spokenTranscript,
            pronunciationMatchScore = state.pronunciationMatchScore,
            translationAnswer = state.translationAnswer
        )
        val accuracy = state.pronunciationMatchScore ?: if (isCorrect) 100 else 40
        prefs.recordExerciseCompleted(currentEx.category, isCorrect, accuracy)

        val attempt = ExerciseAttempt(
            id = "att_${System.currentTimeMillis()}",
            lessonId = state.lesson?.id ?: "",
            exerciseId = currentEx.id,
            category = currentEx.category,
            wasCorrect = isCorrect,
            accuracyScore = accuracy,
            userAnswer = if (currentEx.type == ExerciseType.TRANSLATE) {
                state.translationAnswer
            } else {
                state.selectedOption ?: state.reconstructedWords.joinToString(" ")
            },
            expectedAnswer = currentEx.correctAnswer.ifBlank { currentEx.targetPhrase }
        )
        scope.launch { historyRepo.recordAttempt(attempt) }

        val newCombo = if (isCorrect) state.consecutiveCorrectCount + 1 else 0

        updateState {
            it.copy(
                isAnswerChecked = true,
                isCorrect = isCorrect,
                consecutiveCorrectCount = newCombo,
                score = it.score + if (isCorrect) 10 else 0,
                exerciseResults = it.exerciseResults + (currentEx.id to isCorrect),
                aiExplanation = if (!isCorrect && currentEx.explanation.isNotBlank()) currentEx.explanation else null
            )
        }

        if (!isCorrect) {
            onIncorrectAnswer?.invoke(currentEx)
        }
    }

    /**
     * Completes the current exercise as incorrect immediately so the user is never stuck.
     * Reveals the feedback sheet with explanations and enables the Next button.
     */
    fun giveUpCurrentAnswer() {
        val state = stateProvider()
        val currentEx = state.currentExercise ?: return
        if (state.isAnswerChecked) return

        prefs.recordExerciseCompleted(currentEx.category, false, 0)

        val userAnswer = when (currentEx.type) {
            ExerciseType.SELECT_PAIRS -> "Pares completados: ${state.matchedPairs.size}/${currentEx.pairs.size}"
            ExerciseType.SCRAMBLED_SENTENCE,
            ExerciseType.SENTENCE_RECONSTRUCTION,
            ExerciseType.SCRAMBLE_LISTENING ->
                state.reconstructedWords.joinToString(" ").ifBlank { "(Omitido)" }
            ExerciseType.TRANSLATE -> state.translationAnswer.ifBlank { "(Omitido)" }
            else -> state.selectedOption ?: "(Omitido)"
        }
        val expectedAnswer = when (currentEx.type) {
            ExerciseType.SELECT_PAIRS -> currentEx.pairs.joinToString("; ") { "${it.left} = ${it.right}" }
            else -> currentEx.correctAnswer
        }

        val attempt = ExerciseAttempt(
            id = "att_${System.currentTimeMillis()}",
            lessonId = state.lesson?.id ?: "",
            exerciseId = currentEx.id,
            category = currentEx.category,
            wasCorrect = false,
            accuracyScore = 0,
            userAnswer = userAnswer,
            expectedAnswer = expectedAnswer
        )
        scope.launch { historyRepo.recordAttempt(attempt) }

        updateState {
            it.copy(
                isAnswerChecked = true,
                isCorrect = false,
                exerciseResults = it.exerciseResults + (currentEx.id to false),
                aiExplanation = if (currentEx.explanation.isNotBlank()) currentEx.explanation else null
            )
        }
        onIncorrectAnswer?.invoke(currentEx)
    }

    fun requestAiEvaluation() {
        val state = stateProvider()
        val currentEx = state.currentExercise ?: return
        if (state.isEvaluatingWithAi) return

        val userAnswer = when (currentEx.type) {
            ExerciseType.SCRAMBLED_SENTENCE,
            ExerciseType.SENTENCE_RECONSTRUCTION,
            ExerciseType.SCRAMBLE_LISTENING -> state.reconstructedWords.joinToString(" ")
            ExerciseType.PRONUNCIATION, ExerciseType.REPEAT_AFTER_ME, ExerciseType.INTONATION_CHALLENGE -> state.spokenTranscript.ifBlank { state.selectedOption ?: "" }
            ExerciseType.TRANSLATE -> state.translationAnswer
            else -> state.selectedOption ?: ""
        }

        updateState { it.copy(isEvaluatingWithAi = true) }

        scope.launch {
            val result = geminiClient.evaluateAndExplainAnswer(
                exercise = currentEx,
                userAnswer = userAnswer,
                lessonTitle = state.lesson?.title,
                targetLanguage = prefs.appLanguage
            )
            val shouldOverride = result.isCorrect && !state.isCorrect
            if (shouldOverride) {
                prefs.recordExerciseCompleted(currentEx.category, true, 100)
                historyRepo.recordAttempt(
                    ExerciseAttempt(
                        id = "att_override_${System.currentTimeMillis()}",
                        lessonId = state.lesson?.id ?: "",
                        exerciseId = currentEx.id,
                        category = currentEx.category,
                        wasCorrect = true,
                        accuracyScore = 100,
                        userAnswer = "$userAnswer (AI Validated)",
                        expectedAnswer = currentEx.correctAnswer
                    )
                )
            }
            updateState { current ->
                current.copy(
                    isEvaluatingWithAi = false,
                    isCorrect = if (shouldOverride) true else current.isCorrect,
                    isOverriddenByAi = if (shouldOverride) true else current.isOverriddenByAi,
                    score = if (shouldOverride) current.score + 10 else current.score,
                    exerciseResults = if (shouldOverride) current.exerciseResults + (currentEx.id to true) else current.exerciseResults,
                    aiEvaluationResult = result,
                    aiExplanation = result.specificExplanation,
                    showAiExplanationDialog = true
                )
            }
        }
    }
}
