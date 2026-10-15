package com.app.grammarai.ui.practice.evaluator

import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.ui.practice.ExerciseUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles user interactions and verification for SELECT_PAIRS drill exercises.
 * Uses distinct IDs for both left and right cards to handle duplicate values
 * (e.g. "he" -> "is", "she" -> "is") without cross-selection or blocking bugs.
 */
class DrillPairMatcher(
    private val scope: CoroutineScope,
    private val stateProvider: () -> ExerciseUiState,
    private val updateState: ((ExerciseUiState) -> ExerciseUiState) -> Unit,
    private val onAllPairsMatched: () -> Unit
) {
    fun selectLeft(cardId: String, text: String) {
        val ex = stateProvider().currentExercise ?: return
        if (stateProvider().isAnswerChecked) return
        if (stateProvider().matchedLeftIds.contains(cardId)) return

        val currentRightId = stateProvider().selectedPairRightId
        val currentRightText = stateProvider().selectedPairRightText
        if (currentRightId != null && currentRightText != null) {
            evaluateMatch(ex, cardId, text, currentRightId, currentRightText)
        } else {
            updateState {
                it.copy(
                    selectedPairLeftId = cardId,
                    selectedPairLeftText = text,
                    pairError = null
                )
            }
        }
    }

    fun selectRight(cardId: String, text: String) {
        val ex = stateProvider().currentExercise ?: return
        if (stateProvider().isAnswerChecked) return
        if (stateProvider().matchedRightIds.contains(cardId)) return

        val currentLeftId = stateProvider().selectedPairLeftId
        val currentLeftText = stateProvider().selectedPairLeftText
        if (currentLeftId != null && currentLeftText != null) {
            evaluateMatch(ex, currentLeftId, currentLeftText, cardId, text)
        } else {
            updateState {
                it.copy(
                    selectedPairRightId = cardId,
                    selectedPairRightText = text,
                    pairError = null
                )
            }
        }
    }

    private fun evaluateMatch(
        exercise: GeneratedExercise,
        leftId: String,
        leftText: String,
        rightId: String,
        rightText: String
    ) {
        val isMatch = exercise.pairs.any {
            it.left.trim().equals(leftText.trim(), ignoreCase = true) &&
                    it.right.trim().equals(rightText.trim(), ignoreCase = true)
        }

        if (isMatch) {
            val newMatchedLeft = stateProvider().matchedLeftIds + leftId
            val newMatchedRight = stateProvider().matchedRightIds + rightId
            val allMatched = exercise.pairs.isNotEmpty() && newMatchedLeft.size == exercise.pairs.size

            updateState {
                it.copy(
                    selectedPairLeftId = null,
                    selectedPairLeftText = null,
                    selectedPairRightId = null,
                    selectedPairRightText = null,
                    matchedLeftIds = newMatchedLeft,
                    matchedRightIds = newMatchedRight,
                    matchedPairs = newMatchedLeft,
                    pairError = null
                )
            }

            if (allMatched) {
                onAllPairsMatched()
            }
        } else {
            updateState {
                it.copy(
                    pairError = leftId to rightId,
                    selectedPairLeftId = null,
                    selectedPairLeftText = null,
                    selectedPairRightId = null,
                    selectedPairRightText = null
                )
            }
            scope.launch {
                delay(800)
                updateState { it.copy(pairError = null) }
            }
        }
    }
}
