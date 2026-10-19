package com.app.grammarai.ui.practice

import com.app.grammarai.core.mvi.UiEffect
import com.app.grammarai.core.mvi.UiIntent
import com.app.grammarai.core.mvi.UiState
import com.app.grammarai.data.model.AiEvaluationResult
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.model.Lesson
import com.app.grammarai.data.model.SkillMetrics
import com.app.grammarai.data.remote.LiveConversationTurn

enum class LiveVoiceState {
    IDLE,
    CONNECTING,
    LISTENING,
    USER_SPEAKING,
    AI_THINKING,
    AI_SPEAKING,
    ENDED
}

data class ExerciseUiState(
    val lesson: Lesson? = null,
    val exercises: List<GeneratedExercise> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val isLoadingExercises: Boolean = true,
    val selectedOption: String? = null,
    val reconstructedWords: List<String> = emptyList(),
    val translationAnswerMode: TranslationAnswerMode = TranslationAnswerMode.ORDER,
    val translationTextAnswer: String = "",
    val translationOrderedWords: List<String> = emptyList(),
    val isAnswerChecked: Boolean = false,
    val isCorrect: Boolean = false,
    val consecutiveCorrectCount: Int = 0,
    val score: Int = 0,
    val isCompleted: Boolean = false,
    val showCompletionCelebration: Boolean = false,
    val streakDays: Int = 1,
    val completedLessonsCount: Int = 0,
    val targetLessonsCount: Int = 5,
    val skillMetrics: SkillMetrics = SkillMetrics(),
    val isPlayingAudio: Boolean = false,
    val isAudioLoading: Boolean = false,

    // Live Conversation state
    val conversationTurns: List<LiveConversationTurn> = emptyList(),
    val currentConversationTopic: String = "",
    val userReplySuggestions: List<String> = emptyList(),
    val isLiveConversationActive: Boolean = false,
    val isConversationEnded: Boolean = false,
    val conversationTurnCount: Int = 0,
    val maxConversationTurns: Int = 8,
    val liveStreamingAiText: String = "",
    val liveStreamingUserText: String = "",
    val isGeneratingSuggestions: Boolean = false,
    val isAiRespondingLive: Boolean = false,
    val liveVoiceState: LiveVoiceState = LiveVoiceState.IDLE,
    val isMicMuted: Boolean = false,

    // Speech Recording & Pronunciation Evaluation state
    val isListening: Boolean = false,
    val spokenTranscript: String = "",
    val pronunciationMatchScore: Int? = null,
    
    // AI Explanation & Override state
    val aiExplanation: String? = null,
    val aiEvaluationResult: AiEvaluationResult? = null,
    val isGeneratingExplanation: Boolean = false,
    val isEvaluatingWithAi: Boolean = false,
    val showAiExplanationDialog: Boolean = false,
    val isOverriddenByAi: Boolean = false,

    // Matching Pairs state
    val selectedPairLeftId: String? = null,
    val selectedPairRightId: String? = null,
    val selectedPairLeftText: String? = null,
    val selectedPairRightText: String? = null,
    val matchedLeftIds: Set<String> = emptySet(),
    val matchedRightIds: Set<String> = emptySet(),
    val matchedPairs: Set<String> = emptySet(),
    val pairError: Pair<String, String>? = null,

    // Native Translation state
    val isTranslationVisible: Boolean = false,
    val isTranslating: Boolean = false,
    val exerciseTranslations: Map<String, String> = emptyMap(),

    // Per-exercise correctness results for accurate percentage scoring
    val exerciseResults: Map<String, Boolean> = emptyMap()
) : UiState {
    val currentExercise: GeneratedExercise?
        get() = exercises.getOrNull(currentExerciseIndex)

    val currentExerciseTranslation: String?
        get() = currentExercise?.id?.let { exerciseTranslations[it] }

    val translationAnswer: String
        get() = when (translationAnswerMode) {
            TranslationAnswerMode.WRITE -> translationTextAnswer
            TranslationAnswerMode.ORDER -> translationOrderedWords.joinToString(" ")
        }

    val isCurrentExerciseTranslatable: Boolean
        get() = com.app.grammarai.core.translation.OnDeviceTranslationManager.isExerciseCompatible(currentExercise)

    val isFirstExerciseConversation: Boolean
        get() = currentExerciseIndex == 0 && currentExercise?.type == com.app.grammarai.data.model.ExerciseType.CONVERSATION_DIALOGUE

    val gradableExercises: List<GeneratedExercise>
        get() = exercises.filter {
            it.type != com.app.grammarai.data.model.ExerciseType.THEORY &&
            it.type != com.app.grammarai.data.model.ExerciseType.LONG_READING
        }

    val correctCount: Int
        get() = gradableExercises.count { exerciseResults[it.id] == true }

    val calculatedScorePercent: Int
        get() {
            if (gradableExercises.isEmpty()) return 100
            val ratio = correctCount.toFloat() / gradableExercises.size.toFloat()
            return kotlin.math.round(ratio * 100).toInt().coerceIn(0, 100)
        }

    val progressFraction: Float
        get() = if (exercises.isEmpty()) 0f else (currentExerciseIndex + 1).toFloat() / exercises.size.toFloat()
}

enum class TranslationAnswerMode {
    WRITE,
    ORDER
}

sealed interface ExerciseUiIntent : UiIntent {
    data class LoadLesson(val lessonId: String) : ExerciseUiIntent
    data class SelectOption(val option: String) : ExerciseUiIntent
    data class InputText(val text: String) : ExerciseUiIntent
    data class SetTranslationAnswerMode(val mode: TranslationAnswerMode) : ExerciseUiIntent
    data class InputTranslationAnswer(val text: String) : ExerciseUiIntent
    data class AddTranslationWord(val word: String) : ExerciseUiIntent
    data class RemoveTranslationWord(val index: Int) : ExerciseUiIntent
    object ClearTranslationWords : ExerciseUiIntent
    data class TapReconstructWord(val word: String) : ExerciseUiIntent
    data class RemoveReconstructWord(val index: Int) : ExerciseUiIntent
    object ClearReconstruction : ExerciseUiIntent
    object CheckAnswer : ExerciseUiIntent
    object ToggleExerciseTranslation : ExerciseUiIntent
    object DismissExerciseTranslation : ExerciseUiIntent
    object GiveUpExercise : ExerciseUiIntent
    object NextExercise : ExerciseUiIntent
    object FinishPractice : ExerciseUiIntent
    object DismissCelebration : ExerciseUiIntent
    object Back : ExerciseUiIntent
    object PlayExerciseAudio : ExerciseUiIntent
    object StopAudio : ExerciseUiIntent
    object ToggleAudio : ExerciseUiIntent
    data class SpeakText(val text: String) : ExerciseUiIntent

    // Matching Pairs Intents
    data class SelectPairLeft(val id: String, val text: String) : ExerciseUiIntent
    data class SelectPairRight(val id: String, val text: String) : ExerciseUiIntent

    // Live Conversation intents
    data class SendConversationTurn(val text: String) : ExerciseUiIntent
    data class ChangeConversationTopic(val newTopic: String) : ExerciseUiIntent
    object StartLiveConversation : ExerciseUiIntent
    object EndLiveConversation : ExerciseUiIntent
    data class LiveStreamingTextUpdated(val aiText: String, val userText: String) : ExerciseUiIntent
    data class LiveTurnsUpdated(val turns: List<LiveConversationTurn>, val isEnded: Boolean) : ExerciseUiIntent
    object ContinueFromConversation : ExerciseUiIntent

    // Speech Recognition & Voice Intents
    object ToggleSpeechRecording : ExerciseUiIntent
    data class OnSpokenResult(val transcript: String) : ExerciseUiIntent
    data class AudioLoadingChanged(val isLoading: Boolean) : ExerciseUiIntent
    data class AudioPlayingChanged(val isPlaying: Boolean) : ExerciseUiIntent
    data class ListeningChanged(val isListening: Boolean) : ExerciseUiIntent

    // AI Explanation & Evaluation Intents
    object RequestAiEvaluation : ExerciseUiIntent
    object DismissAiExplanationDialog : ExerciseUiIntent
}

sealed interface ExerciseUiEffect : UiEffect {
    object NavigateBack : ExerciseUiEffect
    data class NavigateToRadar(val lessonId: String) : ExerciseUiEffect
    object NavigateToFeed : ExerciseUiEffect
}
